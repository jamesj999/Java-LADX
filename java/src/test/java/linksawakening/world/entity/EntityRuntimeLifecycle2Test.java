package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeLifecycle2Test extends EntityRuntimeTestSupport {

    @Test
    void simplePairEnemiesUseTheirRomAnimationCadences() {
        RoomEntitySnapshot initial = snapshot(
                new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x0F)
                        .at(24, 32)
                        .spriteDefinition(pairDefinition(0x0F, 2))
                        .build(),
                new RoomEntityBuilder()
                        .slot(1)
                        .sourceLoadOrder(1)
                        .type(0x12)
                        .at(40, 32)
                        .spriteDefinition(pairDefinition(0x12, 2))
                        .build(),
                new RoomEntityBuilder()
                        .slot(2)
                        .sourceLoadOrder(2)
                        .type(0x20)
                        .at(56, 32)
                        .spriteDefinition(pairDefinition(0x20, 2))
                        .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(1, runtime.inspection().snapshot().slots().get(1).spriteVariant());
        assertEquals(0, runtime.inspection().snapshot().slots().get(2).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 8));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(1, runtime.inspection().snapshot().slots().get(1).spriteVariant());
        assertEquals(1, runtime.inspection().snapshot().slots().get(2).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 16));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0, runtime.inspection().snapshot().slots().get(1).spriteVariant());
        assertEquals(0, runtime.inspection().snapshot().slots().get(2).spriteVariant());
    }

    @Test
    void antiFairyUsesTheRomRandomSpeedVectorAndFrameAnimation() {
        EntitySpriteDefinition definition = pairDefinition(0x15, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x15)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        // EntityRandomSpeedX/Y[1] = (+12, -12). The init handler runs before
        // the first active handler pass.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x01)));
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).antiFairySpeedX(0));
        assertEquals(0xF4, new EntityEnemyInspectionQueries(runtime.enemies()).antiFairySpeedY(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(63, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x00)));
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(62, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 8, 120, 120, sequence(0x00)));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void starUsesTheRomRandomSpeedVectorAndFourFrameAnimation() {
        EntitySpriteDefinition definition = pairDefinition(0x9C, 5);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x9C)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        // EntityInitWithRandomSpeed[1] = (+12, -12). The Star handler keeps
        // the fifth ROM display-list variant available but selects variants
        // 0..3 from (hFrameCounter >> 3) & 3.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x01)));
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedX(0));
        assertEquals(0xF4, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedY(0));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(63, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x00)));
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(62, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 24, 120, 120, sequence(0x00)));
        assertEquals(3, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void inventoryTransitionMakesEntityHandlersNonInteractiveWithoutSkippingThePass() {
        EntitySpriteDefinition definition = pairDefinition(0x9C, 5);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9C)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.configuration().setTalkState(true, 0, 0x78);
        EnemyProjectileCollision.LinkState interactive =
                new EnemyProjectileCollision.LinkState(120, 120, 0, 0, 0, false);

        runtime.tick(EntityTestFrames.projectileFrame(runtime, 1, 120, 120, sequence(0x00), null, interactive));
        runtime.tick(EntityTestFrames.projectileFrame(runtime, 2, 120, 120, sequence(0x00), null, interactive));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedX(0));
        assertEquals(0x0C, new EntityEnemyInspectionQueries(runtime.enemies()).starSpeedY(0));
    }

    @Test
    void spikeTrapUsesTheRomRandomDirectionAndFourStateLaunchLoop() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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
        assertEquals(64, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapPrivateState1(0));
        assertEquals(64, new EntityEnemyInspectionQueries(runtime.enemies()).spikeTrapPrivateState2(0));

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
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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

        List<EntityCombatEvent> launchEvents = runtime.consumePendingEntityEvents();
        assertEquals(1, launchEvents.size());
        assertEquals(0, launchEvents.get(0).slot());
        assertEquals(0x27, launchEvents.get(0).type());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE, launchEvents.get(0).soundChannel());
        assertEquals(0x0A, launchEvents.get(0).soundId());

        runtime.tick(EntityTestFrames.frame(runtime, 3, 80, 64, sequence(0x00)));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void spikeTrapBlockedLaunchDoesNotPublishTheWhooshNoise() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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

        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void spikeTrapFallsBackToTheRomVerticalLaunchTable() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
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

    @Test
    void enemyProjectileWallTransitionUnloadsAndClearsTheReverseSlot() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()));
        EntitySpriteDefinition definition = catalog.forEntityType(0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < 15; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        slots.add(new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(new RoomEntitySnapshot(slots))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();
        RoomEntityBackgroundCollision rightWall = (entity, direction, nextX, nextY) -> direction == 0;

        for (int frame = 0; frame <= 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x70, 0x50, () -> 0));
        }
        runtime.tick(EntityTestFrames.frame(runtime, 7, 0x70, 0x50, () -> 0, rightWall));
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyProjectileTransitionCountdown(14));

        for (int frame = 8; frame <= 30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x70, 0x50, () -> 0, rightWall));
        }

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(14).status());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyProjectileSpeedX(14));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyProjectileTransitionCountdown(14));
    }

    @Test
    void fullEntityTableLeavesTheLaunchSourceAndProjectileStateUnchanged() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()));
        EntitySpriteDefinition moblinDefinition = catalog.forEntityType(0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        List<RoomEntity> entities = new ArrayList<>();
        for (int slot = 0; slot < 15; slot++) {
            entities.add(new RoomEntityBuilder()
                    .slot(slot)
                    .type(0x01)
                    .at(0x20, 0x20)
                    .spriteDefinition(EntitySpriteDefinition.unsupported(0x01))
                    .spriteVariant(-1)
                    .build());
        }
        entities.add(new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(0x40, 0x50)
                .spriteDefinition(moblinDefinition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(new RoomEntitySnapshot(entities))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();

        for (int frame = 0; frame <= 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x70, 0x50, () -> 0));
        }

        assertEquals(0x0B, runtime.inspection().snapshot().slots().get(15).type());
        assertEquals(0x0A, new EntityEnemyInspectionQueries(runtime.enemies()).octorokTransitionCountdown(15));
        for (int slot = 0; slot < 15; slot++) {
            assertNotEquals(
                    0x0C, runtime.inspection().snapshot().slots().get(slot).type());
        }
    }

    @Test
    void ballPlayingChildrenPushLinkAtGroundPositionEvenWhileHopping() {
        for (int type : new int[] {0x71, 0x72}) {
            RoomEntity kid = new RoomEntityBuilder()
                    .sourceLoadOrder(0)
                    .type(type)
                    .at(0x50, 0x60)
                    .spriteDefinition(pairDefinition(type, 4))
                    .z(8)
                    .build();
            RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(kid))
                    .indoorRoom(false)
                    .randomByteSupplier(() -> 0)
                    .build();
            runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x60, () -> 0));
            assertEquals(
                    List.of(new LinkFinalPositionRequest(0, true, true, false)),
                    runtime.consumePendingLinkFinalPositionRequests());
        }
    }

    @Test
    void onlyTheFirstEightLoadOrdersContributeToThePersistentClearMask() {
        EntitySpriteDefinition definition = pairDefinition(0x33, 2);
        RoomEntitySnapshot initial = snapshot(
                new RoomEntityBuilder()
                        .sourceLoadOrder(3)
                        .type(0x33)
                        .at(24, 32)
                        .spriteDefinition(definition)
                        .build(),
                new RoomEntityBuilder()
                        .slot(1)
                        .sourceLoadOrder(8)
                        .type(0x33)
                        .at(40, 32)
                        .spriteDefinition(definition)
                        .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        assertEquals(1 << 3, runtime.clearEntity(0));
        assertEquals(0, runtime.clearEntity(1));
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(1).status());
    }

    @Test
    void collectionUsesTheRomPickableTableAndFrameSlotCadence() {
        EntitySpriteDefinition definition = pairDefinition(0x2D, 1);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x2D)
                .at(24, 32)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0));

        assertNull(runtime.collectIfNeeded(0, 24, 34, false, true));
        EntityPickupEvent pickup = runtime.collectIfNeeded(1, 24, 34, false, true);

        assertNotNull(pickup);
        assertEquals(0, pickup.slot());
        assertEquals(0x2D, pickup.type());
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void floatingItemsAnimateRomZOnTheFirstAndSubsequentActiveFramesWithoutMovingXY() {
        EntitySpriteDefinition definition = pairDefinition(0x86, 7);
        RoomEntity initialEntity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x86)
                .at(24, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .z(0x13)
                .build();
        RoomEntityRuntime runtime =
                RoomEntityRuntime.builder(snapshot(initialEntity)).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0));
        RoomEntity first = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.ACTIVE, first.status());
        assertEquals(0x0F, first.z());
        assertEquals(24, first.x());
        assertEquals(64, first.y());

        runtime.tick(EntityTestFrames.frame(runtime, 24));
        assertEquals(0x11, runtime.inspection().snapshot().slots().get(0).z());
    }

    @Test
    void floatingCollectionUsesVisualYPersistenceAndSourceVariantData() {
        RoomEntity entity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x86)
                .at(24, 64)
                .spriteDefinition(pairDefinition(0x86, 7))
                .z(0x13)
                .build();
        RoomEntityRuntime belowLinkZ =
                RoomEntityRuntime.builder(snapshot(entity)).build();
        assertNull(belowLinkZ.collectIfNeeded(1, 24, 47, true, true, 0, 0x0B));

        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(entity)).build();
        EntityPickupEvent pickup = runtime.collectIfNeeded(1, 24, 47, true, true, 0, 0x0C);

        assertNotNull(pickup);
        assertEquals(0x86, pickup.type());
        assertEquals(0, pickup.sourceVariant());
        assertEquals(1, pickup.persistentClearMask());
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void floatingCollectionAllowsAirborneLinkInSideScrollingRoomsWithoutZGate() {
        RoomEntity entity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xE5)
                .at(24, 64)
                .spriteDefinition(pairDefinition(0xE5, 7))
                .spriteVariant(5)
                .z(0x13)
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(entity)).build();
        runtime.configuration().setGroundInteractionSideScrolling(true);

        EntityPickupEvent pickup = runtime.collectIfNeeded(0, 24, 47, true, true, 0, 0x00);

        assertNotNull(pickup);
        assertEquals(0xE5, pickup.type());
        assertEquals(5, pickup.sourceVariant());
    }

    @Test
    void collectionSkipsAirborneAndNonInteractiveLinkStates() {
        EntitySpriteDefinition definition = pairDefinition(0x2D, 1);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x2D)
                .at(24, 32)
                .spriteDefinition(definition)
                .build());

        assertNull(RoomEntityRuntime.builder(initial).build().collectIfNeeded(1, 24, 34, true, true));
        assertNull(RoomEntityRuntime.builder(initial).build().collectIfNeeded(1, 24, 34, false, false));
    }

    @Test
    void outdoorHeartUsesTheRomObjectUnderEntityRevealPath() {
        EntitySpriteDefinition definition = pairDefinition(0x2D, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2D)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0x04, 0, 0x40, 0x50));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x30, () -> 0));
        assertEquals(0x02, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState3(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x20, 0x30, () -> 0));

        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState3(0));
        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState4(0));
        assertEquals(0x0A, runtime.options1(0));
        assertEquals(0x18, new EntityRoomInspectionQueries(runtime.inspection()).dropPrivateCountdown1(0));
        assertEquals(0x0C, runtime.inspection().dropSpeedX(0));
        assertEquals(0x0C, new EntityRoomInspectionQueries(runtime.inspection()).dropSpeedY(0));
        assertEquals(0x20, runtime.inspection().dropSpeedZ(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).z());
    }

    @Test
    void outdoorFairyUsesTheSharedRevealVectorBeforeItsHoverHandler() {
        EntitySpriteDefinition definition = pairDefinition(0x2F, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x2F)
                        .at(0x40, 0x50)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x30, () -> 0));
        assertEquals(0x01, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState3(0));

        EntityRuntimeFixture.debug(runtime).setDroppablePegasusCollisionForTest(true, true, 0x48, 0x58);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x20, 0x30, () -> 0));

        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).droppablePrivateState3(0));
        assertEquals(0x0A, runtime.options1(0));
        // $629C returns past the fairy handler on the reveal frame too.
        assertEquals(0x0C, new EntityRoomInspectionQueries(runtime.inspection()).fairySpeedX(0));
        assertEquals(0x0C, new EntityRoomInspectionQueries(runtime.inspection()).fairySpeedY(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void terminalStaticDeathRecordsKillAndDynamicDeathDoesNot() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EnemyDropResolver resolver = new EnemyDropResolver(linksawakening.world.entity.EntityRomTestData.view(rom));

        RoomEntityRuntime staticRuntime = RoomEntityRuntime.builder(snapshotAt(dyingEntity(0, 0, 0x09, catalog)))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        staticRuntime.configuration().setEnemyDropResolver(resolver);
        EntityRuntimeFixture.debug(staticRuntime).setDroppedItemForTest(0, EnemyDropResolver.ENTITY_NONE);
        staticRuntime.tick(EntityTestFrames.frame(staticRuntime, 0, 0, 0, () -> 0));

        assertEquals(
                EntityStatus.DISABLED,
                staticRuntime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, new EntityRoomInspectionQueries(staticRuntime.inspection()).killCount());
        assertEquals(0, staticRuntime.inspection().killOrderAt(0));
        assertEquals(1, staticRuntime.consumePendingClearedEntityMask());

        RoomEntityRuntime dynamicRuntime = RoomEntityRuntime.builder(snapshotAt(dyingEntity(0, -1, 0x09, catalog)))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        dynamicRuntime.configuration().setEnemyDropResolver(resolver);
        EntityRuntimeFixture.debug(dynamicRuntime).setDroppedItemForTest(0, EnemyDropResolver.ENTITY_NONE);
        dynamicRuntime.tick(EntityTestFrames.frame(dynamicRuntime, 0, 0, 0, () -> 0));

        assertEquals(
                EntityStatus.DISABLED,
                dynamicRuntime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, new EntityRoomInspectionQueries(dynamicRuntime.inspection()).killCount());
        assertEquals(
                new EnemyDropResolver.CounterState(0, 0),
                dynamicRuntime.inspection().enemyDropCounters());
        assertEquals(0, dynamicRuntime.consumePendingClearedEntityMask());
    }

    @Test
    void explicitAndRandomNoDropDeathsUnloadWithExpectedCounterAndRandomBehavior() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(dyingEntity(0, 0, 0x09, catalog), dyingEntity(1, 1, 0x0B, catalog)))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        runtime.configuration()
                .setEnemyDropResolver(new EnemyDropResolver(linksawakening.world.entity.EntityRomTestData.view(rom)));
        EntityRuntimeFixture.debug(runtime).setDroppedItemForTest(0, EnemyDropResolver.ENTITY_NONE);
        EntityRuntimeFixture.debug(runtime).setDroppedItemForTest(1, 0);
        AtomicInteger randomReads = new AtomicInteger();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> {
            randomReads.incrementAndGet();
            return 0x01;
        }));

        assertFalse(runtime.inspection().snapshot().slots().get(0).loaded());
        assertFalse(runtime.inspection().snapshot().slots().get(1).loaded());
        assertEquals(2, new EntityRoomInspectionQueries(runtime.inspection()).killCount());
        assertEquals(1, runtime.inspection().killOrderAt(0));
        assertEquals(0, runtime.inspection().killOrderAt(1));
        assertEquals(
                new EnemyDropResolver.CounterState(1, 1), runtime.inspection().enemyDropCounters());
        assertEquals(1, randomReads.get());
        assertEquals(0b11, runtime.consumePendingClearedEntityMask());
    }
}
