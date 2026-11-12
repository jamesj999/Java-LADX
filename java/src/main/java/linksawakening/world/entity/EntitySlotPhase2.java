package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** Ordered slot dispatch phase 2; false preserves the ROM handler early return. */
final class EntitySlotPhase2 {
    static boolean run(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_2_BEFORE_STATUS_BRANCHES, state)) return false;

        if (slotState.status() == EntityStatus.LIFTED) {
            slotState.lifted(state.runtime().liftThrow.renderLiftedEntity(slotState.entity(), frameInput.frame()));
            // Side-view pot pickup enters status LIFTED with the ROM's
            // transition countdown set to $02. UpdateEntityTimers has
            // already decremented it for this frame; EntityLiftedHandler
            // does not advance the pot until that countdown reaches zero.
            // Other state.lifted entities retain their established generic
            // presentation timing here.
            if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_2_LIFTED, state)) return false;
            slotState.liftedRomDirection(frameInput.romLinkDirection());
            if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_2_LIFTED_FOLLOWER, state)) return false;
            state.runtime().slots[slotState.index()] = state.runtime()
                    .liftThrow
                    .advanceLiftedEntity(
                            slotState.lifted(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.linkZ(),
                            slotState.liftedRomDirection());
            return false;
        } else if (slotState.status() == EntityStatus.THROWN) {
            if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_2_THROWN, state)) return false;
            RoomEntity thrown = state.runtime()
                    .liftThrow
                    .advanceThrownEntity(
                            slotState.entity(),
                            frameInput.backgroundCollision(),
                            state.runtime().entityState.physicsTables.readGroundInteractionSideScrolling(),
                            frameInput.romLinkDirection(),
                            frameInput.frame());
            state.runtime().slots[slotState.index()] = thrown;
            if (thrown.status() == EntityStatus.THROWN) {
                state.runtime().liftThrow.collideThrownEntityWithEntities(thrown, frameInput.frame());
            }
            return false;
        } else if (slotState.status() == EntityStatus.FALLING) {
            if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_2_FALLING, state)) return false;
            RoomEntity falling = state.runtime().backgroundMechanics.advanceFallingEntity(slotState.entity());
            if (falling == null) {
                state.runtime()
                        .lifecycle
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            int renderFlipAttribute = state.runtime()
                    .entityState
                    .physicsTables
                    .readBaseEntityFlipAttribute(slotState.entity().slot());
            if (state.runtime()
                            .entityState
                            .combatTables
                            .readEnemyFlashCountdown(slotState.entity().slot())
                    > 0) {
                renderFlipAttribute ^= (state.runtime()
                                        .entityState
                                        .combatTables
                                        .readEnemyFlashCountdown(
                                                slotState.entity().slot())
                                << 2)
                        & 0x10;
            }
            state.runtime().slots[slotState.index()] = new RoomEntity(
                    falling.slot(),
                    falling.sourceLoadOrder(),
                    falling.type(),
                    falling.x(),
                    falling.y(),
                    EntityStatus.FALLING,
                    falling.spriteDefinition(),
                    falling.spriteVariant(),
                    renderFlipAttribute,
                    falling.spriteTileOffset(),
                    falling.z(),
                    -1,
                    false);
            return false;
        } else if (slotState.status() == EntityStatus.DYING) {
            if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_2_DYING, state)) return false;
            if (isBossKeyDropProducer(slotState.entity().type())) {
                state.runtime()
                        .bossRewards
                        .advanceBossKeyDropProducer(slotState.entity(), frameInput.randomByteSupplier());
                return false;
            }
            if (state.runtime()
                            .entityState
                            .combatTables
                            .readDyingCountdown(slotState.entity().slot())
                    == 0) {
                state.runtime()
                        .bossRewards
                        .handleTerminalEnemyDeath(slotState.entity(), frameInput.randomByteSupplier());
            } else {
                int slot = slotState.entity().slot();
                int presentationCountdown =
                        state.runtime().entityState.combatTables.readDyingCountdown(slot);
                // EntityDeathHandler ($03:557D): the initial counter is a
                // ceiling, not a fixed wait. Begin destruction once the
                // hit recoil has expired; bosses use their handlers above.
                if (presentationCountdown >= 0x20
                        && state.runtime().entityState.combatTables.readEnemyIgnoreHitsCountdown(slot) == 0) {
                    state.runtime().entityState.combatTables.writeDyingCountdown(slot, 0x1F);
                    state.runtime()
                            .entityState
                            .events
                            .pendingEntityEvents
                            .add(new EntityCombatEvent(
                                    slot,
                                    slotState.entity().type(),
                                    0,
                                    false,
                                    EntityCombatEvent.SoundChannel.NOISE,
                                    0x13));
                }
                RoomEntity updated = state.runtime().entityState.combatTables.readEnemyIgnoreHitsCountdown(slot) > 0
                        ? state.runtime()
                                .backgroundMechanics
                                .applyEnemyRecoilIfNeeded(slotState.entity(), frameInput.backgroundCollision())
                                .entity()
                        : slotState.entity();
                updated = state.runtime().slotProperties.refreshColorShellDisplay(updated, slotState.status());
                state.runtime().slots[slotState.index()] = withDeathPresentation(
                        updated,
                        EntityMotionMath.deathSpriteVariantForCountdown(presentationCountdown),
                        state.runtime().entityState.combatTables.readPowerRecoilDeath(slot));
            }
            return false;
        } else if (slotState.status() == EntityStatus.BURNING) {
            if (state.runtime()
                            .entityState
                            .physicsTables
                            .readEnemyTransitionCountdown(slotState.entity().slot())
                    == 0) {
                state.runtime().lifecycle.finishBurning(slotState.entity());
            } else {
                state.runtime().slots[slotState.index()] =
                        state.runtime().slotProperties.refreshColorShellDisplay(slotState.entity(), slotState.status());
            }
            return false;
        } else if (slotState.status() == EntityStatus.STUNNED) {
            if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_2_STUNNED, state)) return false;
            if (state.runtime().entityState.linkState.powerBraceletButtonHeld
                    && isLiftableEntity(slotState.entity())
                    && (slotState.entity().type() == EntityRomConstants.ENTITY_BOMB
                            || (state.runtime()
                                                    .entityState
                                                    .physicsTables
                                                    .readEnemyPhysicsFlags(
                                                            slotState.entity().slot())
                                            & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE)
                                    != 0)
                    && RoomEntityPickupRules.overlapsLink(
                            slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY())) {
                if (state.runtime().beginLift(slotState.entity().slot(), frameInput.romLinkDirection())) {
                    state.runtime().slots[slotState.index()] = state.runtime()
                            .liftThrow
                            .advanceLiftedEntity(
                                    state.runtime()
                                            .liftThrow
                                            .renderLiftedBomb(state.runtime().slots[slotState.index()]),
                                    frameInput.linkEntityX(),
                                    frameInput.linkEntityY(),
                                    frameInput.linkZ(),
                                    frameInput.romLinkDirection());
                }
                return false;
            }
            if (state.runtime()
                            .entityState
                            .combatTables
                            .readEnemyStunnedCountdown(slotState.entity().slot())
                    == 0) {
                state.runtime().slots[slotState.index()] = state.runtime()
                        .slotProperties
                        .refreshColorShellDisplay(
                                withStatus(slotState.entity(), EntityStatus.ACTIVE), EntityStatus.ACTIVE);
            } else {
                state.runtime().slots[slotState.index()] =
                        state.runtime().slotProperties.refreshColorShellDisplay(slotState.entity(), slotState.status());
            }
            return false;
        } else if (slotState.status() == EntityStatus.INIT) {
            if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_2_BEFORE_INITIALIZE, state)) return false;
            if (!state.runtime().initializeBossBattle(slotState.entity())) return false;
            slotState.status(EntityStatus.ACTIVE);
            state.runtime().pickups.initializeEntityTimers(slotState.entity());
            state.runtime().entityHandlers.initialize(slotState.entity(), frameInput.randomByteSupplier());

            if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_2_AFTER_INITIALIZE, state)) return false;
        } else if (slotState.status() == EntityStatus.ACTIVE) {
            if (!timerGates.freezeTarinTimers() && !timerGates.freezeMoblinKingTimers()) {
                state.runtime()
                        .lifecycle
                        .decrementSlowTransitionCountdown(slotState.entity().slot(), frameInput.frame());
            }
            if (state.runtime().lifecycle.shouldDisappear(slotState.entity())) {
                if (state.runtime()
                                .entityState
                                .physicsTables
                                .readSlowTransitionCountdown(slotState.entity().slot())
                        == 0) {
                    state.runtime().clearEntity(slotState.entity().slot());
                    return false;
                }
            }
        }
        slotState.updated(slotState.entity());
        if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_2_BEFORE_RECOIL, state)) return false;
        if (presentationFlags.preserveGhiniPresentation()) {
            slotState.updated(withVariant(slotState.entity(), -1));
        }
        if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION, state))
            return false;
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && (usesSharedRecoil(slotState.entity().type())
                        || (slotState.entity().type() == EntityRomConstants.ENTITY_POKEY
                                && state.runtime()
                                        .entityState
                                        .motions
                                        .get(PokeyMotion.class, PokeyMotion::new)
                                        .isSegment(slotState.entity().slot())
                                && timerGates.handlerLinkCollisionEnabled()))
                && slotState.entity().type() != EntityRomConstants.ENTITY_ROLLING_BONES
                && slotState.entity().type() != EntityRomConstants.ENTITY_MINI_MOLDORM
                && slotState.entity().type() != EntityRomConstants.ENTITY_BOO_BUDDY
                && (slotState.entity().type() != EntityRomConstants.ENTITY_MASKED_MIMIC_GORIYA
                        || state.runtime().entityState.roomVariables.readEntityMapId() != 0x1F)
                && ((slotState.entity().type() != EntityRomConstants.ENTITY_STAR
                                && slotState.entity().type() != EntityRomConstants.ENTITY_BLOOPER)
                        || timerGates.handlerLinkCollisionEnabled())
                && ((slotState.entity().type() != EntityRomConstants.ENTITY_ZOMBIE
                                && slotState.entity().type() != EntityRomConstants.ENTITY_BUZZ_BLOB
                                && slotState.entity().type() != EntityRomConstants.ENTITY_SAND_CRAB)
                        || timerGates.handlerLinkCollisionEnabled())) {
            // Bank-$03 AnimateRoamingEnemy and the bank-$04/$06/$07
            // handlers apply the shared recoil before their own movement.
            EnemyRecoilMotion.Update recoil = state.runtime()
                    .backgroundMechanics
                    .applyEnemyRecoilIfNeeded(slotState.entity(), frameInput.backgroundCollision());
            slotState.entity(recoil.entity());
            slotState.updated(slotState.entity());
        }
        if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION, state)) return false;
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && state.runtime().entityHandlers.hasAdvance(slotState.entity().type())) {
            slotState.updated(state.runtime()
                    .entityHandlers
                    .advance(
                            slotState.entity(),
                            state.context()
                                    .withRuntimeInputs(
                                            frameInput.frame(),
                                            frameInput.linkEntityX(),
                                            frameInput.linkEntityY(),
                                            frameInput.backgroundCollision())));
            if (slotState.entity().type() == EntityRomConstants.ENTITY_LEEVER
                    && timerGates.handlerInteractive()
                    && timerGates.handlerLinkCollisionEnabled()) {
                state.runtime().finishLeeverContactRecoil(slotState.updated());
            }
        }

        if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_2_AFTER_LEEVER_CONTACT, state)) return false;
        return true;
    }
}
