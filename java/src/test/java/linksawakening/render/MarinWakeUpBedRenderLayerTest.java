package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Tile;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import linksawakening.world.ScrollController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MarinWakeUpBedRenderLayerTest {
    @Test
    void rendersDedicatedAwakePairAndGbcMattressFromEntityTiles() {
        Tile[] tiles = new Tile[EntitySpriteTileSnapshot.TILE_COUNT];
        for (int index = 0; index < tiles.length; index++) tiles[index] = new Tile();
        fill(tiles[0x48 - 0x40], 2);
        fill(tiles[0x49 - 0x40], 2);
        fill(tiles[0x4A - 0x40], 2);
        fill(tiles[0x4B - 0x40], 2);
        fill(tiles[0x4E - 0x40], 1);
        fill(tiles[0x4F - 0x40], 1);
        int[][] palettes = new int[8][4];
        palettes[0][2] = 0x00CC1122;
        palettes[6][1] = 0x0033AA44;
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());
        link.showMarinWakeUpBed(3);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        IndexedRenderer.clear(buffer);

        new MarinWakeUpBedRenderLayer(link, new EntitySpriteTileSnapshot(tiles), palettes,
            new ScrollController()).render(new RenderContext(buffer, new GPU()));

        assertEquals(0x00CC1122, pixel(buffer, 0x30, 0x24));
        assertEquals(0x0033AA44, pixel(buffer, 0x30, 0x34));
        assertEquals(0, pixel(buffer, 0x30, 0x3C));
    }

    private static void fill(Tile tile, int color) {
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) tile.setPixel(x, y, color);
    }

    private static int pixel(byte[] buffer, int x, int y) {
        int offset = (y * Framebuffer.WIDTH + x) * 4;
        return (buffer[offset] & 0xFF) << 16 | (buffer[offset + 1] & 0xFF) << 8
            | buffer[offset + 2] & 0xFF;
    }
}
