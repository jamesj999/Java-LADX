package linksawakening.render;

import linksawakening.world.DroppableRupeeSystem;
import linksawakening.world.ScrollController;

public final class DroppableRupeeRenderLayer implements RenderLayer {
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
            droppableRupeeSystem.render(context.buffer(), context.gpu());
        }
    }
}
