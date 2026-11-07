package linksawakening.render;

import linksawakening.world.DroppableRupeeSystem;
import linksawakening.world.ScrollController;

public final class DroppableRupeeRenderLayer implements HardwareRenderLayer {
    private final DroppableRupeeSystem droppableRupeeSystem;
    private final ScrollController scrollController;

    public DroppableRupeeRenderLayer(DroppableRupeeSystem droppableRupeeSystem,
                                     ScrollController scrollController) {
        this.droppableRupeeSystem = droppableRupeeSystem;
        this.scrollController = scrollController;
    }

    @Override
    public void render(RenderContext context) {
        if (!scrollController.isActive()) {
            droppableRupeeSystem.render(context.buffer(), context.gpu(),
                -scrollController.screenShakeHorizontal());
        }
    }
    @Override public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        if (scrollController.isActive()) return;
        int tile = droppableRupeeSystem.spriteTileIndex();
        int[] palette = droppableRupeeSystem.spritePalette();
        var slots = droppableRupeeSystem.activeSlots();
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            if (!slot.visible()) continue;
            int x = slot.worldX() - 4 - scrollController.screenShakeHorizontal();
            int y = slot.worldY() - (slot.posZSubpixels() >> 4) - 16;
            frame.append8x16(gpu.getTile(tile), gpu.getTile(tile + 1), x, y, false, false, palette, false);
        }
    }
}
