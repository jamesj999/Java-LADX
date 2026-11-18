package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.MotionTestFixture;
import linksawakening.world.entity.motion.ZolGelMotion;
import linksawakening.world.entity.motion.ZolGelMotion.ZolGelFrame;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class ZolGelPhysicsTest {

    @Test
    void zolAndGelApplyBankSixRecoilBeforeTheirStateMovement() throws IOException {
        int[] types = {0x1B, 0x1C};
        RomEnemyCombatTables tables =
                new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom()));
        for (int type : types) {
            EntitySpriteDefinition definition = pairDefinition(type, 2);
            // ConfigureEntityRecoil runs before the ROM damage lookup. A
            // level-zero context keeps the one-health entity active so the
            // following handler tick can observe that recoil step.
            EnemyAttackContext nonDamaging = new EnemyAttackContext(0, false, false, false, false);
            RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                            .sourceLoadOrder(0)
                            .type(type)
                            .at(64, 64)
                            .spriteDefinition(definition)
                            .build()))
                    .indoorRoom(false)
                    .randomByteSupplier(sequence(0x01))
                    .spriteHandlers(null)
                    .enemyCombatTables(tables)
                    .build();

            runtime.combat()
                    .resolveCombat(
                            new EntityVerticalContactFrame(0, 120, 120, false, false, true, 72, 1, 72, 1, nonDamaging));
            runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x01)));

            assertEquals(61, runtime.inspection().snapshot().slots().get(0).x(), "type=" + Integer.toHexString(type));
            assertEquals(61, runtime.inspection().snapshot().slots().get(0).y(), "type=" + Integer.toHexString(type));
            assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0), "type=" + Integer.toHexString(type));
        }
    }

    @Test
    void zolAndGelKeepBankSixRecoilWhenBackgroundBlocksTheStep() throws IOException {
        int[] types = {0x1B, 0x1C};
        RomEnemyCombatTables tables =
                new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(loadRom()));
        for (int type : types) {
            EntitySpriteDefinition definition = pairDefinition(type, 2);
            EnemyAttackContext nonDamaging = new EnemyAttackContext(0, false, false, false, false);
            RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                            .sourceLoadOrder(0)
                            .type(type)
                            .at(64, 64)
                            .spriteDefinition(definition)
                            .build()))
                    .indoorRoom(false)
                    .randomByteSupplier(sequence(0x01))
                    .spriteHandlers(null)
                    .enemyCombatTables(tables)
                    .build();

            runtime.combat()
                    .resolveCombat(
                            new EntityVerticalContactFrame(0, 120, 120, false, false, true, 72, 1, 72, 1, nonDamaging));
            RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 1 || direction == 2;

            runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x01), wall));

            assertEquals(64, runtime.inspection().snapshot().slots().get(0).x(), "type=" + Integer.toHexString(type));
            assertEquals(64, runtime.inspection().snapshot().slots().get(0).y(), "type=" + Integer.toHexString(type));
            assertTrue(
                    new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0),
                    "type=" + Integer.toHexString(type));
            assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0), "type=" + Integer.toHexString(type));
        }
    }

    @Test
    void zolGelPhysicsUsesRomTemporaryIgnoreHitsForRichBackgroundProbes() {
        EntitySpriteDefinition definition = pairDefinition(0x1B, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1B)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        List<Integer> observedIgnoreHits = new ArrayList<>();
        runtime.configuration().setBackgroundInteraction(new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction, int nextX, int nextY) {
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
                observedIgnoreHits.add(ignoreHitsCountdown);
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }
        });

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, sequence(0x00)));
        for (int frame = 1; frame <= 8; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0x00)));
        }

        assertFalse(observedIgnoreHits.isEmpty());
        assertTrue(
                observedIgnoreHits.stream().allMatch(value -> value == 0x02),
                "observed ignore-hit values=" + observedIgnoreHits);
    }

    @Test
    void zolGelPhysicsSkipsRichBackgroundProbeWhilePrivateCountdownOneIsActive() {
        ZolGelMotion motion = new ZolGelMotion();
        RoomEntity entity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1B)
                .at(64, 64)
                .spriteDefinition(pairDefinition(0x1B, 2))
                .build();
        motion.prepareChestSpawn(entity.slot());
        MotionTestFixture.setPrivateCountdown1ForTest(motion, entity.slot(), 2);
        List<Integer> observedIgnoreHits = new ArrayList<>();
        RoomEntityBackgroundInteraction background = new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity probed, int direction, int nextX, int nextY) {
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity probed, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
                observedIgnoreHits.add(ignoreHitsCountdown);
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }
        };

        ZolGelMotion.Update first =
                motion.advance(new ZolGelFrame(entity, 80, 64, 0, () -> 0, background, 0, 0, false));
        assertTrue(observedIgnoreHits.isEmpty());
        assertEquals(1, motion.backgroundCollisionBypassCountdown(entity.slot()));

        motion.advance(new ZolGelFrame(first.entity(), 80, 64, 0, () -> 0, background, 1, 0, false));
        assertEquals(List.of(0x02), observedIgnoreHits);
        assertEquals(0, motion.backgroundCollisionBypassCountdown(entity.slot()));
    }
}
