package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class AggressiveStalfosRuntimeTest {

    @Test
    void stalfosAggressiveUsesTheRomPursuitAndJumpArc() {
        EntitySpriteDefinition definition = pairDefinition(0x1A, 3);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1A)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0x00)));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosState(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).z());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 64, 64, sequence(0x00)));
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosState(0));
        assertEquals(0x28, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosSpeedZ(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosSpeedX(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosSpeedY(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 64, 64, sequence(0x00)));
        assertEquals(0x02, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0x26, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosSpeedZ(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 3; frame <= 21; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 64, 64, sequence(0x00)));
        }
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosState(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosTransitionCountdown(0));
        assertEquals(0xC0, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosSpeedZ(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        int frame = 22;
        while (new EntityEnemyInspectionQueries(runtime.enemies()).stalfosState(0) != 0 && frame < 100) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 64, 64, sequence(0x00)));
        }
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosState(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosTransitionCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosSpeedZ(0));
    }

    @Test
    void stalfosJumpRestoresTheBlockedAxisAndRetainsVerticalAndHeightMotion() {
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1A)
                .at(97, 79)
                .spriteDefinition(pairDefinition(0x1A, 3))
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        var probes = new ArrayList<List<Integer>>();
        RoomEntityBackgroundCollision crystal = (entity, direction, x, y) -> {
            probes.add(List.of(direction, x, y));
            return direction == 1;
        };

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 69, sequence(0), crystal));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 69, sequence(0), crystal));
        assertEquals(List.of(List.of(1, 96, 78), List.of(2, 97, 78)), probes);
        var jumping = runtime.inspection().snapshot().slots().get(0);
        assertEquals(97, jumping.x());
        assertEquals(78, jumping.y());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 80, 69, sequence(0), crystal));
        var airborne = runtime.inspection().snapshot().slots().get(0);
        assertEquals(97, airborne.x());
        assertEquals(2, airborne.z());
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).stalfosState(0));
    }

    @Test
    void stalfosAggressiveUsesHealthGroupTwoACombatValues() {
        EntitySpriteDefinition definition = pairDefinition(0x1A, 3);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1A)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        assertEquals(1, runtime.enemies().enemyHealth(0));
    }
}
