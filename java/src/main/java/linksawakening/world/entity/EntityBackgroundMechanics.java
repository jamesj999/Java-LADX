package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.MoldormSegmentPosition;
import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.HookshotBridgeMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;

/** ROM background collision, falling, recoil and Link push glue. */
public final class EntityBackgroundMechanics {
    private final EntityBackgroundMechanicsServices runtime;

    EntityBackgroundMechanics(EntityBackgroundMechanicsServices runtime) {
        this.runtime = runtime;
    }

    public RoomEntityBackgroundInteraction interaction() {
        return runtime.physicsTables().readBackgroundInteraction();
    }

    boolean moldormOrHeadOverlapsSword(RoomEntity entity, int swordX, int swordWidth, int swordY, int swordHeight) {
        if (RoomEntityCombatRules.overlapsSword(entity, swordX, swordWidth, swordY, swordHeight)) {
            return true;
        }
        if (entity.type() != EntityRomConstants.ENTITY_MOLDORM) {
            return false;
        }
        if (runtime.combatTables().readEnemyFlashCountdown(entity.slot()) != 0) {
            return false;
        }
        MoldormSegmentPosition tail = runtime.motionSet().moldormMotion.vulnerableTail(entity.slot());
        RoomEntity tailCollisionEntity =
                withPositionAndVariant(entity, tail.x(), (tail.y() + entity.z()) & 0xFF, entity.spriteVariant());
        return RoomEntityCombatRules.overlapsSword(tailCollisionEntity, swordX, swordWidth, swordY, swordHeight);
    }

    public void advanceHookshotBridgeEntity(int index, RoomEntity entity) {
        HookshotBridgeMotion.State bridgeState =
                runtime.motionSet().hookshotBridgeMotion.state(entity.slot());
        if (bridgeState == null) {
            runtime.clearEntity(entity.slot());
            return;
        }

        HookshotBridgeMotion.ObjectCell cell = HookshotBridgeMotion.objectCell(bridgeState);
        HookshotBridgeMotion.Step step =
                runtime.motionSet().hookshotBridgeMotion.advance(entity.slot());
        RoomEntity moved =
                withPositionAndVariant(entity, step.state().x(), step.state().y(), entity.spriteVariant());
        RoomEntityObjectSample object = runtime.physicsTables().readObjectQuery() == null
                ? null
                : runtime.physicsTables().readObjectQuery().sample(moved);
        boolean clear = object != null
                && object.objectId() != EntityRomConstants.OBJECT_HOOKSHOT_BRIDGE_REPLACEMENT
                && object.physicsFlag() == 0;
        runtime.events()
                .hookshotBridgeUpdates
                .add(new HookshotBridgeUpdate(cell.objectLeft(), cell.objectTop(), bridgeState.direction(), !clear));
        if (clear) {
            runtime.clearEntity(entity.slot());
        } else {
            runtime.slots()[index] = moved;
        }
    }

    void resolvePushedBlockLinkContact(int slot, int linkEntityX, int linkEntityY) {
        if (slot >= 0
                && slot < runtime.slots().length
                && runtime.slots()[slot].type() == EntityRomConstants.ENTITY_PUSHED_BLOCK
                && runtime.slots()[slot].status() == EntityStatus.ACTIVE
                && RoomEntityCombatRules.overlapsLink(runtime.slots()[slot], linkEntityX, linkEntityY)) {
            requestLinkPush(runtime.slots()[slot], EntityLinkCollisionRules.PUSHED_BLOCK);
        }
    }

    boolean gameplayEntityTimersInteractive(boolean creditsGameplay) {
        return runtime.roomInspection().gameplayEntityTimersInteractive(creditsGameplay);
    }

    boolean hasNoGroundInteraction(RoomEntity entity) {
        return runtime.roomInspection().hasNoGroundInteraction(entity);
    }

    boolean beginFalling(
            RoomEntity entity,
            RoomEntityGroundInteraction.PitTransition transition,
            boolean ignoreHitsDecrementedBeforeHandler) {
        int slot = entity.slot();
        int ignoreHitsCountdown = runtime.combatTables().readEnemyIgnoreHitsCountdown(slot);
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
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, ignoreHitsCountdown);
        runtime.combatTables().writeEnemyFlashCountdown(slot, 0);
        runtime.physicsTables().writeFallingTargetX(slot, transition.targetX() & 0xFF);
        runtime.physicsTables().writeFallingTargetY(slot, transition.targetY() & 0xFF);
        runtime.physicsTables().writeFallingSpeedX(slot, 0);
        runtime.physicsTables().writeFallingSpeedY(slot, 0);
        runtime.physicsTables().writeFallingSpeedXAccumulator(slot, 0);
        runtime.physicsTables().writeFallingSpeedYAccumulator(slot, 0);
        runtime.physicsTables().writeFallingVisualYOffset(slot, 0);

