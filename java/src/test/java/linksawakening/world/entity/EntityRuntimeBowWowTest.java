package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeBowWowTest extends EntityRuntimeTestSupport {

    @Test
    void dynamicBowWowRunsItsRomSetupThenFollowerSpeedPhase() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
                .forFollowerEntityType(FollowingNpcEntitySpawner.ENTITY_BOW_WOW);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .type(FollowingNpcEntitySpawner.ENTITY_BOW_WOW)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial)
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setFollowingNpcState(new FollowingNpcState(false, 0, false, true, 0, 0, false));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x60, 0x70, () -> 0));
        assertEquals(0x44, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x58, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x60, 0x70, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x60, 0x70, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 3, 0x60, 0x70, () -> 0));

        RoomEntity bowWow = runtime.inspection().snapshot().slots().get(0);
        assertEquals(0x44, bowWow.x());
        assertEquals(0x57, bowWow.y());
    }

    @Test
    void touchingKidnappedBowWowInTheHideoutStartsFollowingAndItemMusic() {
        RoomEntity bowWow = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x6D)
                .at(0x88, 0x40)
                .spriteDefinition(pairDefinition(0x6D, 7))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(bowWow))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setBowWowState(0x80);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x88, 0x40, 1, () -> 0, null, null, 0, 0, 0));

        assertEquals(1, runtime.inspection().bowWowState());
        assertEquals(0x10, runtime.consumePendingMusicTrack());
        assertEquals(List.of(new DialogRequest(1, 0x6C)), runtime.consumePendingDialogRequests());
    }

    @Test
    void rescuedSourceBowWowImmediatelyUsesTheFollowingSpriteTable() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity bowWow = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x6D)
                .at(0x88, 0x40)
                .spriteDefinition(catalog.forEntityType(0x6D, EntityRoomLoader.RoomTable.INDOORS_B))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(bowWow))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        runtime.configuration().setBowWowState(0x80);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x88, 0x40, 1, () -> 0, null, null, 0, 0, 0));

        RoomEntity rescued = runtime.inspection().snapshot().slots().get(0);
        assertEquals(0x05, rescued.spriteDefinition().bank());
        assertEquals(0x401C, rescued.spriteDefinition().address());
    }

    @Test
    void kidnappedBowWowUsesFollowingTilesBeforeRescueEvenDuringDialog() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity bowWow = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x6D)
                .at(0x88, 0x40)
                .spriteDefinition(catalog.forEntityType(0x6D, EntityRoomLoader.RoomTable.INDOORS_B))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(bowWow))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        runtime.configuration().setBowWowState(0x80);
        runtime.configuration().setDialogActive(true);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x40, () -> 0));
        assertEquals(0x80, runtime.inspection().bowWowState());
        assertEquals(
                0x401C,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(0)
                        .spriteDefinition()
                        .address());
        assertTrue(runtime.consumePendingDialogRequests().isEmpty());

        runtime.configuration().setBowWowState(0);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x20, 0x40, () -> 0));
        assertEquals(
                0x4000,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(0)
                        .spriteDefinition()
                        .address());
    }

    @Test
    void kidnappedBowWowWarningStartsAutomaticallyAndDoesNotRepeatWhileIdle() {
        RoomEntity kid = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x71)
                .at(0x40, 0x40)
                .spriteDefinition(pairDefinition(0x71, 4))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(kid))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setBowWowState(0x80);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x40, 0x50, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x40, 0x50, () -> 0));
        assertEquals(1, runtime.consumePendingDialogRequests().size());
        for (int frame = 2; frame < 64; frame++)
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x40, 0x50, () -> 0));
        assertTrue(runtime.consumePendingDialogRequests().isEmpty());
    }

    @Test
    void kidnappedBowWowDoesNotReleaseFromAnUnrelatedRoomCollision() {
        RoomEntity bowWow = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x6D)
                .at(0x88, 0x40)
                .spriteDefinition(pairDefinition(0x6D, 7))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(bowWow))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setBowWowState(0x80);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x20, () -> 0, true));

        assertEquals(0x80, runtime.inspection().bowWowState());
        assertEquals(-1, runtime.consumePendingMusicTrack());
        assertTrue(runtime.consumePendingDialogRequests().isEmpty());
    }

    @Test
    void kidnappedBowWowDoesNotReleaseWithoutTheSourceCollisionByte() {
        RoomEntity bowWow = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x6D)
                .at(0x88, 0x40)
                .spriteDefinition(pairDefinition(0x6D, 7))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(bowWow))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();
        runtime.configuration().setBowWowState(0x80);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x88, 0x40, 0, () -> 0, null, null, 0, 0, 0));

        assertEquals(0x80, runtime.inspection().bowWowState());
        assertEquals(-1, runtime.consumePendingMusicTrack());
        assertTrue(runtime.consumePendingDialogRequests().isEmpty());
    }

    @Test
    void dynamicBowWowUsesTheRomEatableTableDuringTheLiveEntityTick() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition bowWowDefinition =
                catalog.forFollowerEntityType(FollowingNpcEntitySpawner.ENTITY_BOW_WOW);
        EntitySpriteDefinition targetDefinition = catalog.forEntityType(0x09, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(
                        new RoomEntityBuilder()
                                .slot(5)
                                .type(FollowingNpcEntitySpawner.ENTITY_BOW_WOW)
                                .at(0x20, 0x20)
                                .spriteDefinition(bowWowDefinition)
                                .build(),
                        new RoomEntityBuilder()
                                .slot(10)
                                .sourceLoadOrder(0)
                                .type(0x09)
                                .at(0x54, 0x68)
                                .spriteDefinition(targetDefinition)
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        runtime.configuration().setFollowingNpcState(new FollowingNpcState(false, 0, false, true, 0, 0, false));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x60, 0x70, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x60, 0x70, () -> 0));

        assertEquals(10, runtime.inspection().bowWowTargetSlot(5));
    }

    @Test
    void bowWowConsumesAnOrdinaryTargetThroughDidKillEnemy() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(
                        new RoomEntityBuilder()
                                .slot(5)
                                .type(FollowingNpcEntitySpawner.ENTITY_BOW_WOW)
                                .at(0x40, 0x50)
                                .spriteDefinition(
                                        catalog.forFollowerEntityType(FollowingNpcEntitySpawner.ENTITY_BOW_WOW))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(6)
                                .sourceLoadOrder(0)
                                .type(0x09)
                                .at(0x44, 0x58)
                                .spriteDefinition(catalog.forEntityType(0x09, EntityRoomLoader.RoomTable.OVERWORLD))
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        runtime.configuration().setFollowingNpcState(new FollowingNpcState(false, 0, false, true, 0, 0, false));
        runtime.configuration().setEnemyDropResolver(new EnemyDropResolver(rom));
        EntityRuntimeFixture.debug(runtime).setDroppedItemForTest(6, EnemyDropResolver.ENTITY_NONE);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x44, 0x58, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x44, 0x58, () -> 0));
        EntityRuntimeFixture.debug(runtime).setBowWowTransitionCountdownForTest(5, 1);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x44, 0x58, () -> 0));

        assertFalse(runtime.inspection().snapshot().slots().get(6).loaded());
        assertEquals(1, runtime.inspection().killCount());
        assertEquals(1, runtime.consumePendingClearedEntityMask());
        assertEquals(
                List.of(new EntityCombatEvent(
                        5,
                        FollowingNpcEntitySpawner.ENTITY_BOW_WOW,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x03)),
                runtime.consumePendingEntityEvents());
    }

    @Test
    void bowWowFlashesKikiWithoutConsumingIt() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(
                        new RoomEntityBuilder()
                                .slot(5)
                                .type(FollowingNpcEntitySpawner.ENTITY_BOW_WOW)
                                .at(0x40, 0x50)
                                .spriteDefinition(
                                        catalog.forFollowerEntityType(FollowingNpcEntitySpawner.ENTITY_BOW_WOW))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(6)
                                .sourceLoadOrder(0)
                                .type(0xAD)
                                .at(0x44, 0x58)
                                .spriteDefinition(catalog.forEntityType(0xAD, EntityRoomLoader.RoomTable.OVERWORLD))
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        runtime.configuration().setFollowingNpcState(new FollowingNpcState(false, 0, false, true, 0, 0, false));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x44, 0x58, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x44, 0x58, () -> 0));
        EntityRuntimeFixture.debug(runtime).setBowWowTransitionCountdownForTest(5, 1);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x44, 0x58, () -> 0));

        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(6).status());
        assertEquals(0x18, runtime.enemies().enemyFlashCountdown(6));
        assertEquals(1, runtime.inspection().entityInertia(6));
        assertEquals(
                List.of(new EntityCombatEvent(
                        5,
                        FollowingNpcEntitySpawner.ENTITY_BOW_WOW,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x03)),
                runtime.consumePendingEntityEvents());
    }

    @Test
    void bowWowOpensTheSecretShellDialogOnlyForAnActiveShell() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(
                        new RoomEntityBuilder()
                                .slot(5)
                                .type(FollowingNpcEntitySpawner.ENTITY_BOW_WOW)
                                .at(0x40, 0x50)
                                .spriteDefinition(
                                        catalog.forFollowerEntityType(FollowingNpcEntitySpawner.ENTITY_BOW_WOW))
                                .build(),
                        new RoomEntityBuilder()
                                .slot(6)
                                .sourceLoadOrder(0)
                                .type(0x3D)
                                .at(0x44, 0x58)
                                .spriteDefinition(catalog.forEntityType(0x3D, EntityRoomLoader.RoomTable.OVERWORLD))
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        runtime.configuration().setFollowingNpcState(new FollowingNpcState(false, 0, false, true, 0, 0, false));
        EntityRuntimeFixture.debug(runtime).setEntityPrivateState4ForTest(6, 1);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x44, 0x58, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x44, 0x58, () -> 0));
        EntityRuntimeFixture.debug(runtime).setBowWowTransitionCountdownForTest(5, 1);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x44, 0x58, () -> 0));

        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(6).status());
        assertEquals(0, runtime.inspection().bowWowTransitionCountdown(5));
        assertEquals(0x80, runtime.inspection().bowWowPrivateCountdown2(5));
        assertEquals(List.of(new DialogRequest(1, 0x15)), runtime.consumePendingDialogRequests());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }
}
