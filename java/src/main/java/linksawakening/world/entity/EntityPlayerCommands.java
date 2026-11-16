package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.withDefinition;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withVariant;
import static linksawakening.world.entity.EntitySnapshots.withZ;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LiftedEntityState;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.motion.BombMotion;
import linksawakening.world.entity.motion.SideViewPotMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;

/** Player lift, throw, block movement and held-item presentation commands. */
public final class EntityPlayerCommands {
    final EntityBombState bombInteractions;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLiftState liftThrow;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPresentationState presentationState;
    final EntityRewardState rewards;
    final EntityRoomInspection inspection;
    final EntitySlotProperties slotProperties;
    final EntitySlots slots;
    // Marin's eight direction/walk variants repeat during the opening dialog.
    // Cache complete immutable values only while every source field still matches.
    private static final RoomEntity UNREAD_PRESENTATION = RoomEntity.disabled(0);
    private final RoomEntity[] marinPresentations = new RoomEntity[8];

    {
        java.util.Arrays.fill(marinPresentations, UNREAD_PRESENTATION);
    }

    EntityPlayerCommands(
            EntityBombState bombInteractions,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityMotionRegistry motions,
            EntityPresentationState presentationState,
            EntityRewardState rewards,
            EntityRoomInspection inspection,
            EntitySlotProperties slotProperties,
            EntitySlots slots) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        this.bombInteractions = java.util.Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.rewards = java.util.Objects.requireNonNull(rewards, "rewards");
        this.inspection = java.util.Objects.requireNonNull(inspection, "inspection");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
    }

    public boolean beginLift(int slot, int romDirection) {
        EntitySlotValidation.validateEntitySlot(slot);
        EntitySlotValidation.validateRomDirection(romDirection);
        RoomEntity entity = this.slots.get(slot);
        if (!entity.loaded()) {
            return false;
        }
        if (entity.type() == EntityRomConstants.ENTITY_BOMB
                && this.bombInteractions.bombFinalPresentationPending(slot)) {
            return false;
        }
        if (this.liftThrow.readLiftedEntitySlot() >= 0
                && this.liftThrow.readLiftedEntitySlot() != slot
                && this.slots.get(this.liftThrow.readLiftedEntitySlot()).status() == EntityStatus.LIFTED) {
            return false;
        }

        this.liftThrow.writeLiftedEntitySlot(slot);
        this.liftThrow.writeLiftedPhase(slot, 0);
        this.liftThrow.writeLiftedSourceDirection(slot, romDirection);
        this.liftThrow.writeLiftedStateInitialized(slot, true);
        this.liftThrow.writeLiftedCarryState(0);
        this.liftThrow.writeLiftedEffectiveDirection(romDirection);
        this.physicsTables.writeEnemyTransitionCountdown(slot, 0x02);
        if (entity.type() == EntityRomConstants.ENTITY_BOMB) {
            if (this.bombInteractions.readPlacedBombMotionInitialized(slot)) {
                var thrownEntityMotion = this.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
                thrownEntityMotion.clear(slot);
                this.bombInteractions.writePlacedBombMotionInitialized(slot, false);
            }
            this.bombInteractions.setBombFinalPresentationPending(slot, false);
            this.combatTables.writeEnemyFlashCountdown(slot, 0);
            entity = withDefinition(entity, this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB), 0);
        }
        this.slots.replace(slot, withStatus(entity, EntityStatus.LIFTED));
        return true;
    }

    public boolean throwLiftedEntity(int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        LiftedEntityState state = this.inspection.liftedEntityState();
        if (!state.active() || state.carryState() != 0x01) {
            return false;
        }

        int slot = state.slot();
        RoomEntity entity = this.slots.get(slot);
        this.events.preTickLinkAttackStep(new LinkAttackStepRequest(slot));
        this.events.preTickCombat(new EntityCombatEvent(
                slot,
                entity.type(),
                0,
                false,
                EntityCombatEvent.SoundChannel.JINGLE,
                EntityRomConstants.LIFTED_THROW_JINGLE_ID));
        if (entity.type() == EntityRomConstants.ENTITY_SIDE_VIEW_POT) {
            this.liftThrow.writeThrownDirection(slot, romDirection);
            var sideViewPotMotion = this.motions.get(SideViewPotMotion.class, SideViewPotMotion::new);
            sideViewPotMotion.start(slot, romDirection);
            var thrownEntityMotion = this.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
            thrownEntityMotion.clear(slot);
            this.liftThrow.writeThrownMotionInitialized(slot, false);
            this.liftThrow.writeLiftedStateInitialized(slot, false);
            this.liftThrow.writeLiftedPhase(slot, 0);
            this.liftThrow.writeLiftedCarryState(0);
            this.liftThrow.writeLiftedEffectiveDirection(0);
            this.liftThrow.writeLiftedEntitySlot(-1);
            this.physicsTables.writeEnemyTransitionCountdown(slot, 0);
            this.slots.replace(slot, withStatus(entity, EntityStatus.ACTIVE));
            return true;
        }
        this.liftThrow.writeThrownDirection(slot, romDirection);
        var thrownEntityMotion = this.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
        thrownEntityMotion.start(
                slot, romDirection, entity.type(), this.physicsTables.readGroundInteractionSideScrolling());
        this.liftThrow.writeThrownMotionInitialized(slot, true);
        this.liftThrow.writeLiftedStateInitialized(slot, false);
        this.liftThrow.writeLiftedPhase(slot, 0);
        this.liftThrow.writeLiftedCarryState(0);
        this.liftThrow.writeLiftedEffectiveDirection(0);
        this.liftThrow.writeLiftedEntitySlot(-1);
        this.physicsTables.writeEnemyTransitionCountdown(
                slot, entity.type() == EntityRomConstants.ENTITY_BOMB ? BombMotion.INITIAL_COUNTDOWN : 0);
        this.slots.replace(slot, withStatus(entity, EntityStatus.THROWN));
        return true;
    }

    public void movePushedBlock(int slot, int x, int y) {
        if (slot < 0
                || slot >= this.slots.size()
                || this.slots.get(slot).type() != EntityRomConstants.ENTITY_PUSHED_BLOCK
                || this.slots.get(slot).status() == EntityStatus.DISABLED) {
            return;
        }
        RoomEntity entity = this.slots.get(slot);
        this.slots.replace(
                slot,
                new RoomEntity(
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
                        false));
    }

    public void removePushedBlock(int slot) {
        if (slot >= 0
                && slot < this.slots.size()
                && this.slots.get(slot).type() == EntityRomConstants.ENTITY_PUSHED_BLOCK) {
            this.slots.clear(slot);
        }
    }

    public boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        for (int slot = 0; slot < this.slots.size(); slot++) {
            RoomEntity entity = this.slots.get(slot);
            if (entity.loaded() && entity.type() == 0x3E) {
                this.slots.replace(slot, marinPresentation(entity, x & 0xFF, y & 0xFF, spriteVariant));
                return true;
            }
        }
        return false;
    }

    private RoomEntity marinPresentation(RoomEntity entity, int x, int y, int variant) {
        if (entity.x() == x && entity.y() == y && entity.spriteVariant() == variant) return entity;
        RoomEntity cached =
                variant >= 0 && variant < marinPresentations.length ? marinPresentations[variant] : UNREAD_PRESENTATION;
        if (cached.slot() == entity.slot()
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

    public void updateHouseTarinPresentation(int linkX, int linkY) {
        for (int slot = 0; slot < this.slots.size(); slot++) {
            RoomEntity entity = this.slots.get(slot);
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_TARIN) {
                int dx = (byte) (linkX - entity.x());
                int dy = (byte) (linkY - entity.y());
                int direction = Math.abs(dy) >= Math.abs(dx) ? (dy < 0 ? 2 : 3) : (dx < 0 ? 1 : 0);
                this.slots.replace(slot, withVariant(entity, direction));
            }
        }
    }

    void startSwordPickup(int slot) {
        this.rewards.writeSwordPickupSequenceActive(slot, true);
        this.rewards.writeSwordPickupState(slot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(slot, 0xA0);
        this.physicsTables.writeSlowTimerInitialized(slot, true);
        this.presentationState.writePendingMusicTrack(0x0F);
    }

    void startToadstoolPickup(int slot) {
        this.rewards.writeToadstoolPickupSequenceActive(slot, true);
        this.physicsTables.writeEnemyTransitionCountdown(slot, 0x68);
        this.presentationState.writePendingMusicTrack(0x10);
    }

    void presentWitchGotItem(int slot) {
        this.events.linkMotionBlock(new LinkMotionBlockRequest(slot));
        this.events.linkHeldItemPose(new LinkHeldItemPoseRequest(slot));
    }

    void holdSwordAboveLink(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, boolean finalPose) {
        int slot = entity.slot();
        this.events.linkMotionBlock(new LinkMotionBlockRequest(slot));
        this.events.linkHeldItemPose(new LinkHeldItemPoseRequest(slot));
        int x = finalPose ? (linkEntityX - 4) & 0xFF : linkEntityX;
        RoomEntity held = withPositionAndVariant(entity, x, (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        this.slots.replace(slot, withZ(held, linkZ & 0xFF));
    }

    public int thrownDirection(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return liftThrow.readThrownDirection(slot) & 0xFF;
    }
}