        boolean longFallingTransition = entity.type() == EntityRomConstants.ENTITY_MOBLIN_SWORD
                || entity.type() == EntityRomConstants.ENTITY_MOBLIN
                || entity.type() == EntityRomConstants.ENTITY_OCTOROK;
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, longFallingTransition ? 0x6F : 0x48);
        if (!longFallingTransition && ignoreHitsCountdown == 0) {
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x2F);
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot,
                            entity.type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            EntityRomConstants.FALLING_JINGLE_ID));
        }
        return true;
    }

    RoomEntity advanceFallingEntity(RoomEntity entity) {
        int slot = entity.slot();
        int transition = runtime.physicsTables().readEnemyTransitionCountdown(slot) & 0xFF;
        if (transition == 0) {
            return null;
        }

        if (transition >= 0x40) {
            // EntityFallHandler calls SetEntityVariantForDirection_03 three
            // times before the family handler. ReturnIfNonInteractive then
            // stops that handler after presentation because the status is
            // still FALLING, so movement and wall collision must not run here.
            runtime.physicsTables().writeFallingVisualYOffset(slot, 0);
            int variant = entity.spriteVariant();
            if (entity.type() == EntityRomConstants.ENTITY_OCTOROK
                    || entity.type() == EntityRomConstants.ENTITY_MOBLIN) {
                variant = runtime.motionSet().roamingEnemyMotion.advancePresentationVariant(slot, 3);
            } else if (entity.type() == EntityRomConstants.ENTITY_MOBLIN_SWORD) {
                variant = runtime.motionSet().moblinSwordMotion.advancePresentationVariant(slot, entity.x(), 3);
            }
            return withPositionAndVariant(entity, entity.x(), entity.y(), variant);
        }

        int phase = (transition >>> 4) & 0x03;
        runtime.physicsTables().writeFallingVisualYOffset(slot, EntityRomConstants.FALLING_VISUAL_Y_OFFSETS[phase]);
        if (transition == 0x3F) {
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot,
                            entity.type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            EntityRomConstants.FALLING_JINGLE_ID));
        }

        FallingVector vector = vectorTowardsTarget(
                entity.x(),
                entity.y(),
                entity.z(),
                runtime.physicsTables().readFallingTargetX(slot),
                runtime.physicsTables().readFallingTargetY(slot),
                EntityRomConstants.FALLING_VECTOR_LENGTHS[phase]);
        runtime.physicsTables().writeFallingSpeedX(slot, vector.x());
        runtime.physicsTables().writeFallingSpeedY(slot, vector.y());
        int x = runtime.physicsTables()
                .advanceFallingX(entity.x(), runtime.physicsTables().readFallingSpeedX(slot), slot);
        int y = runtime.physicsTables()
                .advanceFallingY(entity.y(), runtime.physicsTables().readFallingSpeedY(slot), slot);
        if (runtime.spriteHandlers() != null) {
            entity = withDefinition(entity, runtime.spriteHandlers().forFallingEntity(), phase);
        }
        return withPositionAndVariant(entity, x, y, phase);
    }

    public int verticalSpeedZ(RoomEntity entity) {
        int slot = entity.slot();
        if (runtime.bossRewards().readEnemyDropActive(slot)) {
            return runtime.motionSet().enemyDropMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_TEKTITE) {
            return runtime.motionSet().tektiteMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_FISH) {
            return runtime.motionSet().fishMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_CROW) {
            return runtime.motionSet().crowMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_CUCCO) {
            return runtime.motionSet().cuccoMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            return runtime.motionSet().secretSeashellMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_WINGED_OCTOROK) {
            return runtime.motionSet().wingedOctorokMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_STALFOS_AGGRESSIVE) {
            return runtime.motionSet().stalfosAggressiveMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_STALFOS_EVASIVE) {
            return runtime.motionSet().stalfosEvasiveMotion.speedZ(slot);
        }
        if (isZolGelType(entity.type())) {
            return runtime.motionSet().zolGelMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_HIDING_ZOL) {
            return runtime.motionSet().hidingZolMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_POLS_VOICE) {
            return runtime.motionSet().polsVoiceMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE) {
            return runtime.motionSet().spikedBeetleMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_DOG) {
            return runtime.motionSet().dogMotion.speedZ(slot);
        }
        if (isEnemyProjectileType(entity.type())) {
            return runtime.motionSet().enemyProjectileMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_ARROW) {
            return runtime.motionSet().playerArrowMotion.speedZ(slot);
        }
        if (ColorShellMotion.isColorShellType(entity.type())) {
            return runtime.motionSet().colorShellMotion.speedZ(slot);
        }
        return 0;
    }

    boolean entityHandlerInteractive(boolean creditsGameplay) {
        return runtime.enemyInspection().entityHandlerInteractive(creditsGameplay);
    }

    public boolean entityTimersInteractive() {
        return runtime.enemyInspection().entityTimersInteractive();
    }

    public void requestArmosLinkPush(RoomEntity entity, int linkEntityX, int linkEntityY, int linkMotionState) {
        if (linkMotionState >= EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE
                || !RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return;
        }
        requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
    }

    public void requestLinkPush(RoomEntity entity, EntityLinkCollisionRules.LinkPushPolicy policy) {
        if (entity == null || policy == null || !policy.restoreFinalPosition()) {
            return;
        }
        runtime.events()
                .pendingLinkFinalPositionRequests
                .add(new LinkFinalPositionRequest(
                        entity.slot(),
                        policy.resetPegasusBoots(),
                        policy.resetHookshotChain(),
                        policy.markLinkPushing(),
                        policy.clearLinkPositionIncrement()));
        if (policy.resetHookshotChain()) {
            resetHookshotChainAfterLinkPush();
        }
    }

    void resetHookshotChainAfterLinkPush() {
        for (int slot = 0; slot < runtime.slots().length; slot++) {
            if (!runtime.slots()[slot].loaded()
                    || runtime.slots()[slot].type() != EntityRomConstants.ENTITY_HOOKSHOT_CHAIN
                    || !runtime.motionSet().hookshotChainMotion.active(slot)) {
                continue;
            }
            HookshotChainMotion.State state =
                    runtime.motionSet().hookshotChainMotion.state(slot);
            runtime.motionSet()
                    .hookshotChainMotion
                    .setState(
                            slot,
                            new HookshotChainMotion.State(
                                    state.x(),
                                    state.y(),
                                    state.z(),
                                    state.direction(),
                                    state.speedX(),
                                    state.speedY(),
                                    state.transitionCountdown(),
                                    state.speedXAccumulator(),
                                    state.speedYAccumulator(),
                                    0,
                                    state.wallCollisionPending()));
            return;
        }
    }

    public EnemyRecoilMotion.Update applyEnemyRecoilIfNeeded(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!runtime.motionSet().enemyRecoilMotion.isActive(slot)) {
            return new EnemyRecoilMotion.Update(entity, false);
        }
        if (runtime.combatTables().readEnemyIgnoreHitsCountdown(slot) == 0) {
            runtime.motionSet().enemyRecoilMotion.clear(slot);
            return new EnemyRecoilMotion.Update(entity, false);
        }

        if (isRoamingEnemyType(entity.type())) {
            runtime.motionSet().roamingEnemyMotion.beginRecoil(slot);
        }
        // ApplyRecoilIfNeeded_03/06 decrements the shared countdown
        // immediately before applying one fixed-point recoil step.
        runtime.combatTables().postEnemyIgnoreHitsCountdown(slot, -1);
        EnemyRecoilMotion.Update update = runtime.motionSet()
                .enemyRecoilMotion
                .advance(entity, backgroundCollision, isRoamingEnemyType(entity.type()));
        if (update.blocked() && isRoamingEnemyType(entity.type())) {
            // StopEntityRecoilOnCollision clears the ignore-hits countdown.
            runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, 0);
        }
        return update;
    }

    public boolean groundInteractionSideScrolling() {
        return runtime.physicsTables().readGroundInteractionSideScrolling();
    }

    public RoomEntityObjectQuery objectQuery() {
        return runtime.physicsTables().readObjectQuery();
    }

    public RoomEntityGroundInteraction groundInteraction() {
        return runtime.physicsTables().readGroundInteraction();
    }

    public boolean switchBlockAnimationActive() {
        return runtime.roomVariables().readSwitchBlockAnimationActive();
    }

    public void requestSwitchBlockAnimation() {
        runtime.roomVariables().writePendingSwitchBlockAnimationRequest(true);
    }

    public int entityGroundStatus(int slot) {
        return runtime.physicsTables().readEntityGroundStatus(slot);
    }

    public void setEntityGroundStatus(int slot, int value) {
        runtime.physicsTables().writeEntityGroundStatus(slot, value);
    }

    public void setFallingVisualYOffset(int slot, int value) {
        runtime.physicsTables().writeFallingVisualYOffset(slot, value);
    }

    public void initializeKeyQuicksandFall(int slot) {
        runtime.physicsTables().writeFallingTargetX(slot, 0x50);
        runtime.physicsTables().writeFallingTargetY(slot, 0x48);
        runtime.physicsTables().writeFallingSpeedX(slot, 0);
        runtime.physicsTables().writeFallingSpeedY(slot, 0);
        runtime.physicsTables().writeFallingSpeedXAccumulator(slot, 0);
        runtime.physicsTables().writeFallingSpeedYAccumulator(slot, 0);
        runtime.physicsTables().writeFallingVisualYOffset(slot, 0);
    }
}
