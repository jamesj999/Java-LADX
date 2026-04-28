package linksawakening.entity;

import linksawakening.gpu.Tile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LinkSpriteSheetTest {

    private static final int BANK_SIZE = 0x4000;

    @Test
    void linkBodyTilesComeFromAdjustedCgbBank() {
        byte[] romData = new byte[romOffset(0x2C, 0x5800) + 0x1000];
        romData[romOffset(0x0C, 0x5800)] = (byte) 0x40;
        romData[romOffset(0x2C, 0x5800)] = (byte) 0x80;
        romData[romOffset(0x20, 0x5319)] = 0x00;
        romData[romOffset(0x20, 0x531A)] = 0x02;

        LinkSpriteSheet spriteSheet = LinkSpriteSheet.loadFromRom(romData);
        Tile[] resolved = new Tile[4];

        spriteSheet.resolveTiles(0, resolved);

        assertEquals(1, resolved[0].getPixel(0, 0));
    }

    private static int romOffset(int bank, int address) {
        if (bank == 0) {
            return address;
        }
        return bank * BANK_SIZE + (address - BANK_SIZE);
    }
}
