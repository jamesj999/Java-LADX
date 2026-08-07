package linksawakening.world;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MagicRodRuntimeTest {

    @Test
    void spawnUsesTheRomFireballDisplayAndMovesOnTheSharedProjectileTables()
            throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x40, 0, 0);

        assertEquals(0x0F, slot);
        assertEquals(1, runtime.activeProjectileCount());
        assertEquals(0x03, runtime.snapshot().slots().get(slot).spriteDefinition().bank());
        assertEquals(0x69AA, runtime.snapshot().slots().get(slot).spriteDefinition().address());
        assertEquals(0, runtime.magicRodFireballPrivateCountdown1(slot));

        tick(runtime, 0, 0x20, 0x20);
        RoomEntity first = runtime.snapshot().slots().get(slot);
        assertEquals(0x42, first.x());
        assertEquals(0, first.spriteVariant());

        tick(runtime, 8, 0x20, 0x20);
        assertEquals(0x44, runtime.snapshot().slots().get(slot).x());
        assertEquals(1, runtime.snapshot().slots().get(slot).spriteVariant());
    }

    @Test
    void solidObjectStartsTheSharedFireDisplayAndUnloadsAtPrivateCountdownZero()
            throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicRodFireball(0x40, 0x40, 0, 0);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            0x01, 0x01, 0x40, 0x40));

        tick(runtime, 0, 0x20, 0x20);

        assertEquals(MagicRodFireballMotion.FIRE_TRANSITION_COUNTDOWN,
            runtime.magicRodFireballPrivateCountdown1(slot));
        assertEquals(0x69AA, runtime.snapshot().slots().get(slot).spriteDefinition().address());
        assertEquals(0, runtime.snapshot().slots().get(slot).spriteVariant());

        tick(runtime, 1, 0x20, 0x20);
        assertEquals(0x2F, runtime.magicRodFireballPrivateCountdown1(slot));
        assertEquals(0x4C44, runtime.snapshot().slots().get(slot).spriteDefinition().address());
        assertEquals(0, runtime.snapshot().slots().get(slot).spriteVariant());

        for (int frame = 9; frame < 0x38 && runtime.snapshot().slots().get(slot).loaded(); frame++) {
            tick(runtime, frame, 0x20, 0x20);
        }
        assertFalse(runtime.snapshot().slots().get(slot).loaded());
        assertEquals(0, runtime.activeProjectileCount());
    }

    @Test
    void outdoorBushRequestsRomObjectRevealSmokeAndDestroyNoise() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicRodFireball(0x20, 0x40, 0, 0);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            0x5C, 0x01, 0x30, 0x40));

        tick(runtime, 0, 0x20, 0x20);

        assertEquals(1, runtime.magicRodObjectRequests().size());
        RoomEntityRuntime.MagicRodObjectRequest request =
            runtime.magicRodObjectRequests().getFirst();
        assertEquals(slot, request.sourceSlot());
        assertEquals(0x43, request.location());
        assertEquals(0x30, request.objectLeft());
        assertEquals(0x40, request.objectTop());
    }

    private static RoomEntityRuntime runtime(boolean indoor) throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        return RoomEntityRuntime.from(emptySnapshot(), indoor, () -> 0, catalog,
            new RomEnemyCombatTables(rom), new ChestContentsTable(rom));
    }

    private static void tick(RoomEntityRuntime runtime, int frame, int linkX, int linkY) {
        runtime.tickWithProjectileEvents(frame, linkX, linkY, () -> 0, null,
            new EnemyProjectileCollision.LinkState(linkX, linkY, 0, 0, 0, false));
    }

    private static RoomEntitySnapshot emptySnapshot() {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots);
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = MagicRodRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
