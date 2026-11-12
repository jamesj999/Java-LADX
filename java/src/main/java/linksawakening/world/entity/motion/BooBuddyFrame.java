package linksawakening.world.entity.motion;
/** Inputs for one motion update. */
record BooBuddyFrame(
        linksawakening.world.entity.RoomEntity entity,
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        int transitionCountdown,
        int roomTriggerCount,
        boolean swordMiddle,
        java.util.function.IntSupplier randomByteSupplier) {}
