package linksawakening.render;

import static linksawakening.render.EntityRenderTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntitySpriteShapeRenderTest {
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
        variants.set(
                6,
                new EntitySpriteDefinition.Variant(
                        new EntitySpriteDefinition.OamAttribute(0x20, 0x01),
                        new EntitySpriteDefinition.OamAttribute(0x22, 0x02)));
        variants.set(
                7,
                new EntitySpriteDefinition.Variant(
                        new EntitySpriteDefinition.OamAttribute(0x24, 0x23),
                        new EntitySpriteDefinition.OamAttribute(0x26, 0x24)));
        EntitySpriteDefinition pairodd =
                new EntitySpriteDefinition(0x57, 0x04, 0x5DD1, EntitySpriteDefinition.Shape.PAIR, 0, variants);
        RoomEntity entity = new RoomEntity(0, 0, 0x57, 40, 32, EntityStatus.ACTIVE, pairodd, 3, 0, 0, 0, -1, false);
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

        EntitySpriteDefinition rectangle = new EntitySpriteDefinition(
                0x77,
                0x06,
                0x5C51,
                EntitySpriteDefinition.Shape.RECTANGLE,
                0,
                List.of(),
                List.of(List.of(
                        new EntitySpriteDefinition.RectangleSprite(
                                -8, -8, new EntitySpriteDefinition.OamAttribute(0x00, 0x00)),
                        new EntitySpriteDefinition.RectangleSprite(
                                -8, 0, new EntitySpriteDefinition.OamAttribute(0x02, 0x00)))));
        RoomEntity entity =
                new RoomEntity(0, 0, 0x77, 24, 32, EntityStatus.ACTIVE, rectangle, 0, 0, 0x20, 0, -1, false);
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
                        linksawakening.world.entity.EntityRoomLoader.RoomTable.OVERWORLD,
                        0,
                        0,
                        new int[] {0xFF, 0xFF, 0xFF, 0xFF},
                        true,
                        palettes)
                .withSpriteOverride(0x6D, following);
        RoomEntity entity = new RoomEntity(0, 0, 0x6D, 24, 32, EntityStatus.ACTIVE, atHome, 0, 0, 0, 0, -1, false);
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
        int[][] palettes = {{0, leftColor, rightColor, 0}};
        writePatternTile(gpu, 0x20, new int[][] {
            {1, 1, 1, 1, 1, 1, 1, 2},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1}
        });
        writeSolidTile(gpu, 0x21, 1);
        writeSolidTile(gpu, 0x22, 2);
        writeSolidTile(gpu, 0x23, 2);

        EntitySpriteDefinition pair = pairDefinition(
                new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
                new EntitySpriteDefinition.OamAttribute(0x22, 0x00));
        RoomEntity entity = new RoomEntity(0, 0, 0x01, 24, 32, EntityStatus.ACTIVE, pair, 0, 0x20, 0, 0, -1, false);
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
        RoomEntity entity = new RoomEntity(0, 0, 0x01, 24, 32, EntityStatus.ACTIVE, pair, 0, 0x10, 0, 0, -1, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(entity), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));

        assertEquals(palettes[4][1], pixelColor(buffer, 16, 16));
    }

    @Test
    void rejectsAnUnavailableRomObjectPaletteInsteadOfClampingIt() {
        GPU gpu = new GPU();
        writeSolidTile(gpu, 0x20, 1);
        EntitySpriteDefinition pair = pairDefinition(
                new EntitySpriteDefinition.OamAttribute(0x20, 0x07),
                new EntitySpriteDefinition.OamAttribute(0x20, 0x07));
        RoomEntity entity = new RoomEntity(0, 0, 0x01, 24, 32, EntityStatus.ACTIVE, pair, 0, 0, 0, 0, -1, false);
        EntityRenderLayer layer =
                new EntityRenderLayer(snapshot(entity), new int[][] {{0, 0x123456, 0, 0}}, new ScrollController());
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        assertThrows(IllegalArgumentException.class, () -> layer.render(new RenderContext(buffer, gpu)));
    }

    @Test
    void clipsEntitiesAndUsesCurrentRoomOffsetDuringScroll() {
        GPU gpu = new GPU();
        int color = 0x123456;
        int[][] palettes = {{0, color, 0, 0}};
        writeSolidTile(gpu, 0x30, 1);
        writeSolidTile(gpu, 0x31, 1);
        EntitySpriteDefinition single = new EntitySpriteDefinition(
                0x02,
                0x00,
                0x4000,
                EntitySpriteDefinition.Shape.SINGLE,
                0,
                List.of(new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x30, 0x00), null)));
        RoomEntity entity = new RoomEntity(0, 0, 0x02, 4, 12, EntityStatus.ACTIVE, single, 0, 0, 0, 0, -1, false);
        RoomEntitySnapshot snapshot = snapshot(entity);
        ScrollController scroll = new ScrollController();
        scroll.start(ScrollController.RIGHT, 0, 0, null, 160);
        scroll.tick(8);

        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(snapshot, palettes, scroll).render(new RenderContext(buffer, gpu));

        assertEquals(color, pixelColor(buffer, 152, 0));
        assertEquals(color, pixelColor(buffer, 159, 7));
    }
}
