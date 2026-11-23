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
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EnemyRecoilRuntimeTest {

    @Test
    void richBackgroundProbeSuppliesIgnoreHitsToLegacyRecoilMovement() {
        RoomEntity initial = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(0x40, 0x40)
                .spriteDefinition(pairDefinition(0x0B, 2))
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
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0, (entity, direction, nextX, nextY) -> true));

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
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));

        for (int frame = 0; frame < 0x40; frame++) {
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
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedX(0));
        assertEquals(0xD0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilSpeedY(0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 64, 64, sequence(0)));
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
