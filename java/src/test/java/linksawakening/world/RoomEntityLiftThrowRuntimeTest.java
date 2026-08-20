package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomEntityLiftThrowRuntimeTest {

    @Test
    void liftedStatusUsesTheLinkAnchorAndPublishesTheRomCarryState() {
        RoomEntityRuntime runtime = runtimeWithEntity(EntityStatus.ACTIVE);

        assertTrue(runtime.beginLift(0, LiftedEntityMotion.ROM_DIRECTION_RIGHT));
        runtime.tick(0, 0x50, 0x60, () -> 0, null, null, 0x04,
            3, 0);

        RoomEntity lifted = runtime.snapshot().slots().get(0);
        assertEquals(EntityStatus.LIFTED, lifted.status());
        assertEquals(0x60, lifted.x());
        assertEquals(0x60, lifted.y());
        assertEquals(0x04, lifted.z());
        assertEquals(1, runtime.liftedEntityState().phase());
        assertEquals(0x37, runtime.liftedEntityState().carryState());
    }

    @Test
    void fullyHeldEntityCanHandOffToThrownMotion() {
        RoomEntityRuntime runtime = runtimeWithEntity(EntityStatus.ACTIVE);
        assertTrue(runtime.beginLift(0, LiftedEntityMotion.ROM_DIRECTION_RIGHT));

        int frame = 0;
        while (runtime.liftedEntityState().carryState() != 0x01 && frame < 64) {
            runtime.tick(frame++, 0x50, 0x60, () -> 0, null, null, 0x00,
                3, 0);
        }
        assertEquals(0x01, runtime.liftedEntityState().carryState());

        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        assertEquals(EntityStatus.THROWN, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.liftedEntityState().carryState());

        runtime.tick(frame, 0, 0, () -> 0);
        RoomEntity thrown = runtime.snapshot().slots().get(0);
        assertEquals(EntityStatus.THROWN, thrown.status());
        assertEquals(0x53, thrown.x());
        assertEquals(0x0E, thrown.z());
    }

    @Test
    void thrownEntityPublishesTheRichObjectIdFromABlockedBackgroundProbe() {
        RoomEntityRuntime runtime = runtimeWithEntity(EntityStatus.THROWN);
        runtime.setBackgroundInteraction((entity, direction, nextX, nextY) ->
            EntityBackgroundCollisionResult.blocked(
                direction, 0x35, 0x03, nextX, nextY));

        runtime.tick(0, 0, 0, () -> 0);

        assertEquals(List.of(new RoomEntityRuntime.ThrownBackgroundCollisionEvent(
                0, 0x05, ThrownEntityMotion.ROM_DIRECTION_DOWN, 0x35)),
            runtime.consumePendingThrownBackgroundCollisions());
    }

    @Test
    void thrownBombAlsoPublishesDoorCollisionLikeTheGenericRomThrownHandler() {
        RoomEntityRuntime runtime = runtimeWithEntity(EntityStatus.ACTIVE, 0x02);
        assertTrue(runtime.beginLift(0, ThrownEntityMotion.ROM_DIRECTION_DOWN));
        int frame = 0;
        while (runtime.liftedEntityState().carryState() != 0x01 && frame < 0x40) {
            runtime.tick(frame++, 0x20, 0x30, () -> 0, null, null, 0, 0, 0);
        }
        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_DOWN));
        runtime.setBackgroundInteraction((entity, direction, nextX, nextY) ->
            EntityBackgroundCollisionResult.blocked(
                direction, 0x3C, 0x03, nextX, nextY));

        runtime.tick(frame, 0, 0, () -> 0);

        assertTrue(runtime.consumePendingThrownBackgroundCollisions().stream()
            .anyMatch(event -> event.entityType() == 0x02 && event.objectId() == 0x3C));
    }

    @Test
    void thrownEntityDamagesOverlappingEnemyAfterBounceMotion() {
        EntitySpriteDefinition potDefinition = definition(0x05);
        EntitySpriteDefinition polsVoiceDefinition = definition(0x18);
        List<RoomEntity> slots = new ArrayList<>();
        int targetSlot = 0;
        slots.add(new RoomEntity(targetSlot, 1, 0x18, 0x53, 0x63,
            EntityStatus.ACTIVE, polsVoiceDefinition, 0));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES - 1; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        int thrownSlot = EntityRoomLoader.MAX_ENTITIES - 1;
        slots.add(new RoomEntity(thrownSlot, 0, 0x05, 0x53, 0x60,
            EntityStatus.THROWN, potDefinition, 0));
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(slots), false, () -> 0, null,
            new RomEnemyCombatTables(loadRom()));
        int initialHealth = runtime.enemyHealth(targetSlot);

        runtime.tick(0, 0, 0, () -> 0);

        RoomEntity thrown = runtime.snapshot().slots().get(thrownSlot);
        assertEquals(0x53, thrown.x());
        assertEquals(0x63, thrown.y());
        assertEquals(0, thrown.z());
        assertEquals(4, initialHealth);
        assertEquals(0, runtime.enemyHealth(targetSlot));
        assertEquals(EntityStatus.DYING,
            runtime.snapshot().slots().get(targetSlot).status());
    }

    @Test
    void stunnedLiftableEntityUsesTheHeldPowerBraceletButtonGate() {
        RoomEntityRuntime runtime = runtimeWithEntity(EntityStatus.STUNNED);
        runtime.setPowerBraceletButtonHeld(true);

        runtime.tick(0, 0x20, 0x30, () -> 0, null, null, 0x00, 3, 0);

        assertEquals(EntityStatus.LIFTED, runtime.snapshot().slots().get(0).status());
        assertEquals(0x37, runtime.liftedEntityState().carryState());
    }

    @Test
    void activeSideViewPotUsesItsBraceletPickupHandler() {
        EntitySpriteDefinition definition = definition(0xD6);
        List<RoomEntity> slots = new ArrayList<>();
        slots.add(new RoomEntity(0, 0, 0xD6, 0x50, 0x60,
            EntityStatus.ACTIVE, definition, 0));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
            new RoomEntitySnapshot(slots).withSideScrolling(true));
        runtime.setPowerBraceletButtonHeld(true);

        runtime.tickWithProjectileEvents(0, 0x50, 0x60, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x50, 0x60, 0, 0, 3, false));

        assertEquals(EntityStatus.LIFTED, runtime.snapshot().slots().get(0).status());
        assertEquals(0, runtime.liftedEntityState().slot());
        assertEquals(0, runtime.liftedEntityState().phase());
        assertEquals(0, runtime.liftedEntityState().carryState());
        assertEquals(0x50, runtime.snapshot().slots().get(0).x());
        assertEquals(0x60, runtime.snapshot().slots().get(0).y());
        assertEquals(1, runtime.consumePendingEntityEvents().stream()
            .filter(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.WAVE
                && event.soundId() == 0x02)
            .count());
        assertEquals(0x02, runtime.transitionCountdown(0));
        runtime.tickWithProjectileEvents(1, 0x50, 0x60, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x50, 0x60, 0, 0, 3, false));
        assertEquals(0x50, runtime.snapshot().slots().get(0).x());
        assertEquals(0x60, runtime.snapshot().slots().get(0).y());
        assertEquals(0, runtime.liftedEntityState().phase());
        assertEquals(0, runtime.liftedEntityState().carryState());
        assertEquals(0x01, runtime.transitionCountdown(0));
        runtime.tickWithProjectileEvents(2, 0x50, 0x60, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x50, 0x60, 0, 0, 3, false));
        assertEquals(0x60, runtime.snapshot().slots().get(0).x());
        assertEquals(0x37, runtime.liftedEntityState().carryState());
        assertEquals(1, runtime.liftedEntityState().phase());
        runtime.tick(2, 0x50, 0x60, () -> 0, null, null, 0, 3, 0);
        assertTrue(runtime.consumePendingEntityEvents().stream()
            .noneMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.WAVE
                && event.soundId() == 0x02));
    }

    @Test
    void activeSideViewPotCannotBeLiftedWithoutTheInitialCollision() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        runtime.setPowerBraceletButtonHeld(true);

        runtime.tick(0, 0x62, 0x60, () -> 0, null, null, 0, 3, 0);

        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void activeSideViewPotContactRequiresInteractiveLinkButPickupDoesNot() {
        RoomEntityRuntime airborne = sideViewPotRuntime();
        airborne.setPowerBraceletButtonHeld(true);
        airborne.tickWithProjectileEvents(0, 0x50, 0x60, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x50, 0x60, 1, 0, 0, false));
        assertEquals(EntityStatus.LIFTED, airborne.snapshot().slots().get(0).status());

        RoomEntityRuntime nonInteractive = sideViewPotRuntime();
        nonInteractive.setPowerBraceletButtonHeld(true);
        nonInteractive.tickWithProjectileEvents(0, 0x50, 0x60, () -> 0, null,
            EnemyProjectileCollision.LinkState.nonInteractive());
        assertEquals(EntityStatus.LIFTED,
            nonInteractive.snapshot().slots().get(0).status());
    }

    @Test
    void sideViewPotState0PublishesGroundedPushRequest() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        runtime.tickWithProjectileEvents(0, 0x50, 0x60, 0, () -> 0, null, null, null,
            0, 3, 0,
            new EnemyProjectileCollision.LinkState(0x50, 0x60, 0, 0, 3, false),
            false, 0, 0, 0, 0, 0, 0);

        RoomEntityRuntime.SideViewPotLinkRequest request =
            runtime.consumePendingSideViewPotLinkRequests().getFirst();
        assertTrue(request.resetPegasusBoots());
        assertFalse(request.restoreFinalPositionX());
        assertEquals(0x02, request.ignoreCollisionCountdown());
        assertEquals(0x10, request.speedX());
        assertFalse(request.snapTop());
    }

    @Test
    void sideViewPotState0PublishesAirborneFinalXRestore() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        runtime.tickWithProjectileEvents(0, 0x50, 0x60, 0, () -> 0, null, null, null,
            0, true, 3, 0,
            new EnemyProjectileCollision.LinkState(0x50, 0x60, 0, 0, 3, false),
            false, 0, 0, 0, 0, 0, 0);

        RoomEntityRuntime.SideViewPotLinkRequest request =
            runtime.consumePendingSideViewPotLinkRequests().getFirst();
        assertTrue(request.restoreFinalPositionX());
        assertTrue(request.resetPegasusBoots());
        assertEquals(0, request.speedX());
    }

    @Test
    void sideViewPotState0PublishesTopSnapAndStandingSpeed() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        runtime.tickWithProjectileEvents(0, 0x50, 0x57, 0, () -> 0, null, null, null,
            0, 3, 0,
            new EnemyProjectileCollision.LinkState(0x50, 0x57, 0, 0, 3, false),
            false, 0, 0, 0, 0, 0, 0);

        RoomEntityRuntime.SideViewPotLinkRequest request =
            runtime.consumePendingSideViewPotLinkRequests().getFirst();
        assertTrue(request.snapTop());
        assertEquals(0x50, request.positionY());
        assertEquals(0x02, request.speedY());
    }

    @Test
    void sideViewPotLiftUsesTheTopSnapYWrittenEarlierInTheHandler() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        runtime.setPowerBraceletButtonHeld(true);
        runtime.tickWithProjectileEvents(0, 0x50, 0x57, 0, () -> 0, null, null, null,
            0, 3, 0,
            new EnemyProjectileCollision.LinkState(0x50, 0x57, 0, 0, 3, false),
            false, 0, 0, 0, 0, 0, 0);

        assertEquals(EntityStatus.LIFTED, runtime.snapshot().slots().get(0).status());
        // Pickup enters the lifted state at the end of this handler frame; the
        // EntityLiftedHandler first advances on the following frame.
        assertEquals(0, runtime.liftedEntityState().phase());
        assertEquals(0x60, runtime.snapshot().slots().get(0).y());

        runtime.tickWithProjectileEvents(1, 0x50, 0x50, 0, () -> 0, null, null, null,
            0, 3, 0,
            new EnemyProjectileCollision.LinkState(0x50, 0x50, 0, 0, 3, false),
            false, 0, 0, 0, 0, 0, 0);
        assertEquals(0x60, runtime.snapshot().slots().get(0).y());
        assertEquals(0, runtime.liftedEntityState().phase());

        runtime.tickWithProjectileEvents(2, 0x50, 0x50, 0, () -> 0, null, null, null,
            0, 3, 0,
            new EnemyProjectileCollision.LinkState(0x50, 0x50, 0, 0, 3, false),
            false, 0, 0, 0, 0, 0, 0);
        assertEquals(0x50, runtime.snapshot().slots().get(0).y());
    }

    @Test
    void airbornePotLiftUsesCapturedFinalXAfterTheRestoreWrite() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        runtime.setPowerBraceletButtonHeld(true);
        runtime.setLinkFinalPositionX(0x80);
        runtime.tickWithProjectileEvents(0, 0x50, 0x60, 0, () -> 0, null, null, null,
            0, true, 3, 0,
            new EnemyProjectileCollision.LinkState(0x50, 0x60, 0, 0, 3, false),
            false, 0, 0, 0, 0, 0, 0);

        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void sideViewPotState0DoesNotContactNonInteractiveLink() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        runtime.tickWithProjectileEvents(0, 0x50, 0x60, () -> 0, null,
            EnemyProjectileCollision.LinkState.nonInteractive());

        assertTrue(runtime.consumePendingSideViewPotLinkRequests().isEmpty());
    }

    @Test
    void sideViewPotPickupUsesBSlotBraceletPriority() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        runtime.setLikeLikeLinkInventoryForTest(0x03, 0x03);
        runtime.setActionButtonsHeld(true, false);
        runtime.setPowerBraceletButtonHeld(true);
        runtime.tickWithProjectileEvents(0, 0x50, 0x60, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x50, 0x60, 0, 0, 3, false));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());

        runtime.setActionButtonsHeld(false, true);
        runtime.tickWithProjectileEvents(1, 0x50, 0x60, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x50, 0x60, 0, 0, 3, false));
        assertEquals(EntityStatus.LIFTED, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void fullyHeldSideViewPotEntersDedicatedActiveMotionOnThrow() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        assertTrue(runtime.beginLift(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        int frame = 0;
        while (runtime.liftedEntityState().carryState() != 0x01 && frame < 64) {
            runtime.tick(frame++, 0x50, 0x60, () -> 0, null, null, 0, 3, 0);
        }
        assertEquals(0x01, runtime.liftedEntityState().carryState());

        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
        assertEquals(ThrownEntityMotion.ROM_DIRECTION_RIGHT, runtime.thrownDirection(0));
        assertTrue(runtime.consumePendingEntityEvents().stream()
            .anyMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE
                && event.soundId() == 0x08));
        RoomEntity beforeMotion = runtime.snapshot().slots().get(0);

        runtime.tick(frame, 0, 0, () -> 0);
        RoomEntity moved = runtime.snapshot().slots().get(0);
        assertEquals((beforeMotion.x() + 3) & 0xFF, moved.x());
        assertEquals((beforeMotion.y() - 1) & 0xFF, moved.y());
        assertEquals(EntityStatus.ACTIVE, moved.status());
    }

    @Test
    void liftedThrowJingleSurvivesTheFollowingEntityTick() {
        RoomEntityRuntime runtime = thrownSideViewPotRuntime();
        runtime.tick(0, 0, 0, () -> 0);

        assertTrue(runtime.consumePendingEntityEvents().stream()
            .anyMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE
                && event.soundId() == 0x08));
    }

    @Test
    void liftedThrowAttackStepIsAppliedAfterTheThrowFrame() {
        RoomEntityRuntime runtime = thrownSideViewPotRuntime();
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());
        assertEquals(0, link.romAttackStepAnimationCountdown());

        runtime.tick(0, 0, 0, () -> 0);
        for (RoomEntityRuntime.LinkAttackStepRequest request
                : runtime.consumePendingLinkAttackStepRequests()) {
            link.startRomItemAttackStep();
        }

        assertEquals(0x0C, link.romAttackStepAnimationCountdown());
    }

    @Test
    void linkAttackRequestsMergeByDescendingEntitySlot() {
        assertEquals(0,
            attackCountdownAfterOrderedRequests(15, 0));
        assertEquals(0x0C,
            attackCountdownAfterOrderedRequests(0, 15));
    }

    private static int attackCountdownAfterOrderedRequests(int potSlot, int tarinSlot) {
        RoomEntityRuntime runtime = sideViewPotAndTarinRuntime(potSlot, tarinSlot);
        assertTrue(runtime.beginLift(potSlot, ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        int frame = 0;
        while (runtime.liftedEntityState().carryState() != 0x01 && frame < 64) {
            runtime.tick(frame++, 0x50, 0x60, () -> 0, null, null, 0, 3, 0);
        }
        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        runtime.setTarinRaccoonStateForTest(tarinSlot, 1, 0, 0, 0, 0, 0, false);
        runtime.tick(frame, 0, 0, () -> 0);

        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());
        List<RoomEntityRuntime.LinkAttackClearRequest> clears =
            runtime.consumePendingLinkAttackClearRequests();
        List<RoomEntityRuntime.LinkAttackStepRequest> steps =
            runtime.consumePendingLinkAttackStepRequests();
        int clearIndex = 0;
        int stepIndex = 0;
        while (clearIndex < clears.size() || stepIndex < steps.size()) {
            boolean applyClear = stepIndex >= steps.size()
                || (clearIndex < clears.size()
                    && clears.get(clearIndex).sourceSlot() > steps.get(stepIndex).sourceSlot());
            if (applyClear) {
                clearIndex++;
                link.clearRomAttackStepAnimationCountdown();
            } else {
                stepIndex++;
                link.startRomItemAttackStep();
            }
        }
        return link.romAttackStepAnimationCountdown();
    }

    private static RoomEntityRuntime sideViewPotAndTarinRuntime(int potSlot, int tarinSlot) {
        EntitySpriteDefinition potDefinition = definition(0xD6);
        EntitySpriteDefinition tarinDefinition = EntitySpriteDefinition.unsupported(0x3F);
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        slots.set(potSlot, new RoomEntity(potSlot, 0, 0xD6, 0x50, 0x60,
            EntityStatus.ACTIVE, potDefinition, 0));
        slots.set(tarinSlot, new RoomEntity(tarinSlot, 1, 0x3F, 0x20, 0x40,
            EntityStatus.ACTIVE, tarinDefinition, -1));
        return RoomEntityRuntime.from(
            new RoomEntitySnapshot(slots).withSideScrolling(true));
    }

    @Test
    void sideViewPotCollisionSpawnsRockSmashAndClearsPot() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        assertTrue(runtime.beginLift(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        int frame = 0;
        while (runtime.liftedEntityState().carryState() != 0x01 && frame < 64) {
            runtime.tick(frame++, 0x50, 0x60, () -> 0, null, null, 0, 3, 0);
        }
        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));

        runtime.tick(frame, 0, 0, () -> 0,
            (entity, direction, nextX, nextY) -> true);

        assertTrue(runtime.snapshot().slots().get(0).status() == EntityStatus.DISABLED
            || runtime.snapshot().slots().get(0).type() == 0x05);
        assertTrue(runtime.snapshot().slots().stream()
            .anyMatch(entity -> entity.loaded() && entity.type() == 0x05));
        assertTrue(runtime.consumePendingEntityEvents().stream()
            .anyMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                && event.soundId() == 0x09));
    }

    @Test
    void sideViewPotProbesNonzeroSpeedsWithoutWholePixelMovement() {
        RoomEntityRuntime runtime = thrownSideViewPotRuntime();
        runtime.setSideViewPotSpeedForTest(0, 0x01, 0x01);
        List<Integer> probeDirections = new ArrayList<>();
        runtime.setBackgroundInteraction((entity, direction, nextX, nextY) -> {
            probeDirections.add(direction);
            return EntityBackgroundCollisionResult.blocked(direction, 0x20, 0x01,
                nextX, nextY);
        });

        runtime.tick(0, 0, 0, () -> 0);

        assertEquals(List.of(EntityBackgroundCollisionResult.RIGHT,
            EntityBackgroundCollisionResult.DOWN), probeDirections);
        assertTrue(runtime.snapshot().slots().stream()
            .anyMatch(entity -> entity.loaded() && entity.type() == 0x05));
    }

    @Test
    void sideViewPotSmashUsesTheMovedEntityCoordinates() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        assertTrue(runtime.beginLift(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        int frame = 0;
        while (runtime.liftedEntityState().carryState() != 0x01 && frame < 64) {
            runtime.tick(frame++, 0x50, 0x60, () -> 0, null, null, 0, 3, 0);
        }
        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        RoomEntity beforeMotion = runtime.snapshot().slots().get(0);
        runtime.setBackgroundInteraction((entity, direction, nextX, nextY) ->
            EntityBackgroundCollisionResult.blocked(direction, 0x20, 0x01, 0x53, 0x52));
        runtime.tick(frame, 0, 0, () -> 0);

        RoomEntity smash = runtime.snapshot().slots().stream()
            .filter(entity -> entity.loaded() && entity.type() == 0x05)
            .findFirst().orElseThrow();
        // SpawnNewEntity repopulates hMultiPurpose0/1/3 from the moved source
        // entity immediately before SmashRock reads them.
        assertEquals((beforeMotion.x() + 3) & 0xFF, smash.x());
        assertEquals((beforeMotion.y() - 1) & 0xFF, smash.y());
    }

    @Test
    void sideViewPotDoesNotEnterGenericThrownDamagePass() {
        RoomEntityRuntime runtime = sideViewPotWithTargetRuntime();
        int initialHealth = runtime.enemyHealth(1);
        assertTrue(runtime.beginLift(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        int frame = 0;
        while (runtime.liftedEntityState().carryState() != 0x01 && frame < 64) {
            runtime.tick(frame++, 0x50, 0x60, () -> 0, null, null, 0, 3, 0);
        }
        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));

        runtime.tick(frame, 0, 0, () -> 0);

        assertEquals(initialHealth, runtime.enemyHealth(1));
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void sideViewPotMotionContinuesWhenLinkIsNonInteractive() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        assertTrue(runtime.beginLift(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        int frame = 0;
        while (runtime.liftedEntityState().carryState() != 0x01 && frame < 64) {
            runtime.tick(frame++, 0x50, 0x60, () -> 0, null, null, 0, 3, 0);
        }
        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        RoomEntity before = runtime.snapshot().slots().get(0);

        runtime.tickWithProjectileEvents(frame, 0, 0, () -> 0, null,
            EnemyProjectileCollision.LinkState.nonInteractive());

        assertEquals((before.x() + 3) & 0xFF,
            runtime.snapshot().slots().get(0).x());
        assertEquals((before.y() - 1) & 0xFF,
            runtime.snapshot().slots().get(0).y());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
    }

    @Test
    void sideViewPotMotionUsesTheExactReturnIfNonInteractiveGate() {
        RoomEntityRuntime dialog = thrownSideViewPotRuntime();
        dialog.setDialogActive(true);
        RoomEntity dialogBefore = dialog.snapshot().slots().get(0);
        dialog.tick(0, 0, 0, () -> 0);
        assertEquals(dialogBefore.x(), dialog.snapshot().slots().get(0).x());

        RoomEntityRuntime inventory = thrownSideViewPotRuntime();
        inventory.setTalkState(true, 0, 0x80);
        RoomEntity inventoryBefore = inventory.snapshot().slots().get(0);
        inventory.tick(0, 0, 0, () -> 0);
        assertEquals(inventoryBefore.x(), inventory.snapshot().slots().get(0).x());

        RoomEntityRuntime gotItem = thrownSideViewPotRuntime();
        gotItem.setWitchGotItemPresentationActiveForTest(true);
        RoomEntity gotItemBefore = gotItem.snapshot().slots().get(0);
        gotItem.tick(0, 0, 0, () -> 0);
        assertEquals(gotItemBefore.x(), gotItem.snapshot().slots().get(0).x());

        RoomEntityRuntime roomTransition = thrownSideViewPotRuntime();
        roomTransition.setRoomTransitionStateForTest(true);
        RoomEntity roomTransitionBefore = roomTransition.snapshot().slots().get(0);
        roomTransition.tick(0, 0, 0, () -> 0);
        assertEquals(roomTransitionBefore.x(),
            roomTransition.snapshot().slots().get(0).x());

        RoomEntityRuntime worldMap = thrownSideViewPotRuntime();
        worldMap.setGameplayWorldForTest(false);
        RoomEntity worldMapBefore = worldMap.snapshot().slots().get(0);
        worldMap.tick(0, 0, 0, () -> 0);
        assertEquals(worldMapBefore.x(), worldMap.snapshot().slots().get(0).x());

        RoomEntityRuntime ocarina = thrownSideViewPotRuntime();
        ocarina.setOcarinaPlaybackForTest(0x01, 0, 0);
        RoomEntity ocarinaBefore = ocarina.snapshot().slots().get(0);
        ocarina.tick(0, 0, 0, () -> 0);
        assertEquals((ocarinaBefore.x() + 3) & 0xFF,
            ocarina.snapshot().slots().get(0).x());

        RoomEntityRuntime transition = thrownSideViewPotRuntime();
        transition.setTransitionSequenceCounterForTest(0x03);
        RoomEntity transitionBefore = transition.snapshot().slots().get(0);
        transition.tick(0, 0, 0, () -> 0);
        assertEquals(transitionBefore.x(), transition.snapshot().slots().get(0).x());

        RoomEntityRuntime credits = thrownSideViewPotRuntime();
        credits.setTransitionSequenceCounterForTest(0x03);
        RoomEntity creditsBefore = credits.snapshot().slots().get(0);
        credits.tick(0, 0, 0, () -> 0, true);
        assertEquals((creditsBefore.x() + 3) & 0xFF,
            credits.snapshot().slots().get(0).x());
    }

    private static RoomEntityRuntime thrownSideViewPotRuntime() {
        RoomEntityRuntime runtime = sideViewPotRuntime();
        assertTrue(runtime.beginLift(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        int frame = 0;
        while (runtime.liftedEntityState().carryState() != 0x01 && frame < 64) {
            runtime.tick(frame++, 0x50, 0x60, () -> 0, null, null, 0, 3, 0);
        }
        assertTrue(runtime.throwLiftedEntity(ThrownEntityMotion.ROM_DIRECTION_RIGHT));
        return runtime;
    }

    private static RoomEntityRuntime sideViewPotRuntime() {
        EntitySpriteDefinition definition = definition(0xD6);
        List<RoomEntity> slots = new ArrayList<>();
        slots.add(new RoomEntity(0, 0, 0xD6, 0x50, 0x60,
            EntityStatus.ACTIVE, definition, 0));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return RoomEntityRuntime.from(
            new RoomEntitySnapshot(slots).withSideScrolling(true));
    }

    private static RoomEntityRuntime sideViewPotWithTargetRuntime() {
        EntitySpriteDefinition potDefinition = definition(0xD6);
        EntitySpriteDefinition targetDefinition = definition(0x18);
        List<RoomEntity> slots = new ArrayList<>();
        slots.add(new RoomEntity(0, 0, 0xD6, 0x50, 0x60,
            EntityStatus.ACTIVE, potDefinition, 0));
        slots.add(new RoomEntity(1, 1, 0x18, 0x50, 0x52,
            EntityStatus.ACTIVE, targetDefinition, 0));
        for (int slot = 2; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return RoomEntityRuntime.from(
            new RoomEntitySnapshot(slots).withSideScrolling(true));
    }

    private static RoomEntityRuntime runtimeWithEntity(EntityStatus status) {
        return runtimeWithEntity(status, 0x05);
    }

    private static RoomEntityRuntime runtimeWithEntity(EntityStatus status, int type) {
        EntitySpriteDefinition definition = definition(type);
        List<RoomEntity> slots = new ArrayList<>();
        slots.add(new RoomEntity(0, 0, type, 0x20, 0x30, status, definition, 0));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return RoomEntityRuntime.from(new RoomEntitySnapshot(slots));
    }

    private static EntitySpriteDefinition definition(int type) {
        return new EntitySpriteDefinition(
            type, 0x03, 0x5B65, EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(0x14, 0x02),
                new EntitySpriteDefinition.OamAttribute(0x14, 0x22))));
    }

    private static byte[] loadRom() {
        try (var stream = RoomEntityLiftThrowRuntimeTest.class.getClassLoader()
                .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load ROM", exception);
        }
    }
}
