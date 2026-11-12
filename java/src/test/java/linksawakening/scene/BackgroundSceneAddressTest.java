package linksawakening.scene;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.rom.Rom;
import org.junit.jupiter.api.Test;

class BackgroundSceneAddressTest {
    @Test
    void retainsLegacyFileOffsetAliasesAndCrossWindowPaletteReads() {
        var loader = new BackgroundSceneLoader(Rom.of(new byte[0xC000]));
        // Bank 2:$3000 aliases bank 1:$7000 under the original romOffset conversion.
        // Object palettes start $40 after $7FF0, intentionally crossing the CPU window.
        var aliased = loader.load(new BackgroundSceneSpec(2, 0x3000, 2, 0x3000, 1, 0x7FF0));
        var canonical = loader.load(new BackgroundSceneSpec(1, 0x7000, 1, 0x7000, 1, 0x7FF0));
        assertArrayEquals(canonical.tilemap(), aliased.tilemap());
        assertArrayEquals(canonical.attrmap(), aliased.attrmap());
        assertEquals(0x7E, aliased.tilemap()[0]);
        assertEquals(0, aliased.attrmap()[0]);
        assertEquals(8, aliased.objectPalettes().length);
        assertArrayEquals(new int[4], aliased.objectPalettes()[7]);
    }
}
