package linksawakening.render;

import static linksawakening.render.EntityRenderTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.rom.Rom;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.WingedOctorokOam;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityGeneratedOamRenderTest {
    @Test
    void rendersWingedOctorokGeneratedRectFromTheGpuTileRegion() {
        GPU gpu = new GPU();
        int gpuColor = 0x123456;
        int[][] palettes = {{0, gpuColor, 0, 0}};
        writeSolidTile(gpu, 0x22, 1);
        writeSolidTile(gpu, 0x23, 1);
        EntitySpriteDefinition hiddenWingedOctorok = new EntitySpriteDefinition(
                0xAE,
                0x07,
                0x562D,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(new EntitySpriteDefinition.Variant(
                        new EntitySpriteDefinition.OamAttribute(0xFF, 0x00),
                        new EntitySpriteDefinition.OamAttribute(0xFF, 0x20))));
        RoomEntity entity =
                new RoomEntity(2, 0, 0xAE, 0x40, 0x50, EntityStatus.ACTIVE, hiddenWingedOctorok, 0, 0, 0, 0, -1, false);
        RoomEntitySnapshot snapshot = snapshot(null, gpu.snapshotEntityTiles(), entity)
                .withWingedOctorokOam(WingedOctorokOam.entries(2, 0, 0x40, 0x50, 0));
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot, palettes, new ScrollController()).render(new RenderContext(buffer, gpu));

        assertEquals(gpuColor, pixelColor(buffer, 0x34, 0x40));
        assertEquals(gpuColor, pixelColor(buffer, 0x44, 0x40));
    }

    @Test
    void rendersMoblinSwordDynamicOamInRomOrderUsingBothTileSources() {
        GPU gpu = new GPU();
        int bodyColor = 0x112233;
        int swordColor = 0x223344;
        int warningColor = 0x334455;
        int[][] palettes = new int[8][4];
        palettes[3][1] = bodyColor;
        palettes[3][2] = swordColor;
        palettes[6][2] = warningColor;

        writeSolidTile(gpu, 0x04, 2);
        writeSolidTile(gpu, 0x05, 2);
        writeSolidTile(gpu, 0x60, 1);
        writeSolidTile(gpu, 0x61, 1);
        writeSolidTile(gpu, 0x62, 1);
        writeSolidTile(gpu, 0x63, 1);
        writeSolidTile(gpu, 0x86, 2);
        writeSolidTile(gpu, 0x87, 2);
        writeSolidTile(gpu, 0xF0, 2);
        writeSolidTile(gpu, 0xF1, 2);

        EntitySpriteDefinition definition = moblinDefinition();
        RoomEntity entity = new RoomEntity(0, 0, 0x14, 24, 32, EntityStatus.ACTIVE, definition, 0, 0, 0, 0, -1, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(null, gpu.snapshotEntityTiles(), entity), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));

        assertEquals(warningColor, pixelColor(buffer, 14, 21));
        assertEquals(warningColor, pixelColor(buffer, 16, 24));
        assertEquals(swordColor, pixelColor(buffer, 24, 24));
        assertEquals(bodyColor, pixelColor(buffer, 16, 16));
        assertEquals(bodyColor, pixelColor(buffer, 24, 16));
    }

    @Test
    void rendersBushCrawlerCrawlPassesInRomOamPriorityOrder() throws Exception {
        GPU gpu = new GPU();
        int crawlingBodyColor = 0x112233;
        int bushOverlayColor = 0x445566;
        int[][] palettes = new int[8][4];
        palettes[6][1] = crawlingBodyColor;
        palettes[2][1] = bushOverlayColor;
        for (int tile : new int[] {0xF0, 0xF2, 0xF4, 0xF6, 0x7C, 0x7E}) {
            writeSolidTile(gpu, tile, 1);
        }

        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(Rom.of(loadRom()))
                .forBushCrawlerCrawlState(EntityRoomLoader.RoomTable.OVERWORLD, 0);
        RoomEntity entity =
                new RoomEntity(0, 0, 0xBB, 0x40, 0x40, EntityStatus.ACTIVE, definition, 0, 0, 0, 0, -1, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(null, gpu.snapshotEntityTiles(), entity), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));

        // The Bushcrawler2 pair is emitted after the room-specific pair. The
        // renderer walks the list backwards so the earlier room pair retains
        // Game Boy OAM priority when both passes overlap.
        assertEquals(crawlingBodyColor, pixelColor(buffer, 0x38, 0x30));
        assertEquals(crawlingBodyColor, pixelColor(buffer, 0x40, 0x30));
    }

    @Test
    void rendersPairAndSingleObjectsWithPaletteSelectionAndTransparentPixels() {
        GPU gpu = new GPU();
        int pairTopColor = 0x112233;
        int pairBottomColor = 0x223344;
        int singleColor = 0x445566;
        int[][] palettes = {
            {0, 0x010203, 0x020304, 0x030405},
            {0, pairTopColor, pairBottomColor, 0x334455},
            {0, singleColor, 0x556677, 0x667788}
        };

        writePatternTile(gpu, 0x20, new int[][] {
            {0, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1}
        });
        writeSolidTile(gpu, 0x21, 2);
        writeSolidTile(gpu, 0x22, 1);
        writeSolidTile(gpu, 0x23, 1);
        writeSolidTile(gpu, 0x24, 1);
        writeSolidTile(gpu, 0x25, 1);

        EntitySpriteDefinition pair = pairDefinition(
                new EntitySpriteDefinition.OamAttribute(0x20, 0x01),
                new EntitySpriteDefinition.OamAttribute(0x22, 0x02));
        EntitySpriteDefinition single = new EntitySpriteDefinition(
                0x91,
                0x00,
                0x4000,
                EntitySpriteDefinition.Shape.SINGLE,
                0,
                List.of(new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x24, 0x02), null)));

        RoomEntity pairEntity = new RoomEntity(0, 0, 0x7A, 24, 32, EntityStatus.ACTIVE, pair, 0, 0, 0, 0, -1, false);
        RoomEntity singleEntity =
                new RoomEntity(1, 1, 0x91, 56, 32, EntityStatus.ACTIVE, single, 0, 0, 0, 0, -1, false);
        byte[] buffer = filledBuffer(0x7A, 0x6B, 0x5C);

        new EntityRenderLayer(snapshot(pairEntity, singleEntity), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));

        assertEquals(pairTopColor, pixelColor(buffer, 17, 16));
        assertEquals(pairBottomColor, pixelColor(buffer, 16, 24));
        assertEquals(palettes[2][1], pixelColor(buffer, 52, 16));
        assertEquals(0x7A6B5C, pixelColor(buffer, 16, 16));
    }

    @Test
    void appliesTheFallingPhaseTwoVisualYOffsetOnlyToOamY() {
        GPU gpu = new GPU();
        int color = 0x123456;
        int[][] palettes = {{0, color, 0, 0}};
        writeSolidTile(gpu, 0x20, 1);
        EntitySpriteDefinition single = new EntitySpriteDefinition(
                0x91,
                0x00,
                0x4000,
                EntitySpriteDefinition.Shape.SINGLE,
                0,
                List.of(new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null)));
        RoomEntity entity = new RoomEntity(0, 0, 0x91, 24, 32, EntityStatus.FALLING, single, 0, 0, 0, 0, -1, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(entity).withVisualYOffset(0, 4), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));

        assertEquals(color, pixelColor(buffer, 20, 20));
        assertEquals(0, pixelColor(buffer, 20, 16));
    }

    @Test
    void rendersAFirstOnlyPairVariantAtTheSingleSpriteOrigin() {
        GPU gpu = new GPU();
        int color = 0x123456;
        int[][] palettes = {{0, color, 0, 0}};
        writeSolidTile(gpu, 0x20, 1);
        EntitySpriteDefinition mixed = new EntitySpriteDefinition(
                0x9B,
                0x07,
                0x729B,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(
                        new EntitySpriteDefinition.Variant(
                                new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
                                new EntitySpriteDefinition.OamAttribute(0x20, 0x00)),
                        new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null)));
        RoomEntity entity = new RoomEntity(0, 0, 0x9B, 24, 32, EntityStatus.ACTIVE, mixed, 1, 0, 0, 0, -1, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(entity), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));

        assertEquals(color, pixelColor(buffer, 20, 16));
        assertEquals(0, pixelColor(buffer, 16, 16));
    }

    private static EntitySpriteDefinition moblinDefinition() {
        return EntitySpriteDefinition.dynamic(
                0x14,
                0x07,
                0x7A95,
                0,
                List.of(List.of(
                        new EntitySpriteDefinition.DynamicSprite(
                                5,
                                -2,
                                new EntitySpriteDefinition.OamAttribute(0x86, 0x16),
                                EntitySpriteDefinition.DynamicSprite.TileSource.GPU,
                                false),
                        new EntitySpriteDefinition.DynamicSprite(
                                8,
                                0,
                                new EntitySpriteDefinition.OamAttribute(0xF0, 0x03),
                                EntitySpriteDefinition.DynamicSprite.TileSource.GPU,
                                false),
                        new EntitySpriteDefinition.DynamicSprite(
                                8,
                                8,
                                new EntitySpriteDefinition.OamAttribute(0x04, 0x03),
                                EntitySpriteDefinition.DynamicSprite.TileSource.GPU,
                                false),
                        new EntitySpriteDefinition.DynamicSprite(
                                0,
                                0,
                                new EntitySpriteDefinition.OamAttribute(0x60, 0x03),
                                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                                true),
                        new EntitySpriteDefinition.DynamicSprite(
                                0,
                                8,
                                new EntitySpriteDefinition.OamAttribute(0x62, 0x03),
                                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                                true))));
    }
}
