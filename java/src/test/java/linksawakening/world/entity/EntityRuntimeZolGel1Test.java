package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.MotionTestFixture;
import linksawakening.world.entity.motion.ZolGelFrame;
import linksawakening.world.entity.motion.ZolGelMotion;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeZolGel1Test extends EntityRuntimeTestSupport {

    @Test
    void zolAndGelApplyBankSixRecoilBeforeTheirStateMovement() throws IOException {
        int[] types = {0x1B, 0x1C};
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        for (int type : types) {
            EntitySpriteDefinition definition = pairDefinition(type, 2);
            // ConfigureEntityRecoil runs before the ROM damage lookup. A
            // level-zero context keeps the one-health entity active so the
            // following handler tick can observe that recoil step.
            EnemyAttackContext nonDamaging = new EnemyAttackContext(0, false, false, false, false);
            RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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

            runtime.resolveCombat(
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
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        for (int type : types) {
            EntitySpriteDefinition definition = pairDefinition(type, 2);
            EnemyAttackContext nonDamaging = new EnemyAttackContext(0, false, false, false, false);
            RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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

            runtime.resolveCombat(
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
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
        assertEquals(1, motion.privateCountdown1(entity.slot()));

        motion.advance(new ZolGelFrame(first.entity(), 80, 64, 0, () -> 0, background, 1, 0, false));
        assertEquals(List.of(0x02), observedIgnoreHits);
        assertEquals(0, motion.privateCountdown1(entity.slot()));
    }

    @Test
    void gelClingingStateRunsTheRomBackgroundProbeAfterLinkRelativePlacement() {
        ZolGelMotion motion = new ZolGelMotion();
        RoomEntity entity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1C)
                .at(64, 64)
                .spriteDefinition(pairDefinition(0x1C, 2))
                .build();
        motion.prepareChestSpawn(entity.slot());
        motion.onLinkCollision(entity.slot());
        List<Integer> observedDirections = new ArrayList<>();
        List<Integer> observedIgnoreHits = new ArrayList<>();

        RoomEntityBackgroundInteraction background = new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity probed, int direction, int nextX, int nextY) {
                observedDirections.add(direction);
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity probed, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
                observedDirections.add(direction);
                observedIgnoreHits.add(ignoreHitsCountdown);
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }
        };

        motion.advance(new ZolGelFrame(entity, 80, 64, 0, () -> 0, background, 0, 0x09, false));

        assertEquals(List.of(0), observedDirections);
        assertEquals(List.of(0x09), observedIgnoreHits);
    }

    @Test
    void zolUsesTheRomInchingAndLeapStateLoop() {
        EntitySpriteDefinition definition = pairDefinition(0x1B, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1B)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x01))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, sequence(0x01)));
        assertEquals(2, runtime.enemies().enemyHealth(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x01)));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
        assertEquals(7, new EntityEnemyInspectionQueries(runtime.enemies()).zolTransitionCountdown(0));
        assertEquals(0x04, new EntityEnemyInspectionQueries(runtime.enemies()).zolSpeedX(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).zolSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 2, 80, 64, sequence(0x01)));
        assertEquals(6, new EntityEnemyInspectionQueries(runtime.enemies()).zolTransitionCountdown(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
    }

    @Test
    void zolSplittingTurnsTheOriginalSlotIntoAGelAndUsesTheLastFreeSlot() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables combatTables = new RomEnemyCombatTables(rom);
        assertEquals(1, combatTables.initialHealth(0x1B));
        EntitySpriteDefinition definition = catalog.forEntityType(0x1B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1B)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x01))
                .spriteHandlers(catalog)
                .enemyCombatTables(combatTables)
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, sequence(0x01)));
        assertEquals(2, runtime.enemies().enemyHealth(0));
        List<EntityCombatEvent> sword =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(
                0x7C11,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(0)
                        .spriteDefinition()
                        .address());

        for (int frame = 1; frame <= 16; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0x01)));
        }

        RoomEntity originalSlot = runtime.inspection().snapshot().slots().get(0);
        RoomEntity spawnedSlot = runtime.inspection().snapshot().slots().get(15);
        assertEquals(0x1C, originalSlot.type());
        assertEquals(0x1C, spawnedSlot.type());
        assertEquals(EntityStatus.ACTIVE, spawnedSlot.status());
        assertEquals(0, originalSlot.sourceLoadOrder());
        assertEquals(0, spawnedSlot.sourceLoadOrder());
        assertEquals(originalSlot.x() + 12 & 0xFF, spawnedSlot.x());
        assertEquals(originalSlot.y(), spawnedSlot.y());
        assertEquals(originalSlot.z(), spawnedSlot.z());
        assertEquals(1, runtime.enemies().enemyHealth(15));
        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(0x11, runtime.enemies().physicsFlags(0));
        assertEquals(0x11, runtime.enemies().physicsFlags(15));
        assertEquals(0x10, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(0));
        assertEquals(0x10, EntityRuntimeFixture.enemies(runtime).hitboxFlagsForTest(15));
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).zolSpeedZ(0));
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).zolSpeedZ(15));
        assertFalse(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(0));
        assertFalse(new EntityEnemyInspectionQueries(runtime.enemies()).enemyRecoilActive(15));
        assertEquals(0, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void zolStateOneNonLeapBranchClearsStateRatherThanCountdown() {
        EntitySpriteDefinition definition = pairDefinition(0x1B, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1B)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x01))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, sequence(0x01)));
        for (int frame = 1; frame <= 7; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0x01)));
        }

        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).zolTransitionCountdown(0));
        int pausedX = runtime.inspection().snapshot().slots().get(0).x();
        int pausedY = runtime.inspection().snapshot().slots().get(0).y();
        for (int frame = 8; frame < 23; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0x01)));
            RoomEntity paused = runtime.inspection().snapshot().slots().get(0);
            assertEquals(pausedX, paused.x(), "pause X frame " + frame);
            assertEquals(pausedY, paused.y(), "pause Y frame " + frame);
            assertEquals(0, paused.z(), "no leap frame " + frame);
            assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
        }
        runtime.tick(EntityTestFrames.frame(runtime, 23, 80, 64, sequence(0x01)));
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
        assertEquals(7, new EntityEnemyInspectionQueries(runtime.enemies()).zolTransitionCountdown(0));
    }

    @Test
    void zolStateTwoSetsItsSpeedBeforeTheRomPhysicsStep() {
        EntitySpriteDefinition definition = pairDefinition(0x1B, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1B)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, sequence(0x00)));
        for (int frame = 1; frame <= 7; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0x00)));
        }
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
        assertEquals(0x50, new EntityEnemyInspectionQueries(runtime.enemies()).zolTransitionCountdown(0));

        runtime.tick(EntityTestFrames.frame(runtime, 8, 80, 64, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 9, 80, 64, sequence(0x00)));

        assertEquals(0x40, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x40, runtime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void gelUsesTheRomSmallEnemyHitboxAndClingingCollisionState() {
        EntitySpriteDefinition definition = pairDefinition(0x1C, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1C)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        assertTrue(runtime.resolveCombat(new EntitySwordContactFrame(1, 72, 64, false, true, false, 0, 0, 0, 0))
                .isEmpty());

        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 68, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0, contact.get(0).linkDamage());
        assertFalse(contact.get(0).linkCollisionResponse().active());
        assertEquals(4, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
        assertTrue(runtime.resolveCombat(new EntitySwordContactFrame(1, 68, 64, false, true, false, 0, 0, 0, 0))
                .isEmpty());
    }

    @Test
    void gelExportsClingFlagOnlyWhileItsInteractiveHandlerIsAttached() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1C)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x1C, 2))
                        .build()))
                .build();
        runtime.resolveCombat(new EntitySwordContactFrame(1, 68, 64, false, true, false, 0, 0, 0, 0));
        assertFalse(runtime.gelClingingToLink());
        runtime.configuration().setJoypadHeld(true);
        for (int frame = 0; frame < 43; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 68, 64, () -> 0));
            assertTrue(runtime.gelClingingToLink());
            if (frame == 2) {
                assertEquals(67, runtime.inspection().snapshot().slots().get(0).x());
                assertEquals(62, runtime.inspection().snapshot().slots().get(0).y());
            }
        }
        runtime.tick(EntityTestFrames.frame(runtime, 43, 68, 64, () -> 0));
        assertFalse(runtime.gelClingingToLink());
    }

    @Test
    void gelReleasesClingingStateAfterRomJoypadCountdownDecrementsThreeTimesPerFrame() {
        EntitySpriteDefinition definition = pairDefinition(0x1C, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1C)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        assertEquals(
                1,
                runtime.resolveCombat(new EntitySwordContactFrame(1, 68, 64, false, true, false, 0, 0, 0, 0))
                        .size());
        assertEquals(4, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
        assertEquals(0x80, new EntityEnemyInspectionQueries(runtime.enemies()).zolTransitionCountdown(0));

        runtime.configuration().setJoypadHeld(true);
        for (int frame = 0; frame < 42; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 68, 64, sequence(0x00)));
        }
        assertEquals(4, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
        assertEquals(0x02, new EntityEnemyInspectionQueries(runtime.enemies()).zolTransitionCountdown(0));

        // The third decrement reaches zero at the end of this clinging
        // handler invocation; the state-4 release runs on the next frame.
        runtime.tick(EntityTestFrames.frame(runtime, 42, 68, 64, sequence(0x00)));
        assertEquals(4, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).zolTransitionCountdown(0));

        runtime.tick(EntityTestFrames.frame(runtime, 43, 68, 64, sequence(0x00)));
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(0));
    }

    @Test
    void hidingZolUsesTheRomProximityRevealAndRisePhases() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition definition = catalog.forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
        assertTrue(runtime.resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, true, 72, 1, 72, 1))
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
        List<EntityCombatEvent> contact = runtime.resolveCombat(
                new EntitySwordContactFrame(1, hidingZol.x() + 4, hidingZol.y() + 4, false, true, false, 0, 0, 0, 0));
        assertTrue(contact.isEmpty());

        List<EntityCombatEvent> sword = runtime.resolveCombat(new EntitySwordContactFrame(
                1, 120, 120, false, true, true, hidingZol.x() + 8, 1, hidingZol.y() + 8, 1));
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void hidingZolUsesTheBankSevenRecoilStepBeforeItsHandler() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
                runtime.consumePendingEntityEvents());
    }
}
