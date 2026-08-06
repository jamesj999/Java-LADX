package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class EntityRoomLoaderTest {

    @Test
    void decodesLocationAndUnsignedTypeIntoFixedEntitySlots() {
        byte[] rom = syntheticRom();
        writePointer(rom, EntityRoomLoader.RoomTable.OVERWORLD, 0, 0x5000);
        writeStream(rom, 0x5000,
            0x67, 0x80,
            0x24, 0xFE,
            0xFF);

        RoomEntitySnapshot snapshot = new EntityRoomLoader(rom)
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0);

        assertEquals(EntityRoomLoader.MAX_ENTITIES, snapshot.slots().size());

        RoomEntity first = snapshot.slots().get(0);
        assertEquals(0, first.slot());
        assertEquals(0, first.sourceLoadOrder());
        assertEquals(0x80, first.type());
        assertEquals(0x78, first.x());
        assertEquals(0x70, first.y());
        assertEquals(EntityStatus.INIT, first.status());

        RoomEntity second = snapshot.slots().get(1);
        assertEquals(1, second.sourceLoadOrder());
        assertEquals(0xFE, second.type());
        assertEquals(0x48, second.x());
        assertEquals(0x30, second.y());
        assertEquals(EntityStatus.INIT, second.status());

        assertEquals(EntityStatus.DISABLED, snapshot.slots().get(2).status());
    }

    @Test
    void selectsAllFourDisassemblyPointerTables() {
        byte[] rom = syntheticRom();
        List<EntityRoomLoader.RoomTable> tables = List.of(
            EntityRoomLoader.RoomTable.OVERWORLD,
            EntityRoomLoader.RoomTable.INDOORS_A,
            EntityRoomLoader.RoomTable.INDOORS_B,
            EntityRoomLoader.RoomTable.COLOR_DUNGEON);

        for (int index = 0; index < tables.size(); index++) {
            EntityRoomLoader.RoomTable table = tables.get(index);
            int address = 0x5000 + index * 0x20;
            writePointer(rom, table, 0, address);
            writeStream(rom, address, 0x10 + index, 0xA0 + index, 0xFF);

            RoomEntity entity = new EntityRoomLoader(rom).load(table, 0).slots().get(0);
            assertEquals(0xA0 + index, entity.type(), table.name());
            assertEquals(0x08 + index * 0x10, entity.x(), table.name());
            assertEquals(0x20, entity.y(), table.name());
        }
    }

    @Test
    void clearedDefinitionsAreSkippedButStillConsumeSourceLoadOrder() {
        byte[] rom = syntheticRom();
        writePointer(rom, EntityRoomLoader.RoomTable.OVERWORLD, 0, 0x5100);
        writeStream(rom, 0x5100,
            0x00, 0x20,
            0x11, 0x21,
            0x22, 0x22,
            0x33, 0x23,
            0x44, 0x24,
            0x55, 0x25,
            0x66, 0x26,
            0x77, 0x27,
            0x88, 0x28,
            0xFF);

        RoomEntitySnapshot snapshot = new EntityRoomLoader(rom).load(
            EntityRoomLoader.RoomTable.OVERWORLD, 0, 0b0101_0011);

        assertEquals(5, snapshot.loadedEntities().size());
        assertEquals(2, snapshot.loadedEntities().get(0).sourceLoadOrder());
        assertEquals(0x22, snapshot.loadedEntities().get(0).type());
        assertEquals(3, snapshot.loadedEntities().get(1).sourceLoadOrder());
        assertEquals(0x23, snapshot.loadedEntities().get(1).type());
        assertEquals(5, snapshot.loadedEntities().get(2).sourceLoadOrder());
        assertEquals(0x25, snapshot.loadedEntities().get(2).type());
        assertEquals(7, snapshot.loadedEntities().get(3).sourceLoadOrder());
        assertEquals(0x27, snapshot.loadedEntities().get(3).type());
        assertEquals(8, snapshot.loadedEntities().get(4).sourceLoadOrder());
        assertEquals(0x28, snapshot.loadedEntities().get(4).type());
    }

    @Test
    void appliesExactBankThreeInitialPositionTransforms() {
        byte[] rom = syntheticRom();
        writePointer(rom, EntityRoomLoader.RoomTable.OVERWORLD, 0, 0x5150);
        writeStream(rom, 0x5150,
            0x12, 0x33, // TreeOrPotDroppable: shift both axes on overworld.
            0x23, 0x39, // Instrument: shift X on every map.
            0x34, 0xC2, // Marin at Tal Tal: shift Y by -3.
            0xFF);

        List<RoomEntity> overworld = new EntityRoomLoader(rom)
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0)
            .loadedEntities();
        assertEquals(0x30, overworld.get(0).x());
        assertEquals(0x28, overworld.get(0).y());
        assertEquals(0x40, overworld.get(1).x());
        assertEquals(0x30, overworld.get(1).y());
        assertEquals(0x48, overworld.get(2).x());
        assertEquals(0x3D, overworld.get(2).y());

        writePointer(rom, EntityRoomLoader.RoomTable.INDOORS_A, 0, 0x5150);
        List<RoomEntity> indoor = new EntityRoomLoader(rom)
            .load(EntityRoomLoader.RoomTable.INDOORS_A, 0)
            .loadedEntities();
        assertEquals(0x28, indoor.get(0).x());
        assertEquals(0x20, indoor.get(0).y());
        assertEquals(0x40, indoor.get(1).x());
        assertEquals(0x30, indoor.get(1).y());
    }

    @Test
    void initializesOctorokWithTheHandlerTileOffset() {
        byte[] rom = syntheticRom();
        writePointer(rom, EntityRoomLoader.RoomTable.OVERWORLD, 0, 0x5170);
        writeStream(rom, 0x5170, 0x22, 0x09, 0xFF);

        RoomEntity octorok = new EntityRoomLoader(rom)
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0)
            .loadedEntities().get(0);

        assertEquals(0x09, octorok.type());
        assertEquals(0x30, octorok.spriteTileOffset());
    }

    @Test
    void initializesFloatingItemsWithPositionVariantAndRomInitialZ() {
        byte[] rom = syntheticRom();
        writePointer(rom, EntityRoomLoader.RoomTable.OVERWORLD, 0, 0x5170);
        writeStream(rom, 0x5170,
            0x00, 0x86,
            0x01, 0x86,
            0x10, 0xE5,
            0x11, 0xE5,
            0xFF);

        List<RoomEntity> entities = new EntityRoomLoader(rom)
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0)
            .loadedEntities();

        assertEquals(0, entities.get(0).spriteVariant());
        assertEquals(1, entities.get(1).spriteVariant());
        assertEquals(4, entities.get(2).spriteVariant());
        assertEquals(5, entities.get(3).spriteVariant());
        for (RoomEntity entity : entities) {
            assertEquals(0x13, entity.z());
        }
    }

    @Test
    void loadsColorShellsWithTheirInactiveRomRectangleDefinitions() {
        byte[] rom = syntheticRom();
        writePointer(rom, EntityRoomLoader.RoomTable.OVERWORLD, 0, 0x5180);
        writeStream(rom, 0x5180,
            0x12, 0xE9,
            0x23, 0xEA,
            0x34, 0xEB,
            0xFF);

        List<RoomEntity> shells = new EntityRoomLoader(rom)
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0)
            .loadedEntities();

        assertEquals(3, shells.size());
        assertEquals(EntitySpriteDefinition.Shape.RECTANGLE,
            shells.get(0).spriteDefinition().shape());
        assertEquals(0x67A8, shells.get(0).spriteDefinition().address());
        assertEquals(0x67D8, shells.get(1).spriteDefinition().address());
        assertEquals(0x6808, shells.get(2).spriteDefinition().address());
        assertEquals(4, shells.get(0).spriteDefinition().variantCount());
    }

    @Test
    void shiftsTreeSecretSeashellOnlyInItsTwoSpecialOverworldRooms() {
        byte[] rom = syntheticRom();
        writePointer(rom, EntityRoomLoader.RoomTable.OVERWORLD, 0xA4, 0x5160);
        writeStream(rom, 0x5160, 0x12, 0x3D, 0xFF);
        writePointer(rom, EntityRoomLoader.RoomTable.OVERWORLD, 0x10, 0x5160);

        RoomEntity special = new EntityRoomLoader(rom)
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0xA4)
            .loadedEntities().get(0);
        RoomEntity ordinary = new EntityRoomLoader(rom)
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0x10)
            .loadedEntities().get(0);

        assertEquals(0x30, special.x());
        assertEquals(0x28, special.y());
        assertEquals(0x28, ordinary.x());
        assertEquals(0x20, ordinary.y());
    }

    @Test
    void emptyStreamsAndSeventeenthDefinitionRespectSixteenSlots() {
        byte[] rom = syntheticRom();
        writePointer(rom, EntityRoomLoader.RoomTable.OVERWORLD, 0, 0x5200);
        writeStream(rom, 0x5200, 0xFF);

        RoomEntitySnapshot empty = new EntityRoomLoader(rom)
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0);
        assertEquals(16, empty.slots().size());
        assertEquals(0, empty.loadedEntities().size());

        writePointer(rom, EntityRoomLoader.RoomTable.OVERWORLD, 1, 0x5300);
        int[] definitions = new int[17 * 2 + 1];
        for (int i = 0; i < 17; i++) {
            definitions[i * 2] = i;
            definitions[i * 2 + 1] = 0x40 + i;
        }
        definitions[34] = 0xFF;
        writeStream(rom, 0x5300, definitions);

        RoomEntitySnapshot full = new EntityRoomLoader(rom)
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 1);
        assertEquals(16, full.loadedEntities().size());
        assertEquals(0x40, full.loadedEntities().get(0).type());
        assertEquals(0x4F, full.loadedEntities().get(15).type());
    }

    @Test
    void rejectsInvalidRoomAndTruncatedDefinition() {
        byte[] rom = syntheticRom();
        EntityRoomLoader loader = new EntityRoomLoader(rom);

        assertThrows(IllegalArgumentException.class,
            () -> loader.load(EntityRoomLoader.RoomTable.OVERWORLD, -1));
        assertThrows(IllegalArgumentException.class,
            () -> loader.load(EntityRoomLoader.RoomTable.OVERWORLD, 0x100));

        writePointer(rom, EntityRoomLoader.RoomTable.OVERWORLD, 0, 0x5400);
        writeStream(rom, 0x5400, 0x10);
        assertThrows(IllegalArgumentException.class,
            () -> loader.load(EntityRoomLoader.RoomTable.OVERWORLD, 0));
    }

    @Test
    void shippedRomContainsKnownOverworldDefinitionsInSourceOrder() throws Exception {
        RoomEntitySnapshot room00 = new EntityRoomLoader(loadRom())
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0x00);

        assertEquals(List.of(0x29, 0x35, 0x7A),
            room00.loadedEntities().stream().map(RoomEntity::type).toList());
        assertEquals(0x78, room00.loadedEntities().get(0).x());
        assertEquals(0x70, room00.loadedEntities().get(0).y());
        assertEquals(0x48, room00.loadedEntities().get(1).x());
        assertEquals(0x30, room00.loadedEntities().get(1).y());

        RoomEntitySnapshot room92 = new EntityRoomLoader(loadRom())
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0x92);
        assertEquals(List.of(0x73, 0x3E, 0x6F, 0x6E, 0x6E, 0x6E, 0xDC),
            room92.loadedEntities().stream().map(RoomEntity::type).toList());
        assertEquals(0, room92.loadedEntities().get(0).sourceLoadOrder());
        assertEquals(5, room92.loadedEntities().get(5).sourceLoadOrder());
    }

    @Test
    void shippedRomUsesIndoorAndColorDungeonPointerTables() throws Exception {
        EntityRoomLoader loader = new EntityRoomLoader(loadRom());

        assertEquals(List.of(0x39), loader
            .load(EntityRoomLoader.RoomTable.INDOORS_A, 0x02)
            .loadedEntities().stream().map(RoomEntity::type).toList());
        assertEquals(List.of(0x91, 0x9F), loader
            .load(EntityRoomLoader.RoomTable.INDOORS_B, 0x01)
            .loadedEntities().stream().map(RoomEntity::type).toList());
        assertEquals(List.of(0xF9), loader
            .load(EntityRoomLoader.RoomTable.COLOR_DUNGEON, 0x00)
            .loadedEntities().stream().map(RoomEntity::type).toList());
    }

    private static byte[] syntheticRom() {
        return new byte[RomBank.romOffset(0x20, 0x8000)];
    }

    private static void writePointer(byte[] rom, EntityRoomLoader.RoomTable table,
                                     int roomId, int address) {
        int offset = RomBank.romOffset(0x16, table.pointerAddress() + roomId * 2);
        rom[offset] = (byte) address;
        rom[offset + 1] = (byte) (address >> 8);
    }

    private static void writeStream(byte[] rom, int address, int... bytes) {
        int offset = RomBank.romOffset(0x16, address);
        for (int value : bytes) {
            rom[offset++] = (byte) value;
        }
    }

    private static byte[] loadRom() throws Exception {
        try (var stream = EntityRoomLoaderTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
