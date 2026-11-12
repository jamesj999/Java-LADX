package linksawakening.world.entity.motion;
/** Inputs for one motion update. */
record BomberFrame(
        linksawakening.world.entity.RoomEntity entity,
        int transitionCountdown,
        int linkEntityX,
        int linkEntityY,
        int romLinkDirection,
        boolean swordAnimationActive,
        java.util.function.IntSupplier randomByteSupplier,
        linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision) {}
