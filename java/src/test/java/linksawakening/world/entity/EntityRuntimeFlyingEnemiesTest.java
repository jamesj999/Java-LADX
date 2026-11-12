package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeFlyingEnemiesTest extends EntityRuntimeTestSupport {

    @Test
    void keeseSleepsOutsideTheRomWakeWindowAndWakesInsideIt() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x19)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 96, 64, sequence(0x00)));
        assertEquals(0, runtime.flyingEnemies().keeseState(0));
        assertEquals(0, runtime.flyingEnemies().keeseTransitionCountdown(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 64, sequence(0x00)));
        assertEquals(1, runtime.flyingEnemies().keeseState(0));
        assertEquals(0x50, runtime.flyingEnemies().keeseTransitionCountdown(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void keeseRetunesItsAngleAndLoadsTheRomSpeedTablesEveryTenFlightFrames() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x19)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        IntSupplier randomBytes = sequence(0x00, 0x01, 0x01);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 72, 64, randomBytes));
        for (int frame = 1; frame <= 11; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 72, 64, randomBytes));
        }

        assertEquals(1, runtime.flyingEnemies().keeseState(0));
        assertEquals(0x0D, runtime.flyingEnemies().keeseAngle(0));
        assertEquals(0x05, runtime.flyingEnemies().keeseSpeedX(0));
        assertEquals(0xF3, runtime.flyingEnemies().keeseSpeedY(0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void keeseUsesTheRomEnemyHitboxCadenceAndContactDamage() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x19)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> skipped =
                runtime.resolveCombat(new EntitySwordContactFrame(0, 64, 64, false, true, false, 0, 0, 0, 0));
        assertTrue(skipped.isEmpty());

        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0, contact.get(0).slot());
        assertEquals(0x19, contact.get(0).type());
        assertEquals(4, contact.get(0).linkDamage());
        assertFalse(contact.get(0).swordHit());

        assertTrue(runtime.resolveCombat(new EntitySwordContactFrame(1, 73, 64, false, true, false, 0, 0, 0, 0))
                .isEmpty());
        assertTrue(runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, true, true, false, 0, 0, 0, 0))
                .isEmpty());
        assertTrue(runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, false, false, 0, 0, 0, 0))
                .isEmpty());
    }

    @Test
    void aBasicSwordHitPutsKeeseIntoTheRomDyingState() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x19)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(0, events.get(0).linkDamage());
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(0x2F, new EntityEnemyInspectionQueries(runtime.enemies()).dyingCountdown(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).deathSpriteVariant());
        assertFalse(runtime.inspection().snapshot().slots().get(0).powerRecoilDeath());

        for (int frame = 1; frame <= 11; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(0x1F, new EntityEnemyInspectionQueries(runtime.enemies()).dyingCountdown(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).deathSpriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 12, 120, 120, sequence(0x00)));
        assertEquals(0x1E, new EntityEnemyInspectionQueries(runtime.enemies()).dyingCountdown(0));
        assertEquals(3, runtime.inspection().snapshot().slots().get(0).deathSpriteVariant());

        for (int frame = 13; frame < 42; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertTrue(runtime.inspection().snapshot().slots().get(0).loaded());
        runtime.tick(EntityTestFrames.frame(runtime, 42, 120, 120, sequence(0x00)));
        assertFalse(runtime.inspection().snapshot().slots().get(0).loaded());
    }

    @Test
    void peaHatRunsTheRomRestTakeoffAndHeightAnimationStates() {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xA0)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        IntSupplier randomBytes = sequence(0x00);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, randomBytes));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatPrivateState1(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 1; frame <= 128; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatState(0));
        assertEquals(8, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatPrivateState1(0));

        runtime.tick(EntityTestFrames.frame(runtime, 129, 120, 120, randomBytes));
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatState(0));
        assertEquals(0x80, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatSlowTransitionCountdown(0));

        for (int frame = 130; frame <= 136; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).z());
    }

    @Test
    void peaHatUsesTheRomPhaseTablesAfterReachingItsMaximumHeight() {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xA0)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        IntSupplier randomBytes = sequence(0x00);

        for (int frame = 0; frame <= 280; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }

        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatState(0));
        assertEquals(0x10, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatPrivateState4(0));
        assertEquals(0x07, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatSpeedY(0));

        for (int frame = 281; frame <= 304; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, randomBytes));
        }

        assertEquals(0x0F, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatPrivateState4(0));
        assertEquals(0x06, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatSpeedX(0));
        assertEquals(0xFD, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatSpeedY(0));
    }

    @Test
    void peaHatIsCombatVulnerableOnlyWhileGrounded() {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xA0)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> grounded =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, grounded.size());
        assertEquals(0x04, grounded.get(0).linkDamage());

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        List<EntityCombatEvent> airborne =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertTrue(airborne.isEmpty());
    }

    @Test
    void peaHatSwordHitConfiguresBankSevenRecoilBeforeItsStateMovement() throws IOException {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        EnemyAttackContext nonDamaging = new EnemyAttackContext(0, false, false, false, false);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xA0)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x01))
                .spriteHandlers(null)
                .enemyCombatTables(
                        new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom())))
                .build();

        List<EntityCombatEvent> events = runtime.resolveCombat(
                new EntityVerticalContactFrame(0, 120, 120, false, false, true, 72, 1, 72, 1, nonDamaging));

        assertEquals(1, events.size());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedX(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x01)));

        RoomEntity afterRecoil = runtime.inspection().snapshot().slots().get(0);
        assertEquals(61, afterRecoil.x());
        assertEquals(61, afterRecoil.y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void peaHatKeepsBankSevenRecoilWhenBackgroundBlocksTheStep() throws IOException {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        EnemyAttackContext nonDamaging = new EnemyAttackContext(0, false, false, false, false);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xA0)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x01))
                .spriteHandlers(null)
                .enemyCombatTables(
                        new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom())))
                .build();

        runtime.resolveCombat(
                new EntityVerticalContactFrame(0, 120, 120, false, false, true, 72, 1, 72, 1, nonDamaging));
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 1 || direction == 2;

        runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x01), wall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void peaHatRestoresItsPositionWhenTheRomBackgroundHelperBlocksRightwardMotion() {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xA0)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime blocked = RoomEntityRuntime.builder(initial).build();
        RoomEntityRuntime unblocked = RoomEntityRuntime.builder(initial).build();
        IntSupplier blockedRandom = sequence(0x00);
        IntSupplier unblockedRandom = sequence(0x00);

        for (int frame = 0; frame <= 280; frame++) {
            blocked.tick(EntityTestFrames.frame(blocked, frame, 120, 120, blockedRandom));
            unblocked.tick(EntityTestFrames.frame(unblocked, frame, 120, 120, unblockedRandom));
        }
        RoomEntity phaseStart = blocked.inspection().snapshot().slots().get(0);
        assertEquals(2, new EntityEnemyInspectionQueries(blocked.enemies()).peaHatState(0));
        assertEquals(0x07, new EntityEnemyInspectionQueries(blocked.enemies()).peaHatSpeedX(0));

        RoomEntityBackgroundCollision rightWall = (entity, direction, nextX, nextY) -> direction == 0;
        for (int frame = 281; frame <= 304; frame++) {
            blocked.tick(EntityTestFrames.frame(blocked, frame, 120, 120, blockedRandom, rightWall));
            unblocked.tick(EntityTestFrames.frame(unblocked, frame, 120, 120, unblockedRandom));
        }

        RoomEntity blockedEntity = blocked.inspection().snapshot().slots().get(0);
        RoomEntity unblockedEntity = unblocked.inspection().snapshot().slots().get(0);
        assertEquals(phaseStart.x(), blockedEntity.x());
        assertTrue(unblockedEntity.x() > blockedEntity.x());
        assertEquals(unblockedEntity.y(), blockedEntity.y());
        assertEquals(
                new EntityEnemyInspectionQueries(unblocked.enemies()).peaHatState(0),
                new EntityEnemyInspectionQueries(blocked.enemies()).peaHatState(0));
        assertEquals(
                new EntityEnemyInspectionQueries(unblocked.enemies()).peaHatSpeedX(0),
                new EntityEnemyInspectionQueries(blocked.enemies()).peaHatSpeedX(0));
        assertEquals(
                new EntityEnemyInspectionQueries(unblocked.enemies()).peaHatSpeedY(0),
                new EntityEnemyInspectionQueries(blocked.enemies()).peaHatSpeedY(0));
    }

    @Test
    void peaHatUsesTheRomSwordClinkPathWhileAirborne() {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0xA0)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).peaHatState(0));

        List<EntityCombatEvent> events =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 120, 120, false, false, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        EntityCombatEvent event = events.get(0);
        assertEquals(0xA0, event.type());
        assertTrue(event.swordHit());
        assertEquals(0, event.linkDamage());
        assertEquals(0, event.enemyDamage());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, event.soundChannel());
        assertEquals(0x07, event.soundId());
        assertEquals(new EntityCombatEvent.SwordPokeVfx(0x40, 0x40), event.swordPokeVfx());
        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x10, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertFalse(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
    }
}
