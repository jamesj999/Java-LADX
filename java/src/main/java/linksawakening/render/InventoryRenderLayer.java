package linksawakening.render;

import linksawakening.ui.InventoryController;

/** Applies inventory entry hiding and submits the ROM-backed window. */
public final class InventoryRenderLayer implements HardwareRenderLayer {
    private InventoryController inventoryController;
    private int[][] roomPalettes;

    public InventoryRenderLayer(InventoryController inventoryController) {
        this(inventoryController, null);
    }

    public InventoryRenderLayer(InventoryController inventoryController, int[][] roomPalettes) {
        this.inventoryController = inventoryController;
        this.roomPalettes = roomPalettes;
    }

    void configure(InventoryController inventoryController, int[][] roomPalettes) {
        this.inventoryController = inventoryController;
        this.roomPalettes = roomPalettes;
    }

    @Override
    public boolean supportsHardware() {
        return true;
    }

    @Override
    public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        if (inventoryController.inventoryAppearing())
            frame.hideGameplayObjectsUnderWindow(inventoryController.windowY());
        inventoryController.submit(frame, gpu, roomPalettes);
    }
}
