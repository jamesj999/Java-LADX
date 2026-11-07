package linksawakening.render;

import linksawakening.ui.InventoryController;

public final class InventoryRenderLayer implements HardwareRenderLayer {
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
    @Override public boolean supportsHardware() { return inventoryController.canSubmitClosedWindow() || inventoryController.canSubmitFullyOpenWindow(); }
    @Override public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        if (!inventoryController.canSubmitClosedWindow()) {
            frame.hideGameplayObjectsForOpenInventory();
            inventoryController.submit(frame, gpu, roomPalettes);
            return;
        }
        var window = inventoryController.closedWindow(roomPalettes);
        for (int y = 0; y < 18; y++) for (int x = 0; x < 20; x++) {
            int index = y * 20 + x, attr = window.attributes()[index] & 255;
            int tile = window.tiles()[index] & 255;
            frame.closedHudTile(gpu.getTile(IndexedRenderer.resolveSignedBgTileIndex(tile)), tile, attr,
                x * 8, y * 8 + window.y(), window.palettes()[attr & 7]);
        }
    }
}
