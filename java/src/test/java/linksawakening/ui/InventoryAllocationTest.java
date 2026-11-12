package linksawakening.ui;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.gpu.GPU;
import linksawakening.render.HardwareFrame;
import linksawakening.rom.Rom;
import linksawakening.rom.RomSymbols;
import linksawakening.state.PlayerState;
import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

class InventoryAllocationTest {
    @Test
    void hudAndInventorySubmissionStayWithinAllocationBudget() {
        byte[] bytes = new byte[0x100000];
        writeRows(bytes, RomSymbols.InventoryTilemap.offset(), 0x7F);
        writeRows(bytes, RomSymbols.InventoryAttrmap.offset(), 0);
        Rom rom = Rom.of(bytes);
        var player = new PlayerState();
        player.playerInitialization().initializeDebugState();
        var menu = new InventoryMenu(InventoryTilemapLoader.loadFromRom(rom), player);
        var gpu = new GPU();
        gpu.loadBaseTiles(rom);
        var frame = new HardwareFrame(true);
        var retainedSnapshot = menu.closedWindow(null);
        int[] retainedTiles = retainedSnapshot.tiles().clone();
        int[] retainedAttributes = retainedSnapshot.attributes().clone();
        menu.requestToggle();
        for (int i = 0; i < 60; i++) menu.tick();
        assertTrue(menu.isFullyOpen());
        double bytesPerFrame = AllocationProbe.bytesPerIteration(2000, 1000, () -> {
            menu.tick();
            frame.reset();
            menu.submit(frame, gpu, null);
        });
        assertArrayEquals(retainedTiles, retainedSnapshot.tiles());
        assertArrayEquals(retainedAttributes, retainedSnapshot.attributes());
        System.out.println("HUD/inventory submission bytes/frame=" + bytesPerFrame);
        assertTrue(bytesPerFrame <= 8, "bytes/frame=" + bytesPerFrame);
    }

    private static void writeRows(byte[] bytes, int offset, int value) {
        for (int row = 0; row < 18; row++) {
            int address = 0x9800 + row * 32;
            bytes[offset++] = (byte) (address >>> 8);
            bytes[offset++] = (byte) address;
            bytes[offset++] = 0x53;
            bytes[offset++] = (byte) value;
        }
    }
}
