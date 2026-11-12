package linksawakening.world.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.RomBank;
import linksawakening.world.entity.motion.MotionTestAccess;

/** Shared synthetic definitions and ROM access for the runtime family tests. */
abstract class EntityRuntimeTestSupport {

    static void setPhysicsFlags(RoomEntityRuntime runtime, int slot, int flags) {
        EntityRuntimeFixture.debug(runtime).setPhysicsFlagsForTest(slot, flags);
    }

    static RoomEntity ghiniEntity(EntitySpriteHandlerCatalog catalog, int slot, int type, EntityStatus status) {
        EntitySpriteDefinition definition = catalog.forEntityType(type, EntityRoomLoader.RoomTable.OVERWORLD);
        int initialZ = type == 0x12 ? 0x10 : 0;
        return new linksawakening.world.entity.RoomEntityBuilder()
                .slot(slot)
                .sourceLoadOrder(slot)
                .type(type)
                .at(0x50, 0x50)
                .status(status)
                .spriteDefinition(definition)
                .spriteVariant(definition.initialVariant())
                .z(initialZ)
                .build();
    }

    static EntitySpriteDefinition pairDefinition(int type, int variants) {
        List<EntitySpriteDefinition.Variant> displayList = new ArrayList<>();
        for (int i = 0; i < variants; i++) {
            displayList.add(new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x14, 0x02),
                    new EntitySpriteDefinition.OamAttribute(0x14, 0x22)));
        }
        return new EntitySpriteDefinition(type, 0x03, 0x5B65, EntitySpriteDefinition.Shape.PAIR, 0, displayList);
    }

    static EntitySpriteDefinition keyDropDefinition() {
        List<EntitySpriteDefinition.Variant> displayList = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            displayList.add(
                    new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0xCA, 0x17), null));
        }
        return new EntitySpriteDefinition(0x30, 0x03, 0x5C78, EntitySpriteDefinition.Shape.SINGLE, 0, displayList);
    }

    static EntitySpriteDefinition butterflyDefinition() {
        return new EntitySpriteDefinition(
                0x6E,
                0x06,
                0x6BBD,
                EntitySpriteDefinition.Shape.SINGLE,
                0,
                List.of(
                        new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x5E, 0x01), null),
                        new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x5E, 0x41), null)));
    }

    static EntitySpriteDefinition keeseDefinition() {
        return new EntitySpriteDefinition(
                0x19,
                0x06,
                0x6708,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                List.of(
                        new EntitySpriteDefinition.Variant(
                                new EntitySpriteDefinition.OamAttribute(0x42, 0x00),
                                new EntitySpriteDefinition.OamAttribute(0x42, 0x20)),
                        new EntitySpriteDefinition.Variant(
                                new EntitySpriteDefinition.OamAttribute(0x40, 0x00),
                                new EntitySpriteDefinition.OamAttribute(0x40, 0x20))));
    }

    static RoomEntity dyingEntity(int slot, int sourceLoadOrder, int type, EntitySpriteHandlerCatalog catalog) {
        EntitySpriteDefinition definition = catalog.forEntityType(type, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        return new linksawakening.world.entity.RoomEntityBuilder()
                .slot(slot)
                .sourceLoadOrder(sourceLoadOrder)
                .type(type)
                .at(0x40, 0x50)
                .status(EntityStatus.DYING)
                .spriteDefinition(definition)
                .spriteVariant(definition.initialVariant())
                .build();
    }

    static RoomEntitySnapshot snapshotAt(RoomEntity entity) {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        slots.set(entity.slot(), entity);
        return new RoomEntitySnapshot(slots);
    }

    static RoomEntitySnapshot snapshotWithSlots(RoomEntity... entities) {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        for (RoomEntity entity : entities) {
            slots.set(entity.slot(), entity);
        }
        return new RoomEntitySnapshot(slots);
    }

    static IntSupplier sequence(int... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[Math.min(index.getAndIncrement(), values.length - 1)];
    }

    static final class RuntimeColorShellWorld implements ColorShellWorld {
        int objectValue;
        final List<Integer> objectWrites = new ArrayList<>();
        final List<Integer> noises = new ArrayList<>();
        final List<List<Integer>> poofs = new ArrayList<>();

        @Override
        public int objectAt(RoomEntity entity, int relativeOffset) {
            return objectValue;
        }

        @Override
        public void writeObject(RoomEntity entity, int objectId) {
            objectWrites.add(objectId);
        }

        @Override
        public void playJingle(int id) {}

        @Override
        public void playNoise(int id) {
            noises.add(id);
        }

        @Override
        public void spawnPoof(int x, int y) {
            poofs.add(List.of(x, y));
        }
    }

    static byte[] romWithSwordResult(int entityType, int rawValue) throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        int healthGroup = tables.healthGroup(entityType);
        int damageMatrixOffset = RomBank.romOffset(0x03, 0x43EC);
        int damageValuesOffset = RomBank.romOffset(0x03, 0x473C);
        // Force level-one sword damage to use entry 1, then replace that
        // shared raw value with the controlled special result under test.
        rom[damageMatrixOffset + healthGroup * 0x10] = 0x01;
        rom[damageValuesOffset + 0x01] = (byte) rawValue;
        return rom;
    }

    static byte[] syntheticRom() {
        return new byte[0x100000];
    }

    static void write(byte[] rom, int bank, int address, int... values) {
        int offset = RomBank.romOffset(bank, address);
        for (int value : values) {
            rom[offset++] = (byte) value;
        }
    }

    static byte[] loadRom() throws IOException {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }

    static RoomEntitySnapshot snapshot(RoomEntity... entities) {
        List<RoomEntity> slots = new ArrayList<>(List.of(entities));
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots);
    }

    static RoomEntity threeOfAKind(int slot, int x, int y) {
        return new linksawakening.world.entity.RoomEntityBuilder()
                .slot(slot)
                .sourceLoadOrder(slot)
                .type(MotionTestAccess.ThreeOfAKindMotion_ENTITY_TYPE)
                .at(x, y)
                .spriteDefinition(EntitySpriteDefinition.unsupported(MotionTestAccess.ThreeOfAKindMotion_ENTITY_TYPE))
                .build();
    }
}
