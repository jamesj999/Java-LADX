package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class OctorokRuntimeTest {

    @Test
    void octorokUsesTheSharedRoamingEnemyTimersAndDirectionSpeeds() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        IntSupplier randomBytes = sequence(0x02, 0x03);

        // State 0 pauses for ($10 + random & $0F) frames, then state 1
        // selects direction 3 (down) and the ROM's +8/16 fixed-point speed.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 200, 32, randomBytes));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).octorokState(0));
        assertEquals(0x12, new EntityEnemyInspectionQueries(runtime.enemies()).octorokTransitionCountdown(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).octorokSpeedY(0));

        for (int frame = 1; frame <= 0x12; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 200, 32, randomBytes));
        }
        assertEquals(
                0,
                new EntityEnemyInspectionQueries(runtime.enemies()).octorokState(0),
                () -> "state=" + new EntityEnemyInspectionQueries(runtime.enemies()).octorokState(0) + " timer="
                        + new EntityEnemyInspectionQueries(runtime.enemies()).octorokTransitionCountdown(0));
        assertEquals(0x23, new EntityEnemyInspectionQueries(runtime.enemies()).octorokTransitionCountdown(0));
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).octorokDirection(0));
        assertEquals(8, new EntityEnemyInspectionQueries(runtime.enemies()).octorokSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 0x13, 200, 32, randomBytes));
        runtime.tick(EntityTestFrames.frame(runtime, 0x14, 200, 32, randomBytes));
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void runtimePassesCreditsContextToSuppressOnlyOctorokLaunches() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, () -> 0, true));
        for (int frame = 1; frame <= 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, () -> 0, true));
        }

        assertTrue(new EntityRoomInspectionQueries(runtime.inspection())
                .projectileLaunchRequests()
                .isEmpty());
        assertEquals(1, runtime.inspection().snapshot().loadedEntities().size());
    }

    @Test
    void octorokStopsAtTheRoamingEnemyBackgroundCollisionPoint() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        IntSupplier randomBytes = sequence(0x00, 0x03, 0x01);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 200, 32, randomBytes));
        for (int frame = 1; frame <= 0x12; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 200, 32, randomBytes));
        }

        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 3;
        runtime.tick(EntityTestFrames.frame(runtime, 0x13, 200, 32, randomBytes, wall));
        runtime.tick(EntityTestFrames.frame(runtime, 0x14, 200, 32, randomBytes, wall));
        runtime.tick(EntityTestFrames.frame(runtime, 0x15, 200, 32, randomBytes, wall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).octorokSpeedY(0));
    }

    @Test
    void octorokUsesTheNormalEnemyHitboxAndOneBasicSwordDamage() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        RoomEntityRuntime swordRuntime = new RoomEntityRuntimeBuilder(initial).build();
        List<EntityCombatEvent> events = swordRuntime
                .combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(
                EntityStatus.DYING,
                swordRuntime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void lethalOctorokSwordHitStillPublishesFinalEnemyHitJingle() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x03, events.get(0).soundId());
    }
}
