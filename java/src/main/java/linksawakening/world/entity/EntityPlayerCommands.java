package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LiftedEntityState;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BombMotion;

/** Player lift, throw, block movement and held-item presentation commands. */
final class EntityPlayerCommands {
    private final EntityPlayerCommandsServices runtime;
    // Marin's eight direction/walk variants repeat during the opening dialog.
    // Cache complete immutable values only while every source field still matches.
    private final RoomEntity[] marinPresentations = new RoomEntity[8];

    EntityPlayerCommands(EntityPlayerCommandsServices runtime) {
        this.runtime = runtime;
    }

    boolean beginLift(int slot, int romDirection) {
        EntitySlotValidation.validateEntitySlot(slot);
        EntitySlotValidation.validateRomDirection(romDirection);
        RoomEntity entity = runtime.slots()[slot];
        if (!entity.loaded()) {
            return false;
        }
        if (entity.type() == EntityRomConstants.ENTITY_BOMB && runtime.bombFinalPresentationPending(slot)) {
            return false;
        }
        if (runtime.liftThrow().readLiftedEntitySlot() >= 0
                && runtime.liftThrow().readLiftedEntitySlot() != slot
                && runtime.slots()[runtime.liftThrow().readLiftedEntitySlot()].status() == EntityStatus.LIFTED) {
            return false;
        }

        runtime.liftThrow().writeLiftedEntitySlot(slot);
        runtime.liftThrow().writeLiftedPhase(slot, 0);
        runtime.liftThrow().writeLiftedSourceDirection(slot, romDirection);
        runtime.liftThrow().writeLiftedStateInitialized(slot, true);
        runtime.liftThrow().writeLiftedCarryState(0);
        runtime.liftThrow().writeLiftedEffectiveDirection(romDirection);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x02);
        if (entity.type() == EntityRomConstants.ENTITY_BOMB) {
            if (runtime.bombInteractions().readPlacedBombMotionInitialized(slot)) {
                runtime.motions()
                        .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                        .clear(slot);
                runtime.bombInteractions().writePlacedBombMotionInitialized(slot, false);
            }
            runtime.setBombFinalPresentationPending(slot, false);
            runtime.combatTables().writeEnemyFlashCountdown(slot, 0);
            entity = withDefinition(
                    entity, runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB), 0);
        }
        runtime.slots()[slot] = withStatus(entity, EntityStatus.LIFTED);
        return true;
    }

    boolean throwLiftedEntity(int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        LiftedEntityState state = runtime.inspection().liftedEntityState();
        if (!state.active() || state.carryState() != 0x01) {
            return false;
        }

        int slot = state.slot();
        RoomEntity entity = runtime.slots()[slot];
        runtime.events().pendingPreTickLinkAttackStepRequests.add(new LinkAttackStepRequest(slot));
        runtime.events()
                .pendingPreTickEntityEvents
                .add(new EntityCombatEvent(
                        slot,
                        entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        EntityRomConstants.LIFTED_THROW_JINGLE_ID));
        if (entity.type() == EntityRomConstants.ENTITY_SIDE_VIEW_POT) {
            runtime.liftThrow().writeThrownDirection(slot, romDirection);
            runtime.motions()
                    .get(SideViewPotMotion.class, SideViewPotMotion::new)
                    .start(slot, romDirection);
            runtime.motions()
                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                    .clear(slot);
            runtime.liftThrow().writeThrownMotionInitialized(slot, false);
            runtime.liftThrow().writeLiftedStateInitialized(slot, false);
            runtime.liftThrow().writeLiftedPhase(slot, 0);
            runtime.liftThrow().writeLiftedCarryState(0);
            runtime.liftThrow().writeLiftedEffectiveDirection(0);
            runtime.liftThrow().writeLiftedEntitySlot(-1);
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0);
            runtime.slots()[slot] = withStatus(entity, EntityStatus.ACTIVE);
            return true;
        }
        runtime.liftThrow().writeThrownDirection(slot, romDirection);
        runtime.motions()
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .start(
                        slot,
                        romDirection,
                        entity.type(),
                        runtime.physicsTables().readGroundInteractionSideScrolling());
        runtime.liftThrow().writeThrownMotionInitialized(slot, true);
        runtime.liftThrow().writeLiftedStateInitialized(slot, false);
        runtime.liftThrow().writeLiftedPhase(slot, 0);
        runtime.liftThrow().writeLiftedCarryState(0);
        runtime.liftThrow().writeLiftedEffectiveDirection(0);
        runtime.liftThrow().writeLiftedEntitySlot(-1);
        runtime.physicsTables()
                .writeEnemyTransitionCountdown(
                        slot, entity.type() == EntityRomConstants.ENTITY_BOMB ? BombMotion.INITIAL_COUNTDOWN : 0);
        runtime.slots()[slot] = withStatus(entity, EntityStatus.THROWN);
        return true;
    }

    void movePushedBlock(int slot, int x, int y) {
        if (slot < 0
                || slot >= runtime.slots().length
                || runtime.slots()[slot].type() != EntityRomConstants.ENTITY_PUSHED_BLOCK
                || runtime.slots()[slot].status() == EntityStatus.DISABLED) {
            return;
        }
        RoomEntity entity = runtime.slots()[slot];
        runtime.slots()[slot] = new RoomEntity(
                slot,
                entity.sourceLoadOrder(),
                entity.type(),
                x & 0xFF,
                y & 0xFF,
                entity.status(),
                entity.spriteDefinition(),
                entity.spriteVariant(),
                entity.entityFlipAttribute(),
                entity.spriteTileOffset(),
                entity.z(),
                -1,
                false);
    }

    void removePushedBlock(int slot) {
        if (slot >= 0
                && slot < runtime.slots().length
                && runtime.slots()[slot].type() == EntityRomConstants.ENTITY_PUSHED_BLOCK) {
            runtime.slots()[slot] = RoomEntity.disabled(slot);
        }
    }

    boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        for (int slot = 0; slot < runtime.slots().length; slot++) {
            RoomEntity entity = runtime.slots()[slot];
            if (entity.loaded() && entity.type() == 0x3E) {
                runtime.slots()[slot] = marinPresentation(entity, x & 0xFF, y & 0xFF, spriteVariant);
                return true;
            }
        }
        return false;
    }

    private RoomEntity marinPresentation(RoomEntity entity, int x, int y, int variant) {
        if (entity.x() == x && entity.y() == y && entity.spriteVariant() == variant) return entity;
        RoomEntity cached = variant >= 0 && variant < marinPresentations.length ? marinPresentations[variant] : null;
        if (cached != null
                && cached.slot() == entity.slot()
                && cached.sourceLoadOrder() == entity.sourceLoadOrder()
                && cached.type() == entity.type()
                && cached.x() == x
                && cached.y() == y
                && cached.status() == entity.status()
                && cached.spriteDefinition() == entity.spriteDefinition()
                && cached.entityFlipAttribute() == entity.entityFlipAttribute()
                && cached.spriteTileOffset() == entity.spriteTileOffset()
                && cached.z() == entity.z()
                && cached.deathSpriteVariant() == entity.deathSpriteVariant()
                && cached.powerRecoilDeath() == entity.powerRecoilDeath()) return cached;
        RoomEntity value = EntitySnapshots.withMotion(
                entity,
                x,
                y,
                variant,
                entity.entityFlipAttribute(),
                entity.z(),
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
        if (variant >= 0 && variant < marinPresentations.length) marinPresentations[variant] = value;
        return value;
    }

    void updateHouseTarinPresentation(int linkX, int linkY) {
        for (int slot = 0; slot < runtime.slots().length; slot++) {
            RoomEntity entity = runtime.slots()[slot];
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_TARIN) {
                int dx = (byte) (linkX - entity.x());
                int dy = (byte) (linkY - entity.y());
                int direction = Math.abs(dy) >= Math.abs(dx) ? (dy < 0 ? 2 : 3) : (dx < 0 ? 1 : 0);
                runtime.slots()[slot] = withVariant(entity, direction);
            }
        }
    }

    void startSwordPickup(int slot) {
        runtime.rewards().writeSwordPickupSequenceActive(slot, true);
        runtime.rewards().writeSwordPickupState(slot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xA0);
        runtime.physicsTables().writeSlowTimerInitialized(slot, true);
        runtime.presentationState().writePendingMusicTrack(0x0F);
    }

    void startToadstoolPickup(int slot) {
        runtime.rewards().writeToadstoolPickupSequenceActive(slot, true);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x68);
        runtime.presentationState().writePendingMusicTrack(0x10);
    }

    void presentWitchGotItem(int slot) {
        runtime.events().pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events().pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
    }

    void holdSwordAboveLink(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, boolean finalPose) {
        int slot = entity.slot();
        runtime.events().pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events().pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        int x = finalPose ? (linkEntityX - 4) & 0xFF : linkEntityX;
        RoomEntity held = withPositionAndVariant(entity, x, (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        runtime.slots()[slot] = withZ(held, linkZ & 0xFF);
    }
}
