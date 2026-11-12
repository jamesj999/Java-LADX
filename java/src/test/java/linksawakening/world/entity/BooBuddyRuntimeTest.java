package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.motion.BooBuddyMotion;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class BooBuddyRuntimeTest {

    @Test
    void sharedTimerReachingOneRunsTheRomDisappearanceReposition() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        EntitySpriteDefinition definition =
                catalog.forEntityType(BooBuddyMotion.ENTITY_TYPE, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(BooBuddyMotion.ENTITY_TYPE)
                        .at(0x30, 0x40)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 2);

        runtime.tick(EntityTestFrames.frame(runtime, 4, 0x50, 0x40, () -> 0));

        assertEquals(1, runtime.flyingEnemies().booBuddyTransitionCountdown(0));
        assertEquals(0x70, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x50, runtime.inspection().snapshot().slots().get(0).y());
        runtime.tick(EntityTestFrames.frame(runtime, 5, 0x50, 0x40, () -> 0));
        assertEquals(0x70, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x50, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0, runtime.flyingEnemies().booBuddySpeedX(0));
    }

    @Test
    void routesBooBuddyThroughItsRomDisplayAndTriggerStates() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        EntitySpriteDefinition definition =
                catalog.forEntityType(BooBuddyMotion.ENTITY_TYPE, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(BooBuddyMotion.ENTITY_TYPE)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x60, 0x50, () -> 0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, runtime.flyingEnemies().booBuddyState(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));
        assertEquals(0x28, runtime.options1(0));
        assertEquals(
                0x79A9,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(0)
                        .spriteDefinition()
                        .address());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x60, 0x50, () -> 0));
        assertEquals(4, runtime.flyingEnemies().booBuddySpeedX(0));
        assertEquals(
                0x08,
                runtime.resolveCombat(new EntitySwordContactFrame(1, 0x40, 0x50, false, true, false, 0, 0, 0, 0))
                        .getFirst()
                        .linkDamage());
        assertTrue(runtime.resolveCombat(
                        new EntitySwordContactFrame(1, 0x40, 0x50, false, true, true, 0x40, 0x10, 0x50, 0x10))
                .isEmpty());

        EntityRuntimeFixture.debug(runtime).setBooBuddyTriggerCountForTest(1);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x60, 0x50, () -> 0));
        assertEquals(1, runtime.flyingEnemies().booBuddyState(0));
        assertEquals(
                0x08,
                runtime.resolveCombat(new EntitySwordContactFrame(3, 0x40, 0x50, false, true, false, 0, 0, 0, 0))
                        .getFirst()
                        .linkDamage());

        runtime.tick(EntityTestFrames.frame(runtime, 3, 0x60, 0x50, () -> 0));
        assertEquals(0xFC, runtime.flyingEnemies().booBuddySpeedX(0));
        assertEquals(0x01, runtime.enemies().enemyHealth(0));
        assertEquals(
                0x08,
                runtime.resolveCombat(new EntitySwordContactFrame(5, 0x40, 0x50, false, true, false, 0, 0, 0, 0))
                        .getFirst()
                        .linkDamage());
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
