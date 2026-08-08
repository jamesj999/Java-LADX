package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BooBuddyRuntimeTest {

    @Test
    void routesBooBuddyThroughItsRomDisplayAndTriggerStates() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        EntitySpriteDefinition definition = catalog.forEntityType(
            BooBuddyMotion.ENTITY_TYPE, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, BooBuddyMotion.ENTITY_TYPE, 0x40, 0x50,
                EntityStatus.INIT, definition, 0)), true, null, catalog);

        runtime.tick(0, 0x60, 0x50, () -> 0);
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.booBuddyState(0));
        assertEquals(0x12, runtime.physicsFlags(0));
        assertEquals(0x28, runtime.options1(0));
        assertEquals(0x79A9, runtime.snapshot().slots().get(0)
            .spriteDefinition().address());

        runtime.tick(1, 0x60, 0x50, () -> 0);
        assertEquals(4, runtime.booBuddySpeedX(0));
        assertEquals(0x08, runtime.resolveCombat(1, 0x40, 0x50, false, true,
            false, 0, 0, 0, 0).getFirst().linkDamage());
        assertTrue(runtime.resolveCombat(1, 0x40, 0x50, false, true,
            true, 0x40, 0x10, 0x50, 0x10).isEmpty());

        runtime.setBooBuddyTriggerCountForTest(1);
        runtime.tick(2, 0x60, 0x50, () -> 0);
        assertEquals(1, runtime.booBuddyState(0));
        assertEquals(0x08, runtime.resolveCombat(3, 0x40, 0x50, false, true,
            false, 0, 0, 0, 0).getFirst().linkDamage());

        runtime.tick(3, 0x60, 0x50, () -> 0);
        assertEquals(0xFC, runtime.booBuddySpeedX(0));
        assertEquals(0x01, runtime.enemyHealth(0));
        assertEquals(0x08, runtime.resolveCombat(5, 0x40, 0x50, false, true,
            false, 0, 0, 0, 0).getFirst().linkDamage());
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
        try (var stream = BooBuddyRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
