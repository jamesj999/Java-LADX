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

        if (!state.handlers().tick(EntityTickStage.PHASE_5_BEFORE_COMMIT, state)) return false;
        RoomEntity beforeDropPhysics = slotState.updated();
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !slotState.keyDropTransitionActive()
                && state.bossRewards().readEnemyDropActive(slotState.updated().slot())) {
            slotState.updated(state.motion(EnemyDropMotion.class, EnemyDropMotion::new)
                    .advance(
                            slotState.updated(),
                            frameInput.frame(),
                            state.physicsTables()
                                    .readEntityGroundStatus(slotState.updated().slot()),
                            state.physicsTables().readGroundInteractionSideScrolling()));
        }
        if (!applyGroundInteraction(state, beforeDropPhysics)) return false;
        if (!applyFinalPresentation(state)) return false;

        return true;
    }

    private static void bounceDroppedItem(
            EntityFrameExecution state, RoomEntity beforeDropPhysics, RoomEntityGroundInteraction.Result groundResult) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        if (slotState.status() == EntityStatus.ACTIVE
                && state.bossRewards().readEnemyDropActive(slotState.updated().slot())) {
            // BouncingEntityPhysics calls the complete background helper before
            // bouncing. Ground effects alone omit the neighbouring bush walls.
            RoomEntityBackgroundInteraction dropBackground =
                    state.physicsTables().readBackgroundInteraction();
            if (dropBackground == null && frameInput.backgroundCollision() != null) {
                dropBackground = RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
            }
            slotState.updated(state.motion(EnemyDropMotion.class, EnemyDropMotion::new)
                    .resolveWalls(
                            beforeDropPhysics,
                            slotState.updated(),
                            dropBackground,
                            state.combatTables()
                                    .readEnemyIgnoreHitsCountdown(
                                            slotState.updated().slot()),
                            frameInput.frame()));
            boolean dropGroundCollision = state.physicsTables().readGroundInteractionSideScrolling()
                    && state.motion(EnemyDropMotion.class, EnemyDropMotion::new)
                                    .speedY(slotState.updated().slot())
                            != 0
                    && (state.motion(EnemyDropMotion.class, EnemyDropMotion::new)
                                            .speedY(slotState.updated().slot())
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
            slotState.updated(state.motion(EnemyDropMotion.class, EnemyDropMotion::new)
                    .bounce(
                            slotState.updated(),
                            groundResult.groundStatus(),
                            state.physicsTables().readGroundInteractionSideScrolling(),
                            dropGroundCollision));
        }
    }

    private static boolean applyGroundInteraction(EntityFrameExecution state, RoomEntity beforeDropPhysics) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !slotState.keyDropTransitionActive()
                && slotState.applyGenericGroundInteraction()
                && !state.background().hasNoGroundInteraction(slotState.updated())
                && (timerGates.handlerLinkCollisionEnabled()
                        || slotState.updated().type() != EntityRomConstants.ENTITY_BLOOPER)) {
            // ApplyEntityInteractionWithBackground runs after each ROM
            // entity handler's movement and before the final display-list
            // presentation. The room session supplies terrain physics;
            // direct runtime users retain the no-op default.
            int previousGroundStatus = state.physicsTables()
                    .readEntityGroundStatus(slotState.updated().slot());
            RoomEntityGroundInteraction.Result groundResult = slotState.preAppliedGroundResult() != null
                    ? slotState.preAppliedGroundResult()
                    : Objects.requireNonNull(
                            state.physicsTables()
                                    .readGroundInteraction()
                                    .apply(
                                            slotState.updated(),
                                            frameInput.frame(),
                                            previousGroundStatus,
                                            state.background().verticalSpeedZ(slotState.updated()),
                                            state.physicsTables().readGroundInteractionSideScrolling()),
                            "Room entity ground interaction returned null");
            slotState.updated(Objects.requireNonNull(
                    groundResult.entity(), "Room entity ground interaction returned a null entity"));
            state.physicsTables()
                    .writeEntityGroundStatus(slotState.updated().slot(), groundResult.groundStatus() & 0xFF);
            if (groundResult.pitTransition() != null
                    && state.background()
                            .beginFalling(
                                    slotState.updated(),
                                    groundResult.pitTransition(),
                                    timerGates.ignoreHitsDecrementedBeforeHandler())) {
                slotState.status(EntityStatus.FALLING);
            }
            if (groundResult.waterSplash()) {
                state.events()
                        .transientVfxRequests
                        .add(new TransientVfxRequest(
                                TransientVfxType.WATER_SPLASH,
                                slotState.updated().x(),
                                slotState.updated().y()));
                state.events()
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
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.updated().slot());
                return false;
            }
            bounceDroppedItem(state, beforeDropPhysics, groundResult);
        }
        return true;
    }

    private static boolean applyFinalPresentation(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();
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
            state.background().requestLinkPush(slotState.updated(), slotState.genericLinkPushPolicy());
        }
        if (ColorShellMotion.isColorShellType(slotState.updated().type())) {
            slotState.updated(state.slotProperties().refreshColorShellDisplay(slotState.updated(), slotState.status()));
        }
        if (slotState.status() == EntityStatus.ACTIVE && isRuntimeFloatingItem(slotState.updated())) {
            slotState.updated(withZ(
                    slotState.updated(),
                    FloatingItemMotion.zForFrame(
                            state.physicsTables().readGroundInteractionSideScrolling(), frameInput.frame())));
        }
        slotState.variant(
                presentationFlags.preservesHandlerPresentation()
                        ? slotState.updated().spriteVariant()
                        : EntityTypeRules.variantFor(slotState.updated(), frameInput.frame()));
        if (slotState.status() == EntityStatus.ACTIVE && state.lifecycle().shouldDisappear(slotState.entity())) {
            slotState.variant(
                    (state.physicsTables()
                                                    .readSlowTransitionCountdown(
                                                            slotState.entity().slot())
                                            & 0x01)
                                    != 0
                            ? 0
                            : -1);
        }
        slotState.renderFlipAttribute(
                presentationFlags.preservesHandlerPresentation()
                        ? slotState.updated().entityFlipAttribute()
                        : state.physicsTables()
                                .readBaseEntityFlipAttribute(slotState.entity().slot()));
        if (presentationFlags.preserveBombitePresentation()
                && slotState.updated().type() == EntityRomConstants.ENTITY_TIMER_BOMBITE) {
            slotState.renderFlipAttribute(slotState.renderFlipAttribute()
                    | ((state.bombInteractions()
                                            .readBombPrivateCountdown1(
                                                    slotState.updated().slot())
                                    << 3)
                            & 0x10));
        }
        if (state.combatTables().readEnemyFlashCountdown(slotState.entity().slot()) > 0) {
            slotState.renderFlipAttribute(slotState.renderFlipAttribute()
                    ^ ((state.combatTables()
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
