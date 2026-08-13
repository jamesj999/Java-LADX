package linksawakening.entity;

import linksawakening.rom.RomBank;
import linksawakening.world.EntityRoomLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EntitySpriteCatalogTest {

    @Test
    void selectsGroupsAndFourSheetEntriesFromEachStandardRoomTable() {
        byte[] rom = syntheticRom();
        write(rom, 0x20, 0x70D3 + 0x05, 0x02);
        write(rom, 0x20, 0x71D3 + 0x05, 0x03);
        write(rom, 0x20, 0x72D3 + 0x05, 0x04);

        write(rom, 0x20, 0x73F3 + 0x02 * 4, 0xA4, 0xFF, 0x7C, 0xC8);
        write(rom, 0x20, 0x763B + 0x03 * 4, 0x90, 0x91, 0x92, 0xFF);
        write(rom, 0x20, 0x763B + 0x04 * 4, 0xA0, 0xA1, 0xA2, 0xA3);

        EntitySpriteCatalog catalog = new EntitySpriteCatalog(rom);

        EntitySpriteSelection overworld = catalog.load(
            EntityRoomLoader.RoomTable.OVERWORLD, 0x05);
        assertEquals(0x02, overworld.groupIndex());
        assertTrue(overworld.hasStandardSheets());
        assertArrayEquals(new int[] {0xA4, 0xFF, 0x7C, 0xC8}, overworld.sheetValues());
        assertEquals(0x03, overworld.burningSpriteDefinition().bank());
        assertEquals(0x4C44, overworld.burningSpriteDefinition().address());
        assertEquals(0x5488, overworld.deathSpriteDefinition().address());
        assertEquals(0x54C8, overworld.powerRecoilDeathSpriteDefinition().address());

        EntitySpriteSelection indoorsA = catalog.load(
            EntityRoomLoader.RoomTable.INDOORS_A, 0x05);
        assertEquals(0x03, indoorsA.groupIndex());
        assertArrayEquals(new int[] {0x90, 0x91, 0x92, 0xFF}, indoorsA.sheetValues());

        EntitySpriteSelection indoorsB = catalog.load(
            EntityRoomLoader.RoomTable.INDOORS_B, 0x05);
        assertEquals(0x04, indoorsB.groupIndex());
        assertArrayEquals(new int[] {0xA0, 0xA1, 0xA2, 0xA3}, indoorsB.sheetValues());
    }

    @Test
    void colorDungeonIsExplicitlyOutsideTheStandardSheetPath() {
        byte[] rom = syntheticRom();
        write(rom, 0x20, 0x70D3 + 0x00, 0x12);

        EntitySpriteSelection selection = new EntitySpriteCatalog(rom)
            .load(EntityRoomLoader.RoomTable.COLOR_DUNGEON, 0x00);

        assertEquals(0x12, selection.groupIndex());
        assertFalse(selection.hasStandardSheets());
        assertArrayEquals(new int[0], selection.sheetValues());
        assertEquals(0x4C44, selection.burningSpriteDefinition().address());
        assertEquals(0x5488, selection.deathSpriteDefinition().address());
        assertEquals(0x54C8, selection.powerRecoilDeathSpriteDefinition().address());
        assertNull(selection.spriteOverlayFor(0x86));
        assertNotNull(selection.spriteOverlayFor(0xE5));
        assertEquals(0x7AEB, selection.spriteOverlayFor(0xE5).address());
    }

    @Test
    void appliesRoomLoadSpriteGroupOverridesFromDisassembly() {
        byte[] rom = syntheticRom();
        write(rom, 0x20, 0x70D3 + 0x10, 0x23);
        write(rom, 0x20, 0x70D3 + 0x11, 0x21);
        write(rom, 0x20, 0x72D3 + 0xB5, 0x01);
        write(rom, 0x20, 0x73F3 + 0x24 * 4, 0x24, 0x25, 0x26, 0x27);
        write(rom, 0x20, 0x73F3 + 0x22 * 4, 0x22, 0x23, 0x24, 0x25);
        write(rom, 0x20, 0x763B + 0x3D * 4, 0x3D, 0x3E, 0x3F, 0x40);
        byte[] overworldRoomStatus = new byte[0x100];
        overworldRoomStatus[0xC9] = 0x20;
        overworldRoomStatus[0xFD] = 0x20;

        EntitySpriteCatalog catalog = new EntitySpriteCatalog(rom);

        EntitySpriteSelection siren = catalog.load(
            EntityRoomLoader.RoomTable.OVERWORLD, 0x10, 0x00, overworldRoomStatus);
        EntitySpriteSelection walrus = catalog.load(
            EntityRoomLoader.RoomTable.OVERWORLD, 0x11, 0x00, overworldRoomStatus);
        EntitySpriteSelection cameraShop = catalog.load(
            EntityRoomLoader.RoomTable.INDOORS_B, 0xB5, 0x10, null);

        assertEquals(0x24, siren.groupIndex());
        assertArrayEquals(new int[] {0x24, 0x25, 0x26, 0x27}, siren.sheetValues());
        assertEquals(0x22, walrus.groupIndex());
        assertArrayEquals(new int[] {0x22, 0x23, 0x24, 0x25}, walrus.sheetValues());
        assertEquals(0x3D, cameraShop.groupIndex());
        assertArrayEquals(new int[] {0x3D, 0x3E, 0x3F, 0x40}, cameraShop.sheetValues());
    }

    @Test
    void decodesSevenObjectPalettesAtTheDisassemblyAddress() {
        byte[] rom = syntheticRom();
        writePalette(rom, 0x21, 0x5518, 0x001F, 0x03E0, 0x7C00, 0x7FFF);

        int[][] palettes = new EntitySpriteCatalog(rom).loadObjectPalettes();

        assertEquals(6, palettes.length);
        assertEquals(4, palettes[0].length);
        assertEquals(0xFF0000, palettes[0][0]);
        assertEquals(0x00FF00, palettes[0][1]);
        assertEquals(0x0000FF, palettes[0][2]);
        assertEquals(0xFFFFFF, palettes[0][3]);
    }

    @Test
    void replacesObjectPaletteSixOnlyForEaglesTowerAfterRoomPaletteComposition() {
        byte[] rom = syntheticRom();
        write(rom, 0x21, 0x42B1, 0x00, 0x56);
        writePalette(rom, 0x21, 0x5548, 0x001F, 0x03E0, 0x7C00, 0x7FFF);

        EntitySpriteSelection normalRoom = new EntitySpriteCatalog(rom)
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0x00);
        EntitySpriteSelection eaglesTower = new EntitySpriteCatalog(rom)
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0x0E);

        assertEquals(8, normalRoom.objectPalettes().length);
        assertEquals(8, eaglesTower.objectPalettes().length);
        assertEquals(0xFF0000, eaglesTower.objectPalettes()[5][0]);
    }

    @Test
    void shippedRoom92UsesTheOverworldGroupAndItsFourRomEntries() throws Exception {
        byte[] rom = loadRom();
        EntitySpriteSelection selection = new EntitySpriteCatalog(rom)
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0x92);

        assertEquals(0x43, selection.groupIndex());
        assertArrayEquals(new int[] {0xA4, 0xE5, 0xE6, 0xDC}, selection.sheetValues());
    }

    @Test
    void shippedMarinHouseSupportsIndoorTarinDisplayList() throws Exception {
        EntitySpriteDefinition tarin = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(0x3F, EntityRoomLoader.RoomTable.INDOORS_B, 0x10);

        assertTrue(tarin.supported());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, tarin.shape());
        assertEquals(4, tarin.variantCount());
        assertEquals(0x4932, tarin.address());
        assertEquals(0x5A, tarin.variant(0).first().tile());
        assertEquals(0x58, tarin.variant(0).second().tile());
    }

    @Test
    void shippedOutdoorTarinSupportsRaccoonAndHumanTransformationVariants() throws Exception {
        EntitySpriteDefinition tarin = new EntitySpriteHandlerCatalog(loadRom())
            .forEntityType(0x3F, EntityRoomLoader.RoomTable.OVERWORLD, 0x51);

        assertEquals(12, tarin.variantCount());
        assertEquals(0x4912, tarin.address());
        assertEquals(0x72, tarin.variant(7).first().tile());
        assertEquals(0x5A, tarin.variant(8).first().tile());
        assertEquals(0x50, tarin.variant(11).first().tile());
    }

    @Test
    void preservesFloatingOverlayMetadataWhenSpriteOverridesAreChanged() throws Exception {
        EntitySpriteSelection selection = new EntitySpriteCatalog(loadRom())
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0x00);

        EntitySpriteDefinition overlay = selection.spriteOverlayFor(0x86);
        assertNotNull(overlay);
        assertSame(overlay, selection.spriteOverlayFor(0xE5));

        EntitySpriteSelection changed = selection.withSpriteOverride(
            0x6D, EntitySpriteDefinition.unsupported(0x6D));
        assertSame(overlay, changed.spriteOverlayFor(0x86));
        assertSame(overlay, changed.spriteOverlayFor(0xE5));
    }

    private static byte[] syntheticRom() {
        return new byte[RomBank.romOffset(0x22, 0x4000)];
    }

    private static void write(byte[] rom, int bank, int address, int... values) {
        int offset = RomBank.romOffset(bank, address);
        for (int value : values) {
            rom[offset++] = (byte) value;
        }
    }

    private static void writePalette(byte[] rom, int bank, int address, int... colors) {
        int offset = RomBank.romOffset(bank, address);
        for (int color : colors) {
            rom[offset++] = (byte) color;
            rom[offset++] = (byte) (color >> 8);
        }
    }

    private static byte[] loadRom() throws Exception {
        try (var stream = EntitySpriteCatalogTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
