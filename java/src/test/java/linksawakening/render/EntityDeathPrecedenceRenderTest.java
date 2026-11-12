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
final class EntityDeathPrecedenceRenderTest {
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
                0x09,
                0x03,
                0x4000,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(new EntitySpriteDefinition.Variant(
                        new EntitySpriteDefinition.OamAttribute(0x38, 0x01),
                        new EntitySpriteDefinition.OamAttribute(0x38, 0x01))));
        EntitySpriteSelection selection = new EntitySpriteSelection(
                        EntityRoomLoader.RoomTable.OVERWORLD, 0, 0, new int[] {0xFF, 0xFF, 0xFF, 0xFF}, true, palettes)
                .withDeathSpriteDefinitions(
                        rectangleDefinition(0x5488, 0x32, 0x01), rectangleDefinition(0x54C8, 0x34, 0x02))
                .withSpriteOverride(0x09, override);
        RoomEntity dying = new RoomEntity(0, 0, 0x09, 24, 32, EntityStatus.DYING, body, 0, 0, 0, 0, 3, true);
        RoomEntity active = new RoomEntity(1, 1, 0x09, 64, 32, EntityStatus.ACTIVE, body, 0, 0, 0, 0, -1, false);
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

        EntitySpriteDefinition normal = normalBombDefinition();
        EntitySpriteDefinition warning = new EntitySpriteDefinition(
                0x02,
                0x03,
                0x5484,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(new EntitySpriteDefinition.Variant(
                        new EntitySpriteDefinition.OamAttribute(0x20, 0x00),
                        new EntitySpriteDefinition.OamAttribute(0x20, 0x00))));
        EntitySpriteDefinition explosion = new EntitySpriteDefinition(
                0x02,
                0x03,
                0x6530,
                EntitySpriteDefinition.Shape.RECTANGLE,
                0,
                List.of(),
                List.of(List.of(new EntitySpriteDefinition.RectangleSprite(
                        0, 0, new EntitySpriteDefinition.OamAttribute(0x20, 0x00)))));

        byte[] normalBuffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(
                        snapshot(
                                new RoomEntity(0, 0, 0x02, 24, 32, EntityStatus.ACTIVE, normal, 0, 0, 0, 0, -1, false)),
                        palettes,
                        new ScrollController())
                .render(new RenderContext(normalBuffer, gpu));
        assertEquals(bombColor, pixelColor(normalBuffer, 20, 18));
        assertEquals(0, pixelColor(normalBuffer, 20, 16));

        byte[] warningBuffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(
                        snapshot(new RoomEntity(
                                0, 0, 0x02, 24, 32, EntityStatus.ACTIVE, warning, 0, 0, 0, 0, -1, false)),
                        palettes,
                        new ScrollController())
                .render(new RenderContext(warningBuffer, gpu));
        assertEquals(bombColor, pixelColor(warningBuffer, 16, 16));
        assertEquals(0, pixelColor(warningBuffer, 16, 14));

        byte[] explosionBuffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        new EntityRenderLayer(
                        snapshot(new RoomEntity(
                                0, 0, 0x02, 24, 32, EntityStatus.ACTIVE, explosion, 0, 0, 0, 0, -1, false)),
                        palettes,
                        new ScrollController())
                .render(new RenderContext(explosionBuffer, gpu));
        assertEquals(bombColor, pixelColor(explosionBuffer, 16, 16));
        assertEquals(0, pixelColor(explosionBuffer, 16, 14));
    }

    private static EntitySpriteDefinition normalBombDefinition() {
        return new EntitySpriteDefinition(
                0x02,
                0x03,
                0x652E,
                EntitySpriteDefinition.Shape.SINGLE,
                0,
                List.of(new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x20, 0x00), null)));
    }
}
