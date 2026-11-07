package linksawakening.app.gameplay;

import linksawakening.app.gameplay.*;



import static linksawakening.world.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;


import java.util.function.*;


import static linksawakening.app.gameplay.GameplaySimulation.*;

final class AnimatedTilesPhase {
    void tick(GameplaySimulation context, boolean dialogBlocksGameplay) {
        var gpu = context.gpu();
        var romData = context.romData();
        var roomSession = context.roomSession();
        var transitionController = context.transitionController();
        var scrollController = context.scrollController();
        var inventoryController = context.inventoryController();
            // Advance the animated BG tiles (waterfalls, weather vanes, etc.).
            // Original gates this behind wRoomTransitionState == 0 and the
            // inventory window not overlapping; mirror both here.
            if (!scrollController.isActive()
                && !transitionController.isInputBlocked()
                && !inventoryController.shouldBlockOverworldInput()
                && !dialogBlocksGameplay) {
                if (roomSession == null) {
                    gpu.tickAnimatedTiles(romData);
                } else {
                    roomSession.tickGameplayVBlank();
                }
            }
    }
}
