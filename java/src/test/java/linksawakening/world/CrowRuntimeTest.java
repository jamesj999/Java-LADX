package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CrowRuntimeTest {

    @Test
    void routesCrowThroughTheRomHandlerAndGatesCombatUntilTakeoff() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        EntitySpriteDefinition definition = catalog.forEntityType(
            CrowMotion.ENTITY_TYPE, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, CrowMotion.ENTITY_TYPE, 0x40, 0x50,
                EntityStatus.INIT, definition, 0)), false, null, catalog);

        runtime.tick(0, 0x44, 0x50, () -> 0);
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.crowState(0));
        assertEquals(0x52, runtime.physicsFlags(0));
        assertTrue(runtime.resolveCombat(1, 0x40, 0x50, false, true,
            false, 0, 0, 0, 0).isEmpty());

        runtime.tick(1, 0x44, 0x50, () -> 0);
        RoomEntity triggered = runtime.snapshot().slots().get(0);
        assertEquals(1, runtime.crowState(0));
        assertEquals(0x22, runtime.crowTransitionCountdown(0));
        assertEquals(0x4C, triggered.y());
        assertEquals(0x12, runtime.physicsFlags(0));
        assertEquals(0x20, runtime.options1(0));

        assertEquals(0x08, runtime.resolveCombat(3, triggered.x(), triggered.y(), false,
            true, false, 0, 0, 0, 0).getFirst().linkDamage());

        runtime.tick(2, 0x44, 0x50, () -> 0);
        assertTrue(runtime.consumePendingEntityEvents().stream()
            .anyMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                && event.soundId() == 0x2D));
        assertFalse(runtime.snapshot().slots().get(0).status() == EntityStatus.DISABLED);
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
        try (var stream = CrowRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
