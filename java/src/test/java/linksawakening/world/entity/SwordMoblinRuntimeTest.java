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
    void bladeClashesWithRaisedShieldAtTheRoom52ContactPose() {
        RoomEntityRuntime runtime = room52BladePose();
        var shield = new EnemyAttackContext(1, false, false, false, false, true, 2, 0);

        var blocked = new EnemyCombatFrame(148, 63, 102, false, 0, false, false, 0, 0, 0, 0, shield);
        assertTrue(runtime.combat().resolveCombat(blocked).isEmpty());

        var contact = new EnemyCombatFrame(148, 63, 102, false, 0, true, false, 0, 0, 0, 0, shield);
        var events = runtime.combat().resolveCombat(contact);
        assertEquals(1, events.size());
        var clash = events.getFirst();
        assertEquals(0, clash.linkDamage());
        assertEquals(0, clash.enemyDamage());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, clash.soundChannel());
        assertEquals(0x07, clash.soundId());
        assertEquals(EntityCombatEvent.LinkAction.SWORD_MOBLIN_BLADE_CLASH, clash.linkAction());
        assertEquals(new EntityCombatEvent.SwordPokeVfx(63, 86), clash.swordPokeVfx());
        assertEquals(new EntityCombatEvent.LinkCollisionResponse(18, 14, 8), clash.linkCollisionResponse());

        runtime.tick(EntityTestFrames.frame(runtime, 148, 63, 102, () -> 0));
        assertEquals(44, runtime.inspection().snapshot().slots().get(1).x());
        assertEquals(87, runtime.inspection().snapshot().slots().get(1).y());
        assertEquals(8, runtime.enemies().enemyIgnoreHitsCountdown(1));
        var inspection = new EntityEnemyInspectionQueries(runtime.enemies());
        assertEquals(0xE8, inspection.enemyRecoilSpeedX(1));
        assertEquals(0xEE, inspection.enemyRecoilSpeedY(1));
    }

    @Test
    void bladeClashesWithAnActiveSwordOrHurtsLinkOnItsBodyContactFrame() {
        RoomEntityRuntime runtime = room52BladePose();
        var attack = new EnemyAttackContext(1, false, false, false, false, false, 2, 0);
        var touchingSword = new EnemyCombatFrame(149, 63, 102, false, 0, true, true, 74, 8, 102, 3, attack);
        assertTrue(runtime.combat().resolveCombat(touchingSword).isEmpty());
        var swordFrame = new EnemyCombatFrame(149, 63, 102, false, 0, true, true, 71, 8, 102, 3, attack);
        var events = runtime.combat().resolveCombat(swordFrame);
        assertEquals(1, events.size());
        assertEquals(
                EntityCombatEvent.LinkAction.SWORD_MOBLIN_BLADE_CLASH,
                events.getFirst().linkAction());
        assertEquals(2, runtime.enemies().enemyHealth(1));

        var bladeOnly = new EnemyCombatFrame(148, 56, 91, false, 0, true, false, 0, 0, 0, 0, attack);
        events = runtime.combat().resolveCombat(bladeOnly);
        assertEquals(1, events.size());
        assertEquals(4, events.getFirst().linkDamage());
        assertEquals(
                new EntityCombatEvent.LinkCollisionResponse(20, 6, 16),
                events.getFirst().linkCollisionResponse());
        assertTrue(runtime.combat()
                .resolveCombat(new EnemyCombatFrame(149, 56, 91, false, 0, true, false, 0, 0, 0, 0, attack))
                .isEmpty());
    }

    @Test
    void bladeSeesTheSwordTargetLatchedByAnEarlierEnemySlot() {
        var attack = new EnemyAttackContext(1, false, false, false, false, false, 2, 0);
        var activeSword = new EnemyCombatFrame(136, 67, 120, false, 0, true, true, 75, 8, 120, 3, attack);
        RoomEntity blade = room52OrderingMoblin(0, 0x14, 47, 104, 6);
        RoomEntity ordinary = room52OrderingMoblin(1, 0x0B, 72, 108, 0);

        var onlyBlade = new RoomEntityRuntimeBuilder(snapshotWithSlots(blade)).build();
        assertEquals(
                EntityCombatEvent.LinkAction.SWORD_MOBLIN_BLADE_CLASH,
                onlyBlade.combat().resolveCombat(activeSword).getFirst().linkAction());

        var priorSwingHit = new EnemyCombatFrame(136, 67, 120, false, 0, true, true, 75, 8, 120, 3, attack, 2, true);
        assertTrue(onlyBlade.combat().resolveCombat(priorSwingHit).isEmpty());

        var twoEnemies = new RoomEntityRuntimeBuilder(snapshotWithSlots(blade, ordinary)).build();
        var events = twoEnemies.combat().resolveCombat(activeSword);
        assertEquals(1, events.size());
        assertEquals(1, events.getFirst().slot());
        assertEquals(
                EntityCombatEvent.LinkAction.RECORD_SWORD_HIT_TARGET,
                events.getFirst().linkAction());
        assertEquals(1, twoEnemies.enemies().enemyHealth(1));
    }

    @Test
    void earlierBodyHitSuppressesBladeClashOnlyWhenLinkAcceptsDamage() {
        var attack = new EnemyAttackContext(1, false, false, false, false, false, 2, 0);
        RoomEntity blade = room52OrderingMoblin(0, 0x14, 47, 104, 6);
        RoomEntity body = room52OrderingMoblin(1, 0x0B, 65, 123, 0);
        var runtime = new RoomEntityRuntimeBuilder(snapshotWithSlots(blade, body)).build();
        var activeSword = new EnemyCombatFrame(136, 67, 120, false, 0, true, true, 75, 8, 120, 3, attack);
        var events = runtime.combat().resolveCombat(activeSword);
        assertEquals(1, events.size());
        assertEquals(4, events.getFirst().linkDamage());
        assertEquals(
                new EntityCombatEvent.LinkCollisionResponse(13, 236, 16),
                events.getFirst().linkCollisionResponse());

        var invincible = new EnemyCombatFrame(136, 67, 120, false, 0, true, true, 75, 8, 120, 3, attack, 0, false);
        events = runtime.combat().resolveCombat(invincible);
        assertEquals(
                EntityCombatEvent.LinkAction.SWORD_MOBLIN_BLADE_CLASH,
                events.getLast().linkAction());
        assertEquals(
                new EntityCombatEvent.LinkCollisionResponse(18, 14, 8),
                events.getLast().linkCollisionResponse());
    }

    private static RoomEntity room52OrderingMoblin(int slot, int type, int x, int y, int variant) {
        return new RoomEntityBuilder()
                .slot(slot)
                .sourceLoadOrder(slot)
                .type(type)
                .at(x, y)
                .spriteDefinition(pairDefinition(type, 8))
                .spriteVariant(variant)
                .build();
    }

    private static RoomEntityRuntime room52BladePose() {
        RoomEntity moblin = new RoomEntityBuilder()
                .slot(1)
                .sourceLoadOrder(0)
                .type(0x14)
                .at(44, 87)
                .spriteDefinition(pairDefinition(0x14, 8))
                .spriteVariant(7)
                .build();
        return new RoomEntityRuntimeBuilder(snapshotWithSlots(moblin)).build();
    }

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
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
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
