package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 Like LikeEntityHandler state and its shared Gibdo walk. */
final class LikeLikeMotion {
    private static final int INVENTORY_SHIELD = 0x04;
    private static final int LINK_MOTION_DEFAULT = 0x00;
    private static final int LINK_MOTION_NON_INTERACTIVE = 0x02;

    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final GibdoMotion walkMotion = new GibdoMotion();

    record Update(RoomEntity entity, int slowTransitionCountdown,
                  RoomEntityRuntime.LikeLikeEvent event, int spriteVariant) {
    }

    void initialize(int slot) {
        privateState1[slot] = 0;
        privateState3[slot] = 0;
        privateState4[slot] = 0;
        inertia[slot] = 0;
        initialized[slot] = true;
        walkMotion.initialize(slot);
    }

    void clear(int slot) {
        privateState1[slot] = 0;
        privateState3[slot] = 0;
        privateState4[slot] = 0;
        inertia[slot] = 0;
        initialized[slot] = false;
        walkMotion.clear(slot);
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   int linkZ, int linkMotionState, boolean actionButtonsHeld,
                   int linkItemA, int linkItemB, int shieldLevel,
                   int slowTransitionCountdown, IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        if (privateState3[slot] == 0) {
            if (slowTransitionCountdown == 0
                && linkZ == 0
                && linkMotionState < LINK_MOTION_NON_INTERACTIVE
                && RoomEntityCombatRules.collisionCadenceMatches(frameCounter, slot)
                && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
                privateState3[slot] = 1;
                inertia[slot] = 0;
            }
            return new Update(
                walkMotion.advance(entity, randomByteSupplier, backgroundCollision),
                slowTransitionCountdown, null, -1);
        }

        if (actionButtonsHeld) {
            inertia[slot] = (inertia[slot] + 1) & 0xFF;
            if (inertia[slot] >= 0x08) {
                privateState3[slot] = 0;
                slowTransitionCountdown = 0x15;
                return new Update(entity, slowTransitionCountdown,
                    new RoomEntityRuntime.LikeLikeEvent(
                        slot, RoomEntityRuntime.LikeLikeEvent.Kind.RELEASE,
                        entity.x(), entity.y(), -1, 0), -1);
            }
        }

        int stolenInventorySlot = -1;
        int stolenShieldLevel = 0;
        if (privateState1[slot] == 0) {
            if (linkItemB == INVENTORY_SHIELD) {
                if (shieldLevel < 2) {
                    privateState1[slot] = shieldLevel & 0xFF;
                    stolenInventorySlot = 0;
                    stolenShieldLevel = shieldLevel & 0xFF;
                }
            } else if (linkItemA == INVENTORY_SHIELD && shieldLevel < 2) {
                privateState1[slot] = shieldLevel & 0xFF;
                stolenInventorySlot = 1;
                stolenShieldLevel = shieldLevel & 0xFF;
            }
        }

        // The handler writes HIDDEN before this check. There is no Link event
        // to emit until the source accepts the current motion state.
        if (linkMotionState != LINK_MOTION_DEFAULT) {
            return new Update(entity, slowTransitionCountdown, null, -1);
        }

        // The source calls func_006_7F05 twice. Four RRA instructions select
        // bit four of the pre-increment privateState4 value; the second call
        // is the final variant written to the entity slot.
        privateState4[slot] = (privateState4[slot] + 1) & 0xFF;
        int secondAnimationCounter = privateState4[slot];
        privateState4[slot] = (privateState4[slot] + 1) & 0xFF;
        int spriteVariant = (secondAnimationCounter >>> 4) & 0x01;

        return new Update(entity, slowTransitionCountdown,
            new RoomEntityRuntime.LikeLikeEvent(
                slot, RoomEntityRuntime.LikeLikeEvent.Kind.CAPTURE,
                entity.x(), entity.y(), stolenInventorySlot, stolenShieldLevel),
            spriteVariant);
    }

    int state(int slot) {
        return privateState3[slot];
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    int inertia(int slot) {
        return inertia[slot];
    }

    int walkState(int slot) {
        return walkMotion.state(slot);
    }

    int walkSpeedY(int slot) {
        return walkMotion.speedY(slot);
    }
}
