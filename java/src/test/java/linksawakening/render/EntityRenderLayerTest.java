package linksawakening.render;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.world.EntityStatus;
import linksawakening.world.EntityRoomLoader;
import linksawakening.world.RoomEntity;
import linksawakening.world.RoomEntitySnapshot;
import linksawakening.world.RoomRenderSnapshot;
import linksawakening.world.ScrollController;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EntityRenderLayerTest {

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
    void rendersPowerRecoilDeathRectangleInsteadOfBodyAndSkipsHiddenEntry() {
        GPU gpu = new GPU();
        int bodyColor = 0x112233;
        int normalColor = 0x445566;
        int powerColor = 0x778899;
        int backgroundColor = 0x7A6B5C;
        int[][] palettes = {
            {0, bodyColor, 0, 0},
            {0, normalColor, 0, 0},
            {0, powerColor, 0, 0}
        };
        writeSolidTile(gpu, 0x20, 1);
        writeSolidTile(gpu, 0x21, 1);
        writeSolidTile(gpu, 0x22, 1);
        writeSolidTile(gpu, 0x23, 1);
        writeSolidTile(gpu, 0x24, 1);
        writeSolidTile(gpu, 0x25, 1);

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
                    new EntitySpriteDefinition.OamAttribute(0x24, 0x02)),
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
        assertEquals(powerColor, pixelColor(buffer, 16, 16));
        assertEquals(backgroundColor, pixelColor(buffer, 24, 16));
    }

    private static EntitySpriteDefinition pairDefinition(EntitySpriteDefinition.OamAttribute first,
                                                          EntitySpriteDefinition.OamAttribute second) {
        return new EntitySpriteDefinition(0x7A, 0x06, 0x5C89,
            EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(first, second)));
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
