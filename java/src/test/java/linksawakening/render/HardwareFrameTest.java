package linksawakening.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Tile;
import org.junit.jupiter.api.Test;

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

    @Test
    void copiedBackgroundKeepsItsTransformWhenScratchIsReused() {
        var frame = new HardwareFrame(true);
        var scratch = frame.backgroundScratch();
        scratch.withPaletteTransform(color -> color + 3, () -> scratch.backgroundPixel(0, 0, 1, PALETTE, false));
        frame.copyBackgroundRows(scratch, 1);
        scratch.reset();
        scratch.withPaletteTransform(color -> color * 7, () -> scratch.backgroundPixel(0, 0, 1, PALETTE, false));
        assertEquals(25, frame.backgroundRgb(0));
    }

    @Test
    void priorityDependsOnColorIndexAndMasterSwitch() {
        for (boolean enabled : new boolean[] {false, true})
            for (boolean bgPriority : new boolean[] {false, true})
                for (boolean behind : new boolean[] {false, true})
                    for (int index : new int[] {0, 1}) {
                        HardwareFrame frame = new HardwareFrame(enabled);
                        frame.backgroundTile(solid(index), 0, 0, false, false, PALETTE, bgPriority);
                        frame.append(solid(2), 0, 0, false, false, PALETTE, behind);
                        assertEquals(
                                enabled && index != 0 && (bgPriority || behind) ? PALETTE[index] : 33,
                                pixel(frame, 0, 0));
                    }
    }

    @Test
    void transparentObjectDoesNotClaimPriorityButHiddenObjectDoes() {
        HardwareFrame frame = new HardwareFrame(true, new HardwareFrameDiagnostics());
        frame.backgroundTile(solid(1), 0, 0, false, false, PALETTE, false);
        frame.append(solid(2), 0, 0, false, false, PALETTE, false);
        frame.append(solid(0), 0, 0, false, false, PALETTE, false);
        assertEquals(33, pixel(frame, 0, 0));
        frame.append(solid(3), 0, 0, false, false, PALETTE, true);
        assertEquals(22, pixel(frame, 0, 0));
    }

    @Test
    void windowColorZeroReplacesBackgroundAndClearsItsPriority() {
        HardwareFrame frame = new HardwareFrame(true, new HardwareFrameDiagnostics());
        frame.backgroundTile(solid(1), 0, 0, false, false, PALETTE, true);
        frame.backgroundTile(solid(0), 0, 0, false, false, PALETTE, false);
        assertEquals(11, pixel(frame, 0, 0));
        frame.append(solid(2), 0, 0, false, false, PALETTE, true);
        assertEquals(33, pixel(frame, 0, 0));
    }

    @Test
    void flipsAndClipsAtAllFourEdges() {
        for (boolean flipX : new boolean[] {false, true})
            for (boolean flipY : new boolean[] {false, true}) {
                Tile tile = solid(0);
                tile.setPixel(flipX ? 7 : 0, flipY ? 7 : 0, 3);
                HardwareFrame frame = new HardwareFrame(true, new HardwareFrameDiagnostics());
                frame.append(tile, 0, 0, flipX, flipY, PALETTE, false);
                assertEquals(44, pixel(frame, 0, 0));
                for (int[] origin : new int[][] {{-7, -7}, {159, 143}}) {
                    frame = new HardwareFrame(true, new HardwareFrameDiagnostics());
                    frame.backgroundTile(solid(2), origin[0], origin[1], flipX, flipY, PALETTE, false);
                    assertEquals(33, pixel(frame, Math.max(0, origin[0]), Math.max(0, origin[1])));
                }
            }
    }

    @Test
    void laterPainterSubmissionWinsRegardlessOfXOrUnusedSlotMetadata() {
        HardwareFrame frame = new HardwareFrame(true, new HardwareFrameDiagnostics());
        frame.appendInSlot(2, solid(1), 0, 0, false, false, PALETTE, false);
        frame.appendInSlot(5, solid(2), 4, 0, false, false, PALETTE, false);
        assertEquals(33, pixel(frame, 4, 0));
    }

    @Test
    void shadowSubmittedAfterLinkCoversLink() {
        HardwareFrame frame = new HardwareFrame(true, new HardwareFrameDiagnostics());
        frame.appendInSlot(2, solid(2), 0, 0, false, false, PALETTE, false);
        frame.appendInSlot(3, solid(3), 8, 0, false, false, PALETTE, false);
        frame.appendInSlot(0, solid(1), 4, 0, false, false, PALETTE, false);
        assertEquals(22, pixel(frame, 4, 0));
        assertEquals(22, pixel(frame, 8, 0));
    }

    @Test
    void hudColorZeroCannotHideAnObjectEvenWithBgPrioritySet() {
        // User-supplied ROM: InventoryTilemap row 0 col 8 is $7F,
        // InventoryAttrmap is $01, shared CGB tile slot $17F pixel (7,0) is index 0.
        // home/animated_tiles.asm $1D57-$1D59 rejects OAM Y >= $88,
        // but that is an entry guard, not a per-pixel HUD clip.
        HardwareFrame frame = new HardwareFrame(true, new HardwareFrameDiagnostics());
        frame.backgroundTile(solid(0), 64, 128, false, false, PALETTE, true);
        frame.appendInSlot(2, solid(2), 64, 128, false, false, PALETTE, true);
        assertEquals(33, pixel(frame, 71, 128));
    }

    @Test
    void fidelityClassifierRequiresHardwareVisibleObjectAndExactLegacyBgColor() {
        HardwareFrame frame = new HardwareFrame(true, new HardwareFrameDiagnostics());
        frame.closedHudTile(solid(0), 0x7F, 1, 64, 128, PALETTE);
        frame.appendInSlot(2, solid(2), 64, 128, false, false, PALETTE, false);
        byte[] hardware = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        frame.compose(hardware);
        int pixel = 128 * Framebuffer.WIDTH + 71, at = pixel * 4;
        byte[] legacy = hardware.clone();
        legacy[at + 2] = 11;
        org.junit.jupiter.api.Assertions.assertTrue(
                TestHardwareFrames.diagnostics(frame).explainsHudPriority(legacy, hardware, pixel));
        legacy[at] = 1;
        org.junit.jupiter.api.Assertions.assertFalse(
                TestHardwareFrames.diagnostics(frame).explainsHudPriority(legacy, hardware, pixel));
        legacy[at] = 0;
        frame.closedHudTile(solid(3), 0xA9, 2, 64, 128, PALETTE);
        frame.compose(hardware);
        legacy[at + 2] = 44;
        org.junit.jupiter.api.Assertions.assertTrue(
                TestHardwareFrames.diagnostics(frame).explainsHudPriority(legacy, hardware, pixel));
        frame.closedHudTile(solid(3), 0xA9, 0x82, 64, 128, PALETTE);
        frame.compose(hardware);
        org.junit.jupiter.api.Assertions.assertFalse(
                TestHardwareFrames.diagnostics(frame).explainsHudPriority(legacy, hardware, pixel));
    }

    @Test
    void dialogHidingUsesWholeEntryYAndPreservesReservedSources() {
        for (boolean bottom : new boolean[] {false, true}) {
            int hiddenY = bottom ? 72 : 45;
            int visibleY = bottom ? 71 : 46;
            HardwareFrame frame = new HardwareFrame(true, new HardwareFrameDiagnostics());
            frame.submittingLayer("EntityRenderLayer");
            frame.append8x16(solid(2), solid(2), 0, hiddenY, false, false, PALETTE, false);
            frame.append8x16(solid(2), solid(2), 16, visibleY, false, false, PALETTE, false);
            frame.submittingLayer("LinkRenderLayer");
            frame.append8x16(solid(3), solid(3), 32, hiddenY, false, false, PALETTE, false);
            frame.submittingLayer("DialogRenderLayer");
            frame.append8x16(solid(1), solid(1), 48, hiddenY, false, false, PALETTE, false);
            frame.hideSpritesUnderDialog(bottom);
            assertEquals(0, pixel(frame, 0, hiddenY));
            assertEquals(0, pixel(frame, 0, hiddenY + 8));
            assertEquals(33, pixel(frame, 16, visibleY));
            assertEquals(33, pixel(frame, 16, visibleY + 8));
            assertEquals(44, pixel(frame, 32, hiddenY));
            assertEquals(22, pixel(frame, 48, hiddenY));
        }
    }

    @Test
    void dialogFidelityClassifierNeverAcceptsAnEntitySource() {
        HardwareFrame frame = new HardwareFrame(true, new HardwareFrameDiagnostics());
        frame.submittingLayer("DialogRenderLayer");
        frame.backgroundPixel(0, 0, 3, PALETTE, -1, 1);
        frame.submittingLayer("EntityRenderLayer");
        frame.append(solid(2), 0, 0, false, false, PALETTE, false);
        byte[] hardware = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        frame.compose(hardware);
        byte[] legacy = hardware.clone();
        legacy[2] = 44;
        org.junit.jupiter.api.Assertions.assertFalse(
                TestHardwareFrames.diagnostics(frame).explainsHudPriority(legacy, hardware, 0));
    }

    @Test
    void slidingWindowHidesWholeEntriesAtRawYThresholdAndClassifiesOnlyTracedPixels() {
        HardwareFrame frame = new HardwareFrame(true, new HardwareFrameDiagnostics());
        frame.submittingLayer("RoomRenderLayer");
        frame.backgroundTile(solid(1), 0, 56, false, false, PALETTE, false);
        frame.submittingLayer("EntityRenderLayer");
        frame.append8x16(solid(2), solid(2), 0, 56, false, false, PALETTE, false);
        frame.append8x16(solid(2), solid(2), 16, 55, false, false, PALETTE, false);
        frame.hideGameplayObjectsUnderWindow(64);
        assertEquals(22, pixel(frame, 0, 56));
        assertEquals(0, pixel(frame, 0, 64));
        assertEquals(33, pixel(frame, 16, 55));
        assertEquals(33, pixel(frame, 16, 63));
        byte[] hardware = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        frame.compose(hardware);
        int pixel = 56 * Framebuffer.WIDTH;
        byte[] legacy = hardware.clone();
        legacy[pixel * 4 + 2] = 33;
        org.junit.jupiter.api.Assertions.assertTrue(
                TestHardwareFrames.diagnostics(frame).explainsInventoryHiding(legacy, hardware, pixel));
        legacy[pixel * 4 + 2] = 44;
        org.junit.jupiter.api.Assertions.assertFalse(
                TestHardwareFrames.diagnostics(frame).explainsInventoryHiding(legacy, hardware, pixel));
    }

    @Test
    void resetDropsPlanesEntriesTransformsAndWindowFilters() {
        HardwareFrame frame = new HardwareFrame(true);
        frame.withPaletteTransform(
                color -> color + 100, () -> frame.backgroundTile(solid(1), 0, 0, false, false, PALETTE, true));
        frame.append(solid(3), 0, 0, false, false, PALETTE, false);
        frame.hideGameplayObjectsUnderWindow(64);
        assertEquals(122, pixel(frame, 0, 0));
        frame.reset();
        assertEquals(0, pixel(frame, 0, 0));
        frame.submittingLayer("EntityRenderLayer");
        frame.append(solid(2), 0, 72, false, false, PALETTE, false);
        frame.hideSpritesUnderDialog(true);
        assertEquals(0, pixel(frame, 0, 72));
        frame.backgroundTile(solid(1), 0, 0, false, false, PALETTE, false);
        frame.append(solid(3), 0, 0, false, false, PALETTE, false);
        assertEquals(44, pixel(frame, 0, 0));
    }

    @Test
    void copyingRowsFromTheSameFramePreservesPaletteTransformOrder() {
        HardwareFrame frame = new HardwareFrame(true);
        frame.withPaletteTransform(
                color -> color + 1, () -> frame.backgroundTile(solid(1), 0, 0, false, false, PALETTE, false));
        frame.withPaletteTransform(color -> color * 2, () -> frame.copyBackgroundRows(frame, 1));
        assertEquals(46, pixel(frame, 0, 0));
        assertEquals(23, pixel(frame, 0, 1));
    }
}
