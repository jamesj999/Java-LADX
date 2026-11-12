package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;

/** Read-only views over ROM slot state for gameplay and diagnostics. */
public final class EntityEnemyInspection {
    private final EntityEnemyInspectionServices runtime;

    EntityEnemyInspection(EntityEnemyInspectionServices runtime) {
        this.runtime = runtime;
    }

    public List<TransientVfxRequest> transientVfxRequests() {
        return List.copyOf(runtime.events().transientVfxRequests);
    }

    public List<BoomerangObjectRequest> boomerangObjectRequests() {
        return List.copyOf(runtime.events().boomerangObjectRequests);
    }

    public List<MagicRodObjectRequest> magicRodObjectRequests() {
        return List.copyOf(runtime.events().magicRodObjectRequests);
    }

    public List<MagicPowderObjectRequest> magicPowderObjectRequests() {
        return List.copyOf(runtime.events().magicPowderObjectRequests);
    }

    public int physicsFlags(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.physicsTables().readEnemyPhysicsFlags(slot);
    }

    public int transitionCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    public int enemyHealth(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.combatTables().readEnemyHealth(slot);
    }

    public int enemyIgnoreHitsCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.combatTables().readEnemyIgnoreHitsCountdown(slot);
    }

    public int ledgeTransitionTimer(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.liftThrow().readLedgeTransitionTimer(slot) & 0xFF;
    }

    public int fallingTargetX(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.physicsTables().readFallingTargetX(slot);
    }

    public boolean polsVoiceBalladOcarinaTrigger() {
        return runtime.linkState().linkPlayingOcarinaCountdown == 0x01
                && (runtime.linkState().ocarinaSongFlags & 0x04) != 0
                && runtime.linkState().selectedSongIndex == 0;
    }

    boolean shouldDisappear(RoomEntity entity) {
        return runtime.physicsTables().readSlowTimerInitialized(entity.slot())
                && isCommonDroppableType(entity.type())
                && entity.type() != EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                // DroppableDisappearIfNeeded is after the hidden-state helper in
                // every shared drop handler. A hidden item therefore must not be
                // unloaded merely because its global fade timer reached zero.
                && runtime.bossRewards().readDroppablePrivateState3(entity.slot()) == 0
                && runtime.physicsTables().readSlowTransitionCountdown(entity.slot()) < 0x1C;
    }

    boolean isDisabledFollower(int type) {
        return switch (type) {
            case FollowingNpcEntitySpawner.ENTITY_ROOSTER -> !runtime.motions()
                    .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                    .readFollowingNpcState()
                    .roosterFollowing();
            case FollowingNpcEntitySpawner.ENTITY_GHOST -> runtime.motions()
                            .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                            .readFollowingNpcState()
                            .ghostFollowingState()
                    != 1;
            case FollowingNpcEntitySpawner.ENTITY_MARIN_AT_THE_SHORE -> !runtime.motions()
                    .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                    .readFollowingNpcState()
                    .marinFollowing();
            case FollowingNpcEntitySpawner.ENTITY_BOW_WOW -> !runtime.motions()
                    .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                    .readFollowingNpcState()
                    .bowWowFollowing();
            default -> false;
        };
    }

    boolean entityHandlerInteractive(boolean creditsGameplay) {
        if (!creditsGameplay
                && (!runtime.roomVariables().readGameplayWorld()
                        || runtime.roomVariables().readTransitionSequenceCounter() != 0x04)) {
            return false;
        }
        return !runtime.linkState().dialogActive
                && !runtime.linkState().inventoryAppearing
                && !runtime.inspection().witchGotItemPresentationActive()
                && !runtime.roomVariables().readRoomTransitionActive();
    }

    boolean entityTimersInteractive() {
        return !runtime.linkState().dialogActive
                && !runtime.linkState().inventoryAppearing
                && runtime.linkState().linkPlayingOcarinaCountdown == 0;
    }

    EntityEnemyInspectionServices inspectionServices() {
        return runtime;
    }
}
