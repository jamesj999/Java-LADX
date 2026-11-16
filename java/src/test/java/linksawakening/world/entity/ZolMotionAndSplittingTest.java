package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class ZolMotionAndSplittingTest {

    @Test
    void zolUsesTheRomInchingAndLeapStateLoop() {
        EntitySpriteDefinition definition = pairDefinition(0x1B, 2);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables combatTables =
                new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        assertEquals(1, combatTables.initialHealth(0x1B));
        EntitySpriteDefinition definition = catalog.forEntityType(0x1B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        List<EntityCombatEvent> sword = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 120, 120, false, true, true, 72, 1, 72, 1));
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
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
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
}
