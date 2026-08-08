package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.RomBank;
import linksawakening.vfx.TransientVfxType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomEntityRuntimeTest {

    @Test
    void deathPresentationFieldsAreIndependentFromNormalSpriteVariant() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        RoomEntity entity = new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.DYING,
            body, 1, 0, 0, 0, 3, true);
        assertEquals(1, entity.spriteVariant());
        assertEquals(3, entity.deathSpriteVariant());
        assertTrue(entity.powerRecoilDeath());
    }

    @Test
    void crystalSwitchHitFlashesThenRequestsTheRomSwitchAnimation() throws Exception {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x66, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x66, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            true, null, catalog, tables);

        List<EntityCombatEvent> hit = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, hit.size());
        assertTrue(hit.get(0).swordHit());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, hit.get(0).soundChannel());
        assertEquals(0x03, hit.get(0).soundId());
        assertEquals(0x18, runtime.enemyFlashCountdown(0));
        assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());

        runtime.tick(0, 120, 120, sequence(0x00));

        assertEquals(0, runtime.enemyFlashCountdown(0));
        assertEquals(0x18, runtime.transitionCountdown(0));
        assertTrue(runtime.consumePendingSwitchBlockAnimationRequest());
        assertEquals(List.of(new EntityCombatEvent(0, 0x66, 0, false,
            EntityCombatEvent.SoundChannel.WAVE, 0x0E)),
            runtime.consumePendingEntityEvents());
        assertFalse(runtime.consumePendingSwitchBlockAnimationRequest());
    }

    @Test
    void crystalSwitchDoesNotRequestAnotherAnimationWhileSwitchBlocksAreActive()
        throws Exception {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x66, 64, 64, EntityStatus.ACTIVE,
                catalog.forEntityType(0x66, EntityRoomLoader.RoomTable.INDOORS_A), 0)),
            true, null, catalog, tables);

        runtime.setSwitchBlockAnimationActiveForTest(true);
        runtime.setEnemyFlashCountdownForTest(0, 0x02);
        runtime.tick(0, 120, 120, sequence(0x00));

        assertEquals(0, runtime.enemyFlashCountdown(0));
        assertFalse(runtime.consumePendingSwitchBlockAnimationRequest());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void rejectsDeathSpriteVariantsOutsideTheFourFrameRange() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        assertThrows(IllegalArgumentException.class, () -> new RoomEntity(0, 0, 0x09,
            64, 64, EntityStatus.DYING, body, 1, 0, 0, 0, -2, false));
        assertThrows(IllegalArgumentException.class, () -> new RoomEntity(0, 0, 0x09,
            64, 64, EntityStatus.DYING, body, 1, 0, 0, 0, 4, false));
    }

    @Test
    void rejectsDeathSpriteVariantsForNonDyingEntities() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        assertThrows(IllegalArgumentException.class, () -> new RoomEntity(0, 0, 0x09,
            64, 64, EntityStatus.ACTIVE, body, 1, 0, 0, 0, 0, false));
    }

    @Test
    void rejectsPowerRecoilDeathForNonDyingEntities() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        assertThrows(IllegalArgumentException.class, () -> new RoomEntity(0, 0, 0x09,
            64, 64, EntityStatus.ACTIVE, body, 1, 0, 0, 0, -1, true));
    }

    @Test
    void hydratesPowerRecoilDeathStateFromAnInitialDyingEntity() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        RoomEntity initialEntity = new RoomEntity(0, 0, 0x09, 64, 64,
            EntityStatus.DYING, body, 1, 0, 0, 0, 2, true);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(initialEntity));

        assertTrue(runtime.powerRecoilDeath(0));
        assertEquals(2, runtime.snapshot().slots().get(0).deathSpriteVariant());
    }

    @Test
    void colorShellDyingRefreshPreservesDeathPresentationMetadata() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        EntitySpriteDefinition body = pairDefinition(0xE9, 2);
        RoomEntity initialEntity = new RoomEntity(0, 0, 0xE9, 64, 64,
            EntityStatus.ACTIVE, body, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            snapshot(initialEntity), false, null, catalog);

        runtime.resolveCombat(0, 120, 120, false, true, true,
            72, 1, 72, 1, new EnemyAttackContext(1, false, true, false, false));
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
        assertEquals(-1, runtime.snapshot().slots().get(0).deathSpriteVariant());
        assertTrue(runtime.snapshot().slots().get(0).powerRecoilDeath());

        for (int frame = 1; frame <= 0x30; frame++) {
            runtime.tick(frame, 120, 120, () -> 0);
        }

        RoomEntity rebuilt = runtime.snapshot().slots().get(0);
        assertEquals(EntityStatus.DYING, rebuilt.status());
        assertEquals(0x10, runtime.dyingCountdown(0));
        assertEquals(2, rebuilt.deathSpriteVariant());
        assertTrue(rebuilt.powerRecoilDeath());
        assertEquals(EntitySpriteDefinition.Shape.RECTANGLE,
            rebuilt.spriteDefinition().shape());
    }

    @Test
    void disablingARecoilDeathClearsItsInternalPowerState() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x19, 64, 64, EntityStatus.ACTIVE,
                keeseDefinition(), 0)));

        runtime.resolveCombat(0, 120, 120, false, true, true,
            72, 1, 72, 1, new EnemyAttackContext(1, false, true, false, false));
        assertTrue(runtime.powerRecoilDeath(0));

        for (int frame = 0; frame < 0x40; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }

        assertFalse(runtime.snapshot().slots().get(0).loaded());
        assertFalse(runtime.powerRecoilDeath(0));
    }

    @Test
    void clearingThenReusingAProjectileSlotDoesNotRetainPowerDeathState() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
        EntitySpriteDefinition octorok = catalog.forEntityType(
            0x09, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        EntitySpriteDefinition keese = catalog.forEntityType(
            0x19, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        List<RoomEntity> slots = new ArrayList<>();
        slots.add(new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE, octorok, 0));
        for (int slot = 1; slot < 15; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        slots.add(new RoomEntity(15, 1, 0x19, 120, 120, EntityStatus.ACTIVE, keese, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(slots), false, null, catalog);

        runtime.resolveCombat(0, 80, 64, false, true, true,
            112, 16, 112, 16, new EnemyAttackContext(1, false, true, false, false));
        assertTrue(runtime.powerRecoilDeath(15));
        runtime.clearEntity(15);
        assertFalse(runtime.powerRecoilDeath(15));

        runtime.tick(0, 80, 64, () -> 0, false);
        for (int frame = 1; frame <= 6; frame++) {
            runtime.tick(frame, 80, 64, () -> 0, false);
        }

        RoomEntity projectile = runtime.snapshot().slots().get(15);
        assertEquals(EntityStatus.ACTIVE, projectile.status());
        assertEquals(0x0A, projectile.type());
        assertEquals(-1, projectile.deathSpriteVariant());
        assertFalse(projectile.powerRecoilDeath());
        assertFalse(runtime.powerRecoilDeath(15));
    }

    @Test
    void followsPieceOfPowerFrameDrivenPaletteVariant() {
        EntitySpriteDefinition definition = pairDefinition(0x33, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x33, 24, 32, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0x00);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(0x08);
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(0x10);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void activeEntitiesPassThroughTheGroundInteractionBoundaryAfterMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x4D, 1);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x4D, 32, 48, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        AtomicInteger calls = new AtomicInteger();
        runtime.setGroundInteraction((entity, frameCounter, previousStatus, speedZ,
                                      sideScrolling) -> {
            calls.incrementAndGet();
            RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
                entity.x() + 1, entity.y(), entity.status(), entity.spriteDefinition(),
                entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
                entity.z());
            return RoomEntityGroundInteraction.Result.unchanged(updated, 0x02);
        });

        runtime.tick(0, 0, 0, () -> 0);
        runtime.tick(1, 0, 0, () -> 0);

        assertEquals(2, calls.get());
        assertEquals(34, runtime.snapshot().slots().get(0).x());
        assertEquals(0x02, runtime.groundStatus(0));
    }

    @Test
    void richBackgroundProbeSuppliesIgnoreHitsToLegacyRecoilMovement() {
        RoomEntity initial = new RoomEntity(0, 0, 0x0B, 0x40, 0x40,
            EntityStatus.ACTIVE, pairDefinition(0x0B, 2), 0);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(initial));
        List<Integer> observedIgnoreHits = new ArrayList<>();
        runtime.setBackgroundInteraction(new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                           int nextX, int nextY) {
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }

            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                       int nextX, int nextY,
                                                       int ignoreHitsCountdown) {
                observedIgnoreHits.add(ignoreHitsCountdown);
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }
        });

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 0x40, 0x40, false, false, true,
            0x30, 0x30, 0x30, 0x30);
        assertEquals(1, events.size());
        assertTrue(runtime.enemyRecoilActive(0));

        runtime.tick(0, 0, 0, () -> 0,
            (entity, direction, nextX, nextY) -> true);

        assertTrue(observedIgnoreHits.contains(0x09),
            () -> "observed=" + observedIgnoreHits
                + " x=" + runtime.snapshot().slots().get(0).x()
                + " status=" + runtime.snapshot().slots().get(0).status()
                + " recoil=" + runtime.enemyRecoilActive(0)
                + " countdown=" + runtime.enemyIgnoreHitsCountdown(0));
        assertEquals(0x3D, runtime.snapshot().slots().get(0).x());
    }

    @Test
    void ledgeCollisionStateUsesRomResetBytesAndClearsWithTheEntity() {
        RoomEntity initial = new RoomEntity(0, 0, 0x09, 0x40, 0x40,
            EntityStatus.ACTIVE, pairDefinition(0x09, 2), 0);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(initial));

        assertEquals(0xFF, runtime.thrownDirection(0));
        assertEquals(0x00, runtime.ledgeTransitionTimer(0));

        runtime.setThrownDirectionForTest(0, 0x02);
        runtime.setLedgeTransitionTimerForTest(0, 0x07);
        assertEquals(0x02, runtime.thrownDirection(0));
        assertEquals(0x07, runtime.ledgeTransitionTimer(0));

        runtime.clearEntity(0);
        assertEquals(0xFF, runtime.thrownDirection(0));
        assertEquals(0x00, runtime.ledgeTransitionTimer(0));
    }

    @Test
    void groundResultQueuesSplashAndUnloadsTheSlotInSourceOrder() {
        EntitySpriteDefinition definition = pairDefinition(0x4D, 1);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x4D, 0x40, 0x50, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        runtime.setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
            RoomEntityGroundInteraction.Result.unloaded(entity, 0x02, true));

        runtime.tick(0, 0, 0, () -> 0);

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.groundStatus(0));
        assertEquals(List.of(new RoomEntityRuntime.TransientVfxRequest(
            TransientVfxType.WATER_SPLASH, 0x40, 0x50)),
            runtime.transientVfxRequests());
        assertEquals(List.of(new EntityCombatEvent(0, 0x4D, 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, 0x0E)),
            runtime.consumePendingEntityEvents());
    }

    @Test
    void pitResultStartsRomFallingStateWithAlignedTargetAndCountdown() {
        EntitySpriteDefinition definition = pairDefinition(0x4D, 4);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x4D, 0x40, 0x50, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 0x02);
        runtime.setEnemyFlashCountdownForTest(0, 0x08);
        runtime.setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
            RoomEntityGroundInteraction.Result.pit(entity, 0x01, 0x68, 0x70));

        runtime.tick(0, 0, 0, () -> 0);

        RoomEntity falling = runtime.snapshot().slots().get(0);
        assertEquals(EntityStatus.FALLING, falling.status());
        assertEquals(0x48, runtime.transitionCountdown(0));
        assertEquals(0x01, runtime.enemyIgnoreHitsCountdown(0));
        assertEquals(0x00, runtime.enemyFlashCountdown(0));
        assertEquals(0x68, runtime.fallingTargetX(0));
        assertEquals(0x70, runtime.fallingTargetY(0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void octorokUsesTheRomLongFallingCountdown() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x09, 0x40, 0x50, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 0x02);
        runtime.setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
            RoomEntityGroundInteraction.Result.pit(entity, 0x01, 0x68, 0x70));

        runtime.tick(0, 0, 0, () -> 0);

        assertEquals(EntityStatus.FALLING, runtime.snapshot().slots().get(0).status());
        assertEquals(0x6F, runtime.transitionCountdown(0));
        assertEquals(0x01, runtime.enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void colorDungeonShellFallingDispatchReturnsToItsRomStateSix() {
        EntitySpriteDefinition definition = EntitySpriteDefinition.unsupported(0xE9);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0xE9, 0x40, 0x50, EntityStatus.ACTIVE, definition, -1));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial, true, () -> 0, null);
        runtime.setEntityMapIdForTest(0xFF);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 0x02);
        runtime.setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
            RoomEntityGroundInteraction.Result.pit(entity, 0x01, 0x68, 0x70));

        runtime.tick(0, 0, 0, () -> 0);
        assertEquals(EntityStatus.FALLING, runtime.snapshot().slots().get(0).status());

        runtime.tick(1, 0, 0, () -> 0);

        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0x06, runtime.colorShellState(0));
    }

    @Test
    void fallingHandlerUsesRomVectorPhaseJingleAndUnloadBoundary() {
        EntitySpriteDefinition definition = pairDefinition(0x4D, 4);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x4D, 0x40, 0x50, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 0x02);
        runtime.setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
            RoomEntityGroundInteraction.Result.pit(entity, 0x01, 0x68, 0x70));

        runtime.tick(0, 0, 0, () -> 0);
        List<EntityCombatEvent> fallingEvents = new ArrayList<>();
        for (int frame = 1; frame <= 16; frame++) {
            runtime.tick(frame, 0, 0, () -> 0);
            fallingEvents.addAll(runtime.consumePendingEntityEvents());
        }

        RoomEntity moving = runtime.snapshot().slots().get(0);
        assertEquals(EntityStatus.FALLING, moving.status());
        assertEquals(0x43, moving.x());
        assertEquals(0x52, moving.y());
        assertEquals(0x03, moving.spriteVariant());
        assertEquals(0, runtime.fallingVisualYOffset(0));
        assertEquals(List.of(new EntityCombatEvent(0, 0x4D, 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, 0x18)),
            fallingEvents);

        for (int frame = 17; frame <= 25; frame++) {
            runtime.tick(frame, 0, 0, () -> 0);
        }
        assertEquals(4, runtime.fallingVisualYOffset(0));
        assertEquals(4, runtime.snapshot().visualYOffset(0));
        for (int frame = 26; frame <= 72; frame++) {
            runtime.tick(frame, 0, 0, () -> 0);
        }
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void longFallingHandlerRunsOnlyItsRomPresentationHandoff() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x09, 0x40, 0x50, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 0x04);
        runtime.setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
            RoomEntityGroundInteraction.Result.pit(entity, 0x01, 0x68, 0x70));
        AtomicInteger backgroundProbes = new AtomicInteger();
        runtime.setBackgroundInteraction((entity, direction, nextX, nextY) -> {
            backgroundProbes.incrementAndGet();
            return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
        });

        runtime.tick(0, 0, 0, () -> 0);
        runtime.tick(1, 0, 0, () -> 0);
        runtime.tick(2, 0, 0, () -> 0);
        runtime.tick(3, 0, 0, () -> 0);
        runtime.tick(4, 0, 0, () -> 0);

        RoomEntity falling = runtime.snapshot().slots().get(0);
        assertEquals(EntityStatus.FALLING, falling.status());
        assertEquals(0x6C, runtime.transitionCountdown(0));
        assertEquals(7, falling.spriteVariant());
        assertEquals(0x40, falling.x());
        assertEquals(0x50, falling.y());
        assertEquals(0, backgroundProbes.get());
    }

    @Test
    void longFallingMoblinSwordAppliesAlertFacingBeforePresentationHandoff() {
        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>();
        for (int variant = 0; variant < 8; variant++) {
            variants.add(List.of(new EntitySpriteDefinition.DynamicSprite(0, 0,
                new EntitySpriteDefinition.OamAttribute(0x60, 0x03),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true)));
        }
        EntitySpriteDefinition definition = EntitySpriteDefinition.dynamic(
            0x14, 0x07, 0x7A95, 0, variants);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x14, 0x50, 0x50, EntityStatus.INIT, definition, 6));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 0x04);
        runtime.setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
            RoomEntityGroundInteraction.Result.pit(entity, 0x01, 0x68, 0x70));
        AtomicInteger backgroundProbes = new AtomicInteger();
        runtime.setBackgroundInteraction((entity, direction, nextX, nextY) -> {
            backgroundProbes.incrementAndGet();
            return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
        });

        for (int frame = 0; frame <= 4; frame++) {
            runtime.tick(frame, 0, 0, () -> 0);
        }

        RoomEntity falling = runtime.snapshot().slots().get(0);
        assertEquals(EntityStatus.FALLING, falling.status());
        assertEquals(0x6C, runtime.transitionCountdown(0));
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
    void staggersButterflyWingVariantsByEntitySlot() {
        EntitySpriteDefinition definition = new EntitySpriteDefinition(0x6E, 0x06, 0x6BBD,
            EntitySpriteDefinition.Shape.SINGLE, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x5E, 0x01), null),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x5E, 0x41), null)));
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x6E, 24, 32, EntityStatus.INIT, definition, 0),
            new RoomEntity(1, 1, 0x6E, 40, 32, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0);

        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(1, runtime.snapshot().slots().get(1).spriteVariant());
    }

    @Test
    void butterflyUsesBankSixSignedFixedPointPositionUpdates() {
        EntitySpriteDefinition definition = butterflyDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x6E, 24, 32, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        // State 0 selects +4 X speed; state 16 selects -4 Y speed.
        IntSupplier randomBytes = sequence(0x00, 0x01);
        for (int frame = 0; frame <= 17; frame++) {
            runtime.tick(frame, 200, 32, randomBytes);
        }

        RoomEntity butterfly = runtime.snapshot().slots().get(0);
        assertEquals(28, butterfly.x());
        assertEquals(31, butterfly.y());
    }

    @Test
    void butterflyAppliesTheTwoPixelVectorTowardLinkOnItsSixtyFourFramePhase() {
        EntitySpriteDefinition definition = butterflyDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x6E, 24, 32, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        // The initial speed is zero. At state 0 the handler chooses a speed,
        // then writes the vector toward Link into its private state. The
        // subsequent 32-frame speed selection adds that private X component.
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00);
        runtime.tick(64, 40, 32, randomBytes);
        for (int frame = 65; frame <= 99; frame++) {
            runtime.tick(frame, 40, 32, randomBytes);
        }

        // The attraction vector is stored in private state at frame 64. The
        // frame-96 speed refresh therefore selects 4 + 2 = 6, which produces
        // its first pixel one frame earlier than a plain +4 speed.
        RoomEntity butterfly = runtime.snapshot().slots().get(0);
        assertEquals(33, butterfly.x());
        assertEquals(36, butterfly.y());
    }

    @Test
    void butterflyUsesTheRomRatioForDiagonalAttractionVectors() {
        EntitySpriteDefinition definition = butterflyDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x6E, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        // GetVectorTowardsLink(2) turns dx=16, dy=8 into (2,1), not (2,2).
        runtime.tick(0, 80, 72, sequence(0x00, 0x00));

        assertEquals(2, runtime.butterflyPrivateStateX(0));
        assertEquals(1, runtime.butterflyPrivateStateY(0));
    }

    @Test
    void keeseSleepsOutsideTheRomWakeWindowAndWakesInsideIt() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x19, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 96, 64, sequence(0x00));
        assertEquals(0, runtime.keeseState(0));
        assertEquals(0, runtime.keeseTransitionCountdown(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(1, 72, 64, sequence(0x00));
        assertEquals(1, runtime.keeseState(0));
        assertEquals(0x50, runtime.keeseTransitionCountdown(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());
    }

    @Test
    void keeseRetunesItsAngleAndLoadsTheRomSpeedTablesEveryTenFlightFrames() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x19, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        IntSupplier randomBytes = sequence(0x00, 0x01, 0x01);

        runtime.tick(0, 72, 64, randomBytes);
        for (int frame = 1; frame <= 10; frame++) {
            runtime.tick(frame, 72, 64, randomBytes);
        }

        assertEquals(1, runtime.keeseState(0));
        assertEquals(0x0D, runtime.keeseAngle(0));
        assertEquals(0x05, runtime.keeseSpeedX(0));
        assertEquals(0xF3, runtime.keeseSpeedY(0));
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void entityHandlersConsumeSharedRandomBytesInRomReverseSlotOrder() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x19, 64, 64, EntityStatus.ACTIVE, definition, 0),
            new RoomEntity(1, 1, 0x19, 64, 80, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        // AnimateEntities starts at MAX_ENTITIES - 1 and decrements. Slot 1
        // therefore consumes the first wake timer byte.
        runtime.tick(0, 72, 72, sequence(0x00, 0x3F));

        assertEquals(0x8F, runtime.keeseTransitionCountdown(0));
        assertEquals(0x50, runtime.keeseTransitionCountdown(1));
    }

    @Test
    void keeseUsesTheRomEnemyHitboxCadenceAndContactDamage() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x19, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> skipped = runtime.resolveCombat(
            0, 64, 64, false, true, false, 0, 0, 0, 0);
        assertTrue(skipped.isEmpty());

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0, contact.get(0).slot());
        assertEquals(0x19, contact.get(0).type());
        assertEquals(4, contact.get(0).linkDamage());
        assertFalse(contact.get(0).swordHit());

        assertTrue(runtime.resolveCombat(1, 73, 64, false, true,
            false, 0, 0, 0, 0).isEmpty());
        assertTrue(runtime.resolveCombat(1, 64, 64, true, true,
            false, 0, 0, 0, 0).isEmpty());
        assertTrue(runtime.resolveCombat(1, 64, 64, false, false,
            false, 0, 0, 0, 0).isEmpty());
    }

    @Test
    void aBasicSwordHitPutsKeeseIntoTheRomDyingState() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x19, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(0, events.get(0).linkDamage());
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
        assertEquals(0x40, runtime.dyingCountdown(0));
        assertEquals(-1, runtime.snapshot().slots().get(0).deathSpriteVariant());
        assertFalse(runtime.snapshot().slots().get(0).powerRecoilDeath());

        for (int frame = 1; frame <= 0x20; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertEquals(0x20, runtime.dyingCountdown(0));
        assertEquals(-1, runtime.snapshot().slots().get(0).deathSpriteVariant());
        runtime.tick(0x21, 120, 120, sequence(0x00));
        assertEquals(0x1F, runtime.dyingCountdown(0));
        assertEquals(3, runtime.snapshot().slots().get(0).deathSpriteVariant());

        for (int frame = 0x22; frame < 0x40; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertTrue(runtime.snapshot().slots().get(0).loaded());
        runtime.tick(0x40, 120, 120, sequence(0x00));
        assertFalse(runtime.snapshot().slots().get(0).loaded());
    }

    @Test
    void aPowerRecoilSwordHitMarksTheRomPowerDeathPresentation() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x19, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1,
            new EnemyAttackContext(1, false, true, false, false));

        assertEquals(1, events.size());
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
        assertEquals(0x40, runtime.dyingCountdown(0));
        assertEquals(-1, runtime.snapshot().slots().get(0).deathSpriteVariant());
        assertTrue(runtime.snapshot().slots().get(0).powerRecoilDeath());
    }

    @Test
    void octorokUsesTheSharedRoamingEnemyTimersAndDirectionSpeeds() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        IntSupplier randomBytes = sequence(0x02, 0x03);

        // State 0 pauses for ($10 + random & $0F) frames, then state 1
        // selects direction 3 (down) and the ROM's +8/16 fixed-point speed.
        runtime.tick(0, 200, 32, randomBytes);
        assertEquals(1, runtime.octorokState(0));
        assertEquals(0x12, runtime.octorokTransitionCountdown(0));
        assertEquals(0, runtime.octorokSpeedY(0));

        for (int frame = 1; frame <= 0x12; frame++) {
            runtime.tick(frame, 200, 32, randomBytes);
        }
        assertEquals(0, runtime.octorokState(0),
            () -> "state=" + runtime.octorokState(0)
                + " timer=" + runtime.octorokTransitionCountdown(0));
        assertEquals(0x23, runtime.octorokTransitionCountdown(0));
        assertEquals(3, runtime.octorokDirection(0));
        assertEquals(8, runtime.octorokSpeedY(0));
        assertEquals(64, runtime.snapshot().slots().get(0).y());

        runtime.tick(0x13, 200, 32, randomBytes);
        runtime.tick(0x14, 200, 32, randomBytes);
        assertEquals(65, runtime.snapshot().slots().get(0).y());
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void ironMaskUsesItsRomRoamingHandlerSpeedAndMaskedDisplay() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x24, EntityRoomLoader.RoomTable.INDOORS_A, -1);

        assertTrue(definition.supported());
        assertEquals(0x03, definition.bank());
        assertEquals(0x4FCB, definition.address());
        assertEquals(8, definition.variantCount());

        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x24, 0x50, 0x50, EntityStatus.ACTIVE,
                definition, 0)), true, () -> 0, catalog, tables);
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x03);

        assertEquals(2, runtime.enemyHealth(0));
        runtime.tick(0, 0xC0, 0xC0, randomBytes);

        assertEquals(1, runtime.ironMaskState(0));
        assertEquals(0x10, runtime.ironMaskTransitionCountdown(0));
        assertEquals(0, runtime.ironMaskSpeedY(0));
        assertEquals(0x4FCB, runtime.snapshot().slots().get(0).spriteDefinition().address());

        for (int frame = 1; frame <= 0x10; frame++) {
            runtime.tick(frame, 0xC0, 0xC0, randomBytes);
        }

        assertEquals(0, runtime.ironMaskState(0));
        assertEquals(3, runtime.ironMaskDirection(0));
        assertEquals(0x0C, runtime.ironMaskSpeedY(0));
        assertEquals(0x20, runtime.ironMaskTransitionCountdown(0));
        assertEquals(0x4FCB, runtime.snapshot().slots().get(0).spriteDefinition().address());

        runtime.tick(0x11, 0xC0, 0xC0, randomBytes);
        runtime.tick(0x12, 0xC0, 0xC0, randomBytes);
        assertEquals(0x51, runtime.snapshot().slots().get(0).y());
    }

    @Test
    void maskedIronMaskRejectsARearSwordHitWithTheRomPokeResponse() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x24, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x24, 0x50, 0x50, EntityStatus.ACTIVE,
                definition, 0)), true, () -> 0, catalog, tables);

        // Java direction 1 is ROM UP, while the freshly initialized Iron Mask
        // is facing ROM RIGHT (direction 0).
        runtime.tick(0, 0xC0, 0xC0, () -> 0, null, null, 0, 1, 0);
        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 0xC0, 0xC0, false, true, true, 0x58, 1, 0x58, 1);

        assertEquals(1, events.size());
        EntityCombatEvent event = events.get(0);
        assertTrue(event.swordHit());
        assertEquals(0, event.enemyDamage());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, event.soundChannel());
        assertEquals(0x07, event.soundId());
        assertEquals(new EntityCombatEvent.SwordPokeVfx(0x50, 0x50), event.swordPokeVfx());
        assertEquals(2, runtime.enemyHealth(0));
        assertEquals(0x10, runtime.enemyIgnoreHitsCountdown(0));
        assertEquals(0xF0, runtime.enemyRecoilSpeedXForTest(0));
        assertEquals(0xF0, runtime.enemyRecoilSpeedYForTest(0));
    }

    @Test
    void hookshotUnmasksAnIronMaskAndSpawnsItsRomMaskEntity() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition ironMaskDefinition = catalog.forEntityType(
            0x24, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x24, 0x4D, 0x50, EntityStatus.ACTIVE,
                ironMaskDefinition, 0)), true, () -> 0, catalog, tables);

        int hookshotSlot = runtime.spawnHookshotChain(0x50, 0x50, 0, 1);
        assertEquals(15, hookshotSlot);

        runtime.tick(0, 0xC0, 0xC0, () -> 0);

        assertEquals(1, runtime.ironMaskPrivateState2(0));
        RoomEntity mask = runtime.snapshot().slots().stream()
            .filter(entity -> entity.loaded() && entity.type() == 0x32)
            .findFirst()
            .orElseThrow();
        RoomEntity chain = runtime.snapshot().slots().get(hookshotSlot);
        assertEquals(chain.x(), mask.x());
        assertEquals(chain.y(), mask.y());
        assertEquals(0x03, mask.spriteDefinition().bank());
        assertEquals(0x5B80, mask.spriteDefinition().address());
        assertEquals(chain.spriteVariant() & 0x01, mask.spriteVariant());
        assertEquals(0xB2, runtime.physicsFlags(mask.slot()));

        runtime.tick(1, 0xC0, 0xC0, () -> 0);
        runtime.tick(2, 0xC0, 0xC0, () -> 0);
        runtime.tick(3, 0xC0, 0xC0, () -> 0);

        RoomEntity unmasked = runtime.snapshot().slots().get(0);
        assertEquals(0x4F, unmasked.x());
        assertEquals(0x4FEB, unmasked.spriteDefinition().address());
        assertEquals(0, unmasked.spriteVariant());
    }

    @Test
    void runtimeExposesTheRoamingEnemyLaunchRequestAndSpawnsTheProjectile() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x09, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial, false, null, catalog);
        AtomicInteger randomCalls = new AtomicInteger();
        IntSupplier randomBytes = () -> {
            randomCalls.incrementAndGet();
            return 0;
        };

        runtime.tick(0, 80, 64, randomBytes, false);
        for (int frame = 1; frame <= 6; frame++) {
            runtime.tick(frame, 80, 64, randomBytes, false);
        }

        assertEquals(1, randomCalls.get());
        assertEquals(1, runtime.projectileLaunchRequests().size());
        RoamingEnemyMotion.LaunchRequest request = runtime.projectileLaunchRequests().get(0);
        assertEquals(0, request.sourceSlot());
        assertEquals(0x09, request.sourceType());
        assertEquals(0x0A, request.projectileType());
        assertEquals(2, runtime.snapshot().loadedEntities().size());
        RoomEntity projectile = runtime.snapshot().slots().get(15);
        assertEquals(EntityStatus.ACTIVE, projectile.status());
        assertEquals(-1, projectile.sourceLoadOrder());
        assertEquals(0x0A, projectile.type());
        assertEquals(0x48, projectile.x());
        assertEquals(0x40, projectile.y());
    }

    @Test
    void runtimePassesCreditsContextToSuppressOnlyOctorokLaunches() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 80, 64, () -> 0, true);
        for (int frame = 1; frame <= 6; frame++) {
            runtime.tick(frame, 80, 64, () -> 0, true);
        }

        assertTrue(runtime.projectileLaunchRequests().isEmpty());
        assertEquals(1, runtime.snapshot().loadedEntities().size());
    }

    @Test
    void moblinUsesTheSameRomRoamingStateMachineAndDirectionalDisplayPairs() {
        EntitySpriteDefinition definition = pairDefinition(0x0B, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0B, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        IntSupplier randomBytes = sequence(0x02, 0x03);

        runtime.tick(0, 200, 32, randomBytes);
        for (int frame = 1; frame <= 0x12; frame++) {
            runtime.tick(frame, 200, 32, randomBytes);
        }

        assertEquals(0, runtime.octorokState(0));
        assertEquals(3, runtime.octorokDirection(0));
        assertEquals(8, runtime.octorokSpeedY(0));
        runtime.tick(0x13, 200, 32, randomBytes);
        runtime.tick(0x14, 200, 32, randomBytes);
        assertEquals(65, runtime.snapshot().slots().get(0).y());
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void moblinSwordUsesTheBankSevenIdleAlertAndSlowWalkingStates() {
        EntitySpriteDefinition definition = pairDefinition(0x14, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x14, 0x50, 64, EntityStatus.ACTIVE, definition, 6));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 0xC0, 64, () -> 0);
        assertEquals(1, runtime.moblinSwordState(0));
        assertEquals(0x80, runtime.moblinSwordTransitionCountdown(0));
        assertEquals(0, runtime.moblinSwordDirection(0));
        assertEquals(0x06, runtime.moblinSwordSpeedX(0));

        runtime.tick(1, 0xC0, 64, () -> 0);
        runtime.tick(2, 0xC0, 64, () -> 0);
        runtime.tick(3, 0xC0, 64, () -> 0);
        assertEquals(0x51, runtime.snapshot().slots().get(0).x());

        runtime.tick(4, 0x70, 64, () -> 0);
        assertEquals(2, runtime.moblinSwordState(0));
        assertEquals(0x80, runtime.moblinSwordTransitionCountdown(0));
        assertEquals(0x10, runtime.moblinSwordPrivateCountdown1(0));
        assertEquals(0, runtime.moblinSwordDirection(0));
    }

    @Test
    void moblinSwordUsesTheRomHealthContactAndSwordDamageFamily() {
        EntitySpriteDefinition definition = pairDefinition(0x14, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x14, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, firstHit.size());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemyHealth(0));
        assertEquals(0x18, runtime.enemyFlashCountdown(0));
        assertTrue(runtime.enemyRecoilActive(0));
    }

    @Test
    void projectileWallImpactAlertsMoblinSwordSlotsProcessedLaterInTheFrame() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshotWithSlots(
            new RoomEntity(0, 0, 0x14, 0x50, 64, EntityStatus.ACTIVE,
                pairDefinition(0x14, 8), 6),
            new RoomEntity(1, 1, 0x0C, 0x20, 64, EntityStatus.ACTIVE,
                pairDefinition(0x0C, 4), 0)));

        runtime.tick(0, 0xC0, 64, () -> 0,
            (entity, direction, nextX, nextY) -> entity.type() == 0x0C && direction == 0);

        assertEquals(0x04, runtime.swordMoblinAlertingSoundCounter());
        assertEquals(2, runtime.moblinSwordState(0));
        assertEquals(0x10, runtime.moblinSwordPrivateCountdown1(0));

        runtime.tick(1, 0xC0, 64, () -> 0,
            (entity, direction, nextX, nextY) -> false);
        assertEquals(0x03, runtime.swordMoblinAlertingSoundCounter());
    }

    @Test
    void hideoutMoblinSwordPublishesDialog190OnlyOnce() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x14, 0x50, 0x40, EntityStatus.ACTIVE,
                pairDefinition(0x14, 8), 6)));
        runtime.setEntityMapIdForTest(0x15);
        runtime.setTransitionSequenceCounterForTest(0x04);

        runtime.tick(0, 0xC0, 0x40, () -> 0);
        runtime.tick(1, 0x70, 0x40, () -> 0);
        runtime.tick(2, 0x70, 0x40, () -> 0);

        assertEquals(List.of(new RoomEntityRuntime.DialogRequest(1, 0x90)),
            runtime.consumePendingDialogRequests());
        assertEquals(1, runtime.moblinSwordPrivateState3(0));

        runtime.tick(3, 0x70, 0x40, () -> 0);
        assertTrue(runtime.consumePendingDialogRequests().isEmpty());
        assertEquals(1, runtime.moblinSwordPrivateState3(0));
    }

    @Test
    void octorokStopsAtTheRoamingEnemyBackgroundCollisionPoint() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        IntSupplier randomBytes = sequence(0x00, 0x03, 0x01);

        runtime.tick(0, 200, 32, randomBytes);
        for (int frame = 1; frame <= 0x12; frame++) {
            runtime.tick(frame, 200, 32, randomBytes);
        }

        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 3;
        runtime.tick(0x13, 200, 32, randomBytes, wall);
        runtime.tick(0x14, 200, 32, randomBytes, wall);
        runtime.tick(0x15, 200, 32, randomBytes, wall);

        assertEquals(64, runtime.snapshot().slots().get(0).y());
        assertEquals(0, runtime.octorokSpeedY(0));
    }

    @Test
    void octorokUsesTheNormalEnemyHitboxAndOneBasicSwordDamage() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        RoomEntityRuntime swordRuntime = RoomEntityRuntime.from(initial);
        List<EntityCombatEvent> events = swordRuntime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(EntityStatus.DYING, swordRuntime.snapshot().slots().get(0).status());
    }

    @Test
    void moblinUsesTwoHealthPointsAndTheRomDamageCooldown() {
        EntitySpriteDefinition definition = pairDefinition(0x0B, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0B, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, firstHit.size());
        assertTrue(firstHit.get(0).swordHit());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemyHealth(0));
        assertEquals(0x18, runtime.enemyFlashCountdown(0));
        assertTrue(runtime.resolveCombat(
            1, 120, 120, false, true, true, 72, 1, 72, 1).isEmpty());

        for (int frame = 1; frame <= 0x18; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertEquals(0, runtime.enemyFlashCountdown(0));

        RoomEntity movedMoblin = runtime.snapshot().slots().get(0);
        List<EntityCombatEvent> secondHit = runtime.resolveCombat(
            0x19, 120, 120, false, true, true,
            movedMoblin.x() + 8, 1, movedMoblin.y() + 8, 1);
        assertEquals(1, secondHit.size());
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.enemyHealth(0));
    }

    @Test
    void moblinSwordHitConfiguresRomRecoilAndEnemyHitJingle() {
        EntitySpriteDefinition definition = pairDefinition(0x0B, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0B, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        EntityCombatEvent event = events.get(0);
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, event.soundChannel());
        assertEquals(0x03, event.soundId());
        assertEquals(0x18, runtime.enemyFlashCountdown(0));
        assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.enemyRecoilActive(0));
        assertEquals(0xD0, runtime.enemyRecoilSpeedX(0));
        assertEquals(0xD0, runtime.enemyRecoilSpeedY(0));

        runtime.tick(1, 120, 120, sequence(0x00));

        RoomEntity afterRecoil = runtime.snapshot().slots().get(0);
        assertEquals(61, afterRecoil.x(), "x=" + afterRecoil.x());
        assertEquals(61, afterRecoil.y(), "y=" + afterRecoil.y());
        assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
    }

    @Test
    void lethalOctorokSwordHitStillPublishesFinalEnemyHitJingle() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
        assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0));
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x03, events.get(0).soundId());
    }

    @Test
    void clearingARecoilingEnemyClearsAllRecoilStateBeforeSlotReuse() {
        EntitySpriteDefinition definition = pairDefinition(0x0B, 8);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x0B, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        runtime.resolveCombat(0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertTrue(runtime.enemyRecoilActive(0));

        assertEquals(1, runtime.clearEntity(0));

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
        assertFalse(runtime.enemyRecoilActive(0));
        assertEquals(0, runtime.enemyRecoilSpeedX(0));
        assertEquals(0, runtime.enemyRecoilSpeedY(0));
        assertEquals(0, runtime.enemyIgnoreHitsCountdown(0));
    }

    @Test
    void dyingCleanupClearsRecoilStateWhenTheRomDeathCountdownExpires() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        runtime.resolveCombat(0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertTrue(runtime.enemyRecoilActive(0));

        for (int frame = 0; frame < 0x40; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
        assertFalse(runtime.enemyRecoilActive(0));
        assertEquals(0, runtime.enemyRecoilSpeedX(0));
        assertEquals(0, runtime.enemyRecoilSpeedY(0));
    }

    @Test
    void pairoddUsesItsRomNormalEnemyHealthAndContactDamage() {
        EntitySpriteDefinition definition = pairDefinition(0x57, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x57, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0x57));
        assertEquals(2, runtime.enemyHealth(0));

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, firstHit.size());
        assertTrue(firstHit.get(0).swordHit());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemyHealth(0));

        for (int frame = 1; frame <= 0x18; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        List<EntityCombatEvent> secondHit = runtime.resolveCombat(
            0x19, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, secondHit.size());
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());

        assertFalse(RoomEntityCombatRules.supportsEnemyCollision(0x58));
        assertEquals(0, RoomEntityCombatRules.basicSwordDamage(0x58));
    }

    @Test
    void colorShellsAreAdmittedAsRomCombatTargets() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xE9, 64, 64, EntityStatus.ACTIVE,
                pairDefinition(0xE9, 2), 0),
            new RoomEntity(1, 1, 0xEA, 96, 64, EntityStatus.ACTIVE,
                pairDefinition(0xEA, 2), 0),
            new RoomEntity(2, 2, 0xEB, 128, 64, EntityStatus.ACTIVE,
                pairDefinition(0xEB, 2), 0)),
            false, null, null, tables);

        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0xE9));
        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0xEA));
        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0xEB));
        assertEquals(tables.initialHealth(0xE9), runtime.enemyHealth(0));
        assertEquals(tables.initialHealth(0xEA), runtime.enemyHealth(1));
        assertEquals(tables.initialHealth(0xEB), runtime.enemyHealth(2));
    }

    @Test
    void colorShellRuntimeDispatchesRomStateAndWorldSideEffects() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RuntimeColorShellWorld world = new RuntimeColorShellWorld();
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xE9, 0x40, 0x40, EntityStatus.ACTIVE,
                EntitySpriteDefinition.unsupported(0xE9), -1)),
            false, () -> 0x00, catalog);
        runtime.setColorShellWorld(world);
        runtime.setColorShellStateForTest(0, 0x08, 1, 0, 0, 0);
        world.objectValue = 0x5E;

        runtime.tick(0, 0x40, 0x40, () -> 0x00, null);

        RoomEntity shell = runtime.snapshot().slots().get(0);
        assertEquals(0x0C, runtime.colorShellState(0));
        assertEquals(0xF0, runtime.colorShellPhysicsFlags(0));
        assertEquals(0x67A8, shell.spriteDefinition().address());
        assertEquals(List.of(0x67), world.objectWrites);
        assertEquals(List.of(0x04), world.noises);
    }

    @Test
    void colorShellCompletionUnloadsAndPublishesItsRoomPersistenceMask() {
        RuntimeColorShellWorld world = new RuntimeColorShellWorld();
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xE9, 0x40, 0x40, EntityStatus.ACTIVE,
                EntitySpriteDefinition.unsupported(0xE9), -1)));
        runtime.setColorShellWorld(world);
        runtime.setColorShellStateForTest(0, 0x0D, 1, 0, 0, 0);

        runtime.tick(0, 0x40, 0x40, () -> 0x00, null);

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
        assertEquals(1, runtime.consumePendingClearedEntityMask());
        assertEquals(List.of(0x5F), world.objectWrites);
        assertEquals(List.of(List.of(0x40, 0x40)), world.poofs);
    }

    @Test
    void romBurningStatusUsesTheSharedSpecialDamageValueAndExpiresIntoDeath()
        throws IOException {
        byte[] rom = romWithSwordResult(0x09, 0xFE);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE,
                pairDefinition(0x09, 2), 0)), false, null, null, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertEquals(0xFE, events.get(0).enemySpecialAction());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(EntityStatus.BURNING, runtime.snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemyHealth(0));
        assertEquals(0x60, runtime.transitionCountdown(0));
        assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0));
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x03, events.get(0).soundId());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE,
            events.get(0).secondarySoundChannel());
        assertEquals(0x12, events.get(0).secondarySoundId());
        assertTrue(runtime.resolveCombat(1, 120, 120, false, true,
            true, 72, 1, 72, 1).isEmpty());

        for (int frame = 0; frame < 0x60; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
        assertEquals(0x1F, runtime.dyingCountdown(0));
        assertEquals(3, runtime.snapshot().slots().get(0).deathSpriteVariant());
        assertFalse(runtime.snapshot().slots().get(0).powerRecoilDeath());
        assertEquals(0x04, runtime.physicsFlags(0));
        List<EntityCombatEvent> expiryEvents = runtime.consumePendingEntityEvents();
        assertEquals(1, expiryEvents.size());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE,
            expiryEvents.getFirst().soundChannel());
        assertEquals(0x13, expiryEvents.getFirst().soundId());
    }

    @Test
    void romStunStatusUsesColorShellsRawSpecialDamageValueAndRecovers() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xE9, 64, 64, EntityStatus.ACTIVE,
                pairDefinition(0xE9, 2), 0)), false, null, null, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertEquals(0xFF, events.get(0).enemySpecialAction());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(EntityStatus.STUNNED, runtime.snapshot().slots().get(0).status());
        assertEquals(tables.initialHealth(0xE9), runtime.enemyHealth(0));
        assertEquals(0xFF, runtime.stunnedCountdown(0));
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x03, events.get(0).soundId());
        assertEquals(EntityCombatEvent.SoundChannel.NONE,
            events.get(0).secondarySoundChannel());
        assertEquals(-1, events.get(0).secondarySoundId());
        assertTrue(runtime.resolveCombat(1, 120, 120, false, true,
            true, 72, 1, 72, 1).isEmpty());

        for (int frame = 0; frame < 0xFF; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.stunnedCountdown(0));
    }

    @Test
    void romBurningGibdoBecomesAnActiveStalfosAfterItsTimerExpires() throws IOException {
        byte[] rom = romWithSwordResult(0x1F, 0xFE);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x1F, 64, 64, EntityStatus.ACTIVE,
                pairDefinition(0x1F, 2), 0)), false, null, catalog, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(0xFE, events.get(0).enemySpecialAction());

        for (int frame = 0; frame < 0x60; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertEquals(0x1E, runtime.snapshot().slots().get(0).type());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.transitionCountdown(0));
        assertEquals(0x12, runtime.physicsFlags(0));
        assertEquals(0x4E7D,
            runtime.snapshot().slots().get(0).spriteDefinition().address());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void unknownRomSpecialDamageRemainsEventDataWithoutChangingHealthOrStatus()
        throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(romWithSwordResult(0x09, 0xFD));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE,
                pairDefinition(0x09, 2), 0)), false, null, null, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertEquals(0xFD, events.get(0).enemySpecialAction());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemyHealth(0));
        assertEquals(EntityCombatEvent.SoundChannel.NONE,
            events.get(0).secondarySoundChannel());
        assertEquals(-1, events.get(0).secondarySoundId());
    }

    @Test
    void clearingBurningOrStunnedEntitiesResetsTheirStatusCountdowns() throws IOException {
        RomEnemyCombatTables burningTables =
            new RomEnemyCombatTables(romWithSwordResult(0x09, 0xFE));
        RoomEntityRuntime burningRuntime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE,
                pairDefinition(0x09, 2), 0)), false, null, null, burningTables);
        burningRuntime.resolveCombat(0, 120, 120, false, true,
            true, 72, 1, 72, 1);
        burningRuntime.clearEntity(0);

        assertEquals(EntityStatus.DISABLED, burningRuntime.snapshot().slots().get(0).status());
        assertEquals(0, burningRuntime.transitionCountdown(0));
        assertEquals(0, burningRuntime.stunnedCountdown(0));

        RomEnemyCombatTables stunnedTables = new RomEnemyCombatTables(loadRom());
        RoomEntityRuntime stunnedRuntime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xE9, 64, 64, EntityStatus.ACTIVE,
                pairDefinition(0xE9, 2), 0)), false, null, null, stunnedTables);
        stunnedRuntime.resolveCombat(0, 120, 120, false, true,
            true, 72, 1, 72, 1);
        stunnedRuntime.clearEntity(0);

        assertEquals(0, stunnedRuntime.transitionCountdown(0));
        assertEquals(0, stunnedRuntime.stunnedCountdown(0));
    }

    @Test
    void romCombatTablesDriveInitialHealthAndContactDamage() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.ACTIVE,
                pairDefinition(0x09, 2), 0),
            new RoomEntity(1, 1, 0x0B, 96, 64, EntityStatus.ACTIVE,
                pairDefinition(0x0B, 2), 0)),
            false, null, null, tables);

        assertEquals(1, runtime.enemyHealth(0));
        assertEquals(2, runtime.enemyHealth(1));

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());
    }

    @Test
    void attackContextUsesRomSwordUpgradeDamageInTheRuntime() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x0B, 64, 64, EntityStatus.ACTIVE,
                pairDefinition(0x0B, 2), 0)),
            false, null, null, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1,
            new EnemyAttackContext(2, false, false, false, false));

        assertEquals(1, events.size());
        assertEquals(0, runtime.enemyHealth(0));
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
        assertEquals(2, events.get(0).enemyDamage());
        assertEquals(-1, events.get(0).enemySpecialAction());
    }

    @Test
    void zeroRomSwordResultKeepsHealthAndReportsOnlyTheBump() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x15, 64, 64, EntityStatus.ACTIVE,
                pairDefinition(0x15, 2), 0)),
            false, null, null, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertEquals(4, runtime.enemyHealth(0));
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(-1, events.get(0).enemySpecialAction());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x09, events.get(0).soundId());
    }

    @Test
    void simplePairEnemiesUseTheirRomAnimationCadences() {
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0F, 24, 32, EntityStatus.ACTIVE, pairDefinition(0x0F, 2), 0),
            new RoomEntity(1, 1, 0x12, 40, 32, EntityStatus.ACTIVE, pairDefinition(0x12, 2), 0),
            new RoomEntity(2, 2, 0x20, 56, 32, EntityStatus.ACTIVE, pairDefinition(0x20, 2), 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(1, runtime.snapshot().slots().get(1).spriteVariant());
        assertEquals(0, runtime.snapshot().slots().get(2).spriteVariant());

        runtime.tick(8);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(1, runtime.snapshot().slots().get(1).spriteVariant());
        assertEquals(1, runtime.snapshot().slots().get(2).spriteVariant());

        runtime.tick(16);
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(0, runtime.snapshot().slots().get(1).spriteVariant());
        assertEquals(0, runtime.snapshot().slots().get(2).spriteVariant());
    }

    @Test
    void ghiniUsesItsRomTargetTimersAccelerationAndZCorrection() {
        EntitySpriteDefinition definition = pairDefinition(0x12, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x12, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 96, 96, sequence(0x00, 0x00));
        assertEquals(0x20, runtime.ghiniTransitionCountdown(0));
        assertEquals(0x18, runtime.ghiniPrivateCountdown1(0));
        assertEquals(0, runtime.ghiniTargetXDirection(0));
        assertEquals(0, runtime.ghiniTargetYDirection(0));
        assertEquals(1, runtime.ghiniSpeedX(0));
        assertEquals(1, runtime.ghiniSpeedY(0));
        assertEquals(1, runtime.snapshot().slots().get(0).z());

        runtime.tick(4, 96, 96, sequence(0x00, 0x00));
        assertEquals(2, runtime.ghiniSpeedX(0));
        assertEquals(2, runtime.ghiniSpeedY(0));
        assertEquals(2, runtime.snapshot().slots().get(0).z());
    }

    @Test
    void ghiniUsesTheRomNormalEnemyContactAndHealthValues() {
        EntitySpriteDefinition definition = pairDefinition(0x12, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x12, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, firstHit.size());
        assertEquals(0x07, runtime.enemyHealth(0));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void ghiniTakesEachTargetDirectionFromTheSameRandomByteAsItsTimer() {
        EntitySpriteDefinition definition = pairDefinition(0x12, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x12, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 96, 96, sequence(0x1F, 0x0F));

        assertEquals(0x3F, runtime.ghiniTransitionCountdown(0));
        assertEquals(1, runtime.ghiniTargetXDirection(0));
        assertEquals(0x1F, runtime.ghiniPrivateCountdown1(0));
        assertEquals(1, runtime.ghiniTargetYDirection(0));
        assertEquals(0xFF, runtime.ghiniSpeedX(0));
        assertEquals(0xFF, runtime.ghiniSpeedY(0));
    }

    @Test
    void ghiniFamilyCombatRulesUseRomHealthDamageAndHitboxEntries() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);

        for (int type : new int[] {0x10, 0x11, 0x12}) {
            assertTrue(RoomEntityCombatRules.supportsEnemyCollision(type));
            assertEquals(0x08, RoomEntityCombatRules.initialHealth(type));
            assertEquals(tables.initialHealth(type), RoomEntityCombatRules.initialHealth(type));
            assertEquals(0x08, RoomEntityCombatRules.contactDamage(type));
            assertEquals(tables.contactDamage(type), RoomEntityCombatRules.contactDamage(type));
        }

        RoomEntity normal = ghiniEntity(catalog, 0, 0x10, EntityStatus.ACTIVE);
        RoomEntity giant = ghiniEntity(catalog, 1, 0x11, EntityStatus.ACTIVE);
        assertFalse(RoomEntityCombatRules.overlapsLink(normal, 0x5D, 0x50));
        assertTrue(RoomEntityCombatRules.overlapsLink(giant, 0x5D, 0x50));
        assertFalse(RoomEntityCombatRules.overlapsSword(normal, 0x62, 1, 0x58, 1));
        assertTrue(RoomEntityCombatRules.overlapsSword(giant, 0x62, 1, 0x58, 1));
    }

    @Test
    void hidingAndGiantGhinisStartHiddenAndSkipGroundAndCombat() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);

        for (int type : new int[] {0x10, 0x11}) {
            RoomEntityRuntime runtime = RoomEntityRuntime.from(
                snapshot(ghiniEntity(catalog, 0, type, EntityStatus.INIT)), false,
                sequence(0x00), catalog, tables);
            List<Integer> groundCalls = new ArrayList<>();
            runtime.setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) -> {
                groundCalls.add(entity.slot());
                return RoomEntityGroundInteraction.Result.unchanged(entity, previousStatus);
            });

            runtime.tick(0, 0x90, 0x90, 0, sequence(0x00));

            RoomEntity afterInit = runtime.snapshot().slots().get(0);
            assertEquals(EntityStatus.ACTIVE, afterInit.status());
            assertEquals(-1, afterInit.spriteVariant(), "type=0x"
                + Integer.toHexString(type));
            assertTrue(groundCalls.isEmpty());
            assertTrue(runtime.resolveCombat(1, 0x50, 0x50, false, true,
                true, 0x58, 1, 0x58, 1).isEmpty());
        }
    }

    @Test
    void collisionTypeIsExplicitPerTickAndRevealKeepsHiddenPresentationForThatTick()
        throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);

        RoomEntityRuntime compatibilityRuntime = RoomEntityRuntime.from(
            snapshot(ghiniEntity(catalog, 0, 0x10, EntityStatus.ACTIVE)), false,
            sequence(0x00), catalog, tables);
        compatibilityRuntime.tick(0, 0x50, 0x50, sequence(0x00));
        assertEquals(-1, compatibilityRuntime.snapshot().slots().get(0).spriteVariant());

        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            snapshot(ghiniEntity(catalog, 0, 0x10, EntityStatus.ACTIVE)), false,
            sequence(0x00), catalog, tables);
        RoomEntity before = runtime.snapshot().slots().get(0);
        runtime.tick(0, 0x50, 0x50, 0x08, sequence(0x00));

        RoomEntity revealTick = runtime.snapshot().slots().get(0);
        assertEquals(-1, revealTick.spriteVariant());
        assertEquals(before.x(), revealTick.x());
        assertEquals(before.y(), revealTick.y());
        assertTrue(runtime.resolveCombat(1, 0x50, 0x50, false, true,
            true, 0x58, 1, 0x58, 1).isEmpty());

        runtime.tick(1, 0x50, 0x50, 0, sequence(0x00));
        RoomEntity visible = runtime.snapshot().slots().get(0);
        assertTrue(visible.spriteVariant() >= 0);
        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 0x50, 0x50, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());
    }

    @Test
    void typedGhiniPresentationReachesNormalFlipAndGiantRectangleOutput() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);

        RoomEntityRuntime normalRuntime = RoomEntityRuntime.from(
            snapshot(ghiniEntity(catalog, 0, 0x12, EntityStatus.ACTIVE)), false,
            sequence(0x00), catalog, tables);
        normalRuntime.tick(0, 0x90, 0x90, 0, sequence(0x00));
        assertNotEquals(0, normalRuntime.snapshot().slots().get(0).entityFlipAttribute() & 0x20);

        RoomEntityRuntime giantRuntime = RoomEntityRuntime.from(
            snapshot(ghiniEntity(catalog, 0, 0x11, EntityStatus.ACTIVE)), false,
            sequence(0x00), catalog, tables);
        giantRuntime.tick(0, 0x50, 0x50, 0x01, sequence(0x00));
        giantRuntime.tick(1, 0x90, 0x90, 0, sequence(0x00));
        RoomEntity giant = giantRuntime.snapshot().slots().get(0);
        assertEquals(2, giant.spriteVariant());
        assertEquals(0, giant.entityFlipAttribute() & 0x20);

    }

    @Test
    void allGhiniTypesUseTheSharedRecoilPath() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EnemyAttackContext nonDamaging = new EnemyAttackContext(0, false, false, false, false);

        for (int type : new int[] {0x10, 0x11, 0x12}) {
            RoomEntityRuntime runtime = RoomEntityRuntime.from(
                snapshot(ghiniEntity(catalog, 0, type, EntityStatus.ACTIVE)), false,
                sequence(0x00), catalog, tables);
            if (type != 0x12) {
                runtime.tick(0, 0x50, 0x50, 0x01, sequence(0x00));
            }
            runtime.tick(1, 0x90, 0x90, 0, sequence(0x00));

            RoomEntity current = runtime.snapshot().slots().get(0);
            int linkX = (current.x() + 0x08) & 0xFF;
            int linkY = (current.y() - current.z() + 0x08) & 0xFF;
            List<EntityCombatEvent> events = runtime.resolveCombat(
                0, linkX, linkY, false, false, true, linkX, 1, linkY, 1,
                nonDamaging);
            assertEquals(1, events.size(), "type=0x" + Integer.toHexString(type));
            assertTrue(runtime.enemyRecoilActive(0), "type=0x"
                + Integer.toHexString(type));
            assertEquals(0xD0, runtime.enemyRecoilSpeedX(0), "type=0x"
                + Integer.toHexString(type));
            assertEquals(0xD0, runtime.enemyRecoilSpeedY(0), "type=0x"
                + Integer.toHexString(type));
        }
    }

    @Test
    void visibleGiantUsesTheExistingLethalCombatStatusPath() throws IOException {
        byte[] rom = romWithSwordResult(0x11, 0x08);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            snapshot(ghiniEntity(catalog, 0, 0x11, EntityStatus.ACTIVE)), false,
            sequence(0x00), catalog, tables);

        runtime.tick(0, 0x50, 0x50, 0x01, sequence(0x00));
        runtime.tick(1, 0x90, 0x90, 0, sequence(0x00));
        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 0x90, 0x90, false, false, true, 0x58, 1, 0x58, 1,
            EnemyAttackContext.standard());

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(0, runtime.enemyHealth(0));
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void GhiniOptionsDisableGroundInteractionAndWallCollisionForEveryVariant()
        throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            ghiniEntity(catalog, 0, 0x10, EntityStatus.ACTIVE),
            ghiniEntity(catalog, 1, 0x11, EntityStatus.ACTIVE),
            ghiniEntity(catalog, 2, 0x12, EntityStatus.ACTIVE)), false,
            sequence(0x00), catalog, tables);
        List<Integer> groundCalls = new ArrayList<>();
        runtime.setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) -> {
            groundCalls.add(entity.slot());
            return RoomEntityGroundInteraction.Result.unchanged(entity, previousStatus);
        });

        runtime.tick(0, 0x90, 0x90, 0, sequence(0x00));

        assertTrue(groundCalls.isEmpty());
        for (int slot = 0; slot < 3; slot++) {
            assertEquals(0x11, runtime.options1(slot));
        }
    }

    @Test
    void hardHatUsesTheRomTargetVectorAndFourFrameSpeedRefresh() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x20, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 80, 72, sequence(0x00));
        assertEquals(1, runtime.hardHatSpeedX(0));
        assertEquals(1, runtime.hardHatSpeedY(0));
        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());

        for (int frame = 1; frame <= 4; frame++) {
            runtime.tick(frame, 80, 72, sequence(0x00));
        }
        assertEquals(2, runtime.hardHatSpeedX(0));
        assertEquals(2, runtime.hardHatSpeedY(0));
    }

    @Test
    void hardHatUsesItsRomNormalEnemyHealthAndContactValues() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x20, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, firstHit.size());
        assertEquals(0x03, runtime.enemyHealth(0));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void hardHatSwordHitConfiguresAndAppliesTheRomSharedRecoil() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x20, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 72, 72, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertEquals(0x03, runtime.enemyHealth(0));
        assertEquals(0x18, runtime.enemyFlashCountdown(0));
        assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.enemyRecoilActive(0));
        assertEquals(0xD0, runtime.enemyRecoilSpeedX(0));
        assertEquals(0xD0, runtime.enemyRecoilSpeedY(0));

        runtime.tick(1, 72, 72, sequence(0x00));

        RoomEntity afterRecoil = runtime.snapshot().slots().get(0);
        assertEquals(61, afterRecoil.x());
        assertEquals(61, afterRecoil.y());
        assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
    }

    @Test
    void hardHatSharedRecoilKeepsStateWhenTheRomBackgroundPathBlocksIt() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x20, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        runtime.resolveCombat(0, 72, 72, false, true, true, 72, 1, 72, 1);
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) ->
            direction == 1 || direction == 2;

        runtime.tick(1, 72, 72, sequence(0x00), wall);

        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());
        assertTrue(runtime.enemyRecoilActive(0));
        assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
    }

    @Test
    void bankSixNormalEnemiesConfigureTheRomSharedSwordRecoil() {
        int[] types = {0x19, 0x0D, 0x15, 0x1A, 0x16, 0x17, 0x1B, 0x1C};
        for (int type : types) {
            EntitySpriteDefinition definition = pairDefinition(type, 3);
            RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
                new RoomEntity(0, 0, type, 64, 64, EntityStatus.ACTIVE, definition, 0)));

            List<EntityCombatEvent> events = runtime.resolveCombat(
                0, 72, 72, false, true, true, 72, 1, 72, 1);

            assertEquals(1, events.size(), "type=" + Integer.toHexString(type));
            assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0),
                "type=" + Integer.toHexString(type));
            assertTrue(runtime.enemyRecoilActive(0),
                "type=" + Integer.toHexString(type));
            assertEquals(0xD0, runtime.enemyRecoilSpeedX(0),
                "type=" + Integer.toHexString(type));
            assertEquals(0xD0, runtime.enemyRecoilSpeedY(0),
                "type=" + Integer.toHexString(type));
        }
    }

    @Test
    void zolAndGelApplyBankSixRecoilBeforeTheirStateMovement() throws IOException {
        int[] types = {0x1B, 0x1C};
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        for (int type : types) {
            EntitySpriteDefinition definition = pairDefinition(type, 2);
            // ConfigureEntityRecoil runs before the ROM damage lookup. A
            // level-zero context keeps the one-health entity active so the
            // following handler tick can observe that recoil step.
            EnemyAttackContext nonDamaging =
                new EnemyAttackContext(0, false, false, false, false);
            RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
                new RoomEntity(0, 0, type, 64, 64, EntityStatus.ACTIVE, definition, 0)),
                false, sequence(0x01), null, tables);

            runtime.resolveCombat(0, 120, 120, false, false,
                true, 72, 1, 72, 1, nonDamaging);
            runtime.tick(1, 72, 72, sequence(0x01));

            assertEquals(61, runtime.snapshot().slots().get(0).x(),
                "type=" + Integer.toHexString(type));
            assertEquals(61, runtime.snapshot().slots().get(0).y(),
                "type=" + Integer.toHexString(type));
            assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0),
                "type=" + Integer.toHexString(type));
        }
    }

    @Test
    void zolAndGelKeepBankSixRecoilWhenBackgroundBlocksTheStep() throws IOException {
        int[] types = {0x1B, 0x1C};
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        for (int type : types) {
            EntitySpriteDefinition definition = pairDefinition(type, 2);
            EnemyAttackContext nonDamaging =
                new EnemyAttackContext(0, false, false, false, false);
            RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
                new RoomEntity(0, 0, type, 64, 64, EntityStatus.ACTIVE, definition, 0)),
                false, sequence(0x01), null, tables);

            runtime.resolveCombat(0, 120, 120, false, false,
                true, 72, 1, 72, 1, nonDamaging);
            RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) ->
                direction == 1 || direction == 2;

            runtime.tick(1, 72, 72, sequence(0x01), wall);

            assertEquals(64, runtime.snapshot().slots().get(0).x(),
                "type=" + Integer.toHexString(type));
            assertEquals(64, runtime.snapshot().slots().get(0).y(),
                "type=" + Integer.toHexString(type));
            assertTrue(runtime.enemyRecoilActive(0),
                "type=" + Integer.toHexString(type));
            assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0),
                "type=" + Integer.toHexString(type));
        }
    }

    @Test
    void tektiteAppliesBankSixRecoilBeforeItsOrdinaryMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x0D, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        runtime.resolveCombat(0, 72, 72, false, true, true, 72, 1, 72, 1);
        runtime.tick(1, 72, 72, sequence(0x00));

        assertEquals(61, runtime.snapshot().slots().get(0).x());
        assertEquals(61, runtime.snapshot().slots().get(0).y());
        assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
    }

    @Test
    void armosWakesOnLinkCollisionThenChargesForTheRomCountdown() {
        EntitySpriteDefinition definition = pairDefinition(0x0F, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0F, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 64, 64, sequence(0x00));
        assertEquals(1, runtime.armosState(0));
        assertEquals(0x30, runtime.armosTransitionCountdown(0));
        assertEquals(0, runtime.armosSpeedX(0));
        assertEquals(0, runtime.armosSpeedY(0));

        runtime.tick(1, 120, 120, sequence(0x00));
        assertEquals(0x2F, runtime.armosTransitionCountdown(0));
        assertEquals(0xF8, runtime.armosSpeedX(0));
        assertEquals(0, runtime.armosSpeedY(0));

        for (int frame = 2; frame <= 0x30; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertEquals(2, runtime.armosState(0));
        assertEquals(0, runtime.armosTransitionCountdown(0));
        assertEquals(0, runtime.armosSpeedX(0));
        assertEquals(0, runtime.armosSpeedY(0));
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void armosUsesRomActivationFlagsAndOnlyTheActiveStateJoinsCombat() {
        EntitySpriteDefinition definition = pairDefinition(0x0F, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x0F, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        assertEquals(0x92, runtime.physicsFlags(0));
        assertTrue(runtime.resolveCombat(1, 64, 64, false, true,
            true, 72, 1, 72, 1).isEmpty());

        runtime.tick(0, 64, 64, sequence(0x00));
        assertEquals(1, runtime.armosState(0));
        assertEquals(0x18, runtime.enemyFlashCountdown(0));
        assertEquals(0x92, runtime.physicsFlags(0));
        assertTrue(runtime.resolveCombat(1, 64, 64, false, true,
            true, 72, 1, 72, 1).isEmpty());

        for (int frame = 1; frame <= 0x30; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }

        assertEquals(2, runtime.armosState(0));
        assertEquals(0x12, runtime.physicsFlags(0));

        RoomEntity active = runtime.snapshot().slots().get(0);
        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, active.x(), active.y(), false, true,
            false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x10, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword = runtime.resolveCombat(
            0, 0, 0, true, true,
            true, active.x() + 8, 1, active.y() + 8, 1);
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(1, sword.get(0).enemyDamage());
        assertEquals(3, runtime.enemyHealth(0));
        assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.enemyRecoilActive(0));

        runtime.tick(0x31, 0, 0, sequence(0x00));
        assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.enemyRecoilActive(0));
    }

    @Test
    void armosStateTwoLoadsItsContiguousRomSpeedTables() {
        EntitySpriteDefinition definition = pairDefinition(0x0F, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0F, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 64, 64, sequence(0x00));
        for (int frame = 1; frame <= 0x30; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        runtime.tick(0x31, 120, 120, sequence(0x04));

        assertEquals(2, runtime.armosState(0));
        assertEquals(0x24, runtime.armosTransitionCountdown(0));
        assertEquals(0, runtime.armosSpeedX(0));
        assertEquals(0x08, runtime.armosSpeedY(0));
    }

    @Test
    void tektiteLandsWithTheRomCountdownThenStartsItsNextJump() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0D, 64, 64, EntityStatus.ACTIVE, definition, 0, 0, 0, 0x80));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x00);

        runtime.tick(0, 120, 120, randomBytes);
        assertEquals(1, runtime.tektiteState(0));
        assertEquals(0x10, runtime.tektiteTransitionCountdown(0));
        assertEquals(0, runtime.snapshot().slots().get(0).z());
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());

        for (int frame = 1; frame <= 16; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }
        assertEquals(1, runtime.tektiteState(0));
        assertEquals(0, runtime.tektiteTransitionCountdown(0));

        for (int frame = 17; frame <= 31; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }
        runtime.tick(32, 120, 120, randomBytes);
        assertEquals(0, runtime.tektiteState(0));
        assertEquals(0x10, runtime.tektiteSpeedZ(0));
        assertEquals(0x10, runtime.tektiteSpeedX(0));
        assertEquals(0x10, runtime.tektiteSpeedY(0));
        assertEquals(1, runtime.snapshot().slots().get(0).z());
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void tektiteReversesAndHalvesSpeedXAfterHorizontalWallCollision() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x0D, 64, 64, EntityStatus.ACTIVE,
                definition, 0, 0, 0, 0x80)));
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x00);

        for (int frame = 0; frame <= 32; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }
        assertEquals(0, runtime.tektiteState(0));
        assertEquals(0x10, runtime.tektiteSpeedX(0));
        assertEquals(0x10, runtime.tektiteSpeedY(0));

        RoomEntityBackgroundCollision rightWall =
            (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(33, 120, 120, randomBytes, rightWall);

        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(65, runtime.snapshot().slots().get(0).y());
        assertEquals(0xF8, runtime.tektiteSpeedX(0));
        assertEquals(0x10, runtime.tektiteSpeedY(0));
    }

    @Test
    void tektiteVerticalWallCollisionUsesTheRomXSpeedHelper() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x0D, 64, 64, EntityStatus.ACTIVE,
                definition, 0, 0, 0, 0x80)));
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x00);

        for (int frame = 0; frame <= 32; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }
        RoomEntityBackgroundCollision downWall =
            (entity, direction, nextX, nextY) -> direction == 3;
        runtime.tick(33, 120, 120, randomBytes, downWall);

        assertEquals(65, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());
        assertEquals(0xF8, runtime.tektiteSpeedX(0));
        assertEquals(0x10, runtime.tektiteSpeedY(0));
    }

    @Test
    void tektiteAddsTheRomLandingTimerBaseAfterMaskingRandomness() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0D, 64, 64, EntityStatus.ACTIVE, definition, 0, 0, 0, 0x80));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 120, 120, sequence(0x1F));

        assertEquals(0x2F, runtime.tektiteTransitionCountdown(0));
    }

    @Test
    void tektiteCanReplaceItsRandomJumpDirectionWithTheRomLinkVector() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0D, 64, 64, EntityStatus.ACTIVE, definition, 0, 0, 0, 0x80));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x01);
        runtime.tick(0, 120, 120, randomBytes);
        for (int frame = 1; frame <= 32; frame++) {
            runtime.tick(frame, 80, 72, randomBytes);
        }

        assertEquals(0x14, runtime.tektiteSpeedX(0));
        // GetEntityYDistanceToLink includes the post-launch Z position, so
        // dy is 9 here and the ROM divide loop yields 11.
        assertEquals(0x0B, runtime.tektiteSpeedY(0));
    }

    @Test
    void tektiteUsesTheHealthGroupOneContactAndSwordValues() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0D, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, firstHit.size());
        assertEquals(0x01, runtime.enemyHealth(0));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void leeverUsesTheRomHideEmergeAndChaseStateBoundaries() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0E, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        IntSupplier randomBytes = sequence(0x00);

        runtime.tick(0, 120, 120, randomBytes);
        assertEquals(1, runtime.leeverState(0));
        assertEquals(0x1F, runtime.leeverTransitionCountdown(0));
        assertEquals(-1, runtime.snapshot().slots().get(0).spriteVariant());

        for (int frame = 1; frame <= 30; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }
        assertEquals(1, runtime.leeverState(0));
        assertEquals(1, runtime.leeverTransitionCountdown(0));
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(31, 120, 120, randomBytes);
        assertEquals(2, runtime.leeverState(0));
        assertEquals(0x70, runtime.leeverTransitionCountdown(0));

        runtime.tick(32, 80, 64, randomBytes);
        assertEquals(0x08, runtime.leeverSpeedX(0));
        assertEquals(0, runtime.leeverSpeedY(0));
        assertEquals(2, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void leeverInitializationAppliesTheRomHiddenSpriteVariantBeforeItsFirstHandlerPass() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0E, 64, 64, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 120, 120, sequence(0x00));

        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(-1, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(0, runtime.leeverState(0));
        assertEquals(0, runtime.leeverTransitionCountdown(0));
    }

    @Test
    void leeverBurrowsAfterItsChaseTimerAndStartsTheRomHidingWindow() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0E, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        IntSupplier randomBytes = sequence(0x00);

        for (int frame = 0; frame <= 143; frame++) {
            runtime.tick(frame, 120, 64, randomBytes);
        }
        assertEquals(3, runtime.leeverState(0));
        assertEquals(0x1F, runtime.leeverTransitionCountdown(0));
        assertEquals(0, runtime.leeverSpeedX(0));
        assertEquals(0, runtime.leeverSpeedY(0));

        for (int frame = 144; frame <= 173; frame++) {
            runtime.tick(frame, 120, 64, randomBytes);
        }
        runtime.tick(174, 120, 64, randomBytes);
        assertEquals(0, runtime.leeverState(0));
        assertEquals(0x30, runtime.leeverTransitionCountdown(0));
        assertEquals(0x08, runtime.leeverSpeedX(0));
        assertEquals(0, runtime.leeverSpeedY(0));

        runtime.tick(175, 120, 64, randomBytes);
        assertEquals(-1, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void leeverOnlyUsesItsHealthGroupOneCombatValuesWhileChasing() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x0E, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        IntSupplier randomBytes = sequence(0x00);

        List<EntityCombatEvent> hiddenContact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertTrue(hiddenContact.isEmpty());

        for (int frame = 0; frame <= 31; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }
        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, sword.size());
        assertEquals(1, runtime.enemyHealth(0));
    }

    @Test
    void leeverSwordHitConfiguresBankFourRecoilBeforeItsStateMovement() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x0E, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        for (int frame = 0; frame <= 31; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertEquals(2, runtime.leeverState(0));

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x03, events.get(0).soundId());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemyHealth(0));
        assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.enemyRecoilActive(0));
        assertEquals(0xD0, runtime.enemyRecoilSpeedX(0));
        assertEquals(0xD0, runtime.enemyRecoilSpeedY(0));

        runtime.tick(1, 120, 120, sequence(0x00));

        RoomEntity afterRecoil = runtime.snapshot().slots().get(0);
        assertEquals(61, afterRecoil.x());
        assertEquals(61, afterRecoil.y());
        assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
    }

    @Test
    void leeverKeepsBankFourRecoilWhenBackgroundBlocksTheStep() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x0E, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        for (int frame = 0; frame <= 31; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertEquals(2, runtime.leeverState(0));

        runtime.resolveCombat(0, 72, 72, false, true, true, 72, 1, 72, 1);
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) ->
            direction == 1 || direction == 2;

        runtime.tick(1, 120, 120, sequence(0x00), wall);

        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());
        assertTrue(runtime.enemyRecoilActive(0));
        assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
    }

    @Test
    void leeverRestoresItsPositionWhenTheRomBackgroundHelperBlocksRightwardMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x0E, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        for (int frame = 0; frame <= 31; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        runtime.tick(32, 80, 64, sequence(0x00));
        assertEquals(2, runtime.leeverState(0));
        assertEquals(0x08, runtime.leeverSpeedX(0));

        RoomEntityBackgroundCollision rightWall =
            (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(33, 80, 64, sequence(0x00), rightWall);
        runtime.tick(34, 80, 64, sequence(0x00), rightWall);

        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(0x08, runtime.leeverSpeedX(0));
        assertEquals(2, runtime.leeverState(0));
    }

    @Test
    void leeverAdvancesItsPositionWhenTheBackgroundDoesNotBlock() {
        EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x0E, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        for (int frame = 0; frame <= 31; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        runtime.tick(32, 80, 64, sequence(0x00));
        runtime.tick(33, 80, 64, sequence(0x00));
        runtime.tick(34, 80, 64, sequence(0x00));

        assertEquals(65, runtime.snapshot().slots().get(0).x());
        assertEquals(0x08, runtime.leeverSpeedX(0));
    }

    @Test
    void peaHatRunsTheRomRestTakeoffAndHeightAnimationStates() {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0xA0, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        IntSupplier randomBytes = sequence(0x00);

        runtime.tick(0, 120, 120, randomBytes);
        assertEquals(1, runtime.peaHatState(0));
        assertEquals(0, runtime.peaHatPrivateState1(0));
        assertEquals(0, runtime.snapshot().slots().get(0).z());
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        for (int frame = 1; frame <= 128; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }
        assertEquals(1, runtime.peaHatState(0));
        assertEquals(8, runtime.peaHatPrivateState1(0));

        runtime.tick(129, 120, 120, randomBytes);
        assertEquals(2, runtime.peaHatState(0));
        assertEquals(0x80, runtime.peaHatSlowTransitionCountdown(0));

        for (int frame = 130; frame <= 136; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }
        assertEquals(1, runtime.snapshot().slots().get(0).z());
    }

    @Test
    void peaHatUsesTheRomPhaseTablesAfterReachingItsMaximumHeight() {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0xA0, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        IntSupplier randomBytes = sequence(0x00);

        for (int frame = 0; frame <= 280; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }

        assertEquals(2, runtime.peaHatState(0));
        assertEquals(0x10, runtime.snapshot().slots().get(0).z());
        assertEquals(0, runtime.peaHatPrivateState4(0));
        assertEquals(0x07, runtime.peaHatSpeedX(0));
        assertEquals(0, runtime.peaHatSpeedY(0));

        for (int frame = 281; frame <= 304; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }

        assertEquals(0x0F, runtime.peaHatPrivateState4(0));
        assertEquals(0x06, runtime.peaHatSpeedX(0));
        assertEquals(0xFD, runtime.peaHatSpeedY(0));
    }

    @Test
    void peaHatIsCombatVulnerableOnlyWhileGrounded() {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0xA0, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> grounded = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, grounded.size());
        assertEquals(0x04, grounded.get(0).linkDamage());

        runtime.tick(0, 120, 120, sequence(0x00));
        List<EntityCombatEvent> airborne = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertTrue(airborne.isEmpty());
    }

    @Test
    void peaHatSwordHitConfiguresBankSevenRecoilBeforeItsStateMovement() throws IOException {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        EnemyAttackContext nonDamaging =
            new EnemyAttackContext(0, false, false, false, false);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xA0, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            false, sequence(0x01), null, new RomEnemyCombatTables(loadRom()));

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, false, true, 72, 1, 72, 1, nonDamaging);

        assertEquals(1, events.size());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.enemyRecoilActive(0));
        assertEquals(0xD0, runtime.enemyRecoilSpeedX(0));
        assertEquals(0xD0, runtime.enemyRecoilSpeedY(0));

        runtime.tick(1, 72, 72, sequence(0x01));

        RoomEntity afterRecoil = runtime.snapshot().slots().get(0);
        assertEquals(61, afterRecoil.x());
        assertEquals(61, afterRecoil.y());
        assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
    }

    @Test
    void peaHatKeepsBankSevenRecoilWhenBackgroundBlocksTheStep() throws IOException {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        EnemyAttackContext nonDamaging =
            new EnemyAttackContext(0, false, false, false, false);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xA0, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            false, sequence(0x01), null, new RomEnemyCombatTables(loadRom()));

        runtime.resolveCombat(0, 120, 120, false, false,
            true, 72, 1, 72, 1, nonDamaging);
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) ->
            direction == 1 || direction == 2;

        runtime.tick(1, 72, 72, sequence(0x01), wall);

        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());
        assertTrue(runtime.enemyRecoilActive(0));
        assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
    }

    @Test
    void peaHatRestoresItsPositionWhenTheRomBackgroundHelperBlocksRightwardMotion() {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0xA0, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime blocked = RoomEntityRuntime.from(initial);
        RoomEntityRuntime unblocked = RoomEntityRuntime.from(initial);
        IntSupplier blockedRandom = sequence(0x00);
        IntSupplier unblockedRandom = sequence(0x00);

        for (int frame = 0; frame <= 280; frame++) {
            blocked.tick(frame, 120, 120, blockedRandom);
            unblocked.tick(frame, 120, 120, unblockedRandom);
        }
        RoomEntity phaseStart = blocked.snapshot().slots().get(0);
        assertEquals(2, blocked.peaHatState(0));
        assertEquals(0x07, blocked.peaHatSpeedX(0));

        RoomEntityBackgroundCollision rightWall =
            (entity, direction, nextX, nextY) -> direction == 0;
        for (int frame = 281; frame <= 304; frame++) {
            blocked.tick(frame, 120, 120, blockedRandom, rightWall);
            unblocked.tick(frame, 120, 120, unblockedRandom);
        }

        RoomEntity blockedEntity = blocked.snapshot().slots().get(0);
        RoomEntity unblockedEntity = unblocked.snapshot().slots().get(0);
        assertEquals(phaseStart.x(), blockedEntity.x());
        assertTrue(unblockedEntity.x() > blockedEntity.x());
        assertEquals(unblockedEntity.y(), blockedEntity.y());
        assertEquals(unblocked.peaHatState(0), blocked.peaHatState(0));
        assertEquals(unblocked.peaHatSpeedX(0), blocked.peaHatSpeedX(0));
        assertEquals(unblocked.peaHatSpeedY(0), blocked.peaHatSpeedY(0));
    }

    @Test
    void peaHatUsesTheRomSwordClinkPathWhileAirborne() {
        EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xA0, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        runtime.tick(0, 120, 120, sequence(0x00));
        assertEquals(1, runtime.peaHatState(0));

        List<EntityCombatEvent> events = runtime.resolveCombat(
            1, 120, 120, false, false, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        EntityCombatEvent event = events.get(0);
        assertEquals(0xA0, event.type());
        assertTrue(event.swordHit());
        assertEquals(0, event.linkDamage());
        assertEquals(0, event.enemyDamage());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, event.soundChannel());
        assertEquals(0x07, event.soundId());
        assertEquals(new EntityCombatEvent.SwordPokeVfx(0x40, 0x40),
            event.swordPokeVfx());
        assertEquals(1, runtime.enemyHealth(0));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0x10, runtime.enemyIgnoreHitsCountdown(0));
        assertFalse(runtime.enemyRecoilActive(0));
    }

    @Test
    void stalfosAggressiveUsesTheRomPursuitAndJumpArc() {
        EntitySpriteDefinition definition = pairDefinition(0x1A, 3);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x1A, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 64, 64, sequence(0x00));
        assertEquals(1, runtime.stalfosState(0));
        assertEquals(0, runtime.snapshot().slots().get(0).z());

        runtime.tick(1, 64, 64, sequence(0x00));
        assertEquals(2, runtime.stalfosState(0));
        assertEquals(0x28, runtime.stalfosSpeedZ(0));
        assertEquals(0x10, runtime.stalfosSpeedX(0));
        assertEquals(0x10, runtime.stalfosSpeedY(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(2, 64, 64, sequence(0x00));
        assertEquals(0x02, runtime.snapshot().slots().get(0).z());
        assertEquals(0x26, runtime.stalfosSpeedZ(0));
        assertEquals(2, runtime.snapshot().slots().get(0).spriteVariant());

        for (int frame = 3; frame <= 21; frame++) {
            runtime.tick(frame, 64, 64, sequence(0x00));
        }
        assertEquals(3, runtime.stalfosState(0));
        assertEquals(0x10, runtime.stalfosTransitionCountdown(0));
        assertEquals(0xC0, runtime.stalfosSpeedZ(0));
        assertEquals(2, runtime.snapshot().slots().get(0).spriteVariant());

        int frame = 22;
        while (runtime.stalfosState(0) != 0 && frame < 100) {
            runtime.tick(frame++, 64, 64, sequence(0x00));
        }
        assertEquals(0, runtime.stalfosState(0));
        assertEquals(0, runtime.snapshot().slots().get(0).z());
        assertEquals(0x20, runtime.stalfosTransitionCountdown(0));
        assertEquals(0, runtime.stalfosSpeedZ(0));
    }

    @Test
    void stalfosAggressiveUsesHealthGroupTwoACombatValues() {
        EntitySpriteDefinition definition = pairDefinition(0x1A, 3);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x1A, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword = runtime.resolveCombat(
            1, 72, 72, false, true, true, 72, 1, 72, 1);
        assertEquals(1, sword.size());
        assertEquals(1, runtime.enemyHealth(0));
    }

    @Test
    void gibdoRunsTheRomInitDirectionChoiceAndFixedPointWalk() {
        EntitySpriteDefinition definition = pairDefinition(0x1F, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x1F, 64, 64, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        // EntityInitHandlersTable._1F only increments the state. The first
        // handler pass therefore takes the forced-direction path.
        runtime.tick(0, 120, 120, sequence(0xFF));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(1, runtime.gibdoState(0));
        assertEquals(0, runtime.gibdoSpeedX(0));
        assertEquals(0, runtime.gibdoSpeedY(0));

        // LikeLikeGibdoSpeeds[2] is $F8 (-8 pixels per 16 frames).
        runtime.tick(1, 120, 120, sequence(0x02));
        assertEquals(0, runtime.gibdoState(0));
        assertEquals(0xF8, runtime.gibdoSpeedX(0));
        assertEquals(0, runtime.gibdoSpeedY(0));
        assertEquals(64, runtime.snapshot().slots().get(0).x());

        // A nonzero random low six bits keeps the selected direction and the
        // ROM fixed-point update moves the entity left by one pixel here.
        runtime.tick(2, 120, 120, sequence(0x01));
        assertEquals(63, runtime.snapshot().slots().get(0).x());
        assertEquals(0xF8, runtime.gibdoSpeedX(0));
    }

    @Test
    void likeLikeRunsTheRomInitDirectionChoiceAndFixedPointWalk() {
        EntitySpriteDefinition definition = pairDefinition(0x23, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x23, 64, 64, EntityStatus.INIT, definition, 0)));

        runtime.tick(0, 120, 120, sequence(0xFF));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(1, runtime.likeLikeWalkState(0));

        // LikeLikeGibdoSpeeds[1] is $08 (+8 pixels per 16 frames).
        runtime.tick(1, 120, 120, sequence(0x00, 0x01));
        assertEquals(0, runtime.likeLikeWalkState(0));
        assertEquals(0x08, runtime.likeLikeWalkSpeedY(0));
        assertEquals(64, runtime.snapshot().slots().get(0).y());

        runtime.tick(2, 120, 120, sequence(0xFF));
        runtime.tick(3, 120, 120, sequence(0xFF));
        assertEquals(65, runtime.snapshot().slots().get(0).y());
    }

    @Test
    void likeLikeCapturesLinkOnTheRomCollisionCadenceWithoutContactDamage() {
        EntitySpriteDefinition definition = pairDefinition(0x23, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x23, 64, 64, EntityStatus.INIT, definition, 0)));

        runtime.tick(0, 64, 64, sequence(0xFF));
        runtime.tickWithProjectileEvents(1, 64, 64, sequence(0x00, 0x00), null,
            new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0));
        runtime.tickWithProjectileEvents(2, 64, 64, sequence(0x00, 0x00), null,
            new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0));

        List<RoomEntityRuntime.LikeLikeEvent> events =
            runtime.consumePendingLikeLikeEvents();
        assertEquals(1, events.size());
        assertEquals(RoomEntityRuntime.LikeLikeEvent.Kind.CAPTURE, events.getFirst().kind());
        assertEquals(-1, events.getFirst().stolenInventorySlot());
        assertEquals(1, runtime.likeLikeState(0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());

        assertTrue(runtime.resolveCombat(3, 64, 64, false, true,
            true, 72, 1, 72, 1).isEmpty());
        assertEquals(2, runtime.enemyHealth(0));
    }

    @Test
    void likeLikeStealsTheBShieldFirstAndRefusesToStealTheLevelTwoShield() {
        EntitySpriteDefinition definition = pairDefinition(0x23, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x23, 64, 64, EntityStatus.INIT, definition, 0)));
        runtime.setLikeLikeLinkInventoryForTest(0x04, 0x04);
        runtime.tick(0, 64, 64, sequence(0xFF));
        runtime.tickWithProjectileEvents(1, 64, 64, sequence(0x00), null,
            new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0));
        runtime.consumePendingLikeLikeEvents();

        runtime.tickWithProjectileEvents(2, 64, 64, sequence(0x00), null,
            new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0));
        RoomEntityRuntime.LikeLikeEvent stolen =
            runtime.consumePendingLikeLikeEvents().getFirst();
        assertEquals(RoomEntityRuntime.LikeLikeEvent.Kind.CAPTURE, stolen.kind());
        assertEquals(0, stolen.stolenInventorySlot());
        assertEquals(1, stolen.stolenShieldLevel());
        assertEquals(1, runtime.likeLikePrivateState1(0));

        RoomEntityRuntime levelTwo = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x23, 64, 64, EntityStatus.INIT, definition, 0)));
        levelTwo.setLikeLikeLinkInventoryForTest(0x00, 0x04);
        levelTwo.tick(0, 64, 64, sequence(0xFF));
        levelTwo.tickWithProjectileEvents(1, 64, 64, sequence(0x00), null,
            new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 2, 0));
        levelTwo.consumePendingLikeLikeEvents();
        levelTwo.tickWithProjectileEvents(2, 64, 64, sequence(0x00), null,
            new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 2, 0));
        RoomEntityRuntime.LikeLikeEvent protectedShield =
            levelTwo.consumePendingLikeLikeEvents().getFirst();
        assertEquals(-1, protectedShield.stolenInventorySlot());
        assertEquals(0, levelTwo.likeLikePrivateState1(0));
    }

    @Test
    void likeLikeReleasesLinkAfterEightHeldActionFramesAndStartsTheRomSlowTimer() {
        EntitySpriteDefinition definition = pairDefinition(0x23, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x23, 64, 64, EntityStatus.INIT, definition, 0)));
        runtime.tick(0, 64, 64, sequence(0xFF));
        runtime.tickWithProjectileEvents(1, 64, 64, sequence(0x00), null,
            new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0));
        runtime.consumePendingLikeLikeEvents();

        runtime.setActionButtonsHeld(true);
        for (int frame = 2; frame <= 8; frame++) {
            runtime.tickWithProjectileEvents(frame, 64, 64, sequence(0x00), null,
                new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0));
            runtime.consumePendingLikeLikeEvents();
        }
        assertEquals(1, runtime.likeLikeState(0));
        assertEquals(7, runtime.likeLikeInertia(0));

        runtime.tickWithProjectileEvents(9, 64, 64, sequence(0x00), null,
            new EnemyProjectileCollision.LinkState(64, 64, 0, 0, 0, false, 1, 0));
        RoomEntityRuntime.LikeLikeEvent release =
            runtime.consumePendingLikeLikeEvents().getFirst();
        assertEquals(RoomEntityRuntime.LikeLikeEvent.Kind.RELEASE, release.kind());
        assertEquals(0, runtime.likeLikeState(0));
        assertEquals(0x15, runtime.slowTransitionCountdown(0));
    }

    @Test
    void goombaUsesTheRomNormalRoomRandomWalkAndAnimation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x9F, 64, 64, EntityStatus.INIT, definition, 0)),
            true, null, catalog, tables);

        // The room init is a noop for Goomba; its first active handler pass
        // chooses the random-walk interval and direction.
        runtime.tick(0, 120, 120, sequence(0x00));
        runtime.tick(1, 120, 120, sequence(0x00, 0x00));

        assertEquals(1, runtime.goombaState(0));
        assertEquals(0x30, runtime.goombaTransitionCountdown(0));
        assertEquals(0x08, runtime.goombaSpeedX(0));
        assertEquals(0x00, runtime.goombaSpeedY(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(2, 120, 120, sequence(0xFF));
        assertEquals(0x2F, runtime.goombaTransitionCountdown(0));
        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(0x02, runtime.goombaSpeedY(0));

        runtime.tick(0x10, 120, 120, sequence(0xFF));
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void goombaUsesRomHealthGroupZeroContactAndSwordValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x9F, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            true, null, catalog, tables);

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 72, 72, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword = runtime.resolveCombat(
            2, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0, runtime.enemyHealth(0));
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void goombaStompUsesTheRomDescendingVelocityBranchAndSharedDeathHandler()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x9F, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            true, null, catalog, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            1, 72, 72, true, 0xF0, true, false, 0, 0, 0, 0);
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN,
            events.get(0).linkAction());
        assertEquals(EntityCombatEvent.SoundChannel.WAVE, events.get(0).soundChannel());
        assertEquals(0x0E, events.get(0).soundId());
        assertEquals(2, runtime.goombaState(0));
        assertEquals(0x30, runtime.goombaTransitionCountdown(0));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());

        for (int frame = 0; frame < 0x30; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
        assertEquals(0x2D, runtime.droppedItemForTest(0));
        assertEquals(0x0C, runtime.dyingCountdown(0));
        assertEquals(0x04, runtime.physicsFlags(0));
        assertEquals(2, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void goombaDoesNotStompWhileLinkIsAscending() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x9F, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            true, null, catalog, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            1, 72, 72, true, 0x10, true, false, 0, 0, 0, 0);
        assertTrue(events.isEmpty());
        assertEquals(0, runtime.goombaState(0));
    }

    @Test
    void goombaSideScrollStompUsesTheRomHorizontalBounceAction() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x9F, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x9F, 64, 64, EntityStatus.ACTIVE, definition, 0))
            .withSideScrolling(true);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            initial, true, null, catalog, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            1, 72, 72, true, 0x10, true, false, 0, 0, 0, 0);
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING,
            events.get(0).linkAction());
        assertEquals(2, runtime.goombaState(0));

        for (int frame = 0; frame < 0x30; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void snakeUsesTheRomInitCountdownRandomWalkAndAnimation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0xA1, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xA1, 64, 64, EntityStatus.INIT, definition, 0)),
            true, null, catalog, tables);

        runtime.tick(0, 120, 120, sequence(0x00));
        runtime.tick(1, 120, 120, sequence(0x00));

        assertEquals(1, runtime.snakeState(0));
        assertEquals(0x30, runtime.snakeTransitionCountdown(0));
        assertEquals(0x2F, runtime.snakePrivateCountdown1(0));
        assertEquals(0x08, runtime.snakeSpeedX(0));
        assertEquals(0x00, runtime.snakeSpeedY(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(2, 120, 120, sequence(0xFF));
        assertEquals(0x2F, runtime.snakeTransitionCountdown(0));
        assertEquals(0x2E, runtime.snakePrivateCountdown1(0));
        assertEquals(64, runtime.snapshot().slots().get(0).x());

        runtime.tick(8, 120, 120, sequence(0xFF));
        assertEquals(2, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void snakeStartsTheRomAxisDashWhenItsPrivateTimerExpires() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0xA1, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xA1, 64, 64, EntityStatus.INIT, definition, 0)),
            true, null, catalog, tables);

        runtime.tick(0, 120, 120, sequence(0x00));
        for (int frame = 1; frame <= 48; frame++) {
            runtime.tick(frame, 64, 120, sequence(0x02));
        }

        assertEquals(2, runtime.snakeState(0));
        assertEquals(0x30, runtime.snakeTransitionCountdown(0));
        assertEquals(0x00, runtime.snakeSpeedX(0));
        assertEquals(0x10, runtime.snakeSpeedY(0));
        assertEquals(0, runtime.snakePrivateCountdown1(0));
    }

    @Test
    void snakeBackgroundCollisionResetsTheRomStateAndPrivateRecoveryWindow()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0xA1, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xA1, 64, 64, EntityStatus.INIT, definition, 0)),
            true, null, catalog, tables);

        runtime.tick(0, 120, 120, sequence(0x00));
        runtime.tick(1, 120, 120, sequence(0x00));
        runtime.tick(2, 120, 120, sequence(0x00));
        runtime.tick(3, 120, 120, sequence(0x00), (entity, direction, nextX, nextY) -> true);

        assertEquals(0, runtime.snakeState(0));
        assertEquals(0x08, runtime.snakeTransitionCountdown(0));
        assertEquals(0x20, runtime.snakePrivateCountdown1(0));
        assertEquals(0x00, runtime.snakeSpeedX(0));
        assertEquals(0x00, runtime.snakeSpeedY(0));
        assertEquals(64, runtime.snapshot().slots().get(0).x());
    }

    @Test
    void snakeUsesHealthGroupZeroContactAndSwordValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0xA1, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0xA1, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            true, null, catalog, tables);

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 72, 72, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword = runtime.resolveCombat(
            2, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0, runtime.enemyHealth(0));
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void spikedBeetleUsesTheRomRestWalkAndAnimationStateMachine() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x2C, 64, 64, EntityStatus.INIT, definition, 0)),
            true, null, catalog, tables);

        runtime.tick(0, 120, 80, sequence(0x00));
        runtime.tick(1, 120, 80, sequence(0x00, 0x00));

        assertEquals(1, runtime.spikedBeetleState(0));
        assertEquals(0x30, runtime.spikedBeetleTransitionCountdown(0));
        assertEquals(0x06, runtime.spikedBeetleSpeedX(0));
        assertEquals(0x00, runtime.spikedBeetleSpeedY(0));
        assertEquals(0x12, runtime.physicsFlags(0));
        assertEquals(0x48, runtime.options1(0));
        assertEquals(0x80, runtime.hitboxFlagsForTest(0));

        for (int frame = 2; frame <= 0x31; frame++) {
            runtime.tick(frame, 120, 80, sequence(0xFF));
        }
        assertEquals(0, runtime.spikedBeetleState(0));
        assertEquals(0x18, runtime.spikedBeetleTransitionCountdown(0));
        assertEquals(0x06, runtime.spikedBeetleSpeedX(0));
        assertEquals(0x00, runtime.spikedBeetleSpeedY(0));

        runtime.tick(0x40, 120, 64, sequence(0xFF));
        assertEquals(1, runtime.spikedBeetleState(0));
        assertEquals(0xFF, runtime.spikedBeetleTransitionCountdown(0));
        runtime.tick(0x41, 120, 64, sequence(0xFF));
        assertEquals(2, runtime.spikedBeetleState(0));
        assertEquals(0xFF, runtime.spikedBeetleTransitionCountdown(0));
        assertEquals(0x00, runtime.spikedBeetleSpeedX(0));
        assertEquals(0x00, runtime.spikedBeetleSpeedY(0));
    }

    @Test
    void spikedBeetleRestingKeepsTheLastHandlerSelectedAnimation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x2C, 64, 64, EntityStatus.INIT, definition, 0)),
            true, null, catalog, tables);

        runtime.tick(0, 120, 80, sequence(0x00));
        runtime.tick(0xE1, 120, 80, sequence(0x00, 0x00));
        for (int frame = 0xE2; frame != 0x12; frame = (frame + 1) & 0xFF) {
            runtime.tick(frame, 120, 80, sequence(0xFF));
        }

        assertEquals(0, runtime.spikedBeetleState(0));
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(0x20, 120, 80, sequence(0xFF));
        runtime.tick(0x21, 120, 80, sequence(0xFF));
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void spikedBeetleFlipsOnTheInitialRomSwordPassAndDoesNotTakeDamage()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x2C, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            true, null, catalog, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x09, events.get(0).soundId());
        assertEquals(3, runtime.spikedBeetleState(0));
        assertEquals(0xFF, runtime.spikedBeetleTransitionCountdown(0));
        assertEquals(0x10, runtime.spikedBeetleSpeedX(0));
        assertEquals(0x00, runtime.spikedBeetleSpeedY(0));
        assertEquals(0x02, runtime.enemyHealth(0));

        runtime.tick(1, 120, 120, sequence(0x00));
        assertEquals(0x08, runtime.options1(0));
        assertEquals(0x00, runtime.hitboxFlagsForTest(0));
    }

    @Test
    void spikedBeetleNormalHandlerUsesTheRomSwordPokeOptions() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x2C, 64, 64, EntityStatus.INIT, definition, 0)),
            true, null, catalog, tables);

        runtime.tick(0, 120, 80, sequence(0x00));
        runtime.tick(1, 120, 80, sequence(0x00, 0x00));
        List<EntityCombatEvent> events = runtime.resolveCombat(
            2, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(0, events.get(0).enemyDamage());
        assertEquals(new EntityCombatEvent.SwordPokeVfx(64, 64),
            events.get(0).swordPokeVfx());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x07, events.get(0).soundId());
        assertEquals(1, runtime.spikedBeetleState(0));
        assertEquals(0x02, runtime.enemyHealth(0));

        runtime.tick(2, 120, 80, sequence(0xFF));
        assertEquals(0, runtime.spikedBeetleSpeedX(0));
        assertEquals(0, runtime.spikedBeetleSpeedY(0));
    }

    @Test
    void spikedBeetleUsesTheRomHealthAndContactDamageValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x2C, EntityRoomLoader.RoomTable.INDOORS_A, 0x00);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x2C, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            true, null, catalog, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            1, 72, 72, false, true, false, 0, 0, 0, 0);

        assertEquals(1, events.size());
        assertEquals(0x04, events.get(0).linkDamage());
    }

    @Test
    void polsVoiceUsesTheRomJumpLoopAndAnimation() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x18, 64, 64, EntityStatus.INIT, definition, 0)),
            true, null, catalog, tables);

        runtime.tick(0, 120, 80, sequence(0x00));
        runtime.tick(1, 120, 80, sequence(0x00, 0x00));

        assertEquals(1, runtime.polsVoiceState(0));
        assertEquals(0x10, runtime.polsVoiceSpeedZ(0));
        assertEquals(0x08, runtime.polsVoiceSpeedX(0));
        assertEquals(0xFC, runtime.polsVoiceSpeedY(0));
        assertEquals(0x12, runtime.physicsFlags(0));
        assertEquals(0x08, runtime.options1(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(2, 120, 80, sequence(0xFF));
        assertEquals(1, runtime.snapshot().slots().get(0).z());
        assertEquals(0x0F, runtime.polsVoiceSpeedZ(0));

        int frame = 3;
        while (runtime.polsVoiceState(0) == 1 && frame < 80) {
            runtime.tick(frame, 120, 80, sequence(0x03));
            frame++;
        }
        assertEquals(2, runtime.polsVoiceState(0));
        assertEquals(0, runtime.snapshot().slots().get(0).z());
        assertEquals(0, runtime.polsVoiceSpeedX(0));
        assertEquals(0, runtime.polsVoiceSpeedY(0));
        assertEquals(0x1B, runtime.polsVoiceTransitionCountdown(0));

        runtime.tick(frame, 120, 80, sequence(0xFF));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(frame + 1, 120, 80, sequence(0xFF));
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void polsVoiceVectorBranchMatchesTheRomZeroDistanceResult() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x18, 64, 64, EntityStatus.INIT, definition, 0)),
            true, null, catalog, tables);

        runtime.tick(0, 64, 64, sequence(0x00));
        runtime.tick(1, 64, 64, sequence(0x00, 0x06));

        // GetVectorTowardsLink's repeated-remainder loop increments both
        // components when both source distances are zero.
        assertEquals(0x0A, runtime.polsVoiceSpeedX(0));
        assertEquals(0x0A, runtime.polsVoiceSpeedY(0));
    }

    @Test
    void polsVoiceForcesTheRomIgnoreHitsByteOnlyAroundBackgroundInteraction()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x18, 64, 64, EntityStatus.INIT, definition, 0)),
            true, null, catalog, tables);
        List<Integer> observedIgnoreHits = new ArrayList<>();
        runtime.setBackgroundInteraction(new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                           int nextX, int nextY) {
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }

            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                       int nextX, int nextY,
                                                       int ignoreHitsCountdown) {
                observedIgnoreHits.add(ignoreHitsCountdown);
                return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
            }
        });

        runtime.tick(0, 120, 80, sequence(0x00));
        runtime.tick(1, 120, 80, sequence(0x00, 0x00));
        runtime.tick(2, 120, 80, sequence(0xFF));

        assertTrue(observedIgnoreHits.contains(0x01));
        assertEquals(0, runtime.enemyIgnoreHitsCountdown(0));
    }

    @Test
    void polsVoiceUsesTheRomBalladOcarinaDeathPreamble() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x18, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            true, null, catalog, tables);
        runtime.setOcarinaPlaybackForTest(0x01, 0x04, 0x00);

        runtime.tick(0, 120, 80, sequence(0x00));

        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
        assertEquals(0x1F, runtime.dyingCountdown(0));
        assertEquals(0x04, runtime.physicsFlags(0));
        assertEquals(List.of(new EntityCombatEvent(0, 0x18, 0, false,
            EntityCombatEvent.SoundChannel.NOISE, 0x13)),
            runtime.consumePendingEntityEvents());
    }

    @Test
    void ocarinaAnimationSpawnsAndAdvancesTheRomMusicalNoteEntity() {
        byte[] rom = syntheticRom();
        write(rom, 0x05, 0x7EF8, 0x0E, 0x13);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            snapshot(), true, null, catalog);
        runtime.setOcarinaPlaybackForTest(0x20, 0x01, 0x00);
        runtime.setOcarinaAnimationForTest(0x14, 0x00);

        runtime.tick(0, 0x40, 0x50, sequence(0x00));

        RoomEntity note = runtime.snapshot().loadedEntities().stream()
            .filter(entity -> entity.type() == 0xC9)
            .findFirst().orElseThrow();
        assertEquals(0x48, note.x());
        assertEquals(0x48, note.y());
        assertEquals(0x05, note.spriteDefinition().bank());
        assertEquals(0x7EF8, note.spriteDefinition().address());
        assertEquals(0x40, runtime.musicalNoteInertiaForTest(note.slot()));

        runtime.tick(1, 0x40, 0x50, sequence(0x00));

        RoomEntity movedNote = runtime.snapshot().slots().get(note.slot());
        assertEquals(0x4E, movedNote.x());
        assertEquals(0x44, movedNote.y());
        assertEquals(0x3F, runtime.musicalNoteInertiaForTest(note.slot()));
    }

    @Test
    void polsVoiceUsesTheRomHealthAndContactDamageValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x18, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x18, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            true, null, catalog, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            1, 72, 72, false, true, false, 0, 0, 0, 0);

        assertEquals(1, events.size());
        assertEquals(0x08, events.get(0).linkDamage());
        assertEquals(0x04, runtime.enemyHealth(0));
    }

    @Test
    void wizrobeRunsTheRomRevealStateMachineAndLaunchesItsProjectile() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x21, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x21, 0x40, 0x50, EntityStatus.INIT, definition, 0)),
            true, null, catalog, tables);

        runtime.tick(0, 0x80, 0x50, sequence(0x00));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0x80, runtime.transitionCountdown(0));
        assertEquals(-1, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(0x02, runtime.physicsFlags(0));

        for (int frame = 1; frame <= 0x80; frame++) {
            runtime.tick(frame, 0x80, 0x50, sequence(0x00));
        }
        assertEquals(1, runtime.wizrobeState(0));
        assertEquals(0x20, runtime.wizrobePrivateCountdown1(0));
        assertEquals(0x01, runtime.wizrobePrivateState1(0));
        assertEquals(-1, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.setWizrobeStateForTest(0, 2, 0, 1, 2, 0);
        runtime.tick(0x81, 0x80, 0x50, sequence(0x00));
        assertEquals(3, runtime.wizrobeState(0));
        assertEquals(0x01, runtime.wizrobePrivateState1(0));
        assertEquals(0x01, runtime.wizrobePrivateCountdown1(0));

        runtime.tick(0x82, 0x80, 0x50, sequence(0x00));
        assertEquals(0x40, runtime.wizrobePrivateCountdown1(0));
        assertEquals(0, runtime.wizrobeDirection(0));
        assertEquals(1, runtime.wizrobeNextSpriteVariant(0));

        runtime.setWizrobeStateForTest(0, 3, 0, 0xFF, 0x29, 0);
        runtime.tick(0x83, 0x80, 0x50, sequence(0x00));
        assertEquals(0x28, runtime.wizrobePrivateCountdown1(0));
        RoomEntity projectile = runtime.snapshot().slots().get(15);
        assertEquals(EntityStatus.ACTIVE, projectile.status());
        assertEquals(-1, projectile.sourceLoadOrder());
        assertEquals(0x22, projectile.type());
        assertEquals(0x48, projectile.x());
        assertEquals(0x50, projectile.y());
        assertEquals(0, projectile.spriteVariant());
        assertEquals(0x42, runtime.physicsFlags(15));
        assertEquals(0x02, runtime.physicsFlags(0));
        assertEquals(0x20, runtime.wizrobeProjectileSpeedX(15));
        assertEquals(0x00, runtime.wizrobeProjectileSpeedY(15));
    }

    @Test
    void wizrobeProjectileUsesRomPaletteFlipMovementAndCollisionLifecycle() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x22, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, -1, 0x22, 0x3F, 0x50, EntityStatus.ACTIVE, definition, 0)),
            true, null, catalog, tables);
        runtime.setWizrobeProjectileForTest(0, 0x20, 0x00, 0);

        List<EntityProjectileEvent> events = runtime.tickWithProjectileEvents(
            4, 0x40, 0x50, sequence(0x00), null,
            new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0x00, 0, false));

        assertEquals(1, events.size());
        assertEquals(EntityProjectileEvent.Kind.LINK_DAMAGE, events.getFirst().kind());
        assertEquals(0x08, events.getFirst().linkDamage());
        assertTrue(events.getFirst().remove());
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void wizrobeUsesHealthGroupTwelveContactAndSwordValues() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x21, EntityRoomLoader.RoomTable.INDOORS_A);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x21, 0x40, 0x50, EntityStatus.ACTIVE, definition, 0)),
            true, null, catalog, tables);
        runtime.setWizrobeStateForTest(0, 3, 0, 0xFF, 0x28, 0);

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 0x48, 0x58, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword = runtime.resolveCombat(
            2, 0x80, 0x80, false, true, true, 0x48, 1, 0x58, 1);
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        // Health group $0C's sword rows are zero in Data_003_43EC; Wizrobe
        // contact damage is $08, but a normal sword hit is ignored.
        assertEquals(4, runtime.enemyHealth(0));
        assertEquals(0, sword.get(0).enemyDamage());
        assertEquals(0, RoomEntityCombatRules.basicSwordDamage(0x21));
    }

    @Test
    void evasiveStalfosUsesTheRomRandomWalkAndNormalAnimation() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x1E, 64, 64, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 120, 120, sequence(0x01));
        for (int frame = 1; frame <= 4; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x01));
        }

        assertEquals(65, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());

        runtime.tick(8, 120, 120, sequence(0x01));
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void evasiveStalfosJumpsAwayFromHeldActionButtonsAndLandsWithRomState() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x1E, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        runtime.setActionButtonsHeld(true);
        runtime.tick(0, 80, 64, sequence(0x00));

        assertEquals(1, runtime.evasiveInertia(0));
        assertEquals(0x08, runtime.transitionCountdown(0));
        assertEquals(0x15, runtime.evasiveSpeedZ(0));
        assertEquals(0xEE, runtime.evasiveSpeedX(0));
        assertEquals(0x00, runtime.evasiveSpeedY(0));
        assertEquals(2, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());

        runtime.setActionButtonsHeld(false);
        runtime.tick(1, 80, 64, sequence(0x00));
        assertEquals(62, runtime.snapshot().slots().get(0).x());
        assertEquals(1, runtime.snapshot().slots().get(0).z());
        assertEquals(0x14, runtime.evasiveSpeedZ(0));

        int frame = 2;
        while (runtime.evasiveInertia(0) != 0 && frame < 100) {
            runtime.tick(frame++, 80, 64, sequence(0x00));
        }

        assertEquals(0, runtime.evasiveInertia(0));
        assertEquals(0, runtime.snapshot().slots().get(0).z());
        assertEquals(0x08, runtime.evasiveSpeedX(0));
        assertEquals(0x08, runtime.evasiveSpeedY(0));
        assertEquals(0x10, runtime.evasivePrivateCountdown1(0));
    }

    @Test
    void evasiveStalfosClonesInAnglersTunnelWithRomAttributesAndVector() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x1E, 0x40, 0x50, EntityStatus.ACTIVE, definition, 0,
                0, 0, 0x03)));
        runtime.setEntityMapIdForTest(0x03);
        // AnimateEntities decrements private countdowns before dispatching the
        // handler; raw value two therefore enters the source's ==1 branch.
        runtime.setEvasivePrivateCountdown1ForTest(0, 0x02);

        runtime.tick(0, 0x70, 0x50, sequence(0x01));

        RoomEntity clone = runtime.snapshot().slots().get(15);
        assertEquals(EntityStatus.ACTIVE, clone.status());
        assertEquals(-1, clone.sourceLoadOrder());
        assertEquals(0x1E, clone.type());
        assertEquals(0x40, clone.x());
        assertEquals(0x50, clone.y());
        assertEquals(0x03, clone.z());
        assertEquals(1, runtime.evasivePrivateState1(15));
        assertEquals(0x52, runtime.physicsFlags(15));
        assertEquals(0x1A, runtime.options1(15));
        assertEquals(0x00, runtime.enemyIgnoreHitsCountdown(15));
        // GetVectorTowardsLink($18) sees dx=$30 and dy=$03 (source Z is
        // included in the Y distance), producing X=$18, Y=$01.
        assertEquals(0x18, runtime.evasiveSpeedX(15));
        assertEquals(0x01, runtime.evasiveSpeedY(15));
        List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE, events.getFirst().soundChannel());
        assertEquals(0x0A, events.getFirst().soundId());
    }

    @Test
    void evasiveStalfosCloneSkipsGenericGroundInteractionButKeepsFleeingMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x1E, 0x40, 0x50, EntityStatus.ACTIVE, definition, 0)));
        runtime.setEntityMapIdForTest(0x03);
        runtime.setEvasivePrivateCountdown1ForTest(0, 0x02);
        List<Integer> groundInteractionSlots = new ArrayList<>();
        runtime.setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) -> {
            groundInteractionSlots.add(entity.slot());
            return RoomEntityGroundInteraction.Result.unchanged(entity, previousStatus);
        });

        runtime.tick(0, 0x70, 0x50, sequence(0x01));
        groundInteractionSlots.clear();
        runtime.tick(1, 0x70, 0x50, sequence(0x01));

        assertFalse(groundInteractionSlots.contains(15));
    }

    @Test
    void evasiveStalfosCloneIsGatedBelowAnglersTunnelAndDoesNotWhoosh() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x1E, 0x40, 0x50, EntityStatus.ACTIVE, definition, 0)));
        runtime.setEntityMapIdForTest(0x02);
        runtime.setEvasivePrivateCountdown1ForTest(0, 0x02);

        runtime.tick(0, 0x70, 0x50, sequence(0x01));

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(15).status());
        assertEquals(0x01, runtime.evasivePrivateCountdown1(0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void evasiveStalfosCloneDoesNotEmitWhooshWhenAllSlotsAreOccupied() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 3);
        List<RoomEntity> entities = new ArrayList<>();
        entities.add(new RoomEntity(0, 0, 0x1E, 0x40, 0x50, EntityStatus.ACTIVE,
            definition, 0));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            entities.add(new RoomEntity(slot, -1, 0xFF, 0, 0, EntityStatus.ACTIVE,
                EntitySpriteDefinition.unsupported(0xFF), -1));
        }
        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(entities));
        runtime.setEntityMapIdForTest(0x03);
        runtime.setEvasivePrivateCountdown1ForTest(0, 0x02);

        runtime.tick(0, 0x70, 0x50, sequence(0x01));

        assertEquals(0x1E, runtime.snapshot().slots().get(0).type());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void fleeingEvasiveStalfosPokesAndClearsOnBackgroundCollision() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x1E, 0x40, 0x50, EntityStatus.ACTIVE, definition, 0,
                0, 0, 0x02)));
        runtime.setEvasiveFleeingForTest(0, 0x10, 0);
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 0;

        runtime.tick(0, 0x70, 0x50, sequence(0x00), wall);

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
        List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x07, events.getFirst().soundId());
        assertEquals(List.of(new RoomEntityRuntime.TransientVfxRequest(
            TransientVfxType.SWORD_POKE, 0x40, 0x4E)), runtime.transientVfxRequests());
    }

    @Test
    void fleeingEvasiveStalfosClearsAtTheRomScreenEdgeWithoutSwordPoke() {
        EntitySpriteDefinition definition = pairDefinition(0x1E, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x1E, 0xA7, 0x50, EntityStatus.ACTIVE, definition, 0)));
        runtime.setEvasiveFleeingForTest(0, 0x10, 0);

        runtime.tick(0, 0x70, 0x50, sequence(0x00));

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        assertTrue(runtime.transientVfxRequests().isEmpty());
    }

    @Test
    void gibdoFallsBackToTheRomVerticalSpeedTableWhenHorizontalSpeedIsZero() {
        EntitySpriteDefinition definition = pairDefinition(0x1F, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x1F, 64, 64, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0, 120, 120, sequence(0x00));
        runtime.tick(1, 120, 120, sequence(0x00, 0x01));

        assertEquals(0, runtime.gibdoSpeedX(0));
        assertEquals(0x08, runtime.gibdoSpeedY(0));
        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());
    }

    @Test
    void gibdoReversesTheMovingAxisWhenTheBackgroundQueryBlocksIt() {
        EntitySpriteDefinition definition = pairDefinition(0x1F, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x1F, 64, 64, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        runtime.tick(0, 120, 120, sequence(0xFF));
        runtime.tick(1, 120, 120, sequence(0x02));

        RoomEntityBackgroundCollision leftWall = (entity, direction, nextX, nextY) -> direction == 1;
        runtime.tick(2, 120, 120, sequence(0x01), leftWall);

        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(0x08, runtime.gibdoSpeedX(0));
    }

    @Test
    void gibdoUsesHealthGroupTwoFCombatValues() {
        EntitySpriteDefinition definition = pairDefinition(0x1F, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x1F, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());
        assertEquals(0x06, runtime.enemyHealth(0));

        List<EntityCombatEvent> sword = runtime.resolveCombat(
            1, 72, 72, false, true, true, 72, 1, 72, 1);
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0x05, runtime.enemyHealth(0));
    }

    @Test
    void antiFairyUsesTheRomRandomSpeedVectorAndFrameAnimation() {
        EntitySpriteDefinition definition = pairDefinition(0x15, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x15, 64, 64, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        // EntityRandomSpeedX/Y[1] = (+12, -12). The init handler runs before
        // the first active handler pass.
        runtime.tick(0, 120, 120, sequence(0x01));
        assertEquals(0x0C, runtime.antiFairySpeedX(0));
        assertEquals(0xF4, runtime.antiFairySpeedY(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(1, 120, 120, sequence(0x00));
        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(63, runtime.snapshot().slots().get(0).y());

        runtime.tick(2, 120, 120, sequence(0x00));
        assertEquals(65, runtime.snapshot().slots().get(0).x());
        assertEquals(62, runtime.snapshot().slots().get(0).y());

        runtime.tick(8, 120, 120, sequence(0x00));
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void antiFairyReversesTheMovingAxisAtARomBackgroundCollisionPoint() {
        EntitySpriteDefinition definition = pairDefinition(0x15, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x15, 64, 64, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
        runtime.tick(0, 120, 120, sequence(0x02));

        RoomEntityBackgroundCollision leftWall = (entity, direction, nextX, nextY) -> direction == 1;
        runtime.tick(1, 120, 120, sequence(0x00), leftWall);

        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(0x0C, runtime.antiFairySpeedX(0));
        assertEquals(64, runtime.snapshot().slots().get(0).y());
    }

    @Test
    void antiFairyUsesHealthGroupSixCombatValues() {
        EntitySpriteDefinition definition = pairDefinition(0x15, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x15, 64, 64, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());
        assertEquals(0x04, runtime.enemyHealth(0));

        List<EntityCombatEvent> sword = runtime.resolveCombat(
            1, 72, 72, false, true, true, 72, 1, 72, 1);
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0x03, runtime.enemyHealth(0));
    }

    @Test
    void sparkInitializersSelectTheRomDirectionAndOffset() {
        EntitySpriteDefinition counterDefinition = pairDefinition(0x16, 2);
        RoomEntityRuntime counterClockwise = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x16, 64, 64, EntityStatus.INIT, counterDefinition, 0)));
        counterClockwise.tick(0);

        assertEquals(61, counterClockwise.snapshot().slots().get(0).y());
        assertEquals(0, counterClockwise.sparkPrivateState2(0));
        assertEquals(0, counterClockwise.sparkSpeedX(0));
        assertEquals(0, counterClockwise.sparkSpeedY(0));

        EntitySpriteDefinition clockwiseDefinition = pairDefinition(0x17, 2);
        RoomEntityRuntime clockwise = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x17, 64, 64, EntityStatus.INIT, clockwiseDefinition, 0)));
        clockwise.tick(0);

        assertEquals(67, clockwise.snapshot().slots().get(0).y());
        assertEquals(4, clockwise.sparkPrivateState2(0));
    }

    @Test
    void sparkFollowsTheRomEightDirectionTableAndFrameCadence() {
        EntitySpriteDefinition definition = pairDefinition(0x16, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x16, 64, 64, EntityStatus.INIT, definition, 0)));

        runtime.tick(0);
        runtime.tick(1, 120, 120, () -> 0);

        assertEquals(0, runtime.sparkPrivateState1(0));
        assertEquals(9, runtime.sparkTransitionCountdown(0));
        assertEquals(0x10, runtime.sparkSpeedX(0));
        assertEquals(0x00, runtime.sparkSpeedY(0));

        runtime.tick(2, 120, 120, () -> 0);
        assertEquals(65, runtime.snapshot().slots().get(0).x());
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void sparkUsesCollisionMaskToAdvanceItsRomDirectionIndex() {
        EntitySpriteDefinition definition = pairDefinition(0x16, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x16, 64, 64, EntityStatus.INIT, definition, 0)));
        runtime.tick(0);

        RoomEntityBackgroundCollision rightWall = (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(1, 120, 120, () -> 0, rightWall);

        assertEquals(1, runtime.sparkPrivateState1(0));
        assertEquals(0x00, runtime.sparkSpeedX(0));
        assertEquals(0x10, runtime.sparkSpeedY(0));
        assertEquals(0, runtime.sparkTransitionCountdown(0));
    }

    @Test
    void sparkUsesHealthGroupTwoCCombatValues() {
        EntitySpriteDefinition definition = pairDefinition(0x16, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x16, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x04, contact.get(0).linkDamage());
        assertEquals(0x01, runtime.enemyHealth(0));

        List<EntityCombatEvent> sword = runtime.resolveCombat(
            1, 72, 72, false, true, true, 72, 1, 72, 1);
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void zolUsesTheRomInchingAndLeapStateLoop() {
        EntitySpriteDefinition definition = pairDefinition(0x1B, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x1B, 64, 64, EntityStatus.INIT, definition, 0)),
            false, sequence(0x01));

        runtime.tick(0, 80, 64, sequence(0x01));
        assertEquals(2, runtime.enemyHealth(0));
        assertEquals(0, runtime.zolState(0));

        runtime.tick(1, 80, 64, sequence(0x01));
        assertEquals(1, runtime.zolState(0));
        assertEquals(7, runtime.zolTransitionCountdown(0));
        assertEquals(0x04, runtime.zolSpeedX(0));
        assertEquals(0x00, runtime.zolSpeedY(0));

        runtime.tick(2, 80, 64, sequence(0x01));
        assertEquals(6, runtime.zolTransitionCountdown(0));
        assertEquals(64, runtime.snapshot().slots().get(0).x());
    }

    @Test
    void zolSplittingTurnsTheOriginalSlotIntoAGelAndUsesTheLastFreeSlot() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition definition = pairDefinition(0x1B, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x1B, 64, 64, EntityStatus.INIT, definition, 0)),
            false, sequence(0x01), catalog);

        runtime.tick(0, 80, 64, sequence(0x01));
        List<EntityCombatEvent> sword = runtime.resolveCombat(
            1, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, sword.size());
        assertEquals(1, runtime.enemyHealth(0));
        assertEquals(0x7C11, runtime.snapshot().slots().get(0).spriteDefinition().address());

        for (int frame = 1; frame <= 16; frame++) {
            runtime.tick(frame, 80, 64, sequence(0x01));
        }

        RoomEntity originalSlot = runtime.snapshot().slots().get(0);
        RoomEntity spawnedSlot = runtime.snapshot().slots().get(15);
        assertEquals(0x1C, originalSlot.type());
        assertEquals(0x1C, spawnedSlot.type());
        assertEquals(EntityStatus.ACTIVE, spawnedSlot.status());
        assertEquals(0, originalSlot.sourceLoadOrder());
        assertEquals(0, spawnedSlot.sourceLoadOrder());
        assertEquals(originalSlot.x() + 12 & 0xFF, spawnedSlot.x());
        assertEquals(originalSlot.y(), spawnedSlot.y());
        assertEquals(originalSlot.z(), spawnedSlot.z());
        assertEquals(1, runtime.enemyHealth(15));
        assertFalse(runtime.enemyRecoilActive(0));
        assertFalse(runtime.enemyRecoilActive(15));
        assertEquals(0, runtime.enemyIgnoreHitsCountdown(0));
    }

    @Test
    void gelUsesTheRomSmallEnemyHitboxAndClingingCollisionState() {
        EntitySpriteDefinition definition = pairDefinition(0x1C, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x1C, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        assertTrue(runtime.resolveCombat(1, 72, 64, false, true,
            false, 0, 0, 0, 0).isEmpty());

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 68, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());
        assertEquals(4, runtime.zolState(0));
        assertTrue(runtime.resolveCombat(1, 68, 64, false, true,
            false, 0, 0, 0, 0).isEmpty());
    }

    @Test
    void hidingZolUsesTheRomProximityRevealAndRisePhases() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x9B, 64, 64, EntityStatus.INIT, definition, 0)),
            false, sequence(0x02));

        runtime.tick(0, 120, 120, sequence(0x02));
        assertEquals(0, runtime.hidingZolState(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(1, 80, 64, sequence(0x02));
        assertEquals(1, runtime.hidingZolState(0));
        assertEquals(0x20, runtime.hidingZolTransitionCountdown(0));
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());

        runtime.tick(2, 80, 64, sequence(0x02));
        assertEquals(0x1F, runtime.hidingZolTransitionCountdown(0));
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());

        for (int frame = 3; frame <= 18; frame++) {
            runtime.tick(frame, 80, 64, sequence(0x02));
        }
        assertEquals(0x0F, runtime.hidingZolTransitionCountdown(0));
        assertEquals(2, runtime.snapshot().slots().get(0).spriteVariant());

        for (int frame = 19; frame <= 33; frame++) {
            runtime.tick(frame, 80, 64, sequence(0x02));
        }
        assertEquals(2, runtime.hidingZolState(0));
        assertEquals(0, runtime.hidingZolTransitionCountdown(0));
        assertEquals(3, runtime.snapshot().slots().get(0).spriteVariant());
        assertEquals(8, runtime.snapshot().slots().get(0).z());
    }

    @Test
    void hidingZolUsesTheRomSwordAndLinkCollisionPhases() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
            .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x9B, 64, 64, EntityStatus.INIT, definition, 0)),
            false, sequence(0x00));

        runtime.tick(0, 80, 64, sequence(0x00));
        assertTrue(runtime.resolveCombat(1, 72, 72, false, true,
            true, 72, 1, 72, 1).isEmpty());

        int frame = 1;
        while (runtime.hidingZolState(0) != 3 && frame < 200) {
            runtime.tick(frame++, 80, 64, sequence(0x00));
        }
        assertEquals(3, runtime.hidingZolState(0));

        // The state-2 handler is the frame that enters state 3, so its
        // state-3 sword continuation has not run yet. Advance one handler
        // frame before checking state-3 collision behavior.
        runtime.tick(frame++, 80, 64, sequence(0x00));
        RoomEntity hidingZol = runtime.snapshot().slots().get(0);
        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, hidingZol.x() + 4, hidingZol.y() + 4, false, true,
            false, 0, 0, 0, 0);
        assertTrue(contact.isEmpty());

        List<EntityCombatEvent> sword = runtime.resolveCombat(
            1, 120, 120, false, true, true,
            hidingZol.x() + 8, 1, hidingZol.y() + 8, 1);
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void spikeTrapUsesTheRomRandomDirectionAndFourStateLaunchLoop() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x27, 64, 64, EntityStatus.INIT, definition, 0)));

        runtime.tick(0, 120, 120, sequence(0x00));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.spikeTrapState(0));
        assertEquals(0, runtime.spikeTrapDirection(0));

        runtime.tick(1, 80, 64, sequence(0x00));
        assertEquals(1, runtime.spikeTrapState(0));
        assertEquals(64, runtime.spikeTrapPrivateState1(0));
        assertEquals(64, runtime.spikeTrapPrivateState2(0));

        // Link is within the Y window, so state 1 chooses the right-facing
        // $20 X speed and immediately enters state 2.
        runtime.tick(2, 80, 64, sequence(0x00));
        assertEquals(2, runtime.spikeTrapState(0));
        assertEquals(0, runtime.spikeTrapDirection(0));
        assertEquals(0x20, runtime.spikeTrapSpeedX(0));
        assertEquals(0, runtime.spikeTrapSpeedY(0));
        assertEquals(0x18, runtime.spikeTrapTransitionCountdown(0));

        runtime.tick(3, 80, 64, sequence(0x00));
        assertEquals(66, runtime.snapshot().slots().get(0).x());
        assertEquals(0x17, runtime.spikeTrapTransitionCountdown(0));

        for (int frame = 4; frame <= 26; frame++) {
            runtime.tick(frame, 80, 64, sequence(0x00));
        }
        assertEquals(3, runtime.spikeTrapState(0));
        assertEquals(112, runtime.snapshot().slots().get(0).x());

        int frame = 27;
        while (runtime.spikeTrapState(0) != 1 && frame < 280) {
            runtime.tick(frame++, 80, 64, sequence(0x00));
        }
        assertEquals(1, runtime.spikeTrapState(0));
        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());
        assertEquals(0x20, runtime.spikeTrapTransitionCountdown(0));
    }

    @Test
    void spikeTrapLaunchPublishesOneRomWhooshNoise() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x27, 64, 64, EntityStatus.INIT, definition, 0)));

        runtime.tick(0, 120, 120, sequence(0x00));
        runtime.tick(1, 80, 64, sequence(0x00));
        runtime.tick(2, 80, 64, sequence(0x00));

        List<EntityCombatEvent> launchEvents = runtime.consumePendingEntityEvents();
        assertEquals(1, launchEvents.size());
        assertEquals(0, launchEvents.get(0).slot());
        assertEquals(0x27, launchEvents.get(0).type());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE,
            launchEvents.get(0).soundChannel());
        assertEquals(0x0A, launchEvents.get(0).soundId());

        runtime.tick(3, 80, 64, sequence(0x00));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void spikeTrapBlockedLaunchDoesNotPublishTheWhooshNoise() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x27, 64, 64, EntityStatus.INIT, definition, 0)));

        runtime.tick(0, 120, 120, sequence(0x00));
        runtime.tick(1, 80, 64, sequence(0x00));
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(2, 80, 64, sequence(0x00), wall);

        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void spikeTrapForwardEndpointPublishesOneRomSwordPokeJingle() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x27, 64, 64, EntityStatus.INIT, definition, 0)));

        runtime.tick(0, 120, 120, sequence(0x00));
        runtime.tick(1, 80, 64, sequence(0x00));
        runtime.tick(2, 80, 64, sequence(0x00));
        assertEquals(0x0A, runtime.consumePendingEntityEvents().getFirst().soundId());

        for (int frame = 3; frame <= 25; frame++) {
            runtime.tick(frame, 80, 64, sequence(0x00));
            assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        }

        runtime.tick(26, 80, 64, sequence(0x00));
        List<EntityCombatEvent> endpointEvents = runtime.consumePendingEntityEvents();
        assertEquals(1, endpointEvents.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE,
            endpointEvents.get(0).soundChannel());
        assertEquals(0x07, endpointEvents.get(0).soundId());
        assertEquals(3, runtime.spikeTrapState(0));

        runtime.tick(27, 80, 64, sequence(0x00));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void spikeTrapForwardBackgroundCollisionPublishesTheSameRomJingle() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x27, 64, 64, EntityStatus.INIT, definition, 0)));

        runtime.tick(0, 120, 120, sequence(0x00));
        runtime.tick(1, 80, 64, sequence(0x00));
        runtime.tick(2, 80, 64, sequence(0x00));
        assertEquals(0x0A, runtime.consumePendingEntityEvents().getFirst().soundId());

        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> true;
        runtime.tick(3, 80, 64, sequence(0x00), wall);

        List<EntityCombatEvent> collisionEvents = runtime.consumePendingEntityEvents();
        assertEquals(1, collisionEvents.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE,
            collisionEvents.get(0).soundChannel());
        assertEquals(0x07, collisionEvents.get(0).soundId());
        assertEquals(3, runtime.spikeTrapState(0));
    }

    @Test
    void spikeTrapFallsBackToTheRomVerticalLaunchTable() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x27, 64, 64, EntityStatus.INIT, definition, 0)));

        runtime.tick(0, 120, 120, sequence(0x02));
        runtime.tick(1, 64, 100, sequence(0x02));
        runtime.tick(2, 64, 100, sequence(0x02));

        assertEquals(2, runtime.spikeTrapState(0));
        assertEquals(3, runtime.spikeTrapDirection(0));
        assertEquals(0, runtime.spikeTrapSpeedX(0));
        assertEquals(0x20, runtime.spikeTrapSpeedY(0));
        assertEquals(0x10, runtime.spikeTrapTransitionCountdown(0));
    }

    @Test
    void spikeTrapBackgroundCollisionKeepsItInItsLaunchState() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x27, 64, 64, EntityStatus.INIT, definition, 0)));

        runtime.tick(0, 120, 120, sequence(0x00));
        runtime.tick(1, 80, 64, sequence(0x00));
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(2, 80, 64, sequence(0x00), wall);

        assertEquals(1, runtime.spikeTrapState(0));
        assertEquals(0, runtime.spikeTrapTransitionCountdown(0));
        assertEquals(0x20, runtime.spikeTrapSpeedX(0));
    }

    @Test
    void spikeTrapSwordCollisionPublishesTheRomSwordPokeWithoutDamage() {
        EntitySpriteDefinition definition = pairDefinition(0x27, 1);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x27, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        assertEquals(4, runtime.enemyHealth(0));
        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(8, contact.get(0).linkDamage());

        List<EntityCombatEvent> sword = runtime.resolveCombat(
            1, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, sword.size());
        EntityCombatEvent event = sword.get(0);
        assertTrue(event.swordHit());
        assertEquals(0, event.enemyDamage());
        assertEquals(-1, event.enemySpecialAction());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, event.soundChannel());
        assertEquals(0x07, event.soundId());
        assertEquals(new EntityCombatEvent.SwordPokeVfx(0x40, 0x40),
            event.swordPokeVfx());
        assertEquals(4, runtime.enemyHealth(0));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0x10, runtime.enemyIgnoreHitsCountdown(0));
        assertFalse(runtime.enemyRecoilActive(0));
    }

    @Test
    void waterTektiteUsesNoopInitializationAndTheRomFrameVariant() {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x99, 64, 64, EntityStatus.INIT, definition, 0)));
        AtomicInteger randomCalls = new AtomicInteger();
        IntSupplier randomBytes = () -> {
            randomCalls.incrementAndGet();
            return 0x00;
        };

        runtime.tick(0, 120, 120, randomBytes);
        assertEquals(0, randomCalls.get());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());

        runtime.tick(0x00, 120, 120, randomBytes);
        assertEquals(2, randomCalls.get());
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(0x10, 120, 120, randomBytes);
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void waterTektiteUsesTheRomThreeStateAccelerationAndSignedDecelerationLoop() {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x99, 64, 64, EntityStatus.INIT, definition, 0)));
        IntSupplier randomBytes = sequence(0x00, 0x02);

        runtime.tick(0, 120, 120, randomBytes);
        runtime.tick(1, 120, 120, randomBytes);
        assertEquals(1, runtime.waterTektiteState(0));
        assertEquals(0x20, runtime.waterTektiteTransitionCountdown(0));
        assertEquals(0xFF, runtime.waterTektitePrivateState1(0));
        assertEquals(0x01, runtime.waterTektitePrivateState2(0));

        runtime.tick(2, 120, 120, randomBytes);
        assertEquals(0, runtime.waterTektiteSpeedX(0));
        assertEquals(0, runtime.waterTektiteSpeedY(0));
        runtime.tick(3, 120, 120, randomBytes);
        assertEquals(0xFF, runtime.waterTektiteSpeedX(0));
        assertEquals(0x01, runtime.waterTektiteSpeedY(0));
        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());

        for (int frame = 4; frame <= 33; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }
        assertEquals(2, runtime.waterTektiteState(0));
        assertEquals(0xF1, runtime.waterTektiteSpeedX(0));
        assertEquals(0x0F, runtime.waterTektiteSpeedY(0));

        runtime.tick(34, 120, 120, randomBytes);
        assertEquals(0xF2, runtime.waterTektiteSpeedX(0));
        assertEquals(0x0E, runtime.waterTektiteSpeedY(0));
        runtime.tick(35, 120, 120, randomBytes);
        assertEquals(0xF2, runtime.waterTektiteSpeedX(0));
        assertEquals(0x0E, runtime.waterTektiteSpeedY(0));
        runtime.tick(36, 120, 120, randomBytes);
        assertEquals(0xF3, runtime.waterTektiteSpeedX(0));
        assertEquals(0x0D, runtime.waterTektiteSpeedY(0));
    }

    @Test
    void waterTektiteResetsToStateZeroWhenTheMovedPositionHitsBackground() {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x99, 64, 64, EntityStatus.INIT, definition, 0)));
        IntSupplier randomBytes = sequence(0x02);

        runtime.tick(0, 120, 120, randomBytes);
        for (int frame = 1; frame <= 33; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }
        assertEquals(2, runtime.waterTektiteState(0));
        int beforeX = runtime.snapshot().slots().get(0).x();
        int beforeY = runtime.snapshot().slots().get(0).y();
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> true;

        runtime.tick(34, 120, 120, randomBytes, wall);

        RoomEntity blocked = runtime.snapshot().slots().get(0);
        assertEquals(beforeX, blocked.x());
        assertEquals(beforeY, blocked.y());
        assertEquals(0, runtime.waterTektiteState(0));
        assertEquals(0x10, runtime.waterTektiteTransitionCountdown(0));
        assertEquals(0, runtime.waterTektiteSpeedX(0));
        assertEquals(0, runtime.waterTektiteSpeedY(0));
    }

    @Test
    void waterTektiteAppliesBankSevenRecoilBeforeItsStateMovement() throws IOException {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        EnemyAttackContext nonDamaging =
            new EnemyAttackContext(0, false, false, false, false);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x99, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            false, sequence(0x01), null, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, false, true, 72, 1, 72, 1, nonDamaging);

        assertEquals(1, events.size());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.enemyRecoilActive(0));
        assertEquals(0xD0, runtime.enemyRecoilSpeedX(0));
        assertEquals(0xD0, runtime.enemyRecoilSpeedY(0));

        runtime.tick(1, 72, 72, sequence(0x01));

        assertEquals(61, runtime.snapshot().slots().get(0).x());
        assertEquals(61, runtime.snapshot().slots().get(0).y());
        assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
        assertEquals(1, runtime.waterTektiteState(0));
    }

    @Test
    void waterTektiteKeepsBankSevenRecoilWhenBackgroundBlocksTheStep() throws IOException {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());
        EnemyAttackContext nonDamaging =
            new EnemyAttackContext(0, false, false, false, false);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x99, 64, 64, EntityStatus.ACTIVE, definition, 0)),
            false, sequence(0x01), null, tables);

        runtime.resolveCombat(0, 120, 120, false, false,
            true, 72, 1, 72, 1, nonDamaging);
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) ->
            direction == 1 || direction == 2;

        runtime.tick(1, 72, 72, sequence(0x01), wall);

        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());
        assertTrue(runtime.enemyRecoilActive(0));
        assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
        assertEquals(1, runtime.waterTektiteState(0));
    }

    @Test
    void waterTektiteUsesHealthGroupZeroAndTheNormalEnemyHitbox() {
        EntitySpriteDefinition definition = pairDefinition(0x99, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x99, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        assertEquals(1, runtime.enemyHealth(0));
        assertTrue(RoomEntityCombatRules.overlapsLink(
            runtime.snapshot().slots().get(0), 64, 64));
        assertFalse(RoomEntityCombatRules.overlapsLink(
            runtime.snapshot().slots().get(0), 73, 64));

        List<EntityCombatEvent> contact = runtime.resolveCombat(
            1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x99, 64, 64, EntityStatus.ACTIVE, definition, 0)));
        List<EntityCombatEvent> sword = runtime.resolveCombat(
            1, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0, runtime.enemyHealth(0));
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void pairoddRuntimeConsumesItsRandomDirectionInitializerAndStartsTeleportState() {
        EntitySpriteDefinition definition = pairDefinition(0x57, 8);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x57, 64, 80, EntityStatus.INIT, definition, 0));
        AtomicInteger randomCalls = new AtomicInteger();
        IntSupplier randomBytes = () -> {
            randomCalls.incrementAndGet();
            return 0x03;
        };
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial, false, randomBytes);

        runtime.tick(0, 0x00, 0x00, randomBytes);
        assertEquals(1, randomCalls.get());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.pairoddState(0));

        runtime.tick(1, 64, 80, randomBytes);

        assertEquals(1, runtime.pairoddState(0));
        assertEquals(0x20, runtime.pairoddTransitionCountdown(0));
        assertEquals(3, runtime.pairoddDirection(0));
    }

    @Test
    void pairoddSpawnsAReverseSlotProjectileWithRomDefinitionPositionAndVector() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x57, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x57, 64, 80, EntityStatus.ACTIVE, definition, 0, 0, 0, 3));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial, false, () -> 0, catalog);

        // Initialize the active snapshot, enter the teleport, and let the
        // disassembly's countdown reach the mirrored resting position.
        runtime.tick(0, 0, 0, () -> 0);
        runtime.tick(1, 64, 80, () -> 0);
        int frame = 2;
        while (runtime.pairoddState(0) == 1) {
            runtime.tick(frame++, 0, 0, () -> 0);
        }
        while (runtime.pairoddState(0) == 2) {
            runtime.tick(frame++, 0, 0, () -> 0);
        }
        assertEquals(0, runtime.pairoddState(0));
        assertEquals(0x30, runtime.pairoddTransitionCountdown(0));
        assertEquals(0x60, runtime.snapshot().slots().get(0).x());
        assertEquals(0x40, runtime.snapshot().slots().get(0).y());

        while (runtime.pairoddTransitionCountdown(0) > 0x19) {
            runtime.tick(frame++, 0, 0, () -> 0);
        }
        runtime.tick(frame, 0x70, 0x58, () -> 0);

        RoomEntity projectile = runtime.snapshot().slots().get(15);
        assertEquals(EntityStatus.ACTIVE, projectile.status());
        assertEquals(-1, projectile.sourceLoadOrder());
        assertEquals(0x58, projectile.type());
        assertEquals(0x60, projectile.x());
        assertEquals(0x40, projectile.y());
        assertEquals(3, projectile.z());
        assertTrue(projectile.spriteDefinition().supported());
        assertEquals(0x04, projectile.spriteDefinition().bank());
        assertEquals(0x5EF4, projectile.spriteDefinition().address());
        // ApplyVectorTowardsLink includes the source Z in its Y displacement:
        // dx=$10, dy=$1B, length=$18 -> ($0E, $18).
        assertEquals(0x0E, runtime.pairoddProjectileSpeedX(15));
        assertEquals(0x18, runtime.pairoddProjectileSpeedY(15));
        assertEquals(0, runtime.pairoddProjectileDirection(15));

        assertEquals(0, runtime.clearEntity(15));
        assertEquals(0, runtime.pairoddProjectileSpeedX(15));
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(15).status());
    }

    @Test
    void pairoddProjectileIsRemovedAfterRoomObjectIntersectionAndQueuesRomVfx() {
        EntitySpriteDefinition definition = pairDefinition(0x58, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, -1, 0x58, 0x40, 0x40, EntityStatus.ACTIVE, definition, 0)));

        List<EntityProjectileEvent> events = runtime.tickWithProjectileEvents(
            0, 0, 0, () -> 0, null, entity -> true,
            EnemyProjectileCollision.LinkState.nonInteractive(),
            false, 0, 0, 0, 0);

        assertTrue(events.isEmpty());
        assertFalse(runtime.snapshot().slots().get(0).loaded());
        assertEquals(1, runtime.transientVfxRequests().size());
        RoomEntityRuntime.TransientVfxRequest request = runtime.transientVfxRequests().get(0);
        assertEquals(TransientVfxType.SWORD_POKE, request.type());
        assertEquals(0x40, request.worldX());
        assertEquals(0x40, request.worldY());
    }

    @Test
    void pairoddProjectileKeepsItsLinkCollisionPathWhenTheRoomObjectPasses() {
        EntitySpriteDefinition definition = pairDefinition(0x58, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, -1, 0x58, 0x40, 0x40, EntityStatus.ACTIVE, definition, 0)));

        List<EntityProjectileEvent> events = runtime.tickWithProjectileEvents(
            0, 0x40, 0x40, () -> 0, null, entity -> false,
            new EnemyProjectileCollision.LinkState(
                0x40, 0x40, 0, 0, 0, false, 1, 0),
            false, 0, 0, 0, 0);

        assertEquals(1, events.size());
        assertEquals(EntityProjectileEvent.Kind.LINK_DAMAGE, events.get(0).kind());
        assertEquals(0x08, events.get(0).linkDamage());
        assertFalse(runtime.snapshot().slots().get(0).loaded());
    }

    @Test
    void moblinLaunchUsesTheHighestDisabledSlotAndDoesNotTickItTwice() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < 15; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        slots.add(new RoomEntity(15, 0, 0x0B, 0x40, 0x50, EntityStatus.ACTIVE,
            definition, 0, 0, 0, 0x03));
        RoomEntitySnapshot initial = new RoomEntitySnapshot(slots);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial, false, () -> 0, catalog);

        for (int frame = 0; frame <= 6; frame++) {
            runtime.tick(frame, 0x70, 0x50, () -> 0);
        }

        RoomEntity projectile = runtime.snapshot().slots().get(14);
        assertEquals(EntityStatus.ACTIVE, projectile.status());
        assertEquals(-1, projectile.sourceLoadOrder());
        assertEquals(0x0C, projectile.type());
        assertEquals(0x48, projectile.x());
        assertEquals(0x4C, projectile.y());
        assertEquals(0x03, projectile.z());
        assertEquals(0, projectile.spriteVariant());
        assertTrue(projectile.spriteDefinition().supported());
        assertEquals(1, runtime.enemyIgnoreHitsCountdown(14));
        assertEquals(0x20, runtime.enemyProjectileSpeedX(14));
        assertEquals(1, runtime.projectileLaunchRequests().size());
        assertEquals(15, runtime.projectileLaunchRequests().get(0).sourceSlot());
        assertEquals(0x0C, runtime.projectileLaunchRequests().get(0).projectileType());

        assertEquals(0, runtime.clearEntity(14));
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(14).status());
        assertEquals(0, runtime.enemyProjectileSpeedX(14));
        assertEquals(0, runtime.enemyProjectileTransitionCountdown(14));
    }

    @Test
    void runtimePublishesArrowTransitionVariantsButLeavesRockVariantUnspun() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition arrowDefinition = catalog.forEntityType(
            0x0C, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        RoomEntityRuntime arrowRuntime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, -1, 0x0C, 0x20, 0x20, EntityStatus.ACTIVE,
                arrowDefinition, 0)), false, () -> 0, catalog);
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 0;

        arrowRuntime.tick(0, 0, 0, () -> 0, wall);
        assertEquals(0x18, arrowRuntime.enemyProjectileTransitionCountdown(0));
        arrowRuntime.tick(1, 0, 0, () -> 0, wall);
        assertEquals(0x17, arrowRuntime.enemyProjectileTransitionCountdown(0));
        assertEquals(1, arrowRuntime.snapshot().slots().get(0).spriteVariant());
        for (int frame = 2; frame <= 9; frame++) {
            arrowRuntime.tick(frame, 0, 0, () -> 0, wall);
        }
        assertEquals(0x0F, arrowRuntime.enemyProjectileTransitionCountdown(0));
        assertEquals(3, arrowRuntime.snapshot().slots().get(0).spriteVariant());

        EntitySpriteDefinition rockDefinition = catalog.forEntityType(
            0x0A, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        RoomEntityRuntime rockRuntime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, -1, 0x0A, 0x20, 0x20, EntityStatus.ACTIVE,
                rockDefinition, 0)), false, () -> 0, catalog);
        rockRuntime.tick(0, 0, 0, () -> 0, wall);
        rockRuntime.tick(1, 0, 0, () -> 0, wall);
        assertEquals(0, rockRuntime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void laserParentRotatesEveryEightFramesAndSpawnsAnInvisibleRomSensor() {
        EntitySpriteDefinition definition = pairDefinition(0x2A, 8);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x2A, 0x40, 0x50, EntityStatus.ACTIVE, definition, 0)));

        for (int frame = 0; frame < 8; frame++) {
            runtime.tick(frame, 0x10, 0x10, () -> 0);
        }

        RoomEntity parent = runtime.snapshot().slots().get(0);
        RoomEntity sensor = runtime.snapshot().slots().get(15);
        assertEquals(1, runtime.laserDirection(0));
        assertEquals(0, parent.spriteVariant());
        assertEquals(EntityStatus.ACTIVE, sensor.status());
        assertEquals(0x2A, sensor.type());
        assertEquals(-1, sensor.sourceLoadOrder());
        assertEquals(-1, sensor.spriteVariant());
        assertEquals(0x40, sensor.x());
        assertEquals(0x50, sensor.y());
    }

    @Test
    void laserParentSpawnsBeamAtRomCountdownTenWithCopiedSpeed() {
        EntitySpriteDefinition definition = pairDefinition(0x2A, 8);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x2A, 0x40, 0x50, EntityStatus.ACTIVE, definition, 0)));
        runtime.setLaserParentForTest(0, 0x10, 0x20, 0xF0);

        runtime.tick(0, 0x10, 0x10, () -> 0);

        RoomEntity beam = runtime.snapshot().slots().get(15);
        assertEquals(0x0F, runtime.laserParentTransitionCountdown(0));
        assertEquals(EntityStatus.ACTIVE, beam.status());
        assertEquals(0x2B, beam.type());
        assertEquals(-1, beam.sourceLoadOrder());
        assertEquals(-1, beam.spriteVariant());
        assertEquals(0x40, beam.x());
        assertEquals(0x50, beam.y());
        assertEquals(0x20, runtime.laserSpeedX(15));
        assertEquals(0xF0, runtime.laserSpeedY(15));
        List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE,
            events.getFirst().soundChannel());
        assertEquals(0x08, events.getFirst().soundId());
    }

    @Test
    void laserParentDoesNotEmitFiringNoiseWhenTheBeamSlotIsUnavailable() {
        List<RoomEntity> entities = new ArrayList<>();
        entities.add(new RoomEntity(0, 0, 0x2A, 0x40, 0x50, EntityStatus.ACTIVE,
            pairDefinition(0x2A, 8), 0));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            entities.add(new RoomEntity(slot, slot, 0xFF, 0, 0, EntityStatus.ACTIVE,
                EntitySpriteDefinition.unsupported(0xFF), -1));
        }
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(entities));
        runtime.setLaserParentForTest(0, 0x10, 0x20, 0xF0);

        runtime.tick(0, 0x10, 0x10, () -> 0);

        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(15).status());
        assertEquals(0xFF, runtime.snapshot().slots().get(15).type());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void laserSensorClearsAndArmsItsParentWhenLinkEntersTheRomWindow() {
        EntitySpriteDefinition definition = pairDefinition(0x2A, 8);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x2A, 0x40, 0x50, EntityStatus.ACTIVE, definition, 0)));

        for (int frame = 0; frame < 8; frame++) {
            runtime.tick(frame, 0x10, 0x10, () -> 0);
        }

        EnemyProjectileCollision.LinkState link = new EnemyProjectileCollision.LinkState(
            0x40, 0x50, 0, 0, 0, false, 0, 0);
        runtime.tickWithProjectileEvents(8, 0x40, 0x50, () -> 0, null, link);

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(15).status());
        assertEquals(0x1F, runtime.laserParentTransitionCountdown(0));
        assertEquals(0x40, runtime.laserSpeedX(0));
        assertEquals(0x40, runtime.laserSpeedY(0));
        assertEquals(0x0F, runtime.enemyFlashCountdown(0));
        assertEquals(0x10, runtime.snapshot().slots().get(0).entityFlipAttribute());
    }

    @Test
    void laserBeamUsesRomFixedPointMovementAndEmitsTheTransientBeamVfx() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, -1, 0x2B, 0x20, 0x30, EntityStatus.ACTIVE,
                EntitySpriteDefinition.unsupported(0x2B), -1)));
        runtime.setLaserBeamForTest(0, 0x10, 0, 0);

        runtime.tickWithProjectileEvents(0, 0, 0, () -> 0, null,
            EnemyProjectileCollision.LinkState.nonInteractive());

        RoomEntity beam = runtime.snapshot().slots().get(0);
        assertEquals(0x21, beam.x());
        assertEquals(0x30, beam.y());
        assertEquals(1, runtime.transientVfxRequests().size());
        RoomEntityRuntime.TransientVfxRequest request =
            runtime.transientVfxRequests().get(0);
        assertEquals(TransientVfxType.LASER_BEAM, request.type());
        assertEquals(0x25, request.worldX());
        assertEquals(0x30, request.worldY());
    }

    @Test
    void laserBeamMirrorShieldReflectionReversesTheRomAxisBeforeMovement() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, -1, 0x2B, 0x40, 0x50, EntityStatus.ACTIVE,
                EntitySpriteDefinition.unsupported(0x2B), -1)));
        runtime.setLaserBeamForTest(0, 0x10, 0, 2);
        EnemyProjectileCollision.LinkState link = new EnemyProjectileCollision.LinkState(
            0x40, 0x50, 0, 0, 0, true, 2, 0);

        List<EntityProjectileEvent> events = runtime.tickWithProjectileEvents(
            0, 0x40, 0x50, () -> 0, null, link);

        assertEquals(1, events.size());
        assertEquals(EntityProjectileEvent.Kind.SHIELD_BLOCK, events.get(0).kind());
        assertEquals(0x02, events.get(0).collisionValue());
        assertFalse(events.get(0).remove());
        assertEquals(0x41, runtime.snapshot().slots().get(0).x());
        assertEquals(0x50, runtime.snapshot().slots().get(0).y());
        assertEquals(0xF0, runtime.laserSpeedX(0));
        assertEquals(1, runtime.transientVfxRequests().size());
        assertEquals(TransientVfxType.SWORD_POKE,
            runtime.transientVfxRequests().get(0).type());
        assertEquals(0x41, runtime.transientVfxRequests().get(0).worldX());
        assertEquals(0x50, runtime.transientVfxRequests().get(0).worldY());
    }

    @Test
    void enemyProjectileWallTransitionUnloadsAndClearsTheReverseSlot() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < 15; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        slots.add(new RoomEntity(15, 0, 0x0B, 0x40, 0x50, EntityStatus.ACTIVE,
            definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(slots), false, () -> 0, catalog);
        RoomEntityBackgroundCollision rightWall = (entity, direction, nextX, nextY) -> direction == 0;

        for (int frame = 0; frame <= 6; frame++) {
            runtime.tick(frame, 0x70, 0x50, () -> 0);
        }
        runtime.tick(7, 0x70, 0x50, () -> 0, rightWall);
        assertEquals(0x18, runtime.enemyProjectileTransitionCountdown(14));

        for (int frame = 8; frame <= 30; frame++) {
            runtime.tick(frame, 0x70, 0x50, () -> 0, rightWall);
        }

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(14).status());
        assertEquals(0, runtime.enemyProjectileSpeedX(14));
        assertEquals(0, runtime.enemyProjectileTransitionCountdown(14));
    }

    @Test
    void fullEntityTableLeavesTheLaunchSourceAndProjectileStateUnchanged() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition moblinDefinition = catalog.forEntityType(
            0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        List<RoomEntity> entities = new ArrayList<>();
        for (int slot = 0; slot < 15; slot++) {
            entities.add(new RoomEntity(slot, -1, 0x01, 0x20, 0x20,
                EntityStatus.ACTIVE, EntitySpriteDefinition.unsupported(0x01), -1));
        }
        entities.add(new RoomEntity(15, 0, 0x0B, 0x40, 0x50, EntityStatus.ACTIVE,
            moblinDefinition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(entities), false, () -> 0, catalog);

        for (int frame = 0; frame <= 6; frame++) {
            runtime.tick(frame, 0x70, 0x50, () -> 0);
        }

        assertEquals(0x0B, runtime.snapshot().slots().get(15).type());
        assertEquals(0x0A, runtime.octorokTransitionCountdown(15));
        for (int slot = 0; slot < 15; slot++) {
            assertNotEquals(0x0C, runtime.snapshot().slots().get(slot).type());
        }
    }

    @Test
    void combatUsesTheEntityVisualYWhenHidingZolIsAirborne() {
        EntitySpriteDefinition definition = pairDefinition(0x9B, 4);
        RoomEntity airborne = new RoomEntity(0, 0, 0x9B, 64, 64,
            EntityStatus.ACTIVE, definition, 0, 0, 0, 0x10);

        assertTrue(RoomEntityCombatRules.overlapsLink(airborne, 64, 48));
        assertFalse(RoomEntityCombatRules.overlapsLink(airborne, 64, 64));
        assertTrue(RoomEntityCombatRules.overlapsSword(airborne, 72, 1, 56, 1));
        assertFalse(RoomEntityCombatRules.overlapsSword(airborne, 72, 1, 72, 1));
    }

    @Test
    void dynamicGhostRuntimeAdvancesZBobEvenWhenItsVariantIsUnchanged() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
            .forFollowerEntityType(0xD4);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, -1, 0xD4, 0x40, 0x40, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial, false, () -> 0);
        runtime.setFollowingNpcState(new FollowingNpcState(false, 1, false, false,
            0, 0, false));

        for (int frame = 0; frame <= 8; frame++) {
            runtime.tick(frame, 0x40, 0x30, () -> 0);
        }

        RoomEntity ghost = runtime.snapshot().slots().get(0);
        assertEquals(0x0D, ghost.z());
        assertEquals(4, ghost.spriteVariant());
    }

    @Test
    void dynamicMarinRuntimeConsumesTheSharedLinkHistory() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
            .forFollowerEntityType(0xC1);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, -1, 0xC1, 0x10, 0x20, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial, false, () -> 0);
        runtime.setFollowingNpcState(new FollowingNpcState(false, 0, true, false,
            0, 0, false));
        LinkPositionHistory history = new LinkPositionHistory();
        history.fill(0x10, 0x20, 0x01, 0x02);
        history.write(1, 0x30, 0x40, 0x03, 0x01);

        runtime.tick(0, 0x50, 0x60, () -> 0, null, history, 0x04, 0x00, 0x00);

        RoomEntity marin = runtime.snapshot().slots().get(0);
        assertEquals(0x30, marin.x());
        assertEquals(0x40, marin.y());
        assertEquals(0x03, marin.z());
    }

    @Test
    void dynamicBowWowRunsItsRomSetupThenFollowerSpeedPhase() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(syntheticRom())
            .forFollowerEntityType(FollowingNpcEntitySpawner.ENTITY_BOW_WOW);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, -1, FollowingNpcEntitySpawner.ENTITY_BOW_WOW,
                0x40, 0x50, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial, false, () -> 0);
        runtime.setFollowingNpcState(new FollowingNpcState(false, 0, false, true,
            0, 0, false));

        runtime.tick(0, 0x60, 0x70, () -> 0);
        assertEquals(0x44, runtime.snapshot().slots().get(0).x());
        assertEquals(0x58, runtime.snapshot().slots().get(0).y());

        runtime.tick(1, 0x60, 0x70, () -> 0);
        runtime.tick(2, 0x60, 0x70, () -> 0);
        runtime.tick(3, 0x60, 0x70, () -> 0);

        RoomEntity bowWow = runtime.snapshot().slots().get(0);
        assertEquals(0x44, bowWow.x());
        assertEquals(0x57, bowWow.y());
    }

    @Test
    void kidHandlersAnimateTheirTwoWalkingFramesEverySixteenFrames() {
        EntitySpriteDefinition definition = pairDefinition(0x70, 4);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x70, 24, 32, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(8);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(16);
        assertEquals(1, runtime.snapshot().slots().get(0).spriteVariant());
        runtime.tick(32);
        assertEquals(0, runtime.snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void onlyTheFirstEightLoadOrdersContributeToThePersistentClearMask() {
        EntitySpriteDefinition definition = pairDefinition(0x33, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 3, 0x33, 24, 32, EntityStatus.ACTIVE, definition, 0),
            new RoomEntity(1, 8, 0x33, 40, 32, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        assertEquals(1 << 3, runtime.clearEntity(0));
        assertEquals(0, runtime.clearEntity(1));
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(1).status());
    }

    @Test
    void collectionUsesTheRomPickableTableAndFrameSlotCadence() {
        EntitySpriteDefinition definition = pairDefinition(0x2D, 1);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x2D, 24, 32, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        runtime.tick(0);

        assertNull(runtime.collectIfNeeded(0, 24, 34, false, true));
        EntityPickupEvent pickup = runtime.collectIfNeeded(1, 24, 34, false, true);

        assertNotNull(pickup);
        assertEquals(0, pickup.slot());
        assertEquals(0x2D, pickup.type());
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void floatingItemsAnimateRomZOnTheFirstAndSubsequentActiveFramesWithoutMovingXY() {
        EntitySpriteDefinition definition = pairDefinition(0x86, 7);
        RoomEntity initialEntity = new RoomEntity(0, 0, 0x86, 24, 64,
            EntityStatus.INIT, definition, 0, 0, 0, 0x13);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(initialEntity));

        runtime.tick(0);
        RoomEntity first = runtime.snapshot().slots().get(0);
        assertEquals(EntityStatus.ACTIVE, first.status());
        assertEquals(0x0F, first.z());
        assertEquals(24, first.x());
        assertEquals(64, first.y());

        runtime.tick(24);
        assertEquals(0x11, runtime.snapshot().slots().get(0).z());
    }

    @Test
    void floatingCollectionUsesVisualYPersistenceAndSourceVariantData() {
        RoomEntity entity = new RoomEntity(0, 0, 0x86, 24, 64,
            EntityStatus.ACTIVE, pairDefinition(0x86, 7), 0, 0, 0, 0x13);
        RoomEntityRuntime belowLinkZ = RoomEntityRuntime.from(snapshot(entity));
        assertNull(belowLinkZ.collectIfNeeded(1, 24, 47, true, true, 0, 0x0B));

        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(entity));
        EntityPickupEvent pickup = runtime.collectIfNeeded(1, 24, 47,
            true, true, 0, 0x0C);

        assertNotNull(pickup);
        assertEquals(0x86, pickup.type());
        assertEquals(0, pickup.sourceVariant());
        assertEquals(1, pickup.persistentClearMask());
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void floatingCollectionAllowsAirborneLinkInSideScrollingRoomsWithoutZGate() {
        RoomEntity entity = new RoomEntity(0, 0, 0xE5, 24, 64,
            EntityStatus.ACTIVE, pairDefinition(0xE5, 7), 5, 0, 0, 0x13);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(entity));
        runtime.setGroundInteractionSideScrolling(true);

        EntityPickupEvent pickup = runtime.collectIfNeeded(0, 24, 47,
            true, true, 0, 0x00);

        assertNotNull(pickup);
        assertEquals(0xE5, pickup.type());
        assertEquals(5, pickup.sourceVariant());
    }

    @Test
    void collectionUsesThePickupHitboxEdgesFromHitboxPositions() {
        EntitySpriteDefinition definition = pairDefinition(0x2D, 1);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x2D, 24, 32, EntityStatus.ACTIVE, definition, 0));

        RoomEntityRuntime xInside = RoomEntityRuntime.from(initial);
        assertNotNull(xInside.collectIfNeeded(1, 34, 34, false, true));

        RoomEntityRuntime xOutside = RoomEntityRuntime.from(initial);
        assertNull(xOutside.collectIfNeeded(1, 35, 34, false, true));

        RoomEntityRuntime yInside = RoomEntityRuntime.from(initial);
        assertNotNull(yInside.collectIfNeeded(1, 24, 43, false, true));

        RoomEntityRuntime yOutside = RoomEntityRuntime.from(initial);
        assertNull(yOutside.collectIfNeeded(1, 24, 44, false, true));
    }

    @Test
    void collectionSkipsAirborneAndNonInteractiveLinkStates() {
        EntitySpriteDefinition definition = pairDefinition(0x2D, 1);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x2D, 24, 32, EntityStatus.ACTIVE, definition, 0));

        assertNull(RoomEntityRuntime.from(initial).collectIfNeeded(1, 24, 34, true, true));
        assertNull(RoomEntityRuntime.from(initial).collectIfNeeded(1, 24, 34, false, false));
    }

    @Test
    void transitionPickupsRemainInTheHeldStateAfterCollection() {
        EntitySpriteDefinition definition = pairDefinition(0x33, 2);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x33, 24, 32, EntityStatus.ACTIVE, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);

        EntityPickupEvent pickup = runtime.collectIfNeeded(1, 24, 34, false, true);

        assertNotNull(pickup);
        assertEquals(1, pickup.persistentClearMask());
        assertEquals(EntityStatus.LIFTED, runtime.snapshot().slots().get(0).status());
        assertNull(runtime.collectIfNeeded(1, 24, 34, false, true));
    }

    @Test
    void keyDropPointNormalVariantImmediatelyAwardsASmallKeyAndClears() {
        EntitySpriteDefinition definition = keyDropDefinition();
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x30, 24, 32, EntityStatus.ACTIVE, definition, 0)));

        assertEquals(0xB1, runtime.physicsFlags(0));
        assertEquals(0x0A, runtime.options1(0));

        EntityPickupEvent pickup = runtime.collectIfNeeded(1, 24, 34, false, true);

        assertNotNull(pickup);
        assertEquals(0x30, pickup.type());
        assertEquals(1, pickup.persistentClearMask());
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
        assertEquals(List.of(new RoomEntityRuntime.KeyRewardEvent(
            0, ChestContentsTable.CHEST_SMALL_KEY)),
            runtime.consumePendingKeyRewardEvents());
    }

    @Test
    void keyDropPointDungeonVariantStartsTheHeldRewardTransitionAndUsesRomDialog() {
        EntitySpriteDefinition definition = keyDropDefinition();
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x30, 24, 32, EntityStatus.ACTIVE, definition, 2)));

        EntityPickupEvent pickup = runtime.collectIfNeeded(1, 24, 34, false, true);
        assertNotNull(pickup);
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0x68, runtime.keyDropTransitionCountdown(0));

        runtime.setKeyDropTransitionCountdownForTest(0, 0x11);
        runtime.tick(2, 0x40, 0x50, () -> 0);

        RoomEntity held = runtime.snapshot().slots().get(0);
        assertEquals(EntityStatus.ACTIVE, held.status());
        assertEquals(0x40, held.x());
        assertEquals(0x44, held.y());
        assertEquals(0x0F, runtime.keyDropTransitionCountdown(0));
        assertEquals(List.of(new RoomEntityRuntime.DialogRequest(0, 0xA3)),
            runtime.consumePendingDialogRequests());
        assertEquals(List.of(new RoomEntityRuntime.KeyRewardEvent(
            0, ChestContentsTable.CHEST_ANGLER_KEY)),
            runtime.consumePendingKeyRewardEvents());
    }

    @Test
    void keyDropPointMasterStalfosRoomUsesHookshotPresentationAndReward() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition definition = keyDropDefinition();
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, -1, 0x30, 24, 32, EntityStatus.ACTIVE, definition, 0)),
            true, () -> 0, catalog);
        runtime.setEntityRoomIdForTest(0x80);
        runtime.setKeyDropTransitionCountdownForTest(0, 0x11);

        runtime.tick(1, 0x40, 0x50, () -> 0);

        RoomEntity held = runtime.snapshot().slots().get(0);
        assertEquals(-1, held.spriteDefinition().bank());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, held.spriteDefinition().shape());
        assertEquals(0x36, held.spriteDefinition().variant(0).first().tile());
        assertEquals(0x0F, runtime.keyDropTransitionCountdown(0));
        assertEquals(List.of(new RoomEntityRuntime.DialogRequest(0, 0x93)),
            runtime.consumePendingDialogRequests());
        assertEquals(List.of(new RoomEntityRuntime.KeyRewardEvent(
            0, ChestContentsTable.CHEST_HOOKSHOT)),
            runtime.consumePendingKeyRewardEvents());
    }

    @Test
    void keyDropPointAtYarnaQuicksandCenterStartsTheRomFallingHandoff() {
        EntitySpriteDefinition definition = keyDropDefinition();
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, -1, 0x30, 0x50, 0x48, EntityStatus.ACTIVE, definition, 0)));
        runtime.setEntityRoomIdForTest(0xCE);

        runtime.tick(0, 0, 0, () -> 0);

        RoomEntity falling = runtime.snapshot().slots().get(0);
        assertEquals(EntityStatus.FALLING, falling.status());
        assertEquals(0x2F, runtime.transitionCountdown(0));
        assertEquals(0x50, runtime.fallingTargetX(0));
        assertEquals(0x48, runtime.fallingTargetY(0));
        assertEquals(List.of(new RoomEntityRuntime.KeyQuicksandEvent(0)),
            runtime.consumePendingKeyQuicksandEvents());
        assertEquals(List.of(new EntityCombatEvent(0, 0x30, 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, 0x18)),
            runtime.consumePendingEntityEvents());
    }

    @Test
    void armosKnightKeyDropUsesTheSourceForcedSpriteVariant() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity source = new RoomEntity(15, 0, 0x88, 0x50, 0x60,
            EntityStatus.DYING, catalog.forEntityType(
                0x88, EntityRoomLoader.RoomTable.OVERWORLD, -1), 0);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshotAt(source), false,
            () -> 0, catalog, new RomEnemyCombatTables(rom));
        runtime.setEnemyDropResolver(new EnemyDropResolver(rom));
        runtime.setDroppedItemForTest(15, 0x30);

        runtime.tick(0, 0, 0, () -> 0);

        assertEquals(3, runtime.snapshot().slots().get(14).spriteVariant());
    }

    @Test
    void indoorDroppablesUseTheRomSlowFadeAndUnloadAtZero() {
        EntitySpriteDefinition definition = pairDefinition(0x37, 1);
        RoomEntitySnapshot initial = snapshot(
            new RoomEntity(0, 0, 0x37, 24, 32, EntityStatus.INIT, definition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(initial, true);

        runtime.tick(0);

        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0x80, runtime.slowTransitionCountdown(0));

        int frame = 1;
        while (runtime.slowTransitionCountdown(0) > 3) {
            runtime.tick(frame++);
        }
        assertEquals(3, runtime.slowTransitionCountdown(0));

        while ((frame & 0x03) != 0) {
            runtime.tick(frame++);
        }
        runtime.tick(frame);

        assertEquals(2, runtime.slowTransitionCountdown(0));
        assertEquals(-1, runtime.snapshot().slots().get(0).spriteVariant());

        while (runtime.snapshot().slots().get(0).loaded()) {
            runtime.tick(frame++);
        }
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void terminalStaticDeathRecordsKillAndDynamicDeathDoesNot() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EnemyDropResolver resolver = new EnemyDropResolver(rom);

        RoomEntityRuntime staticRuntime = RoomEntityRuntime.from(snapshotAt(
            dyingEntity(0, 0, 0x09, catalog)), false, () -> 0, catalog, tables);
        staticRuntime.setEnemyDropResolver(resolver);
        staticRuntime.setDroppedItemForTest(0, EnemyDropResolver.ENTITY_NONE);
        staticRuntime.tick(0, 0, 0, () -> 0);

        assertEquals(EntityStatus.DISABLED, staticRuntime.snapshot().slots().get(0).status());
        assertEquals(1, staticRuntime.killCount());
        assertEquals(0, staticRuntime.killOrderAt(0));
        assertEquals(1, staticRuntime.consumePendingClearedEntityMask());

        RoomEntityRuntime dynamicRuntime = RoomEntityRuntime.from(snapshotAt(
            dyingEntity(0, -1, 0x09, catalog)), false, () -> 0, catalog, tables);
        dynamicRuntime.setEnemyDropResolver(resolver);
        dynamicRuntime.setDroppedItemForTest(0, EnemyDropResolver.ENTITY_NONE);
        dynamicRuntime.tick(0, 0, 0, () -> 0);

        assertEquals(EntityStatus.DISABLED, dynamicRuntime.snapshot().slots().get(0).status());
        assertEquals(0, dynamicRuntime.killCount());
        assertEquals(new EnemyDropResolver.CounterState(0, 0),
            dynamicRuntime.enemyDropCounters());
        assertEquals(0, dynamicRuntime.consumePendingClearedEntityMask());
    }

    @Test
    void explicitAndRandomNoDropDeathsUnloadWithExpectedCounterAndRandomBehavior()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            dyingEntity(0, 0, 0x09, catalog),
            dyingEntity(1, 1, 0x0B, catalog)), false, () -> 0, catalog, tables);
        runtime.setEnemyDropResolver(new EnemyDropResolver(rom));
        runtime.setDroppedItemForTest(0, EnemyDropResolver.ENTITY_NONE);
        runtime.setDroppedItemForTest(1, 0);
        AtomicInteger randomReads = new AtomicInteger();

        runtime.tick(0, 0, 0, () -> {
            randomReads.incrementAndGet();
            return 0x01;
        });

        assertFalse(runtime.snapshot().slots().get(0).loaded());
        assertFalse(runtime.snapshot().slots().get(1).loaded());
        assertEquals(2, runtime.killCount());
        assertEquals(1, runtime.killOrderAt(0));
        assertEquals(0, runtime.killOrderAt(1));
        assertEquals(new EnemyDropResolver.CounterState(1, 1),
            runtime.enemyDropCounters());
        assertEquals(1, randomReads.get());
        assertEquals(0b11, runtime.consumePendingClearedEntityMask());
    }

    @Test
    void terminalEnemyDeathSpawnsTopDownDropInReverseFreeSlotAndPreservesItUntilNextTick()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        int sourceX = 0x44;
        int sourceY = 0x58;
        int sourceZ = 0x07;
        RoomEntity source = new RoomEntity(15, 0, 0x09, sourceX, sourceY,
            EntityStatus.DYING, catalog.forEntityType(
                0x09, EntityRoomLoader.RoomTable.OVERWORLD, -1), 0, 0, 0, sourceZ);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshotAt(source), false,
            () -> 0, catalog, tables);
        runtime.setEnemyDropResolver(new EnemyDropResolver(rom));
        runtime.setDroppedItemForTest(15, 0x2E);

        runtime.tick(0, 0, 0, () -> 0);

        RoomEntity drop = runtime.snapshot().slots().get(14);
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(15).status());
        assertEquals(EntityStatus.ACTIVE, drop.status());
        assertEquals(-1, drop.sourceLoadOrder());
        assertEquals(0x2E, drop.type());
        assertEquals(sourceX, drop.x());
        assertEquals(sourceY, drop.y());
        assertEquals(sourceZ, drop.z());
        assertTrue(drop.spriteDefinition().supported());
        assertEquals(drop.spriteDefinition().initialVariant(), drop.spriteVariant());
        assertEquals(0x80, runtime.slowTransitionCountdown(14));
        assertEquals(0x18, runtime.dropPrivateCountdown1(14));
        assertEquals(0x03, runtime.dropPrivateCountdown3(14));
        assertEquals(0x18, runtime.dropSpeedZ(14));
        assertEquals(0x00, runtime.dropSpeedY(14));
        assertEquals(0, runtime.droppedItemForTest(15));

        runtime.tick(1, 0, 0, () -> 0);

        assertEquals(0x17, runtime.dropPrivateCountdown1(14));
        assertEquals(0x02, runtime.dropPrivateCountdown3(14));
        assertEquals((sourceZ + 1) & 0xFF, runtime.snapshot().slots().get(14).z());
        assertEquals(0x16, runtime.dropSpeedZ(14));
        assertNull(runtime.collectIfNeeded(1, sourceX, sourceY + 2,
            false, true));

        for (int frame = 2; frame <= 24; frame++) {
            runtime.tick(frame, 0, 0, () -> 0);
        }
        assertEquals(0, runtime.dropPrivateCountdown1(14));
        runtime.tick(25, 0, 0, () -> 0);
        EntityPickupEvent pickup = runtime.collectIfNeeded(25, sourceX, sourceY + 2,
            false, true);
        assertNotNull(pickup);
        assertEquals(14, pickup.slot());
        assertEquals(0x2E, pickup.type());
        assertEquals(0, pickup.persistentClearMask());
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(14).status());
        assertEquals(0, runtime.dropPrivateCountdown1(14));
        assertEquals(0, runtime.dropPrivateCountdown3(14));
        assertEquals(0, runtime.dropSpeedZ(14));
        assertEquals(0, runtime.dropSpeedY(14));
    }

    @Test
    void terminalEnemyDeathSpawnsSideScrollHeartWithRomDropConfiguration()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntity source = new RoomEntity(15, 0, 0x0B, 0x50, 0x60,
            EntityStatus.DYING, catalog.forEntityType(
                0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1), 0, 0, 0, 0x09);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshotAt(source).withSideScrolling(true),
            false, () -> 0, catalog, tables);
        runtime.setEnemyDropResolver(new EnemyDropResolver(rom));
        runtime.setDroppedItemForTest(15, 0x2D);

        runtime.tick(0, 0, 0, () -> 0);

        RoomEntity drop = runtime.snapshot().slots().get(14);
        assertEquals(EntityStatus.ACTIVE, drop.status());
        assertEquals(-1, drop.sourceLoadOrder());
        assertEquals(0x2D, drop.type());
        assertEquals(0x50, drop.x());
        assertEquals(0x60, drop.y());
        assertEquals(0x09, drop.z());
        assertTrue(drop.spriteDefinition().supported());
        assertEquals(drop.spriteDefinition().initialVariant(), drop.spriteVariant());
        assertEquals(0x80, runtime.slowTransitionCountdown(14));
        assertEquals(0x18, runtime.dropPrivateCountdown1(14));
        assertEquals(0x03, runtime.dropPrivateCountdown3(14));
        assertEquals(0xEC, runtime.dropSpeedY(14));
        assertEquals(0x00, runtime.dropSpeedZ(14));
    }

    @Test
    void sideScrollDropUsesDownCollisionToAlignAndStopWeakLanding()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntity source = new RoomEntity(15, 0, 0x0B, 0x50, 0x60,
            EntityStatus.DYING, catalog.forEntityType(
                0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1), 0, 0, 0, 0x09);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshotAt(source).withSideScrolling(true),
            false, () -> 0, catalog, tables);
        runtime.setEnemyDropResolver(new EnemyDropResolver(rom));
        runtime.setDroppedItemForTest(15, 0x2D);

        runtime.tick(0, 0, 0, () -> 0);
        RoomEntityBackgroundCollision collision = (entity, direction, nextX, nextY) ->
            direction == EntityBackgroundCollisionResult.DOWN;
        for (int frame = 1; frame <= 11; frame++) {
            runtime.tick(frame, 0, 0, () -> 0, collision);
        }

        RoomEntity drop = runtime.snapshot().slots().get(14);
        assertEquals(0x55, drop.y());
        assertEquals(0x00, runtime.dropSpeedY(14));
    }

    @Test
    void fullEntitySlotsStillUnloadSourceWhenDropCannotBeSpawned() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        List<RoomEntity> entities = new ArrayList<>();
        for (int slot = 0; slot < 15; slot++) {
            entities.add(new RoomEntity(slot, slot + 1, 0xFF, 0x20, 0x20,
                EntityStatus.ACTIVE, EntitySpriteDefinition.unsupported(0xFF), -1));
        }
        RoomEntity source = new RoomEntity(15, 0, 0x09, 0x44, 0x58,
            EntityStatus.DYING, catalog.forEntityType(
                0x09, EntityRoomLoader.RoomTable.OVERWORLD, -1), 0);
        entities.add(source);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(entities),
            false, () -> 0, catalog, tables);
        runtime.setEnemyDropResolver(new EnemyDropResolver(rom));
        runtime.setDroppedItemForTest(15, 0x2E);

        runtime.tick(0, 0, 0, () -> 0);

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(15).status());
        assertFalse(runtime.snapshot().slots().stream().anyMatch(
            entity -> entity.loaded() && (entity.type() == 0x2D || entity.type() == 0x2E)));
        assertEquals(1, runtime.killCount());
        assertEquals(1, runtime.consumePendingClearedEntityMask());
    }

    @Test
    void swordCollectionTableMatchesTheSeventeenPickableEntries() {
        int[] canBeCollected = {0x2D, 0x2E, 0x31, 0x33, 0x34, 0x37, 0x38, 0x3B};
        int[] cannotBeCollected = {0x2F, 0x30, 0x32, 0x35, 0x36, 0x39, 0x3A, 0x3C, 0x3D};

        for (int type : canBeCollected) {
            assertTrue(RoomEntityPickupRules.canBeCollectedBySword(type),
                "type 0x" + Integer.toHexString(type));
        }
        for (int type : cannotBeCollected) {
            assertFalse(RoomEntityPickupRules.canBeCollectedBySword(type),
                "type 0x" + Integer.toHexString(type));
        }
    }

    @Test
    void spawnBombUsesTheRomSlotStateAndDoesNotSkipItsFirstTick() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())), false, null, catalog);

        int slot = runtime.spawnBomb(0x40, 0x50, 0x07, 3);

        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, slot);
        RoomEntity bomb = runtime.snapshot().slots().get(slot);
        assertEquals(0x02, bomb.type());
        assertEquals(-1, bomb.sourceLoadOrder());
        assertEquals(EntityStatus.ACTIVE, bomb.status());
        assertEquals(0x40, bomb.x());
        assertEquals(0x54, bomb.y());
        assertEquals(0x08, bomb.z());
        assertEquals(EntitySpriteDefinition.Shape.SINGLE, bomb.spriteDefinition().shape());
        assertEquals(0x03, bomb.spriteDefinition().bank());
        assertEquals(0x652E, bomb.spriteDefinition().address());
        assertEquals(0, bomb.spriteVariant());
        assertEquals(0xA0, runtime.transitionCountdown(slot));
        assertEquals(0xD2, runtime.physicsFlags(slot));
        assertEquals(0x0A, runtime.options1(slot));
        assertEquals(3, runtime.bombDirection(slot));
        assertTrue(runtime.bombActive());
        assertTrue(runtime.lastBombPlacementPlayedBump());

        runtime.tick(0, 0x40, 0x50, () -> 0);

        RoomEntity afterFirstTick = runtime.snapshot().slots().get(slot);
        assertEquals(slot, afterFirstTick.slot());
        assertEquals(0x02, afterFirstTick.type());
        assertEquals(0x9F, runtime.transitionCountdown(slot));
        assertEquals(1, runtime.snapshot().loadedEntities().size());
    }

    @Test
    void liftableRockSmashUsesTheRomCountdownDisplayFramesAndNoise() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())), false, null, catalog);

        int slot = runtime.spawnLiftableRockSmash(0x58, 0x60, 0x01);

        RoomEntity initial = runtime.snapshot().slots().get(slot);
        assertEquals(EntityStatus.ACTIVE, initial.status());
        assertEquals(0x05, initial.type());
        assertEquals(0x58, initial.x());
        assertEquals(0x60, initial.y());
        assertEquals(0x06, initial.spriteVariant());
        assertEquals(EntitySpriteDefinition.Shape.DYNAMIC, initial.spriteDefinition().shape());
        assertEquals(0xD4, runtime.physicsFlags(slot));
        assertEquals(0x0A, runtime.options1(slot));
        assertEquals(0x05, runtime.consumePendingEntityEvents().stream()
            .filter(event -> event.type() == 0x05)
            .findFirst()
            .orElseThrow()
            .soundId());

        runtime.tick(0, 0, 0, () -> 0);
        assertEquals(0x06, runtime.snapshot().slots().get(slot).spriteVariant());
        runtime.tick(1, 0, 0, () -> 0);
        runtime.tick(2, 0, 0, () -> 0);
        runtime.tick(3, 0, 0, () -> 0);
        assertEquals(0x07, runtime.snapshot().slots().get(slot).spriteVariant());

        for (int frame = 4; frame <= 29; frame++) {
            runtime.tick(frame, 0, 0, () -> 0);
        }
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(slot).status());
    }

    @Test
    void liftableRockRubbleUsesTheRomSilentPhysicsAndRockFrame() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())), false, null, catalog);

        int slot = runtime.spawnLiftableRockRubble(0x48, 0x4F);

        RoomEntity rubble = runtime.snapshot().slots().get(slot);
        assertEquals(EntityStatus.ACTIVE, rubble.status());
        assertEquals(0x05, rubble.type());
        assertEquals(0x05, rubble.spriteVariant());
        assertEquals(0xC4, runtime.physicsFlags(slot));
        assertEquals(0x0A, runtime.options1(slot));
        assertEquals(List.of(), runtime.consumePendingEntityEvents());

        runtime.tick(0, 0, 0, () -> 0);
        assertEquals(0x05, runtime.snapshot().slots().get(slot).spriteVariant());
    }

    @Test
    void sideScrollBombPlacementSkipsTheTopViewBumpJingle() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())).withSideScrolling(true),
            false, null, catalog);

        assertTrue(runtime.spawnBomb(0x40, 0x50, 0x07, 3) >= 0);

        assertFalse(runtime.lastBombPlacementPlayedBump());
        assertEquals(0, runtime.snapshot().loadedEntities().get(0).z());
    }

    @Test
    void spawnPlayerArrowUsesLinkStateAndTheRomArrowDisplayList() {
        byte[] rom = syntheticRom();
        write(rom, 0x03, 0x6BC6,
            0x2E, 0x21, 0x2C, 0x21,
            0x2C, 0x01, 0x2E, 0x01,
            0x2A, 0x41, 0x2A, 0x61,
            0x2A, 0x01, 0x2A, 0x21);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())), false, null, catalog);

        int slot = runtime.spawnArrow(0x40, 0x50, 0x07, 3);

        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, slot);
        RoomEntity arrow = runtime.snapshot().slots().get(slot);
        assertEquals(0x00, arrow.type());
        assertEquals(-1, arrow.sourceLoadOrder());
        assertEquals(EntityStatus.ACTIVE, arrow.status());
        assertEquals(0x40, arrow.x());
        assertEquals(0x50, arrow.y());
        assertEquals(0x08, arrow.z());
        assertEquals(0x03, arrow.spriteVariant());
        assertEquals(0x03, arrow.spriteDefinition().bank());
        assertEquals(0x6BC6, arrow.spriteDefinition().address());
        assertEquals(0x42, runtime.physicsFlags(slot));
        assertEquals(0x12, runtime.options1(slot));
        assertEquals(1, runtime.enemyIgnoreHitsCountdown(slot));
        // ShootArrow calls label_140F after SpawnPlayerProjectile. The
        // initial $20 movement table is replaced by data_13AD/data_13B5.
        assertEquals(0x40, runtime.playerArrowSpeedY(slot));
        assertEquals(3, runtime.playerArrowDirection(slot));
        assertEquals(1, runtime.activePlayerArrowCount());

        runtime.tick(0, 0, 0, () -> 0);

        assertEquals(0x54, runtime.snapshot().slots().get(slot).y());
    }

    @Test
    void pieceOfPowerSelectsTheAlternateShootArrowSpeedTable() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot());

        int slot = runtime.spawnArrow(0x40, 0x50, 0, 3, true);

        assertEquals(0x30, runtime.playerArrowSpeedY(slot));
        assertEquals(0x00, runtime.playerArrowSpeedX(slot));
    }

    @Test
    void ordinaryPlayerArrowRunsTheRomEntityDamagePassBeforeMoving() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition octorokDefinition = pairDefinition(0x09, 8);
        RoomEntity octorok = new RoomEntity(15, 15, 0x09, 0x40, 0x50,
            EntityStatus.ACTIVE, octorokDefinition, 0);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            snapshotWithSlots(octorok), false, () -> 0, null, tables);

        int arrowSlot = runtime.spawnArrow(0x40, 0x50, 0, 0);
        assertEquals(14, arrowSlot);

        // $75A2 checks (hFrameCounter XOR targetSlot) bit 0. Slot 15 is
        // therefore checked on frame 1. The arrow collision runs before its
        // own movement, exactly as ArrowEntityHandler does.
        runtime.tick(1, 0x40, 0x50, () -> 0);

        assertEquals(0, runtime.enemyHealth(15));
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(15).status());
        assertEquals(0x18, runtime.enemyFlashCountdown(15));
        assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(15));
        assertEquals(List.of(new EntityCombatEvent(15, 0x09, 0, false,
            1, -1, EntityCombatEvent.SoundChannel.JINGLE, 0x03,
            EntityCombatEvent.SoundChannel.NONE, -1, null)),
            runtime.consumePendingEntityEvents());
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(arrowSlot).status());
        // func_003_77A7 copies the active arrow's speed directly into the
        // target recoil velocity, even on a lethal hit.
        assertEquals(0x40, runtime.enemyRecoilSpeedXForTest(15));
        assertEquals(0x00, runtime.enemyRecoilSpeedYForTest(15));
    }

    @Test
    void bombArrowCollisionIsHarmlessButSetsTheTargetTransitionCountdown() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition moblinDefinition = pairDefinition(0x0B, 8);
        RoomEntity moblin = new RoomEntity(15, 15, 0x0B, 0x40, 0x50,
            EntityStatus.ACTIVE, moblinDefinition, 0);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            snapshotWithSlots(moblin), false, () -> 0, null, tables);

        int candidateBomb = runtime.spawnBomb(0x40, 0x50, 0, 0);
        int arrowSlot = runtime.spawnArrow(0x40, 0x50, 0, 0);
        assertTrue(runtime.isBombArrowForTest(arrowSlot));
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(candidateBomb).status());

        runtime.tick(1, 0x40, 0x50, () -> 0);

        assertEquals(2, runtime.enemyHealth(15));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(15).status());
        assertEquals(0x03, runtime.transitionCountdown(15));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(arrowSlot).status());
        assertTrue(runtime.isBombArrowForTest(arrowSlot));
    }

    @Test
    void shootingWithinTheBombArrowWindowRemovesTheLatestBombAndMarksTheArrow()
        throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(), false, null, catalog);

        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        assertEquals(0x06, runtime.bombArrowCooldownForTest());

        int arrowSlot = runtime.spawnArrow(0x40, 0x50, 0, 0);

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(bombSlot).status());
        assertTrue(runtime.isBombArrowForTest(arrowSlot));
        assertEquals(0, runtime.bombArrowCooldownForTest());
        assertFalse(runtime.lastArrowShotPlayedWhoosh());
        assertEquals(EntitySpriteDefinition.Shape.DYNAMIC,
            runtime.snapshot().slots().get(arrowSlot).spriteDefinition().shape());
    }

    @Test
    void placingWithinTheBombArrowWindowMarksTheLatestArrowButKeepsTheBomb() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot());

        int arrowSlot = runtime.spawnArrow(0x40, 0x50, 0, 1);
        assertEquals(0x06, runtime.bombArrowCooldownForTest());

        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 1);

        assertTrue(runtime.snapshot().slots().get(bombSlot).loaded());
        assertTrue(runtime.isBombArrowForTest(arrowSlot));
        assertEquals(0, runtime.bombArrowCooldownForTest());
        assertEquals(0x10, runtime.bombPrivateCountdown1ForTest(bombSlot));
    }

    @Test
    void bombArrowCooldownExpiresAfterSixEntityFrames() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot());

        int arrowSlot = runtime.spawnArrow(0x40, 0x50, 0, 0);
        assertFalse(runtime.isBombArrowForTest(arrowSlot));
        for (int frame = 0; frame < 6; frame++) {
            runtime.tick(frame, 0x40, 0x50, () -> 0);
        }

        assertEquals(0, runtime.bombArrowCooldownForTest());
        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        assertTrue(runtime.snapshot().slots().get(bombSlot).loaded());
        assertFalse(runtime.isBombArrowForTest(arrowSlot));
        assertEquals(0x06, runtime.bombArrowCooldownForTest());
    }

    @Test
    void bombArrowSpawnsTheSourceBombExplosionAfterAWallTransition() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(), false, null, catalog);

        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        int arrowSlot = runtime.spawnArrow(0x40, 0x50, 0, 0);
        assertTrue(runtime.isBombArrowForTest(arrowSlot));
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(bombSlot).status());

        runtime.tick(0, 0x40, 0x50, () -> 0,
            (entity, direction, nextX, nextY) -> direction == 0);
        assertEquals(0x18, runtime.playerArrowTransitionCountdown(arrowSlot));

        runtime.tick(1, 0x40, 0x50, () -> 0);

        RoomEntity explosion = runtime.snapshot().loadedEntities().getFirst();
        assertEquals(0x02, explosion.type());
        assertEquals(0x17, runtime.transitionCountdown(explosion.slot()));
        assertEquals(List.of(new EntityCombatEvent(explosion.slot(), 0x02, 0, false,
            EntityCombatEvent.SoundChannel.NOISE, 0x0C)),
            runtime.consumePendingEntityEvents());
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(arrowSlot).status());
    }

    @Test
    void playerArrowWallImpactStartsTheSharedMoblinAlertTimer() {
        byte[] rom = syntheticRom();
        write(rom, 0x03, 0x6BC6,
            0x2E, 0x21, 0x2C, 0x21,
            0x2C, 0x01, 0x2E, 0x01,
            0x2A, 0x41, 0x2A, 0x61,
            0x2A, 0x01, 0x2A, 0x21);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())), false, null, catalog);
        int slot = runtime.spawnArrow(0x40, 0x50, 0x00, 0);

        runtime.tick(0, 0, 0, () -> 0,
            (entity, direction, nextX, nextY) -> direction == 0);

        assertEquals(0x04, runtime.swordMoblinAlertingSoundCounter());
        assertEquals(0x18, runtime.playerArrowTransitionCountdown(slot));
        assertEquals(0xF0, runtime.playerArrowSpeedX(slot));
    }

    @Test
    void playerArrowCountIsCappedAtTwoAndSlotsCanBeReused() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot());

        int first = runtime.spawnArrow(0x40, 0x50, 0, 0);
        int second = runtime.spawnArrow(0x40, 0x50, 0, 1);

        assertEquals(15, first);
        assertEquals(14, second);
        assertEquals(-1, runtime.spawnArrow(0x40, 0x50, 0, 2));
        assertEquals(2, runtime.activePlayerArrowCount());

        runtime.clearEntity(first);

        assertEquals(15, runtime.spawnArrow(0x40, 0x50, 0, 2));
        assertEquals(2, runtime.activePlayerArrowCount());
    }

    @Test
    void bombSpawnRejectsASecondLoadedBombAndReusesTheSlotAfterClear() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot());

        int firstSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);

        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, firstSlot);
        assertEquals(-1, runtime.spawnBomb(0x42, 0x52, 0, 1));

        runtime.clearEntity(firstSlot);

        assertFalse(runtime.bombActive());
        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1,
            runtime.spawnBomb(0x42, 0x52, 0, 1));
    }

    @Test
    void bombSpawnRejectsAFullEntityTableWithoutChangingItsSlots() {
        List<RoomEntity> slots = new ArrayList<>();
        EntitySpriteDefinition unsupported = EntitySpriteDefinition.unsupported(0xFF);
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(new RoomEntity(slot, slot, 0xFF, 0x20, 0x20,
                EntityStatus.ACTIVE, unsupported, -1));
        }
        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(slots));

        assertEquals(-1, runtime.spawnBomb(0x40, 0x50, 0, 0));
        assertFalse(runtime.bombActive());
        assertEquals(0xFF, runtime.snapshot().slots().get(15).type());
    }

    @Test
    void bombKeepsOneSlotAcrossWarningExplosionSoundAndUnload() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())), false, null, catalog);
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 0);

        runtime.setBombTransitionCountdownForTest(slot, 0x22);
        runtime.tick(0, 0x40, 0x50, () -> 0);
        RoomEntity warning = runtime.snapshot().slots().get(slot);
        assertEquals(0x02, warning.type());
        assertEquals(0x5484, warning.spriteDefinition().address());
        assertEquals(0, warning.spriteVariant());
        assertEquals(0x21, runtime.transitionCountdown(slot));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());

        // UpdateEntityTimers decrements before BombEntityHandler sees $18.
        runtime.setBombTransitionCountdownForTest(slot, 0x19);
        runtime.tick(1, 0x40, 0x50, () -> 0);
        RoomEntity warningAtSound = runtime.snapshot().slots().get(slot);
        assertEquals(slot, warningAtSound.slot());
        assertEquals(0x02, warningAtSound.type());
        assertEquals(0x5484, warningAtSound.spriteDefinition().address());
        assertEquals(0x17, runtime.transitionCountdown(slot));
        assertEquals(List.of(new EntityCombatEvent(slot, 0x02, 0, false,
            EntityCombatEvent.SoundChannel.NOISE, 0x0C)),
            runtime.consumePendingEntityEvents());

        runtime.tick(2, 0x40, 0x50, () -> 0);
        RoomEntity explosionVariantThree = runtime.snapshot().slots().get(slot);
        assertEquals(slot, explosionVariantThree.slot());
        assertEquals(0x02, explosionVariantThree.type());
        assertEquals(0x6530, explosionVariantThree.spriteDefinition().address());
        assertEquals(3, explosionVariantThree.spriteVariant());
        assertEquals(0xD8, runtime.physicsFlags(slot));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());

        runtime.setBombTransitionCountdownForTest(slot, 0x14);
        runtime.tick(3, 0x40, 0x50, () -> 0);
        assertEquals(0x6530, runtime.snapshot().slots().get(slot).spriteDefinition().address());
        assertEquals(2, runtime.snapshot().slots().get(slot).spriteVariant());

        runtime.setBombTransitionCountdownForTest(slot, 0x10);
        runtime.tick(4, 0x40, 0x50, () -> 0);
        assertEquals(1, runtime.snapshot().slots().get(slot).spriteVariant());

        runtime.setBombTransitionCountdownForTest(slot, 0x08);
        runtime.tick(5, 0x40, 0x50, () -> 0);
        assertEquals(0, runtime.snapshot().slots().get(slot).spriteVariant());
        assertTrue(runtime.bombActive());

        runtime.setBombTransitionCountdownForTest(slot, 0x01);
        runtime.tick(6, 0x40, 0x50, () -> 0);

        RoomEntity finalExplosion = runtime.snapshot().slots().get(slot);
        assertEquals(slot, finalExplosion.slot());
        assertEquals(0x02, finalExplosion.type());
        assertEquals(EntityStatus.ACTIVE, finalExplosion.status());
        assertEquals(0x6530, finalExplosion.spriteDefinition().address());
        assertEquals(0, finalExplosion.spriteVariant());
        assertEquals(0xD8, runtime.physicsFlags(slot));
        assertFalse(runtime.bombActive());
        assertEquals(1, runtime.snapshot().loadedEntities().size());

        runtime.tick(7, 0x40, 0x50, () -> 0);

        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(slot).status());
        assertFalse(runtime.bombActive());
        assertEquals(0, runtime.physicsFlags(slot));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void spawnBombFinalizesPendingFinalPresentationBeforeReusingItsSlot() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())), false, null, catalog);
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 0);

        runtime.setBombTransitionCountdownForTest(slot, 0x01);
        runtime.tick(0, 0x40, 0x50, () -> 0);

        assertFalse(runtime.bombActive());
        assertEquals(slot, runtime.spawnBomb(0x44, 0x54, 0, 1));
        RoomEntity replacement = runtime.snapshot().slots().get(slot);
        assertEquals(EntityStatus.ACTIVE, replacement.status());
        assertEquals(0x02, replacement.type());
        assertEquals(0x652E, replacement.spriteDefinition().address());
        assertEquals(0xA0, runtime.transitionCountdown(slot));
        assertTrue(runtime.bombActive());
        assertEquals(1, runtime.snapshot().loadedEntities().size());
    }

    @Test
    void alternateBombDisableClearsPendingPresentationAndDirectionBeforeReuse()
        throws Exception {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())), false, null, catalog);
        int slot = runtime.spawnBomb(0x40, 0x50, 0, 3);

        runtime.setBombTransitionCountdownForTest(slot, 0x01);
        runtime.tick(0, 0x40, 0x50, () -> 0);

        Field pendingField = RoomEntityRuntime.class
            .getDeclaredField("bombFinalPresentationPending");
        pendingField.setAccessible(true);
        boolean[] pending = (boolean[]) pendingField.get(runtime);
        assertTrue(pending[slot]);

        Method disable = RoomEntityRuntime.class
            .getDeclaredMethod("disableEntityWithoutPersistence", int.class);
        disable.setAccessible(true);
        disable.invoke(runtime, slot);

        assertFalse(pending[slot]);
        assertEquals(0xFF, runtime.bombDirection(slot));
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(slot).status());
        assertEquals(slot, runtime.spawnBomb(0x44, 0x54, 0, 1));
        assertEquals(1, runtime.bombDirection(slot));
        assertFalse(pending[slot]);
    }

    @Test
    void bombExplosionPublishesOnlyTargetsInsideTheInclusiveCountdownWindow() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshotWithSlots(
            new RoomEntity(11, 11, 0x4D, 0x40, 0x67, EntityStatus.ACTIVE,
                pairDefinition(0x4D, 1), 0),
            new RoomEntity(12, 12, 0x4D, 0x58, 0x4F, EntityStatus.ACTIVE,
                pairDefinition(0x4D, 1), 0),
            new RoomEntity(13, 13, 0x4D, 0x57, 0x66, EntityStatus.ACTIVE,
                pairDefinition(0x4D, 1), 0),
            new RoomEntity(14, 14, 0x4D, 0x28, 0x4F, EntityStatus.ACTIVE,
                pairDefinition(0x4D, 1), 0)));
        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        runtime.setBombTransitionCountdownForTest(bombSlot, 0x13);

        runtime.tick(0, 0, 0, () -> 0);

        List<BombExplosionEvent> events = runtime.consumeBombExplosionEvents();
        assertEquals(List.of(13, 12), events.stream()
            .filter(event -> !event.targetsRoomObjects())
            .map(BombExplosionEvent::targetSlot)
            .toList());
        assertEquals(0x04, runtime.swordMoblinAlertingSoundCounter());
        for (BombExplosionEvent event : events) {
            assertEquals(bombSlot, event.bombSlot());
            assertEquals(0x48, event.bombX());
            assertEquals(0x4F, event.bombVisualY());
            assertEquals(0x12, event.countdown());
            assertEquals(0x07, event.damageType());
        }
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());

        runtime.tick(1, 0, 0, () -> 0);
        List<BombExplosionEvent> nextFrame = runtime.consumeBombExplosionEvents();
        assertEquals(List.of(BombExplosionEvent.OBJECT_TARGET), nextFrame.stream()
            .map(BombExplosionEvent::targetSlot).toList());
    }

    @Test
    void bombExplosionFiltersNonInteractiveTargetsAtTheExactInteractionTick() throws Exception {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshotWithSlots(
            new RoomEntity(10, 10, 0x4D, 0x40, 0x4F, EntityStatus.ACTIVE,
                pairDefinition(0x4D, 1), 0),
            new RoomEntity(11, 11, 0x4D, 0x40, 0x4F, EntityStatus.ACTIVE,
                pairDefinition(0x4D, 1), 0),
            new RoomEntity(12, 12, 0x4D, 0x40, 0x4F, EntityStatus.ACTIVE,
                pairDefinition(0x4D, 1), 0),
            new RoomEntity(13, 13, 0x05, 0x40, 0x4F, EntityStatus.ACTIVE,
                pairDefinition(0x05, 1), 0),
            new RoomEntity(14, 14, 0x4D, 0x40, 0x4F, EntityStatus.ACTIVE,
                pairDefinition(0x4D, 1), 0)));
        setPhysicsFlags(runtime, 14, 0x40);
        runtime.setHitboxFlagsForTest(12, 0x80);
        runtime.setEnemyIgnoreHitsCountdownForTest(11, 0x02);
        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        runtime.setBombTransitionCountdownForTest(bombSlot, 0x13);

        runtime.tick(0, 0, 0, () -> 0);

        List<BombExplosionEvent> events = runtime.consumeBombExplosionEvents();
        List<BombExplosionEvent> targetEvents = events.stream()
            .filter(event -> !event.targetsRoomObjects())
            .toList();
        assertEquals(2, targetEvents.size());
        assertEquals(List.of(11, 10), targetEvents.stream()
            .map(BombExplosionEvent::targetSlot).toList());
        assertEquals(0x12, targetEvents.getFirst().countdown());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void bombExplosionAppliesRomDamageAndFinalSourceRelativeRecoil() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntity octorok = new RoomEntity(14, 14, 0x09, 0x40, 0x4F,
            EntityStatus.ACTIVE, pairDefinition(0x09, 8), 0);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            snapshotWithSlots(octorok), false, () -> 0, null, tables);

        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        runtime.setBombTransitionCountdownForTest(bombSlot, 0x13);

        // The bomb candidate is placed at ($48,$50,$01), so the target's
        // visual Y is aligned with the explosion while its X is eight pixels
        // to the left. The ROM's final length-$30 vector must therefore be
        // -$30 on X and zero on Y.
        runtime.tick(0, 0x40, 0x50, () -> 0);

        assertEquals(0, runtime.enemyHealth(14));
        assertEquals(EntityStatus.DYING, runtime.snapshot().slots().get(14).status());
        // CheckExplosionInteractionWithEntities does not pre-arm the normal
        // sword hit flash/ignore timers; a lethal ApplySwordDamagesToEnemy
        // path reaches DYING with both values still clear.
        assertEquals(0x00, runtime.enemyFlashCountdown(14));
        assertEquals(0x00, runtime.enemyIgnoreHitsCountdown(14));
        assertEquals(0xD0, runtime.enemyRecoilSpeedXForTest(14));
        assertEquals(0x00, runtime.enemyRecoilSpeedYForTest(14));
        assertEquals(List.of(new EntityCombatEvent(14, 0x09, 0, false,
            1, -1, EntityCombatEvent.SoundChannel.JINGLE, 0x03,
            EntityCombatEvent.SoundChannel.NONE, -1, null)),
            runtime.consumePendingEntityEvents());

        assertEquals(List.of(14), runtime.consumeBombExplosionEvents().stream()
            .filter(event -> !event.targetsRoomObjects())
            .map(BombExplosionEvent::targetSlot)
            .toList());
    }

    @Test
    void enemyBombExplosionUsesTheSourceLinkCollisionBranch() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            snapshot(), false, () -> 0, catalog, tables);

        int bombSlot = runtime.spawnEnemyBomb(0x48, 0x50, 0x00, 0x13);
        List<EntityProjectileEvent> events = runtime.tickWithProjectileEvents(
            0, 0x48, 0x50, () -> 0, null,
            new EnemyProjectileCollision.LinkState(
                0x48, 0x50, 0x00, 0x00, 0x03, false, 1, 0x00),
            0x10, 0xF0);

        assertEquals(List.of(new EntityProjectileEvent(
            bombSlot, 0x02, EntityProjectileEvent.Kind.LINK_DAMAGE, 0x00, 0x08,
            EntityProjectileEvent.SoundChannel.WAVE, 0x03, false, false,
            0x00, 0x00, 0x20, 0xE0, 0x10)), events);
        assertEquals(0x04, runtime.swordMoblinAlertingSoundCounter());
        assertEquals(List.of(BombExplosionEvent.OBJECT_TARGET),
            runtime.consumeBombExplosionEvents().stream()
                .map(BombExplosionEvent::targetSlot).toList());
    }

    @Test
    void enemyBombUsesRawEntityYInsteadOfExplosionVisualYForLinkCollision() throws IOException {
        byte[] rom = loadRom();
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            snapshot(), false, () -> 0, new EntitySpriteHandlerCatalog(rom),
            new RomEnemyCombatTables(rom));

        int bombSlot = runtime.spawnEnemyBomb(0x48, 0x68, 0x18, 0x13);
        List<EntityProjectileEvent> events = runtime.tickWithProjectileEvents(
            0, 0x48, 0x50, () -> 0, null,
            new EnemyProjectileCollision.LinkState(
                0x48, 0x50, 0x00, 0x00, 0x03, false, 1, 0x00),
            0x10, 0xF0);

        assertEquals(List.of(), events);
        assertEquals(0x04, runtime.swordMoblinAlertingSoundCounter());
        List<BombExplosionEvent> explosionEvents = runtime.consumeBombExplosionEvents();
        assertEquals(List.of(BombExplosionEvent.OBJECT_TARGET),
            explosionEvents.stream().map(BombExplosionEvent::targetSlot).toList());
        assertEquals(bombSlot, explosionEvents.getFirst().bombSlot());
    }

    @Test
    void bomberUsesItsSourceHandlerToSpawnAnEnemyBomb() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition bomberDefinition = catalog.forEntityType(
            0xBA, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshotWithSlots(
            new RoomEntity(0, 0, 0xBA, 0x50, 0x50, EntityStatus.ACTIVE,
                bomberDefinition, 0, 0, 0, 0x00)),
            false, () -> 0, catalog, tables);

        runtime.tick(0, 0x50, 0x50, () -> 0);

        RoomEntity bomb = runtime.snapshot().slots().get(15);
        assertTrue(bomb.loaded());
        assertEquals(EntityStatus.ACTIVE, bomb.status());
        assertEquals(0x02, bomb.type());
        assertEquals(0x50, bomb.x());
        assertEquals(0x50, bomb.y());
        assertEquals(0x10, bomb.z());
        assertEquals(0x40, runtime.transitionCountdown(15));
        assertEquals(0x01, runtime.enemyIgnoreHitsCountdown(15));
        assertEquals(List.of(new EntityCombatEvent(15, 0x02, 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, 0x08)),
            runtime.consumePendingEntityEvents());
    }

    @Test
    void bomberEnemyBombCarriesTheSourceVectorIntoTheNextBombHandlerFrame()
        throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshotWithSlots(
            new RoomEntity(0, 0, 0xBA, 0x50, 0x50, EntityStatus.ACTIVE,
                catalog.forEntityType(0xBA, EntityRoomLoader.RoomTable.OVERWORLD, -1),
                0, 0, 0, 0x00)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));

        runtime.tick(0, 0x70, 0x50, () -> 0);
        runtime.consumePendingEntityEvents();
        runtime.tick(1, 0x70, 0x50, () -> 0);

        RoomEntity bomb = runtime.snapshot().slots().get(15);
        assertEquals(0x51, bomb.x());
        assertEquals(0x50, bomb.y());
        assertEquals(0x10, bomb.z());
        assertEquals(0x3F, runtime.transitionCountdown(15));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void madBomberUsesItsHoleCycleToSpawnTheSourceEnemyBomb() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x93, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        assertTrue(definition.supported());
        assertEquals(0x06, definition.bank());
        assertEquals(0x4126, definition.address());
        assertEquals(5, definition.variantCount());

        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshotWithSlots(
            new RoomEntity(0, 0, 0x93, 0x50, 0x50, EntityStatus.ACTIVE,
                definition, 0, 0, 0, 0x00)),
            false, () -> 0, catalog, tables);

        for (int frame = 0; frame <= 136; frame++) {
            runtime.tick(frame, 0xC0, 0xC0, () -> 0);
        }

        RoomEntity bomb = runtime.snapshot().slots().get(15);
        assertTrue(bomb.loaded());
        assertEquals(EntityStatus.ACTIVE, bomb.status());
        assertEquals(0x02, bomb.type());
        assertEquals(0x28, bomb.x());
        assertEquals(0x40, bomb.y());
        assertEquals(0x04, bomb.z());
        assertEquals(0x40, runtime.transitionCountdown(15));
        assertEquals(0x01, runtime.enemyIgnoreHitsCountdown(15));
        assertEquals(List.of(new EntityCombatEvent(15, 0x02, 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, 0x08)),
            runtime.consumePendingEntityEvents());
    }

    @Test
    void timerBombiteUsesItsRomFuseToProduceAnEnemyBomb() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x56, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        assertTrue(definition.supported());
        assertEquals(0x04, definition.bank());
        assertEquals(0x7CEF, definition.address());
        assertEquals(6, definition.variantCount());

        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshotWithSlots(
            new RoomEntity(0, 0, 0x56, 0x50, 0x50, EntityStatus.ACTIVE,
                definition, 0, 0, 0, 0x00)),
            true, () -> 0, catalog, tables);
        runtime.setEnemyIgnoreHitsCountdownForTest(0, 0x09);

        boolean explosionSound = false;
        boolean sourceCleared = false;
        for (int frame = 0; frame < 520; frame++) {
            runtime.tick(frame, 0xC0, 0xC0, () -> 0);
            explosionSound |= runtime.consumePendingEntityEvents().stream()
                .anyMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                    && event.soundId() == 0x0C);
            sourceCleared = !runtime.snapshot().slots().get(0).loaded();
            if (sourceCleared) {
                break;
            }
        }

        assertTrue(explosionSound);
        assertTrue(sourceCleared);
        RoomEntity bomb = runtime.snapshot().slots().get(15);
        assertTrue(bomb.loaded());
        assertEquals(EntityStatus.ACTIVE, bomb.status());
        assertEquals(0x02, bomb.type());
        assertEquals(0x00, bomb.z());
        assertEquals(0x17, runtime.transitionCountdown(15));
        assertEquals(0x01, runtime.enemyIgnoreHitsCountdown(15));
    }

    @Test
    void bouncingBombiteUsesItsRomSwordRecoilInsteadOfTakingSwordDamage() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x55, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        assertTrue(definition.supported());
        assertEquals(0x04, definition.bank());
        assertEquals(0x7DF5, definition.address());
        assertEquals(2, definition.variantCount());

        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshotWithSlots(
            new RoomEntity(0, 0, 0x55, 0x50, 0x50, EntityStatus.ACTIVE,
                definition, 0, 0, 0, 0x00)),
            true, () -> 0, catalog, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 0xC0, 0xC0, false, true, true,
            0x58, 1, 0x58, 1, EnemyAttackContext.standard());

        assertEquals(List.of(new EntityCombatEvent(0, 0x55, 0, true, 0, -1,
            EntityCombatEvent.SoundChannel.NONE, -1)), events);
        assertEquals(4, runtime.enemyHealth(0));
        assertEquals(2, runtime.bombiteState(0));
        assertEquals(0x40, runtime.transitionCountdown(0));
        assertEquals(0x08, runtime.bombitePrivateCountdown1(0));
        assertEquals(0, runtime.enemyIgnoreHitsCountdown(0));
    }

    @Test
    void litBouncingBombiteCollidesWithAnotherBombiteUsingTheRomEntityPass()
        throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(
            0x55, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        assertTrue(definition.supported());

        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshotWithSlots(
            new RoomEntity(0, 0, 0x55, 0x50, 0x50, EntityStatus.ACTIVE,
                definition, 0, 0, 0, 0x00),
            new RoomEntity(1, 1, 0x55, 0x58, 0x58, EntityStatus.ACTIVE,
                definition, 0, 0, 0, 0x00)),
            true, () -> 0, catalog, tables);

        List<EntityCombatEvent> swordEvents = runtime.resolveCombat(
            0, 0xC0, 0xC0, false, true, true,
            0x58, 1, 0x58, 1, EnemyAttackContext.standard());
        assertEquals(List.of(new EntityCombatEvent(0, 0x55, 0, true, 0, -1,
            EntityCombatEvent.SoundChannel.NONE, -1)), swordEvents);
        int sourceSpeedX = runtime.bombiteSpeedX(0);
        int sourceSpeedY = runtime.bombiteSpeedY(0);

        // Frame 1 selects target slot 1 in func_003_75A2's alternating pass.
        // The source moves three pixels toward the upper-left first, remaining
        // inside the ROM's strict twelve-pixel entity-collision windows.
        runtime.tick(1, 0xC0, 0xC0, () -> 0);

        assertFalse(runtime.snapshot().slots().get(0).loaded());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(1).status());
        assertEquals(2, runtime.bombiteState(1));
        assertEquals(0x40, runtime.transitionCountdown(1));
        assertEquals(0x08, runtime.bombitePrivateCountdown1(1));
        assertEquals(sourceSpeedX, runtime.bombiteSpeedX(1));
        assertEquals(sourceSpeedY, runtime.bombiteSpeedY(1));

        RoomEntity bomb = runtime.snapshot().slots().get(15);
        assertTrue(bomb.loaded());
        assertEquals(EntityStatus.ACTIVE, bomb.status());
        assertEquals(0x02, bomb.type());
        assertEquals(0x00, bomb.z());
        assertEquals(0x17, runtime.transitionCountdown(15));
        assertEquals(0x01, runtime.enemyIgnoreHitsCountdown(15));
        assertEquals(List.of(new EntityCombatEvent(15, 0x02, 0, false,
            EntityCombatEvent.SoundChannel.NOISE, 0x0C)),
            runtime.consumePendingEntityEvents());
    }

    @Test
    void protectedEnemyBombStillDoublesLinkSpeedWithoutDamageOrHurtSound() throws IOException {
        byte[] rom = loadRom();
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            snapshot(), false, () -> 0, new EntitySpriteHandlerCatalog(rom),
            new RomEnemyCombatTables(rom));

        int bombSlot = runtime.spawnEnemyBomb(0x48, 0x50, 0x00, 0x13);
        List<EntityProjectileEvent> events = runtime.tickWithProjectileEvents(
            0, 0x48, 0x50, () -> 0, null,
            new EnemyProjectileCollision.LinkState(
                0x48, 0x50, 0x00, 0x00, 0x03, false, 1, 0x01),
            0x10, 0xF0);

        assertEquals(List.of(new EntityProjectileEvent(
            bombSlot, 0x02, EntityProjectileEvent.Kind.LINK_DAMAGE, 0x00, 0x00,
            EntityProjectileEvent.SoundChannel.NONE, -1, false, false,
            0x00, 0x00, 0x20, 0xE0, 0x00)), events);
        assertEquals(0x04, runtime.swordMoblinAlertingSoundCounter());
        List<BombExplosionEvent> explosionEvents = runtime.consumeBombExplosionEvents();
        assertEquals(List.of(BombExplosionEvent.OBJECT_TARGET),
            explosionEvents.stream().map(BombExplosionEvent::targetSlot).toList());
        assertEquals(bombSlot, explosionEvents.getFirst().bombSlot());
    }

    @Test
    void bombExplosionUsesNonlethalRomDamageBeforeTheTargetRecoilHandler()
        throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntity ghini = new RoomEntity(14, 14, 0x12, 0x50, 0x4F,
            EntityStatus.ACTIVE, pairDefinition(0x12, 2), 0, 0, 0, 0x10);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            snapshotWithSlots(ghini), false, () -> 0, null, tables);

        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);
        runtime.setBombTransitionCountdownForTest(bombSlot, 0x13);

        runtime.tick(0, 0x40, 0x50, () -> 0);

        assertEquals(4, runtime.enemyHealth(14));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(14).status());
        // The lower-slot target's handler decrements the newly written flash
        // once after the higher-slot bomb has applied it.
        assertEquals(0x17, runtime.enemyFlashCountdown(14));
        assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(14));
        assertEquals(0x30, runtime.enemyRecoilSpeedXForTest(14));
        assertEquals(0x00, runtime.enemyRecoilSpeedYForTest(14));
        assertEquals(List.of(new EntityCombatEvent(14, 0x12, 0, false,
            4, -1, EntityCombatEvent.SoundChannel.JINGLE, 0x03,
            EntityCombatEvent.SoundChannel.NONE, -1, null)),
            runtime.consumePendingEntityEvents());
    }

    @Test
    void bombExplosionObjectWindowIsInclusiveAndSilentOutsideIt() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot());
        int bombSlot = runtime.spawnBomb(0x40, 0x50, 0, 0);

        runtime.setBombTransitionCountdownForTest(bombSlot, 0x18);
        runtime.tick(0, 0, 0, () -> 0);
        assertTrue(runtime.consumeBombExplosionEvents().isEmpty());

        runtime.setBombTransitionCountdownForTest(bombSlot, 0x17);
        runtime.tick(1, 0, 0, () -> 0);
        assertEquals(List.of(0x16), runtime.consumeBombExplosionEvents().stream()
            .map(BombExplosionEvent::countdown).toList());

        runtime.setBombTransitionCountdownForTest(bombSlot, 0x0F);
        runtime.tick(2, 0, 0, () -> 0);
        assertEquals(List.of(0x0E), runtime.consumeBombExplosionEvents().stream()
            .map(BombExplosionEvent::countdown).toList());

        runtime.setBombTransitionCountdownForTest(bombSlot, 0x0E);
        runtime.tick(3, 0, 0, () -> 0);
        assertTrue(runtime.consumeBombExplosionEvents().isEmpty());
    }

    private static void setPhysicsFlags(RoomEntityRuntime runtime, int slot, int flags) {
        runtime.setPhysicsFlagsForTest(slot, flags);
    }

    @Test
    void clearingAnEntityResetsTheRomHitboxIgnoreFlag() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot());
        runtime.setHitboxFlagsForTest(0, 0x80);

        runtime.clearEntity(0);

        assertEquals(0, runtime.hitboxFlagsForTest(0));
    }

    private static RoomEntity ghiniEntity(EntitySpriteHandlerCatalog catalog, int slot, int type,
                                          EntityStatus status) {
        EntitySpriteDefinition definition = catalog.forEntityType(
            type, EntityRoomLoader.RoomTable.OVERWORLD);
        int initialZ = type == 0x12 ? 0x10 : 0;
        return new RoomEntity(slot, slot, type, 0x50, 0x50, status, definition,
            definition.initialVariant(), 0, 0, initialZ);
    }

    private static EntitySpriteDefinition pairDefinition(int type, int variants) {
        List<EntitySpriteDefinition.Variant> displayList = new ArrayList<>();
        for (int i = 0; i < variants; i++) {
            displayList.add(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x14, 0x02),
                new EntitySpriteDefinition.OamAttribute(0x14, 0x22)));
        }
        return new EntitySpriteDefinition(type, 0x03, 0x5B65,
            EntitySpriteDefinition.Shape.PAIR, 0, displayList);
    }

    private static EntitySpriteDefinition keyDropDefinition() {
        List<EntitySpriteDefinition.Variant> displayList = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            displayList.add(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0xCA, 0x17), null));
        }
        return new EntitySpriteDefinition(0x30, 0x03, 0x5C78,
            EntitySpriteDefinition.Shape.SINGLE, 0, displayList);
    }

    private static EntitySpriteDefinition butterflyDefinition() {
        return new EntitySpriteDefinition(0x6E, 0x06, 0x6BBD,
            EntitySpriteDefinition.Shape.SINGLE, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x5E, 0x01), null),
            new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x5E, 0x41), null)));
    }

    private static EntitySpriteDefinition keeseDefinition() {
        return new EntitySpriteDefinition(0x19, 0x06, 0x6708,
            EntitySpriteDefinition.Shape.PAIR, 0, List.of(
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x42, 0x00),
                    new EntitySpriteDefinition.OamAttribute(0x42, 0x20)),
                new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(0x40, 0x00),
                    new EntitySpriteDefinition.OamAttribute(0x40, 0x20))));
    }

    private static RoomEntity dyingEntity(int slot, int sourceLoadOrder, int type,
                                           EntitySpriteHandlerCatalog catalog) {
        EntitySpriteDefinition definition = catalog.forEntityType(
            type, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        return new RoomEntity(slot, sourceLoadOrder, type, 0x40, 0x50,
            EntityStatus.DYING, definition, definition.initialVariant());
    }

    private static RoomEntitySnapshot snapshotAt(RoomEntity entity) {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        slots.set(entity.slot(), entity);
        return new RoomEntitySnapshot(slots);
    }

    private static RoomEntitySnapshot snapshotWithSlots(RoomEntity... entities) {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        for (RoomEntity entity : entities) {
            slots.set(entity.slot(), entity);
        }
        return new RoomEntitySnapshot(slots);
    }

    private static IntSupplier sequence(int... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[Math.min(index.getAndIncrement(), values.length - 1)];
    }

    private static final class RuntimeColorShellWorld implements ColorShellWorld {
        private int objectValue;
        private final List<Integer> objectWrites = new ArrayList<>();
        private final List<Integer> noises = new ArrayList<>();
        private final List<List<Integer>> poofs = new ArrayList<>();

        @Override
        public int objectAt(RoomEntity entity, int relativeOffset) {
            return objectValue;
        }

        @Override
        public void writeObject(RoomEntity entity, int objectId) {
            objectWrites.add(objectId);
        }

        @Override
        public void playJingle(int id) {
        }

        @Override
        public void playNoise(int id) {
            noises.add(id);
        }

        @Override
        public void spawnPoof(int x, int y) {
            poofs.add(List.of(x, y));
        }
    }

    private static byte[] romWithSwordResult(int entityType, int rawValue) throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        int healthGroup = tables.healthGroup(entityType);
        int damageMatrixOffset = RomBank.romOffset(0x03, 0x43EC);
        int damageValuesOffset = RomBank.romOffset(0x03, 0x473C);
        // Force level-one sword damage to use entry 1, then replace that
        // shared raw value with the controlled special result under test.
        rom[damageMatrixOffset + healthGroup * 0x10] = 0x01;
        rom[damageValuesOffset + 0x01] = (byte) rawValue;
        return rom;
    }

    private static byte[] syntheticRom() {
        return new byte[0x100000];
    }

    private static void write(byte[] rom, int bank, int address, int... values) {
        int offset = RomBank.romOffset(bank, address);
        for (int value : values) {
            rom[offset++] = (byte) value;
        }
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = RoomEntityRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }

    private static RoomEntitySnapshot snapshot(RoomEntity... entities) {
        List<RoomEntity> slots = new ArrayList<>(List.of(entities));
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots);
    }
}
