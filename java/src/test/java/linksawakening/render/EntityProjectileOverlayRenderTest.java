package linksawakening.render;

import static linksawakening.render.EntityRenderTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityProjectileOverlayRenderTest {
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
                0x09,
                0x03,
                0x57FB,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(new EntitySpriteDefinition.Variant(
                        new EntitySpriteDefinition.OamAttribute(0x30, 0x00),
                        new EntitySpriteDefinition.OamAttribute(0x30, 0x00))));
        EntitySpriteDefinition fire = burningDefinition();
        EntitySpriteSelection selection = new EntitySpriteSelection(
                        EntityRoomLoader.RoomTable.OVERWORLD, 0, 0, new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
                .withBurningSpriteDefinition(fire);
        RoomEntity entity = new RoomEntity(0, 0, 0x09, 24, 32, EntityStatus.BURNING, body, 0, 0x20, 0x10, 4, -1, false);

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
                0x86,
                0x06,
                0x7AEB,
                EntitySpriteDefinition.Shape.RECTANGLE,
                0,
                List.of(),
                List.of(
                        List.of(
                                new EntitySpriteDefinition.RectangleSprite(
                                        0, -4, new EntitySpriteDefinition.OamAttribute(0x22, 0x00)),
                                new EntitySpriteDefinition.RectangleSprite(
                                        0, 12, new EntitySpriteDefinition.OamAttribute(0x22, 0x20))),
                        List.of(
                                new EntitySpriteDefinition.RectangleSprite(
                                        0, -4, new EntitySpriteDefinition.OamAttribute(0x22, 0x40)),
                                new EntitySpriteDefinition.RectangleSprite(
                                        0, 12, new EntitySpriteDefinition.OamAttribute(0x22, 0x60)))));
        EntitySpriteSelection selection = new EntitySpriteSelection(
                        EntityRoomLoader.RoomTable.OVERWORLD, 0, 0, new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
                .withSpriteOverlay(0x86, overlay);
        EntitySpriteDefinition main = new EntitySpriteDefinition(
                0x86,
                0x06,
                0x7ADD,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0xFF, 0x00), null)));
        RoomEntity entity = new RoomEntity(0, 0, 0x86, 24, 32, EntityStatus.ACTIVE, main, 0, 0, 0, 0, -1, false);
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
                0x86,
                0x06,
                0x7AEB,
                EntitySpriteDefinition.Shape.RECTANGLE,
                0,
                List.of(),
                List.of(List.of(new EntitySpriteDefinition.RectangleSprite(
                        0, -4, new EntitySpriteDefinition.OamAttribute(0x22, 0x00)))));
        EntitySpriteSelection selection = new EntitySpriteSelection(
                        EntityRoomLoader.RoomTable.COLOR_DUNGEON, 0, 0, new int[0], false, palettes)
                .withSpriteOverlay(0x86, overlay);
        RoomEntity entity = new RoomEntity(
                0,
                0,
                0x86,
                24,
                32,
                EntityStatus.ACTIVE,
                EntitySpriteDefinition.unsupported(0x86),
                -1,
                0,
                0,
                0,
                -1,
                false);
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
                0x86,
                0x06,
                0x7ADD,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(
                        new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null),
                        new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null),
                        new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null),
                        new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null),
                        new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null),
                        new EntitySpriteDefinition.Variant(
                                new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
                                new EntitySpriteDefinition.OamAttribute(0x20, 0x00))));
        RoomEntity mixedSingle =
                new RoomEntity(0, 0, 0x86, 24, 32, EntityStatus.ACTIVE, definition, 0, 0, 0, 0, -1, false);
        RoomEntity pair = new RoomEntity(1, 1, 0x86, 56, 32, EntityStatus.ACTIVE, definition, 5, 0, 0, 0, -1, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(mixedSingle, pair).withSideScrolling(true), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));

        assertEquals(color, pixelColor(buffer, 20, 12));
        assertEquals(0, pixelColor(buffer, 20, 16));
        assertEquals(color, pixelColor(buffer, 48, 16));
    }

    private static EntitySpriteDefinition burningDefinition() {
        return new EntitySpriteDefinition(
                0x00,
                0x03,
                0x4C44,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(
                        new EntitySpriteDefinition.Variant(
                                new EntitySpriteDefinition.OamAttribute(0x34, 0x02),
                                new EntitySpriteDefinition.OamAttribute(0x34, 0x22)),
                        new EntitySpriteDefinition.Variant(
                                new EntitySpriteDefinition.OamAttribute(0x34, 0x14),
                                new EntitySpriteDefinition.OamAttribute(0x34, 0x34))));
    }
}
