package linksawakening.world;

import linksawakening.world.entity.event.MoldormSegmentPosition;

import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.HookshotBridgeMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.MoldormMotion;

import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.LinkFinalPositionRequest;

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import static linksawakening.world.RoomEntityRuntime.*;

/** ROM background collision, falling, recoil and Link push glue. */
final class EntityBackgroundMechanics {
    private final RoomEntityRuntime runtime;
    EntityBackgroundMechanics(RoomEntityRuntime runtime) { this.runtime = runtime; }

    boolean moldormOrHeadOverlapsSword(RoomEntity entity,
                                                int swordX, int swordWidth,
                                                int swordY, int swordHeight) {
        if (RoomEntityCombatRules.overlapsSword(
                entity, swordX, swordWidth, swordY, swordHeight)) {
            return true;
        }
        if (entity.type() != runtime.ENTITY_MOLDORM) {
            return false;
        }
        if (runtime.enemyFlashCountdown[entity.slot()] != 0) {
            return false;
        }
        MoldormSegmentPosition tail = runtime.moldormMotion.vulnerableTail(entity.slot());
        RoomEntity tailCollisionEntity = withPositionAndVariant(
            entity, tail.x(), (tail.y() + entity.z()) & 0xFF, entity.spriteVariant());
        return RoomEntityCombatRules.overlapsSword(
            tailCollisionEntity, swordX, swordWidth, swordY, swordHeight);
    }

    void advanceHookshotBridgeEntity(int index, RoomEntity entity) {
        HookshotBridgeMotion.State bridgeState = runtime.hookshotBridgeMotion.state(entity.slot());
        if (bridgeState == null) {
            runtime.clearEntity(entity.slot());
            return;
        }

        HookshotBridgeMotion.ObjectCell cell = HookshotBridgeMotion.objectCell(bridgeState);
        HookshotBridgeMotion.Step step = runtime.hookshotBridgeMotion.advance(entity.slot());
        RoomEntity moved = withPositionAndVariant(entity, step.state().x(), step.state().y(),
            entity.spriteVariant());
        RoomEntityObjectSample object = runtime.objectQuery == null ? null : runtime.objectQuery.sample(moved);
        boolean clear = object != null && object.objectId() != runtime.OBJECT_HOOKSHOT_BRIDGE_REPLACEMENT
            && object.physicsFlag() == 0;
        runtime.hookshotBridgeUpdates.add(new HookshotBridgeUpdate(
            cell.objectLeft(), cell.objectTop(), bridgeState.direction(), !clear));
        if (clear) {
            runtime.clearEntity(entity.slot());
        } else {
            runtime.slots[index] = moved;
        }
    }

    void resolvePushedBlockLinkContact(int slot, int linkEntityX, int linkEntityY) {
        if (slot >= 0 && slot < runtime.slots.length && runtime.slots[slot].type() == runtime.ENTITY_PUSHED_BLOCK
            && runtime.slots[slot].status() == EntityStatus.ACTIVE
            && RoomEntityCombatRules.overlapsLink(runtime.slots[slot], linkEntityX, linkEntityY)) {
            requestLinkPush(runtime.slots[slot], EntityLinkCollisionRules.PUSHED_BLOCK);
        }
    }

    boolean gameplayEntityTimersInteractive(boolean creditsGameplay) { return runtime.roomInspection.gameplayEntityTimersInteractive(creditsGameplay); }

    boolean hasNoGroundInteraction(RoomEntity entity) { return runtime.roomInspection.hasNoGroundInteraction(entity); }

