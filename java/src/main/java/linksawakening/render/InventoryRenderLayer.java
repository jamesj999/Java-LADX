package linksawakening.render;

import linksawakening.ui.InventoryController;
import linksawakening.world.RoomRenderSnapshot;
import linksawakening.world.TransitionController;

public final class InventoryRenderLayer implements RenderLayer {
    private final InventoryController inventoryController;
    private final RoomRenderSnapshot room;
    private final TransitionController transitionController;
    private final int frameCounter;

    public InventoryRenderLayer(InventoryController inventoryController, RoomRenderSnapshot room,
                                TransitionController transitionController, int frameCounter) {
        this.inventoryController = inventoryController;
        this.room = room;
        this.frameCounter = frameCounter;
        this.transitionController = transitionController;
    }

    @Override
    public void render(RenderContext context) {
        int[][] gameplayPalettes = transitionController.applyFade(room.palettes());
        boolean swordFlashVisible = (frameCounter & 8) == 0
            && (inventoryController.shouldBlockOverworldInput() || transitionController.fadeLevel() == 0);
        inventoryController.render(context.buffer(), context.gpu(), gameplayPalettes, swordFlashVisible);
    }
}
