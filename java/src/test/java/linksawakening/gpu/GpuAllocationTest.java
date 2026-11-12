package linksawakening.gpu;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.rom.Rom;
import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

final class GpuAllocationTest {
    @Test
    void animationUploadsAndStableSnapshotsDoNotAllocate() {
        byte[] data = new byte[0x100000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        Rom rom = Rom.of(data);
        GPU gpu = new GPU();
        gpu.loadAnimatedTilesGroup(rom, 9);
        double tick = AllocationProbe.bytesPerIteration(5000, 1000, () -> gpu.tickAnimatedTiles(rom));
        double snapshot = AllocationProbe.bytesPerIteration(5000, 1000, gpu::snapshotEntityTiles);
        System.out.println("Animated tiles bytes/tick=" + tick + " stable snapshot bytes/call=" + snapshot);
        assertTrue(tick < 16);
        assertTrue(snapshot < 16);
    }

    @Test
    void cachedSnapshotsNoticeDirectTileMutationAndProtectTheirPixels() {
        GPU gpu = new GPU();
        var before = gpu.snapshotEntityTiles();
        assertSame(before, gpu.snapshotEntityTiles());
        gpu.getTile(0x40).setPixel(0, 0, 3);
        var after = gpu.snapshotEntityTiles();
        assertNotSame(before, after);
        assertEquals(0, before.tile(0x40).getPixel(0, 0));
        assertEquals(3, after.readOnlyTile(0x40).getPixel(0, 0));
        assertThrows(UnsupportedOperationException.class, () -> after.readOnlyTile(0x40)
                .setPixel(0, 0, 2));
        after.tile(0x40).setPixel(0, 0, 2);
        assertEquals(3, after.tile(0x40).getPixel(0, 0));
    }

    @Test
    void roomSheetReloadRetainsDistinctSnapshotIdentityEvenWhenPixelsMatch() {
        GPU gpu = new GPU();
        var before = gpu.snapshotEntityTiles();
        gpu.loadEntitySpriteSheets(Rom.of(new byte[0x100000]), new int[] {255, 255, 255, 255});
        assertNotSame(before, gpu.snapshotEntityTiles());
    }
}
