package linksawakening.render;

import linksawakening.ui.InventoryController;

public final class InventoryRenderLayer implements RenderLayer {
    private final InventoryController inventoryController;
    private final int[][] roomPalettes;

    public InventoryRenderLayer(InventoryController inventoryController) {
        this(inventoryController, null);
    }

    public InventoryRenderLayer(InventoryController inventoryController, int[][] roomPalettes) {
        this.inventoryController = inventoryController;
        this.roomPalettes = roomPalettes;
    }

    @Override
    public void render(RenderContext context) {
        inventoryController.render(context.buffer(), context.gpu(), roomPalettes);
    }
}
