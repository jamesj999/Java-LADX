package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.Objects;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.FloatingItemMotion;

/** Ordered slot dispatch phase 5; false preserves the ROM handler early return. */
final class EntitySlotPhase5 {
    static boolean run(EntityFrameExecution state) {

        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_5_ROOSTER, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_5_COLOR_SHELL_TYPE, state)) return false;
        RoomEntity beforeDropPhysics = state.updated;
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && !state.keyDropTransitionActive
                && state.runtime.bossRewards.readEnemyDropActive(state.updated.slot())) {
            state.updated = state.runtime.entityState.motionSet.enemyDropMotion.advance(
                    state.updated,
                    state.frame,
                    state.runtime.entityState.physicsTables.readEntityGroundStatus(state.updated.slot()),
                    state.runtime.entityState.physicsTables.readGroundInteractionSideScrolling());
        }
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && !state.keyDropTransitionActive
                && state.applyGenericGroundInteraction
                && !state.runtime.backgroundMechanics.hasNoGroundInteraction(state.updated)
                && (state.handlerLinkCollisionEnabled || state.updated.type() != EntityRomConstants.ENTITY_BLOOPER)) {
            // ApplyEntityInteractionWithBackground runs after each ROM
            // entity handler's movement and before the final display-list
            // presentation. The room session supplies terrain physics;
            // direct runtime users retain the no-op default.
            int previousGroundStatus =
                    state.runtime.entityState.physicsTables.readEntityGroundStatus(state.updated.slot());
            RoomEntityGroundInteraction.Result groundResult = state.preAppliedGroundResult != null
                    ? state.preAppliedGroundResult
                    : Objects.requireNonNull(
                            state.runtime
                                    .entityState
                                    .physicsTables
                                    .readGroundInteraction()
                                    .apply(
                                            state.updated,
                                            state.frame,
                                            previousGroundStatus,
                                            state.runtime.backgroundMechanics.verticalSpeedZ(state.updated),
                                            state.runtime.entityState.physicsTables
                                                    .readGroundInteractionSideScrolling()),
                            "Room entity ground interaction returned null");
            state.updated = Objects.requireNonNull(
                    groundResult.entity(), "Room entity ground interaction returned a null entity");
            state.runtime.entityState.physicsTables.writeEntityGroundStatus(
                    state.updated.slot(), groundResult.groundStatus() & 0xFF);
            if (groundResult.pitTransition() != null
                    && state.runtime.backgroundMechanics.beginFalling(
                            state.updated, groundResult.pitTransition(), state.ignoreHitsDecrementedBeforeHandler)) {
                state.status = EntityStatus.FALLING;
            }
            if (groundResult.waterSplash()) {
                state.runtime.entityState.events.transientVfxRequests.add(
                        new TransientVfxRequest(TransientVfxType.WATER_SPLASH, state.updated.x(), state.updated.y()));
                state.runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                        state.updated.slot(),
                        state.updated.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x0E));
            }
            if (groundResult.unloaded()) {
                state.runtime.lifecycle.disableEntityWithoutPersistence(state.updated.slot());
                return false;
            }
            if (state.status == EntityStatus.ACTIVE
                    && state.runtime.bossRewards.readEnemyDropActive(state.updated.slot())) {
                // BouncingEntityPhysics calls the complete background helper before
                // bouncing. Ground effects alone omit the neighbouring bush walls.
                RoomEntityBackgroundInteraction dropBackground =
                        state.runtime.entityState.physicsTables.readBackgroundInteraction();
                if (dropBackground == null && state.backgroundCollision != null) {
                    dropBackground = RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
                }
                state.updated = state.runtime.entityState.motionSet.enemyDropMotion.resolveWalls(
                        beforeDropPhysics,
                        state.updated,
                        dropBackground,
                        state.runtime.entityState.combatTables.readEnemyIgnoreHitsCountdown(state.updated.slot()),
                        state.frame);
                boolean dropGroundCollision =
                        state.runtime.entityState.physicsTables.readGroundInteractionSideScrolling()
                                && state.runtime.entityState.motionSet.enemyDropMotion.speedY(state.updated.slot()) != 0
                                && (state.runtime.entityState.motionSet.enemyDropMotion.speedY(state.updated.slot())
                                                & 0x80)
                                        == 0
                                && state.backgroundCollision != null
                                && state.backgroundCollision.blocks(
                                        state.updated,
                                        EntityBackgroundCollisionResult.DOWN,
                                        state.updated.x(),
                                        state.updated.y());
                state.updated = state.runtime.entityState.motionSet.enemyDropMotion.bounce(
                        state.updated,
                        groundResult.groundStatus(),
                        state.runtime.entityState.physicsTables.readGroundInteractionSideScrolling(),
                        dropGroundCollision);
            }
        }
        state.genericLinkPushPolicy = EntityLinkCollisionRules.genericPolicyFor(state.updated.type());
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && state.handlerLinkCollisionEnabled
                && state.updated.type() != EntityRomConstants.ENTITY_GENIE
                && state.genericLinkPushPolicy.restoreFinalPosition()
                && RoomEntityCombatRules.overlapsLink(state.updated, state.linkEntityX, state.linkEntityY)) {
            // The shared PushLinkOutOfEntity_* helpers run after their
            // handler-specific work. Restore the pre-entity final
            // position and carry the helper's other side effects across
            // the room/runtime boundary.
            state.runtime.backgroundMechanics.requestLinkPush(state.updated, state.genericLinkPushPolicy);
        }
        if (ColorShellMotion.isColorShellType(state.updated.type())) {
            state.updated = state.runtime.slotProperties.refreshColorShellDisplay(state.updated, state.status);
        }
        if (state.status == EntityStatus.ACTIVE && isRuntimeFloatingItem(state.updated)) {
            state.updated = withZ(
                    state.updated,
                    FloatingItemMotion.zForFrame(
                            state.runtime.entityState.physicsTables.readGroundInteractionSideScrolling(), state.frame));
        }
        state.variant = state.preserveGhiniPresentation
                        || state.preserveMadBomberPresentation
                        || state.preserveBombitePresentation
                        || state.preserveIronMaskPresentation
                        || state.preserveSnakePresentation
                        || state.preserveStarPresentation
                        || state.preserveBlooperPresentation
                        || state.preserveWingedOctorokPresentation
                        || state.preservePincerPresentation
                        || state.preserveBushCrawlerPresentation
                        || state.preserveMimicPresentation
                        || state.preserveMaskedMimicPresentation
                        || state.preserveMiniMoldormPresentation
                        || state.preserveMoldormPresentation
                        || state.preserveCuccoPresentation
                        || state.preserveGopongaFlowerPresentation
                        || state.preserveGiantGopongaPresentation
                        || state.preserveGopongaProjectilePresentation
                        || state.preservePokeyPresentation
                        || state.preservePiranhaPresentation
                        || state.preserveZoraPresentation
                        || state.preserveZombiePresentation
                        || state.preserveBuzzBlobPresentation
                        || state.preserveSandCrabPresentation
                        || state.preserveDogPresentation
                        || state.preserveUrchinPresentation
                        || state.preserveWitchRatPresentation
                        || state.preserveWitchPresentation
                        || state.preserveRoosterPresentation
                        || state.preserveWizrobePresentation
                        || state.preserveWizrobeProjectilePresentation
                        || state.preservePolsVoicePresentation
                        || state.preserveSpikedBeetlePresentation
                        || state.preserveArmosKnightPresentation
                        || state.preserveHinoxPresentation
                        || state.preserveRollingBonesPresentation
                        || state.preserveThreeOfAKindPresentation
                        || state.preserveMoblinKingPresentation
                        || state.preserveSecretSeashellPresentation
                        || state.preserveDroppablePresentation
                        || state.preserveTarinPresentation
                ? state.updated.spriteVariant()
                : EntityTypeRules.variantFor(state.updated, state.frame);
        if (state.status == EntityStatus.ACTIVE && state.runtime.lifecycle.shouldDisappear(state.entity)) {
            state.variant =
                    (state.runtime.entityState.physicsTables.readSlowTransitionCountdown(state.entity.slot()) & 0x01)
                                    != 0
                            ? 0
                            : -1;
        }
        state.renderFlipAttribute = state.preserveGhiniPresentation
                        || state.preserveMadBomberPresentation
                        || state.preserveBombitePresentation
                        || state.preserveIronMaskPresentation
                        || state.preserveSnakePresentation
                        || state.preserveStarPresentation
                        || state.preserveBlooperPresentation
                        || state.preserveWingedOctorokPresentation
                        || state.preservePincerPresentation
                        || state.preserveBushCrawlerPresentation
                        || state.preserveMimicPresentation
                        || state.preserveMaskedMimicPresentation
                        || state.preserveMiniMoldormPresentation
                        || state.preserveMoldormPresentation
                        || state.preserveCuccoPresentation
                        || state.preserveGopongaFlowerPresentation
                        || state.preserveGiantGopongaPresentation
                        || state.preserveGopongaProjectilePresentation
                        || state.preservePokeyPresentation
                        || state.preservePiranhaPresentation
                        || state.preserveZoraPresentation
                        || state.preserveZombiePresentation
                        || state.preserveBuzzBlobPresentation
                        || state.preserveSandCrabPresentation
                        || state.preserveDogPresentation
                        || state.preserveUrchinPresentation
                        || state.preserveWitchRatPresentation
                        || state.preserveWitchPresentation
                        || state.preserveRoosterPresentation
                        || state.preserveWizrobePresentation
                        || state.preserveWizrobeProjectilePresentation
                        || state.preservePolsVoicePresentation
                        || state.preserveSpikedBeetlePresentation
                        || state.preserveArmosKnightPresentation
                        || state.preserveHinoxPresentation
                        || state.preserveRollingBonesPresentation
                        || state.preserveThreeOfAKindPresentation
                        || state.preserveMoblinKingPresentation
                        || state.preserveSecretSeashellPresentation
                        || state.preserveDroppablePresentation
                        || state.preserveTarinPresentation
                ? state.updated.entityFlipAttribute()
                : state.runtime.entityState.physicsTables.readBaseEntityFlipAttribute(state.entity.slot());
        if (state.preserveBombitePresentation && state.updated.type() == EntityRomConstants.ENTITY_TIMER_BOMBITE) {
            state.renderFlipAttribute |=
                    (state.runtime.bombInteractions.readBombPrivateCountdown1(state.updated.slot()) << 3) & 0x10;
        }
        if (state.runtime.entityState.combatTables.readEnemyFlashCountdown(state.entity.slot()) > 0) {
            state.renderFlipAttribute ^=
                    (state.runtime.entityState.combatTables.readEnemyFlashCountdown(state.entity.slot()) << 2) & 0x10;
        }
        if (state.status != state.entity.status()
                || state.variant != state.entity.spriteVariant()
                || state.updated.type() != state.entity.type()
                || state.updated.spriteDefinition() != state.entity.spriteDefinition()
                || state.updated.x() != state.originalEntity.x()
                || state.updated.y() != state.originalEntity.y()
                || state.renderFlipAttribute != state.originalEntity.entityFlipAttribute()
                || state.updated.z() != state.originalEntity.z()) {
            state.runtime.slots[state.index] = new RoomEntity(
                    state.updated.slot(),
                    state.updated.sourceLoadOrder(),
                    state.updated.type(),
                    state.updated.x(),
                    state.updated.y(),
                    state.status,
                    state.updated.spriteDefinition(),
                    state.variant,
                    state.renderFlipAttribute,
                    state.updated.spriteTileOffset(),
                    state.updated.z(),
                    -1,
                    false);
        }

        return true;
    }
}
