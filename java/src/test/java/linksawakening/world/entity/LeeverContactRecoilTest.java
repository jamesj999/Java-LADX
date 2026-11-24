package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class LeeverContactRecoilTest {
    @Test
    void acceptedContactKeepsCachedHitboxButUsesMovedPositionForRecoil() {
        var runtime = chasingLeever(110, 40);
        var contact = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(35, 109, 39, false, true, false, 0, 0, 0, 0))
                .getFirst();
        assertEquals(4, contact.linkDamage());
        assertTrue(runtime.combat().deferContactRecoil(contact.slot(), 109, 39));

        runtime.tick(EntityTestFrames.frame(runtime, 35, 109, 39, () -> 0));

        var response = runtime.outbox().consumePendingEntityEvents().getFirst();
        assertEquals(109, runtime.inspection().snapshot().slots().get(2).x());
        assertResponse(response, 0, 0xEC);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 8})
    void simultaneousBodyAndSwordUsePostMovementVectorsEvenOnALethalHit(int initialHealth) {
        var runtime = chasingLeever(110, 40);
        runtime.inspection().combatTables().writeEnemyHealth(2, initialHealth);
        var contact = runtime
                .combat()
                .resolveCombat(new EntitySwordContactFrame(35, 109, 39, false, true, true, 118, 5, 48, 5))
                .stream()
                .filter(EntityCombatEvent::swordHit)
                .findFirst()
                .orElseThrow();
        assertEquals(4, contact.linkDamage());
        assertEquals(16, contact.linkCollisionResponse().ignoreCollisionCountdown());
        assertEquals(initialHealth - 1, runtime.enemies().enemyHealth(2));
        assertTrue(runtime.combat().deferContactRecoil(2, 109, 39));
        runtime.tick(EntityTestFrames.frame(runtime, 35, 109, 39, () -> 0));
        var entity = runtime.inspection().slots().get(2);
        assertEquals(109, entity.x());
        assertEquals(initialHealth == 1 ? EntityStatus.DYING : EntityStatus.ACTIVE, entity.status());
        assertResponse(runtime.outbox().consumePendingEntityEvents().getFirst(), 0, 236);
        var recoil = runtime.inspection().motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        assertEquals(0, recoil.recoilSpeedX(2));
        assertEquals(48, recoil.recoilSpeedY(2));
        assertEquals(10, runtime.enemies().enemyIgnoreHitsCountdown(2));
        assertEquals(24, runtime.inspection().combatTables().readEnemyFlashCountdown(2));
        if (initialHealth == 1) {
            assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(2));
            assertEquals(47, runtime.inspection().combatTables().readDyingCountdown(2));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void lethalSwordRecoilUsesBankThreeOnceLeeverEntersItsDeathHandler(boolean open) {
        var runtime = chasingLeever(110, 40);
        var combat = runtime.inspection().combatTables();
        combat.writeEnemyHealth(2, 1);
        RoomEntityBackgroundCollision background = (entity, direction, x, y) -> !open && direction == 3 && y > 64;

        runtime.combat().resolveCombat(new EntitySwordContactFrame(35, 109, 39, false, true, true, 118, 5, 48, 5));
        runtime.tick(EntityTestFrames.frame(runtime, 35, 109, 39, () -> 0, background));
        assertEquals(EntityStatus.DYING, runtime.inspection().slots().get(2).status());
        assertEquals(47, combat.readDyingCountdown(2));

        int finalStep = open ? 10 : 9;
        for (int step = 1; step <= finalStep; step++) {
            runtime.tick(EntityTestFrames.frame(runtime, 35 + step, 109, 39, () -> 0, background));
            var enemy = runtime.inspection().slots().get(2);
            assertEquals(109, enemy.x());
            assertEquals(open ? 40 + 3 * step : Math.min(40 + 3 * step, 64), enemy.y());
            assertEquals(step == finalStep ? 0 : 10 - step, runtime.enemies().enemyIgnoreHitsCountdown(2));
        }
        runtime.tick(EntityTestFrames.frame(runtime, 36 + finalStep, 109, 39, () -> 0, background));
        assertEquals(31, combat.readDyingCountdown(2));
    }

    @Test
    void beachShieldBumpReproducesBothRomVectorsWhenTheLeeverStaysInPlace() {
        var runtime = chasingLeever(49, 44);
        var shield = new EnemyAttackContext(0, false, false, false, false, true, 0, 0);
        var frame = new EnemyCombatFrame(32, 41, 32, false, 0, true, false, 0, 0, 0, 0, shield);
        var bump = runtime.combat().resolveCombat(frame).getLast();
        assertEquals(0, bump.linkDamage());
        assertEquals(9, bump.soundId());
        assertEquals(new EntityCombatEvent.LinkCollisionResponse(244, 238, 12), bump.linkCollisionResponse());

        // Keep this Leever in place, as at ROM frame 4619.
        runtime.tick(EntityTestFrames.frame(runtime, 32, 41, 32, () -> 0, (entity, direction, x, y) -> true));
        assertEquals(
                bump.linkCollisionResponse(),
                runtime.outbox().consumePendingEntityEvents().getFirst().linkCollisionResponse());
        assertEquals(10, runtime.enemies().enemyIgnoreHitsCountdown(2));
        var recoil = runtime.inspection().motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        assertEquals(21, recoil.recoilSpeedX(2));
        assertEquals(32, recoil.recoilSpeedY(2));
    }

    @Test
    void shieldUsesCachedLeeverHitboxAndProjectsBothRecoilVectorsFromItsNewPosition() {
        var runtime = chasingLeever(110, 40);
        var shield = new EnemyAttackContext(0, false, false, false, false, true, 0, 0);
        var frame = new EnemyCombatFrame(35, 99, 34, false, 0, true, false, 0, 0, 0, 0, shield);
        var bump = runtime.combat().resolveCombat(frame).getLast();
        assertEquals(9, bump.soundId());
        runtime.tick(EntityTestFrames.frame(runtime, 35, 99, 34, () -> 0));
        assertEquals(109, runtime.inspection().slots().get(2).x());
        assertEquals(
                new EntityCombatEvent.LinkCollisionResponse(238, 246, 12),
                runtime.outbox().consumePendingEntityEvents().getFirst().linkCollisionResponse());
        var recoil = runtime.inspection().motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        assertEquals(32, recoil.recoilSpeedX(2));
        assertEquals(19, recoil.recoilSpeedY(2));
    }

    @Test
    void lowerOctorokKeepsLastShieldResponseWhileHigherLeeverStillRecoils() {
        var runtime = chasingLeever(65, 38);
        runtime.inspection()
                .slots()
                .replace(
                        1,
                        new RoomEntityBuilder()
                                .slot(1)
                                .sourceLoadOrder(1)
                                .type(0x09)
                                .at(71, 25)
                                .spriteDefinition(EntityRuntimeTestSupport.pairDefinition(0x09, 8))
                                .build());
        var shield = new EnemyAttackContext(0, false, false, false, false, true, 0, 0);
        var frame = new EnemyCombatFrame(68, 56, 26, false, 0, true, false, 0, 0, 0, 0, shield);
        var bumps = runtime.combat().resolveCombat(frame);
        assertEquals(2, bumps.size());
        assertEquals(2, bumps.getFirst().slot());
        assertEquals(1, bumps.getLast().slot());
        assertEquals(
                new EntityCombatEvent.LinkCollisionResponse(238, 1, 12),
                bumps.getLast().linkCollisionResponse());
        runtime.tick(EntityTestFrames.frame(runtime, 68, 56, 26, () -> 0));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
        assertEquals(10, runtime.enemies().enemyIgnoreHitsCountdown(2));
        assertEquals(10, runtime.enemies().enemyIgnoreHitsCountdown(1));
        var recoil = runtime.inspection().motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        assertEquals(32, recoil.recoilSpeedX(1));
        assertEquals(254, recoil.recoilSpeedY(1));
    }

    @Test
    void backgroundRestorationKeepsTheOriginalRecoilVector() {
        var runtime = chasingLeever(64, 65);
        runtime.combat().resolveCombat(new EntitySwordContactFrame(35, 63, 64, false, true, false, 0, 0, 0, 0));
        assertTrue(runtime.combat().deferContactRecoil(2, 63, 64));

        runtime.tick(EntityTestFrames.frame(runtime, 35, 63, 64, () -> 0, (entity, direction, x, y) -> true));

        assertEquals(64, runtime.inspection().snapshot().slots().get(2).x());
        assertResponse(runtime.outbox().consumePendingEntityEvents().getFirst(), 0xEC, 0xEC);
    }

    @Test
    void contactsRejectedByThePlayerDamageGateDoNotWriteRecoil() {
        var runtime = chasingLeever(110, 40);
        runtime.combat().resolveCombat(new EntitySwordContactFrame(35, 109, 39, false, true, false, 0, 0, 0, 0));
        runtime.tick(EntityTestFrames.frame(runtime, 35, 109, 39, () -> 0));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }

    @Test
    void skippedHandlerDoesNotLeaveAResponseForTheFollowingFrame() {
        var runtime = chasingLeever(110, 40);
        runtime.combat().resolveCombat(new EntitySwordContactFrame(35, 109, 39, false, true, false, 0, 0, 0, 0));
        runtime.combat().deferContactRecoil(2, 109, 39);
        runtime.configuration().setDialogActive(true);
        runtime.tick(EntityTestFrames.frame(runtime, 35, 109, 39, () -> 0));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
        runtime.configuration().setDialogActive(false);
        runtime.tick(EntityTestFrames.frame(runtime, 36, 109, 39, () -> 0));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }

    private static void assertResponse(EntityCombatEvent event, int speedX, int speedY) {
        assertEquals(2, event.slot());
        assertEquals(0, event.linkDamage());
        assertEquals(EntityCombatEvent.SoundChannel.NONE, event.soundChannel());
        assertEquals(new EntityCombatEvent.LinkCollisionResponse(speedX, speedY, 0x10), event.linkCollisionResponse());
    }

    private static RoomEntityRuntime chasingLeever(int x, int y) {
        var slots = new ArrayList<RoomEntity>();
        for (int slot = 0; slot < 16; slot++) slots.add(RoomEntity.disabled(slot));
        var sprite = new EntitySpriteDefinition(
                0x0E,
                4,
                0x7EE5,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                Collections.nCopies(
                        4,
                        new EntitySpriteDefinition.Variant(
                                new EntitySpriteDefinition.OamAttribute(0, 0),
                                new EntitySpriteDefinition.OamAttribute(0, 0))));
        slots.set(
                2,
                new RoomEntityBuilder()
                        .slot(2)
                        .sourceLoadOrder(0)
                        .type(0x0E)
                        .at(x, y)
                        .spriteDefinition(sprite)
                        .build());
        var runtime = new RoomEntityRuntimeBuilder(new RoomEntitySnapshot(slots)).build();
        for (int frame = 0; frame <= 34; frame++)
            runtime.tick(EntityTestFrames.frame(runtime, frame, x - 10, y, () -> 0));
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).leeverState(2));
        assertEquals(0xF8, new EntityEnemyInspectionQueries(runtime.enemies()).leeverSpeedX(2));
        return runtime;
    }
}
