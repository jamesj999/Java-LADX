package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.room.ChestContentsTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@linksawakening.testing.RequiresRom
final class SwordBeamRuntimeTest {

    @Test
    void spawnRunsTheRomInitialStateThenMovesAndEmitsBeamVfx() throws IOException {
        RoomEntityRuntime runtime = runtime();
        int slot = runtime.spawnSwordBeam(0x40, 0x40, 0, 0);

        assertEquals(0x0F, slot);
        assertEquals(1, runtime.inspection().activeProjectileCount());
        assertEquals(
                0x19,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(slot)
                        .spriteDefinition()
                        .bank());
        assertEquals(
                0x44FC,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(slot)
                        .spriteDefinition()
                        .address());
        assertEquals(0, runtime.inspection().swordBeamState(slot));

        tick(runtime, 0, 0x40, 0x40, 3);

        RoomEntity first = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(0x40, first.x());
        assertEquals(0x41, first.y());
        assertEquals(0x40, runtime.inspection().swordBeamSpeedX(slot));
        assertEquals(0x00, runtime.inspection().swordBeamSpeedY(slot));
        assertEquals(1, runtime.inspection().swordBeamState(slot));
        assertEquals(-1, first.spriteVariant());
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x3B));

        tick(runtime, 1, 0x40, 0x40, 3);
        RoomEntity second = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(0x44, second.x());
        assertEquals(0, second.spriteVariant());

        RoomEntity beforeTrail = runtime.inspection().snapshot().slots().get(slot);
        tick(runtime, 3, 0x40, 0x40, 3);
        assertEquals(
                List.of(TransientVfxType.SWORD_BEAM),
                runtime.enemies().transientVfxRequests().stream()
                        .map(TransientVfxRequest::type)
                        .toList());
        TransientVfxRequest request = runtime.enemies().transientVfxRequests().getFirst();
        assertEquals(0, request.variant());
        assertEquals(beforeTrail.x(), request.worldX());
        assertEquals((beforeTrail.y() - beforeTrail.z()) & 0xFF, request.worldY());
    }

    @Test
    void swordBeamStopsOnTheCommonObjectIntersection() throws IOException {
        RoomEntityRuntime runtime = runtime();
        int slot = runtime.spawnSwordBeam(0x40, 0x40, 0, 0);
        runtime.configuration()
                .setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0x01, 0x01, 0x40, 0x40));

        tick(runtime, 0, 0x40, 0x40, 0);
        tick(runtime, 1, 0x40, 0x40, 0);

        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(0, runtime.inspection().activeProjectileCount());
    }

    @Test
    void swordBeamSpawnIsBlockedByAnyExistingPlayerProjectile() throws IOException {
        RoomEntityRuntime runtime = runtime();

        int first = runtime.spawnSwordBeam(0x40, 0x40, 0, 0);

        assertTrue(first >= 0);
        assertEquals(-1, runtime.spawnSwordBeam(0x40, 0x40, 0, 0));
        assertEquals(1, runtime.inspection().activeProjectileCount());
    }

    @Test
    void swordBeamStopsAtOppositeDirectionalBarrierAfterItsCrossingCounterExpires() throws IOException {
        RoomEntityRuntime runtime = runtime();
        int slot = runtime.spawnSwordBeam(0x40, 0x40, 0, 0);
        runtime.configuration().setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(1, 0xD1, 0x40, 0x40));
        tick(runtime, 0, 0x40, 0x40, 0);
        tick(runtime, 1, 0x40, 0x40, 0);
        assertTrue(runtime.inspection().snapshot().slots().get(slot).loaded());
        tick(runtime, 2, 0x40, 0x40, 0);
        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
    }

    @ParameterizedTest
    @CsvSource({"0", "1", "2", "3"})
    void blockedBeamEmitsItsPokeAtThePreMovementVisualPosition(int direction) throws IOException {
        RoomEntityRuntime runtime = runtime();
        int slot = runtime.spawnSwordBeam(0x40, 0x50, 7, direction);
        tick(runtime, 0, 0x40, 0x50, direction);
        RoomEntity before = runtime.inspection().snapshot().slots().get(slot);
        runtime.configuration().setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(1, 1, 0x40, 0x40));
        tick(runtime, 3, 0x40, 0x50, direction);

        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(1, runtime.enemies().transientVfxRequests().size());
        TransientVfxRequest impact = runtime.enemies().transientVfxRequests().getFirst();
        assertEquals(TransientVfxType.SWORD_POKE, impact.type());
        assertEquals(before.x(), impact.worldX());
        assertEquals((before.y() - before.z() + 3) & 0xFF, impact.worldY());
    }

    @Test
    void physicsFFRemovesTheBeamWithoutTheOrdinaryImpactSpark() throws IOException {
        RoomEntityRuntime runtime = runtime();
        int slot = runtime.spawnSwordBeam(0x40, 0x50, 0, 0);
        tick(runtime, 0, 0x40, 0x50, 0);
        runtime.configuration().setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(1, 0xFF, 0x40, 0x40));
        tick(runtime, 3, 0x40, 0x50, 0);
        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertTrue(runtime.enemies().transientVfxRequests().isEmpty());
    }

    private static RoomEntityRuntime runtime() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        return RoomEntityRuntime.from(
                emptySnapshot(), false, () -> 0, catalog, new RomEnemyCombatTables(rom), new ChestContentsTable(rom));
    }

    private static void tick(RoomEntityRuntime runtime, int frame, int linkX, int linkY, int linkDirection) {
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                frame,
                linkX,
                linkY,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(linkX, linkY, 0, 0, linkDirection, false)));
    }

    private static RoomEntitySnapshot emptySnapshot() {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
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
