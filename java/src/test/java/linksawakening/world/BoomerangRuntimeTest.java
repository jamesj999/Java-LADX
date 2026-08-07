package linksawakening.world;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BoomerangRuntimeTest {

    @Test
    void spawnPublishesTheRomEntityAndLaunchesThenReturns() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables combatTables = new RomEnemyCombatTables(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot(), false, () -> 0,
            catalog, combatTables, new ChestContentsTable(rom));

        int slot = runtime.spawnBoomerang(0x40, 0x40, 0, 0, 0);

        assertEquals(0x0F, slot);
        assertTrue(runtime.boomerangActive());
        assertEquals(0x19, runtime.snapshot().slots().get(slot).spriteDefinition().bank());
        assertEquals(0x4451, runtime.snapshot().slots().get(slot).spriteDefinition().address());
        assertEquals(0x28, runtime.boomerangTransitionCountdown(slot));

        tickInteractive(runtime, 0, 0x20, 0x20);

        RoomEntity outbound = runtime.snapshot().slots().get(slot);
        assertEquals(0x42, outbound.x());
        assertEquals(0x27, runtime.boomerangTransitionCountdown(slot));
        assertEquals(0, runtime.boomerangState(slot));
        assertTrue(runtime.consumePendingEntityEvents().stream()
            .anyMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                && event.soundId() == 0x2D));

        for (int frame = 1; frame < 40; frame++) {
            tickInteractive(runtime, frame, 0x20, 0x20);
            outbound = runtime.snapshot().slots().get(slot);
        }

        assertTrue(runtime.boomerangActive());
        assertEquals(1, runtime.boomerangState(slot));

        for (int frame = 40; frame < 120 && runtime.boomerangActive(); frame++) {
            tickInteractive(runtime, frame, 0x20, 0x20);
        }

        assertFalse(runtime.boomerangActive());
    }

    @Test
    void objectGuardAndBushIntersectionUseThePaddedLocation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot(), false, () -> 0,
            catalog, new RomEnemyCombatTables(rom), new ChestContentsTable(rom));
        int slot = runtime.spawnBoomerang(0x20, 0x40, 0, 0, 0);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            0x5C, 0x10, 0x30, 0x40));

        runtime.tick(0, 0x20, 0x40, () -> 0);
        assertEquals(1, runtime.boomerangObjectRequests().size());
        RoomEntityRuntime.BoomerangObjectRequest request =
            runtime.boomerangObjectRequests().get(0);

        runtime.setObjectIntersectionQuery(entity -> RoomEntityObjectSample.none());
        runtime.tick(1, 0x20, 0x40, () -> 0);
        runtime.tick(2, 0x20, 0x40, () -> 0);

        assertEquals(slot, request.sourceSlot());
        assertEquals(0x43, request.location());
        assertEquals(0x30, request.objectLeft());
        assertEquals(0x40, request.objectTop());
    }

    @Test
    void playerProjectileGateMatchesTheRomCounter() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot(), false, () -> 0,
            catalog, new RomEnemyCombatTables(rom), new ChestContentsTable(rom));

        int boomerangSlot = runtime.spawnBoomerang(0x40, 0x40, 0, 0, 0);

        assertEquals(1, runtime.activeProjectileCount());
        assertEquals(0x0E, runtime.spawnArrow(0x40, 0x40, 0, 0));
        assertEquals(-1, runtime.spawnBoomerang(0x40, 0x40, 0, 0, 0));

        runtime.clearEntity(boomerangSlot);
        assertEquals(1, runtime.activeProjectileCount());
        assertEquals(-1, runtime.spawnBoomerang(0x40, 0x40, 0, 0, 0));
    }

    private static RoomEntitySnapshot emptySnapshot() {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots);
    }

    private static void tickInteractive(RoomEntityRuntime runtime, int frame,
                                         int linkX, int linkY) {
        runtime.tickWithProjectileEvents(frame, linkX, linkY, () -> 0, null,
            new EnemyProjectileCollision.LinkState(linkX, linkY, 0, 0, 0, false));
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = BoomerangRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
