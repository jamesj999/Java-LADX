package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeMoblinKingTest extends EntityRuntimeTestSupport {

    @Test
    void kidnappedBowWowEnablesTheMoblinKingSourceIntro() {
        RoomEntity king = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xE4)
                .at(0x58, 0x58)
                .status(EntityStatus.INIT)
                .spriteDefinition(pairDefinition(0xE4, 14))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(king))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setBowWowState(0x80);

        boolean openedIntro = false;
        for (int frame = 0; frame < 36; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x30, 0x58, () -> 0));
            openedIntro |= runtime.consumePendingDialogRequests().equals(List.of(new DialogRequest(1, 0x91, true)));
        }

        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(2, runtime.inspection().moblinKingState(0));
        assertTrue(openedIntro);
    }

    @Test
    void moblinKingStateMachineFreezesWhileDialogIsActive() {
        RoomEntity king = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xE4)
                .at(0x58, 0x58)
                .spriteDefinition(pairDefinition(0xE4, 14))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(king))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setBowWowState(0x80);
        EntityRuntimeFixture.debug(runtime).setMoblinKingStateForTest(0, 3, 0, 0, 0, 0, 0x20);
        EntityRuntimeFixture.debug(runtime).setSlowTransitionCountdownForTest(0, 0x30);
        runtime.configuration().setDialogActive(true);

        for (int frame = 0; frame < 300; frame++)
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x30, 0x58, () -> 0));

        assertEquals(3, runtime.inspection().moblinKingState(0));
        assertEquals(0x20, EntityRuntimeFixture.inspection(runtime).transitionCountdownForTest(0));
        assertEquals(0x30, EntityRuntimeFixture.inspection(runtime).slowTransitionCountdownForTest(0));
    }

    @Test
    void moblinKingWaitsForRoomTransitionBeforeStartingCharge() {
        RoomEntity king = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xE4)
                .at(0x58, 0x58)
                .spriteDefinition(pairDefinition(0xE4, 14))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(king))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setBowWowState(0x80);
        EntityRuntimeFixture.debug(runtime).setMoblinKingStateForTest(0, 3, 0, 0, 0, 0, 0);
        EntityRuntimeFixture.debug(runtime).setRoomTransitionStateForTest(true);
        for (int frame = 0; frame < 60; frame++)
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x30, 0x58, () -> 0));
        assertEquals(3, runtime.inspection().moblinKingState(0));
        assertEquals(0x58, runtime.inspection().snapshot().slots().get(0).x());
        EntityRuntimeFixture.debug(runtime).setRoomTransitionStateForTest(false);
        runtime.tick(EntityTestFrames.frame(runtime, 60, 0x30, 0x58, () -> 0));
        assertEquals(4, runtime.inspection().moblinKingState(0));
    }

    @Test
    void moblinKingUnloadsUnlessBowWowIsKidnapped() {
        RoomEntity king = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xE4)
                .at(0x58, 0x58)
                .spriteDefinition(pairDefinition(0xE4, 14))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(king))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x30, 0x58, () -> 0));

        assertFalse(runtime.inspection().snapshot().slots().get(0).loaded());
    }

    @Test
    void moblinKingOnlyTakesSwordDamageDuringTheWallStunState() {
        RoomEntity king = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xE4)
                .at(0x58, 0x58)
                .spriteDefinition(pairDefinition(0xE4, 14))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(king))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setBowWowState(0x80);
        EntityRuntimeFixture.debug(runtime).setMoblinKingStateForTest(0, 2, 0, 0, 0, 0, 0x30);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x30, 0x58, () -> 0));

        runtime.resolveCombat(1, 0x30, 0x58, false, true, true, 0x58, 8, 0x58, 8);
        assertEquals(0x08, runtime.enemies().enemyHealth(0));

        EntityRuntimeFixture.debug(runtime).setMoblinKingStateForTest(0, 5, 0, 0, 0, 0, 0x60);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x30, 0x58, () -> 0));
        runtime.resolveCombat(3, 0x30, 0x58, false, true, true, 0x58, 8, 0x58, 8);

        assertEquals(0x07, runtime.enemies().enemyHealth(0));
        int beforeX = runtime.inspection().snapshot().slots().get(0).x();
        int beforeZ = runtime.inspection().snapshot().slots().get(0).z();
        runtime.tick(EntityTestFrames.frame(runtime, 4, 0x30, 0x58, () -> 0));
        assertTrue(runtime.inspection().snapshot().slots().get(0).x() > beforeX);
        assertEquals(beforeZ, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(5, runtime.inspection().moblinKingState(0));
    }

    @Test
    void moblinKingChargeImpactUsesItsDedicatedLinkDamageAndImmunityWrites() {
        RoomEntity king = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xE4)
                .at(0x58, 0x58)
                .spriteDefinition(pairDefinition(0xE4, 14))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(king))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setBowWowState(0x80);
        EntityRuntimeFixture.debug(runtime).setMoblinKingStateForTest(0, 4, 0, 0x28, 0, 0, 0x22);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x58, 0x58, () -> 0, true));
        assertEquals(4, runtime.inspection().moblinKingState(0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x58, 0x58, () -> 0, true));
        List<EntityCombatEvent> events = runtime.resolveCombat(3, 0x58, 0x58, false, true, false, 0, 0, 0, 0);

        EntityCombatEvent impact = events.getFirst();
        assertEquals(0x08, impact.linkDamage());
        assertEquals(0x28, impact.linkCollisionResponse().speedX());
        assertEquals(0, impact.linkCollisionResponse().speedY());
        assertEquals(0x28, impact.linkCollisionResponse().ignoreCollisionCountdown());
        assertEquals(0x40, impact.linkCollisionResponse().invincibilityCountdown());
    }

    @Test
    void stunnedMoblinKingIsHarmlessToLinkWhileVulnerable() {
        RoomEntity king = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xE4)
                .at(0x58, 0x58)
                .spriteDefinition(pairDefinition(0xE4, 14))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(king))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setBowWowState(0x80);
        EntityRuntimeFixture.debug(runtime).setMoblinKingStateForTest(0, 5, 0, 0, 0, 0, 0x60);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x58, 0x58, () -> 0));
        List<EntityCombatEvent> events = runtime.resolveCombat(1, 0x58, 0x58, false, true, false, 0, 0, 0, 0);

        assertEquals(0, events.getFirst().linkDamage());
    }

    @Test
    void defeatingMoblinKingPersistsTheMinibossRoomEventThatOpensTheRescuePath() {
        RoomEntity king = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xE4)
                .at(0x58, 0x58)
                .spriteDefinition(pairDefinition(0xE4, 14))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(king))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setBowWowState(0x80);
        EntityRuntimeFixture.debug(runtime).setEnemyHealthForTest(0, 1);
        EntityRuntimeFixture.debug(runtime).setMoblinKingStateForTest(0, 5, 0, 0, 0, 0, 0x60);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x30, 0x58, () -> 0));

        runtime.resolveCombat(1, 0x30, 0x58, false, true, true, 0x58, 8, 0x58, 8);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x30, 0x58, () -> 0));
        assertEquals(0xA0, EntityRuntimeFixture.inspection(runtime).transitionCountdownForTest(0));
        for (int frame = 3; frame <= 162; frame++)
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x30, 0x58, () -> 0));
        assertEquals(0xC0, EntityRuntimeFixture.inspection(runtime).transitionCountdownForTest(0));
        assertTrue(runtime.inspection().snapshot().slots().get(0).loaded());
        assertEquals(0, runtime.consumePendingRoomStatusMask());
        for (int frame = 163; frame < 354; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x30, 0x58, () -> 0));
        }
        assertTrue(runtime.inspection().snapshot().slots().get(0).loaded());
        runtime.tick(EntityTestFrames.frame(runtime, 354, 0x30, 0x58, () -> 0));
        assertFalse(runtime.inspection().snapshot().slots().get(0).loaded());
        assertEquals(0x20, runtime.consumePendingRoomStatusMask());
    }
}
