package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class HardhatBeetleRuntimeTest {

    @Test
    void hardHatUsesTheRomTargetVectorAndFourFrameSpeedRefresh() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x20)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 72, sequence(0x00)));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).hardHatSpeedX(0));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).hardHatSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());

        for (int frame = 1; frame <= 4; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 72, sequence(0x00)));
        }
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).hardHatSpeedX(0));
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).hardHatSpeedY(0));
    }

    @Test
    void hardHatUsesItsRomNormalEnemyHealthAndContactValues() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x20)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, firstHit.size());
        assertEquals(0x03, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void hardHatSwordHitConfiguresAndAppliesTheRomSharedRecoil() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x20)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        List<EntityCombatEvent> events =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(0, 72, 72, false, true, true, 72, 1, 72, 1));

        assertEquals(1, events.size());
        assertEquals(0x03, runtime.enemies().enemyHealth(0));
        assertEquals(0, runtime.enemies().enemyIgnoreHitsCountdown(0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x00)));

        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedX(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedY(0));

        assertEquals(64, runtime.inspection().slots().get(0).x());
        assertEquals(64, runtime.inspection().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 72, 72, sequence(0x00)));

        RoomEntity afterRecoil = runtime.inspection().snapshot().slots().get(0);
        assertEquals(61, afterRecoil.x());
        assertEquals(61, afterRecoil.y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void hardHatSharedRecoilKeepsStateWhenTheRomBackgroundPathBlocksIt() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x20)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.combat().resolveCombat(new EntitySwordContactFrame(0, 72, 72, false, true, true, 72, 1, 72, 1));
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 1 || direction == 2;

        runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x00), wall));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 72, 72, sequence(0x00), wall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void shieldBumpsHardhatBeforeItsBodyReachesLink() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x20)
                        .at(104, 72)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        var shield = new EnemyAttackContext(0, false, false, false, false, true, 0, 0);
        var frame = new EnemyCombatFrame(234, 90, 72, false, 0, true, false, 0, 0, 0, 0, shield);

        var bump = runtime.combat().resolveCombat(frame).getLast();
        assertEquals(0, bump.linkDamage());
        assertEquals(0x09, bump.soundId());
        assertEquals(new EntityCombatEvent.LinkCollisionResponse(0xEE, 0, 12), bump.linkCollisionResponse());
        runtime.tick(EntityTestFrames.frame(runtime, 234, 90, 72, sequence(0x00)));
        assertEquals(104, runtime.inspection().slots().get(0).x());
        assertEquals(10, runtime.enemies().enemyIgnoreHitsCountdown(0));

        runtime.tick(EntityTestFrames.frame(runtime, 235, 88, 72, sequence(0x00)));
        assertEquals(106, runtime.inspection().slots().get(0).x());
        assertEquals(9, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void bankSixRecoilUsesTheOrdinaryFractionsAndSkipsSteeringOnItsLastTick() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x20)
                        .at(104, 72)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 88, sequence(0x00)));
        for (int i = 0; i < 8; i++) {
            runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 88, sequence(0x00)));
        }
        var queries = new EntityEnemyInspectionQueries(runtime.enemies());
        assertEquals(1, queries.hardHatSpeedX(0));
        assertEquals(1, queries.hardHatSpeedY(0));
        runtime.inspection()
                .motions()
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .configureFromSpeed(0, 8, 8);
        runtime.inspection().combatTables().writeEnemyIgnoreHitsCountdown(0, 3);

        runtime.inspection().linkState().writeDialogActive(true);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 88, sequence(0x00)));
        assertEquals(104, runtime.inspection().slots().get(0).x());
        assertEquals(3, runtime.enemies().enemyIgnoreHitsCountdown(0));
        runtime.inspection().linkState().writeDialogActive(false);

        int[] expectedX = {105, 105, 106};
        int[] expectedY = {73, 73, 74};
        for (int i = 0; i < expectedX.length; i++) {
            runtime.tick(EntityTestFrames.frame(runtime, 2 + i, 120, 88, sequence(0x00)));
            assertEquals(expectedX[i], runtime.inspection().slots().get(0).x());
            assertEquals(expectedY[i], runtime.inspection().slots().get(0).y());
            assertEquals(2 - i, runtime.enemies().enemyIgnoreHitsCountdown(0));
            assertEquals(1, queries.hardHatSpeedX(0));
            assertEquals(1, queries.hardHatSpeedY(0));
        }
    }
}
