package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static linksawakening.world.entity.EntityRuntimeTestSupport.syntheticRom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class HidingZolRuntimeTest {

    @Test
    void hidingZolUsesTheRomProximityRevealAndRisePhases() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()));
        EntitySpriteDefinition definition = catalog.forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9B)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x02))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x02)));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x02)));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolTransitionCountdown(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 80, 64, sequence(0x02)));
        assertEquals(0x1F, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolTransitionCountdown(0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 3; frame <= 18; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0x02)));
        }
        assertEquals(0x0F, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolTransitionCountdown(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 19; frame <= 33; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0x02)));
        }
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolTransitionCountdown(0));
        assertEquals(3, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(8, runtime.inspection().snapshot().slots().get(0).z());
        assertEquals(0x12, runtime.enemies().physicsFlags(0));
    }

    @Test
    void hidingZolUsesTheRomSwordAndLinkCollisionPhases() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()))
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9B)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, sequence(0x00)));
        assertTrue(runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, true, 72, 1, 72, 1))
                .isEmpty());

        int frame = 1;
        while (new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0) != 3 && frame < 200) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
        }
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));

        // The state-2 handler is the frame that enters state 3, so its
        // state-3 sword continuation has not run yet. Advance one handler
        // frame before checking state-3 collision behavior.
        runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
        RoomEntity hidingZol = runtime.inspection().snapshot().slots().get(0);
        List<EntityCombatEvent> contact = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(
                        1, hidingZol.x() + 4, hidingZol.y() + 4, false, true, false, 0, 0, 0, 0));
        assertTrue(contact.isEmpty());

        List<EntityCombatEvent> sword = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(
                        1, 120, 120, false, true, true, hidingZol.x() + 8, 1, hidingZol.y() + 8, 1));
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void hidingZolUsesTheBankSevenRecoilStepBeforeItsHandler() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()))
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9B)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        EntityRuntimeFixture.debug(runtime).configureEnemyRecoilForTest(0, 80, 80, 0x30);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x0A);

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));

        RoomEntity afterRecoil = runtime.inspection().snapshot().slots().get(0);
        assertEquals(61, afterRecoil.x());
        assertEquals(61, afterRecoil.y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
    }

    @Test
    void hidingZolPreservesBankSevenRecoilWhenTheBackgroundBlocksIt() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()))
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9B)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        runtime.configuration()
                .setBackgroundInteraction((entity, direction, nextX, nextY) ->
                        EntityBackgroundCollisionResult.blocked(direction, 0x2A, 0, nextX, nextY));
        EntityRuntimeFixture.debug(runtime).configureEnemyRecoilForTest(0, 80, 80, 0x30);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x0A);

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));

        RoomEntity afterRecoil = runtime.inspection().snapshot().slots().get(0);
        assertEquals(64, afterRecoil.x());
        assertEquals(64, afterRecoil.y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
    }

    @Test
    void hidingZolStateThreeMovesWithoutTheLaterBackgroundHelper() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()))
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9B)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .build();
        List<Integer> observedIgnoreHits = new ArrayList<>();
        runtime.configuration().setBackgroundInteraction(new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction, int nextX, int nextY) {
                observedIgnoreHits.add(-1);
                return EntityBackgroundCollisionResult.blocked(direction, 0x2A, 0, nextX, nextY);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
                observedIgnoreHits.add(ignoreHitsCountdown);
                return EntityBackgroundCollisionResult.blocked(direction, 0x2A, 0, nextX, nextY);
            }
        });
        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, sequence(0x00)));

        int frame = 1;
        while (new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0) != 3 && frame < 200) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
        }
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));
        observedIgnoreHits.clear();

        runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));

        assertTrue(observedIgnoreHits.isEmpty(), () -> "state-3 background probes=" + observedIgnoreHits);
    }

    @Test
    void hidingZolFlyingBackgroundHelperForcesTheRomIgnoreHitsValue() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()))
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9B)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .build();
        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, sequence(0x00)));

        int frame = 1;
        while (new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0) != 5 && frame < 300) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
        }
        assertEquals(5, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));

        List<Integer> observedIgnoreHits = new ArrayList<>();
        runtime.configuration().setBackgroundInteraction(new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction, int nextX, int nextY) {
                observedIgnoreHits.add(-1);
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
                observedIgnoreHits.add(ignoreHitsCountdown);
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }
        });
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x09);

        runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));

        assertTrue(observedIgnoreHits.contains(0x03), () -> "flying background probes=" + observedIgnoreHits);
        assertEquals(0, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void hidingZolRequestsTheRomJumpJingleWhenItLeavesTheGround() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()))
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9B)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        for (int frame = 2; frame <= 33; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0x00)));
        }

        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));
        assertEquals(
                List.of(new EntityCombatEvent(0, 0x9B, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x24)),
                runtime.outbox().consumePendingEntityEvents());
    }

    @Test
    void hidingZolRequestsTheRomJumpJingleForEachLaunchedLeap() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()))
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9B)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        for (int frame = 2; frame <= 33; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0x00)));
        }
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));
        assertEquals(
                0x24, runtime.outbox().consumePendingEntityEvents().getFirst().soundId());

        int frame = 34;
        while (new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0) != 4 && frame < 200) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
        }
        assertEquals(4, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));

        boolean foundSecondJumpJingle = false;
        for (int count = 0; count < 40; count++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
            List<EntityCombatEvent> events = runtime.outbox().consumePendingEntityEvents();
            if (!events.isEmpty()) {
                assertEquals(1, events.size());
                assertEquals(
                        EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
                assertEquals(0x24, events.getFirst().soundId());
                foundSecondJumpJingle = true;
                break;
            }
        }
        assertTrue(foundSecondJumpJingle);
        assertEquals(5, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));
    }

    @Test
    void hidingZolRestoresHarmlessPhysicsWhenItsFinalLeapHides() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()))
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9B)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));

        int frame = 2;
        while (new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0) != 6 && frame < 1000) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
        }

        assertEquals(6, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));
        assertEquals(0xD2, runtime.enemies().physicsFlags(0));
    }

    @Test
    void combatUsesTheEntityVisualYWhenHidingZolIsAirborne() {
        EntitySpriteDefinition definition = pairDefinition(0x9B, 4);
        RoomEntity airborne = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x9B)
                .at(64, 64)
                .spriteDefinition(definition)
                .z(0x10)
                .build();

        assertTrue(RoomEntityCombatRules.overlapsLink(airborne, 64, 48));
        assertFalse(RoomEntityCombatRules.overlapsLink(airborne, 64, 64));
        assertTrue(EntityCombatTestAccess.overlapsSword(airborne, 72, 1, 56, 1));
        assertFalse(EntityCombatTestAccess.overlapsSword(airborne, 72, 1, 72, 1));
    }
}
