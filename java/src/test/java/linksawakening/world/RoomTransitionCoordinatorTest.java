package linksawakening.world;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.BeachSwordRewardConsumer;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.startup.NewGameStartProfile;
import linksawakening.startup.TarinShieldMotion;
import linksawakening.state.PlayerState;
import linksawakening.vfx.TransientVfxSystem;
import org.junit.jupiter.api.Test;

import static linksawakening.world.RoomConstants.ROOM_OBJECTS_BASE;
import static linksawakening.world.RoomConstants.ROOM_OBJECT_ROW_STRIDE;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomTransitionCoordinatorTest {
    private static final int MAP_WITH_NO_WARP_ROOM = 0x01;
    private static final int INDOOR_ROOM_WITH_NO_WARPS = 0x00;
    private static final int OBJECT_NORMAL_PIT = 0xE8;

    @Test
    void indoorBoundaryScrollWithoutWarpsUpdatesRoomEntryPosition() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadInitialOverworld(0x92);
        session.loadIndoor(MAP_WITH_NO_WARP_ROOM, INDOOR_ROOM_WITH_NO_WARPS);
        assertFalse(session.activeRoom().hasWarps());

        ScrollController scrollController = new ScrollController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
            session, new RoomBoundaryController(), new TransitionController(), scrollController);
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            romTables, collision, null, new PlayerState(), new ItemRegistry());

        link.setRoomEntryPixelPosition(0x30, 0x30);
        link.setPixelPosition(ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1, 0x28);

        coordinator.handleWarpAndIndoorBoundaries(link);

        assertTrue(scrollController.isActive());
        assertEquals(ScrollController.RIGHT, scrollController.direction());
        assertEquals(0x01, session.currentRoomId());
        assertEquals(0x00, link.pixelX());
        assertEquals(0x28, link.pixelY());

        collision.setRoom(roomWithPitAt(0, 3));
        collision.setGbcOverlay(null);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_OVERWORLD);
        runUntilPitFallStarts(link);
        runUntilPitFallFinishes(link);

        assertEquals(0x00, link.pixelX());
        assertEquals(0x28, link.pixelY());
    }

    @Test
    void newGameHouseSouthDoorReturnsLinkToTheRomOverworldDestination() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadIndoor(0x10, 0xA3);
        assertTrue(session.activeRoom().indoorHasSouthEntrance());
        assertEquals(Warp.CATEGORY_OVERWORLD, session.activeRoom().firstWarp().category());

        TransitionController transition = new TransitionController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
            session, new RoomBoundaryController(), transition, new ScrollController());
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            romTables, collision, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x48, RoomConstants.ROOM_PIXEL_HEIGHT);

        coordinator.handleWarpAndIndoorBoundaries(link);
        for (int frame = 0; frame < 16; frame++) transition.tick();

        assertEquals(Warp.CATEGORY_OVERWORLD, session.mapCategory());
        assertEquals(0xA2, session.currentRoomId());
        assertEquals(0x50, link.pixelX());
        assertEquals(0x42, link.pixelY());
    }

    @Test
    void tailCaveSideScrollTopEdgeReturnsThroughRomWarpZero() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadIndoor(0x00, 0x19, Warp.CATEGORY_SIDESCROLL);
        assertEquals(Warp.CATEGORY_INDOOR, session.activeRoom().firstWarp().category());
        assertEquals(0x03, session.activeRoom().firstWarp().destRoom());

        TransitionController transition = new TransitionController();
        ScrollController scroll = new ScrollController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
            session, new RoomBoundaryController(), transition, scroll);
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            romTables, collision, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x70, -1);

        coordinator.handleWarpAndIndoorBoundaries(link);

        assertTrue(transition.isActive());
        assertFalse(scroll.isActive());
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
        assertEquals(0x03, session.currentRoomId());
        assertEquals(0x80, link.pixelX());
        assertEquals(0x10, link.pixelY());
    }

    @Test
    void freshGameRuntimeSequencePersistsOpeningProgressThroughTailCaveEntry()
            throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        PlayerState playerState = new PlayerState();
        NewGameStartProfile profile = NewGameStartProfile.romDefaults();
        profile.initializePlayerState(playerState);
        session.initializeNewGameWorldState();
        session.loadIndoor(profile.mapId(), profile.roomId());

        assertTrue(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x3E));
        RoomEntity tarin = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x3F).findFirst().orElseThrow();

        TarinShieldMotion shieldMotion = new TarinShieldMotion();
        assertEquals(0x54,
            shieldMotion.tick(false, 0x60, true, playerState.shieldLevel()).dialogLowId());
        shieldMotion.tick(true, 0x60, false, playerState.shieldLevel());
        shieldMotion.tick(false, 0x60, false, playerState.shieldLevel());
        TarinShieldMotion.Update shieldReward = null;
        for (int frame = 0; frame < 0x80; frame++) {
            shieldReward = shieldMotion.tick(false, 0x60, false, playerState.shieldLevel());
        }
        assertTrue(shieldReward.grantShield());
        playerState.applyChestReward(ChestContentsTable.CHEST_SHIELD);
        assertEquals(1, playerState.shieldLevel());
        assertEquals(0, playerState.swordLevel());
        assertTrue(tarin.spriteDefinition().supported());

        ScrollController scroll = new ScrollController();
        TransitionController transition = new TransitionController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
            session, new RoomBoundaryController(), transition, scroll);
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            romTables, collision, null, playerState, new ItemRegistry());
        link.setPixelPosition(0x48, RoomConstants.ROOM_PIXEL_HEIGHT);
        coordinator.handleWarpAndIndoorBoundaries(link);
        while (transition.isActive()) transition.tick();
        assertEquals(0xA2, session.currentRoomId());

        for (int expectedRoom = 0xB2; expectedRoom <= 0xF2; expectedRoom += 0x10) {
            link.setPixelPosition(link.pixelX(), RoomConstants.ROOM_PIXEL_HEIGHT);
            coordinator.handleOverworldBoundary(link);
            assertEquals(expectedRoom, session.currentRoomId());
            while (scroll.isActive()) {
                scroll.tick(8);
            }
        }

        RoomEntity sword = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x31).findFirst().orElseThrow();
        RoomEntity owl = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x41).findFirst().orElseThrow();
        assertEquals(0x58, sword.x());
        assertEquals(0x60, sword.y());
        assertTrue(sword.spriteDefinition().supported());
        assertTrue(owl.spriteDefinition().supported());

        session.setChestPlayerLevels(
            playerState.shieldLevel(), playerState.swordLevel(),
            playerState.powerBraceletLevel());
        session.tickEntities(0, 0x3F, 0x44);
        session.tickEntities(1, 0x40, 0x44);
        assertEquals(0x22, session.consumePendingMusicTrack());

        int frame = 2;
        boolean owlDialogOpened = false;
        while ((session.overworldRoomStatusForTest(0xF2) & 0x20) == 0
            && frame < 0x300) {
            session.tickEntities(frame++, 0x58, 0x60);
            owlDialogOpened |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x0D9);
        }
        assertTrue(owlDialogOpened);

        while (session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x41) && frame < 0x400) {
            session.tickEntities(frame++, 0x58, 0x60);
        }
        int pickupFrame = (frame & 0xFE) | ((sword.slot() ^ 1) & 1);
        EntityPickupEvent pickup = session.collectEntityIfNeeded(
            pickupFrame, sword.x(), sword.y(), false, true, 3, 0);
        assertNotNull(pickup);

        frame = pickupFrame + 1;
        while (playerState.swordLevel() == 0 && frame < pickupFrame + 0x300) {
            session.tickEntities(frame++, sword.x(), sword.y());
            session.consumeEntityDialogRequests();
            BeachSwordRewardConsumer.consume(session, playerState);
        }
        assertEquals(1, playerState.swordLevel());
        assertEquals(0x30, session.overworldRoomStatusForTest(0xF2) & 0x30);

        session.loadInitialOverworld(0xF2);
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x31 || entity.type() == 0x41));

        session.setChestPlayerLevels(
            playerState.shieldLevel(), playerState.swordLevel(),
            playerState.powerBraceletLevel());
        link.setPixelPosition(0x50, 0x50);
        int[] routeToForest = {
            ScrollController.UP, ScrollController.LEFT, ScrollController.LEFT,
            ScrollController.UP, ScrollController.RIGHT, ScrollController.RIGHT,
            ScrollController.RIGHT, ScrollController.UP, ScrollController.DOWN,
            ScrollController.RIGHT, ScrollController.RIGHT, ScrollController.UP,
            ScrollController.LEFT, ScrollController.UP, ScrollController.LEFT,
            ScrollController.UP, ScrollController.LEFT, ScrollController.LEFT,
            ScrollController.LEFT, ScrollController.DOWN, ScrollController.UP,
            ScrollController.UP, ScrollController.UP
        };
        int[] expectedRooms = {
            0xE2, 0xE1, 0xE0, 0xD0, 0xD1, 0xD2, 0xD3, 0xC3,
            0xD3, 0xD4, 0xD5, 0xC5, 0xC4, 0xB4, 0xB3, 0xA3,
            0xA2, 0xA1, 0xA0, 0xB0, 0xA0, 0x90, 0x80
        };
        for (int index = 0; index < routeToForest.length; index++) {
            walkToAndCrossOverworldBoundary(
                coordinator, scroll, collision, link, routeToForest[index]);
            assertEquals(expectedRooms[index], session.currentRoomId());
        }
        assertEquals(0x80, session.currentRoomId());
        assertTrue(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x41));

        session.tickEntities(frame++, 0x50, 0x50);
        session.tickEntities(frame++, 0x50, 0x50);
        assertEquals(0x22, session.consumePendingMusicTrack());
        boolean forestOwlDialogOpened = false;
        while (!forestOwlDialogOpened && frame < pickupFrame + 0x600) {
            session.tickEntities(frame++, 0x50, 0x50);
            forestOwlDialogOpened |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x0C0);
        }
        assertTrue(forestOwlDialogOpened);
        while ((session.overworldRoomStatusForTest(0x80) & 0x20) == 0
            && frame < pickupFrame + 0x700) {
            session.tickEntities(frame++, 0x50, 0x50);
        }
        assertEquals(0x20, session.overworldRoomStatusForTest(0x80) & 0x20);

        int[] routeToToadstool = {
            ScrollController.UP, ScrollController.UP, ScrollController.RIGHT,
            ScrollController.RIGHT, ScrollController.UP, ScrollController.UP,
            ScrollController.LEFT, ScrollController.LEFT, ScrollController.DOWN
        };
        int[] toadstoolRouteRooms = {
            0x70, 0x60, 0x61, 0x62, 0x52, 0x42, 0x41, 0x40, 0x50
        };
        for (int index = 0; index < routeToToadstool.length; index++) {
            walkToAndCrossOverworldBoundary(
                coordinator, scroll, collision, link, routeToToadstool[index]);
            assertEquals(toadstoolRouteRooms[index], session.currentRoomId());
        }
        assertEquals(0x50, session.currentRoomId());
        RoomEntity toadstool = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x3A).findFirst().orElseThrow();
        session.setToadstoolPlayerState(false, 0);
        session.tickEntities(frame++, toadstool.x(), toadstool.y());
        toadstool = session.activeRoom().entities().slots().get(toadstool.slot());
        EntityPickupEvent toadstoolPickup = session.collectEntityIfNeeded(
            (frame & 0xFE) | ((toadstool.slot() ^ 1) & 1),
            toadstool.x(), toadstool.y(), false, true, Link.DIRECTION_RIGHT, 0);
        assertNotNull(toadstoolPickup);
        List<RoomEntityRuntime.ToadstoolRewardEvent> toadstoolRewards = List.of();
        while (toadstoolRewards.isEmpty() && frame < pickupFrame + 0x900) {
            session.tickEntities(frame++, toadstool.x(), toadstool.y());
            session.consumeEntityDialogRequests();
            toadstoolRewards = session.consumeToadstoolRewards();
        }
        assertEquals(List.of(new RoomEntityRuntime.ToadstoolRewardEvent(toadstool.slot())),
            toadstoolRewards);
        // Mirrors Main's live event consumer; RoomSession owns the entity
        // cadence and PlayerState owns the resulting inventory mutation.
        playerState.applyToadstoolReward();
        assertTrue(playerState.hasToadstool());
        assertEquals(PlayerState.INVENTORY_MAGIC_POWDER, playerState.subscreenItem(0));

        int[] routeToWitchHut = {
            ScrollController.UP, ScrollController.UP, ScrollController.DOWN,
            ScrollController.RIGHT, ScrollController.RIGHT, ScrollController.RIGHT,
            ScrollController.RIGHT, ScrollController.DOWN, ScrollController.DOWN,
            ScrollController.RIGHT
        };
        int[] witchRouteRooms = {
            0x40, 0x30, 0x40, 0x41, 0x42, 0x43, 0x44, 0x54, 0x64, 0x65
        };
        for (int index = 0; index < routeToWitchHut.length; index++) {
            walkToAndCrossOverworldBoundary(
                coordinator, scroll, collision, link, routeToWitchHut[index]);
            assertEquals(witchRouteRooms[index], session.currentRoomId());
        }
        assertEquals(0x65, session.currentRoomId());
        Warp witchHut = session.activeRoom().warps().stream()
            .filter(warp -> warp.destMap() == 0x0E && warp.destRoom() == 0xA2)
            .findFirst().orElseThrow();
        assertEquals(0x24, witchHut.tileLocation());
        coordinator = new RoomTransitionCoordinator(
            session, new RoomBoundaryController(), transition, scroll);
        link.setPixelPosition(
            (witchHut.tileLocation() & 0x0F) * 16,
            (witchHut.tileLocation() >>> 4) * 16);
        coordinator.handleWarpAndIndoorBoundaries(link);
        while (transition.isActive()) transition.tick();
        assertEquals(0x0E, session.activeRoom().mapId());
        assertEquals(0xA2, session.currentRoomId());

        RoomEntity witch = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x40).findFirst().orElseThrow();
        playerState.swapItemAWithSubscreen(0);
        assertEquals(PlayerState.INVENTORY_MAGIC_POWDER, playerState.itemA());
        session.setToadstoolPlayerState(true, 0);
        session.setEntityInventorySlots(playerState.itemA(), playerState.itemB());
        session.setEntityActionButtonsHeld(true, false);
        session.tickEntitiesWithProjectileEvents(
            frame++, witch.x(), 0x56, 0, 0, 1, false);
        session.tickEntitiesWithProjectileEvents(
            frame++, witch.x(), 0x56, 0, 0, 1, false);
        List<RoomEntityRuntime.WitchExchangeEvent> exchangeEvents =
            session.consumeWitchExchangeEvents();
        assertEquals(1, exchangeEvents.size());
        playerState.beginWitchToadstoolExchange(exchangeEvents.getFirst().inventorySlot());
        assertFalse(playerState.hasToadstool());
        assertEquals(PlayerState.INVENTORY_EMPTY, playerState.itemA());

        session.setEntityActionButtonsHeld(false, false);
        boolean brewingDialog = false;
        while (!brewingDialog && frame < pickupFrame + 0xA00) {
            session.tickEntitiesWithProjectileEvents(
                frame++, witch.x(), 0x56, 0, 0, 1, false);
            brewingDialog |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x009);
        }
        assertTrue(brewingDialog);
        session.setEntityDialogActive(true);
        session.tickEntitiesWithProjectileEvents(
            frame++, witch.x(), 0x56, 0, 0, 1, false);
        session.setEntityDialogActive(false);
        boolean readyDialog = false;
        while (!readyDialog && frame < pickupFrame + 0xB00) {
            session.tickEntitiesWithProjectileEvents(
                frame++, witch.x(), 0x56, 0, 0, 1, false);
            readyDialog |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x0FE);
        }
        assertTrue(readyDialog);
        session.setEntityDialogActive(true);
        session.tickEntitiesWithProjectileEvents(
            frame++, witch.x(), 0x56, 0, 0, 1, false);
        session.setEntityDialogActive(false);
        session.tickEntitiesWithProjectileEvents(
            frame++, witch.x(), 0x56, 0, 0, 1, false);
        assertEquals(1, session.consumeWitchRewardEvents().size());
        playerState.applyWitchMagicPowderReward();
        assertEquals(PlayerState.INVENTORY_MAGIC_POWDER, playerState.itemA());
        assertEquals(20, playerState.magicPowderCount());

        link.setPixelPosition(link.pixelX(), RoomConstants.ROOM_PIXEL_HEIGHT);
        coordinator.handleWarpAndIndoorBoundaries(link);
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_OVERWORLD, session.mapCategory());
        assertEquals(0x65, session.currentRoomId());

        int[] routeToRaccoon = {
            ScrollController.RIGHT, ScrollController.DOWN, ScrollController.RIGHT,
            ScrollController.UP, ScrollController.UP, ScrollController.LEFT,
            ScrollController.LEFT, ScrollController.LEFT, ScrollController.DOWN,
            ScrollController.UP, ScrollController.UP, ScrollController.LEFT,
            ScrollController.LEFT, ScrollController.LEFT, ScrollController.DOWN
        };
        int[] raccoonRouteRooms = {
            0x66, 0x76, 0x77, 0x67, 0x57, 0x56, 0x55, 0x54,
            0x64, 0x54, 0x44, 0x43, 0x42, 0x41, 0x51
        };
        for (int index = 0; index < routeToRaccoon.length; index++) {
            walkToAndCrossOverworldBoundary(
                coordinator, scroll, collision, link, routeToRaccoon[index]);
            assertEquals(raccoonRouteRooms[index], session.currentRoomId());
        }
        assertEquals(0x51, session.currentRoomId());
        assertTrue(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x3F));

        session.setEntityInventorySlots(playerState.itemA(), playerState.itemB());
        session.setToadstoolPlayerState(false, playerState.magicPowderCount());
        session.tickEntitiesWithProjectileEvents(
            frame++, 0x6A, 0x40, 0, 0, Link.DIRECTION_RIGHT, false);
        // ROM direction zero adds $0E to Link's entity X, placing the
        // sprinkle at Tarin's live $78 coordinate.
        assertTrue(session.sprinkleMagicPowder(0x6A, 0x40, 0, 0));
        boolean transformedDialogOpened = false;
        while ((!transformedDialogOpened
            || (session.overworldRoomStatusForTest(0x51) & 0x10) == 0)
            && frame < pickupFrame + 0xD00) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0x6A, 0x40, 0, 0, Link.DIRECTION_RIGHT, false);
            transformedDialogOpened |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x00A);
        }
        assertEquals(0x10, session.overworldRoomStatusForTest(0x51) & 0x10);
        assertTrue(transformedDialogOpened);
        assertTrue(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x3F),
            "the persisted flag must not truncate Tarin's live transformation");

        // Deliberately reload the room to prove the completed transformation
        // survives reconstruction before the persisted route continues.
        session.loadInitialOverworld(0x51);
        session.tickEntitiesWithProjectileEvents(
            frame, 0x6A, 0x40, 0, 0, Link.DIRECTION_RIGHT, false);
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x3F));

        walkToAndCrossOverworldBoundary(
            coordinator, scroll, collision, link, ScrollController.UP);
        assertEquals(0x41, session.currentRoomId());
        assertEquals(0x40, session.overworldRoomStatusForTest(0x41) & 0x40);
        assertTrue(session.consumeEntityEvents().stream().anyMatch(event ->
            event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE
                && event.soundId() == 0x02));

        // Exercise the same action/facing bridge that Main calls after input
        // has positioned Link at the chest.
        RoomSession.ChestOpenResult tailKeyChest = session.tryOpenChest(
            0x38, 0x31, Link.DIRECTION_UP, true, playerState.swordLevel());
        assertTrue(tailKeyChest.opened());
        assertEquals(ChestContentsTable.CHEST_TAIL_KEY, tailKeyChest.itemType());
        session.tickEntities(frame++, 0x38, 0x31);
        session.consumeChestRewardEvents().forEach(
            reward -> playerState.applyChestReward(reward.itemType()));
        assertEquals(1, playerState.tailKeyCount());
        session.setTailKeyOwned(playerState.tailKeyCount() != 0);

        boolean tailKeyOwlDialogOpened = false;
        int tailKeyOwlDeadline = frame + 0x300;
        while (!tailKeyOwlDialogOpened && frame < tailKeyOwlDeadline) {
            session.tickEntities(frame++, 0x50, 0x50);
            tailKeyOwlDialogOpened |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x0C1);
        }
        assertTrue(tailKeyOwlDialogOpened);
        int tailKeyOwlExitDeadline = frame + 0x100;
        while ((session.overworldRoomStatusForTest(0x41) & 0x20) == 0
            && frame < tailKeyOwlExitDeadline) {
            session.tickEntities(frame++, 0x50, 0x50);
        }
        assertEquals(0x20, session.overworldRoomStatusForTest(0x41) & 0x20);

        int[] routeToTailCave = {
            ScrollController.DOWN, ScrollController.DOWN, ScrollController.DOWN,
            ScrollController.LEFT, ScrollController.DOWN, ScrollController.DOWN,
            ScrollController.DOWN, ScrollController.DOWN, ScrollController.DOWN,
            ScrollController.RIGHT, ScrollController.RIGHT, ScrollController.DOWN,
            ScrollController.RIGHT
        };
        int[] tailCaveRouteRooms = {
            0x51, 0x61, 0x71, 0x70, 0x80, 0x90, 0xA0,
            0xB0, 0xC0, 0xC1, 0xC2, 0xD2, 0xD3
        };
        for (int index = 0; index < routeToTailCave.length; index++) {
            walkToAndCrossOverworldBoundary(
                coordinator, scroll, collision, link, routeToTailCave[index]);
            assertEquals(tailCaveRouteRooms[index], session.currentRoomId());
        }

        // Exercise Main's collision bridge with the source upward collision
        // bit and the two source keyhole-probe coordinates.
        var tailCaveSounds = new java.util.ArrayList<GameplaySoundEvent>();
        session.setColorShellSoundSink(tailCaveSounds::add);
        assertTrue(session.tryUnlockTailCaveKeyhole(
            0x5A, 0x4A, Link.DIRECTION_UP, 0x01,
            playerState.tailKeyCount() != 0));
        assertEquals(0x10, session.overworldRoomStatusForTest(0xD3) & 0x10);
        assertEquals(0xDF, session.tailCaveKeyholeCountdownForTest());
        int rumbleTicks = 0;
        boolean shookRight = false;
        boolean shookLeft = false;
        while (session.tailCaveKeyholeSequenceActive()) {
            int shake = session.tickTailCaveKeyholeSequence();
            shookRight |= shake == 1;
            shookLeft |= shake == -2;
            rumbleTicks++;
        }
        assertEquals(0xE0, rumbleTicks);
        assertTrue(shookRight);
        assertTrue(shookLeft);
        assertEquals(List.of(GameplaySoundEvent.DOOR_UNLOCKED,
            GameplaySoundEvent.OPEN_KEY_CAVERN,
            GameplaySoundEvent.DUNGEON_OPENED), tailCaveSounds);
        assertEquals(0xE3, session.activeRoom().roomObjectsArea()[
            ROOM_OBJECTS_BASE + 0x16]);

        Warp tailCaveEntrance = session.activeRoom().warps().stream()
            .filter(warp -> warp.tileLocation() == 0x16)
            .findFirst().orElseThrow();
        link.setPixelPosition(
            (tailCaveEntrance.tileLocation() & 0x0F) * 16,
            (tailCaveEntrance.tileLocation() >>> 4) * 16);
        coordinator.handleWarpAndIndoorBoundaries(link);
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
        assertEquals(0x00, session.activeRoom().mapId());
        assertEquals(0x17, session.currentRoomId());

        int[] entranceExit = reachableBoundaryPosition(
            collision, link.pixelX(), link.pixelY(), ScrollController.UP);
        assertNotNull(entranceExit, collisionGrid(collision));
        link.setPixelPosition(entranceExit[0], -1);
        coordinator.handleWarpAndIndoorBoundaries(link);
        while (scroll.isActive()) scroll.tick(8);
        assertEquals(0x13, session.currentRoomId());

        int chestIndex = ROOM_OBJECTS_BASE + 0x28;
        assertEquals(0x63, session.activeRoomEventForTest());
        assertFalse(session.activeRoom().roomObjectsArea()[chestIndex] == 0xA0);
        int buttonDeadline = frame + 0x40;
        while (session.activeRoomEventForTest() != 0 && frame < buttonDeadline) {
            session.tickEntities(frame++, 0x58, 0x40);
        }
        assertEquals(0, session.activeRoomEventForTest());
        int chestAppearanceDeadline = frame + 0x20;
        while (session.activeRoom().roomObjectsArea()[chestIndex] != 0xA0
            && frame < chestAppearanceDeadline) {
            session.tickEntities(frame++, 0x40, 0x60);
        }
        assertEquals(0xA0, session.activeRoom().roomObjectsArea()[chestIndex]);

        RoomSession.ChestOpenResult firstSmallKeyChest = session.tryOpenChest(
            0x78, 0x21, Link.DIRECTION_UP, true, playerState.swordLevel());
        assertTrue(firstSmallKeyChest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, firstSmallKeyChest.itemType());
        session.tickEntities(frame++, 0x78, 0x21);
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x14, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x0F, session.currentRoomId());

        int upperKeyDoorIndex = ROOM_OBJECTS_BASE + 0x39;
        int lowerKeyDoorIndex = ROOM_OBJECTS_BASE + 0x49;
        assertEquals(0x33, session.activeRoom().roomObjectsArea()[upperKeyDoorIndex]);
        assertEquals(0x34, session.activeRoom().roomObjectsArea()[lowerKeyDoorIndex]);
        // bank2's right-facing key-door branch probes Link at ($85,$38)
        // with collision bit $08 against the vertical door pair $39/$49.
        int firstKeyDoorLinkX = 0x85;
        int firstKeyDoorLinkY = 0x38;
        int rightCollisionBit = 0x08;
        link.setPixelPosition(firstKeyDoorLinkX, firstKeyDoorLinkY);
        assertTrue(session.tryUnlockIndoorKeyDoor(
            link.pixelX(), link.pixelY(), Link.DIRECTION_RIGHT, rightCollisionBit));
        assertEquals(0, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            session.tickEntities(frame++, link.pixelX(), link.pixelY());
            assertTrue(session.consumeWorldLinkMotionBlockRequest());
        }
        session.tickEntities(frame++, link.pixelX(), link.pixelY());
        assertFalse(session.consumeWorldLinkMotionBlockRequest());
        assertEquals(0x0B,
            session.activeRoom().roomObjectsArea()[upperKeyDoorIndex]);
        assertEquals(0x0C,
            session.activeRoom().roomObjectsArea()[lowerKeyDoorIndex]);
        assertEquals(0x01, session.indoorRoomStatusForTest(0x00, 0x0F) & 0x01);
        assertEquals(0x02, session.indoorRoomStatusForTest(0x00, 0x10) & 0x02);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x10, session.currentRoomId());
        assertEquals(0, session.activeRoomEventForTest());
        assertTrue(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x1E));

        // Rolling Bones is reachable to the east, but the progression-bearing
        // route is north: room $0A supplies the stone beak and its three-card
        // puzzle is the next sword-solvable room before Link owns the feather.
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x0A, session.currentRoomId());
        assertEquals(0x61, session.activeRoomEventForTest());
        List<RoomEntity> cards = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type()
                == EntitySpriteHandlerCatalog.ENTITY_THREE_OF_A_KIND)
            .toList();
        assertEquals(3, cards.size());
        assertTrue(cards.stream().allMatch(card -> card.spriteDefinition().supported()));

        // The Stone Beak branch is optional. The required route returns south
        // and west to the ROM's static small-key chest in room $0E.
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.DOWN);
        assertEquals(0x10, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x0F, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x0E, session.currentRoomId());

        RoomSession.ChestOpenResult secondSmallKeyChest = session.tryOpenChest(
            0x38, 0x21, Link.DIRECTION_UP, true, playerState.swordLevel());
        assertTrue(secondSmallKeyChest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, secondSmallKeyChest.itemType());
        session.tickEntitiesWithProjectileEvents(
            frame++, 0x38, 0x21, 0, 0, Link.DIRECTION_DOWN, false);
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x0D, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x07, session.currentRoomId());

        link.setPixelPosition(0x48, 0x05);
        assertTrue(session.tryUnlockIndoorKeyDoor(
            link.pixelX(), link.pixelY(), Link.DIRECTION_UP, 0x01));
        for (int tick = 0; tick <= 8; tick++) {
            session.tickEntitiesWithProjectileEvents(
                frame++, link.pixelX(), link.pixelY(), 0, 0, Link.DIRECTION_UP, false);
        }
        assertEquals(0, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x04, session.currentRoomId());

        int movableBlockIndex = ROOM_OBJECTS_BASE + 0x32;
        assertEquals(0xA7, session.activeRoom().roomObjectsArea()[movableBlockIndex]);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(session.tryPushIndoorBlock(
                0x15, 0x28, Link.DIRECTION_RIGHT, 0x08));
        }
        for (int tick = 0; tick < 33; tick++) {
            session.tickEntities(frame++, 0x15, 0x28);
        }
        assertEquals(0, session.activeRoom().shutterDoorMask());
        assertEquals(0, session.indoorRoomStatusForTest(0x00, 0x04) & 0x10);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x03, session.currentRoomId());
        assertEquals(0xA1, session.activeRoomEventForTest());
        int hiddenStairsIndex = ROOM_OBJECTS_BASE + 0x18;
        assertEquals(0x0D, session.activeRoom().roomObjectsArea()[hiddenStairsIndex]);
        List<RoomEntity> beetles = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x2C)
            .toList();
        assertEquals(2, beetles.size());

        // EntityInitHandler runs before the first interactive collision pass.
        session.tickEntitiesWithProjectileEvents(
            frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        beetles = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x2C)
            .toList();
        // EnemyCollidedWithSword flips state-$00 beetles without damage.
        for (RoomEntity beetle : beetles) {
            List<EntityCombatEvent> flipEvents = session.resolveEntityCombat(
                beetle.slot() ^ 1, 0, 0, false, true, true,
                beetle.x() + 0x08, 1, beetle.y() - beetle.z() + 0x08, 1);
            assertTrue(flipEvents.stream().anyMatch(event -> event.slot() == beetle.slot()
                && event.swordHit()), flipEvents.toString());
        }
        session.tickEntitiesWithProjectileEvents(
            frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        beetles = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x2C)
            .toList();
        for (RoomEntity beetle : beetles) {
            List<EntityCombatEvent> damageEvents = session.resolveEntityCombat(
                beetle.slot() ^ 1, 0, 0, false, true, true,
                beetle.x() + 0x08, 1, beetle.y() - beetle.z() + 0x08, 1);
            assertTrue(damageEvents.stream().anyMatch(event -> event.slot() == beetle.slot()
                && event.swordHit() && event.enemyDamage() > 0), damageEvents.toString());
        }
        for (int attack = 0; attack < 3; attack++) {
            for (int recovery = 0; recovery < 12; recovery++) {
                session.tickEntitiesWithProjectileEvents(
                    frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
            }
            beetles = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x2C)
                .toList();
            for (RoomEntity beetle : beetles) {
                session.resolveEntityCombat(
                    beetle.slot() ^ 1, 0, 0, false, true, true,
                    beetle.x() + 0x08, 1, beetle.y() - beetle.z() + 0x08, 1);
            }
        }
        int beetleDeadline = frame + 0x200;
        while (session.activeRoomEventForTest() != 0 && frame < beetleDeadline) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        }
        assertEquals(0, session.activeRoomEventForTest(),
            session.activeRoom().entities().loadedEntities().toString());
        assertEquals(0x0D, session.activeRoom().roomObjectsArea()[hiddenStairsIndex]);
        for (int revealFrame = 0; revealFrame < 11; revealFrame++) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        }
        assertEquals(0xBE, session.activeRoom().roomObjectsArea()[hiddenStairsIndex]);
        assertEquals(0x10, session.indoorRoomStatusForTest(0x00, 0x03) & 0x10);
        session.loadIndoor(0x00, 0x03);
        assertEquals(0xBF, session.activeRoom().roomObjectsArea()[hiddenStairsIndex]);

        // .configureStairs leaves the staircase inactive when the room loads.
        // The source arms it only after Link leaves the 12x12 center box.
        link.setPixelPosition(0x80, 0x10); // ROM entity center ($88,$20)
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertFalse(transition.isActive());
        link.setPixelPosition(0x90, 0x10); // ROM entity center ($98,$20)
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertFalse(transition.isActive());
        link.setPixelPosition(0x80, 0x10);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(transition.isActive());
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_SIDESCROLL, session.mapCategory());
        assertEquals(0x19, session.currentRoomId());
        assertEquals(0x70, link.pixelX());
        assertEquals(0x00, link.pixelY());

        link.setPixelPosition(link.pixelX(), -1);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(transition.isActive());
        assertFalse(scroll.isActive());
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
        assertEquals(0x03, session.currentRoomId());
        assertEquals(0x80, link.pixelX());
        assertEquals(0x10, link.pixelY());
        assertEquals(0xBF, session.activeRoom().roomObjectsArea()[hiddenStairsIndex]);
    }

    @Test
    void mysteriousWoodsUpBoundaryRoutesToRoom63AndQueuesTheLostJingle()
            throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadInitialOverworld(0x51);
        session.tickEntities(0, 0x78, 0x60, 0, 1);
        session.tickEntities(1, 0x78, 0x1F, 0, 1);
        assertTrue(session.shouldGetLostInMysteriousWoods());

        ScrollController scroll = new ScrollController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
            session, new RoomBoundaryController(), new TransitionController(), scroll);
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            romTables, collision, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x78, -1);

        coordinator.handleOverworldBoundary(link);

        assertEquals(0x63, session.currentRoomId());
        assertTrue(scroll.isActive());
        assertTrue(session.consumeEntityEvents().stream().anyMatch(event ->
            event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE
                && event.soundId() == 0x1E));
    }

    private static RoomSession newSession(byte[] rom, RomTables romTables, OverworldCollision collision) {
        return new RoomSession(
            rom,
            new GPU(),
            new RoomLoader(rom),
            new OverworldTilesetTable(rom),
            collision,
            new TransientVfxSystem(16),
            null
        );
    }

    private static void crossOverworldBoundary(RoomTransitionCoordinator coordinator,
                                                ScrollController scroll, Link link,
                                                int direction) {
        switch (direction) {
            case ScrollController.UP -> link.setPixelPosition(link.pixelX(), -1);
            case ScrollController.DOWN ->
                link.setPixelPosition(link.pixelX(), RoomConstants.ROOM_PIXEL_HEIGHT);
            case ScrollController.LEFT -> link.setPixelPosition(-1, link.pixelY());
            case ScrollController.RIGHT ->
                link.setPixelPosition(RoomConstants.ROOM_PIXEL_WIDTH, link.pixelY());
            default -> throw new IllegalArgumentException("Unknown scroll direction: " + direction);
        }
        coordinator.handleOverworldBoundary(link);
        while (scroll.isActive()) {
            scroll.tick(8);
        }
    }

    private static void walkToAndCrossOverworldBoundary(
            RoomTransitionCoordinator coordinator, ScrollController scroll,
            OverworldCollision collision, Link link, int direction) {
        // This is a collision-connectivity harness, not a keyboard-input
        // simulation: find a source-valid pixel path to the requested edge,
        // then exercise the real room-boundary transition from that edge.
        int[] exit = reachableBoundaryPosition(
            collision, link.pixelX(), link.pixelY(), direction);
        assertNotNull(exit, "No sword-accessible collision path from ("
            + link.pixelX() + "," + link.pixelY() + ") toward " + direction
            + "\n" + collisionGrid(collision));
        link.setPixelPosition(exit[0], exit[1]);
        crossOverworldBoundary(coordinator, scroll, link, direction);
    }

    private static void walkToAndCrossIndoorBoundary(
            RoomTransitionCoordinator coordinator, TransitionController transition,
            ScrollController scroll, OverworldCollision collision, Link link,
            int direction) {
        List<int[]> path = reachableBoundaryPath(
            collision, link.pixelX(), link.pixelY(), direction);
        assertNotNull(path, "No collision path to indoor boundary " + direction
            + "\n" + collisionGrid(collision));
        for (int[] position : path) {
            link.setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive(),
                "collision path crossed an active indoor warp");
            assertFalse(scroll.isActive(),
                "collision path left the room before its target boundary");
        }
        int[] exit = path.getLast();
        link.setPixelPosition(exit[0], exit[1]);
        switch (direction) {
            case ScrollController.UP -> link.setPixelPosition(link.pixelX(), -1);
            case ScrollController.DOWN ->
                link.setPixelPosition(link.pixelX(), RoomConstants.ROOM_PIXEL_HEIGHT);
            case ScrollController.LEFT -> link.setPixelPosition(-1, link.pixelY());
            case ScrollController.RIGHT ->
                link.setPixelPosition(RoomConstants.ROOM_PIXEL_WIDTH, link.pixelY());
            default -> throw new IllegalArgumentException(
                "Unknown indoor scroll direction: " + direction);
        }
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(scroll.isActive());
        assertEquals(direction, scroll.direction());
        switch (direction) {
            case ScrollController.UP ->
                assertEquals(RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE,
                    link.pixelY());
            case ScrollController.DOWN -> assertEquals(0, link.pixelY());
            case ScrollController.LEFT ->
                assertEquals(RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE,
                    link.pixelX());
            case ScrollController.RIGHT -> assertEquals(0, link.pixelX());
            default -> throw new AssertionError(direction);
        }
        while (scroll.isActive()) {
            scroll.tick(8);
        }
    }

    private static List<int[]> reachableBoundaryPath(OverworldCollision collision,
                                                      int startX, int startY,
                                                      int targetDirection) {
        int width = RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1;
        int height = RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1;
        boolean[][] visited = new boolean[height][width];
        int[][] parentX = new int[height][width];
        int[][] parentY = new int[height][width];
        for (int row = 0; row < height; row++) {
            java.util.Arrays.fill(parentX[row], -1);
            java.util.Arrays.fill(parentY[row], -1);
        }
        int clampedX = Math.max(0, Math.min(startX, width - 1));
        int clampedY = Math.max(0, Math.min(startY, height - 1));
        visited[clampedY][clampedX] = true;
        parentX[clampedY][clampedX] = clampedX;
        parentY[clampedY][clampedX] = clampedY;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {clampedX, clampedY});
        int[][] moves = {{0, -1, Link.DIRECTION_UP}, {0, 1, Link.DIRECTION_DOWN},
            {-1, 0, Link.DIRECTION_LEFT}, {1, 0, Link.DIRECTION_RIGHT}};

        while (!queue.isEmpty()) {
            int[] position = queue.removeFirst();
            if (isTargetBoundary(
                position[0], position[1], width, height, targetDirection)) {
                ArrayDeque<int[]> reverse = new ArrayDeque<>();
                int x = position[0];
                int y = position[1];
                while (parentX[y][x] != x || parentY[y][x] != y) {
                    reverse.addFirst(new int[] {x, y});
                    int nextX = parentX[y][x];
                    int nextY = parentY[y][x];
                    x = nextX;
                    y = nextY;
                }
                reverse.addFirst(new int[] {x, y});
                return List.copyOf(reverse);
            }
            for (int[] move : moves) {
                int x = position[0] + move[0];
                int y = position[1] + move[1];
                if (x < 0 || x >= width || y < 0 || y >= height
                    || visited[y][x] || leadingEdgeBlocked(collision, x, y, move[2])) {
                    continue;
                }
                visited[y][x] = true;
                parentX[y][x] = position[0];
                parentY[y][x] = position[1];
                queue.addLast(new int[] {x, y});
            }
        }
        return null;
    }

    /** Floods Link top-left positions using the source's two leading-edge probes. */
    private static int[] reachableBoundaryPosition(OverworldCollision collision,
                                                    int startX, int startY,
                                                    int targetDirection) {
        int width = RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1;
        int height = RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1;
        boolean[][] visited = new boolean[height][width];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        int clampedX = Math.max(0, Math.min(startX, width - 1));
        int clampedY = Math.max(0, Math.min(startY, height - 1));
        visited[clampedY][clampedX] = true;
        queue.add(new int[] {clampedX, clampedY});

        int[][] moves = {{0, -1, Link.DIRECTION_UP}, {0, 1, Link.DIRECTION_DOWN},
            {-1, 0, Link.DIRECTION_LEFT}, {1, 0, Link.DIRECTION_RIGHT}};
        while (!queue.isEmpty()) {
            int[] position = queue.removeFirst();
            if (isTargetBoundary(position[0], position[1], width, height, targetDirection)) {
                return position;
            }
            for (int[] move : moves) {
                int x = position[0] + move[0];
                int y = position[1] + move[1];
                if (x < 0 || x >= width || y < 0 || y >= height || visited[y][x]
                    || leadingEdgeBlocked(collision, x, y, move[2])) {
                    continue;
                }
                visited[y][x] = true;
                queue.addLast(new int[] {x, y});
            }
        }
        return null;
    }

    private static boolean isTargetBoundary(int x, int y, int width, int height,
                                            int direction) {
        return switch (direction) {
            case ScrollController.UP -> y == 0;
            case ScrollController.DOWN -> y == height - 1;
            case ScrollController.LEFT -> x == 0;
            case ScrollController.RIGHT -> x == width - 1;
            default -> false;
        };
    }

    private static boolean leadingEdgeBlocked(OverworldCollision collision,
                                              int x, int y, int direction) {
        int[][] pointX = {{6, 9}, {6, 9}, {4, 4}, {11, 11}};
        int[][] pointY = {{15, 15}, {6, 6}, {9, 12}, {9, 12}};
        for (int index = 0; index < 2; index++) {
            int sampleX = x + pointX[direction][index];
            int sampleY = y + pointY[direction][index];
            int object = collision.objectIdAtPoint(sampleX, sampleY);
            // This connectivity helper models the sword-owned route after its
            // already-covered static sword collision removes a bush.
            boolean swordCuttable = object == 0x5C || object == 0xD3;
            if (!swordCuttable && collision.pointBlockedForLink(sampleX, sampleY, false)
                && !collision.pointNormalPit(sampleX, sampleY)) {
                return true;
            }
        }
        return false;
    }

    private static String collisionGrid(OverworldCollision collision) {
        StringBuilder result = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 10; col++) {
                int x = col * 16 + 8;
                int y = row * 16 + 8;
                result.append(String.format("%02X%s ", collision.objectIdAtPoint(x, y),
                    collision.pointBlockedForLink(x, y, false) ? "#" : "."));
            }
            result.append('\n');
        }
        return result.toString();
    }

    private static int[] roomWithPitAt(int col, int row) {
        int[] roomObjectsArea = new int[0x100];
        java.util.Arrays.fill(roomObjectsArea, 0xFF);
        roomObjectsArea[ROOM_OBJECTS_BASE + row * ROOM_OBJECT_ROW_STRIDE + col] = OBJECT_NORMAL_PIT;
        return roomObjectsArea;
    }

    private static void runUntilPitFallStarts(Link link) {
        int guard = 0;
        while (!link.isFallingIntoPit() && guard++ < 128) {
            link.update();
        }
        assertTrue(link.isFallingIntoPit());
    }

    private static void runUntilPitFallFinishes(Link link) {
        int guard = 0;
        while (link.isFallingIntoPit() && guard++ < 128) {
            link.update();
        }
        assertFalse(link.isFallingIntoPit());
    }

    @Test
    void completedMamboPlaybackUsesTheRomDestinationAfterTheNonInteractiveEffect()
        throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadIndoor(0x00, 0x25);
        assertTrue(session.startOcarina(0x01, 0x02, 0x01));
        session.tickEntities(0);

        TransitionController transitionController = new TransitionController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
            session, new RoomBoundaryController(), transitionController, new ScrollController());
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            romTables, collision, null, new PlayerState(), new ItemRegistry());

        coordinator.handlePendingManboTransition(link);
        assertEquals(TransitionController.State.MANBO_IN, transitionController.state());

        for (int frame = 0; frame < TransitionController.MANBO_TRANSITION_FRAMES; frame++) {
            transitionController.tick();
        }

        assertEquals(TransitionController.State.MANBO_OUT, transitionController.state());
        assertEquals(0x17, session.currentRoomId());
        assertEquals(0x48, link.pixelX());
        assertEquals(0x6C, link.pixelY());

        for (int frame = TransitionController.MANBO_OUT_INITIAL_FRAME;
             frame < TransitionController.MANBO_TRANSITION_FRAMES; frame++) {
            transitionController.tick();
        }
        assertEquals(TransitionController.State.IDLE, transitionController.state());
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = RoomTransitionCoordinatorTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
