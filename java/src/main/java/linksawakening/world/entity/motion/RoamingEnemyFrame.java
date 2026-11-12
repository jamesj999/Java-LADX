package linksawakening.world.entity.motion;
/** Inputs for one motion update. */
record RoamingEnemyFrame(
        linksawakening.world.entity.RoomEntity entity,
        int linkEntityX,
        int linkEntityY,
        java.util.function.IntSupplier randomByteSupplier,
        linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction,
        int ignoreHitsCountdown,
        int frameCounter,
        boolean creditsGameplay) {}
