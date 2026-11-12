package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeLifecycle1Test extends EntityRuntimeTestSupport {

    @Test
    void ordinaryDungeonRoomEventStillDropsAKeyBelowMapCaveB() {
        RoomEntityRuntime runtime =
                RoomEntityRuntime.builder(snapshot()).indoorRoom(true).build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x05);

        int slot = runtime.spawnRoomEventKeyDrop();

        assertEquals(0x30, runtime.inspection().snapshot().slots().get(slot).type());
    }

    @Test
    void deathPresentationFieldsAreIndependentFromNormalSpriteVariant() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        RoomEntity entity = new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .status(EntityStatus.DYING)
                .spriteDefinition(body)
                .spriteVariant(1)
                .deathSpriteVariant(3)
                .powerRecoilDeath(true)
                .build();
        assertEquals(1, entity.spriteVariant());
        assertEquals(3, entity.deathSpriteVariant());
        assertTrue(entity.powerRecoilDeath());
    }

    @Test
    void rejectsDeathSpriteVariantsOutsideTheFourFrameRange() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        assertThrows(IllegalArgumentException.class, () -> new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .status(EntityStatus.DYING)
                .spriteDefinition(body)
                .spriteVariant(1)
                .deathSpriteVariant(-2)
                .build());
        assertThrows(IllegalArgumentException.class, () -> new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .status(EntityStatus.DYING)
                .spriteDefinition(body)
                .spriteVariant(1)
                .deathSpriteVariant(4)
                .build());
    }

    @Test
    void rejectsDeathSpriteVariantsForNonDyingEntities() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        assertThrows(IllegalArgumentException.class, () -> new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(body)
                .spriteVariant(1)
                .deathSpriteVariant(0)
                .build());
    }

    @Test
    void clearingThenReusingAProjectileSlotDoesNotRetainPowerDeathState() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        EntitySpriteDefinition octorok = catalog.forEntityType(0x09, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        EntitySpriteDefinition keese = catalog.forEntityType(0x19, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        List<RoomEntity> slots = new ArrayList<>();
        slots.add(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(octorok)
                .build());
        for (int slot = 1; slot < 15; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        slots.add(new linksawakening.world.entity.RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(1)
                .type(0x19)
                .at(120, 120)
                .spriteDefinition(keese)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(new RoomEntitySnapshot(slots))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        runtime.resolveCombat(
                0, 80, 64, false, true, true, 112, 16, 112, 16, new EnemyAttackContext(1, false, true, false, false));
        assertTrue(runtime.enemies().powerRecoilDeath(15));
        runtime.clearEntity(15);
        assertFalse(runtime.enemies().powerRecoilDeath(15));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, () -> 0, false));
        for (int frame = 1; frame <= 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, () -> 0, false));
        }

        RoomEntity projectile = runtime.inspection().snapshot().slots().get(15);
        assertEquals(EntityStatus.ACTIVE, projectile.status());
        assertEquals(0x0A, projectile.type());
        assertEquals(-1, projectile.deathSpriteVariant());
        assertFalse(projectile.powerRecoilDeath());
        assertFalse(runtime.enemies().powerRecoilDeath(15));
    }

    @Test
    void followsPieceOfPowerFrameDrivenPaletteVariant() {
        EntitySpriteDefinition definition = pairDefinition(0x33, 2);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x33)
                .at(24, 32)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0x00));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 0x08));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 0x10));
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void activeEntitiesPassThroughTheGroundInteractionBoundaryAfterMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x4D, 1);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x4D)
                .at(32, 48)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        AtomicInteger calls = new AtomicInteger();
        runtime.configuration().setGroundInteraction((entity, frameCounter, previousStatus, speedZ, sideScrolling) -> {
            calls.incrementAndGet();
            RoomEntity updated = new linksawakening.world.entity.RoomEntityBuilder()
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
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x4D)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
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
                runtime.consumePendingEntityEvents());
    }

    @Test
    void pitResultStartsRomFallingStateWithAlignedTargetAndCountdown() {
        EntitySpriteDefinition definition = pairDefinition(0x4D, 4);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x4D)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
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
        assertEquals(0x00, runtime.enemies().enemyFlashCountdown(0));
        assertEquals(0x68, runtime.enemies().fallingTargetX(0));
        assertEquals(0x70, runtime.enemies().fallingTargetY(0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void colorDungeonShellFallingDispatchReturnsToItsRomStateSix() {
        EntitySpriteDefinition definition = EntitySpriteDefinition.unsupported(0xE9);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xE9)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .spriteVariant(-1)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial)
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
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x4D)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        EntityRuntimeFixture.debug(runtime).setEnemyIgnoreHitsCountdownForTest(0, 0x02);
        runtime.configuration()
                .setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
                        RoomEntityGroundInteraction.Result.pit(entity, 0x01, 0x68, 0x70));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        List<EntityCombatEvent> fallingEvents = new ArrayList<>();
        for (int frame = 1; frame <= 16; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
            fallingEvents.addAll(runtime.consumePendingEntityEvents());
        }

        RoomEntity moving = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.FALLING, moving.status());
        assertEquals(0x43, moving.x());
        assertEquals(0x52, moving.y());
        assertEquals(0x03, moving.spriteVariant());
        assertEquals(0, runtime.enemies().fallingVisualYOffset(0));
        assertEquals(
                List.of(new EntityCombatEvent(0, 0x4D, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x18)),
                fallingEvents);

        for (int frame = 17; frame <= 25; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }
        assertEquals(4, runtime.enemies().fallingVisualYOffset(0));
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
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        EntitySpriteDefinition hardhat = catalog.forEntityType(0x20, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x20)
                .at(0x40, 0x50)
                .status(EntityStatus.FALLING)
                .spriteDefinition(hardhat)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial)
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
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(0x40, 0x50)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
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

    @Test
    void staggersButterflyWingVariantsByEntitySlot() {
        EntitySpriteDefinition definition = new EntitySpriteDefinition(
                0x6E,
                0x06,
                0x6BBD,
                EntitySpriteDefinition.Shape.SINGLE,
                0,
                List.of(
                        new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x5E, 0x01), null),
                        new EntitySpriteDefinition.Variant(new EntitySpriteDefinition.OamAttribute(0x5E, 0x41), null)));
        RoomEntitySnapshot initial = snapshot(
                new linksawakening.world.entity.RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x6E)
                        .at(24, 32)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build(),
                new linksawakening.world.entity.RoomEntityBuilder()
                        .slot(1)
                        .sourceLoadOrder(1)
                        .type(0x6E)
                        .at(40, 32)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0));

        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(1, runtime.inspection().snapshot().slots().get(1).spriteVariant());
    }

    @Test
    void butterflyUsesBankSixSignedFixedPointPositionUpdates() {
        EntitySpriteDefinition definition = butterflyDefinition();
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x6E)
                .at(24, 32)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        // State 0 selects +4 X speed; state 16 selects -4 Y speed.
        IntSupplier randomBytes = sequence(0x00, 0x01);
        for (int frame = 0; frame <= 17; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 200, 32, randomBytes));
        }

        RoomEntity butterfly = runtime.inspection().snapshot().slots().get(0);
        assertEquals(28, butterfly.x());
        assertEquals(31, butterfly.y());
    }

    @Test
    void butterflyAppliesTheTwoPixelVectorTowardLinkOnItsSixtyFourFramePhase() {
        EntitySpriteDefinition definition = butterflyDefinition();
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x6E)
                .at(24, 32)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        // The initial speed is zero. At state 0 the handler chooses a speed,
        // then writes the vector toward Link into its private state. The
        // subsequent 32-frame speed selection adds that private X component.
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00);
        runtime.tick(EntityTestFrames.frame(runtime, 64, 40, 32, randomBytes));
        for (int frame = 65; frame <= 99; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 40, 32, randomBytes));
        }

        // The attraction vector is stored in private state at frame 64. The
        // frame-96 speed refresh therefore selects 4 + 2 = 6, which produces
        // its first pixel one frame earlier than a plain +4 speed.
        RoomEntity butterfly = runtime.inspection().snapshot().slots().get(0);
        assertEquals(33, butterfly.x());
        assertEquals(36, butterfly.y());
    }

    @Test
    void butterflyUsesTheRomRatioForDiagonalAttractionVectors() {
        EntitySpriteDefinition definition = butterflyDefinition();
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x6E)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        // GetVectorTowardsLink(2) turns dx=16, dy=8 into (2,1), not (2,2).
        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 72, sequence(0x00, 0x00)));

        assertEquals(2, runtime.enemies().butterflyPrivateStateX(0));
        assertEquals(1, runtime.enemies().butterflyPrivateStateY(0));
    }

    @Test
    void entityHandlersConsumeSharedRandomBytesInRomReverseSlotOrder() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(
                new linksawakening.world.entity.RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x19)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build(),
                new linksawakening.world.entity.RoomEntityBuilder()
                        .slot(1)
                        .sourceLoadOrder(1)
                        .type(0x19)
                        .at(64, 80)
                        .spriteDefinition(definition)
                        .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        // AnimateEntities starts at MAX_ENTITIES - 1 and decrements. Slot 1
        // therefore consumes the first wake timer byte.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 72, 72, sequence(0x00, 0x3F)));

        assertEquals(0x8F, runtime.flyingEnemies().keeseTransitionCountdown(0));
        assertEquals(0x50, runtime.flyingEnemies().keeseTransitionCountdown(1));
    }

    @Test
    void ironMaskUsesItsRomRoamingHandlerSpeedAndMaskedDisplay() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(0x24, EntityRoomLoader.RoomTable.INDOORS_A, -1);

        assertTrue(definition.supported());
        assertEquals(0x03, definition.bank());
        assertEquals(0x4FCB, definition.address());
        assertEquals(8, definition.variantCount());

        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x24)
                                .at(0x50, 0x50)
                                .spriteDefinition(definition)
                                .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x03);

        assertEquals(2, runtime.enemies().enemyHealth(0));
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0xC0, 0xC0, randomBytes));

        assertEquals(1, runtime.enemies().ironMaskState(0));
        assertEquals(0x10, runtime.enemies().ironMaskTransitionCountdown(0));
        assertEquals(0, runtime.enemies().ironMaskSpeedY(0));
        assertEquals(
                0x4FCB,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(0)
                        .spriteDefinition()
                        .address());

        for (int frame = 1; frame <= 0x10; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0xC0, 0xC0, randomBytes));
        }

        assertEquals(0, runtime.enemies().ironMaskState(0));
        assertEquals(3, runtime.enemies().ironMaskDirection(0));
        assertEquals(0x0C, runtime.enemies().ironMaskSpeedY(0));
        assertEquals(0x20, runtime.enemies().ironMaskTransitionCountdown(0));
        assertEquals(
                0x4FCB,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(0)
                        .spriteDefinition()
                        .address());

        runtime.tick(EntityTestFrames.frame(runtime, 0x11, 0xC0, 0xC0, randomBytes));
        runtime.tick(EntityTestFrames.frame(runtime, 0x12, 0xC0, 0xC0, randomBytes));
        assertEquals(0x51, runtime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void runtimeExposesTheRoamingEnemyLaunchRequestAndSpawnsTheProjectile() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition definition = catalog.forEntityType(0x09, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial)
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        AtomicInteger randomCalls = new AtomicInteger();
        IntSupplier randomBytes = () -> {
            randomCalls.incrementAndGet();
            return 0;
        };

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, randomBytes, false));
        for (int frame = 1; frame <= 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, randomBytes, false));
        }

        assertEquals(1, randomCalls.get());
        assertEquals(1, runtime.inspection().projectileLaunchRequests().size());
        RoamingEnemyMotion.LaunchRequest request =
                runtime.inspection().projectileLaunchRequests().get(0);
        assertEquals(0, request.sourceSlot());
        assertEquals(0x09, request.sourceType());
        assertEquals(0x0A, request.projectileType());
        assertEquals(2, runtime.inspection().snapshot().loadedEntities().size());
        RoomEntity projectile = runtime.inspection().snapshot().slots().get(15);
        assertEquals(EntityStatus.ACTIVE, projectile.status());
        assertEquals(-1, projectile.sourceLoadOrder());
        assertEquals(0x0A, projectile.type());
        assertEquals(0x48, projectile.x());
        assertEquals(0x40, projectile.y());
    }

    @Test
    void clearingBurningOrStunnedEntitiesResetsTheirStatusCountdowns() throws IOException {
        RomEnemyCombatTables burningTables = new RomEnemyCombatTables(romWithSwordResult(0x09, 0xFE));
        RoomEntityRuntime burningRuntime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x09)
                                .at(64, 64)
                                .spriteDefinition(pairDefinition(0x09, 2))
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(burningTables)
                .build();
        burningRuntime.resolveCombat(0, 120, 120, false, true, true, 72, 1, 72, 1);
        burningRuntime.clearEntity(0);

        assertEquals(
                EntityStatus.DISABLED,
                burningRuntime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, burningRuntime.enemies().transitionCountdown(0));
        assertEquals(0, burningRuntime.enemies().stunnedCountdown(0));

        RomEnemyCombatTables stunnedTables = new RomEnemyCombatTables(loadRom());
        RoomEntityRuntime stunnedRuntime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0xE9)
                                .at(64, 64)
                                .spriteDefinition(pairDefinition(0xE9, 2))
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(null)
                .enemyCombatTables(stunnedTables)
                .build();
        stunnedRuntime.resolveCombat(0, 120, 120, false, true, true, 72, 1, 72, 1);
        stunnedRuntime.clearEntity(0);

        assertEquals(0, stunnedRuntime.enemies().transitionCountdown(0));
        assertEquals(0, stunnedRuntime.enemies().stunnedCountdown(0));
    }
}
