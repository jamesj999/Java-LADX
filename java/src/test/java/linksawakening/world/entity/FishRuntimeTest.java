package linksawakening.world.entity;



import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntitySnapshot;

import linksawakening.world.entity.motion.FishMotion;

import linksawakening.world.entity.event.TransientVfxRequest;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.vfx.TransientVfxType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@linksawakening.testing.RequiresRom
final class FishRuntimeTest {

    @Test
    void routesFishThroughTheRomMotionHandlerAndEmitsTheWaterSplash() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        EntitySpriteDefinition definition = catalog.forEntityType(
            FishMotion.ENTITY_TYPE, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, FishMotion.ENTITY_TYPE, 0x40, 0x50,
                EntityStatus.INIT, definition, 0, 0, 0, 0, -1, false)), false, null, catalog);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertEquals(EntityStatus.ACTIVE, runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, runtime.flyingEnemies().fishState(0));
        assertEquals(0x52, runtime.enemies().physicsFlags(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));
        assertEquals(1, runtime.flyingEnemies().fishState(0));
        assertEquals(0x08, runtime.flyingEnemies().fishSpeedX(0));
        assertEquals(0x40, runtime.flyingEnemies().fishTransitionCountdown(0));

        for (int frame = 2; frame < 0x90; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
            List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
            if (!events.isEmpty()) {
                assertEquals(2, runtime.flyingEnemies().fishState(0));
                assertEquals(0x12, runtime.enemies().physicsFlags(0));
                RoomEntity fish = runtime.inspection().snapshot().slots().get(0);
                assertEquals(List.of(new TransientVfxRequest(
                    TransientVfxType.WATER_SPLASH, fish.x(), fish.y())),
                    runtime.enemies().transientVfxRequests());
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
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
