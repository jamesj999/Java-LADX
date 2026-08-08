package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FairyRuntimeTest {

    @Test
    void decodesTheRomDroppableFairyPair() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());

        var definition = catalog.forEntityType(0x2F, EntityRoomLoader.RoomTable.OVERWORLD);

        assertTrue(definition.supported());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
        assertEquals(0x03, definition.bank());
        assertEquals(0x6157, definition.address());
        assertEquals(2, definition.variantCount());
        assertEquals(0x20, definition.variant(0).first().tile());
        assertEquals(0x21, definition.variant(0).first().attributes());
        assertEquals(0x20, definition.variant(0).second().tile());
        assertEquals(0x01, definition.variant(0).second().attributes());
    }

    @Test
    void runsTheFairyHandlerWithItsRomPhysicsAndOptions() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        var definition = catalog.forEntityType(0x2F, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x2F, 0x40, 0x40, EntityStatus.ACTIVE,
                definition, 0, 0, 0, 0x10)), false, () -> 0, catalog);

        runtime.tick(0, 0x50, 0x40, sequence(0x0F, 0x00));

        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0xB1, runtime.physicsFlags(0));
        assertEquals(0x13, runtime.options1(0));
        assertEquals(0x30, runtime.transitionCountdown(0));
        assertEquals(0x07, runtime.fairySpeedX(0));
        assertEquals(0xF8, runtime.fairySpeedY(0));
        assertTrue(runtime.snapshot().slots().get(0).spriteDefinition().supported());
    }

    @Test
    void enemyDeathSpawnsTheFairyWithoutBouncingDropPhysics() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntity source = new RoomEntity(15, 0, 0x09, 0x44, 0x58,
            EntityStatus.DYING, catalog.forEntityType(
                0x09, EntityRoomLoader.RoomTable.OVERWORLD, -1), 0, 0, 0, 0x10);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(source), false,
            () -> 0, catalog, tables);
        runtime.setEnemyDropResolver(new EnemyDropResolver(rom));
        runtime.setDroppedItemForTest(15, 0x2F);

        runtime.tick(0, 0, 0, () -> 0);
        assertEquals(0x2F, runtime.snapshot().slots().get(14).type());

        runtime.tick(1, 0, 0, () -> 0);

        RoomEntity fairy = runtime.snapshot().slots().get(14);
        assertEquals(0x10, fairy.z());
        assertEquals(0xB1, runtime.physicsFlags(14));
        assertEquals(0x13, runtime.options1(14));
        assertEquals(0, runtime.dropSpeedZ(14));
    }

    private static IntSupplier sequence(int... values) {
        return new IntSupplier() {
            private int index;

            @Override
            public int getAsInt() {
                return values[Math.min(index++, values.length - 1)];
            }
        };
    }

    private static RoomEntitySnapshot snapshot(RoomEntity entity) {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        slots.set(entity.slot(), entity);
        return new RoomEntitySnapshot(slots);
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = FairyRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
