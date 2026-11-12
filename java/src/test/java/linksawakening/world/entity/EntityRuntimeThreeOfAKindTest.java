package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeThreeOfAKindTest extends EntityRuntimeTestSupport {

    @Test
    void matchingThreeOfAKindCardsEnterTheSourceDeathSequenceTogether() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(threeOfAKind(0, 0x28, 0x28), threeOfAKind(1, 0x78, 0x28), threeOfAKind(2, 0x48, 0x38)))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        for (int slot = 0; slot < 3; slot++) {
            EntityRuntimeFixture.debug(runtime).setThreeOfAKindSettledForTest(slot, 0);
        }

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime, 1, 0, 0, () -> 0, null, new EnemyProjectileCollision.LinkState(0, 0, 0, 0, 3, false)));

        assertTrue(runtime.inspection().snapshot().loadedEntities().stream()
                .allMatch(card -> card.status() == EntityStatus.DYING));
        List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
        assertTrue(events.stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x02));
        assertEquals(
                1,
                events.stream()
                        .filter(event ->
                                event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x13)
                        .count());
    }

    @Test
    void swordHitFeedsThreeOfAKindsGenericIgnoreHitLatch() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(threeOfAKind(0, 0x40, 0x50)))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime, 1, 0, 0, () -> 0, null, new EnemyProjectileCollision.LinkState(0, 0, 0, 0, 3, false)));

        List<EntityCombatEvent> hit = runtime.resolveCombat(1, 0, 0, false, true, true, 0x40, 0x10, 0x50, 0x10);
        assertTrue(hit.getFirst().swordHit());

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime, 2, 0, 0, () -> 0, null, new EnemyProjectileCollision.LinkState(0, 0, 0, 0, 3, false)));
        assertEquals(2, EntityRuntimeFixture.inspection(runtime).threeOfAKindStateForTest(0));
    }

    @Test
    void settledThreeOfAKindDoesNotRunTheGenericCombatHandler() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(threeOfAKind(0, 0x40, 0x50)))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        EntityRuntimeFixture.debug(runtime).setThreeOfAKindSettledForTest(0, 0);

        assertTrue(runtime.resolveCombat(1, 0, 0, false, true, true, 0x40, 0x10, 0x50, 0x10)
                .isEmpty());
    }

    @Test
    void noninteractiveFramesFreezeThreeOfAKindCountdownAndPuzzleCheck() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(threeOfAKind(0, 0x28, 0x28), threeOfAKind(1, 0x78, 0x28), threeOfAKind(2, 0x48, 0x38)))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        for (int slot = 0; slot < 3; slot++) {
            EntityRuntimeFixture.debug(runtime).setThreeOfAKindSettledForTest(slot, 0);
            EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(slot, 1);
        }

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime, 1, 0, 0, () -> 0, null, new EnemyProjectileCollision.LinkState(0, 0, 0, 7, 3, false)));

        assertEquals(1, EntityRuntimeFixture.inspection(runtime).transitionCountdownForTest(0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        assertTrue(runtime.inspection().snapshot().loadedEntities().stream()
                .allMatch(card -> card.status() == EntityStatus.ACTIVE));

        runtime.configuration().setDialogActive(true);
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime, 2, 0, 0, () -> 0, null, new EnemyProjectileCollision.LinkState(0, 0, 0, 0, 3, false)));
        assertEquals(1, EntityRuntimeFixture.inspection(runtime).transitionCountdownForTest(0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }
}
