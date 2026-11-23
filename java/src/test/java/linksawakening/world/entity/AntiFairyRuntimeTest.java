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
final class AntiFairyRuntimeTest {

    @Test
    void antiFairyReversesTheMovingAxisAtARomBackgroundCollisionPoint() {
        EntitySpriteDefinition definition = pairDefinition(0x15, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x15)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x02)));

        RoomEntityBackgroundCollision leftWall = (entity, direction, nextX, nextY) -> direction == 1;
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00), leftWall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).antiFairySpeedX(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void antiFairyUsesHealthGroupSixCombatValues() {
        EntitySpriteDefinition definition = pairDefinition(0x15, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x15)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());
        assertEquals(0x04, runtime.enemies().enemyHealth(0));

        List<EntityCombatEvent> sword =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0x03, runtime.enemies().enemyHealth(0));
    }

    @Test
    void antiFairyUsesTheRomRandomSpeedVectorAndFrameAnimation() {
        EntitySpriteDefinition definition = pairDefinition(0x15, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x15)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        // EntityRandomSpeedX/Y[1] = (+12, -12). The init handler runs before
        // the first active handler pass.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x01)));
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).antiFairySpeedX(0));
        assertEquals(0xF4, new EntityEnemyInspectionQueries(runtime.enemies()).antiFairySpeedY(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(63, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x00)));
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(62, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 8, 120, 120, sequence(0x00)));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }
}
