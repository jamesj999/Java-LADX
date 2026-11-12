package linksawakening.world.entity.motion;
/** Inputs for one motion update. */
record SpikedBeetleFrame(
        linksawakening.world.entity.RoomEntity entity,
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        java.util.function.IntSupplier randomByteSupplier,
        linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision,
        int transitionCountdown,
        int ignoreHitsCountdown) {}
