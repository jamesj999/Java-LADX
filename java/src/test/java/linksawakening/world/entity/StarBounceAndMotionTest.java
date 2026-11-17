package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class StarBounceAndMotionTest {

    @Test
    void starGivesHorizontalBackgroundCollisionPriorityWhenBouncing() {
        EntitySpriteDefinition definition = pairDefinition(0x9C, 5);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9C)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));
        RoomEntityBackgroundCollision corner = (entity, direction, nextX, nextY) -> direction == 0 || direction == 3;
        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x00), corner));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0xF4, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedX(0));
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedY(0));
    }

    @Test
    void starLeavesItsRecoilAndMovementUntouchedDuringNonInteractiveLinkMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x9C, 5);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9C)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        EnemyProjectileCollision.LinkState nonInteractive =
                new EnemyProjectileCollision.LinkState(120, 120, 0, 0x02, 0, false);
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                120,
                120,
                sequence(0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                nonInteractive));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                2,
                120,
                120,
                sequence(0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                nonInteractive));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedX(0));
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedY(0));
    }

    @Test
    void starUsesTheNormalEnemyCombatValuesAndShadowPhysics() {
        EntitySpriteDefinition definition = pairDefinition(0x9C, 5);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9C)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0x9C));
        assertEquals(0x01, runtime.enemies().enemyHealth(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());
    }

    @Test
    void starUsesTheRomRandomSpeedVectorAndFourFrameAnimation() {
        EntitySpriteDefinition definition = pairDefinition(0x9C, 5);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x9C)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        // EntityInitWithRandomSpeed[1] = (+12, -12). The Star handler keeps
        // the fifth ROM display-list variant available but selects variants
        // 0..3 from (hFrameCounter >> 3) & 3.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x01)));
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedX(0));
        assertEquals(0xF4, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedY(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(63, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x00)));
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(62, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 24, 120, 120, sequence(0x00)));
        assertEquals(3, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }
}
