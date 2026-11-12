package linksawakening.render;

import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.transition.ScrollController;

/** Submits visible dropped rupee entries using their world and height offsets. */
final class DroppableRupeeRenderLayer implements HardwareRenderLayer {
    private DroppableRupeeSystem droppableRupeeSystem;
    private ScrollController scrollController;
    private DroppableRupeeSystem paletteOwner;
    private int[] palette;

    public DroppableRupeeRenderLayer(DroppableRupeeSystem droppableRupeeSystem, ScrollController scrollController) {
        this.droppableRupeeSystem = droppableRupeeSystem;
        this.scrollController = scrollController;
    }

    void configure(DroppableRupeeSystem droppableRupeeSystem, ScrollController scrollController) {
        this.droppableRupeeSystem = droppableRupeeSystem;
        this.scrollController = scrollController;
    }

    @Override
    public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        if (scrollController.isActive()) return;
        int tile = droppableRupeeSystem.spriteTileIndex();
        if (paletteOwner != droppableRupeeSystem) {
            palette = droppableRupeeSystem.spritePalette();
            paletteOwner = droppableRupeeSystem;
        }
        for (int i = 0; i < droppableRupeeSystem.capacity(); i++) {
            if (!droppableRupeeSystem.slotActive(i) || !droppableRupeeSystem.slotVisible(i)) continue;
            int x = droppableRupeeSystem.slotX(i) - 4 - scrollController.screenShakeHorizontal();
            int y = droppableRupeeSystem.slotY(i) - (droppableRupeeSystem.slotZSubpixels(i) >> 4) - 16;
            frame.append8x16(gpu.getTile(tile), gpu.getTile(tile + 1), x, y, false, false, palette, false);
        }
    }
}
