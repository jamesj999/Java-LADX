package linksawakening.render;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FrameSceneTest {

    @Test
    void frameSceneClearsBufferThenRendersLayersInOrder() {
        GPU gpu = new GPU();
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = 0x33;
        }
        List<String> calls = new ArrayList<>();

        FrameScene scene = FrameScene.of(List.of(
            context -> {
                calls.add("first");
                writePixel(context.buffer(), 0, 0, 0x112233);
            },
            context -> {
                calls.add("second");
                writePixel(context.buffer(), 1, 0, 0x445566);
            }
        ));

        scene.drawTo(buffer, gpu);

        assertEquals(List.of("first", "second"), calls);
        assertEquals(0x112233, pixelColor(buffer, 0, 0));
        assertEquals(0x445566, pixelColor(buffer, 1, 0));
        assertEquals(0x000000, pixelColor(buffer, 2, 0));
        assertEquals(0xFF, Byte.toUnsignedInt(buffer[3]));
    }

    private static void writePixel(byte[] buffer, int x, int y, int color) {
        int index = (y * Framebuffer.WIDTH + x) * 4;
        buffer[index] = (byte) ((color >> 16) & 0xFF);
        buffer[index + 1] = (byte) ((color >> 8) & 0xFF);
        buffer[index + 2] = (byte) (color & 0xFF);
        buffer[index + 3] = (byte) 0xFF;
    }

    private static int pixelColor(byte[] buffer, int x, int y) {
        int index = (y * Framebuffer.WIDTH + x) * 4;
        return (Byte.toUnsignedInt(buffer[index]) << 16)
            | (Byte.toUnsignedInt(buffer[index + 1]) << 8)
            | Byte.toUnsignedInt(buffer[index + 2]);
    }
}
