package linksawakening.render;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.util.Arrays;
import linksawakening.entity.Link;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.testing.TestRom;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class LinkGrassRenderLayerTest {
    @Test
    void grassColumnsUseSourceStepFlipPaletteAndReservedDialogPriority() throws Exception {
        RomTables tables;
        try (var input = TestRom.open()) {
            tables = RomTables.loadFromRom(input.readAllBytes());
        }
        var collision = new OverworldCollision(tables);
        int[] objects = new int[0x100];
        Arrays.fill(objects, 0x0A);
        collision.setRoom(objects);
        assertEquals(6, collision.objectPhysicsFlagAtPoint(40, 40));
        var input = new InputState();
        var config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        var link = new Link(input, config, tables, collision, null, new PlayerState(), null);
        link.setPixelPosition(32, 32);
        input.onKeyEvent(config.rightKey(), GLFW_PRESS);
        link.setGelClinging(false, 211);
        link.update();
        assertEquals(8, link.romConsecutiveStepsCount());
        assertTrue(link.grassGroundEffectActive());
        GPU gpu = new GPU();
        for (int y = 0; y < 8; y++) {
            gpu.getTile(0x1A).setPixel(0, y, 1);
            gpu.getTile(0x1B).setPixel(1, y, 2);
        }
        int[][] palettes = new int[8][4];
        palettes[6][1] = 0xCC7722;
        palettes[6][2] = 0x2299DD;
        var scroll = new ScrollController();
        var frame = new HardwareFrame(true);
        StorySprites.linkGrass(link, palettes, scroll, 6).submit(frame, gpu);
        frame.hideSpritesUnderDialog(false); // Reserved OAM0/1 survive dialog filtering.
        byte[] buffer = new byte[160 * 144 * 4];
        frame.compose(buffer);
        // Source entry X/Y are Link's ROM coordinates minus1/plus8.
        // Bit3 of step8 flips the first column; the second uses the opposite flip.
        assertEquals(0xCC7722, pixel(buffer, 39, 40));
        assertEquals(0xCC7722, pixel(buffer, 40, 40));
        assertEquals(0x2299DD, pixel(buffer, 38, 48));
        assertEquals(0x2299DD, pixel(buffer, 41, 48));
        assertEquals(0, pixel(buffer, 32, 40));
        input.onKeyEvent(config.rightKey(), GLFW_RELEASE);
        link.update();
        assertEquals(7, link.romConsecutiveStepsCount());
        assertFalse(link.grassGroundEffectFlipX());
        link.setRoomPhysicsForRoom(1, 0x10, 0xA3, 1);
        assertFalse(link.grassGroundEffectActive(), "warp clears the source transient ground physics");
        link.useRocsFeather();
        link.update();
        assertFalse(link.grassGroundEffectActive(), "airborne Link has no grass columns");
    }

    private static int pixel(byte[] buffer, int x, int y) {
        int offset = (y * 160 + x) * 4;
        return (buffer[offset] & 255) << 16 | (buffer[offset + 1] & 255) << 8 | buffer[offset + 2] & 255;
    }
}
