package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.CrowMotion;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class CrowRuntimeTest {

    @Test
    void routesCrowThroughTheRomHandlerAndGatesCombatUntilTakeoff() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(loadRom()));
        EntitySpriteDefinition definition =
                catalog.forEntityType(CrowMotion.ENTITY_TYPE, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(CrowMotion.ENTITY_TYPE)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x44, 0x50, () -> 0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, runtime.flyingEnemies().crowState(0));
        assertEquals(0x52, runtime.enemies().physicsFlags(0));
        assertTrue(runtime.resolveCombat(new EntitySwordContactFrame(1, 0x40, 0x50, false, true, false, 0, 0, 0, 0))
                .isEmpty());

        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(0x58);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x44, 0x50, () -> 0));
        assertEquals(0, runtime.flyingEnemies().crowState(0));

        int bombSlot = runtime.spawnBomb(0x38, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bombSlot, 0x13);
        EntityRuntimeFixture.debug(runtime).setEnemyFlashCountdownForTest(bombSlot, 0x10);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x44, 0x50, () -> 0));
        RoomEntity triggered = runtime.inspection().snapshot().slots().get(0);
        assertEquals(1, runtime.flyingEnemies().crowState(0));
        assertEquals(0x22, runtime.flyingEnemies().crowTransitionCountdown(0));
        assertEquals(0x4C, triggered.y());
        assertEquals(0x12, runtime.enemies().physicsFlags(0));
        assertEquals(0x20, runtime.options1(0));

        assertEquals(
                0x08,
                runtime.resolveCombat(new EntitySwordContactFrame(
                                3, triggered.x(), triggered.y(), false, true, false, 0, 0, 0, 0))
                        .getFirst()
                        .linkDamage());

        runtime.tick(EntityTestFrames.frame(runtime, 3, 0x44, 0x50, () -> 0));
        assertFalse(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x2D));
        // Crow calls the same read-only sound routine as the boomerang.
        runtime.configuration().setBoomerangSoundCounterForFrame(0);
        runtime.tick(EntityTestFrames.frame(runtime, 4, 0x44, 0x50, () -> 0));
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x2D));
        assertFalse(runtime.inspection().snapshot().slots().get(0).status() == EntityStatus.DISABLED);
    }

    @Test
    void takeoffKeepsFacingAndPresentsThePreviousAltitudeWingFrame() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(loadRom()));
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(CrowMotion.ENTITY_TYPE)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(
                                catalog.forEntityType(CrowMotion.ENTITY_TYPE, EntityRoomLoader.RoomTable.OVERWORLD))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x3C, 0x50, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x3C, 0x50, () -> 0));
        assertEquals(1, runtime.flyingEnemies().crowState(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 2; frame <= 17; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x3C, 0x50, () -> 0));
            assertEquals(
                    0,
                    runtime.inspection().snapshot().slots().get(0).spriteVariant(),
                    "The unmirrored wing frame stays zero until the Z=$08 write is presented");
        }
        assertEquals(8, runtime.inspection().snapshot().slots().get(0).z());
        runtime.tick(EntityTestFrames.frame(runtime, 18, 0x3C, 0x50, () -> 0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void kanaletCrowUsesTheSourceLeafDropAndRoomCompletionPersistence() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(loadRom()));
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(4)
                        .type(CrowMotion.ENTITY_TYPE)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(
                                catalog.forEntityType(CrowMotion.ENTITY_TYPE, EntityRoomLoader.RoomTable.OVERWORLD))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(0x58);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        RoomEntity crow = runtime.inspection().snapshot().slots().get(0);
        assertEquals(0xFF, crow.sourceLoadOrder());
        assertEquals(0x3C, EntityRuntimeFixture.inspection(runtime).droppedItemForTest(0));

        EntityRuntimeFixture.debug(runtime).setEntityRoomStatusForTest(0x10);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void kanaletCrowTakesOffForANearbySmashedRock() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(loadRom()));
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(CrowMotion.ENTITY_TYPE)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(
                                catalog.forEntityType(CrowMotion.ENTITY_TYPE, EntityRoomLoader.RoomTable.OVERWORLD))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(0x58);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x40, 0x50, () -> 0));
        assertEquals(0, runtime.flyingEnemies().crowState(0));

        runtime.spawnLiftableRockSmash(0x40, 0x50, 0);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0, 0, () -> 0));

        assertEquals(1, runtime.flyingEnemies().crowState(0));
    }

    @Test
    void kanaletCrowAppliesThePatchedIgnoreHitsReductionBeforeItsHandler() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(loadRom()));
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(CrowMotion.ENTITY_TYPE)
                        .at(0x40, 0x50)
                        .spriteDefinition(
                                catalog.forEntityType(CrowMotion.ENTITY_TYPE, EntityRoomLoader.RoomTable.OVERWORLD))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(0x58);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x20);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        assertEquals(0x07, runtime.enemies().enemyIgnoreHitsCountdown(0));
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
