package linksawakening.world;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.vfx.TransientVfxType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SwordBeamRuntimeTest {

    @Test
    void spawnRunsTheRomInitialStateThenMovesAndEmitsBeamVfx() throws IOException {
        RoomEntityRuntime runtime = runtime();
        int slot = runtime.spawnSwordBeam(0x40, 0x40, 0, 0);

        assertEquals(0x0F, slot);
        assertEquals(1, runtime.activeProjectileCount());
        assertEquals(0x19, runtime.snapshot().slots().get(slot).spriteDefinition().bank());
        assertEquals(0x44FC, runtime.snapshot().slots().get(slot).spriteDefinition().address());
        assertEquals(0, runtime.swordBeamState(slot));

        tick(runtime, 0, 0x40, 0x40, 3);

        RoomEntity first = runtime.snapshot().slots().get(slot);
        assertEquals(0x40, first.x());
        assertEquals(0x41, first.y());
        assertEquals(0x40, runtime.swordBeamSpeedX(slot));
        assertEquals(0x00, runtime.swordBeamSpeedY(slot));
        assertEquals(1, runtime.swordBeamState(slot));
        assertEquals(-1, first.spriteVariant());
        assertTrue(runtime.consumePendingEntityEvents().stream()
            .anyMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE
                && event.soundId() == 0x3B));

        tick(runtime, 1, 0x40, 0x40, 3);
        RoomEntity second = runtime.snapshot().slots().get(slot);
        assertEquals(0x44, second.x());
        assertEquals(0, second.spriteVariant());

        tick(runtime, 3, 0x40, 0x40, 3);
        assertEquals(List.of(TransientVfxType.SWORD_BEAM),
            runtime.transientVfxRequests().stream()
                .map(RoomEntityRuntime.TransientVfxRequest::type).toList());
        RoomEntityRuntime.TransientVfxRequest request = runtime.transientVfxRequests().getFirst();
        RoomEntity current = runtime.snapshot().slots().get(slot);
        assertEquals(0, request.variant());
        assertEquals(current.x(), request.worldX());
        assertEquals((current.y() - current.z()) & 0xFF, request.worldY());
    }

    @Test
    void swordBeamStopsOnTheCommonObjectIntersection() throws IOException {
        RoomEntityRuntime runtime = runtime();
        int slot = runtime.spawnSwordBeam(0x40, 0x40, 0, 0);
        runtime.setObjectIntersectionQuery(entity ->
            new RoomEntityObjectSample(0x01, 0x01, 0x40, 0x40));

        tick(runtime, 0, 0x40, 0x40, 0);
        tick(runtime, 1, 0x40, 0x40, 0);

        assertFalse(runtime.snapshot().slots().get(slot).loaded());
        assertEquals(0, runtime.activeProjectileCount());
    }

    @Test
    void swordBeamSpawnIsBlockedByAnyExistingPlayerProjectile() throws IOException {
        RoomEntityRuntime runtime = runtime();

        int first = runtime.spawnSwordBeam(0x40, 0x40, 0, 0);

        assertTrue(first >= 0);
        assertEquals(-1, runtime.spawnSwordBeam(0x40, 0x40, 0, 0));
        assertEquals(1, runtime.activeProjectileCount());
    }

    private static RoomEntityRuntime runtime() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        return RoomEntityRuntime.from(emptySnapshot(), false, () -> 0, catalog,
            new RomEnemyCombatTables(rom), new ChestContentsTable(rom));
    }

    private static void tick(RoomEntityRuntime runtime, int frame, int linkX, int linkY,
                             int linkDirection) {
        runtime.tickWithProjectileEvents(frame, linkX, linkY, () -> 0, null,
            new EnemyProjectileCollision.LinkState(linkX, linkY, 0, 0,
                linkDirection, false));
    }

    private static RoomEntitySnapshot emptySnapshot() {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots);
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = SwordBeamRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
