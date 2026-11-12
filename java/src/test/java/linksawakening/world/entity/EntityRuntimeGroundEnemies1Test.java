package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeGroundEnemies1Test extends EntityRuntimeTestSupport {

    @Test
    void octorokUsesTheRomLongFallingCountdown() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
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
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
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
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x14)
                .at(0x50, 0x50)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .spriteVariant(6)
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
    void octorokUsesTheSharedRoamingEnemyTimersAndDirectionSpeeds() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        IntSupplier randomBytes = sequence(0x02, 0x03);

        // State 0 pauses for ($10 + random & $0F) frames, then state 1
        // selects direction 3 (down) and the ROM's +8/16 fixed-point speed.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 200, 32, randomBytes));
        assertEquals(1, runtime.enemies().octorokState(0));
        assertEquals(0x12, runtime.enemies().octorokTransitionCountdown(0));
        assertEquals(0, runtime.enemies().octorokSpeedY(0));

        for (int frame = 1; frame <= 0x12; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 200, 32, randomBytes));
        }
        assertEquals(
                0,
                runtime.enemies().octorokState(0),
                () -> "state=" + runtime.enemies().octorokState(0) + " timer="
                        + runtime.enemies().octorokTransitionCountdown(0));
        assertEquals(0x23, runtime.enemies().octorokTransitionCountdown(0));
        assertEquals(3, runtime.enemies().octorokDirection(0));
        assertEquals(8, runtime.enemies().octorokSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());

        runtime.tick(EntityTestFrames.frame(runtime, 0x13, 200, 32, randomBytes));
        runtime.tick(EntityTestFrames.frame(runtime, 0x14, 200, 32, randomBytes));
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void runtimePassesCreditsContextToSuppressOnlyOctorokLaunches() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, () -> 0, true));
        for (int frame = 1; frame <= 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, () -> 0, true));
        }

        assertTrue(runtime.inspection().projectileLaunchRequests().isEmpty());
        assertEquals(1, runtime.inspection().snapshot().loadedEntities().size());
    }

    @Test
    void moblinUsesTheSameRomRoamingStateMachineAndDirectionalDisplayPairs() {
        EntitySpriteDefinition definition = pairDefinition(0x0B, 8);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        IntSupplier randomBytes = sequence(0x02, 0x03);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 200, 32, randomBytes));
        for (int frame = 1; frame <= 0x12; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 200, 32, randomBytes));
        }

        assertEquals(0, runtime.enemies().octorokState(0));
        assertEquals(3, runtime.enemies().octorokDirection(0));
        assertEquals(8, runtime.enemies().octorokSpeedY(0));
        runtime.tick(EntityTestFrames.frame(runtime, 0x13, 200, 32, randomBytes));
        runtime.tick(EntityTestFrames.frame(runtime, 0x14, 200, 32, randomBytes));
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void moblinSwordUsesTheBankSevenIdleAlertAndSlowWalkingStates() {
        EntitySpriteDefinition definition = pairDefinition(0x14, 8);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x14)
                .at(0x50, 64)
                .spriteDefinition(definition)
                .spriteVariant(6)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0xC0, 64, () -> 0));
        assertEquals(1, runtime.enemies().moblinSwordState(0));
        assertEquals(0x80, runtime.enemies().moblinSwordTransitionCountdown(0));
        assertEquals(0, runtime.enemies().moblinSwordDirection(0));
        assertEquals(0x06, runtime.enemies().moblinSwordSpeedX(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0xC0, 64, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0xC0, 64, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 3, 0xC0, 64, () -> 0));
        assertEquals(0x51, runtime.inspection().snapshot().slots().get(0).x());

        runtime.tick(EntityTestFrames.frame(runtime, 4, 0x70, 64, () -> 0));
        assertEquals(2, runtime.enemies().moblinSwordState(0));
        assertEquals(0x80, runtime.enemies().moblinSwordTransitionCountdown(0));
        assertEquals(0x10, runtime.enemies().moblinSwordPrivateCountdown1(0));
        assertEquals(0, runtime.enemies().moblinSwordDirection(0));
    }

    @Test
    void swordMoblinPausesMotionAndTimersDuringDialogue() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x14)
                                .at(0x50, 64)
                                .spriteDefinition(pairDefinition(0x14, 8))
                                .spriteVariant(6)
                                .build()))
                .build();
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0xC0, 64, () -> 0));
        runtime.configuration().setDialogActive(true);
        for (int frame = 1; frame <= 180; frame++)
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0xC0, 64, () -> 0));
        assertEquals(0x50, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x80, runtime.enemies().moblinSwordTransitionCountdown(0));
        runtime.configuration().setDialogActive(false);
        for (int frame = 181; frame <= 184; frame++)
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0xC0, 64, () -> 0));
        assertTrue(runtime.inspection().snapshot().slots().get(0).x() > 0x50);
    }

    @Test
    void moblinSwordUsesTheRomHealthContactAndSwordDamageFamily() {
        EntitySpriteDefinition definition = pairDefinition(0x14, 8);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x14)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> contact = runtime.resolveCombat(1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit = runtime.resolveCombat(0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, firstHit.size());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(0x18, runtime.enemies().enemyFlashCountdown(0));
        assertTrue(runtime.enemies().enemyRecoilActive(0));
    }

    @Test
    void projectileWallImpactAlertsMoblinSwordSlotsProcessedLaterInTheFrame() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshotWithSlots(
                        new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x14)
                                .at(0x50, 64)
                                .spriteDefinition(pairDefinition(0x14, 8))
                                .spriteVariant(6)
                                .build(),
                        new linksawakening.world.entity.RoomEntityBuilder()
                                .slot(1)
                                .sourceLoadOrder(1)
                                .type(0x0C)
                                .at(0x20, 64)
                                .spriteDefinition(pairDefinition(0x0C, 4))
                                .build()))
                .build();

        runtime.tick(EntityTestFrames.frame(
                runtime,
                0,
                0xC0,
                64,
                () -> 0,
                (entity, direction, nextX, nextY) -> entity.type() == 0x0C && direction == 0));

        assertEquals(0x04, runtime.enemies().swordMoblinAlertingSoundCounter());
        assertEquals(2, runtime.enemies().moblinSwordState(0));
        assertEquals(0x10, runtime.enemies().moblinSwordPrivateCountdown1(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0xC0, 64, () -> 0, (entity, direction, nextX, nextY) -> false));
        assertEquals(0x03, runtime.enemies().swordMoblinAlertingSoundCounter());
    }

    @Test
    void hideoutMoblinSwordPublishesDialog190OnlyOnce() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x14)
                                .at(0x50, 0x40)
                                .spriteDefinition(pairDefinition(0x14, 8))
                                .spriteVariant(6)
                                .build()))
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityMapIdForTest(0x15);
        EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(0x04);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0xC0, 0x40, () -> 0));
        assertEquals(List.of(new DialogRequest(1, 0x90)), runtime.consumePendingDialogRequests());
        assertEquals(1, runtime.enemies().moblinSwordPrivateState3(0));

        runtime.tick(EntityTestFrames.frame(runtime, 3, 0x70, 0x40, () -> 0));
        assertTrue(runtime.consumePendingDialogRequests().isEmpty());
        assertEquals(1, runtime.enemies().moblinSwordPrivateState3(0));
    }

    @Test
    void octorokStopsAtTheRoamingEnemyBackgroundCollisionPoint() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();
        IntSupplier randomBytes = sequence(0x00, 0x03, 0x01);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 200, 32, randomBytes));
        for (int frame = 1; frame <= 0x12; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 200, 32, randomBytes));
        }

        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 3;
        runtime.tick(EntityTestFrames.frame(runtime, 0x13, 200, 32, randomBytes, wall));
        runtime.tick(EntityTestFrames.frame(runtime, 0x14, 200, 32, randomBytes, wall));
        runtime.tick(EntityTestFrames.frame(runtime, 0x15, 200, 32, randomBytes, wall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0, runtime.enemies().octorokSpeedY(0));
    }

    @Test
    void octorokUsesTheNormalEnemyHitboxAndOneBasicSwordDamage() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> contact = runtime.resolveCombat(1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        RoomEntityRuntime swordRuntime = RoomEntityRuntime.builder(initial).build();
        List<EntityCombatEvent> events = swordRuntime.resolveCombat(0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertTrue(events.get(0).swordHit());
        assertEquals(
                EntityStatus.DYING,
                swordRuntime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void moblinUsesTwoHealthPointsAndTheRomDamageCooldown() {
        EntitySpriteDefinition definition = pairDefinition(0x0B, 8);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> contact = runtime.resolveCombat(1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit = runtime.resolveCombat(0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, firstHit.size());
        assertTrue(firstHit.get(0).swordHit());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemies().enemyHealth(0));
        assertEquals(0x18, runtime.enemies().enemyFlashCountdown(0));
        assertTrue(runtime.resolveCombat(1, 120, 120, false, true, true, 72, 1, 72, 1)
                .isEmpty());

        for (int frame = 1; frame <= 0x18; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(0, runtime.enemies().enemyFlashCountdown(0));

        RoomEntity movedMoblin = runtime.inspection().snapshot().slots().get(0);
        List<EntityCombatEvent> secondHit = runtime.resolveCombat(
                0x19, 120, 120, false, true, true, movedMoblin.x() + 8, 1, movedMoblin.y() + 8, 1);
        assertEquals(1, secondHit.size());
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, runtime.enemies().enemyHealth(0));
    }

    @Test
    void moblinSwordHitConfiguresRomRecoilAndEnemyHitJingle() {
        EntitySpriteDefinition definition = pairDefinition(0x0B, 8);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> events = runtime.resolveCombat(0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        EntityCombatEvent event = events.get(0);
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, event.soundChannel());
        assertEquals(0x03, event.soundId());
        assertEquals(0x18, runtime.enemies().enemyFlashCountdown(0));
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.enemies().enemyRecoilActive(0));
        assertEquals(0xD0, runtime.enemies().enemyRecoilSpeedX(0));
        assertEquals(0xD0, runtime.enemies().enemyRecoilSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));

        RoomEntity afterRecoil = runtime.inspection().snapshot().slots().get(0);
        assertEquals(61, afterRecoil.x(), "x=" + afterRecoil.x());
        assertEquals(61, afterRecoil.y(), "y=" + afterRecoil.y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void lethalOctorokSwordHitStillPublishesFinalEnemyHitJingle() {
        EntitySpriteDefinition definition = pairDefinition(0x09, 8);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> events = runtime.resolveCombat(0, 120, 120, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
        assertEquals(0x03, events.get(0).soundId());
    }

    @Test
    void romBurningGibdoBecomesAnActiveStalfosAfterItsTimerExpires() throws IOException {
        byte[] rom = romWithSwordResult(0x1F, 0xFE);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x1F)
                                .at(64, 64)
                                .spriteDefinition(pairDefinition(0x1F, 2))
                                .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.resolveCombat(0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(0xFE, events.get(0).enemySpecialAction());

        for (int frame = 0; frame < 0x60; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(0x1E, runtime.inspection().snapshot().slots().get(0).type());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, runtime.enemies().transitionCountdown(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));
        assertEquals(
                0x4E7D,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(0)
                        .spriteDefinition()
                        .address());
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void hardHatUsesTheRomTargetVectorAndFourFrameSpeedRefresh() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x20)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 72, sequence(0x00)));
        assertEquals(1, runtime.enemies().hardHatSpeedX(0));
        assertEquals(1, runtime.enemies().hardHatSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());

        for (int frame = 1; frame <= 4; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 72, sequence(0x00)));
        }
        assertEquals(2, runtime.enemies().hardHatSpeedX(0));
        assertEquals(2, runtime.enemies().hardHatSpeedY(0));
    }

    @Test
    void hardHatUsesItsRomNormalEnemyHealthAndContactValues() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x20)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> contact = runtime.resolveCombat(1, 64, 64, false, true, false, 0, 0, 0, 0);
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit = runtime.resolveCombat(0, 120, 120, false, true, true, 72, 1, 72, 1);
        assertEquals(1, firstHit.size());
        assertEquals(0x03, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void hardHatSwordHitConfiguresAndAppliesTheRomSharedRecoil() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x20)
                                .at(64, 64)
                                .spriteDefinition(definition)
                                .build()))
                .build();

        List<EntityCombatEvent> events = runtime.resolveCombat(0, 72, 72, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size());
        assertEquals(0x03, runtime.enemies().enemyHealth(0));
        assertEquals(0x18, runtime.enemies().enemyFlashCountdown(0));
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertTrue(runtime.enemies().enemyRecoilActive(0));
        assertEquals(0xD0, runtime.enemies().enemyRecoilSpeedX(0));
        assertEquals(0xD0, runtime.enemies().enemyRecoilSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x00)));

        RoomEntity afterRecoil = runtime.inspection().snapshot().slots().get(0);
        assertEquals(61, afterRecoil.x());
        assertEquals(61, afterRecoil.y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void hardHatSharedRecoilKeepsStateWhenTheRomBackgroundPathBlocksIt() {
        EntitySpriteDefinition definition = pairDefinition(0x20, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x20)
                                .at(64, 64)
                                .spriteDefinition(definition)
                                .build()))
                .build();

        runtime.resolveCombat(0, 72, 72, false, true, true, 72, 1, 72, 1);
        RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) -> direction == 1 || direction == 2;

        runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x00), wall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
        assertTrue(runtime.enemies().enemyRecoilActive(0));
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void tektiteAppliesBankSixRecoilBeforeItsOrdinaryMotion() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x0D)
                                .at(64, 64)
                                .spriteDefinition(definition)
                                .build()))
                .build();

        runtime.resolveCombat(0, 72, 72, false, true, true, 72, 1, 72, 1);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 72, 72, sequence(0x00)));

        assertEquals(61, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(61, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
    }

    @Test
    void armosWakesOnLinkCollisionThenChargesForTheRomCountdown() {
        EntitySpriteDefinition definition = pairDefinition(0x0F, 2);
        RoomEntitySnapshot initial = snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0F)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 64, 64, sequence(0x00)));
        assertEquals(1, runtime.enemies().armosState(0));
        assertEquals(0x30, runtime.enemies().armosTransitionCountdown(0));
        assertEquals(0, runtime.enemies().armosSpeedX(0));
        assertEquals(0, runtime.enemies().armosSpeedY(0));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00)));
        assertEquals(0x2F, runtime.enemies().armosTransitionCountdown(0));
        assertEquals(0xF8, runtime.enemies().armosSpeedX(0));
        assertEquals(0, runtime.enemies().armosSpeedY(0));

        for (int frame = 2; frame <= 0x30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(2, runtime.enemies().armosState(0));
        assertEquals(0, runtime.enemies().armosTransitionCountdown(0));
        assertEquals(0, runtime.enemies().armosSpeedX(0));
        assertEquals(0, runtime.enemies().armosSpeedY(0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void armosStatueRequestsLinkFinalPositionWithoutTeleportingTheStatue() {
        EntitySpriteDefinition definition = pairDefinition(0x0F, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(new linksawakening.world.entity.RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x0F)
                                .at(56, 64)
                                .spriteDefinition(definition)
                                .build()))
                .build();

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                64,
                64,
                sequence(0x00),
                null,
                // The statue uses CheckLinkCollisionWithEnemy.collisionEvenInTheAir;
                // Link's Z must not suppress this collision.
                new EnemyProjectileCollision.LinkState(64, 64, 0x10, 0, 0, false)));

        RoomEntity statue = runtime.inspection().snapshot().slots().get(0);
        assertEquals(56, statue.x());
        assertEquals(64, statue.y());
        assertEquals(List.of(new LinkFinalPositionRequest(0)), runtime.consumePendingLinkFinalPositionRequests());
    }
}
