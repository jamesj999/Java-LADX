package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.Objects;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.FloatingItemMotion;

/** Ordered slot dispatch phase 5; false preserves the ROM handler early return. */
final class EntitySlotPhase5 {
    static boolean run(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_5_BEFORE_COMMIT, state)) return false;
        RoomEntity beforeDropPhysics = slotState.updated();
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !slotState.keyDropTransitionActive()
                && state.runtime()
                        .bossRewards
                        .readEnemyDropActive(slotState.updated().slot())) {
            slotState.updated(state.runtime()
                    .entityState
                    .motions
                    .get(EnemyDropMotion.class, EnemyDropMotion::new)
                    .advance(
                            slotState.updated(),
                            frameInput.frame(),
                            state.runtime()
                                    .entityState
                                    .physicsTables
                                    .readEntityGroundStatus(slotState.updated().slot()),
                            state.runtime().entityState.physicsTables.readGroundInteractionSideScrolling()));
        }
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !slotState.keyDropTransitionActive()
                && slotState.applyGenericGroundInteraction()
                && !state.runtime().backgroundMechanics.hasNoGroundInteraction(slotState.updated())
                && (timerGates.handlerLinkCollisionEnabled()
                        || slotState.updated().type() != EntityRomConstants.ENTITY_BLOOPER)) {
            // ApplyEntityInteractionWithBackground runs after each ROM
            // entity handler's movement and before the final display-list
            // presentation. The room session supplies terrain physics;
            // direct runtime users retain the no-op default.
            int previousGroundStatus = state.runtime()
                    .entityState
                    .physicsTables
                    .readEntityGroundStatus(slotState.updated().slot());
            RoomEntityGroundInteraction.Result groundResult = slotState.preAppliedGroundResult() != null
                    ? slotState.preAppliedGroundResult()
                    : Objects.requireNonNull(
                            state.runtime()
                                    .entityState
                                    .physicsTables
                                    .readGroundInteraction()
                                    .apply(
                                            slotState.updated(),
                                            frameInput.frame(),
                                            previousGroundStatus,
                                            state.runtime().backgroundMechanics.verticalSpeedZ(slotState.updated()),
                                            state.runtime()
                                                    .entityState
                                                    .physicsTables
                                                    .readGroundInteractionSideScrolling()),
                            "Room entity ground interaction returned null");
            slotState.updated(Objects.requireNonNull(
                    groundResult.entity(), "Room entity ground interaction returned a null entity"));
            state.runtime()
                    .entityState
                    .physicsTables
                    .writeEntityGroundStatus(slotState.updated().slot(), groundResult.groundStatus() & 0xFF);
            if (groundResult.pitTransition() != null
                    && state.runtime()
                            .backgroundMechanics
                            .beginFalling(
                                    slotState.updated(),
                                    groundResult.pitTransition(),
                                    timerGates.ignoreHitsDecrementedBeforeHandler())) {
                slotState.status(EntityStatus.FALLING);
            }
            if (groundResult.waterSplash()) {
                state.runtime()
                        .entityState
                        .events
                        .transientVfxRequests
                        .add(new TransientVfxRequest(
                                TransientVfxType.WATER_SPLASH,
                                slotState.updated().x(),
                                slotState.updated().y()));
                state.runtime()
                        .entityState
                        .events
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slotState.updated().slot(),
                                slotState.updated().type(),
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.JINGLE,
                                0x0E));
            }
            if (groundResult.unloaded()) {
                state.runtime()
                        .lifecycle
                        .disableEntityWithoutPersistence(slotState.updated().slot());
                return false;
            }
            if (slotState.status() == EntityStatus.ACTIVE
                    && state.runtime()
                            .bossRewards
                            .readEnemyDropActive(slotState.updated().slot())) {
                // BouncingEntityPhysics calls the complete background helper before
                // bouncing. Ground effects alone omit the neighbouring bush walls.
                RoomEntityBackgroundInteraction dropBackground =
                        state.runtime().entityState.physicsTables.readBackgroundInteraction();
                if (dropBackground == null && frameInput.backgroundCollision() != null) {
                    dropBackground = RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
                }
                slotState.updated(state.runtime()
                        .entityState
                        .motions
                        .get(EnemyDropMotion.class, EnemyDropMotion::new)
                        .resolveWalls(
                                beforeDropPhysics,
                                slotState.updated(),
                                dropBackground,
                                state.runtime()
                                        .entityState
                                        .combatTables
                                        .readEnemyIgnoreHitsCountdown(
                                                slotState.updated().slot()),
                                frameInput.frame()));
                boolean dropGroundCollision =
                        state.runtime().entityState.physicsTables.readGroundInteractionSideScrolling()
                                && state.runtime()
                                                .entityState
                                                .motions
                                                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                                                .speedY(slotState.updated().slot())
                                        != 0
                                && (state.runtime()
                                                        .entityState
                                                        .motions
                                                        .get(EnemyDropMotion.class, EnemyDropMotion::new)
                                                        .speedY(slotState
                                                                .updated()
                                                                .slot())
                                                & 0x80)
                                        == 0
                                && frameInput.backgroundCollision() != null
                                && frameInput
                                        .backgroundCollision()
                                        .blocks(
                                                slotState.updated(),
                                                EntityBackgroundCollisionResult.DOWN,
                                                slotState.updated().x(),
                                                slotState.updated().y());
                slotState.updated(state.runtime()
                        .entityState
                        .motions
                        .get(EnemyDropMotion.class, EnemyDropMotion::new)
                        .bounce(
                                slotState.updated(),
                                groundResult.groundStatus(),
                                state.runtime().entityState.physicsTables.readGroundInteractionSideScrolling(),
                                dropGroundCollision));
            }
        }
        slotState.genericLinkPushPolicy(
                EntityLinkCollisionRules.genericPolicyFor(slotState.updated().type()));
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()
                && slotState.updated().type() != EntityRomConstants.ENTITY_GENIE
                && slotState.genericLinkPushPolicy().restoreFinalPosition()
                && RoomEntityCombatRules.overlapsLink(
                        slotState.updated(), frameInput.linkEntityX(), frameInput.linkEntityY())) {
            // The shared PushLinkOutOfEntity_* helpers run after their
            // handler-specific work. Restore the pre-entity final
            // position and carry the helper's other side effects across
            // the room/runtime boundary.
            state.runtime().backgroundMechanics.requestLinkPush(slotState.updated(), slotState.genericLinkPushPolicy());
        }
        if (ColorShellMotion.isColorShellType(slotState.updated().type())) {
            slotState.updated(
                    state.runtime().slotProperties.refreshColorShellDisplay(slotState.updated(), slotState.status()));
        }
        if (slotState.status() == EntityStatus.ACTIVE && isRuntimeFloatingItem(slotState.updated())) {
            slotState.updated(withZ(
                    slotState.updated(),
                    FloatingItemMotion.zForFrame(
                            state.runtime().entityState.physicsTables.readGroundInteractionSideScrolling(),
                            frameInput.frame())));
        }
        slotState.variant(
                presentationFlags.preserveGhiniPresentation()
                                || presentationFlags.preserveMadBomberPresentation()
                                || presentationFlags.preserveBombitePresentation()
                                || presentationFlags.preserveIronMaskPresentation()
                                || presentationFlags.preserveSnakePresentation()
                                || presentationFlags.preserveStarPresentation()
                                || presentationFlags.preserveBlooperPresentation()
                                || presentationFlags.preserveWingedOctorokPresentation()
                                || presentationFlags.preservePincerPresentation()
                                || presentationFlags.preserveBushCrawlerPresentation()
                                || presentationFlags.preserveMimicPresentation()
                                || presentationFlags.preserveMaskedMimicPresentation()
                                || presentationFlags.preserveMiniMoldormPresentation()
                                || presentationFlags.preserveMoldormPresentation()
                                || presentationFlags.preserveCuccoPresentation()
                                || presentationFlags.preserveGopongaFlowerPresentation()
                                || presentationFlags.preserveGiantGopongaPresentation()
                                || presentationFlags.preserveGopongaProjectilePresentation()
                                || presentationFlags.preservePokeyPresentation()
                                || presentationFlags.preservePiranhaPresentation()
                                || presentationFlags.preserveZoraPresentation()
                                || presentationFlags.preserveZombiePresentation()
                                || presentationFlags.preserveBuzzBlobPresentation()
                                || presentationFlags.preserveSandCrabPresentation()
                                || presentationFlags.preserveDogPresentation()
                                || presentationFlags.preserveUrchinPresentation()
                                || presentationFlags.preserveWitchRatPresentation()
                                || presentationFlags.preserveWitchPresentation()
                                || presentationFlags.preserveRoosterPresentation()
                                || presentationFlags.preserveWizrobePresentation()
                                || presentationFlags.preserveWizrobeProjectilePresentation()
                                || presentationFlags.preservePolsVoicePresentation()
                                || presentationFlags.preserveSpikedBeetlePresentation()
                                || presentationFlags.preserveArmosKnightPresentation()
                                || presentationFlags.preserveHinoxPresentation()
                                || presentationFlags.preserveRollingBonesPresentation()
                                || presentationFlags.preserveThreeOfAKindPresentation()
                                || presentationFlags.preserveMoblinKingPresentation()
                                || presentationFlags.preserveSecretSeashellPresentation()
                                || presentationFlags.preserveDroppablePresentation()
                                || presentationFlags.preserveTarinPresentation()
                        ? slotState.updated().spriteVariant()
                        : EntityTypeRules.variantFor(slotState.updated(), frameInput.frame()));
        if (slotState.status() == EntityStatus.ACTIVE
                && state.runtime().lifecycle.shouldDisappear(slotState.entity())) {
            slotState.variant(
                    (state.runtime()
                                                    .entityState
                                                    .physicsTables
                                                    .readSlowTransitionCountdown(
                                                            slotState.entity().slot())
                                            & 0x01)
                                    != 0
                            ? 0
                            : -1);
        }
        slotState.renderFlipAttribute(
                presentationFlags.preserveGhiniPresentation()
                                || presentationFlags.preserveMadBomberPresentation()
                                || presentationFlags.preserveBombitePresentation()
                                || presentationFlags.preserveIronMaskPresentation()
                                || presentationFlags.preserveSnakePresentation()
                                || presentationFlags.preserveStarPresentation()
                                || presentationFlags.preserveBlooperPresentation()
                                || presentationFlags.preserveWingedOctorokPresentation()
                                || presentationFlags.preservePincerPresentation()
                                || presentationFlags.preserveBushCrawlerPresentation()
                                || presentationFlags.preserveMimicPresentation()
                                || presentationFlags.preserveMaskedMimicPresentation()
                                || presentationFlags.preserveMiniMoldormPresentation()
                                || presentationFlags.preserveMoldormPresentation()
                                || presentationFlags.preserveCuccoPresentation()
                                || presentationFlags.preserveGopongaFlowerPresentation()
                                || presentationFlags.preserveGiantGopongaPresentation()
                                || presentationFlags.preserveGopongaProjectilePresentation()
                                || presentationFlags.preservePokeyPresentation()
                                || presentationFlags.preservePiranhaPresentation()
                                || presentationFlags.preserveZoraPresentation()
                                || presentationFlags.preserveZombiePresentation()
                                || presentationFlags.preserveBuzzBlobPresentation()
                                || presentationFlags.preserveSandCrabPresentation()
                                || presentationFlags.preserveDogPresentation()
                                || presentationFlags.preserveUrchinPresentation()
                                || presentationFlags.preserveWitchRatPresentation()
                                || presentationFlags.preserveWitchPresentation()
                                || presentationFlags.preserveRoosterPresentation()
                                || presentationFlags.preserveWizrobePresentation()
                                || presentationFlags.preserveWizrobeProjectilePresentation()
                                || presentationFlags.preservePolsVoicePresentation()
                                || presentationFlags.preserveSpikedBeetlePresentation()
                                || presentationFlags.preserveArmosKnightPresentation()
                                || presentationFlags.preserveHinoxPresentation()
                                || presentationFlags.preserveRollingBonesPresentation()
                                || presentationFlags.preserveThreeOfAKindPresentation()
                                || presentationFlags.preserveMoblinKingPresentation()
                                || presentationFlags.preserveSecretSeashellPresentation()
                                || presentationFlags.preserveDroppablePresentation()
                                || presentationFlags.preserveTarinPresentation()
                        ? slotState.updated().entityFlipAttribute()
                        : state.runtime()
                                .entityState
                                .physicsTables
                                .readBaseEntityFlipAttribute(slotState.entity().slot()));
        if (presentationFlags.preserveBombitePresentation()
                && slotState.updated().type() == EntityRomConstants.ENTITY_TIMER_BOMBITE) {
            slotState.renderFlipAttribute(slotState.renderFlipAttribute()
                    | ((state.runtime()
                                            .bombInteractions
                                            .readBombPrivateCountdown1(
                                                    slotState.updated().slot())
                                    << 3)
                            & 0x10));
        }
        if (state.runtime()
                        .entityState
                        .combatTables
                        .readEnemyFlashCountdown(slotState.entity().slot())
                > 0) {
            slotState.renderFlipAttribute(slotState.renderFlipAttribute()
                    ^ ((state.runtime()
                                            .entityState
                                            .combatTables
                                            .readEnemyFlashCountdown(
                                                    slotState.entity().slot())
                                    << 2)
                            & 0x10));
        }
        if (slotState.status() != slotState.entity().status()
                || slotState.variant() != slotState.entity().spriteVariant()
                || slotState.updated().type() != slotState.entity().type()
                || slotState.updated().spriteDefinition() != slotState.entity().spriteDefinition()
                || slotState.updated().x() != slotState.originalEntity().x()
                || slotState.updated().y() != slotState.originalEntity().y()
                || slotState.renderFlipAttribute() != slotState.originalEntity().entityFlipAttribute()
                || slotState.updated().z() != slotState.originalEntity().z()) {
            state.runtime().slots[slotState.index()] = new RoomEntity(
                    slotState.updated().slot(),
                    slotState.updated().sourceLoadOrder(),
                    slotState.updated().type(),
                    slotState.updated().x(),
                    slotState.updated().y(),
                    slotState.status(),
                    slotState.updated().spriteDefinition(),
                    slotState.variant(),
                    slotState.renderFlipAttribute(),
                    slotState.updated().spriteTileOffset(),
                    slotState.updated().z(),
                    -1,
                    false);
        }

        return true;
    }
}
