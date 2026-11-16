package linksawakening.world.entity;

import static linksawakening.world.entity.EntityTypeRules.isCommonDroppableType;

import java.util.List;
import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.FollowingNpcMotion;

/** Read-only views over ROM slot state for gameplay and diagnostics. */
public final class EntityEnemyInspection {
    final EntityBossState bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLiftState liftThrow;
    final EntityLinkState linkState;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityRoomInspection inspection;
    final EntityRoomInspection roomInspection;
    final EntityRoomVariables roomVariables;
    final EntitySlotPresentation slotProperties;
    final EntitySlots slots;

    EntityEnemyInspection(
            EntityBossState bossRewards,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityRoomInspection inspection,
            EntityRoomInspection roomInspection,
            EntityRoomVariables roomVariables,
            EntitySlotPresentation slotProperties,
            EntitySlots slots) {
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.inspection = java.util.Objects.requireNonNull(inspection, "inspection");
        this.roomInspection = java.util.Objects.requireNonNull(roomInspection, "roomInspection");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
    }

    public List<TransientVfxRequest> transientVfxRequests() {
        return List.copyOf(this.events.transientVfxRequests);
    }

    public List<BoomerangObjectRequest> boomerangObjectRequests() {
        return List.copyOf(this.events.boomerangObjectRequests);
    }

    public List<MagicRodObjectRequest> magicRodObjectRequests() {
        return List.copyOf(this.events.magicRodObjectRequests);
    }

    public List<MagicPowderObjectRequest> magicPowderObjectRequests() {
        return List.copyOf(this.events.magicPowderObjectRequests);
    }

    public int physicsFlags(int slot) {
        if (slot < 0 || slot >= this.slots.size()) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return this.physicsTables.readEnemyPhysicsFlags(slot);
    }

    public int transitionCountdown(int slot) {
        if (slot < 0 || slot >= this.slots.size()) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return this.physicsTables.readEnemyTransitionCountdown(slot);
    }

    public int enemyHealth(int slot) {
        if (slot < 0 || slot >= this.slots.size()) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return this.combatTables.readEnemyHealth(slot);
    }

    public int enemyIgnoreHitsCountdown(int slot) {
        if (slot < 0 || slot >= this.slots.size()) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return this.combatTables.readEnemyIgnoreHitsCountdown(slot);
    }

    public int ledgeTransitionTimer(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return this.liftThrow.readLedgeTransitionTimer(slot) & 0xFF;
    }

    public int fallingTargetX(int slot) {
        if (slot < 0 || slot >= this.slots.size()) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return this.physicsTables.readFallingTargetX(slot);
    }

    public boolean polsVoiceBalladOcarinaTrigger() {
        return this.linkState.linkPlayingOcarinaCountdown == 0x01
                && (this.linkState.ocarinaSongFlags & 0x04) != 0
                && this.linkState.selectedSongIndex == 0;
    }

    boolean shouldDisappear(RoomEntity entity) {
        return this.physicsTables.readSlowTimerInitialized(entity.slot())
                && isCommonDroppableType(entity.type())
                && entity.type() != EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                // DroppableDisappearIfNeeded is after the hidden-state helper in
                // every shared drop handler. A hidden item therefore must not be
                // unloaded merely because its global fade timer reached zero.
                && this.bossRewards.readDroppablePrivateState3(entity.slot()) == 0
                && this.physicsTables.readSlowTransitionCountdown(entity.slot()) < 0x1C;
    }

    boolean isDisabledFollower(int type) {
        return switch (type) {
            case FollowingNpcEntitySpawner.ENTITY_ROOSTER -> !this.motions
                    .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                    .readFollowingNpcState()
                    .roosterFollowing();
            case FollowingNpcEntitySpawner.ENTITY_GHOST -> this.motions
                            .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                            .readFollowingNpcState()
                            .ghostFollowingState()
                    != 1;
            case FollowingNpcEntitySpawner.ENTITY_MARIN_AT_THE_SHORE -> !this.motions
                    .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                    .readFollowingNpcState()
                    .marinFollowing();
            case FollowingNpcEntitySpawner.ENTITY_BOW_WOW -> !this.motions
                    .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                    .readFollowingNpcState()
                    .bowWowFollowing();
            default -> false;
        };
    }

    boolean entityHandlerInteractive(boolean creditsGameplay) {
        if (!creditsGameplay
                && (!this.roomVariables.readGameplayWorld()
                        || this.roomVariables.readTransitionSequenceCounter() != 0x04)) {
            return false;
        }
        return !this.linkState.dialogActive
                && !this.linkState.inventoryAppearing
                && !this.inspection.witchGotItemPresentationActive()
                && !this.roomVariables.readRoomTransitionActive();
    }

    boolean entityTimersInteractive() {
        return !this.linkState.dialogActive
                && !this.linkState.inventoryAppearing
                && this.linkState.linkPlayingOcarinaCountdown == 0;
    }

    public boolean gelClingingToLink() {
        return motions.get(
                        linksawakening.world.entity.motion.ZolGelMotion.class,
                        linksawakening.world.entity.motion.ZolGelMotion::new)
                .readGelClingingToLink();
    }
}
