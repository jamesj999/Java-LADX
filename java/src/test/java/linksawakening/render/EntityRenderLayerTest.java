package linksawakening.render;

import static linksawakening.render.EntityRenderTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import linksawakening.dialog.DialogController;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.HookshotChainOam;
import linksawakening.world.entity.PincerBodyOam;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRenderLayerTest {
    @Test
    void newlyLoadedEntitiesRenderDuringPausedInitialization() {
        GPU gpu = new GPU();
        writeSolidTile(gpu, 0x40, 1);
        writeSolidTile(gpu, 0x41, 1);
        var definition = pairDefinition(
                new EntitySpriteDefinition.OamAttribute(0x40, 0), new EntitySpriteDefinition.OamAttribute(0x40, 0));
        var entity = new RoomEntity(0, 0, 0x35, 56, 64, EntityStatus.INIT, definition, 0, 0, 0, 0, -1, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(
                        snapshot(null, gpu.snapshotEntityTiles(), entity),
                        new int[][] {{0, 0x112233, 0, 0}},
                        new ScrollController())
                .render(new RenderContext(buffer, gpu));
        assertEquals(0x112233, pixelColor(buffer, 48, 48));
    }

    @Test
    void kingRectangleLayersUseTheDeathFlashPalette() {
        GPU gpu = new GPU();
        writeSolidTile(gpu, 0x40, 1);
        writeSolidTile(gpu, 0x41, 1);
        var definition = EntitySpriteDefinition.dynamic(
                0xE4,
                0x15,
                0x4A15,
                0,
                List.of(List.of(new EntitySpriteDefinition.DynamicSprite(
                        0,
                        0,
                        new EntitySpriteDefinition.OamAttribute(0x40, 2),
                        EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                        false))));
        int[][] palettes = new int[8][4];
        palettes[2][1] = 0x123456;
        palettes[4][1] = 0xABCDEF;
        for (int flash : new int[] {0, 0x10}) {
            var entity = new RoomEntity(0, 0, 0xE4, 56, 64, EntityStatus.DYING, definition, 0, flash, 0, 0, -1, false);
            byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
            new EntityRenderLayer(snapshot(null, gpu.snapshotEntityTiles(), entity), palettes, new ScrollController())
                    .render(new RenderContext(buffer, gpu));
            assertEquals(flash == 0 ? 0x123456 : 0xABCDEF, pixelColor(buffer, 48, 48));
        }
    }

    @Test
    void sharedItemTilesUseGameplayVramWhileRoomTilesKeepTheirSnapshot() {
        GPU gpu = new GPU();
        writeSolidTile(gpu, 0x40, 1);
        writeSolidTile(gpu, 0x41, 1);
        EntitySpriteTileSnapshot tiles = gpu.snapshotEntityTiles();
        writeSolidTile(gpu, 0x40, 3);
        writeSolidTile(gpu, 0x41, 3);
        writeSolidTile(gpu, 0xAC, 2); // Heart-piece graphics, outside the sheet snapshot.
        writeSolidTile(gpu, 0xAD, 2);
        var definition = pairDefinition(
                new EntitySpriteDefinition.OamAttribute(0xAC, 0), new EntitySpriteDefinition.OamAttribute(0x40, 0));
        var entity = new RoomEntity(0, 0, 0x35, 56, 64, EntityStatus.ACTIVE, definition, 0, 0, 0, 0, -1, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        int[][] palettes = {{0, 0x112233, 0x445566, 0x778899}};
        new EntityRenderLayer(snapshot(null, tiles, entity), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));
        assertEquals(0x445566, pixelColor(buffer, 48, 48));
        assertEquals(0x445566, pixelColor(buffer, 48, 56));
        assertEquals(0x112233, pixelColor(buffer, 56, 48));
        assertEquals(0x112233, pixelColor(buffer, 56, 56));
    }

    @Test
    void dialogHidesWholeSpritesAtTheRomOamThresholds() {
        GPU gpu = new GPU();
        writeSolidTile(gpu, 0x40, 1);
        writeSolidTile(gpu, 0x41, 1);
        var definition = pairDefinition(
                new EntitySpriteDefinition.OamAttribute(0x40, 0), new EntitySpriteDefinition.OamAttribute(0x40, 0));
        // Raw OAM Y: top hides below $3E; bottom hides from $58 onward.
        for (var position : DialogController.BoxPosition.values()) {
            int threshold = position == DialogController.BoxPosition.TOP ? 0x3E : 0x58;
            for (int rawY = threshold - 1; rawY <= threshold; rawY++) {
                var entity =
                        new RoomEntity(0, 0, 0x7A, 40, rawY, EntityStatus.ACTIVE, definition, 0, 0, 0, 0, -1, false);
                byte[] buffer = filledBuffer(0x11, 0x22, 0x33);
                new EntityRenderLayer(
                                snapshot(entity),
                                new int[][] {{0, 0xABCDEF, 0, 0}},
                                new ScrollController(),
                                0,
                                position)
                        .render(new RenderContext(buffer, gpu));
                boolean hidden = position == DialogController.BoxPosition.TOP ? rawY < threshold : rawY >= threshold;
                // Check the whole 16px sprite, including pixels outside the text box.
                for (int y = rawY - 16; y < rawY; y++) {
                    assertEquals(
                            hidden ? 0x112233 : 0xABCDEF, pixelColor(buffer, 32, y), position + " raw OAM Y=" + rawY);
                }
            }
        }
    }

    @Test
    void rendersVisibleHookshotChainOamFromTheLiveLinkTileLoad() {
        GPU gpu = new GPU();
        int chainTopColor = 0x123456;
        int chainBottomColor = 0x654321;
        int[][] palettes = {{0, chainTopColor, chainBottomColor, 0}};
        writeSolidTile(gpu, 0x24, 1);
        writeSolidTile(gpu, 0x25, 2);

        RoomEntitySnapshot snapshot =
                snapshot().withHookshotChainOam(HookshotChainOam.entries(0x40, 0x40, 0x40, 0x40, 0));
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot, palettes, new ScrollController()).render(new RenderContext(buffer, gpu));

        assertEquals(chainTopColor, pixelColor(buffer, 0x3C, 0x30));
        assertEquals(chainBottomColor, pixelColor(buffer, 0x3C, 0x38));
    }

    @Test
    void rendersBeachSwordFromGameplayVramInsteadOfEntitySheetSnapshot() {
        GPU gpu = new GPU();
        int swordColor = 0x123456;
        int[][] palettes = {{0, swordColor, 0, 0}};
        writeSolidTile(gpu, 0x84, 1);
        EntitySpriteDefinition swordDefinition = new EntitySpriteDefinition(
                0x31,
                0x03,
                0x5B97,
                EntitySpriteDefinition.Shape.SINGLE,
                0,
                List.of(new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x84, 0x00), null)));
        RoomEntity sword =
                new RoomEntity(0, 0, 0x31, 0x58, 0x60, EntityStatus.ACTIVE, swordDefinition, 0, 0, 0, 0, -1, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(null, gpu.snapshotEntityTiles(), sword), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));

        assertEquals(swordColor, pixelColor(buffer, 0x54, 0x50));
    }

    @Test
    void beachSwordUsesRomPaletteSevenAndKeepsColorZeroTransparent() {
        GPU gpu = new GPU();
        int backgroundColor = 0x0A0B0C;
        int paletteSevenColor = 0xA0B0C0;
        int[][] palettes = new int[8][4];
        palettes[7][1] = paletteSevenColor;
        byte[] buffer = filledBuffer(0x0A, 0x0B, 0x0C);
        writePatternTile(gpu, 0x84, new int[][] {
            {0, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1}
        });
        EntitySpriteDefinition swordDefinition = new EntitySpriteDefinition(
                0x31,
                0x03,
                0x5B97,
                EntitySpriteDefinition.Shape.SINGLE,
                0,
                List.of(new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x84, 0x17), null)));
        RoomEntity sword =
                new RoomEntity(0, 0, 0x31, 0x58, 0x60, EntityStatus.ACTIVE, swordDefinition, 0, 0, 0, 0, -1, false);

        new EntityRenderLayer(snapshot(null, gpu.snapshotEntityTiles(), sword), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));

        assertEquals(backgroundColor, pixelColor(buffer, 0x54, 0x50));
        assertEquals(paletteSevenColor, pixelColor(buffer, 0x55, 0x50));
        assertEquals(paletteSevenColor, pixelColor(buffer, 0x54, 0x51));
    }

    @Test
    void bowWowChainUsesRomTilesPaletteZeroAndRemainsBehindTheHead() {
        GPU gpu = new GPU();
        int chainColor = 0x123456;
        int headColor = 0xABCDEF;
        int[][] palettes = {{0, chainColor, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, headColor, 0, 0}};
        for (int tile : new int[] {0x40, 0x41, 0x4E, 0x4F}) writeSolidTile(gpu, tile, 1);
        EntitySpriteDefinition head = new EntitySpriteDefinition(
                0x6D,
                5,
                0x401C,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(new EntitySpriteDefinition.Variant(
                        new EntitySpriteDefinition.OamAttribute(0x40, 3),
                        new EntitySpriteDefinition.OamAttribute(0x40, 0x23))));
        RoomEntity entity = new RoomEntity(3, 0, 0x6D, 0x40, 0x50, EntityStatus.ACTIVE, head, 0, 0, 0, 0, -1, false);
        RoomEntitySnapshot snapshot = snapshot(null, gpu.snapshotEntityTiles(), entity)
                .withBowWowChainOam(List.of(
                        new linksawakening.world.entity.BowWowChain.Entry(3, 0x40, 0x50, 0x4E),
                        new linksawakening.world.entity.BowWowChain.Entry(3, 0x60, 0x50, 0x4E)));
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(snapshot, palettes, new ScrollController()).render(new RenderContext(buffer, gpu));
        assertEquals(headColor, pixelColor(buffer, 0x38, 0x40));
        assertEquals(chainColor, pixelColor(buffer, 0x58, 0x40));
        assertEquals(chainColor, pixelColor(buffer, 0x58, 0x48));
    }

    @Test
    void rendersPincerBodyOamFromTheEntityTileSnapshot() {
        GPU gpu = new GPU();
        int bodyColor = 0x123456;
        int[][] palettes = {{0, 0, 0, 0}, {0, 0, 0, 0}, {0, bodyColor, 0, 0}};
        writeSolidTile(gpu, 0x6A, 1);
        writeSolidTile(gpu, 0x6B, 1);
        EntitySpriteDefinition hiddenPincer = new EntitySpriteDefinition(
                0xB0,
                0x07,
                0x542B,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(new EntitySpriteDefinition.Variant(
                        new EntitySpriteDefinition.OamAttribute(0xFF, 0x00),
                        new EntitySpriteDefinition.OamAttribute(0xFF, 0x20))));
        RoomEntity entity =
                new RoomEntity(3, 0, 0xB0, 0x4C, 0x58, EntityStatus.ACTIVE, hiddenPincer, 0, 0, 0, 0, -1, false);
        RoomEntitySnapshot snapshot = snapshot(null, gpu.snapshotEntityTiles(), entity)
                .withPincerBodyOam(PincerBodyOam.entries(3, 0x40, 0x50, 0x4C, 0x58, 3));
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot, palettes, new ScrollController()).render(new RenderContext(buffer, gpu));

        assertEquals(bodyColor, pixelColor(buffer, 0x3C, 0x40));
        assertEquals(bodyColor, pixelColor(buffer, 0x3C, 0x48));
        assertEquals(0, pixelColor(buffer, 0x34, 0x40));
    }
}
