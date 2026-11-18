package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
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
