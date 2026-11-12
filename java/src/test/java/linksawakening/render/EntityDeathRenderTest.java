package linksawakening.render;

import static linksawakening.render.EntityRenderTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityDeathRenderTest {
    @Test
    void sharedDeathCloudResetsBodyTileOffsetAndFacingForOctorokAndOtherEnemies() throws Exception {
        var catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()));
        GPU gpu = new GPU();
        int[][] pattern = new int[8][8];
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++) {
                pattern[y][x] = x < 3 ? 1 : 2;
            }
        for (int tile : new int[] {0x10, 0x11, 0x12, 0x13, 0x30, 0x31, 0x3A, 0x3B, 0x3C, 0x3D}) {
            writePatternTile(gpu, tile, pattern);
        }
        int[][] palettes = new int[8][4];
        for (int i = 0; i < palettes.length; i++) {
            palettes[i] = new int[] {0, 0x112233 + i, 0x445566 + i, 0};
        }
        var selection = new EntitySpriteSelection(
                        EntityRoomLoader.RoomTable.OVERWORLD, 0, 0, new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
                .withDeathSpriteDefinitions(catalog.forDeathEntity(), catalog.forPowerRecoilDeathEntity());
        for (int type : new int[] {0x09, 0x0B}) {
            var body = catalog.forEntityType(type, EntityRoomLoader.RoomTable.OVERWORLD);
            for (boolean power : new boolean[] {false, true}) {
                for (int variant = 0; variant < 4; variant++) {
                    for (int flash : new int[] {0, 0x10}) {
                        var expected = new RoomEntity(
                                0, 0, type, 64, 64, EntityStatus.DYING, body, 0, flash, 0, 0, variant, power);
                        var inherited = new RoomEntity(
                                0,
                                0,
                                type,
                                64,
                                64,
                                EntityStatus.DYING,
                                body,
                                0,
                                flash | 0x60,
                                type == 0x09 ? 0x30 : 0,
                                0,
                                variant,
                                power);
                        byte[] baseline = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
                        byte[] actual = baseline.clone();
                        new EntityRenderLayer(snapshot(selection, expected), palettes, new ScrollController())
                                .render(new RenderContext(baseline, gpu));
                        new EntityRenderLayer(snapshot(selection, inherited), palettes, new ScrollController())
                                .render(new RenderContext(actual, gpu));
                        org.junit.jupiter.api.Assertions.assertArrayEquals(
                                baseline,
                                actual,
                                "type=" + type + " variant=" + variant + " power=" + power + " flash=" + flash);
                        if (variant == 0) {
                            int[] palette = palettes[flash == 0 ? 1 : 4];
                            assertEquals(palette[1], pixelColor(actual, 56, 48));
                            assertEquals(palette[1], pixelColor(actual, 71, 48));
                            assertEquals(palette[2], pixelColor(actual, 63, 48));
                        }
                    }
                }
            }
        }
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

        EntitySpriteDefinition body = powerDeathBody();
        List<List<EntitySpriteDefinition.RectangleSprite>> normalVariants = normalDeathVariants();
        EntitySpriteDefinition normal = new EntitySpriteDefinition(
                0x00, 0x03, 0x5488, EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(), normalVariants);
        List<List<EntitySpriteDefinition.RectangleSprite>> powerVariants = List.of(
                List.of(new EntitySpriteDefinition.RectangleSprite(
                        0, 0, new EntitySpriteDefinition.OamAttribute(0x24, 0x02))),
                List.of(new EntitySpriteDefinition.RectangleSprite(
                        0, 0, new EntitySpriteDefinition.OamAttribute(0x24, 0x02))),
                List.of(new EntitySpriteDefinition.RectangleSprite(
                        0, 0, new EntitySpriteDefinition.OamAttribute(0x24, 0x02))),
                List.of(
                        new EntitySpriteDefinition.RectangleSprite(
                                0, 0, new EntitySpriteDefinition.OamAttribute(0x26, 0x02)),
                        new EntitySpriteDefinition.RectangleSprite(
                                0, 8, new EntitySpriteDefinition.OamAttribute(0xFF, 0x00))));
        EntitySpriteDefinition power = new EntitySpriteDefinition(
                0x00, 0x03, 0x54C8, EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(), powerVariants);
        EntitySpriteSelection selection = new EntitySpriteSelection(
                        EntityRoomLoader.RoomTable.OVERWORLD, 0, 0, new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
                .withDeathSpriteDefinitions(normal, power);
        RoomEntity entity = new RoomEntity(0, 0, 0x09, 24, 32, EntityStatus.DYING, body, 0, 0, 0, 0, 3, true);
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
                        EntityRoomLoader.RoomTable.OVERWORLD, 0, 0, new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
                .withDeathSpriteDefinitions(
                        rectangleDefinition(0x5488, 0x28, 0x01), rectangleDefinition(0x54C8, 0x2A, 0x02));
        RoomEntity entity = new RoomEntity(0, 0, 0x09, 24, 32, EntityStatus.DYING, body, 0, 0, 0, 0, -1, false);
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
                        EntityRoomLoader.RoomTable.OVERWORLD, 0, 0, new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
                .withDeathSpriteDefinitions(
                        rectangleDefinition(0x5488, 0x2C, 0x01), rectangleDefinition(0x54C8, 0x2E, 0x02));
        RoomEntity entity = new RoomEntity(0, 0, 0x09, 24, 32, EntityStatus.DYING, body, 0, 0, 0, 0, 3, false);
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        new EntityRenderLayer(snapshot(selection, entity), palettes, new ScrollController())
                .render(new RenderContext(buffer, gpu));

        assertEquals(normalColor, pixelColor(buffer, 16, 16));
    }

    private static List<List<EntitySpriteDefinition.RectangleSprite>> normalDeathVariants() {
        return List.of(
                List.of(new EntitySpriteDefinition.RectangleSprite(
                        0, 0, new EntitySpriteDefinition.OamAttribute(0x22, 0x01))),
                List.of(new EntitySpriteDefinition.RectangleSprite(
                        0, 0, new EntitySpriteDefinition.OamAttribute(0x22, 0x01))),
                List.of(new EntitySpriteDefinition.RectangleSprite(
                        0, 0, new EntitySpriteDefinition.OamAttribute(0x22, 0x01))),
                List.of(new EntitySpriteDefinition.RectangleSprite(
                        0, 0, new EntitySpriteDefinition.OamAttribute(0x22, 0x01))));
    }

    private static EntitySpriteDefinition powerDeathBody() {
        return new EntitySpriteDefinition(
                0x09,
                0x03,
                0x57FB,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(new EntitySpriteDefinition.Variant(
                        new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
                        new EntitySpriteDefinition.OamAttribute(0x20, 0x00))));
    }
}
