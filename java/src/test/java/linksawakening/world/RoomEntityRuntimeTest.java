package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomEntityRuntimeTest {

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

        for (int frame = 1; frame < 0x40; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertTrue(runtime.snapshot().slots().get(0).loaded());
        runtime.tick(0x40, 120, 120, sequence(0x00));
        assertFalse(runtime.snapshot().slots().get(0).loaded());
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
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x1F, 64, 64, EntityStatus.ACTIVE,
                pairDefinition(0x1F, 2), 0)), false, null, null, tables);

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(0xFE, events.get(0).enemySpecialAction());

        for (int frame = 0; frame < 0x60; frame++) {
            runtime.tick(frame, 120, 120, sequence(0x00));
        }
        assertEquals(0x1E, runtime.snapshot().slots().get(0).type());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.transitionCountdown(0));
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
    void spikeTrapUsesTheRomHealthGroupNineCombatValues() {
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
        assertTrue(sword.get(0).swordHit());
        assertEquals(3, runtime.enemyHealth(0));
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

        assertEquals(0, runtime.clearEntity(15));
        assertEquals(0, runtime.pairoddProjectileSpeedX(15));
        assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(15).status());
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

    private static IntSupplier sequence(int... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[Math.min(index.getAndIncrement(), values.length - 1)];
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
