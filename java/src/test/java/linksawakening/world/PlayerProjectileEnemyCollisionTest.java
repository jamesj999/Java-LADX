package linksawakening.world;

import java.io.IOException;
import java.util.ArrayList;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.vfx.TransientVfxType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlayerProjectileEnemyCollisionTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 0xDF})
    void blockingHitboxTakesPrecedenceOverTheIgnoreHitsTimer(int type) throws IOException {
        RoomEntityRuntime runtime = runtime(type, 9, 0, 0);
        int slot = spawn(runtime, type, 0);
        runtime.setHitboxFlagsForTest(0, 0x80);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 8);
        int health = runtime.enemyHealth(0);
        runtime.tick(2, 0, 0, () -> 0);

        assertEquals(health, runtime.enemyHealth(0));
        assertFalse(hitTarget(runtime));
        assertBlocked(runtime, type, slot);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 0xDF})
    void ignoreHitsTimerAloneLetsTheProjectilePass(int type) throws IOException {
        RoomEntityRuntime runtime = runtime(type, 9, 0, 0);
        int slot = spawn(runtime, type, 0);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 8);
        int beforeX = runtime.snapshot().slots().get(slot).x();
        runtime.tick(2, 0, 0, () -> 0);

        assertTrue(runtime.snapshot().slots().get(slot).loaded());
        assertEquals(beforeX + (type == 0 ? 3 : type == 1 ? 2 : 4),
            runtime.snapshot().slots().get(slot).x());
        assertFalse(hitTarget(runtime));
        if (type == 1) {
            assertEquals(0, runtime.boomerangState(slot));
        }
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1, 1", "0xDF, 1", "0, 0", "1, 0", "0xDF, 0"})
    void attachedIronMaskBlocksOnlyTheFront(int type, int direction) throws IOException {
        RoomEntityRuntime runtime = runtime(type, 0x24, 0, 0);
        int slot = spawn(runtime, type, direction);
        assertEquals(0, runtime.ironMaskDirection(0));
        int health = runtime.enemyHealth(0);
        runtime.tick(2, 0, 0, () -> 0);

        if (direction == 1) {
            assertEquals(health, runtime.enemyHealth(0));
            assertFalse(hitTarget(runtime));
            assertBlocked(runtime, type, slot);
        } else {
            assertTrue(hitTarget(runtime));
        }
    }

    @ParameterizedTest
    @CsvSource({"0, 12, 0, true", "0, -12, 0, false", "0, 0, -12, true", "0, 0, 12, false",
        "1, 12, 0, true", "1, -12, 0, false", "1, 0, -12, true", "1, 0, 12, false",
        "0xDF, 12, 0, true", "0xDF, -12, 0, false", "0xDF, 0, -12, true", "0xDF, 0, 12, false"})
    void collisionEdgesFollowTheUnsignedRomComparisons(int type, int dx, int dy,
                                                       boolean hit) throws IOException {
        RoomEntityRuntime runtime = runtime(type, 9, dx, dy);
        spawn(runtime, type, 0);
        runtime.tick(2, 0, 0, () -> 0);
        assertEquals(hit, hitTarget(runtime));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 0xDF})
    void grabbableTargetsBypassDamageAndHitboxImmunity(int type) throws IOException {
        RoomEntityRuntime runtime = runtime(type, 9, 0, 0);
        int slot = spawn(runtime, type, 0);
        runtime.setPhysicsFlagsForTest(0, 0x20);
        runtime.setHitboxFlagsForTest(0, 0x80);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 8);
        runtime.tick(2, 0, 0, () -> 0);
        assertFalse(hitTarget(runtime));
        assertTrue(runtime.snapshot().slots().get(slot).loaded());
        if (type == 1) {
            assertEquals(1, runtime.boomerangState(slot));
        } else if (type == 0) {
            assertEquals(0, runtime.playerArrowTransitionCountdown(slot));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void arrowBlockIsRetainedAcrossPausedHandlersAndClearedOnSlotReuse(boolean paused)
            throws IOException {
        RoomEntityRuntime runtime = runtime(0, 9, 0, 0);
        int slot = spawn(runtime, 0, 0);
        runtime.setHitboxFlagsForTest(0, 0x80);
        runtime.setDialogActive(paused);
        runtime.tick(2, 0, 0, () -> 0);
        if (paused) {
            assertEquals(0, runtime.playerArrowTransitionCountdown(slot));
            runtime.clearEntity(0);
            runtime.setDialogActive(false);
            runtime.tick(3, 0, 0, () -> 0);
        }
        assertEquals(0x18, runtime.playerArrowTransitionCountdown(slot));
        runtime.clearEntity(0);
        runtime.clearEntity(slot);
        assertEquals(slot, runtime.spawnArrow(0x40, 0x50, 0, 0));
        runtime.tick(4, 0, 0, () -> 0);
        assertEquals(0, runtime.playerArrowTransitionCountdown(slot));
    }

    @ParameterizedTest
    @CsvSource({"12, 0, true", "-12, 0, false", "0, -12, true", "0, 12, false"})
    void hookshotUnmaskingUsesTheSameCollisionEdges(int dx, int dy, boolean unmasked)
            throws IOException {
        RoomEntityRuntime runtime = runtime(3, 0x24, dx, dy);
        runtime.spawnHookshotChain(0x40, 0x50, 0, 1);
        runtime.tick(2, 0xC0, 0xC0, () -> 0);
        assertEquals(unmasked ? 1 : 0, runtime.ironMaskPrivateState2(0));
    }

    @Test
    void hookshotUnmasksBeforeTestingBackgroundCollision() throws IOException {
        RoomEntityRuntime runtime = runtime(3, 0x24, 0, 0);
        runtime.spawnHookshotChain(0x40, 0x50, 0, 1);
        runtime.tick(2, 0xC0, 0xC0, () -> 0, (entity, direction, x, y) -> {
            if (entity.type() == 3) {
                assertEquals(1, runtime.ironMaskPrivateState2(0));
                return true;
            }
            return false;
        });
        assertEquals(1, runtime.ironMaskPrivateState2(0));
    }

    @Test
    void returningHookshotSkipsEnemyAndBackgroundInteractions() throws IOException {
        RoomEntityRuntime runtime = runtime(3, 0x24, 0, 0);
        int physics = runtime.physicsFlags(0);
        runtime.setPhysicsFlagsForTest(0, 0x40);
        int slot = runtime.spawnHookshotChain(0x40, 0x50, 0, 1);
        runtime.tick(0, 0xC0, 0xC0, () -> 0,
            (entity, direction, x, y) -> entity.type() == 3);
        runtime.tick(1, 0xC0, 0xC0, () -> 0);
        assertEquals(0, runtime.hookshotTransitionCountdown(slot));
        runtime.setPhysicsFlagsForTest(0, physics);
        runtime.tick(2, 0xC0, 0xC0, () -> 0, (entity, direction, x, y) -> {
            assertFalse(entity.type() == 3, "Returning hookshot probed the background");
            return false;
        });
        assertEquals(0, runtime.ironMaskPrivateState2(0));
    }

    private static boolean hitTarget(RoomEntityRuntime runtime) {
        return runtime.consumePendingEntityEvents().stream().anyMatch(event -> event.slot() == 0);
    }

    private static void assertBlocked(RoomEntityRuntime runtime, int type, int slot) {
        if (type == 0) {
            assertTrue(runtime.snapshot().slots().get(slot).loaded());
            assertEquals(0x18, runtime.playerArrowTransitionCountdown(slot));
        } else if (type == 1) {
            assertTrue(runtime.snapshot().slots().get(slot).loaded());
            assertEquals(1, runtime.boomerangState(slot));
            assertEquals(0, runtime.boomerangTransitionCountdown(slot));
        } else {
            assertFalse(runtime.snapshot().slots().get(slot).loaded());
            assertEquals(TransientVfxType.SWORD_POKE,
                runtime.transientVfxRequests().getFirst().type());
        }
    }

    private static int spawn(RoomEntityRuntime runtime, int type, int direction) {
        int slot = switch (type) {
            case 0 -> runtime.spawnArrow(0x40, 0x50, 0, direction);
            case 1 -> runtime.spawnBoomerang(0x40, 0x50, 0, direction, 0);
            case 0xDF -> runtime.spawnSwordBeam(0x40, 0x50, 0, direction);
            default -> throw new IllegalArgumentException();
        };
        if (type == 0xDF) {
            runtime.tickWithProjectileEvents(1, 0x40, 0x50, () -> 0, null,
                new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0, 3, false));
            runtime.consumePendingEntityEvents();
        }
        return slot;
    }

    private static RoomEntityRuntime runtime(int projectileType, int targetType, int dx, int dy)
            throws IOException {
        byte[] rom;
        try (var stream = PlayerProjectileEnemyCollisionTest.class.getResourceAsStream("/rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM");
            }
            rom = stream.readAllBytes();
        }
        var catalog = new EntitySpriteHandlerCatalog(rom);
        var slots = new ArrayList<RoomEntity>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        slots.set(0, new RoomEntity(0, -1, targetType, 0x40 + dx,
            (projectileType == 0xDF ? 0x50 : 0x4F) + dy, EntityStatus.ACTIVE,
            catalog.forEntityType(targetType, EntityRoomLoader.RoomTable.OVERWORLD, -1), 0));
        return RoomEntityRuntime.from(new RoomEntitySnapshot(slots), false, () -> 0,
            catalog, new RomEnemyCombatTables(rom), new ChestContentsTable(rom));
    }
}
