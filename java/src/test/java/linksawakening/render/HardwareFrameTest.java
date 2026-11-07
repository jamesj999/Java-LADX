package linksawakening.render;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Tile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class HardwareFrameTest {
    private static final int[] PALETTE = {11, 22, 33, 44};
    private static Tile solid(int index) {
        Tile tile = new Tile();
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) tile.setPixel(x, y, index);
        return tile;
    }
    private static int pixel(HardwareFrame frame, int x, int y) {
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        frame.render(new RenderContext(buffer, new GPU()));
        return buffer[(y * Framebuffer.WIDTH + x) * 4 + 2] & 255;
    }
    @Test void priorityDependsOnColorIndexAndMasterSwitch() {
        for (boolean enabled : new boolean[]{false, true}) for (boolean bgPriority : new boolean[]{false, true})
            for (boolean behind : new boolean[]{false, true}) for (int index : new int[]{0, 1}) {
                HardwareFrame frame = new HardwareFrame(enabled);
                frame.backgroundTile(solid(index), 0, 0, false, false, PALETTE, bgPriority);
                frame.objectTile(solid(2), 0, 0, false, false, PALETTE, 0, behind);
                assertEquals(enabled && index != 0 && (bgPriority || behind) ? PALETTE[index] : 33,
                    pixel(frame, 0, 0));
            }
    }
    @Test void transparentObjectDoesNotClaimPriorityButHiddenObjectDoes() {
        HardwareFrame frame = new HardwareFrame(true);
        frame.backgroundTile(solid(1), 0, 0, false, false, PALETTE, false);
        frame.objectTile(solid(2), 0, 0, false, false, PALETTE, 2, false);
        frame.objectTile(solid(0), 0, 0, false, false, PALETTE, 0, false);
        assertEquals(33, pixel(frame, 0, 0));
        frame.objectTile(solid(3), 0, 0, false, false, PALETTE, 1, true);
        assertEquals(22, pixel(frame, 0, 0));
    }
    @Test void windowColorZeroReplacesBackgroundAndClearsItsPriority() {
        HardwareFrame frame = new HardwareFrame(true);
        frame.backgroundTile(solid(1), 0, 0, false, false, PALETTE, true);
        frame.backgroundTile(solid(0), 0, 0, false, false, PALETTE, false);
        assertEquals(11, pixel(frame, 0, 0));
        frame.objectTile(solid(2), 0, 0, false, false, PALETTE, 0, true);
        assertEquals(33, pixel(frame, 0, 0));
    }
    @Test void flipsAndClipsAtAllFourEdges() {
        for (boolean flipX : new boolean[]{false, true}) for (boolean flipY : new boolean[]{false, true}) {
            Tile tile = solid(0);
            tile.setPixel(flipX ? 7 : 0, flipY ? 7 : 0, 3);
            HardwareFrame frame = new HardwareFrame(true);
            frame.objectTile(tile, 0, 0, flipX, flipY, PALETTE, 0, false);
            assertEquals(44, pixel(frame, 0, 0));
            for (int[] origin : new int[][]{{-7,-7},{159,143}}) {
                frame = new HardwareFrame(true);
                frame.backgroundTile(solid(2), origin[0], origin[1], flipX, flipY, PALETTE, false);
                assertEquals(33, pixel(frame, Math.max(0, origin[0]), Math.max(0, origin[1])));
            }
        }
    }
    @Test void laterPainterSubmissionWinsRegardlessOfXOrUnusedSlotMetadata() {
        HardwareFrame frame = new HardwareFrame(true);
        frame.appendInSlot(2, solid(1), 0, 0, false, false, PALETTE, false);
        frame.appendInSlot(5, solid(2), 4, 0, false, false, PALETTE, false);
        assertEquals(33, pixel(frame, 4, 0));
    }
    @Test void shadowSubmittedAfterLinkCoversLink() {
        HardwareFrame frame = new HardwareFrame(true);
        frame.appendInSlot(2, solid(2), 0, 0, false, false, PALETTE, false);
        frame.appendInSlot(3, solid(3), 8, 0, false, false, PALETTE, false);
        frame.appendInSlot(0, solid(1), 4, 0, false, false, PALETTE, false);
        assertEquals(22, pixel(frame, 4, 0));
        assertEquals(22, pixel(frame, 8, 0));
    }
    @Test void hudColorZeroCannotHideAnObjectEvenWithBgPrioritySet() {
        // User-supplied ROM: InventoryTilemap row 0 col 8 is $7F,
        // InventoryAttrmap is $01, shared CGB tile slot $17F pixel (7,0) is index 0.
        // home/animated_tiles.asm $1D57-$1D59 rejects OAM Y >= $88,
        // but that is an entry guard, not a per-pixel HUD clip.
        HardwareFrame frame = new HardwareFrame(true);
        frame.backgroundTile(solid(0), 64, 128, false, false, PALETTE, true);
        frame.appendInSlot(2, solid(2), 64, 128, false, false, PALETTE, true);
        assertEquals(33, pixel(frame, 71, 128));
    }
    @Test void fidelityClassifierRequiresHardwareVisibleObjectAndExactLegacyBgColor() {
        HardwareFrame frame = new HardwareFrame(true);
        frame.closedHudTile(solid(0), 0x7F, 1, 64, 128, PALETTE);
        frame.appendInSlot(2, solid(2), 64, 128, false, false, PALETTE, false);
        byte[] hardware = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        frame.compose(hardware);
        int pixel = 128 * Framebuffer.WIDTH + 71, at = pixel * 4;
        byte[] legacy = hardware.clone();
        legacy[at + 2] = 11;
        org.junit.jupiter.api.Assertions.assertTrue(frame.explainsHudPriority(legacy, hardware, pixel));
        legacy[at] = 1;
        org.junit.jupiter.api.Assertions.assertFalse(frame.explainsHudPriority(legacy, hardware, pixel));
        legacy[at] = 0;
        frame.closedHudTile(solid(3), 0xA9, 2, 64, 128, PALETTE);
        frame.compose(hardware);
        legacy[at + 2] = 44;
        org.junit.jupiter.api.Assertions.assertTrue(frame.explainsHudPriority(legacy, hardware, pixel));
        frame.closedHudTile(solid(3), 0xA9, 0x82, 64, 128, PALETTE);
        frame.compose(hardware);
        org.junit.jupiter.api.Assertions.assertFalse(frame.explainsHudPriority(legacy, hardware, pixel));
    }
}
