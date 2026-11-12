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

        if (!state.handlers().tick(EntityTickStage.PHASE_2_BEFORE_STATUS_BRANCHES, state)) return false;

        if (!advanceStatus(state)) return false;
        slotState.updated(slotState.entity());
        if (!state.handlers().tick(EntityTickStage.PHASE_2_BEFORE_RECOIL, state)) return false;
        if (presentationFlags.preserveGhiniPresentation()) {
            slotState.updated(withVariant(slotState.entity(), -1));
        }
        if (!state.handlers().tick(EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION, state)) return false;
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && (usesSharedRecoil(slotState.entity().type())
                        || (slotState.entity().type() == EntityRomConstants.ENTITY_POKEY
                                && state.motion(PokeyMotion.class, PokeyMotion::new)
                                        .isSegment(slotState.entity().slot())
                                && timerGates.handlerLinkCollisionEnabled()))
                && slotState.entity().type() != EntityRomConstants.ENTITY_ROLLING_BONES
                && slotState.entity().type() != EntityRomConstants.ENTITY_MINI_MOLDORM
                && slotState.entity().type() != EntityRomConstants.ENTITY_BOO_BUDDY
                && (slotState.entity().type() != EntityRomConstants.ENTITY_MASKED_MIMIC_GORIYA
                        || state.roomVariables().readEntityMapId() != 0x1F)
                && ((slotState.entity().type() != EntityRomConstants.ENTITY_STAR
                                && slotState.entity().type() != EntityRomConstants.ENTITY_BLOOPER)
                        || timerGates.handlerLinkCollisionEnabled())
                && ((slotState.entity().type() != EntityRomConstants.ENTITY_ZOMBIE
                                && slotState.entity().type() != EntityRomConstants.ENTITY_BUZZ_BLOB
                                && slotState.entity().type() != EntityRomConstants.ENTITY_SAND_CRAB)
                        || timerGates.handlerLinkCollisionEnabled())) {
            // Bank-$03 AnimateRoamingEnemy and the bank-$04/$06/$07
            // handlers apply the shared recoil before their own movement.
            EnemyRecoilMotion.Update recoil =
                    state.background().applyEnemyRecoilIfNeeded(slotState.entity(), frameInput.backgroundCollision());
            slotState.entity(recoil.entity());
            slotState.updated(slotState.entity());
        }
        if (!state.handlers().tick(EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION, state)) return false;
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && state.handlers().hasAdvance(slotState.entity().type())) {
            slotState.updated(state.handlers()
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

        if (!state.handlers().tick(EntityTickStage.PHASE_2_AFTER_LEEVER_CONTACT, state)) return false;
        return true;
    }

    private static boolean advanceLifted(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        slotState.lifted(state.liftThrow().renderLiftedEntity(slotState.entity(), frameInput.frame()));
        // Side-view pot pickup enters status LIFTED with the ROM's
        // transition countdown set to $02. UpdateEntityTimers has
        // already decremented it for this frame; EntityLiftedHandler
        // does not advance the pot until that countdown reaches zero.
        // Other state.lifted entities retain their established generic
        // presentation timing here.
        if (!state.handlers().tick(EntityTickStage.PHASE_2_LIFTED, state)) return false;
        slotState.liftedRomDirection(frameInput.romLinkDirection());
        if (!state.handlers().tick(EntityTickStage.PHASE_2_LIFTED_FOLLOWER, state)) return false;
        state.runtime().slots[slotState.index()] = state.liftThrow()
                .advanceLiftedEntity(
                        slotState.lifted(),
                        frameInput.linkEntityX(),
                        frameInput.linkEntityY(),
                        frameInput.linkZ(),
                        slotState.liftedRomDirection());
        return false;
    }

    private static boolean advanceThrown(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!state.handlers().tick(EntityTickStage.PHASE_2_THROWN, state)) return false;
        RoomEntity thrown = state.liftThrow()
                .advanceThrownEntity(
                        slotState.entity(),
                        frameInput.backgroundCollision(),
                        state.physicsTables().readGroundInteractionSideScrolling(),
                        frameInput.romLinkDirection(),
                        frameInput.frame());
        state.runtime().slots[slotState.index()] = thrown;
        if (thrown.status() == EntityStatus.THROWN) {
            state.liftThrow().collideThrownEntityWithEntities(thrown, frameInput.frame());
        }
        return false;
    }

    private static boolean advanceFalling(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (!state.handlers().tick(EntityTickStage.PHASE_2_FALLING, state)) return false;
        RoomEntity falling = state.background().advanceFallingEntity(slotState.entity());
        if (falling == null) {
            state.lifecycle().disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }
        int renderFlipAttribute = state.physicsTables()
                .readBaseEntityFlipAttribute(slotState.entity().slot());
        if (state.combatTables().readEnemyFlashCountdown(slotState.entity().slot()) > 0) {
            renderFlipAttribute ^= (state.combatTables()
                                    .readEnemyFlashCountdown(slotState.entity().slot())
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
    }

    private static boolean advanceDying(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!state.handlers().tick(EntityTickStage.PHASE_2_DYING, state)) return false;
        if (isBossKeyDropProducer(slotState.entity().type())) {
            state.bossRewards().advanceBossKeyDropProducer(slotState.entity(), frameInput.randomByteSupplier());
            return false;
        }
        if (state.combatTables().readDyingCountdown(slotState.entity().slot()) == 0) {
            state.bossRewards().handleTerminalEnemyDeath(slotState.entity(), frameInput.randomByteSupplier());
        } else {
            int slot = slotState.entity().slot();
            int presentationCountdown = state.combatTables().readDyingCountdown(slot);
            // EntityDeathHandler ($03:557D): the initial counter is a
            // ceiling, not a fixed wait. Begin destruction once the
            // hit recoil has expired; bosses use their handlers above.
            if (presentationCountdown >= 0x20 && state.combatTables().readEnemyIgnoreHitsCountdown(slot) == 0) {
                state.combatTables().writeDyingCountdown(slot, 0x1F);
                state.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot, slotState.entity().type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
            }
            RoomEntity updated = state.combatTables().readEnemyIgnoreHitsCountdown(slot) > 0
                    ? state.background()
                            .applyEnemyRecoilIfNeeded(slotState.entity(), frameInput.backgroundCollision())
                            .entity()
                    : slotState.entity();
            updated = state.slotProperties().refreshColorShellDisplay(updated, slotState.status());
            state.runtime().slots[slotState.index()] = withDeathPresentation(
                    updated,
                    EntityMotionMath.deathSpriteVariantForCountdown(presentationCountdown),
                    state.combatTables().readPowerRecoilDeath(slot));
        }
        return false;
    }

    private static boolean advanceBurning(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (state.physicsTables()
                        .readEnemyTransitionCountdown(slotState.entity().slot())
                == 0) {
            state.lifecycle().finishBurning(slotState.entity());
        } else {
            state.runtime().slots[slotState.index()] =
                    state.slotProperties().refreshColorShellDisplay(slotState.entity(), slotState.status());
        }
        return false;
    }

    private static boolean advanceStunned(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!state.handlers().tick(EntityTickStage.PHASE_2_STUNNED, state)) return false;
        if (state.linkState().powerBraceletButtonHeld
                && isLiftableEntity(slotState.entity())
                && (slotState.entity().type() == EntityRomConstants.ENTITY_BOMB
                        || (state.physicsTables()
                                                .readEnemyPhysicsFlags(
                                                        slotState.entity().slot())
                                        & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE)
                                != 0)
                && RoomEntityPickupRules.overlapsLink(
                        slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY())) {
            if (state.runtime().beginLift(slotState.entity().slot(), frameInput.romLinkDirection())) {
                state.runtime().slots[slotState.index()] = state.liftThrow()
                        .advanceLiftedEntity(
                                state.liftThrow().renderLiftedBomb(state.runtime().slots[slotState.index()]),
                                frameInput.linkEntityX(),
                                frameInput.linkEntityY(),
                                frameInput.linkZ(),
                                frameInput.romLinkDirection());
            }
            return false;
        }
        if (state.combatTables().readEnemyStunnedCountdown(slotState.entity().slot()) == 0) {
            state.runtime().slots[slotState.index()] = state.slotProperties()
                    .refreshColorShellDisplay(withStatus(slotState.entity(), EntityStatus.ACTIVE), EntityStatus.ACTIVE);
        } else {
            state.runtime().slots[slotState.index()] =
                    state.slotProperties().refreshColorShellDisplay(slotState.entity(), slotState.status());
        }
        return false;
    }

    private static boolean advanceInit(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!state.handlers().tick(EntityTickStage.PHASE_2_BEFORE_INITIALIZE, state)) return false;
        if (!state.runtime().initializeBossBattle(slotState.entity())) return false;
        slotState.status(EntityStatus.ACTIVE);
        state.pickups().initializeEntityTimers(slotState.entity());
        state.handlers().initialize(slotState.entity(), frameInput.randomByteSupplier());

        if (!state.handlers().tick(EntityTickStage.PHASE_2_AFTER_INITIALIZE, state)) return false;

        return true;
    }

    private static boolean advanceActive(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (!timerGates.freezeTarinTimers() && !timerGates.freezeMoblinKingTimers()) {
            state.lifecycle()
                    .decrementSlowTransitionCountdown(slotState.entity().slot(), frameInput.frame());
        }
        if (state.lifecycle().shouldDisappear(slotState.entity())) {
            if (state.physicsTables()
                            .readSlowTransitionCountdown(slotState.entity().slot())
                    == 0) {
                state.runtime().clearEntity(slotState.entity().slot());
                return false;
            }
        }

        return true;
    }

    private static boolean advanceStatus(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        if (slotState.status() == EntityStatus.LIFTED) {
            return advanceLifted(state);
        } else if (slotState.status() == EntityStatus.THROWN) {
            return advanceThrown(state);
        } else if (slotState.status() == EntityStatus.FALLING) {
            return advanceFalling(state);
        } else if (slotState.status() == EntityStatus.DYING) {
            return advanceDying(state);
        } else if (slotState.status() == EntityStatus.BURNING) {
            return advanceBurning(state);
        } else if (slotState.status() == EntityStatus.STUNNED) {
            return advanceStunned(state);
        } else if (slotState.status() == EntityStatus.INIT) {
            return advanceInit(state);
        } else if (slotState.status() == EntityStatus.ACTIVE) {
            return advanceActive(state);
        }
        return true;
    }
}
