package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.GameplaySimulation.*;
import static linksawakening.world.room.RoomConstants.*;

final class AnimatedTilesPhase {
    void tick(GameplaySimulation context, boolean dialogBlocksGameplay) {
        // Advance the animated BG tiles (waterfalls, weather vanes, etc.).
        // Original gates this behind wRoomTransitionState == 0 and the
        // inventory window not overlapping; mirror both here.
        if (!context.scrollController().isActive()
                && !context.transitionController().isInputBlocked()
                && !context.inventoryController().shouldBlockOverworldInput()
                && !dialogBlocksGameplay) {

            context.roomSession().tickGameplayVBlank();
        }
    }
}