    boolean beginFalling(RoomEntity entity,
                                 RoomEntityGroundInteraction.PitTransition transition,
                                 boolean ignoreHitsDecrementedBeforeHandler) {
        int slot = entity.slot();
        int ignoreHitsCountdown = runtime.enemyIgnoreHitsCountdown[slot];
        if (ignoreHitsDecrementedBeforeHandler) {
            // The Java runtime keeps a shared combat countdown pass for the
            // already-ported handlers.  In the ROM, the pit branch reads the
            // value before that branch's own decrement; reconstruct that
            // value here so INIT and ACTIVE entities both get one decrement
            // at the same source-level point.
            ignoreHitsCountdown = (ignoreHitsCountdown + 1) & 0xFF;
        }
        if (ignoreHitsCountdown == 0) {
            return false;
        }

        ignoreHitsCountdown--;
        runtime.enemyIgnoreHitsCountdown[slot] = ignoreHitsCountdown;
        runtime.enemyFlashCountdown[slot] = 0;
        runtime.fallingTargetX[slot] = transition.targetX() & 0xFF;
        runtime.fallingTargetY[slot] = transition.targetY() & 0xFF;
        runtime.fallingSpeedX[slot] = 0;
        runtime.fallingSpeedY[slot] = 0;
        runtime.fallingSpeedXAccumulator[slot] = 0;
        runtime.fallingSpeedYAccumulator[slot] = 0;
        runtime.fallingVisualYOffset[slot] = 0;

        boolean longFallingTransition = entity.type() == runtime.ENTITY_MOBLIN_SWORD
            || entity.type() == runtime.ENTITY_MOBLIN || entity.type() == runtime.ENTITY_OCTOROK;
        runtime.enemyTransitionCountdown[slot] = longFallingTransition ? 0x6F : 0x48;
        if (!longFallingTransition && ignoreHitsCountdown == 0) {
            runtime.enemyTransitionCountdown[slot] = 0x2F;
            runtime.pendingEntityEvents.add(new EntityCombatEvent(
                slot, entity.type(), 0, false,
                EntityCombatEvent.SoundChannel.JINGLE, runtime.FALLING_JINGLE_ID));
        }
        return true;
    }

