package linksawakening.render;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import org.junit.jupiter.api.Test;

final class FrameDrawObserverTest {
    @Test
    void explicitObserverReceivesCompletedOutputAndDefaultDrawDoesNotRetainIt() {
        var gpu = new GPU();
        var scene = FrameScene.of(List.of((HardwareRenderLayer)
                (frame, ignored) -> frame.backgroundPixel(0, 0, 1, new int[] {0, 0x123456}, false)));
        var output = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        var observed = new ArrayList<FrameScene>();
        scene.drawTo(output, gpu, (actual, pixels, actualGpu) -> {
            assertSame(scene, actual);
            assertSame(output, pixels);
            assertSame(gpu, actualGpu);
            assertEquals(0x12, pixels[0] & 255);
            observed.add(actual);
        });
        scene.drawTo(output, gpu);
        assertEquals(List.of(scene), observed);
    }

    @Test
    void builderObserverBelongsToOneBuilderAndCanBeDetached() {
        var first = new GameFrameSceneBuilder();
        var second = new GameFrameSceneBuilder();
        var observed = new ArrayList<FrameScene>();
        first.setDrawObserver((scene, pixels, gpu) -> observed.add(scene));
        var gpu = new GPU();
        var output = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        var scene = first.build(GameFrameState.empty());
        scene.drawTo(output, gpu);
        second.build(GameFrameState.empty()).drawTo(output, gpu);
        first.setDrawObserver(null);
        first.build(GameFrameState.empty()).drawTo(output, gpu);
        assertEquals(List.of(scene), observed);
    }
}
