package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class SpikeTrapLaunchAndCollisionTest {

    @Test
    void spikeTrapForwardEndpointPublishesOneRomSwordPokeJingle() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x27)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 80, 64, sequence(0x00)));
        assertEquals(
                0x0A, runtime.outbox().consumePendingEntityEvents().getFirst().soundId());

        for (int frame = 3; frame <= 25; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0x00)));
            assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
        }

        runtime.tick(EntityTestFrames.frame(runtime, 26, 80, 64, sequence(0x00)));
        List<EntityCombatEvent> endpointEvents = runtime.outbox().consumePendingEntityEvents();
        assertEquals(1, endpointEvents.size());
        assertEquals(
                EntityCombatEvent.SoundChannel.JINGLE, endpointEvents.get(0).soundChannel());
        assertEquals(0x07, endpointEvents.get(0).soundId());
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapState(0));

        runtime.tick(EntityTestFrames.frame(runtime, 27, 80, 64, sequence(0x00)));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }

    @Test
    void spikeTrapForwardBackgroundCollisionPublishesTheSameRomJingle() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x27)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 80, 64, sequence(0x00)));
        assertEquals(
                0x0A, runtime.outbox().consumePendingEntityEvents().getFirst().soundId());

        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> true;
        runtime.tick(EntityTestFrames.frame(runtime, 3, 80, 64, sequence(0x00), wall));

        List<EntityCombatEvent> collisionEvents = runtime.outbox().consumePendingEntityEvents();
        assertEquals(1, collisionEvents.size());
        assertEquals(
                EntityCombatEvent.SoundChannel.JINGLE, collisionEvents.get(0).soundChannel());
        assertEquals(0x07, collisionEvents.get(0).soundId());
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapState(0));
    }

    @Test
    void spikeTrapBackgroundCollisionKeepsItInItsLaunchState() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x27)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(EntityTestFrames.frame(runtime, 2, 80, 64, sequence(0x00), wall));

        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapTransitionCountdown(0));
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapSpeedX(0));
    }

    @Test
    void spikeTrapSwordCollisionPublishesTheRomSwordPokeWithoutDamage() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x27)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        assertEquals(4, runtime.enemies().enemyHealth(0));
        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(8, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        EntityCombatEvent event = sword.get(0);
        assertTrue(event.swordHit());
        assertEquals(0, event.enemyDamage());
        assertEquals(-1, event.enemySpecialAction());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, event.soundChannel());
        assertEquals(0x07, event.soundId());
        assertEquals(new EntityCombatEvent.SwordPokeVfx(0x40, 0x40), event.swordPokeVfx());
        assertEquals(4, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x10, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertFalse(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
    }

    @Test
    void spikeTrapUsesTheRomRandomDirectionAndFourStateLaunchLoop() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x27)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapDirection(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapState(0));
        assertEquals(64, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapHomeX(0));
        assertEquals(64, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapHomeY(0));

        // Link is within the Y window, so state 1 chooses the right-facing
        // $20 X speed and immediately enters state 2.
        runtime.tick(EntityTestFrames.frame(runtime, 2, 80, 64, sequence(0x00)));
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapDirection(0));
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapSpeedY(0));
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapTransitionCountdown(0));

        runtime.tick(EntityTestFrames.frame(runtime, 3, 80, 64, sequence(0x00)));
        assertEquals(66, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x17, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapTransitionCountdown(0));

        for (int frame = 4; frame <= 26; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0x00)));
        }
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapState(0));
        assertEquals(112, runtime.inspection().snapshot().slots().get(0).x());

        int frame = 27;
        while (new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapState(0) != 1 && frame < 280) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
        }
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapState(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapTransitionCountdown(0));
    }

    @Test
    void spikeTrapLaunchPublishesOneRomWhooshNoise() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x27)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 80, 64, sequence(0x00)));

        List<EntityCombatEvent> launchEvents = runtime.outbox().consumePendingEntityEvents();
        assertEquals(1, launchEvents.size());
        assertEquals(0, launchEvents.get(0).slot());
        assertEquals(0x27, launchEvents.get(0).type());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE, launchEvents.get(0).soundChannel());
        assertEquals(0x0A, launchEvents.get(0).soundId());

        runtime.tick(EntityTestFrames.frame(runtime, 3, 80, 64, sequence(0x00)));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }

    @Test
    void spikeTrapBlockedLaunchDoesNotPublishTheWhooshNoise() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x27)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(EntityTestFrames.frame(runtime, 2, 80, 64, sequence(0x00), wall));

        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }

    @Test
    void spikeTrapFallsBackToTheRomVerticalLaunchTable() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x27)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x02)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 64, 100, sequence(0x02)));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 64, 100, sequence(0x02)));

        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapState(0));
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapDirection(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapSpeedX(0));
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapSpeedY(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapTransitionCountdown(0));
    }
}
