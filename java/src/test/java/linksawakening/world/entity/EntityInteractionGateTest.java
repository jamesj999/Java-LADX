package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityInteractionGateTest {

    @Test
    void inventoryTransitionMakesEntityHandlersNonInteractiveWithoutSkippingThePass() {
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
        runtime.configuration().setTalkState(true, 0, 0x78);
        EnemyProjectileCollision.LinkState interactive =
                new EnemyProjectileCollision.LinkState(120, 120, 0, 0, 0, false);

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                120,
                120,
                sequence(0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                interactive));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                2,
                120,
                120,
                sequence(0x00),
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                interactive));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedX(0));
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedY(0));
    }
}
