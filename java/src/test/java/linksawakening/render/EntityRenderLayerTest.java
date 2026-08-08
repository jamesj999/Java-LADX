package linksawakening.render;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.world.EntityStatus;
import linksawakening.world.EntityRoomLoader;
import linksawakening.world.HookshotChainOam;
import linksawakening.world.PincerBodyOam;
import linksawakening.world.RoomEntity;
import linksawakening.world.RoomEntitySnapshot;
import linksawakening.world.RoomRenderSnapshot;
import linksawakening.world.ScrollController;
import linksawakening.world.WingedOctorokOam;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EntityRenderLayerTest {

    @Test
    void rendersVisibleHookshotChainOamFromTheLiveLinkTileLoad() {
        GPU gpu = new GPU();
        int chainTopColor = 0x123456;
        int chainBottomColor = 0x654321;
        int[][] palettes = {{0, chainTopColor, chainBottomColor, 0}};
        writeSolidTile(gpu, 0x24, 1);
        writeSolidTile(gpu, 0x25, 2);

        RoomEntitySnapshot snapshot = snapshot().withHookshotChainOam(
            HookshotChainOam.entries(0x40, 0x40, 0x40, 0x40, 0));
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot, palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        assertEquals(chainTopColor, pixelColor(buffer, 0x3C, 0x30));
        assertEquals(chainBottomColor, pixelColor(buffer, 0x3C, 0x38));
    }

    @Test
    void rendersPincerBodyOamFromTheEntityTileSnapshot() {
        GPU gpu = new GPU();
        int bodyColor = 0x123456;
        int[][] palettes = {{0, 0, 0, 0}, {0, 0, 0, 0}, {0, bodyColor, 0, 0}};
        writeSolidTile(gpu, 0x6A, 1);
        writeSolidTile(gpu, 0x6B, 1);
        EntitySpriteDefinition hiddenPincer = new EntitySpriteDefinition(
            0xB0, 0x07, 0x542B, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0xFF, 0x00),
                new EntitySpriteDefinition.OamAttribute(0xFF, 0x20))));
        RoomEntity entity = new RoomEntity(3, 0, 0xB0, 0x4C, 0x58,
            EntityStatus.ACTIVE, hiddenPincer, 0);
        RoomEntitySnapshot snapshot = snapshot(null, gpu.snapshotEntityTiles(), entity)
            .withPincerBodyOam(PincerBodyOam.entries(3, 0x40, 0x50, 0x4C, 0x58, 3));
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot, palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        assertEquals(bodyColor, pixelColor(buffer, 0x3C, 0x40));
        assertEquals(bodyColor, pixelColor(buffer, 0x3C, 0x48));
        assertEquals(0, pixelColor(buffer, 0x34, 0x40));
    }

    @Test
    void rendersWingedOctorokGeneratedRectFromTheGpuTileRegion() {
        GPU gpu = new GPU();
        int gpuColor = 0x123456;
        int[][] palettes = {{0, gpuColor, 0, 0}};
        writeSolidTile(gpu, 0x22, 1);
        writeSolidTile(gpu, 0x23, 1);
        EntitySpriteDefinition hiddenWingedOctorok = new EntitySpriteDefinition(
            0xAE, 0x07, 0x562D, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0xFF, 0x00),
                new EntitySpriteDefinition.OamAttribute(0xFF, 0x20))));
        RoomEntity entity = new RoomEntity(2, 0, 0xAE, 0x40, 0x50,
            EntityStatus.ACTIVE, hiddenWingedOctorok, 0);
        RoomEntitySnapshot snapshot = snapshot(null, gpu.snapshotEntityTiles(), entity)
            .withWingedOctorokOam(WingedOctorokOam.entries(2, 0, 0x40, 0x50, 0));
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot, palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

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

        EntitySpriteDefinition definition = EntitySpriteDefinition.dynamic(
            0x14, 0x07, 0x7A95, 0, List.of(List.of(
                new EntitySpriteDefinition.DynamicSprite(5, -2,
                    new EntitySpriteDefinition.OamAttribute(0x86, 0x16),
                    EntitySpriteDefinition.DynamicSprite.TileSource.GPU, false),
                new EntitySpriteDefinition.DynamicSprite(8, 0,
                    new EntitySpriteDefinition.OamAttribute(0xF0, 0x03),
                    EntitySpriteDefinition.DynamicSprite.TileSource.GPU, false),
                new EntitySpriteDefinition.DynamicSprite(8, 8,
                    new EntitySpriteDefinition.OamAttribute(0x04, 0x03),
                    EntitySpriteDefinition.DynamicSprite.TileSource.GPU, false),
                new EntitySpriteDefinition.DynamicSprite(0, 0,
                    new EntitySpriteDefinition.OamAttribute(0x60, 0x03),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true),
                new EntitySpriteDefinition.DynamicSprite(0, 8,
                    new EntitySpriteDefinition.OamAttribute(0x62, 0x03),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true))));
        RoomEntity entity = new RoomEntity(0, 0, 0x14, 24, 32,
            EntityStatus.ACTIVE, definition, 0);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(null, gpu.snapshotEntityTiles(), entity), palettes,
            new ScrollController()).render(new RenderContext(buffer, gpu));

        assertEquals(warningColor, pixelColor(buffer, 14, 21));
        assertEquals(warningColor, pixelColor(buffer, 16, 24));
        assertEquals(swordColor, pixelColor(buffer, 24, 24));
        assertEquals(bodyColor, pixelColor(buffer, 16, 16));
        assertEquals(bodyColor, pixelColor(buffer, 24, 16));
    }

    @Test
    void rendersPairAndSingleObjectsWithPaletteSelectionAndTransparentPixels() {
        GPU gpu = new GPU();
        int pairTopColor = 0x112233;
        int pairBottomColor = 0x223344;
        int singleColor = 0x445566;
        int[][] palettes = {
            { 0, 0x010203, 0x020304, 0x030405 },
            { 0, pairTopColor, pairBottomColor, 0x334455 },
            { 0, singleColor, 0x556677, 0x667788 }
        };

        writePatternTile(gpu, 0x20, new int[][] {
            { 0, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1 }
        });
        writeSolidTile(gpu, 0x21, 2);
        writeSolidTile(gpu, 0x22, 1);
        writeSolidTile(gpu, 0x23, 1);
        writeSolidTile(gpu, 0x24, 1);
        writeSolidTile(gpu, 0x25, 1);

        EntitySpriteDefinition pair = pairDefinition(
            new EntitySpriteDefinition.OamAttribute(0x20, 0x01),
            new EntitySpriteDefinition.OamAttribute(0x22, 0x02));
        EntitySpriteDefinition single = new EntitySpriteDefinition(0x91, 0x00, 0x4000,
            EntitySpriteDefinition.Shape.SINGLE, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x24, 0x02), null)));

        RoomEntity pairEntity = new RoomEntity(0, 0, 0x7A, 24, 32, EntityStatus.ACTIVE,
            pair, 0, 0);
        RoomEntity singleEntity = new RoomEntity(1, 1, 0x91, 56, 32, EntityStatus.ACTIVE,
            single, 0, 0);
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
        EntitySpriteDefinition single = new EntitySpriteDefinition(0x91, 0x00, 0x4000,
            EntitySpriteDefinition.Shape.SINGLE, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null)));
        RoomEntity entity = new RoomEntity(0, 0, 0x91, 24, 32, EntityStatus.FALLING,
            single, 0, 0);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(entity).withVisualYOffset(0, 4), palettes,
            new ScrollController()).render(new RenderContext(buffer, gpu));

        assertEquals(color, pixelColor(buffer, 20, 20));
        assertEquals(0, pixelColor(buffer, 20, 16));
    }

    @Test
    void rendersAFirstOnlyPairVariantAtTheSingleSpriteOrigin() {
        GPU gpu = new GPU();
        int color = 0x123456;
        int[][] palettes = {{0, color, 0, 0}};
        writeSolidTile(gpu, 0x20, 1);
        EntitySpriteDefinition mixed = new EntitySpriteDefinition(0x9B, 0x07, 0x729B,
            EntitySpriteDefinition.Shape.PAIR, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x00)),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null)));
        RoomEntity entity = new RoomEntity(0, 0, 0x9B, 24, 32, EntityStatus.ACTIVE,
            mixed, 1, 0);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(entity), palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        assertEquals(color, pixelColor(buffer, 20, 16));
        assertEquals(0, pixelColor(buffer, 16, 16));
    }

    @Test
    void rendersPairoddVariantThreeAsTwoShiftedPairs() {
        GPU gpu = new GPU();
        int firstPairLeft = 0x112233;
        int firstPairRight = 0x223344;
        int secondPairLeft = 0x334455;
        int secondPairRight = 0x445566;
        int[][] palettes = {
            {0, 0, 0, 0},
            {0, firstPairLeft, 0, 0},
            {0, firstPairRight, 0, 0},
            {0, secondPairLeft, 0, 0},
            {0, secondPairRight, 0, 0}
        };
        writeSolidTile(gpu, 0x20, 1);
        writeSolidTile(gpu, 0x22, 1);
        writeSolidTile(gpu, 0x24, 1);
        writeSolidTile(gpu, 0x26, 1);

        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            variants.add(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0xFF, 0x00),
                new EntitySpriteDefinition.OamAttribute(0xFF, 0x00)));
        }
        variants.set(6, new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x20, 0x01),
            new EntitySpriteDefinition.OamAttribute(0x22, 0x02)));
        variants.set(7, new EntitySpriteDefinition.Variant(
            new EntitySpriteDefinition.OamAttribute(0x24, 0x23),
            new EntitySpriteDefinition.OamAttribute(0x26, 0x24)));
        EntitySpriteDefinition pairodd = new EntitySpriteDefinition(0x57, 0x04, 0x5DD1,
            EntitySpriteDefinition.Shape.PAIR, 0, variants);
        RoomEntity entity = new RoomEntity(0, 0, 0x57, 40, 32, EntityStatus.ACTIVE,
            pairodd, 3, 0);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(entity), palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        // The handler shifts the pair origins to entityX - 8 and entityX + 8.
        assertEquals(firstPairLeft, pixelColor(buffer, 24, 16));
        assertEquals(firstPairRight, pixelColor(buffer, 32, 16));
        assertEquals(secondPairLeft, pixelColor(buffer, 40, 16));
        assertEquals(secondPairRight, pixelColor(buffer, 48, 16));
    }

    @Test
    void rendersRectangleOffsetsAndPerEntityTileOffset() {
        GPU gpu = new GPU();
        int color = 0x123456;
        int secondColor = 0x654321;
        int[][] palettes = {{0, color, secondColor, 0}};
        writeSolidTile(gpu, 0x20, 1);
        writeSolidTile(gpu, 0x21, 1);
        writeSolidTile(gpu, 0x22, 2);
        writeSolidTile(gpu, 0x23, 2);

        EntitySpriteDefinition rectangle = new EntitySpriteDefinition(0x77, 0x06, 0x5C51,
            EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(), List.of(List.of(
                new EntitySpriteDefinition.RectangleSprite(-8, -8,
                    new EntitySpriteDefinition.OamAttribute(0x00, 0x00)),
                new EntitySpriteDefinition.RectangleSprite(-8, 0,
                    new EntitySpriteDefinition.OamAttribute(0x02, 0x00)))));
        RoomEntity entity = new RoomEntity(0, 0, 0x77, 24, 32, EntityStatus.ACTIVE,
            rectangle, 0, 0, 0x20);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(entity), palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        assertEquals(color, pixelColor(buffer, 8, 8));
        assertEquals(secondColor, pixelColor(buffer, 16, 8));
        assertEquals(color, pixelColor(buffer, 8, 16));
        assertEquals(secondColor, pixelColor(buffer, 16, 16));
    }

    @Test
    void appliesFollowerDisplayListOverrideFromRoomSpriteSelection() {
        GPU gpu = new GPU();
        int atHomeColor = 0x112233;
        int followingColor = 0x445566;
        int[][] palettes = {{0, atHomeColor, followingColor, 0}};
        writeSolidTile(gpu, 0x20, 1);
        writeSolidTile(gpu, 0x21, 1);
        writeSolidTile(gpu, 0x22, 2);
        writeSolidTile(gpu, 0x23, 2);

        EntitySpriteDefinition atHome = pairDefinition(
            new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
            new EntitySpriteDefinition.OamAttribute(0x20, 0x00));
        EntitySpriteDefinition following = pairDefinition(
            new EntitySpriteDefinition.OamAttribute(0x22, 0x00),
            new EntitySpriteDefinition.OamAttribute(0x22, 0x00));
        EntitySpriteSelection selection = new EntitySpriteSelection(
            linksawakening.world.EntityRoomLoader.RoomTable.OVERWORLD, 0, 0,
            new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
            .withSpriteOverride(0x6D, following);
        RoomEntity entity = new RoomEntity(0, 0, 0x6D, 24, 32, EntityStatus.ACTIVE,
            atHome, 0, 0);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(selection, entity), palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        assertEquals(followingColor, pixelColor(buffer, 16, 16));
    }

    @Test
    void entityFlipXSwapsPairColumnsAndXorsDisplayListAttributes() {
        GPU gpu = new GPU();
        int leftColor = 0x112233;
        int rightColor = 0x445566;
        int[][] palettes = { { 0, leftColor, rightColor, 0 } };
        writePatternTile(gpu, 0x20, new int[][] {
            { 1, 1, 1, 1, 1, 1, 1, 2 },
            { 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1 }
        });
        writeSolidTile(gpu, 0x21, 1);
        writeSolidTile(gpu, 0x22, 2);
        writeSolidTile(gpu, 0x23, 2);

        EntitySpriteDefinition pair = pairDefinition(
            new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
            new EntitySpriteDefinition.OamAttribute(0x22, 0x00));
        RoomEntity entity = new RoomEntity(0, 0, 0x01, 24, 32, EntityStatus.ACTIVE,
            pair, 0, 0x20);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(entity), palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        assertEquals(rightColor, pixelColor(buffer, 16, 16));
        assertEquals(rightColor, pixelColor(buffer, 24, 16));
        assertEquals(leftColor, pixelColor(buffer, 31, 16));
    }

    @Test
    void paletteFlipAttributeSelectsRomObjectPaletteFour() {
        GPU gpu = new GPU();
        writeSolidTile(gpu, 0x20, 1);
        int[][] palettes = new int[6][4];
        for (int palette = 0; palette < palettes.length; palette++) {
            palettes[palette] = new int[] {0, 0x100000 + palette, 0, 0};
        }

        EntitySpriteDefinition pair = pairDefinition(
            new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
            new EntitySpriteDefinition.OamAttribute(0x20, 0x00));
        RoomEntity entity = new RoomEntity(0, 0, 0x01, 24, 32, EntityStatus.ACTIVE,
            pair, 0, 0x10);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(entity), palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        assertEquals(palettes[4][1], pixelColor(buffer, 16, 16));
    }

    @Test
    void clipsEntitiesAndUsesCurrentRoomOffsetDuringScroll() {
        GPU gpu = new GPU();
        int color = 0x123456;
        int[][] palettes = { { 0, color, 0, 0 } };
        writeSolidTile(gpu, 0x30, 1);
        writeSolidTile(gpu, 0x31, 1);
        EntitySpriteDefinition single = new EntitySpriteDefinition(0x02, 0x00, 0x4000,
            EntitySpriteDefinition.Shape.SINGLE, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x30, 0x00), null)));
        RoomEntity entity = new RoomEntity(0, 0, 0x02, 4, 12, EntityStatus.ACTIVE,
            single, 0, 0);
        RoomEntitySnapshot snapshot = snapshot(entity);
        ScrollController scroll = new ScrollController();
        scroll.start(ScrollController.RIGHT, 0, 0, null, 160);
        scroll.tick(8);

        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(snapshot, palettes, scroll).render(new RenderContext(buffer, gpu));

        assertEquals(color, pixelColor(buffer, 152, 0));
        assertEquals(color, pixelColor(buffer, 159, 7));
    }

    @Test
    void rendersPreviousRoomEntitiesWithTheExitingRoomOffset() {
        GPU gpu = new GPU();
        int previousColor = 0x123456;
        int[][] palettes = { { 0, previousColor, 0, 0 } };
        writeSolidTile(gpu, 0x30, 1);
        writeSolidTile(gpu, 0x31, 1);
        EntitySpriteDefinition single = new EntitySpriteDefinition(0x02, 0x00, 0x4000,
            EntitySpriteDefinition.Shape.SINGLE, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x30, 0x00), null)));
        RoomEntity entity = new RoomEntity(0, 0, 0x02, 8, 16, EntityStatus.ACTIVE,
            single, 0, 0);
        EntitySpriteSelection selection = new EntitySpriteSelection(
            linksawakening.world.EntityRoomLoader.RoomTable.OVERWORLD, 0, 0,
            new int[] { 0xFF, 0xFF, 0xFF, 0xFF }, true, palettes);
        RoomEntitySnapshot previousEntities = snapshot(selection, entity);
        RoomEntitySnapshot currentEntities = snapshot(selection);
        RoomRenderSnapshot previousRoom = new RoomRenderSnapshot(new int[] { 0 }, new int[] { 0 },
            palettes, previousEntities);
        ScrollController scroll = new ScrollController();
        scroll.start(ScrollController.LEFT, 0, 0, previousRoom, 160);
        scroll.tick(8);

        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(currentEntities, palettes, scroll)
            .render(new RenderContext(buffer, gpu));

        assertEquals(previousColor, pixelColor(buffer, 12, 0));
    }

    @Test
    void preservesPreviousRoomEntityTilesAfterDestinationSheetsOverwriteVram() {
        GPU gpu = new GPU();
        int previousColor = 0x123456;
        int currentColor = 0x654321;
        int[][] palettes = { { 0, previousColor, currentColor, 0 } };
        writeSolidTile(gpu, 0x40, 1);
        writeSolidTile(gpu, 0x41, 1);
        EntitySpriteTileSnapshot previousTiles = gpu.snapshotEntityTiles();
        writeSolidTile(gpu, 0x40, 2);
        writeSolidTile(gpu, 0x41, 2);
        EntitySpriteTileSnapshot currentTiles = gpu.snapshotEntityTiles();

        EntitySpriteDefinition single = new EntitySpriteDefinition(0x02, 0x00, 0x4000,
            EntitySpriteDefinition.Shape.SINGLE, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x40, 0x00), null)));
        RoomEntity previousEntity = new RoomEntity(0, 0, 0x02, 8, 16, EntityStatus.ACTIVE,
            single, 0, 0);
        RoomEntity currentEntity = new RoomEntity(0, 0, 0x02, 152, 16, EntityStatus.ACTIVE,
            single, 0, 0);
        EntitySpriteSelection selection = new EntitySpriteSelection(
            linksawakening.world.EntityRoomLoader.RoomTable.OVERWORLD, 0, 0,
            new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes);
        RoomEntitySnapshot previousEntities = snapshot(selection, previousTiles, previousEntity);
        RoomEntitySnapshot currentEntities = snapshot(selection, currentTiles, currentEntity);
        RoomRenderSnapshot previousRoom = new RoomRenderSnapshot(new int[] { 0 }, new int[] { 0 },
            palettes, previousEntities);
        ScrollController scroll = new ScrollController();
        scroll.start(ScrollController.LEFT, 0, 0, previousRoom, 160);
        scroll.tick(8);

        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(currentEntities, palettes, scroll)
            .render(new RenderContext(buffer, gpu));

        assertEquals(previousColor, pixelColor(buffer, 12, 0));
        assertEquals(currentColor, pixelColor(buffer, 140, 0));
    }

    @Test
    void rendersEveryRockAndArrowPairVariantWithItsRomPaletteAndColumns() {
        GPU gpu = new GPU();
        int[][] palettes = new int[8][4];
        for (int palette = 0; palette < palettes.length; palette++) {
            palettes[palette] = new int[] {0, 0x100000 + palette * 0x0100,
                0x200000 + palette * 0x0100, 0x300000 + palette * 0x0100};
        }

        List<EntitySpriteDefinition.Variant> rockVariants = new ArrayList<>();
        for (int variant = 0; variant < 2; variant++) {
            int firstTile = 0x20 + variant * 4;
            writeSolidTile(gpu, firstTile, 1);
            writeSolidTile(gpu, firstTile + 1, 1);
            writeSolidTile(gpu, firstTile + 2, 1);
            writeSolidTile(gpu, firstTile + 3, 1);
            rockVariants.add(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(firstTile, variant + 1),
                new EntitySpriteDefinition.OamAttribute(firstTile + 2, variant + 2)));
        }
        EntitySpriteDefinition rock = new EntitySpriteDefinition(
            0x0A, 0x03, 0x6A1E, EntitySpriteDefinition.Shape.PAIR, 0, rockVariants);

        List<EntitySpriteDefinition.Variant> arrowVariants = new ArrayList<>();
        for (int variant = 0; variant < 4; variant++) {
            int firstTile = 0x30 + variant * 4;
            writeSolidTile(gpu, firstTile, 1);
            writeSolidTile(gpu, firstTile + 1, 1);
            writeSolidTile(gpu, firstTile + 2, 1);
            writeSolidTile(gpu, firstTile + 3, 1);
            arrowVariants.add(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(firstTile, variant + 1),
                new EntitySpriteDefinition.OamAttribute(firstTile + 2, variant + 2)));
        }
        EntitySpriteDefinition arrow = new EntitySpriteDefinition(
            0x0C, 0x03, 0x6BC6, EntitySpriteDefinition.Shape.PAIR, 0, arrowVariants);

        for (int variant = 0; variant < rock.variantCount(); variant++) {
            byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
            RoomEntity entity = new RoomEntity(0, -1, 0x0A, 24, 32, EntityStatus.ACTIVE,
                rock, variant, 0);
            new EntityRenderLayer(snapshot(entity), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));
            assertEquals(palettes[variant + 1][1], pixelColor(buffer, 16, 16));
            assertEquals(palettes[variant + 2][1], pixelColor(buffer, 24, 16));
            assertEquals(palettes[variant + 1][1], pixelColor(buffer, 16, 24));
            assertEquals(palettes[variant + 2][1], pixelColor(buffer, 24, 24));
        }

        for (int variant = 0; variant < arrow.variantCount(); variant++) {
            byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
            RoomEntity entity = new RoomEntity(0, -1, 0x0C, 64, 32, EntityStatus.ACTIVE,
                arrow, variant, 0);
            new EntityRenderLayer(snapshot(entity), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));
            assertEquals(palettes[variant + 1][1], pixelColor(buffer, 56, 16));
            assertEquals(palettes[variant + 2][1], pixelColor(buffer, 64, 16));
            assertEquals(palettes[variant + 1][1], pixelColor(buffer, 56, 24));
            assertEquals(palettes[variant + 2][1], pixelColor(buffer, 64, 24));
        }
    }

    @Test
    void appliesProjectileTileOffsetZAndEightBySixteenEntityFlips() {
        GPU gpu = new GPU();
        int firstTop = 0x40;
        int firstBottom = 0x41;
        int secondTop = 0x42;
        int secondBottom = 0x43;
        writeSolidTile(gpu, firstTop, 1);
        writeSolidTile(gpu, firstBottom, 2);
        writeSolidTile(gpu, secondTop, 3);
        writeSolidTile(gpu, secondBottom, 1);

        EntitySpriteDefinition arrow = new EntitySpriteDefinition(
            0x0C, 0x03, 0x6BC6, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
                new EntitySpriteDefinition.OamAttribute(0x22, 0x00))));
        int[][] palettes = {
            {0, 0x112233, 0x223344, 0x334455}
        };
        RoomEntity entity = new RoomEntity(0, -1, 0x0C, 40, 32, EntityStatus.ACTIVE,
            arrow, 0, 0x60, 0x20, 4);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(entity), palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        // The tile offset moves $20/$22 to $40/$42.  X-flip swaps the pair
        // columns; Y-flip swaps each 8x8 half of the 8x16 sprites.
        assertEquals(palettes[0][2], pixelColor(buffer, 40, 12));
        assertEquals(palettes[0][1], pixelColor(buffer, 40, 20));
        assertEquals(palettes[0][1], pixelColor(buffer, 32, 12));
        assertEquals(palettes[0][3], pixelColor(buffer, 32, 20));
    }

    @Test
    void rendersBurningFireOverlayWithRomPhaseTransformAndTileOffset() {
        GPU gpu = new GPU();
        int bodyColor = 0x112233;
        int firePaletteTwo = 0x445566;
        int firePaletteFour = 0x778899;
        int[][] palettes = {
            {0, 0, bodyColor, 0},
            {0, 0, 0, 0},
            {0, firePaletteTwo, 0, 0},
            {0, 0, 0, 0},
            {0, firePaletteFour, 0, 0},
            {0, 0, 0, 0}
        };
        writeSolidTile(gpu, 0x40, 2);
        writeSolidTile(gpu, 0x41, 2);
        writePatternTile(gpu, 0x44, new int[][] {
            {1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0}
        });
        writeSolidTile(gpu, 0x45, 0);

        EntitySpriteDefinition body = new EntitySpriteDefinition(
            0x09, 0x03, 0x57FB, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x30, 0x00),
                new EntitySpriteDefinition.OamAttribute(0x30, 0x00))));
        EntitySpriteDefinition fire = new EntitySpriteDefinition(
            0x00, 0x03, 0x4C44, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x34, 0x02),
                    new EntitySpriteDefinition.OamAttribute(0x34, 0x22)),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x34, 0x14),
                    new EntitySpriteDefinition.OamAttribute(0x34, 0x34))));
        EntitySpriteSelection selection = new EntitySpriteSelection(
            EntityRoomLoader.RoomTable.OVERWORLD, 0, 0,
            new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
            .withBurningSpriteDefinition(fire);
        RoomEntity entity = new RoomEntity(0, 0, 0x09, 24, 32, EntityStatus.BURNING,
            body, 0, 0x20, 0x10, 4);

        byte[] phaseZero = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(snapshot(selection, entity), palettes, new ScrollController(), 0)
            .render(new RenderContext(phaseZero, gpu));
        assertEquals(firePaletteTwo, pixelColor(phaseZero, 16, 12));
        assertEquals(firePaletteTwo, pixelColor(phaseZero, 31, 12));
        assertEquals(bodyColor, pixelColor(phaseZero, 17, 12));

        byte[] phaseEight = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(snapshot(selection, entity), palettes, new ScrollController(), 8)
            .render(new RenderContext(phaseEight, gpu));
        assertEquals(firePaletteFour, pixelColor(phaseEight, 16, 12));
        assertEquals(firePaletteFour, pixelColor(phaseEight, 31, 12));
        assertEquals(bodyColor, pixelColor(phaseEight, 17, 12));
    }

    @Test
    void rendersFloatingItemRectangleFromSelectionMetadataAfterMainList() {
        GPU gpu = new GPU();
        int overlayColor = 0x556677;
        int[][] palettes = {{0, overlayColor, 0, 0}};
        writeSolidTile(gpu, 0x22, 1);

        EntitySpriteDefinition overlay = new EntitySpriteDefinition(
            0x86, 0x06, 0x7AEB, EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(),
            List.of(
                List.of(
                    new EntitySpriteDefinition.RectangleSprite(0, -4,
                        new EntitySpriteDefinition.OamAttribute(0x22, 0x00)),
                    new EntitySpriteDefinition.RectangleSprite(0, 12,
                        new EntitySpriteDefinition.OamAttribute(0x22, 0x20))),
                List.of(
                    new EntitySpriteDefinition.RectangleSprite(0, -4,
                        new EntitySpriteDefinition.OamAttribute(0x22, 0x40)),
                    new EntitySpriteDefinition.RectangleSprite(0, 12,
                        new EntitySpriteDefinition.OamAttribute(0x22, 0x60)))));
        EntitySpriteSelection selection = new EntitySpriteSelection(
            EntityRoomLoader.RoomTable.OVERWORLD, 0, 0,
            new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
            .withSpriteOverlay(0x86, overlay);
        EntitySpriteDefinition main = new EntitySpriteDefinition(
            0x86, 0x06, 0x7ADD, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0xFF, 0x00), null)));
        RoomEntity entity = new RoomEntity(0, 0, 0x86, 24, 32, EntityStatus.ACTIVE,
            main, 0, 0, 0, 0);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(selection, entity), palettes, new ScrollController(), 0)
            .render(new RenderContext(buffer, gpu));

        assertEquals(overlayColor, pixelColor(buffer, 12, 16));
        assertEquals(overlayColor, pixelColor(buffer, 28, 16));
    }

    @Test
    void doesNotRenderFloatingOverlayWhenMainDefinitionIsUnsupported() {
        GPU gpu = new GPU();
        int[][] palettes = {{0, 0x556677, 0, 0}};
        writeSolidTile(gpu, 0x22, 1);
        EntitySpriteDefinition overlay = new EntitySpriteDefinition(
            0x86, 0x06, 0x7AEB, EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(),
            List.of(List.of(new EntitySpriteDefinition.RectangleSprite(0, -4,
                new EntitySpriteDefinition.OamAttribute(0x22, 0x00)))));
        EntitySpriteSelection selection = new EntitySpriteSelection(
            EntityRoomLoader.RoomTable.COLOR_DUNGEON, 0, 0, new int[0], false, palettes)
            .withSpriteOverlay(0x86, overlay);
        RoomEntity entity = new RoomEntity(0, 0, 0x86, 24, 32, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0x86), -1, 0, 0, 0);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(selection, entity), palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        assertEquals(0, pixelColor(buffer, 12, 16));
    }

    @Test
    void sideScrollingMovesMixedSingleDownFourPixelsButLeavesPairsUnchanged() {
        GPU gpu = new GPU();
        int color = 0x556677;
        int[][] palettes = {{0, color, 0, 0}};
        writePatternTile(gpu, 0x20, new int[][] {
            {1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0}
        });
        EntitySpriteDefinition definition = new EntitySpriteDefinition(
            0x86, 0x06, 0x7ADD, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
                    new EntitySpriteDefinition.OamAttribute(0x20, 0x00))));
        RoomEntity mixedSingle = new RoomEntity(0, 0, 0x86, 24, 32,
            EntityStatus.ACTIVE, definition, 0, 0);
        RoomEntity pair = new RoomEntity(1, 1, 0x86, 56, 32,
            EntityStatus.ACTIVE, definition, 5, 0);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(mixedSingle, pair).withSideScrolling(true),
            palettes, new ScrollController()).render(new RenderContext(buffer, gpu));

        assertEquals(color, pixelColor(buffer, 20, 12));
        assertEquals(0, pixelColor(buffer, 20, 16));
        assertEquals(color, pixelColor(buffer, 48, 16));
    }

    @Test
    void rendersPowerRecoilDeathRectangleInsteadOfBodyAndSkipsHiddenEntry() {
        GPU gpu = new GPU();
        int bodyColor = 0x112233;
        int normalColor = 0x445566;
        int powerVariantZeroColor = 0x556677;
        int powerVariantThreeColor = 0x778899;
        int backgroundColor = 0x7A6B5C;
        int[][] palettes = {
            {0, bodyColor, 0, 0},
            {0, normalColor, 0, 0},
            {0, powerVariantThreeColor, powerVariantZeroColor, 0}
        };
        writeSolidTile(gpu, 0x20, 1);
        writeSolidTile(gpu, 0x21, 1);
        writeSolidTile(gpu, 0x22, 1);
        writeSolidTile(gpu, 0x23, 1);
        writeSolidTile(gpu, 0x24, 2);
        writeSolidTile(gpu, 0x25, 2);
        writeSolidTile(gpu, 0x26, 1);
        writeSolidTile(gpu, 0x27, 1);
        // An accidental 8x16 draw can read the paired $FE/$FF tiles.
        writeSolidTile(gpu, 0xFE, 1);
        writeSolidTile(gpu, 0xFF, 1);

        EntitySpriteDefinition body = new EntitySpriteDefinition(
            0x09, 0x03, 0x57FB, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
                new EntitySpriteDefinition.OamAttribute(0x20, 0x00))));
        List<List<EntitySpriteDefinition.RectangleSprite>> normalVariants = List.of(
            List.of(new EntitySpriteDefinition.RectangleSprite(0, 0,
                new EntitySpriteDefinition.OamAttribute(0x22, 0x01))),
            List.of(new EntitySpriteDefinition.RectangleSprite(0, 0,
                new EntitySpriteDefinition.OamAttribute(0x22, 0x01))),
            List.of(new EntitySpriteDefinition.RectangleSprite(0, 0,
                new EntitySpriteDefinition.OamAttribute(0x22, 0x01))),
            List.of(new EntitySpriteDefinition.RectangleSprite(0, 0,
                new EntitySpriteDefinition.OamAttribute(0x22, 0x01))));
        EntitySpriteDefinition normal = new EntitySpriteDefinition(
            0x00, 0x03, 0x5488, EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(),
            normalVariants);
        List<List<EntitySpriteDefinition.RectangleSprite>> powerVariants = List.of(
            List.of(new EntitySpriteDefinition.RectangleSprite(0, 0,
                new EntitySpriteDefinition.OamAttribute(0x24, 0x02))),
            List.of(new EntitySpriteDefinition.RectangleSprite(0, 0,
                new EntitySpriteDefinition.OamAttribute(0x24, 0x02))),
            List.of(new EntitySpriteDefinition.RectangleSprite(0, 0,
                new EntitySpriteDefinition.OamAttribute(0x24, 0x02))),
            List.of(
                new EntitySpriteDefinition.RectangleSprite(0, 0,
                    new EntitySpriteDefinition.OamAttribute(0x26, 0x02)),
                new EntitySpriteDefinition.RectangleSprite(0, 8,
                    new EntitySpriteDefinition.OamAttribute(0xFF, 0x00))));
        EntitySpriteDefinition power = new EntitySpriteDefinition(
            0x00, 0x03, 0x54C8, EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(),
            powerVariants);
        EntitySpriteSelection selection = new EntitySpriteSelection(
            EntityRoomLoader.RoomTable.OVERWORLD, 0, 0,
            new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
            .withDeathSpriteDefinitions(normal, power);
        RoomEntity entity = new RoomEntity(0, 0, 0x09, 24, 32, EntityStatus.DYING,
            body, 0, 0, 0, 0, 3, true);
        byte[] buffer = filledBuffer(0x7A, 0x6B, 0x5C);

        new EntityRenderLayer(snapshot(selection, entity), palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        // Entity OAM positions are adjusted by $08/$10 before reaching the framebuffer.
        assertEquals(powerVariantThreeColor, pixelColor(buffer, 16, 16));
        assertEquals(backgroundColor, pixelColor(buffer, 24, 16));
        assertEquals(backgroundColor, pixelColor(buffer, 24, 24));
    }

    @Test
    void fallsBackToBodyWhenDeathSpriteVariantIsUnset() {
        GPU gpu = new GPU();
        int bodyColor = 0x112233;
        int normalColor = 0x445566;
        int powerColor = 0x778899;
        int[][] palettes = {
            {0, bodyColor, 0, 0},
            {0, normalColor, 0, 0},
            {0, powerColor, 0, 0}
        };
        writeSolidTile(gpu, 0x20, 1);
        writeSolidTile(gpu, 0x21, 1);
        writeSolidTile(gpu, 0x28, 1);
        writeSolidTile(gpu, 0x29, 1);
        writeSolidTile(gpu, 0x2A, 1);
        writeSolidTile(gpu, 0x2B, 1);

        EntitySpriteDefinition body = pairDefinition(
            new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
            new EntitySpriteDefinition.OamAttribute(0x20, 0x00));
        EntitySpriteSelection selection = new EntitySpriteSelection(
            EntityRoomLoader.RoomTable.OVERWORLD, 0, 0,
            new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
            .withDeathSpriteDefinitions(
                rectangleDefinition(0x5488, 0x28, 0x01),
                rectangleDefinition(0x54C8, 0x2A, 0x02));
        RoomEntity entity = new RoomEntity(0, 0, 0x09, 24, 32, EntityStatus.DYING,
            body, 0, 0, 0, 0, -1, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(selection, entity), palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        assertEquals(bodyColor, pixelColor(buffer, 16, 16));
    }

    @Test
    void rendersNormalDeathDefinitionWhenPowerRecoilIsFalse() {
        GPU gpu = new GPU();
        int bodyColor = 0x112233;
        int normalColor = 0x445566;
        int powerColor = 0x778899;
        int[][] palettes = {
            {0, bodyColor, 0, 0},
            {0, normalColor, 0, 0},
            {0, powerColor, 0, 0}
        };
        writeSolidTile(gpu, 0x20, 1);
        writeSolidTile(gpu, 0x21, 1);
        writeSolidTile(gpu, 0x2C, 1);
        writeSolidTile(gpu, 0x2D, 1);
        writeSolidTile(gpu, 0x2E, 1);
        writeSolidTile(gpu, 0x2F, 1);

        EntitySpriteDefinition body = pairDefinition(
            new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
            new EntitySpriteDefinition.OamAttribute(0x20, 0x00));
        EntitySpriteSelection selection = new EntitySpriteSelection(
            EntityRoomLoader.RoomTable.OVERWORLD, 0, 0,
            new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
            .withDeathSpriteDefinitions(
                rectangleDefinition(0x5488, 0x2C, 0x01),
                rectangleDefinition(0x54C8, 0x2E, 0x02));
        RoomEntity entity = new RoomEntity(0, 0, 0x09, 24, 32, EntityStatus.DYING,
            body, 0, 0, 0, 0, 3, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(selection, entity), palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        assertEquals(normalColor, pixelColor(buffer, 16, 16));
    }

    @Test
    void deathDefinitionTakesPrecedenceWhileActiveEntityUsesSpriteOverride() {
        GPU gpu = new GPU();
        int bodyColor = 0x112233;
        int overrideColor = 0x445566;
        int deathColor = 0x778899;
        int[][] palettes = {
            {0, bodyColor, 0, 0},
            {0, overrideColor, 0, 0},
            {0, deathColor, 0, 0}
        };
        writeSolidTile(gpu, 0x30, 1);
        writeSolidTile(gpu, 0x31, 1);
        writeSolidTile(gpu, 0x34, 1);
        writeSolidTile(gpu, 0x35, 1);
        writeSolidTile(gpu, 0x38, 1);
        writeSolidTile(gpu, 0x39, 1);

        EntitySpriteDefinition body = pairDefinition(
            new EntitySpriteDefinition.OamAttribute(0x30, 0x00),
            new EntitySpriteDefinition.OamAttribute(0x30, 0x00));
        EntitySpriteDefinition override = new EntitySpriteDefinition(
            0x09, 0x03, 0x4000, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x38, 0x01),
                new EntitySpriteDefinition.OamAttribute(0x38, 0x01))));
        EntitySpriteSelection selection = new EntitySpriteSelection(
            EntityRoomLoader.RoomTable.OVERWORLD, 0, 0,
            new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
            .withDeathSpriteDefinitions(
                rectangleDefinition(0x5488, 0x32, 0x01),
                rectangleDefinition(0x54C8, 0x34, 0x02))
            .withSpriteOverride(0x09, override);
        RoomEntity dying = new RoomEntity(0, 0, 0x09, 24, 32, EntityStatus.DYING,
            body, 0, 0, 0, 0, 3, true);
        RoomEntity active = new RoomEntity(1, 1, 0x09, 64, 32, EntityStatus.ACTIVE,
            body, 0, 0, 0, 0, -1, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(selection, dying, active), palettes, new ScrollController())
            .render(new RenderContext(buffer, gpu));

        assertEquals(deathColor, pixelColor(buffer, 16, 16));
        assertEquals(overrideColor, pixelColor(buffer, 56, 16));
    }

    @Test
    void shiftsOnlyTheNormalBombVisualYByTheSourceTwoPixels() {
        GPU gpu = new GPU();
        int bombColor = 0x123456;
        int[][] palettes = {{0, bombColor, 0, 0}};
        writeSolidTile(gpu, 0x20, 1);
        writeSolidTile(gpu, 0x21, 1);

        EntitySpriteDefinition normal = new EntitySpriteDefinition(
            0x02, 0x03, 0x652E, EntitySpriteDefinition.Shape.SINGLE, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null)));
        EntitySpriteDefinition warning = new EntitySpriteDefinition(
            0x02, 0x03, 0x5484, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
                new EntitySpriteDefinition.OamAttribute(0x20, 0x00))));
        EntitySpriteDefinition explosion = new EntitySpriteDefinition(
            0x02, 0x03, 0x6530, EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(),
            List.of(List.of(new EntitySpriteDefinition.RectangleSprite(0, 0,
                new EntitySpriteDefinition.OamAttribute(0x20, 0x00)))));

        byte[] normalBuffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(snapshot(new RoomEntity(0, 0, 0x02, 24, 32,
            EntityStatus.ACTIVE, normal, 0)), palettes, new ScrollController())
            .render(new RenderContext(normalBuffer, gpu));
        assertEquals(bombColor, pixelColor(normalBuffer, 20, 18));
        assertEquals(0, pixelColor(normalBuffer, 20, 16));

        byte[] warningBuffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(snapshot(new RoomEntity(0, 0, 0x02, 24, 32,
            EntityStatus.ACTIVE, warning, 0)), palettes, new ScrollController())
            .render(new RenderContext(warningBuffer, gpu));
        assertEquals(bombColor, pixelColor(warningBuffer, 16, 16));
        assertEquals(0, pixelColor(warningBuffer, 16, 14));

        byte[] explosionBuffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(snapshot(new RoomEntity(0, 0, 0x02, 24, 32,
            EntityStatus.ACTIVE, explosion, 0)), palettes, new ScrollController())
            .render(new RenderContext(explosionBuffer, gpu));
        assertEquals(bombColor, pixelColor(explosionBuffer, 16, 16));
        assertEquals(0, pixelColor(explosionBuffer, 16, 14));
    }

    private static EntitySpriteDefinition pairDefinition(EntitySpriteDefinition.OamAttribute first,
                                                          EntitySpriteDefinition.OamAttribute second) {
        return new EntitySpriteDefinition(0x7A, 0x06, 0x5C89,
            EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(first, second)));
    }

    private static EntitySpriteDefinition rectangleDefinition(int address, int tile,
                                                               int attributes) {
        List<EntitySpriteDefinition.RectangleSprite> rectangle = List.of(
            new EntitySpriteDefinition.RectangleSprite(0, 0,
                new EntitySpriteDefinition.OamAttribute(tile, attributes)));
        return new EntitySpriteDefinition(0x00, 0x03, address,
            EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(),
            List.of(rectangle, rectangle, rectangle, rectangle));
    }

    private static RoomEntitySnapshot snapshot(RoomEntity... entities) {
        return snapshot(null, entities);
    }

    private static RoomEntitySnapshot snapshot(EntitySpriteSelection selection,
                                                RoomEntity... entities) {
        List<RoomEntity> slots = new ArrayList<>();
        slots.addAll(List.of(entities));
        while (slots.size() < 0x10) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots, selection);
    }

    private static RoomEntitySnapshot snapshot(EntitySpriteSelection selection,
                                                EntitySpriteTileSnapshot tiles,
                                                RoomEntity... entities) {
        return snapshot(selection, entities).withSpriteTiles(tiles);
    }

    private static byte[] filledBuffer(int red, int green, int blue) {
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        for (int i = 0; i < buffer.length; i += 4) {
            buffer[i] = (byte) red;
            buffer[i + 1] = (byte) green;
            buffer[i + 2] = (byte) blue;
            buffer[i + 3] = (byte) 0xFF;
        }
        return buffer;
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

    private static void writePatternTile(GPU gpu, int tileIndex, int[][] pixels) {
        int base = tileIndex * GPU.TILE_DATA_SIZE;
        for (int y = 0; y < 8; y++) {
            int low = 0;
            int high = 0;
            for (int x = 0; x < 8; x++) {
                int color = pixels[y][x];
                low |= (color & 0x01) << (7 - x);
                high |= ((color >>> 1) & 0x01) << (7 - x);
            }
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
