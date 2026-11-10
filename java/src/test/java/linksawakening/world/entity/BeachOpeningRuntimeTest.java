package linksawakening.world.entity;

import linksawakening.world.room.OwlEventDialogResolver;
import linksawakening.world.room.ChestContentsTable;



import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.entity.RoomEntitySnapshot;

import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.OwlEventCompletion;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@linksawakening.testing.RequiresRom
final class BeachOpeningRuntimeTest {
    private static final int BEACH_ROOM = 0xF2;
    private static final int ENTITY_SWORD_SHIELD_PICKUP = 0x31;
    private static final int ENTITY_SLEEPY_TOADSTOOL = 0x3A;
    private static final int ENTITY_OWL_EVENT = 0x41;

    @Test
    void beachRoomLoadsSwordAndOwlUntilTheirRoomEventsAreSet() throws IOException {
        byte[] rom = loadRom();
        EntityRoomLoader loader = new EntityRoomLoader(rom);
        byte[] statuses = new byte[0x100];

        RoomEntitySnapshot fresh = loader.load(
            EntityRoomLoader.RoomTable.OVERWORLD, BEACH_ROOM, 0, -1, statuses);
        assertTrue(hasType(fresh, ENTITY_SWORD_SHIELD_PICKUP));
        assertTrue(hasType(fresh, ENTITY_OWL_EVENT));

        statuses[BEACH_ROOM] = 0x10;
        RoomEntitySnapshot swordCollected = loader.load(
            EntityRoomLoader.RoomTable.OVERWORLD, BEACH_ROOM, 0, -1, statuses);
        assertFalse(hasType(swordCollected, ENTITY_SWORD_SHIELD_PICKUP));
        assertTrue(hasType(swordCollected, ENTITY_OWL_EVENT));

        statuses[BEACH_ROOM] = 0x30;
        RoomEntitySnapshot openingComplete = loader.load(
            EntityRoomLoader.RoomTable.OVERWORLD, BEACH_ROOM, 0, -1, statuses);
        assertFalse(hasType(openingComplete, ENTITY_SWORD_SHIELD_PICKUP));
        assertFalse(hasType(openingComplete, ENTITY_OWL_EVENT));
    }

