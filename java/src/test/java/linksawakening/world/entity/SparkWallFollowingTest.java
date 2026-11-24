package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class SparkWallFollowingTest {

    @Test
    void sparkInitializersSelectTheRomDirectionAndOffset() {
        EntitySpriteDefinition counterDefinition = pairDefinition(0x16, 2);
        RoomEntityRuntime counterClockwise = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x16)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(counterDefinition)
                        .build()))
                .build();
        counterClockwise.tick(EntityTestFrames.frame(counterClockwise, 0));

        assertEquals(61, counterClockwise.inspection().snapshot().slots().get(0).y());
        assertEquals(0, new EntityEnemyInspectionQueries(counterClockwise.enemies()).sparkWindingOffset(0));
        assertEquals(0, new EntityEnemyInspectionQueries(counterClockwise.enemies()).sparkSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(counterClockwise.enemies()).sparkSpeedY(0));

        EntitySpriteDefinition clockwiseDefinition = pairDefinition(0x17, 2);
        RoomEntityRuntime clockwise = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x17)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(clockwiseDefinition)
                        .build()))
                .build();
        clockwise.tick(EntityTestFrames.frame(clockwise, 0));

        assertEquals(67, clockwise.inspection().snapshot().slots().get(0).y());
        assertEquals(4, new EntityEnemyInspectionQueries(clockwise.enemies()).sparkWindingOffset(0));
    }

    @Test
    void sparkFollowsTheRomEightDirectionTableAndFrameCadence() {
        EntitySpriteDefinition definition = pairDefinition(0x16, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x16)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, () -> 0));

        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).sparkWallTurnIndex(0));
        assertEquals(9, new EntityEnemyInspectionQueries(runtime.enemies()).sparkTransitionCountdown(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).sparkSpeedX(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).sparkSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, () -> 0));
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void chestDialogKeepsBothSparksFlashingInPlaceAndPreservesTheirTurnTimers() {
        for (int type : new int[] {0x16, 0x17}) {
            var runtime = sparkRuntime(type);
            var inspection = new EntityEnemyInspectionQueries(runtime.enemies());
            runtime.tick(EntityTestFrames.frame(runtime, 0));
            runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, () -> 0));
            var before = runtime.inspection().slots().get(0);

            runtime.configuration().setDialogActive(true);
            for (int frame = 2; frame < 132; frame++) {
                runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, () -> 0));
                var spark = runtime.inspection().slots().get(0);
                assertEquals(before.x(), spark.x());
                assertEquals(before.y(), spark.y());
                assertEquals((frame >>> 1) & 1, spark.spriteVariant());
                assertEquals(9, inspection.sparkTransitionCountdown(0));
            }
            runtime.configuration().setDialogActive(false);
            runtime.configuration().setOcarinaPlayback(4, 0, 0);
            runtime.tick(EntityTestFrames.frame(runtime, 132, 120, 120, () -> 0));
            assertEquals(before.x() + 1, runtime.inspection().slots().get(0).x());
            assertEquals(9, inspection.sparkTransitionCountdown(0));
            runtime.configuration().setOcarinaPlayback(0, 0, 0);
            runtime.tick(EntityTestFrames.frame(runtime, 133, 120, 120, () -> 0));
            assertEquals(8, inspection.sparkTransitionCountdown(0));
        }
    }

    @Test
    void incomingRecoilReplacesWallFollowingThroughTheLastRecoilTick() {
        var runtime = sparkRuntime(0x16);
        runtime.tick(EntityTestFrames.frame(runtime, 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, () -> 0));
        var recoil = runtime.enemies().motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        recoil.configureFromSpeed(0, 0x18, 0);
        runtime.inspection().combatTables().writeEnemyIgnoreHitsCountdown(0, 2);
        RoomEntityBackgroundCollision wall = (entity, direction, x, y) -> true;
        runtime.configuration().setDialogActive(true);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, () -> 0, wall));
        assertEquals(2, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(64, runtime.inspection().slots().get(0).x());
        runtime.configuration().setDialogActive(false);
        runtime.tick(EntityTestFrames.frame(runtime, 3, 120, 120, () -> 0, wall));
        assertEquals(65, runtime.inspection().slots().get(0).x());
        runtime.tick(EntityTestFrames.frame(runtime, 4, 120, 120, () -> 0, wall));
        assertEquals(67, runtime.inspection().slots().get(0).x());
        assertEquals(0, runtime.enemies().enemyIgnoreHitsCountdown(0));
        runtime.tick(EntityTestFrames.frame(runtime, 5, 120, 120, () -> 0));
        assertEquals(68, runtime.inspection().slots().get(0).x());
    }

    private static RoomEntityRuntime sparkRuntime(int type) {
        return new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(type)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(pairDefinition(type, 2))
                        .build()))
                .build();
    }

    @Test
    void sparkUsesCollisionMaskToAdvanceItsRomDirectionIndex() {
        EntitySpriteDefinition definition = pairDefinition(0x16, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x16)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        runtime.tick(EntityTestFrames.frame(runtime, 0));

        RoomEntityBackgroundCollision rightWall = (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, () -> 0, rightWall));

        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).sparkWallTurnIndex(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).sparkSpeedX(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).sparkSpeedY(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).sparkTransitionCountdown(0));
    }

    @Test
    void sparkUsesHealthGroupTwoCCombatValues() {
        EntitySpriteDefinition definition = pairDefinition(0x16, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x16)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());
        assertEquals(0x01, runtime.enemies().enemyHealth(0));

        List<EntityCombatEvent> sword =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }
}
