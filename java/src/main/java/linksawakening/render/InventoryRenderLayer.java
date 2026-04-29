package linksawakening.render;

import linksawakening.ui.InventoryController;

public final class InventoryRenderLayer implements RenderLayer {
    private final InventoryController inventoryController;

    public InventoryRenderLayer(InventoryController inventoryController) {
        this.inventoryController = inventoryController;
    }

    @Override
    public void render(RenderContext context) {
        inventoryController.render(context.buffer(), context.gpu());
    }
}
