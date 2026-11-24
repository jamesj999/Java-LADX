package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.MotionTestFixture;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@linksawakening.testing.RequiresRom
final class EnemyRecoilRuntimeTest {

    @ParameterizedTest
    @ValueSource(ints = {0x09, 0x0B})
    void roamingSwordHitMatchesTheNativeHitFrameAndNextWallRecoil(int type) {
        RoomEntity initial = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(type)
                .at(58, 82)
                .spriteDefinition(pairDefinition(type, 8))
                .build();
        var runtime = new RoomEntityRuntimeBuilder(snapshot(initial)).build();
        var roaming = runtime.enemies().motions().get(RoamingEnemyMotion.class, RoamingEnemyMotion::new);
        MotionTestFixture.setStateForTest(roaming, 0, 1, 100, 0, 0, 0);
        runtime.enemies().combatTables().writeEnemyHealth(0, 8);
        RoomEntityBackgroundCollision wall = (e, direction, x, y) -> direction == 0 && x > 58;

        runtime.combat().resolveCombat(new EntitySwordContactFrame(135, 51, 88, false, true, true, 66, 5, 90, 5));
        runtime.tick(EntityTestFrames.frame(runtime, 135, 51, 88, () -> 0, wall));
        assertEquals(7, runtime.enemies().enemyHealth(0));
        assertEquals(10, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(99, roaming.transitionCountdown(0));
        assertEquals(82, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 136, 51, 88, () -> 0, wall));
        assertEquals(0, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(0x40, roaming.transitionCountdown(0));
        assertEquals(58, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(79, runtime.inspection().snapshot().slots().get(0).y());
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
            9, true, false
            9, false, false
            9, true, true
            11, true, false
            11, false, false
            11, true, true
            """)
    void lethalRoamingSwordHitCompletesTheCurrentHandlerBeforeTheDeathDispatch(
            int type, boolean open, boolean walking) {
        RoomEntity initial = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(type)
                .at(58, 82)
                .spriteDefinition(pairDefinition(type, 8))
                .build();
        var runtime = new RoomEntityRuntimeBuilder(snapshot(initial)).build();
        var roaming = runtime.enemies().motions().get(RoamingEnemyMotion.class, RoamingEnemyMotion::new);
        MotionTestFixture.setStateForTest(roaming, 0, walking ? 0 : 1, 100, 0, 0, walking ? 1 : 0);
        var combat = runtime.enemies().combatTables();
        combat.writeEnemyHealth(0, 1);
        RoomEntityBackgroundCollision background = (e, direction, x, y) -> !open && direction == 0 && x > 58;

        runtime.combat().resolveCombat(new EntitySwordContactFrame(135, 51, 88, false, true, true, 66, 5, 90, 5));
        runtime.tick(EntityTestFrames.frame(runtime, 135, 51, 88, () -> 0, background));
        var hit = runtime.inspection().snapshot().slots().getFirst();
        int startX = walking ? 57 : 58;
        assertEquals(startX, hit.x());
        assertEquals(82, hit.y());
        assertEquals(EntityStatus.DYING, hit.status());
        assertEquals(47, combat.readDyingCountdown(0));
        assertEquals(10, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(99, roaming.transitionCountdown(0));
        assertEquals(0, roaming.state(0));

        int[] openY = {79, 76, 74, 71, 69, 66, 64, 61, 58, 56};
        for (int step = 1; step <= (open ? 10 : 1); step++) {
            runtime.tick(EntityTestFrames.frame(runtime, 135 + step, 51, 88, () -> 0, background));
            var enemy = runtime.inspection().snapshot().slots().getFirst();
            assertEquals(open ? startX + 3 * step : startX, enemy.x());
            assertEquals(openY[step - 1], enemy.y());
            assertEquals(99 - step, roaming.transitionCountdown(0));
            assertEquals(0, roaming.state(0));
            assertEquals(open ? 10 - step : 0, runtime.enemies().enemyIgnoreHitsCountdown(0));
        }
        runtime.tick(EntityTestFrames.frame(runtime, open ? 146 : 137, 51, 88, () -> 0, background));
        assertEquals(0x1F, combat.readDyingCountdown(0));
    }

    @Test
    void richBackgroundProbeSuppliesIgnoreHitsToLegacyRecoilMovement() {
        RoomEntity initial = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(0x40, 0x40)
                .spriteDefinition(pairDefinition(0x0B, 8))
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(initial)).build();
        List<Integer> observedIgnoreHits = new ArrayList<>();
        runtime.configuration().setBackgroundInteraction(new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction, int nextX, int nextY) {
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown) {
                observedIgnoreHits.add(ignoreHitsCountdown);
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }
        });

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 0x40, 0x40, false, false, true, 0x30, 0x30, 0x30, 0x30));
        assertEquals(1, events.size());
        assertFalse(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0, (entity, direction, nextX, nextY) -> true));
        assertEquals(0x40, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0, (entity, direction, nextX, nextY) -> true));

        assertTrue(
                observedIgnoreHits.contains(0x09),
                () -> "observed=" + observedIgnoreHits
                        + " x=" + runtime.inspection().snapshot().slots().get(0).x()
                        + " status="
                        + runtime.inspection().snapshot().slots().get(0).status()
                        + " recoil=" + new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0)
                        + " countdown=" + runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(0x3D, runtime.inspection().snapshot().slots().get(0).x());
    }

    @Test
    void clearingARecoilingEnemyClearsAllRecoilStateBeforeSlotReuse() {
        EntitySpriteDefinition definition = pairDefinition(0x0B, 8);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0B)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.combat().resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));

        assertEquals(1, runtime.lifecycle().clearEntity(0));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertFalse(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedY(0));
        assertEquals(0, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void dyingCleanupClearsRecoilStateWhenTheRomDeathCountdownExpires() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x09)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.combat().resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));

        for (int frame = 1; frame < 0x40; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertFalse(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedY(0));
    }

    @Test
    void swordHitAtLinksExactPositionRecoilsUpAndLeftThroughTheLiveRuntime() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x20)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x20, 2))
                        .build()))
                .build();
        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 64, 64, false, false, true, 72, 1, 72, 1));
        assertEquals(1, events.size());
        runtime.tick(EntityTestFrames.frame(runtime, 1, 64, 64, sequence(0)));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedX(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedY(0));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 64, 64, sequence(0)));
        assertEquals(61, runtime.inspection().snapshot().slots().getFirst().x());
        assertEquals(61, runtime.inspection().snapshot().slots().getFirst().y());
    }

    @Test
    void bankSixNormalEnemiesConfigureTheRomSharedSwordRecoil() {
        int[] types = {0x19, 0x0D, 0x15, 0x1A, 0x16, 0x17, 0x1B, 0x1C};
        for (int type : types) {
            EntitySpriteDefinition definition = pairDefinition(type, 3);
            RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                            .sourceLoadOrder(0)
                            .type(type)
                            .at(64, 64)
                            .spriteDefinition(definition)
                            .build()))
                    .build();

            List<EntityCombatEvent> events = runtime.combat()
                    .resolveCombat(new EntitySwordContactFrame(0, 72, 72, false, true, true, 72, 1, 72, 1));

            assertEquals(1, events.size(), "type=" + Integer.toHexString(type));
            assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0), "type=" + Integer.toHexString(type));
            assertTrue(
                    new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0),
                    "type=" + Integer.toHexString(type));
            assertEquals(
                    0xD0,
                    new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedX(0),
                    "type=" + Integer.toHexString(type));
            assertEquals(
                    0xD0,
                    new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedY(0),
                    "type=" + Integer.toHexString(type));
        }
    }
}
