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
    @Override public boolean supportsHardware() {
        return scrollController.isActive() || droppableRupeeSystem.activeCount() == 0;
    }
    @Override public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        if (!supportsHardware()) throw new IllegalStateException("Rupee palette API not exposed");
    }
}
