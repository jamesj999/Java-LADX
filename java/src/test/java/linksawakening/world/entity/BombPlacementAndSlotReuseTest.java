package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class BombPlacementAndSlotReuseTest {

    @Test
    void spawnBombUsesTheRomSlotStateAndDoesNotSkipItsFirstTick() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        int slot = runtime.spawns().spawnBomb(0x40, 0x50, 0x07, 3);

        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, slot);
        RoomEntity bomb = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(0x02, bomb.type());
        assertEquals(-1, bomb.sourceLoadOrder());
        assertEquals(EntityStatus.ACTIVE, bomb.status());
        assertEquals(0x40, bomb.x());
        assertEquals(0x54, bomb.y());
        assertEquals(0x08, bomb.z());
        assertEquals(
                EntitySpriteDefinition.Shape.SINGLE, bomb.spriteDefinition().shape());
        assertEquals(0x03, bomb.spriteDefinition().bank());
        assertEquals(0x652E, bomb.spriteDefinition().address());
        assertEquals(0, bomb.spriteVariant());
        assertEquals(0xA0, runtime.enemies().transitionCountdown(slot));
        assertEquals(0xD2, runtime.enemies().physicsFlags(slot));
        assertEquals(0x0A, runtime.execution().slotProperties().options1(slot));
        assertEquals(3, runtime.inspection().bombDirection(slot));
        assertTrue(runtime.inspection().bombActive());
        assertTrue(runtime.inspection().lastBombPlacementPlayedBump());

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));

        RoomEntity afterFirstTick = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(slot, afterFirstTick.slot());
        assertEquals(0x02, afterFirstTick.type());
        assertEquals(0x9F, runtime.enemies().transitionCountdown(slot));
        assertEquals(1, runtime.inspection().snapshot().loadedEntities().size());
    }

    @Test
    void sideScrollBombPlacementSkipsTheTopViewBumpJingle() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())).withSideScrolling(true))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        assertTrue(runtime.spawns().spawnBomb(0x40, 0x50, 0x07, 3) >= 0);

        assertFalse(runtime.inspection().lastBombPlacementPlayedBump());
        assertEquals(0, runtime.inspection().snapshot().loadedEntities().get(0).z());
    }

    @Test
    void bombSpawnRejectsASecondLoadedBombAndReusesTheSlotAfterClear() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot()).build();

        int firstSlot = runtime.spawns().spawnBomb(0x40, 0x50, 0, 0);

        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, firstSlot);
        assertEquals(-1, runtime.spawns().spawnBomb(0x42, 0x52, 0, 1));

        runtime.lifecycle().clearEntity(firstSlot);

        assertFalse(runtime.inspection().bombActive());
        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, runtime.spawns().spawnBomb(0x42, 0x52, 0, 1));
    }

    @Test
    void bombSpawnRejectsAFullEntityTableWithoutChangingItsSlots() {
        List<RoomEntity> slots = new ArrayList<>();
        EntitySpriteDefinition unsupported = EntitySpriteDefinition.unsupported(0xFF);
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(new RoomEntityBuilder()
                    .slot(slot)
                    .sourceLoadOrder(slot)
                    .type(0xFF)
                    .at(0x20, 0x20)
                    .spriteDefinition(unsupported)
                    .spriteVariant(-1)
                    .build());
        }
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(new RoomEntitySnapshot(slots)).build();

        assertEquals(-1, runtime.spawns().spawnBomb(0x40, 0x50, 0, 0));
        assertFalse(runtime.inspection().bombActive());
        assertEquals(0xFF, runtime.inspection().snapshot().slots().get(15).type());
    }

    @Test
    void spawnBombFinalizesPendingFinalPresentationBeforeReusingItsSlot() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        int slot = runtime.spawns().spawnBomb(0x40, 0x50, 0, 0);

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(slot, 0x01);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));

        assertFalse(runtime.inspection().bombActive());
        assertEquals(slot, runtime.spawns().spawnBomb(0x44, 0x54, 0, 1));
        RoomEntity replacement = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(EntityStatus.ACTIVE, replacement.status());
        assertEquals(0x02, replacement.type());
        assertEquals(0x652E, replacement.spriteDefinition().address());
        assertEquals(0xA0, runtime.enemies().transitionCountdown(slot));
        assertTrue(runtime.inspection().bombActive());
        assertEquals(1, runtime.inspection().snapshot().loadedEntities().size());
    }

    @Test
    void alternateBombDisableClearsPendingPresentationAndDirectionBeforeReuse() throws Exception {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        int slot = runtime.spawns().spawnBomb(0x40, 0x50, 0, 3);

        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(slot, 0x01);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));

        assertTrue(runtime.execution().bombInteractions().state.bombFinalPresentationPending(slot));

        EntityRuntimeFixture.disableWithoutPersistence(runtime, slot);

        assertFalse(runtime.execution().bombInteractions().state.bombFinalPresentationPending(slot));
        assertEquals(0xFF, runtime.inspection().bombDirection(slot));
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(slot).status());
        assertEquals(slot, runtime.spawns().spawnBomb(0x44, 0x54, 0, 1));
        assertEquals(1, runtime.inspection().bombDirection(slot));
        assertFalse(runtime.execution().bombInteractions().state.bombFinalPresentationPending(slot));
    }
}
