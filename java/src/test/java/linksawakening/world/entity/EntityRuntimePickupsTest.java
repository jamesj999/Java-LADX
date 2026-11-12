package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.KeyQuicksandEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.room.ChestContentsTable;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimePickupsTest extends EntityRuntimeTestSupport {

    @Test
    void secretSeashellRemainsHiddenUntilItsRomObjectRevealConditionIsMet() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity shell = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x3D)
                .at(0x40, 0x50)
                .spriteDefinition(catalog.forEntityType(0x3D, EntityRoomLoader.RoomTable.OVERWORLD))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(shell))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(0x10);
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0x09, 0, 0x40, 0x40));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x30, () -> 0, null));

        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0x02, runtime.pickupState().secretSeashellPrivateState3(0));
        assertEquals(0x00, runtime.pickupState().secretSeashellPrivateState4(0));

        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0x04, 0, 0x40, 0x40));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x20, 0x30, () -> 0, null));

        RoomEntity revealed = runtime.inspection().snapshot().slots().get(0);
        assertEquals(0, revealed.spriteVariant());
        assertEquals(0, runtime.pickupState().secretSeashellPrivateState3(0));
        assertEquals(0, runtime.pickupState().secretSeashellPrivateState4(0));
        assertEquals(0x18, runtime.pickupState().secretSeashellPrivateCountdown1(0));
        assertEquals(0x80, runtime.pickupState().secretSeashellSlowTransitionCountdown(0));
        assertEquals(0x0C, runtime.pickupState().secretSeashellSpeedX(0));
        assertEquals(0x0C, runtime.pickupState().secretSeashellSpeedY(0));
        assertEquals(0x20, runtime.pickupState().secretSeashellSpeedZ(0));
    }

    @Test
    void secretSeashellExceptionsExposePrivateStateFourWithoutRevealing() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity shell = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x3D)
                .at(0x40, 0x50)
                .spriteDefinition(catalog.forEntityType(0x3D, EntityRoomLoader.RoomTable.OVERWORLD))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(shell))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(0xDA);
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0x09, 0, 0x40, 0x40));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x30, () -> 0, null));

        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0x02, runtime.pickupState().secretSeashellPrivateState3(0));
        assertEquals(0x01, runtime.pickupState().secretSeashellPrivateState4(0));
    }

    @Test
    void secretSeashellTreeRoomsUsePegasusCollisionInsteadOfGrassReveal() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity shell = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x3D)
                .at(0x40, 0x50)
                .spriteDefinition(catalog.forEntityType(0x3D, EntityRoomLoader.RoomTable.OVERWORLD))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(shell))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(0xA4);
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0x04, 0, 0x40, 0x40));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x30, () -> 0, null));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0x01, runtime.pickupState().secretSeashellPrivateState3(0));

        EntityRuntimeFixture.debug(runtime).setSecretSeashellPegasusCollisionForTest(true, true, 0x90, 0x90);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x20, 0x30, () -> 0, null));

        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0x01, runtime.pickupState().secretSeashellPrivateState3(0));

        EntityRuntimeFixture.debug(runtime).setSecretSeashellPegasusCollisionForTest(true, true, 0x48, 0x58);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x20, 0x30, () -> 0, null));

        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0, runtime.pickupState().secretSeashellPrivateState3(0));
    }

    @Test
    void secretSeashellHandlerUnloadsForSwordUpgradeOrCompletedRoomWithoutPersistence() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity shell = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x3D)
                .at(0x40, 0x50)
                .spriteDefinition(catalog.forEntityType(0x3D, EntityRoomLoader.RoomTable.OVERWORLD))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(shell))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(0x10);
        runtime.configuration().setChestPlayerLevels(1, 2, 1);
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0x04, 0, 0x40, 0x40));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x30, () -> 0, null));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, runtime.consumePendingClearedEntityMask());

        shell = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x3D)
                .at(0x40, 0x50)
                .spriteDefinition(catalog.forEntityType(0x3D, EntityRoomLoader.RoomTable.OVERWORLD))
                .build();
        runtime = RoomEntityRuntime.builder(snapshot(shell))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(0x10);
        EntityRuntimeFixture.debug(runtime).setEntityRoomStatusForTest(0x10);
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0x04, 0, 0x40, 0x40));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x30, () -> 0, null));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, runtime.consumePendingClearedEntityMask());
    }

    @Test
    void collectionUsesThePickupHitboxEdgesFromHitboxPositions() {
        EntitySpriteDefinition definition = pairDefinition(0x2D, 1);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x2D)
                .at(24, 32)
                .spriteDefinition(definition)
                .build());

        RoomEntityRuntime xInside = RoomEntityRuntime.builder(initial).build();
        assertNotNull(xInside.collectIfNeeded(1, 34, 34, false, true));

        RoomEntityRuntime xOutside = RoomEntityRuntime.builder(initial).build();
        assertNull(xOutside.collectIfNeeded(1, 35, 34, false, true));

        RoomEntityRuntime yInside = RoomEntityRuntime.builder(initial).build();
        assertNotNull(yInside.collectIfNeeded(1, 24, 43, false, true));

        RoomEntityRuntime yOutside = RoomEntityRuntime.builder(initial).build();
        assertNull(yOutside.collectIfNeeded(1, 24, 44, false, true));
    }

    @Test
    void keyDropPointNormalVariantImmediatelyAwardsASmallKeyAndClears() {
        EntitySpriteDefinition definition = keyDropDefinition();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x30)
                        .at(24, 32)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        assertEquals(0xB1, runtime.enemies().physicsFlags(0));
        assertEquals(0x0A, runtime.options1(0));

        EntityPickupEvent pickup = runtime.collectIfNeeded(1, 24, 34, false, true);

        assertNotNull(pickup);
        assertEquals(0x30, pickup.type());
        assertEquals(1, pickup.persistentClearMask());
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(
                List.of(new KeyRewardEvent(0, ChestContentsTable.CHEST_SMALL_KEY)),
                runtime.consumePendingKeyRewardEvents());
    }

    @Test
    void keyDropPointDungeonVariantStartsTheHeldRewardTransitionAndUsesRomDialog() {
        EntitySpriteDefinition definition = keyDropDefinition();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x30)
                        .at(24, 32)
                        .spriteDefinition(definition)
                        .spriteVariant(2)
                        .build()))
                .build();

        EntityPickupEvent pickup = runtime.collectIfNeeded(1, 24, 34, false, true);
        assertNotNull(pickup);
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x68, runtime.pickupState().keyDropTransitionCountdown(0));

        EntityRuntimeFixture.debug(runtime).setKeyDropTransitionCountdownForTest(0, 0x11);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x40, 0x50, () -> 0));

        RoomEntity held = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.ACTIVE, held.status());
        assertEquals(0x40, held.x());
        assertEquals(0x44, held.y());
        assertEquals(0x0F, runtime.pickupState().keyDropTransitionCountdown(0));
        assertEquals(List.of(new DialogRequest(0, 0xA3)), runtime.consumePendingDialogRequests());
        assertEquals(
                List.of(new KeyRewardEvent(0, ChestContentsTable.CHEST_ANGLER_KEY)),
                runtime.consumePendingKeyRewardEvents());
    }

    @Test
    void keyDropPointAtYarnaQuicksandCenterStartsTheRomFallingHandoff() {
        EntitySpriteDefinition definition = keyDropDefinition();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .type(0x30)
                        .at(0x50, 0x48)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(0xCE);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        RoomEntity falling = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.FALLING, falling.status());
        assertEquals(0x2F, runtime.enemies().transitionCountdown(0));
        assertEquals(0x50, runtime.enemies().fallingTargetX(0));
        assertEquals(0x48, new EntityEnemyInspectionQueries(runtime.enemies()).fallingTargetY(0));
        assertEquals(List.of(new KeyQuicksandEvent(0)), runtime.consumePendingKeyQuicksandEvents());
        assertEquals(
                List.of(new EntityCombatEvent(0, 0x30, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x18)),
                runtime.consumePendingEntityEvents());
    }

    @Test
    void indoorDroppablesRemainHiddenWhenTheirInitTimerExpires() {
        EntitySpriteDefinition definition = pairDefinition(0x37, 1);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x37)
                .at(24, 32)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime =
                RoomEntityRuntime.builder(initial).indoorRoom(true).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0));

        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x80, new EntityRoomInspectionQueries(runtime.inspection()).slowTransitionCountdown(0));
        assertEquals(0x02, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState3(0));
        assertEquals(0x11, runtime.options1(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 1; frame <= 0x200; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame));
        }
        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).slowTransitionCountdown(0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void outdoorDroppableStaysHiddenUntilTheRomPegasusCollisionRevealsIt() {
        EntitySpriteDefinition definition = pairDefinition(0x37, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x37)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x30, () -> 0));

        assertEquals(0x01, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState3(0));
        assertEquals(0x11, runtime.options1(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertNull(runtime.collectIfNeeded(1, 0x48, 0x58, false, true));

        EntityRuntimeFixture.debug(runtime).setDroppablePegasusCollisionForTest(true, true, 0x48, 0x58);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x20, 0x30, () -> 0));

        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState3(0));
        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState4(0));
        assertEquals(0x0A, runtime.options1(0));
        assertEquals(0x18, new EntityRoomInspectionQueries(runtime.inspection()).dropPrivateCountdown1(0));
        assertEquals(0x80, new EntityRoomInspectionQueries(runtime.inspection()).slowTransitionCountdown(0));
        assertEquals(0x0C, runtime.inspection().dropSpeedX(0));
        assertEquals(0x0C, new EntityRoomInspectionQueries(runtime.inspection()).dropSpeedY(0));
        assertEquals(0x20, runtime.inspection().dropSpeedZ(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        for (int frame = 2; frame <= 35; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x20, 0x30, () -> 0));
        }
        RoomEntity bounced = runtime.inspection().snapshot().slots().get(0);
        assertEquals(0x59, bounced.x());
        assertEquals(0x69, bounced.y());
        assertEquals(0, bounced.z());
        assertEquals(0x11, runtime.inspection().dropSpeedZ(0));
        assertEquals(6, runtime.inspection().dropSpeedX(0));
        assertEquals(6, new EntityRoomInspectionQueries(runtime.inspection()).dropSpeedY(0));
    }

    @Test
    void hidingSlimeKeyUsesTheBuriedRevealPathAndCannotBeCollectedFromGrass() {
        EntitySpriteDefinition definition = pairDefinition(0x3C, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x3C)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(0xC6);
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0x04, 0, 0x40, 0x50));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x30, () -> 0));

        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x02, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState3(0));
        assertEquals(0x1B, runtime.options1(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertNull(runtime.collectIfNeeded(1, 0x40, 0x50, false, true));

        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0xCC, 0, 0x40, 0x50));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x20, 0x30, () -> 0));

        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState3(0));
        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState4(0));
        assertEquals(0x0A, runtime.options1(0));
        assertEquals(0x18, new EntityRoomInspectionQueries(runtime.inspection()).dropPrivateCountdown1(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void hidingSlimeKeyRunsItsLeafRewardAtTransitionCountdownTen() {
        EntitySpriteDefinition definition = pairDefinition(0x3C, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x3C)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(0xC6);
        EntityRuntimeFixture.debug(runtime).setGoldenLeavesCountForTest(5);
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0xCC, 0, 0x40, 0x50));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x30, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x20, 0x30, () -> 0));
        EntityRuntimeFixture.debug(runtime).setHidingSlimeKeyTransitionCountdownForTest(0, 0x11);

        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x20, 0x30, () -> 0));

        RoomEntity held = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.ACTIVE, held.status());
        assertEquals(0x0F, runtime.enemies().transitionCountdown(0));
        assertEquals(0x20, held.x());
        assertEquals(0x24, held.y());
        assertEquals(List.of(new SlimeKeyRewardEvent(0, 6, 0xA2)), runtime.consumePendingSlimeKeyRewardEvents());
        assertEquals(List.of(new DialogRequest(0, 0xA2)), runtime.consumePendingDialogRequests());
    }

    @Test
    void completedRoomUnloadsAnInitializingHidingSlimeKeyWithoutPersistence() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x3C)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(pairDefinition(0x3C, 1))
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityRoomStatusForTest(0x10);

        runtime.tick(EntityTestFrames.frame(runtime, 0));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, runtime.consumePendingClearedEntityMask());
    }

    @Test
    void revealedDroppableUsesTheRomBlinkSentinelBeforeUnloading() {
        EntitySpriteDefinition definition = pairDefinition(0x2E, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2E)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setDroppableRevealedForTest(0, 0x1A);

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x20, 0x30, () -> 0));

        assertEquals(0x1A, new EntityRoomInspectionQueries(runtime.inspection()).slowTransitionCountdown(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void swordCollectionTableMatchesTheSeventeenPickableEntries() {
        int[] canBeCollected = {0x2D, 0x2E, 0x31, 0x33, 0x34, 0x37, 0x38, 0x3B};
        int[] cannotBeCollected = {0x2F, 0x30, 0x32, 0x35, 0x36, 0x39, 0x3A, 0x3C, 0x3D};

        for (int type : canBeCollected) {
            assertTrue(RoomEntityPickupRules.canBeCollectedBySword(type), "type 0x" + Integer.toHexString(type));
        }
        for (int type : cannotBeCollected) {
            assertFalse(RoomEntityPickupRules.canBeCollectedBySword(type), "type 0x" + Integer.toHexString(type));
        }
    }
}
