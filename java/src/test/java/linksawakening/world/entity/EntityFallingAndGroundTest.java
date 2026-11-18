package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshotAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityFallingAndGroundTest {

    @Test
    void ledgeCollisionStateUsesRomResetBytesAndClearsWithTheEntity() {
        RoomEntity initial = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(0x40, 0x40)
                .spriteDefinition(pairDefinition(0x09, 2))
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(initial)).build();

        assertEquals(0xFF, runtime.commands().thrownDirection(0));
        assertEquals(0x00, runtime.enemies().ledgeTransitionTimer(0));

        EntityRuntimeFixture.debug(runtime).setThrownDirectionForTest(0, 0x02);
        EntityRuntimeFixture.debug(runtime).setLedgeTransitionTimerForTest(0, 0x07);
        assertEquals(0x02, runtime.commands().thrownDirection(0));
        assertEquals(0x07, runtime.enemies().ledgeTransitionTimer(0));

        runtime.lifecycle().clearEntity(0);
        assertEquals(0xFF, runtime.commands().thrownDirection(0));
        assertEquals(0x00, runtime.enemies().ledgeTransitionTimer(0));
    }

    @Test
    void sideScrollDropUsesDownCollisionToAlignAndStopWeakLanding() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        RoomEntity source = new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(0x50, 0x60)
                .status(EntityStatus.DYING)
                .spriteDefinition(catalog.forEntityType(0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1))
                .z(0x09)
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(
                        snapshotAt(source).withSideScrolling(true))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        runtime.configuration()
                .setEnemyDropResolver(new EnemyDropResolver(linksawakening.world.entity.EntityRomTestData.view(rom)));
        EntityRuntimeFixture.debug(runtime).setDroppedItemForTest(15, 0x2D);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        RoomEntityBackgroundCollision collision =
                (entity, direction, nextX, nextY) -> direction == EntityBackgroundCollisionResult.DOWN;
        for (int frame = 1; frame <= 11; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0, collision));
        }

        RoomEntity drop = runtime.inspection().snapshot().slots().get(14);
        assertEquals(0x55, drop.y());
        assertEquals(0x00, new EntityRoomInspectionQueries(runtime.inspection()).dropSpeedY(14));
    }

    @Test
    void octorokUsesTheRomLongFallingCountdown() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x02);
        runtime.configuration()
                .setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
                        RoomEntityGroundInteraction.Result.pit(entity, 0x01, 0x68, 0x70));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        assertEquals(
                EntityStatus.FALLING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x6F, runtime.enemies().transitionCountdown(0));
        assertEquals(0x01, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }

    @Test
    void longFallingMoblinSwordAppliesAlertFacingBeforePresentationHandoff() {
        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>();
        for (int variant = 0; variant < 8; variant++) {
            variants.add(List.of(new EntitySpriteDefinition.DynamicSprite(
                    0,
                    0,
                    new EntitySpriteDefinition.OamAttribute(0x60, 0x03),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                    true)));
        }
        EntitySpriteDefinition definition = EntitySpriteDefinition.dynamic(0x14, 0x07, 0x7A95, 0, variants);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x14)
                .at(0x50, 0x50)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .spriteVariant(6)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x04);
        runtime.configuration()
                .setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
                        RoomEntityGroundInteraction.Result.pit(entity, 0x01, 0x68, 0x70));
        AtomicInteger backgroundProbes = new AtomicInteger();
        runtime.configuration().setBackgroundInteraction((entity, direction, nextX, nextY) -> {
            backgroundProbes.incrementAndGet();
            return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
        });

        for (int frame = 0; frame <= 4; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }

        RoomEntity falling = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.FALLING, falling.status());
        assertEquals(0x6C, runtime.enemies().transitionCountdown(0));
        // The active Moblin Sword handler runs before the pit transition. Its
        // ignore-hits path enters state 2 and faces Link (up on this tie), so
        // EntityFallHandler's bank-$03 presentation updates use base variant
        // 2 and cross bit 3 on the final falling frame.
        assertEquals(3, falling.spriteVariant());
        assertEquals(0x50, falling.x());
        assertEquals(0x50, falling.y());
        assertEquals(0, backgroundProbes.get());
    }

    @Test
    void activeEntitiesPassThroughTheGroundInteractionBoundaryAfterMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x4D, 1);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x4D)
                .at(32, 48)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        AtomicInteger calls = new AtomicInteger();
        runtime.configuration().setGroundInteraction((entity, frameCounter, previousStatus, speedZ, sideScrolling) -> {
            calls.incrementAndGet();
            RoomEntity updated = new RoomEntityBuilder()
                    .slot(entity.slot())
                    .sourceLoadOrder(entity.sourceLoadOrder())
                    .type(entity.type())
                    .at(entity.x() + 1, entity.y())
                    .status(entity.status())
                    .spriteDefinition(entity.spriteDefinition())
                    .spriteVariant(entity.spriteVariant())
                    .entityFlipAttribute(entity.entityFlipAttribute())
                    .spriteTileOffset(entity.spriteTileOffset())
                    .z(entity.z())
                    .build();
            return RoomEntityGroundInteraction.Result.unchanged(updated, 0x02);
        });

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));

        assertEquals(2, calls.get());
        assertEquals(34, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x02, runtime.inspection().groundStatus(0));
    }

    @Test
    void groundResultQueuesSplashAndUnloadsTheSlotInSourceOrder() {
        EntitySpriteDefinition definition = pairDefinition(0x4D, 1);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x4D)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        runtime.configuration()
                .setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
                        RoomEntityGroundInteraction.Result.unloaded(entity, 0x02, true));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, runtime.inspection().groundStatus(0));
        assertEquals(
                List.of(new TransientVfxRequest(TransientVfxType.WATER_SPLASH, 0x40, 0x50)),
                runtime.enemies().transientVfxRequests());
        assertEquals(
                List.of(new EntityCombatEvent(0, 0x4D, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x0E)),
                runtime.outbox().consumePendingEntityEvents());
    }

    @Test
    void pitResultStartsRomFallingStateWithAlignedTargetAndCountdown() {
        EntitySpriteDefinition definition = pairDefinition(0x4D, 4);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x4D)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x02);
        EntityRuntimeFixture.debug(runtime).setEnemyFlashCountdownForTest(0, 0x08);
        runtime.configuration()
                .setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
                        RoomEntityGroundInteraction.Result.pit(entity, 0x01, 0x68, 0x70));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        RoomEntity falling = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.FALLING, falling.status());
        assertEquals(0x48, runtime.enemies().transitionCountdown(0));
        assertEquals(0x01, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(0x00, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
        assertEquals(0x68, runtime.enemies().fallingTargetX(0));
        assertEquals(0x70, new EntityEnemyInspectionQueries(runtime.enemies()).fallingTargetY(0));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }

    @Test
    void colorDungeonShellFallingDispatchReturnsToItsRomStateSix() {
        EntitySpriteDefinition definition = EntitySpriteDefinition.unsupported(0xE9);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xE9)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .spriteVariant(-1)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial)
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(null)
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0xFF);
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x02);
        runtime.configuration()
                .setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
                        RoomEntityGroundInteraction.Result.pit(entity, 0x01, 0x68, 0x70));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertEquals(
                EntityStatus.FALLING,
                runtime.inspection().snapshot().slots().get(0).status());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));

        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x06, runtime.pickupState().colorShellState(0));
    }

    @Test
    void fallingHandlerUsesRomVectorPhaseJingleAndUnloadBoundary() {
        EntitySpriteDefinition definition = pairDefinition(0x4D, 4);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x4D)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x02);
        runtime.configuration()
                .setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
                        RoomEntityGroundInteraction.Result.pit(entity, 0x01, 0x68, 0x70));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        List<EntityCombatEvent> fallingEvents = new ArrayList<>();
        for (int frame = 1; frame <= 16; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
            fallingEvents.addAll(runtime.outbox().consumePendingEntityEvents());
        }

        RoomEntity moving = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.FALLING, moving.status());
        assertEquals(0x43, moving.x());
        assertEquals(0x52, moving.y());
        assertEquals(0x03, moving.spriteVariant());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).fallingVisualYOffset(0));
        assertEquals(
                List.of(new EntityCombatEvent(0, 0x4D, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x18)),
                fallingEvents);

        for (int frame = 17; frame <= 25; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }
        assertEquals(4, new EntityEnemyInspectionQueries(runtime.enemies()).fallingVisualYOffset(0));
        assertEquals(4, runtime.inspection().snapshot().visualYOffset(0));
        for (int frame = 26; frame <= 72; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void fallingHandlerUsesItsDedicatedRomDisplayListForTwoVariantEnemies() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(loadRom()));
        EntitySpriteDefinition hardhat = catalog.forEntityType(0x20, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x20)
                .at(0x40, 0x50)
                .status(EntityStatus.FALLING)
                .spriteDefinition(hardhat)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial)
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 0x31);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        RoomEntity falling = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.FALLING, falling.status());
        assertEquals(3, falling.spriteVariant());
        assertEquals(4, falling.spriteDefinition().variantCount());
        assertEquals(2, falling.spriteDefinition().dynamicVariant(3).size());
        assertEquals(
                0x1E, falling.spriteDefinition().dynamicVariant(3).get(0).oam().tile());
    }

    @Test
    void longFallingHandlerRunsOnlyItsRomPresentationHandoff() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(0x40, 0x50)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x04);
        runtime.configuration()
                .setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
                        RoomEntityGroundInteraction.Result.pit(entity, 0x01, 0x68, 0x70));
        AtomicInteger backgroundProbes = new AtomicInteger();
        runtime.configuration().setBackgroundInteraction((entity, direction, nextX, nextY) -> {
            backgroundProbes.incrementAndGet();
            return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
        });

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0, 0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 3, 0, 0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 4, 0, 0, () -> 0));

        RoomEntity falling = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.FALLING, falling.status());
        assertEquals(0x6C, runtime.enemies().transitionCountdown(0));
        assertEquals(7, falling.spriteVariant());
        assertEquals(0x40, falling.x());
        assertEquals(0x50, falling.y());
        assertEquals(0, backgroundProbes.get());
    }
}