    @Test
    void swordPickupUsesTheRomFanfareSequenceBeforeGrantingTheSword() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 1, ENTITY_SWORD_SHIELD_PICKUP, 0x58, 0x60, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_SWORD_SHIELD_PICKUP,
                EntityRoomLoader.RoomTable.OVERWORLD), 0, 0, 0, 0, -1, false)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));
        runtime.configuration().setEntityRoomId(BEACH_ROOM);
        runtime.configuration().setEntityRoomStatus(0);
        runtime.configuration().setChestPlayerLevels(0, 0, 0);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x20, () -> 0));
        RoomEntity sword = runtime.inspection().snapshot().slots().get(0);
        assertEquals(0x84, sword.spriteDefinition().variant(0).first().tile());

        EntityPickupEvent pickup = runtime.collectIfNeeded(
            1, sword.x(), sword.y(), false, true, 3, 0);
        assertNotNull(pickup);
        assertEquals(0, pickup.persistentClearMask());
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x58, 0x60, () -> 0));
        assertEquals(0x0F, runtime.consumePendingMusicTrack());
        assertTrue(runtime.consumePendingSwordPickupRewards().isEmpty());

        boolean foundSwordDialog = false;
        boolean foundOverworldIntro = false;
        boolean foundNormalOverworld = false;
        boolean foundSpinNoise = false;
        boolean foundSpinPose = false;
        boolean foundFinalPose = false;
        boolean foundSpinFrameWithoutMotionBlock = false;
        List<SwordPickupRewardEvent> rewards = List.of();
        for (int frame = 3; frame < 0x240 && rewards.isEmpty(); frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x58, 0x60, () -> 0));
            foundSwordDialog |= runtime.consumePendingDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x09B);
            int musicTrack = runtime.consumePendingMusicTrack();
            foundOverworldIntro |= musicTrack == 0x31;
            foundNormalOverworld |= musicTrack == 0x05;
            foundSpinNoise |= runtime.consumePendingEntityEvents().stream()
                .anyMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                    && event.soundId() == 0x03);
            List<LinkSwordSpinPoseRequest> spinRequests =
                runtime.consumePendingLinkSwordSpinPoseRequests();
            List<LinkMotionBlockRequest> motionBlockRequests =
                runtime.consumePendingLinkMotionBlockRequests();
            foundSpinPose |= !spinRequests.isEmpty();
            if (spinRequests.stream().anyMatch(request -> request.countdown() != 0x20)) {
                foundSpinFrameWithoutMotionBlock |= motionBlockRequests.isEmpty();
            }
            foundFinalPose |= !runtime.consumePendingLinkSwordFinalPoseRequests().isEmpty();
            rewards = runtime.consumePendingSwordPickupRewards();
        }

        assertTrue(foundSwordDialog);
        assertTrue(foundOverworldIntro);
        // $03:5BD3-5BD7 only sets hDefaultMusicTrack to MUSIC_OVERWORLD; it
        // never requests it through wMusicTrackToPlay.
        assertFalse(foundNormalOverworld);
        assertTrue(foundSpinNoise, "rewards=" + rewards + ", status="
            + runtime.inspection().snapshot().slots().get(0).status() + ", state="
            + runtime.inspection().swordPickupStateForTest(0) + ", slow="
            + runtime.inspection().slowTransitionCountdownForTest(0));
        assertTrue(foundSpinPose);
        assertTrue(foundSpinFrameWithoutMotionBlock);
        assertTrue(foundFinalPose);
        assertEquals(List.of(new SwordPickupRewardEvent(0)), rewards);
        assertEquals(EntityStatus.DISABLED, runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void sleepyToadstoolUsesTheRomHeldItemSequenceBeforeGrantingItsReward()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_SLEEPY_TOADSTOOL, 0x58, 0x60, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_SLEEPY_TOADSTOOL,
                EntityRoomLoader.RoomTable.OVERWORLD), 0, 0, 0, 0, -1, false)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));
        runtime.configuration().setEntityRoomId(0x50);
        runtime.configuration().setToadstoolPlayerState(false, 0);
        runtime.configuration().setOwlDefaultMusicResolver(roomId -> 0x09);

        EntityPickupEvent pickup = runtime.collectIfNeeded(
            1, 0x58, 0x60, false, true, 3, 0);
        assertNotNull(pickup);
        assertEquals(0x01, pickup.persistentClearMask());
        assertEquals(0x10, runtime.consumePendingMusicTrack());

        boolean heldAboveLink = false;
        boolean linkBlocked = false;
        boolean dialogOpened = false;
        List<ToadstoolRewardEvent> rewards = List.of();
        for (int frame = 2; frame < 0x100 && rewards.isEmpty(); frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x48, 0x50, () -> 0));
            heldAboveLink |= !runtime.consumePendingLinkHeldItemPoseRequests().isEmpty();
            linkBlocked |= !runtime.consumePendingLinkMotionBlockRequests().isEmpty();
            boolean opened = runtime.consumePendingDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x00F);
            dialogOpened |= opened;
            if (opened) {
                runtime.configuration().setDialogActive(true);
                for (int wait = 0; wait < 360; wait++) {
                    runtime.tick(EntityTestFrames.frame(runtime, frame + wait, 0x48, 0x50, () -> 0));
                    assertTrue(runtime.consumePendingToadstoolRewards().isEmpty(),
                        "Reading the dialogue must not grant or hide the held mushroom");
                    assertEquals(EntityStatus.ACTIVE, runtime.inspection().snapshot().slots().get(0).status());
                }
                runtime.configuration().setDialogActive(false);
            }
            rewards = runtime.consumePendingToadstoolRewards();
        }

        assertTrue(heldAboveLink);
        assertTrue(linkBlocked);
        assertTrue(dialogOpened);
        assertEquals(List.of(new ToadstoolRewardEvent(0)), rewards);
        assertEquals(0x09, runtime.consumePendingMusicTrack());
        assertEquals(EntityStatus.DISABLED, runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void sleepyToadstoolUnloadsWhenAlreadyOwnedOrPowderIsPresent() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);

        RoomEntityRuntime owned = toadstoolRuntime(catalog, rom);
        owned.configuration().setToadstoolPlayerState(true, 0);
        owned.tick(EntityTestFrames.frame(owned, 0, 0, 0, () -> 0));
        assertEquals(EntityStatus.DISABLED, owned.inspection().snapshot().slots().get(0).status());

        RoomEntityRuntime powdered = toadstoolRuntime(catalog, rom);
        powdered.configuration().setToadstoolPlayerState(false, 1);
        powdered.tick(EntityTestFrames.frame(powdered, 0, 0, 0, () -> 0));
        assertEquals(EntityStatus.DISABLED, powdered.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void beachOwlTriggersInTheRomRectangleAndCompletesItsDialogEvent() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_OWL_EVENT, 0x08, 0x10, EntityStatus.ACTIVE,
            catalog.forOwlEvent(false), 0, 0, 0, 0, -1, false)), false, () -> 0, catalog,
            new RomEnemyCombatTables(rom));
        runtime.configuration().setEntityRoomId(BEACH_ROOM);
        runtime.configuration().setEntityRoomStatus(0);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x3F, 0x44, () -> 0));
        assertEquals(0, runtime.inspection().owlEventStateForTest(0));
        assertEquals(-1, runtime.consumePendingMusicTrack());

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x40, 0x44, () -> 0));
        assertEquals(1, runtime.inspection().owlEventStateForTest(0));
        assertEquals(0x22, runtime.consumePendingMusicTrack());

        boolean dialogOpened = false;
        boolean linkBlocked = false;
        boolean fallenPoseRequested = false;
        int frame = 2;
        while (!dialogOpened && frame < 0x300) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 0x58, 0x60, () -> 0));
            dialogOpened |= runtime.consumePendingDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x0D9);
            linkBlocked |= !runtime.consumePendingLinkMotionBlockRequests().isEmpty();
            fallenPoseRequested |= !runtime.consumePendingLinkFallenPoseRequests().isEmpty();
            assertTrue(runtime.consumePendingOwlEventCompletions().isEmpty());
        }

        assertTrue(dialogOpened);
        assertTrue(linkBlocked);
        assertFalse(fallenPoseRequested);
        for (int countdown = 0x0F; countdown > 0; countdown--) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 0x58, 0x60, () -> 0));
            assertTrue(runtime.consumePendingOwlEventCompletions().isEmpty());
        }
        runtime.tick(EntityTestFrames.frame(runtime, frame, 0x58, 0x60, () -> 0));
        assertEquals(List.of(new OwlEventCompletion(0)),
            runtime.consumePendingOwlEventCompletions());
    }

    @Test
    void landedOwlReleasesLinkForTheFiveSourceInteractionCountdownFrames() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_OWL_EVENT, 0x08, 0x10, EntityStatus.ACTIVE,
            catalog.forOwlEvent(false), 0, 0, 0, 0, -1, false)), false, () -> 0, catalog,
            new RomEnemyCombatTables(rom));
        runtime.configuration().setEntityRoomId(BEACH_ROOM);
        int frame = 0;
        runtime.tick(EntityTestFrames.frame(runtime, frame++, 0x58, 0x44, () -> 0));
        while (runtime.inspection().owlEventStateForTest(0) != 2 && frame < 200) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 0x58, 0x45, () -> 0));
            runtime.consumePendingLinkMotionBlockRequests();
        }
        assertEquals(2, runtime.inspection().owlEventStateForTest(0));
        // Captured ROM: C111 counts 5..1 while C1A8 retains the pre-decrement
        // value. No motion-block write occurs in these state-2 frames.
        for (int delay = 0; delay < 5; delay++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 0x58, 0x45 + delay, () -> 0));
            assertTrue(runtime.consumePendingLinkMotionBlockRequests().isEmpty());
            assertTrue(runtime.consumePendingDialogRequests().isEmpty());
        }
        runtime.tick(EntityTestFrames.frame(runtime, frame, 0x58, 0x4A, () -> 0));
        assertEquals(1, runtime.consumePendingDialogRequests().size());
    }

    @Test
    void forestOwlRequiresTheSwordAndStartsItsFlightIntoTheRoom() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime swordless = owlRuntime(catalog, rom);
        swordless.configuration().setEntityRoomId(0x80);
        swordless.configuration().setChestPlayerLevels(0, 0, 0);

        swordless.tick(EntityTestFrames.frame(swordless, 0, 0x50, 0x50, () -> 0));

        assertEquals(EntityStatus.DISABLED, swordless.inspection().snapshot().slots().get(0).status());
        assertEquals(-1, swordless.consumePendingMusicTrack());

        RoomEntityRuntime armed = owlRuntime(catalog, rom);
        armed.configuration().setEntityRoomId(0x80);
        armed.configuration().setChestPlayerLevels(0, 1, 0);
        armed.configuration().setOwlDialogResolver(new OwlEventDialogResolver(rom)::globalDialogId);

        armed.tick(EntityTestFrames.frame(armed, 0, 0x50, 0x50, () -> 0));
        assertEquals(1, armed.inspection().owlEventStateForTest(0));
        assertEquals(0x22, armed.consumePendingMusicTrack());
        armed.tick(EntityTestFrames.frame(armed, 1, 0x50, 0x50, () -> 0));
        assertEquals(List.of(new LinkFacingRequest(0, 0)),
            armed.consumePendingLinkFacingRequests());
        assertTrue(armed.consumePendingEntityEvents().stream().anyMatch(event ->
            event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                && event.soundId() == 0x2D));
        assertEquals(EntitySpriteDefinition.Shape.PAIR,
            armed.inspection().snapshot().slots().get(0).spriteDefinition().shape());
        armed.tick(EntityTestFrames.frame(armed, 2, 0x50, 0x50, () -> 0));
        assertFalse(armed.consumePendingEntityEvents().stream().anyMatch(event ->
            event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                && event.soundId() == 0x2D));

        armed.tick(EntityTestFrames.frame(armed, 8, 0x50, 0x50, () -> 0));
        assertEquals(EntitySpriteDefinition.Shape.RECTANGLE,
            armed.inspection().snapshot().slots().get(0).spriteDefinition().shape());
    }

    @Test
    void swampOwlRequiresTailCaveInstrumentAndBowWowBeforeBottleGrotto()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime missingTailInstrument = owlRuntime(catalog, rom);
        missingTailInstrument.configuration().setEntityRoomId(0x36);
        missingTailInstrument.configuration().setChestPlayerLevels(0, 1, 0);
        missingTailInstrument.configuration().setBowWowState(0x01);
        missingTailInstrument.configuration().setOwlInstrumentFlags(0x00, 0x00);

        missingTailInstrument.tick(EntityTestFrames.frame(missingTailInstrument, 0, 0x50, 0x50, () -> 0));

        assertEquals(EntityStatus.DISABLED,
            missingTailInstrument.inspection().snapshot().slots().get(0).status());

        RoomEntityRuntime ordered = owlRuntime(catalog, rom);
        ordered.configuration().setEntityRoomId(0x36);
        ordered.configuration().setChestPlayerLevels(0, 1, 0);
        ordered.configuration().setBowWowState(0x01);
        ordered.configuration().setOwlInstrumentFlags(0x02, 0x00);
        ordered.tick(EntityTestFrames.frame(ordered, 0, 0x50, 0x50, () -> 0));

        assertEquals(EntityStatus.ACTIVE, ordered.inspection().snapshot().slots().get(0).status());
        assertEquals(2, ordered.inspection().owlEventStateForTest(0));
        assertEquals(0x22, ordered.consumePendingMusicTrack());
    }

    @Test
    void tailKeyOwlWaitsForOwnershipAndTheChestInitializedPrivateCountdown()
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity owl = new RoomEntity(
            0, 0, ENTITY_OWL_EVENT, 0x58, 0x50, EntityStatus.ACTIVE,
            catalog.forOwlEvent(false), 0, 0, 0, 0, -1, false);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(owl), false, () -> 0,
            catalog, new RomEnemyCombatTables(rom), new ChestContentsTable(rom));
        runtime.configuration().setEntityRoomId(0x41);
        runtime.configuration().setOwlDialogResolver(new OwlEventDialogResolver(rom)::globalDialogId);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, () -> 0));
        assertEquals(0, runtime.inspection().owlEventStateForTest(0));

        runtime.configuration().setTailKeyOwned(true);
        int chestSlot = runtime.spawnChestWithItem(
            0x38, 0x31, ChestContentsTable.CHEST_TAIL_KEY);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x50, 0x50, () -> 0));
        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, chestSlot);
        assertEquals(0x37, runtime.inspection().owlPrivateCountdown1ForTest(0));
        assertEquals(0, runtime.inspection().owlEventStateForTest(0));

        runtime.configuration().setDialogActive(true);
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0x50, 0x50, () -> 0));
        assertEquals(0x37, runtime.inspection().owlPrivateCountdown1ForTest(0));
        runtime.configuration().setDialogActive(false);

        runtime.configuration().setTalkState(true, 0, 0x80);
        runtime.tick(EntityTestFrames.frame(runtime, 3, 0x50, 0x50, () -> 0));
        assertEquals(0x37, runtime.inspection().owlPrivateCountdown1ForTest(0));
        runtime.configuration().setTalkState(false, 0, 0x80);

        runtime.debug().setOcarinaPlaybackForTest(1, 0, 0);
        runtime.tick(EntityTestFrames.frame(runtime, 4, 0x50, 0x50, () -> 0));
        assertEquals(0x37, runtime.inspection().owlPrivateCountdown1ForTest(0));
        runtime.debug().setOcarinaPlaybackForTest(0, 0, 0);

        for (int frame = 5; frame < 0x3B; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x50, () -> 0));
        }
        assertEquals(0, runtime.inspection().owlEventStateForTest(0));

        runtime.tick(EntityTestFrames.frame(runtime, 0x3B, 0x50, 0x50, () -> 0));
        assertEquals(1, runtime.inspection().owlEventStateForTest(0));
        assertEquals(0x22, runtime.consumePendingMusicTrack());

        boolean dialogOpened = false;
        for (int frame = 0x3C; frame < 0x300 && !dialogOpened; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x50, 0x50, () -> 0));
            dialogOpened = runtime.consumePendingDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x0C1);
        }
        assertTrue(dialogOpened);
    }

    private static RoomEntityRuntime owlRuntime(EntitySpriteHandlerCatalog catalog, byte[] rom) {
        return RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_OWL_EVENT, 0x58, 0x50, EntityStatus.ACTIVE,
            catalog.forOwlEvent(false), 0, 0, 0, 0, -1, false)), false, () -> 0, catalog,
            new RomEnemyCombatTables(rom));
    }

    private static RoomEntityRuntime toadstoolRuntime(EntitySpriteHandlerCatalog catalog,
                                                       byte[] rom) {
        return RoomEntityRuntime.from(snapshot(new RoomEntity(
            0, 0, ENTITY_SLEEPY_TOADSTOOL, 0x58, 0x60, EntityStatus.ACTIVE,
            catalog.forEntityType(ENTITY_SLEEPY_TOADSTOOL,
                EntityRoomLoader.RoomTable.OVERWORLD), 0, 0, 0, 0, -1, false)),
            false, () -> 0, catalog, new RomEnemyCombatTables(rom));
    }

    private static boolean hasType(RoomEntitySnapshot snapshot, int type) {
        return snapshot.loadedEntities().stream().anyMatch(entity -> entity.type() == type);
    }

    private static RoomEntitySnapshot snapshot(RoomEntity entity) {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        slots.set(entity.slot(), entity);
        return new RoomEntitySnapshot(slots);
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
