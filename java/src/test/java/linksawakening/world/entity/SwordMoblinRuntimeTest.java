package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshotWithSlots;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class SwordMoblinRuntimeTest {

    @Test
    void moblinSwordUsesTheBankSevenIdleAlertAndSlowWalkingStates() {
        EntitySpriteDefinition definition = pairDefinition(0x14, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x14)
                .at(0x50, 64)
                .spriteDefinition(definition)
                .spriteVariant(6)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0xC0, 64, () -> 0));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).moblinSwordState(0));
        assertEquals(0x80, new EntityEnemyInspectionQueries(runtime.enemies()).moblinSwordTransitionCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).moblinSwordDirection(0));
        assertEquals(0x06, new EntityEnemyInspectionQueries(runtime.enemies()).moblinSwordSpeedX(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0xC0, 64, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0xC0, 64, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 3, 0xC0, 64, () -> 0));
        assertEquals(0x51, runtime.inspection().snapshot().slots().get(0).x());

        runtime.tick(EntityTestFrames.frame(runtime, 4, 0x70, 64, () -> 0));
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).moblinSwordState(0));
        assertEquals(0x80, new EntityEnemyInspectionQueries(runtime.enemies()).moblinSwordTransitionCountdown(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).moblinSwordAttackCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).moblinSwordDirection(0));
    }

    @Test
    void swordMoblinPausesMotionAndTimersDuringDialogue() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x14)
                        .at(0x50, 64)
                        .spriteDefinition(pairDefinition(0x14, 8))
                        .spriteVariant(6)
                        .build()))
                .build();
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0xC0, 64, () -> 0));
        runtime.configuration().setDialogActive(true);
        for (int frame = 1; frame <= 180; frame++)
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0xC0, 64, () -> 0));
        assertEquals(0x50, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x80, new EntityEnemyInspectionQueries(runtime.enemies()).moblinSwordTransitionCountdown(0));
        runtime.configuration().setDialogActive(false);
        for (int frame = 181; frame <= 184; frame++)
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0xC0, 64, () -> 0));
        assertTrue(runtime.inspection().snapshot().slots().get(0).x() > 0x50);
    }

    @Test
    void moblinSwordUsesTheRomHealthContactAndSwordDamageFamily() {
        EntitySpriteDefinition definition = pairDefinition(0x14, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x14)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, firstHit.size());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
    }

    @Test
    void projectileWallImpactAlertsMoblinSwordSlotsProcessedLaterInTheFrame() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshotWithSlots(
                        new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x14)
                                .at(0x50, 64)
                                .spriteDefinition(pairDefinition(0x14, 8))
                                .spriteVariant(6)
                                .build(),
                        new RoomEntityBuilder()
                                .slot(1)
                                .sourceLoadOrder(1)
                                .type(0x0C)
                                .at(0x20, 64)
                                .spriteDefinition(pairDefinition(0x0C, 4))
                                .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(
                runtime,
                0,
                0xC0,
                64,
                () -> 0,
                (entity, direction, nextX, nextY) -> entity.type() == 0x0C && direction == 0));

        assertEquals(0x04, new EntityEnemyInspectionQueries(runtime.enemies()).swordMoblinAlertingSoundCounter());
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).moblinSwordState(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).moblinSwordAttackCountdown(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0xC0, 64, () -> 0, (entity, direction, nextX, nextY) -> false));
        assertEquals(0x03, new EntityEnemyInspectionQueries(runtime.enemies()).swordMoblinAlertingSoundCounter());
    }

    @Test
    void hideoutMoblinSwordPublishesDialog190OnlyOnce() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x14)
                        .at(0x50, 0x40)
                        .spriteDefinition(pairDefinition(0x14, 8))
                        .spriteVariant(6)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x15);
        EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(0x04);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0xC0, 0x40, () -> 0));
        assertEquals(List.of(new DialogRequest(1, 0x90)), runtime.outbox().consumePendingDialogRequests());
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).moblinSwordHideoutDialogShown(0));

        runtime.tick(EntityTestFrames.frame(runtime, 3, 0x70, 0x40, () -> 0));
        assertTrue(runtime.outbox().consumePendingDialogRequests().isEmpty());
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).moblinSwordHideoutDialogShown(0));
    }

    @Test
    void moblinSwordHitConfiguresRomRecoilAndEnemyHitJingle() {
        EntitySpriteDefinition definition = pairDefinition(0x0B, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        EntityCombatEvent event = events.get(0);
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, event.soundChannel());
        assertEquals(0x03, event.soundId());
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedX(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));

        RoomEntity afterRecoil = runtime.inspection().snapshot().slots().get(0);
        assertEquals(61, afterRecoil.x(), "x=" + afterRecoil.x());
        assertEquals(61, afterRecoil.y(), "y=" + afterRecoil.y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }
}
