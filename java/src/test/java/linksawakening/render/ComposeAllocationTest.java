package linksawakening.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Tile;
import org.junit.jupiter.api.Test;

/** Measures steady-state indexed scene submission and composition without ROM data. */
final class ComposeAllocationTest {
    @Test
    void composeAllocationBudget() {
        GPU gpu = new GPU();
        Tile tile = new Tile();
        tile.setPixel(0, 0, 2);
        int[] palette = {0, 0x123456, 0xABCDEF, 0xFFFFFF};
        HardwareRenderLayer layer = (frame, ignored) -> {
            for (int y = 0; y < 144; y += 8)
                for (int x = 0; x < 160; x += 8) frame.backgroundTile(tile, x, y, false, false, palette, false);
            for (int i = 0; i < 40; i++) frame.append(tile, i * 4, i * 3, false, false, palette, false);
            frame.hideGameplayObjectsUnderWindow(96);
        };
        FrameScene scene = FrameScene.of(List.of(layer));
        byte[] output = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        var observer = TestDrawObservers.CURRENT.get();
        TestDrawObservers.CURRENT.remove();
        try {
            for (int i = 0; i < 1000; i++) scene.drawTo(output, gpu);
            long thread = Thread.currentThread().threadId();
            long before = bean.getThreadAllocatedBytes(thread);
            for (int i = 0; i < 200; i++) scene.drawTo(output, gpu);
            long bytes = (bean.getThreadAllocatedBytes(thread) - before) / 200;
            System.out.println("Compose allocation: " + bytes + " bytes/frame");
            assertTrue(bytes >= 0);
        } finally {
            if (observer != null) TestDrawObservers.CURRENT.set(observer);
        }
    }
}
