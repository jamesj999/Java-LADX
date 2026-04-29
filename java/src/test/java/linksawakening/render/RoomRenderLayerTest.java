package linksawakening.render;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.world.RoomConstants;
import linksawakening.world.RoomRenderSnapshot;
import linksawakening.world.ScrollController;
import linksawakening.world.TransitionController;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RoomRenderLayerTest {

    @Test
    void scrollLeftRendersCurrentRoomEnteringFromLeftAndPreviousRoomExitingRight() {
        GPU gpu = new GPU();
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        int previousColor = 0x112233;
        int currentColor = 0x445566;
        int[][] palettes = {
            { 0, previousColor, currentColor, 0 }
        };
        writeSolidTile(gpu, 0x80, 1);
        writeSolidTile(gpu, 0x81, 2);

        RoomRenderSnapshot previousRoom = snapshot(0x80, palettes);
        RoomRenderSnapshot currentRoom = snapshot(0x81, palettes);
        ScrollController scrollController = new ScrollController();
        scrollController.start(ScrollController.LEFT, 0, 0, previousRoom, RoomConstants.ROOM_PIXEL_WIDTH);
        scrollController.tick(8);

        new RoomRenderLayer(currentRoom, scrollController, new TransitionController())
            .render(new RenderContext(buffer, gpu));

        assertEquals(currentColor, pixelColor(buffer, 0, 0));
        assertEquals(previousColor, pixelColor(buffer, 8, 0));
    }

    private static RoomRenderSnapshot snapshot(int tileId, int[][] palettes) {
        int[] tileIds = new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT];
        int[] tileAttrs = new int[tileIds.length];
        Arrays.fill(tileIds, tileId);
        return new RoomRenderSnapshot(tileIds, tileAttrs, palettes);
    }

    private static void writeSolidTile(GPU gpu, int tileIndex, int colorIndex) {
        int low = (colorIndex & 0x01) != 0 ? 0xFF : 0x00;
        int high = (colorIndex & 0x02) != 0 ? 0xFF : 0x00;
        int base = tileIndex * GPU.TILE_DATA_SIZE;
        for (int y = 0; y < 8; y++) {
            gpu.writeVRAM(base + y * 2, (byte) low);
            gpu.writeVRAM(base + y * 2 + 1, (byte) high);
        }
    }

    private static int pixelColor(byte[] buffer, int x, int y) {
        int index = (y * Framebuffer.WIDTH + x) * 4;
        return (Byte.toUnsignedInt(buffer[index]) << 16)
            | (Byte.toUnsignedInt(buffer[index + 1]) << 8)
            | Byte.toUnsignedInt(buffer[index + 2]);
    }
}