    RoomEntity advanceFallingEntity(RoomEntity entity) {
        int slot = entity.slot();
        int transition = runtime.enemyTransitionCountdown[slot] & 0xFF;
        if (transition == 0) {
            return null;
        }

        if (transition >= 0x40) {
            // EntityFallHandler calls SetEntityVariantForDirection_03 three
            // times before the family handler. ReturnIfNonInteractive then
            // stops that handler after presentation because the status is
            // still FALLING, so movement and wall collision must not run here.
            runtime.fallingVisualYOffset[slot] = 0;
            int variant = entity.spriteVariant();
            if (entity.type() == runtime.ENTITY_OCTOROK || entity.type() == runtime.ENTITY_MOBLIN) {
                variant = runtime.roamingEnemyMotion.advancePresentationVariant(slot, 3);
            } else if (entity.type() == runtime.ENTITY_MOBLIN_SWORD) {
                variant = runtime.moblinSwordMotion.advancePresentationVariant(slot, entity.x(), 3);
            }
            return withPositionAndVariant(entity, entity.x(), entity.y(), variant);
        }

        int phase = (transition >>> 4) & 0x03;
        runtime.fallingVisualYOffset[slot] = runtime.FALLING_VISUAL_Y_OFFSETS[phase];
        if (transition == 0x3F) {
            runtime.pendingEntityEvents.add(new EntityCombatEvent(
                slot, entity.type(), 0, false,
                EntityCombatEvent.SoundChannel.JINGLE, runtime.FALLING_JINGLE_ID));
        }

        FallingVector vector = vectorTowardsTarget(entity.x(), entity.y(), entity.z(),
            runtime.fallingTargetX[slot], runtime.fallingTargetY[slot], runtime.FALLING_VECTOR_LENGTHS[phase]);
        runtime.fallingSpeedX[slot] = vector.x();
        runtime.fallingSpeedY[slot] = vector.y();
        int x = addFallingSpeedToPosition(entity.x(), runtime.fallingSpeedX[slot],
            runtime.fallingSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(entity.y(), runtime.fallingSpeedY[slot],
            runtime.fallingSpeedYAccumulator, slot);
        if (runtime.spriteHandlers != null) {
            entity = withDefinition(entity, runtime.spriteHandlers.forFallingEntity(), phase);
        }
        return withPositionAndVariant(entity, x, y, phase);
    }

    int verticalSpeedZ(RoomEntity entity) {
        int slot = entity.slot();
        if (runtime.enemyDropActive[slot]) {
            return runtime.enemyDropMotion.speedZ(slot);
        }
        if (entity.type() == runtime.ENTITY_TEKTITE) {
            return runtime.tektiteMotion.speedZ(slot);
        }
        if (entity.type() == runtime.ENTITY_FISH) {
            return runtime.fishMotion.speedZ(slot);
        }
        if (entity.type() == runtime.ENTITY_CROW) {
            return runtime.crowMotion.speedZ(slot);
        }
        if (entity.type() == runtime.ENTITY_CUCCO) {
            return runtime.cuccoMotion.speedZ(slot);
        }
        if (entity.type() == runtime.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            return runtime.secretSeashellMotion.speedZ(slot);
        }
        if (entity.type() == runtime.ENTITY_WINGED_OCTOROK) {
            return runtime.wingedOctorokMotion.speedZ(slot);
        }
        if (entity.type() == runtime.ENTITY_STALFOS_AGGRESSIVE) {
            return runtime.stalfosAggressiveMotion.speedZ(slot);
        }
        if (entity.type() == runtime.ENTITY_STALFOS_EVASIVE) {
            return runtime.stalfosEvasiveMotion.speedZ(slot);
        }
        if (isZolGelType(entity.type())) {
            return runtime.zolGelMotion.speedZ(slot);
        }
        if (entity.type() == runtime.ENTITY_HIDING_ZOL) {
            return runtime.hidingZolMotion.speedZ(slot);
        }
        if (entity.type() == runtime.ENTITY_POLS_VOICE) {
            return runtime.polsVoiceMotion.speedZ(slot);
        }
        if (entity.type() == runtime.ENTITY_SPIKED_BEETLE) {
            return runtime.spikedBeetleMotion.speedZ(slot);
        }
        if (entity.type() == runtime.ENTITY_DOG) {
            return runtime.dogMotion.speedZ(slot);
        }
        if (isEnemyProjectileType(entity.type())) {
            return runtime.enemyProjectileMotion.speedZ(slot);
        }
        if (entity.type() == runtime.ENTITY_ARROW) {
            return runtime.playerArrowMotion.speedZ(slot);
        }
        if (ColorShellMotion.isColorShellType(entity.type())) {
            return runtime.colorShellMotion.speedZ(slot);
        }
        return 0;
    }

    boolean entityHandlerInteractive(boolean creditsGameplay) { return runtime.enemyInspection.entityHandlerInteractive(creditsGameplay); }

    boolean entityTimersInteractive() { return runtime.enemyInspection.entityTimersInteractive(); }

    void requestArmosLinkPush(RoomEntity entity, int linkEntityX, int linkEntityY,
                                      int linkMotionState) {
        if (linkMotionState >= EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE
            || !RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return;
        }
        requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
    }

    void requestLinkPush(RoomEntity entity,
                                 EntityLinkCollisionRules.LinkPushPolicy policy) {
        if (entity == null || policy == null || !policy.restoreFinalPosition()) {
            return;
        }
        runtime.pendingLinkFinalPositionRequests.add(new LinkFinalPositionRequest(
            entity.slot(), policy.resetPegasusBoots(), policy.resetHookshotChain(),
            policy.markLinkPushing(), policy.clearLinkPositionIncrement()));
        if (policy.resetHookshotChain()) {
            resetHookshotChainAfterLinkPush();
        }
    }

    void resetHookshotChainAfterLinkPush() {
        for (int slot = 0; slot < runtime.slots.length; slot++) {
            if (!runtime.slots[slot].loaded() || runtime.slots[slot].type() != runtime.ENTITY_HOOKSHOT_CHAIN
                || !runtime.hookshotChainMotion.active(slot)) {
                continue;
            }
            HookshotChainMotion.State state = runtime.hookshotChainMotion.state(slot);
            runtime.hookshotChainMotion.setState(slot, new HookshotChainMotion.State(
                state.x(), state.y(), state.z(), state.direction(), state.speedX(),
                state.speedY(), state.transitionCountdown(), state.speedXAccumulator(),
                state.speedYAccumulator(), 0, state.wallCollisionPending()));
            return;
        }
    }

    EnemyRecoilMotion.Update applyEnemyRecoilIfNeeded(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!runtime.enemyRecoilMotion.isActive(slot)) {
            return new EnemyRecoilMotion.Update(entity, false);
        }
        if (runtime.enemyIgnoreHitsCountdown[slot] == 0) {
            runtime.enemyRecoilMotion.clear(slot);
            return new EnemyRecoilMotion.Update(entity, false);
        }

        if (isRoamingEnemyType(entity.type())) {
            runtime.roamingEnemyMotion.beginRecoil(slot);
        }
        // ApplyRecoilIfNeeded_03/06 decrements the shared countdown
        // immediately before applying one fixed-point recoil step.
        runtime.enemyIgnoreHitsCountdown[slot]--;
        EnemyRecoilMotion.Update update = runtime.enemyRecoilMotion.advance(
            entity, backgroundCollision, isRoamingEnemyType(entity.type()));
        if (update.blocked() && isRoamingEnemyType(entity.type())) {
            // StopEntityRecoilOnCollision clears the ignore-hits countdown.
            runtime.enemyIgnoreHitsCountdown[slot] = 0;
        }
        return update;
    }
}
