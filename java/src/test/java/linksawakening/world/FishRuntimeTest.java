package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.vfx.TransientVfxType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class FishRuntimeTest {

    @Test
    void routesFishThroughTheRomMotionHandlerAndEmitsTheWaterSplash() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        EntitySpriteDefinition definition = catalog.forEntityType(
            FishMotion.ENTITY_TYPE, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, FishMotion.ENTITY_TYPE, 0x40, 0x50,
                EntityStatus.INIT, definition, 0)), false, null, catalog);

        runtime.tick(0, 0, 0, () -> 0);
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.fishState(0));
        assertEquals(0x52, runtime.physicsFlags(0));

        runtime.tick(1, 0, 0, () -> 0);
        assertEquals(1, runtime.fishState(0));
        assertEquals(0x08, runtime.fishSpeedX(0));
        assertEquals(0x40, runtime.fishTransitionCountdown(0));

        for (int frame = 2; frame < 0x90; frame++) {
            runtime.tick(frame, 0, 0, () -> 0);
            List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
            if (!events.isEmpty()) {
                assertEquals(2, runtime.fishState(0));
                assertEquals(0x12, runtime.physicsFlags(0));
                RoomEntity fish = runtime.snapshot().slots().get(0);
                assertEquals(List.of(new RoomEntityRuntime.TransientVfxRequest(
                    TransientVfxType.WATER_SPLASH, fish.x(), fish.y())),
                    runtime.transientVfxRequests());
                assertEquals(List.of(new EntityCombatEvent(0, FishMotion.ENTITY_TYPE, 0, false,
                    EntityCombatEvent.SoundChannel.JINGLE, 0x0E)), events);

                int combatFrame = (frame & 0x01) == 0 ? frame + 1 : frame;
                List<EntityCombatEvent> contact = runtime.resolveCombat(combatFrame & 0xFF,
                    fish.x(), fish.y(), false, true, false, 0, 0, 0, 0);
                assertEquals(1, contact.size());
                assertEquals(0x04, contact.get(0).linkDamage());
                assertFalse(contact.get(0).swordHit());
                return;
            }
        }

        throw new AssertionError("Fish never launched from its hidden swim state");
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
        try (var stream = FishRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
