package linksawakening.render;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.gpu.GPU;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class EntityScrollVisibilityTest {
    private static final int COLOR = 0x123456;
    private static final int[][] PALETTES = {{0, COLOR, 0, 0}};

    @Test void incomingEnemiesAndDroppedItemsSkipWholeListsUntilVisualYIsAtMost136() {
        for (var shape : List.of(EntitySpriteDefinition.Shape.PAIR,
                EntitySpriteDefinition.Shape.SINGLE, EntitySpriteDefinition.Shape.RECTANGLE)) {
            // Both enemies and item entities call the same ROM OAM helpers.
            for (int type : new int[] {0x09, 0x2D, 0x2E}) {
                var scroll = new ScrollController();
                scroll.start(ScrollController.DOWN, 0, 0, null, 128);
                var entity = entity(type, 40, 16, 0, definition(shape));
                assertEquals(0, coloredPixels(draw(entity, scroll)), "raw visual Y=144");
                scroll.tick(4);
                assertEquals(0, coloredPixels(draw(entity, scroll)), "raw visual Y=140");
                scroll.tick(3);
                assertEquals(0, coloredPixels(draw(entity, scroll)), "raw visual Y=137");
                scroll.tick(1);
                byte[] visible = draw(entity, scroll); // raw visual Y=$88; screen top=120
                assertTrue(coloredPixels(visible) > 0, "raw visual Y=136 is allowed");
                assertEquals(COLOR, pixel(visible, shape == EntitySpriteDefinition.Shape.SINGLE ? 36 : 32, 128),
                    "The guard hides whole lists; it does not clip pixels at the HUD boundary");
            }
        }
    }

    @Test void transitionGuardUsesVisualZButRetainsPositionSignAndPreShakeX() {
        var definition = definition(EntitySpriteDefinition.Shape.PAIR);
        var scroll = new ScrollController();
        scroll.start(ScrollController.DOWN, 0, 0, null, 128);
        assertTrue(coloredPixels(draw(entity(0x09, 40, 16, 8, definition), scroll)) > 0,
            "raw Y=144 minus Z=8 passes the visual-Y test");
        assertEquals(0, coloredPixels(draw(entity(0x09, 40, 128, 128, definition), scroll)),
            "raw Y=256 has a nonzero position sign even though visual Y=128");
        scroll.tick(127);
        // A rectangle can offset an otherwise rejected origin into the visible screen.
        var shifted = new EntitySpriteDefinition(0x09, 3, 0x5488,
            EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(), List.of(List.of(
                new EntitySpriteDefinition.RectangleSprite(-40, 0,
                    new EntitySpriteDefinition.OamAttribute(0x30, 0)))));
        scroll.setScriptedScreenShakeHorizontal(true, 40);
        assertEquals(0, coloredPixels(draw(entity(0x09, 193, 40, 0, shifted), scroll)),
            "raw X=193 is rejected before shake/rectangle offsets move pixels onscreen");
        assertTrue(coloredPixels(draw(entity(0x09, 192, 40, 0, shifted), scroll)) > 0,
            "raw X=192 is allowed");
    }

    @Test void normalBombTestsItsTwoPixelVisualAdjustmentBeforeGeneratingOam() {
        var bomb = new EntitySpriteDefinition(0x02, 3, 0x652E,
            EntitySpriteDefinition.Shape.SINGLE, 0, List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x30, 0), null)));
        var scroll = new ScrollController();
        scroll.start(ScrollController.DOWN, 0, 0, null, 128);
        assertEquals(0, coloredPixels(draw(entity(0x02, 40, 7, 0, bomb), scroll)),
            "RenderBomb adds two: raw Y=135 becomes visual Y=137");
        assertTrue(coloredPixels(draw(entity(0x02, 40, 6, 0, bomb), scroll)) > 0,
            "visual Y=136 is allowed");
    }

    @Test void stationaryEntitiesAreNotSubjectToTransitionBounds() {
        var definition = definition(EntitySpriteDefinition.Shape.PAIR);
        assertTrue(coloredPixels(draw(entity(0x09, 40, 144, 0, definition), new ScrollController())) > 0);
    }

    private static EntitySpriteDefinition definition(EntitySpriteDefinition.Shape shape) {
        var entry = new EntitySpriteDefinition.OamAttribute(0x30, 0);
        return shape == EntitySpriteDefinition.Shape.RECTANGLE
            ? new EntitySpriteDefinition(0x09, 3, 0x5488, shape, 0, List.of(),
                List.of(List.of(new EntitySpriteDefinition.RectangleSprite(0, 0, entry))))
            : new EntitySpriteDefinition(0x09, 3, 0x57FB, shape, 0,
                List.of(new EntitySpriteDefinition.Variant(entry,
                    shape == EntitySpriteDefinition.Shape.PAIR ? entry : null)));
    }

    private static RoomEntity entity(int type, int x, int y, int z, EntitySpriteDefinition definition) {
        return new RoomEntity(0, 0, type, x, y, EntityStatus.ACTIVE, definition, 0, 0, 0, z, -1, false);
    }

    private static byte[] draw(RoomEntity entity, ScrollController scroll) {
        GPU gpu = new GPU();
        for (int tile : new int[] {0x30, 0x31}) for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            gpu.getTile(tile).setPixel(x, y, 1);
        }
        var slots = new ArrayList<RoomEntity>();
        slots.add(entity);
        while (slots.size() < 16) slots.add(RoomEntity.disabled(slots.size()));
        var frame = new HardwareFrame(true);
        new EntityRenderLayer(new RoomEntitySnapshot(slots, null), PALETTES, scroll).submit(frame, gpu);
        byte[] buffer = new byte[160 * 144 * 4];
        frame.compose(buffer);
        return buffer;
    }

    private static long coloredPixels(byte[] buffer) {
        long count = 0;
        for (int y = 0; y < 144; y++) for (int x = 0; x < 160; x++) if (pixel(buffer, x, y) == COLOR) count++;
        return count;
    }

    private static int pixel(byte[] buffer, int x, int y) {
        int at = (y * 160 + x) * 4;
        return (buffer[at] & 255) << 16 | (buffer[at + 1] & 255) << 8 | buffer[at + 2] & 255;
    }
}
