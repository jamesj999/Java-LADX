package linksawakening.world;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.function.BiPredicate;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.equipment.Sword;
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
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomTransitionCoordinatorTest {
    private static final int MAP_WITH_NO_WARP_ROOM = 0x01;
    private static final int INDOOR_ROOM_WITH_NO_WARPS = 0x00;
    private static final int OBJECT_NORMAL_PIT = 0xE8;

    @Test
    void linkRoomPhysicsDefaultReinitializesAcrossRoomAndSaveLikeReloads() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        ScrollController scroll = new ScrollController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
            session, new RoomBoundaryController(), new TransitionController(), scroll);
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            romTables, collision, null, new PlayerState(), new ItemRegistry());

        session.loadIndoor(0x00, 0x19, Warp.CATEGORY_SIDESCROLL);
        link.setPixelPosition(0x50, 0x40);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertEquals(0x01, link.romPhysicsModifier());

        link.setRomPhysicsModifier(0x02);
        session.loadIndoor(0x00, 0x19, Warp.CATEGORY_INDOOR);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertEquals(0x00, link.romPhysicsModifier(),
            "top-down reload resets a prior side-view modifier");

        session.loadIndoor(0x00, 0x19, Warp.CATEGORY_SIDESCROLL);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertEquals(0x01, link.romPhysicsModifier(),
            "same-room side-view reload restores the source entry default");

        link.setRomPhysicsModifier(0x02);
        RoomSession restoredSession = newSession(rom, romTables, collision);
        restoredSession.loadIndoor(0x00, 0x19, Warp.CATEGORY_SIDESCROLL);
        RoomTransitionCoordinator restoredCoordinator = new RoomTransitionCoordinator(
            restoredSession, new RoomBoundaryController(), new TransitionController(),
            new ScrollController());
        restoredCoordinator.handleWarpAndIndoorBoundaries(link);
        assertEquals(0x01, link.romPhysicsModifier(),
            "a new session reload restores the source entry default");
    }

    @Test
    void bottleGrottoTwoTorchTriggerOpensTheRoom31EastShutter() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadIndoor(0x01, 0x31);

        assertEquals(0x25, session.activeRoomEventForTest());
        assertEquals(0xAB, objectAt(session, 0x34));
        assertEquals(0xAB, objectAt(session, 0x35));
        assertEquals(0x08, session.activeRoom().shutterDoorMask() & 0x08);
        int closedEastDoorObject = objectAt(session, 0x39);
        int[] closedEastDoorTiles = roomObjectTiles(session, 0x39);
        assertEquals(0x3B, closedEastDoorObject);
        assertEquals(0x3C, objectAt(session, 0x49));

        int frame = 0;
        assertTrue(session.sprinkleMagicPowder(0x33, 0x39, 0, 0));
        for (int tick = 0; tick < 16; tick++) {
            tickInteractiveEntities(session, frame++, 0x40, 0x50);
        }
        assertTrue(session.sprinkleMagicPowder(0x43, 0x39, 0, 0));
        for (int tick = 0; tick < 16; tick++) {
            tickInteractiveEntities(session, frame++, 0x50, 0x50);
        }

        assertEquals(2, session.roomTriggerCountForTest());
        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(0, session.activeRoom().shutterDoorMask() & 0x08);
        assertEquals(0x0B, objectAt(session, 0x39));
        assertEquals(0x0C, objectAt(session, 0x49));
        assertFalse(java.util.Arrays.equals(
            closedEastDoorTiles, roomObjectTiles(session, 0x39)));
    }

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
        session.loadInitialOverworld(0x92);
        session.loadIndoor(0x00, 0x19, Warp.CATEGORY_SIDESCROLL);
        assertEquals(Warp.CATEGORY_INDOOR, session.activeRoom().firstWarp().category());
        assertEquals(0x03, session.activeRoom().firstWarp().destRoom());

        TransitionController transition = new TransitionController();
        ScrollController scroll = new ScrollController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
            session, new RoomBoundaryController(), transition, scroll);
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            romTables, collision, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x70, -5);

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
    void tailCaveSideScrollPassageRoutesRoom19Through18ToRoom1() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadInitialOverworld(0x92);
        session.loadIndoor(0x00, 0x19, Warp.CATEGORY_SIDESCROLL);

        TransitionController transition = new TransitionController();
        ScrollController scroll = new ScrollController();
        RoomTransitionCoordinator coordinator = new RoomTransitionCoordinator(
            session, new RoomBoundaryController(), transition, scroll);
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            romTables, collision, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(-5, 0x30);

        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(scroll.isActive());
        assertEquals(0x18, session.currentRoomId());
        while (scroll.isActive()) scroll.tick(8);

        link.setPixelPosition(0x20, -5);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(transition.isActive());
        assertFalse(scroll.isActive());
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
        assertEquals(0x01, session.currentRoomId());
        assertEquals(0x40, link.pixelX());
        assertEquals(0x50, link.pixelY());
    }

    @Test
    void bowWowHideoutDirectRoomIdsMoveNorthByOneHexRow() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadIndoor(0x15, 0xF0);

        ScrollController scroll = new ScrollController();
        session.startAdjacentIndoorScroll(scroll, ScrollController.UP, 0x50, 0x00);

        assertEquals(0xE0, session.currentRoomId());
        assertEquals(4, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x0B).count());
    }

    @Test
    void freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder()
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
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        ItemRegistry itemRegistry = new ItemRegistry();
        Link link = new Link(inputState, inputConfig,
            romTables, collision, null, playerState, itemRegistry);
        itemRegistry.register(
            PlayerState.INVENTORY_ROCS_FEATHER, new RocsFeather(link));
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
        tickInteractiveEntities(session, 0, 0x3F, 0x44);
        tickInteractiveEntities(session, 1, 0x40, 0x44);
        assertEquals(0x22, session.consumePendingMusicTrack());

        int frame = 2;
        boolean owlDialogOpened = false;
        while ((session.overworldRoomStatusForTest(0xF2) & 0x20) == 0
            && frame < 0x300) {
            tickInteractiveEntities(session, frame++, 0x58, 0x60);
            owlDialogOpened |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x0D9);
        }
        assertTrue(owlDialogOpened);

        while (session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x41) && frame < 0x400) {
            tickInteractiveEntities(session, frame++, 0x58, 0x60);
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

        tickInteractiveEntities(session, frame++, 0x50, 0x50);
        tickInteractiveEntities(session, frame++, 0x50, 0x50);
        assertEquals(0x22, session.consumePendingMusicTrack());
        boolean forestOwlDialogOpened = false;
        while (!forestOwlDialogOpened && frame < pickupFrame + 0x600) {
            tickInteractiveEntities(session, frame++, 0x50, 0x50);
            forestOwlDialogOpened |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x0C0);
        }
        assertTrue(forestOwlDialogOpened);
        while ((session.overworldRoomStatusForTest(0x80) & 0x20) == 0
            && frame < pickupFrame + 0x700) {
            tickInteractiveEntities(session, frame++, 0x50, 0x50);
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
            tickInteractiveEntities(session, frame++, 0x50, 0x50);
            tailKeyOwlDialogOpened |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x0C1);
        }
        assertTrue(tailKeyOwlDialogOpened);
        int tailKeyOwlExitDeadline = frame + 0x100;
        while ((session.overworldRoomStatusForTest(0x41) & 0x20) == 0
            && frame < tailKeyOwlExitDeadline) {
            tickInteractiveEntities(session, frame++, 0x50, 0x50);
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

        // Room $16 is the ROM's third Small Key source. Lure both Hardhat
        // Beetles into the continuous north pit row, then collect the live
        // DropKeyEffectHandler entity produced by event $81.
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x16, session.currentRoomId());
        session.tickEntitiesWithProjectileEvents(
            frame++, 0x50, 0x50, 0, 0, Link.DIRECTION_UP, false);
        List<Integer> hardhatSlots = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x20)
            .map(RoomEntity::slot).toList();
        assertEquals(2, hardhatSlots.size());
        for (int hardhatSlot : hardhatSlots) {
            for (int hit = 0; hit < 8
                && session.activeRoom().entities().slots().get(hardhatSlot).status()
                    == EntityStatus.ACTIVE; hit++) {
                RoomEntity hardhat = session.activeRoom().entities().slots().get(hardhatSlot);
                List<EntityCombatEvent> recoil = session.resolveEntityCombat(
                    hardhatSlot ^ 1, hardhat.x(), hardhat.y() + 0x20,
                    false, true, true, hardhat.x(), 0x10, hardhat.y(), 0x10);
                assertTrue(recoil.stream().anyMatch(event ->
                    event.slot() == hardhatSlot && event.swordHit()));
                for (int recoilFrame = 0; recoilFrame < 0x30; recoilFrame++) {
                    session.tickEntitiesWithProjectileEvents(
                        frame++, hardhat.x(), hardhat.y() + 0x20,
                        0, 0, Link.DIRECTION_UP, false);
                }
            }
        }
        int room16Deadline = frame + 0x300;
        while (session.activeRoomEventForTest() != 0 && frame < room16Deadline) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0x50, 0x50, 0, 0, Link.DIRECTION_UP, false);
        }
        assertEquals(0, session.activeRoomEventForTest(),
            session.activeRoom().entities().loadedEntities().toString());
        RoomEntity droppedSmallKey = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x30)
            .findFirst().orElseThrow();
        int keyLandingDeadline = frame + 0x200;
        while (droppedSmallKey.z() != 0 && frame < keyLandingDeadline) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0x50, 0x50, 0, 0, Link.DIRECTION_DOWN, false);
            droppedSmallKey = session.activeRoom().entities().slots()
                .get(droppedSmallKey.slot());
        }
        assertEquals(0, droppedSmallKey.z());
        int keyPickupFrame = (droppedSmallKey.slot() & 0x01) == 0
            ? frame | 1 : frame & ~1;
        assertNotNull(session.collectEntityIfNeeded(
            keyPickupFrame, droppedSmallKey.x(), droppedSmallKey.y(),
            false, true, Link.DIRECTION_DOWN, 0));
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x17, session.currentRoomId());
        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(0, session.activeRoom().shutterDoorMask());

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
        assertEquals(2, session.currentDungeonItemFlagsSnapshot()[
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
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
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
        assertEquals(2, session.currentDungeonItemFlagsSnapshot()[
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
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x04, session.currentRoomId());

        int movableBlockIndex = ROOM_OBJECTS_BASE + 0x32;
        assertEquals(0xA7, session.activeRoom().roomObjectsArea()[movableBlockIndex]);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(session.tryInteractWithIndoorBlock(
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

        link.setPixelPosition(-5, 0x30);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(scroll.isActive());
        assertEquals(0x18, session.currentRoomId());
        while (scroll.isActive()) scroll.tick(8);

        link.setPixelPosition(0x20, -5);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(transition.isActive());
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
        assertEquals(0x01, session.currentRoomId());
        assertEquals(0x40, link.pixelX());
        assertEquals(0x50, link.pixelY());

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x1C, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x1D, session.currentRoomId());

        RoomSession.ChestOpenResult featherChest = session.tryOpenChest(
            0x38, 0x21, Link.DIRECTION_UP, true, playerState.swordLevel());
        assertTrue(featherChest.opened());
        assertEquals(ChestContentsTable.CHEST_FEATHER, featherChest.itemType());
        session.tickEntitiesWithProjectileEvents(
            frame++, 0x38, 0x21, 0, 0, Link.DIRECTION_UP, false);
        session.consumeChestRewardEvents().forEach(
            reward -> playerState.applyChestReward(reward.itemType()));
        boolean hasFeather = playerState.itemA() == PlayerState.INVENTORY_ROCS_FEATHER
            || playerState.itemB() == PlayerState.INVENTORY_ROCS_FEATHER;
        int featherSubscreenSlot = -1;
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            hasFeather |= playerState.subscreenItem(slot)
                == PlayerState.INVENTORY_ROCS_FEATHER;
            if (playerState.subscreenItem(slot) == PlayerState.INVENTORY_ROCS_FEATHER) {
                featherSubscreenSlot = slot;
            }
        }
        assertTrue(hasFeather);
        if (playerState.itemA() != PlayerState.INVENTORY_ROCS_FEATHER) {
            assertTrue(featherSubscreenSlot >= 0);
            playerState.swapItemAWithSubscreen(featherSubscreenSlot);
        }
        assertEquals(PlayerState.INVENTORY_ROCS_FEATHER, playerState.itemA());

        // Return through the source-authored side-view passage. Room $01's
        // staircase is inactive at load and arms only after Link leaves it.
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.DOWN);
        assertEquals(0x1C, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.DOWN);
        assertEquals(0x01, session.currentRoomId());
        link.setPixelPosition(0x40, 0x50);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertFalse(transition.isActive());
        link.setPixelPosition(0x50, 0x50);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertFalse(transition.isActive());
        link.setPixelPosition(0x40, 0x50);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(transition.isActive());
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_SIDESCROLL, session.mapCategory());
        assertEquals(0x18, session.currentRoomId());

        link.setPixelPosition(RoomConstants.ROOM_PIXEL_WIDTH, 0x30);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(scroll.isActive());
        assertEquals(0x19, session.currentRoomId());
        while (scroll.isActive()) scroll.tick(8);
        link.setPixelPosition(0x70, -5);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(transition.isActive());
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
        assertEquals(0x03, session.currentRoomId());

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x04, session.currentRoomId());
        assertEquals(null, reachableBoundaryPath(
            collision, link.pixelX(), link.pixelY(), ScrollController.DOWN, true),
            "room $04's south route must cross the ROM-authored pit\n"
                + collisionGrid(collision));
        link.setPixelPosition(0x20, 0x40);
        jumpOverPitWithFeather(
            collision, link, inputState, inputConfig, itemRegistry, playerState, 0x08);
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.DOWN);
        assertEquals(0x07, session.currentRoomId());

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.DOWN);
        assertEquals(0x0D, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x0E, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x0F, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x09, session.currentRoomId());

        // Hold against bank 2's interactive keyhole block for its source
        // $20-frame threshold. This spends room $16's third key.
        for (int tick = 0; tick < 0x20; tick++) {
            assertTrue(session.tryInteractWithIndoorBlock(
                0x20, 0x4A, Link.DIRECTION_UP, 0x01));
        }
        assertEquals(0, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(0x0D,
            session.activeRoom().roomObjectsArea()[ROOM_OBJECTS_BASE + 0x52]);
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x08, session.currentRoomId());

        link.setPixelPosition(0x38, 0x1B);
        RoomSession.ChestOpenResult nightmareKeyChest = session.tryOpenChest(
            link.pixelX(), link.pixelY(), Link.DIRECTION_UP, true,
            playerState.swordLevel());
        assertTrue(nightmareKeyChest.opened());
        assertEquals(ChestContentsTable.CHEST_NIGHTMARE_KEY,
            nightmareKeyChest.itemType());
        session.tickEntitiesWithProjectileEvents(
            frame++, 0x38, 0x1B, 0, 0, Link.DIRECTION_UP, false);
        session.consumeChestRewardEvents().forEach(
            reward -> playerState.applyChestReward(reward.itemType()));
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.NIGHTMARE_KEY_INDEX]);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x09, session.currentRoomId());
        link.setPixelPosition(0x40, 0x60);
        jumpOverPitWithFeather(
            collision, link, inputState, inputConfig, itemRegistry, playerState, 0x01);
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.DOWN);
        assertEquals(0x0F, session.currentRoomId());
        link.setPixelPosition(0x50, 0x50);
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x10, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x11, session.currentRoomId());

        RoomEntity rollingBones = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type()
                == EntitySpriteHandlerCatalog.ENTITY_ROLLING_BONES)
            .findFirst().orElseThrow();
        session.tickEntitiesWithProjectileEvents(
            frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        rollingBones = session.activeRoom().entities().slots().get(rollingBones.slot());
        for (int hit = 0; hit < 16
            && session.activeRoom().entities().slots().get(rollingBones.slot()).status()
                == EntityStatus.ACTIVE; hit++) {
            rollingBones = session.activeRoom().entities().slots().get(rollingBones.slot());
            int rollingBonesSlot = rollingBones.slot();
            List<EntityCombatEvent> hitEvents = session.resolveEntityCombat(
                rollingBonesSlot ^ 1, rollingBones.x(), rollingBones.y(),
                false, true, true, rollingBones.x(), 0x08,
                rollingBones.y() - rollingBones.z(), 0x08);
            assertTrue(hitEvents.stream().anyMatch(event ->
                event.slot() == rollingBonesSlot && event.enemyDamage() > 0),
                "hit " + hit + ": " + hitEvents);
            for (int recovery = 0; recovery < 0x80; recovery++) {
                session.tickEntitiesWithProjectileEvents(
                    frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
            }
        }
        assertEquals(EntityStatus.DYING,
            session.activeRoom().entities().slots().get(rollingBones.slot()).status());
        int rollingBonesDeadline = frame + 0x200;
        while (session.activeRoomEventForTest() != 0 && frame < rollingBonesDeadline) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        }
        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(0x20, session.indoorRoomStatusForTest(0x00, 0x11) & 0x20);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x0B, session.currentRoomId());
        assertTrue(session.tryUnlockIndoorKeyDoor(
            0x48, 0x00, Link.DIRECTION_UP, 0x01));
        for (int tick = 0; tick < 8; tick++) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0x48, 0x00, 0, 0, Link.DIRECTION_UP, false);
        }
        assertEquals(0x43, session.activeRoom().roomObjectsArea()[ROOM_OBJECTS_BASE + 0x04]);
        assertEquals(0x44, session.activeRoom().roomObjectsArea()[ROOM_OBJECTS_BASE + 0x05]);
        assertEquals(0x04, session.indoorRoomStatusForTest(0x00, 0x0B) & 0x04);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x06, session.currentRoomId());
        session.tickEntitiesWithProjectileEvents(
            frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        RoomEntity moldorm = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_MOLDORM)
            .findFirst().orElseThrow();
        for (int hit = 0; hit < 4; hit++) {
            int tailX = 0;
            int tailY = 0;
            int extensionDeadline = frame + 0x100;
            do {
                moldorm = session.activeRoom().entities().slots().get(moldorm.slot());
                List<EntitySpriteDefinition.DynamicSprite> sprites =
                    moldorm.spriteDefinition().dynamicVariant(0);
                assertEquals(16, sprites.size());
                EntitySpriteDefinition.DynamicSprite vulnerableTail = sprites.get(14);
                tailX = (moldorm.x() + vulnerableTail.xOffset()) & 0xFF;
                tailY = (moldorm.y() + vulnerableTail.yOffset() + moldorm.z()) & 0xFF;
                if (!RoomEntityCombatRules.overlapsSword(
                        moldorm, tailX, 0x08, tailY, 0x08)) {
                    break;
                }
                session.tickEntitiesWithProjectileEvents(
                    frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
            } while (frame < extensionDeadline);
            assertFalse(RoomEntityCombatRules.overlapsSword(
                moldorm, tailX, 0x08, tailY, 0x08));
            int moldormSlot = moldorm.slot();
            Sword liveSword = new Sword(romTables, null);
            liveSword.onPress();
            for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
                liveSword.tick(false);
            }
            link.setDirection(Link.DIRECTION_RIGHT);
            Sword.CollisionBox initialSwordBox = liveSword.enemyCollisionBox(
                link.romEntityX(), link.romSwordCollisionY(), link.direction());
            link.setPixelPosition(
                tailX - (initialSwordBox.x() - link.pixelX()),
                tailY - (initialSwordBox.y() - link.pixelY()));
            Sword.CollisionBox swordBox = liveSword.enemyCollisionBox(
                link.romEntityX(), link.romSwordCollisionY(), link.direction());
            assertTrue(swordBox.active());
            List<EntityCombatEvent> tailHit = session.resolveEntityCombat(
                moldormSlot ^ 1, link.romEntityX(), link.romEntityY(),
                link.isAirborne(), true, swordBox.active(),
                swordBox.x(), swordBox.width(), swordBox.y(), swordBox.height());
            assertTrue(tailHit.stream().anyMatch(event ->
                event.slot() == moldormSlot && event.enemyDamage() == 1),
                "Moldorm tail hit " + hit + " at (" + tailX + "," + tailY + "): "
                    + tailHit);
            for (int recovery = 0; recovery < 0x30; recovery++) {
                session.tickEntitiesWithProjectileEvents(
                    frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
            }
        }
        assertEquals(EntityStatus.DYING,
            session.activeRoom().entities().slots().get(moldorm.slot()).status());

        int moldormDeadline = frame + 0x400;
        RoomEntity heartContainer = null;
        while (heartContainer == null && frame < moldormDeadline) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
            heartContainer = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x36)
                .findFirst().orElse(null);
        }
        assertNotNull(heartContainer);
        int heartPickupFrame = (heartContainer.slot() & 0x01) == 0
            ? frame | 1 : frame & ~1;
        assertNotNull(session.collectEntityIfNeeded(
            heartPickupFrame, heartContainer.x(), heartContainer.y(), false, true,
            Link.DIRECTION_DOWN, heartContainer.z()));
        int previousMaxHearts = playerState.maxHearts();
        for (int held = 0; held < 0x70; held++) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0x50, 0x60, 0, 0, Link.DIRECTION_DOWN, false);
        }
        session.consumeHeartContainerRewards().forEach(
            reward -> playerState.applyHeartContainerReward());
        assertEquals(previousMaxHearts + 1, playerState.maxHearts());
        assertEquals(0x20, session.indoorRoomStatusForTest(0x00, 0x06) & 0x20);
        assertEquals(0, session.activeRoom().shutterDoorMask());

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x02, session.currentRoomId());
        RoomEntity instrument = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type()
                == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS)
            .findFirst().orElseThrow();
        session.tickEntitiesWithProjectileEvents(
            frame++, instrument.x(), instrument.y(), 0, 0, Link.DIRECTION_DOWN, false);
        assertNotNull(session.collectEntityIfNeeded(
            (instrument.slot() ^ 1) & 1, instrument.x(), instrument.y(), false, true,
            Link.DIRECTION_DOWN, 0));
        assertEquals(0x1B, session.consumePendingMusicTrack());
        for (int performance = 1; performance <= 88; performance++) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0x50, 0x60, 0, 0, Link.DIRECTION_DOWN, false);
        }
        assertTrue(session.hasDungeonInstrumentForTest(0x00));
        assertEquals(0x100,
            session.consumeEntityDialogRequests().getFirst().globalDialogId());
        session.setEntityDialogActive(true);
        session.setEntityMusicActive(true);
        session.tickEntitiesWithProjectileEvents(
            frame++, 0x50, 0x60, 0, 0, Link.DIRECTION_DOWN, false);
        session.setEntityDialogActive(false);
        session.tickEntitiesWithProjectileEvents(
            frame++, 0x50, 0x60, 0, 0, Link.DIRECTION_DOWN, false);
        session.setEntityMusicActive(false);
        session.tickEntitiesWithProjectileEvents(
            frame++, 0x50, 0x60, 0, 0, Link.DIRECTION_DOWN, false);
        assertEquals(0x20, session.consumePendingMusicTrack());
        int ticksToWarpJingle = 0;
        boolean heardWarpJingle = false;
        while (!heardWarpJingle && ticksToWarpJingle < 0x100) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0x50, 0x60, 0, 0, Link.DIRECTION_DOWN, false);
            ticksToWarpJingle++;
            heardWarpJingle = session.consumeEntityEvents().stream().anyMatch(event ->
                event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE
                    && event.soundId() == 0x2B);
        }
        assertTrue(heardWarpJingle);
        assertEquals(0xFF, ticksToWarpJingle);

        int slowWarpTicks = 0;
        while (!transition.isActive() && slowWarpTicks < 0x204) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0x50, 0x60, 0, 0, Link.DIRECTION_DOWN, false);
            slowWarpTicks++;
            if (coordinator.handlePendingInstrumentTransition(link)) {
                break;
            }
        }
        assertTrue(transition.isActive());
        assertTrue(slowWarpTicks >= 0x1FD && slowWarpTicks <= 0x200,
            "source $80 slow countdown completed in " + slowWarpTicks + " ticks");
        while (transition.isActive()) transition.tick();
        assertEquals(Warp.CATEGORY_OVERWORLD, session.mapCategory());
        assertEquals(0xD3, session.currentRoomId());

        walkToAndCrossOverworldBoundary(
            coordinator, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0xD2, session.currentRoomId());
        assertTrue(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x41));
        assertEquals(0x80, session.bowWowState());

        boolean instrumentOwlDialogOpened = false;
        var instrumentOwlDialogs = new java.util.ArrayList<Integer>();
        int instrumentOwlDeadline = frame + 0x100;
        while (!instrumentOwlDialogOpened && frame < instrumentOwlDeadline) {
            tickInteractiveEntities(session, frame++, 0x50, 0x50);
            instrumentOwlDialogs.addAll(session.consumeEntityDialogRequests().stream()
                .map(RoomEntityRuntime.DialogRequest::globalDialogId).toList());
            instrumentOwlDialogOpened |= instrumentOwlDialogs.contains(0x0C2);
        }
        assertTrue(instrumentOwlDialogOpened, instrumentOwlDialogs.toString());
        int instrumentOwlExitDeadline = frame + 0x100;
        while ((session.overworldRoomStatusForTest(0xD2) & 0x20) == 0
            && frame < instrumentOwlExitDeadline) {
            tickInteractiveEntities(session, frame++, 0x50, 0x50);
        }
        assertEquals(0x20, session.overworldRoomStatusForTest(0xD2) & 0x20);

        int[] routeToKidnappingWarning = {
            ScrollController.RIGHT, ScrollController.UP, ScrollController.DOWN,
            ScrollController.RIGHT, ScrollController.RIGHT, ScrollController.UP,
            ScrollController.LEFT, ScrollController.UP, ScrollController.LEFT,
            ScrollController.UP, ScrollController.LEFT, ScrollController.LEFT,
            ScrollController.LEFT, ScrollController.DOWN
        };
        int[] kidnappingWarningRooms = {
            0xD3, 0xC3, 0xD3, 0xD4, 0xD5, 0xC5, 0xC4,
            0xB4, 0xB3, 0xA3, 0xA2, 0xA1, 0xA0, 0xB0
        };
        for (int index = 0; index < routeToKidnappingWarning.length; index++) {
            try {
                walkToAndCrossOverworldBoundary(
                    coordinator, scroll, collision, link, routeToKidnappingWarning[index]);
            } catch (AssertionError error) {
                throw new AssertionError("kidnapping route index " + index
                    + " from room " + Integer.toHexString(session.currentRoomId()), error);
            }
            assertEquals(kidnappingWarningRooms[index], session.currentRoomId());
        }
        RoomEntity warningKid = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x71).findFirst().orElseThrow();
        session.tickEntities(frame++, 0x10, 0x10);
        session.tickEntities(frame++, 0x10, 0x10);
        assertEquals(0x0E, session.consumePendingMusicTrack());
        session.tickEntities(frame++, warningKid.x(), warningKid.y());
        assertEquals(0x220,
            session.consumeEntityDialogRequests().getFirst().globalDialogId());

        int[] routeToMoblinHideout = {
            ScrollController.UP, ScrollController.UP, ScrollController.UP,
            ScrollController.UP, ScrollController.UP, ScrollController.RIGHT,
            ScrollController.RIGHT, ScrollController.UP, ScrollController.UP,
            ScrollController.RIGHT, ScrollController.RIGHT, ScrollController.UP,
            ScrollController.RIGHT
        };
        int[] moblinHideoutRouteRooms = {
            0xA0, 0x90, 0x80, 0x70, 0x60, 0x61, 0x62,
            0x52, 0x42, 0x43, 0x44, 0x34, 0x35
        };
        for (int index = 0; index < routeToMoblinHideout.length; index++) {
            try {
                walkToAndCrossOverworldBoundary(
                    coordinator, scroll, collision, link, routeToMoblinHideout[index]);
            } catch (AssertionError error) {
                throw new AssertionError("hideout route index " + index
                    + " from room " + Integer.toHexString(session.currentRoomId()), error);
            }
            assertEquals(moblinHideoutRouteRooms[index], session.currentRoomId());
        }
        Warp moblinHideoutEntrance = session.activeRoom().warps().stream()
            .filter(warp -> warp.destMap() == 0x15 && warp.destRoom() == 0xF0)
            .findFirst().orElseThrow();
        link.setPixelPosition(
            (moblinHideoutEntrance.tileLocation() & 0x0F) * 16,
            (moblinHideoutEntrance.tileLocation() >>> 4) * 16);
        coordinator.handleWarpAndIndoorBoundaries(link);
        while (transition.isActive()) transition.tick();
        assertEquals(0x15, session.activeRoom().mapId());
        assertEquals(0xF0, session.currentRoomId());

        RoomEntity hideoutGuard = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x14).findFirst().orElseThrow();
        boolean hideoutGuardDialogOpened = false;
        int guardIntroDeadline = frame + 0x100;
        while (!hideoutGuardDialogOpened && frame < guardIntroDeadline) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0x70, hideoutGuard.y(), 0, 0, Link.DIRECTION_LEFT, false);
            hideoutGuardDialogOpened |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x190);
        }
        assertTrue(hideoutGuardDialogOpened);
        for (int hit = 0; hit < 2; hit++) {
            hideoutGuard = session.activeRoom().entities().slots().get(hideoutGuard.slot());
            int hideoutGuardSlot = hideoutGuard.slot();
            List<EntityCombatEvent> guardHit = session.resolveEntityCombat(
                hideoutGuardSlot ^ 1, hideoutGuard.x(), hideoutGuard.y(),
                false, true, true, hideoutGuard.x(), 0x08,
                hideoutGuard.y(), 0x08);
            assertTrue(guardHit.stream().anyMatch(event ->
                event.slot() == hideoutGuardSlot && event.enemyDamage() > 0));
            for (int recovery = 0; recovery < 0x20; recovery++) {
                session.tickEntitiesWithProjectileEvents(
                    frame++, 0x20, 0x70, 0, 0, Link.DIRECTION_RIGHT, false);
            }
        }
        int guardDeadline = frame + 0x100;
        while (session.activeRoomEventForTest() != 0 && frame < guardDeadline) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0x20, 0x70, 0, 0, Link.DIRECTION_RIGHT, false);
        }
        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(0, session.activeRoom().shutterDoorMask() & 0x01);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0xE0, session.currentRoomId());
        List<Integer> hideoutMoblinSlots = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x0B).map(RoomEntity::slot).toList();
        assertEquals(4, hideoutMoblinSlots.size());
        session.tickEntitiesWithProjectileEvents(
            frame++, 0x50, 0x50, 0, 0, Link.DIRECTION_RIGHT, false);
        for (int moblinSlot : hideoutMoblinSlots) {
            for (int hit = 0; hit < 4
                && session.activeRoom().entities().slots().get(moblinSlot).status()
                    == EntityStatus.ACTIVE; hit++) {
                RoomEntity moblin = session.activeRoom().entities().slots().get(moblinSlot);
                List<EntityCombatEvent> moblinHit = session.resolveEntityCombat(
                    moblinSlot ^ 1, moblin.x(), moblin.y(), false, true, true,
                    moblin.x(), 0x08, moblin.y(), 0x08);
                assertTrue(moblinHit.stream().anyMatch(event ->
                    event.slot() == moblinSlot && event.enemyDamage() > 0));
                for (int recovery = 0; recovery < 0x20; recovery++) {
                    session.tickEntitiesWithProjectileEvents(
                        frame++, 0x50, 0x50, 0, 0, Link.DIRECTION_RIGHT, false);
                }
            }
        }
        int moblinRoomDeadline = frame + 0x200;
        while (session.activeRoomEventForTest() != 0 && frame < moblinRoomDeadline) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0x50, 0x50, 0, 0, Link.DIRECTION_RIGHT, false);
        }
        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(0, session.activeRoom().shutterDoorMask() & 0x08);
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0xE1, session.currentRoomId());

        RoomEntity moblinKing = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0xE4).findFirst().orElseThrow();
        boolean moblinKingIntroOpened = false;
        int moblinKingHits = 0;
        int moblinKingDeadline = frame + 0x2000;
        while (session.activeRoom().entities().slots().get(moblinKing.slot()).loaded()
            && frame < moblinKingDeadline) {
            session.tickEntitiesWithProjectileEvents(
                frame++, 0x00, 0x20, 0, 0, Link.DIRECTION_RIGHT, false);
            moblinKingIntroOpened |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x191);
            moblinKing = session.activeRoom().entities().slots().get(moblinKing.slot());
            if (moblinKing.status() != EntityStatus.ACTIVE) {
                continue;
            }

            Sword liveSword = new Sword(romTables, null);
            liveSword.onPress();
            for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
                liveSword.tick(false);
            }
            link.setDirection(Link.DIRECTION_RIGHT);
            Sword.CollisionBox initialSwordBox = liveSword.enemyCollisionBox(
                link.romEntityX(), link.romSwordCollisionY(), link.direction());
            link.setPixelPosition(
                moblinKing.x() - (initialSwordBox.x() - link.pixelX()),
                moblinKing.y() - (initialSwordBox.y() - link.pixelY()));
            Sword.CollisionBox swordBox = liveSword.enemyCollisionBox(
                link.romEntityX(), link.romSwordCollisionY(), link.direction());
            int moblinKingSlot = moblinKing.slot();
            List<EntityCombatEvent> kingHit = session.resolveEntityCombat(
                moblinKingSlot ^ 1, link.romEntityX(), link.romEntityY(),
                false, true, swordBox.active(), swordBox.x(), swordBox.width(),
                swordBox.y(), swordBox.height());
            if (kingHit.stream().anyMatch(event ->
                    event.slot() == moblinKingSlot && event.enemyDamage() > 0)) {
                moblinKingHits++;
            }
        }
        assertTrue(moblinKingIntroOpened);
        assertEquals(8, moblinKingHits);
        assertFalse(session.activeRoom().entities().slots().get(moblinKing.slot()).loaded());
        assertEquals(0x20, session.indoorRoomStatusForTest(0x15, 0xE1) & 0x20);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0xE2, session.currentRoomId());
        RoomEntity kidnappedBowWow = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x6D).findFirst().orElseThrow();
        int bowWowRescueDeadline = frame + 0x40;
        while (session.bowWowState() != 0x01 && frame < bowWowRescueDeadline) {
            session.tickEntitiesWithProjectileEvents(
                frame++, kidnappedBowWow.x(), kidnappedBowWow.y(),
                0, 0, Link.DIRECTION_RIGHT, 1, false, 1, 0,
                false, 0, 0, 0, 0);
        }
        assertEquals(0x01, session.bowWowState());
        assertEquals(0x10, session.consumePendingMusicTrack());
        assertTrue(session.consumeEntityDialogRequests().stream()
            .anyMatch(request -> request.globalDialogId() == 0x16C));

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        session.tickEntitiesWithProjectileEvents(
            frame, link.romEntityX(), link.romEntityY(),
            0, 0, Link.DIRECTION_LEFT, false);
        assertTrue(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x6D));

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0xE0, session.currentRoomId());
        session.tickEntitiesWithProjectileEvents(
            frame++, link.romEntityX(), link.romEntityY(),
            0, 0, Link.DIRECTION_DOWN, false);
        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(0, session.activeRoom().shutterDoorMask() & 0x02);
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.DOWN);
        assertEquals(0xF0, session.currentRoomId());
        Warp hideoutExit = session.activeRoom().warps().stream()
            .filter(warp -> warp.destMap() == 0x00 && warp.destRoom() == 0x35)
            .findFirst().orElseThrow();
        assertEquals(hideoutExit, session.activeRoom().firstWarp());
        assertTrue(session.activeRoom().indoorHasSouthEntrance());
        walkToAndExitIndoorFrontDoor(
            coordinator, transition, collision, link);
        assertEquals(Warp.CATEGORY_OVERWORLD, session.mapCategory());
        assertEquals(0x35, session.currentRoomId());

        walkToAndCrossOverworldBoundary(
            coordinator, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x36, session.currentRoomId());
        assertTrue(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x41));
        boolean swampOwlDialogOpened = false;
        int swampOwlDeadline = frame + 0x100;
        while (!swampOwlDialogOpened && frame < swampOwlDeadline) {
            tickInteractiveEntities(session, frame++, 0x50, 0x50);
            swampOwlDialogOpened |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x0C3);
        }
        assertTrue(swampOwlDialogOpened);
        int swampOwlExitDeadline = frame + 0x100;
        while ((session.overworldRoomStatusForTest(0x36) & 0x20) == 0
            && frame < swampOwlExitDeadline) {
            tickInteractiveEntities(session, frame++, 0x50, 0x50);
        }
        assertEquals(0x20, session.overworldRoomStatusForTest(0x36) & 0x20);
        int swampOwlDepartureDeadline = frame + 0x100;
        while (session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x41)
            && frame < swampOwlDepartureDeadline) {
            tickInteractiveEntities(session, frame++, 0x50, 0x50);
        }
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x41));

        int[] routeToGopongaSwamp = {
            ScrollController.LEFT, ScrollController.UP, ScrollController.DOWN,
            ScrollController.LEFT, ScrollController.LEFT
        };
        int[] gopongaSwampRouteRooms = {0x35, 0x25, 0x35, 0x34, 0x33};
        for (int index = 0; index < routeToGopongaSwamp.length; index++) {
            walkToAndCrossOverworldBoundary(
                coordinator, scroll, collision, link, routeToGopongaSwamp[index]);
            assertEquals(gopongaSwampRouteRooms[index], session.currentRoomId());
        }
        assertEquals(2, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x7C || entity.type() == 0x7E).count());
        frame = letBowWowEatAllGoponga(session, collision, link, frame);

        walkToAndCrossOverworldBoundary(
            coordinator, scroll, collision, link, ScrollController.UP);
        assertEquals(0x23, session.currentRoomId());
        assertEquals(1, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x7C).count());
        frame = letBowWowEatAllGoponga(session, collision, link, frame);

        walkToAndCrossOverworldBoundary(
            coordinator, scroll, collision, link, ScrollController.DOWN);
        assertEquals(0x33, session.currentRoomId());
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x7C || entity.type() == 0x7E));
        walkToAndCrossOverworldBoundary(
            coordinator, scroll, collision, link, ScrollController.UP);
        assertEquals(0x23, session.currentRoomId());
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x7C));

        walkToAndCrossOverworldBoundary(
            coordinator, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x22, session.currentRoomId());
        assertEquals(2, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x7E).count());
        frame = letBowWowEatAllGoponga(session, collision, link, frame);
        walkToAndCrossOverworldBoundary(
            coordinator, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x23, session.currentRoomId());
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x7C));
        walkToAndCrossOverworldBoundary(
            coordinator, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x24, session.currentRoomId());
        assertEquals(5, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x7E).count());
        assertTrue(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x6D));
        frame = letBowWowEatAllGoponga(session, collision, link, frame);

        walkToAndCrossOverworldBoundary(
            coordinator, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x23, session.currentRoomId());
        walkToAndCrossOverworldBoundary(
            coordinator, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x24, session.currentRoomId());
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x7E));
        assertTrue(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x6D));

        Warp bottleGrottoEntrance = session.activeRoom().warps().stream()
            .filter(warp -> warp.destMap() == 0x01 && warp.destRoom() == 0x36)
            .findFirst().orElseThrow();
        assertEquals(0x13, bottleGrottoEntrance.tileLocation());
        List<int[]> entrancePath = reachableWarpPath(
            collision, link.pixelX(), link.pixelY(), bottleGrottoEntrance.tileLocation());
        assertNotNull(entrancePath, "No collision-valid path from room $24's west entry "
            + "to Bottle Grotto warp $13\n" + collisionGrid(collision));
        for (int[] position : entrancePath) {
            link.setPixelPosition(position[0], position[1]);
        }
        assertEquals(0x13, Warp.packTileLocation(link.pixelX(), link.pixelY()));
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(transition.isActive());
        while (transition.isActive()) {
            transition.tick();
        }

        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
        assertEquals(0x01, session.activeRoom().mapId());
        assertEquals(0x36, session.currentRoomId());
        assertEquals(0x48, link.pixelX());
        assertEquals(0x6C, link.pixelY());
        assertEquals(0x3A, session.indoorRoomPositionForSave());
        assertEquals(0x01, session.bowWowState());
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x6D));
        assertEquals(1, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x61).count());
        assertEquals(4, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x2D).count());
        Warp bottleGrottoExit = session.activeRoom().warps().stream()
            .filter(warp -> warp.destMap() == 0x00 && warp.destRoom() == 0x24)
            .findFirst().orElseThrow();
        assertEquals(0x38, bottleGrottoExit.destX());
        assertEquals(0x22, bottleGrottoExit.destY());

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x31, session.currentRoomId());
        assertEquals(0x25, session.activeRoomEventForTest());
        assertEquals(0xAB, objectAt(session, 0x34));
        assertEquals(0xAB, objectAt(session, 0x35));
        assertEquals(0x08, session.activeRoom().shutterDoorMask() & 0x08);
        PowderApproach firstTorchApproach = reachablePowderApproach(
            collision, link.pixelX(), link.pixelY(), 0x34);
        assertNotNull(firstTorchApproach, collisionGrid(collision));
        for (int[] position : firstTorchApproach.path()) {
            link.setPixelPosition(position[0], position[1]);
        }
        assertTrue(session.sprinkleMagicPowder(
            link.romEntityX(), link.romEntityY(), 0, firstTorchApproach.romDirection()));
        assertTrue(playerState.consumeMagicPowder());
        for (int tick = 0; tick < 16; tick++) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        }
        PowderApproach secondTorchApproach = reachablePowderApproach(
            collision, link.pixelX(), link.pixelY(), 0x35);
        assertNotNull(secondTorchApproach, collisionGrid(collision));
        for (int[] position : secondTorchApproach.path()) {
            link.setPixelPosition(position[0], position[1]);
        }
        assertTrue(session.sprinkleMagicPowder(
            link.romEntityX(), link.romEntityY(), 0, secondTorchApproach.romDirection()));
        assertTrue(playerState.consumeMagicPowder());
        for (int tick = 0; tick < 16; tick++) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        }
        assertEquals(2, session.roomTriggerCountForTest());
        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(0, session.activeRoom().shutterDoorMask() & 0x08);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x32, session.currentRoomId());
        assertEquals(0x81, session.activeRoomEventForTest());
        List<Integer> bottleStalfosSlots = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x1A || entity.type() == 0x1E)
            .map(RoomEntity::slot).toList();
        assertEquals(2, bottleStalfosSlots.size());
        session.tickEntitiesWithProjectileEvents(
            frame++, link.romEntityX(), link.romEntityY(),
            0, 0, Link.DIRECTION_RIGHT, false);
        for (int stalfosSlot : bottleStalfosSlots) {
            for (int hit = 0; hit < 8
                && session.activeRoom().entities().slots().get(stalfosSlot).status()
                    == EntityStatus.ACTIVE; hit++) {
                RoomEntity stalfos = session.activeRoom().entities().slots().get(stalfosSlot);
                List<int[]> combatPath = reachableEntityContactPath(
                    collision, link.pixelX(), link.pixelY(), stalfos);
                assertNotNull(combatPath, collisionGrid(collision));
                for (int[] position : combatPath) {
                    link.setPixelPosition(position[0], position[1]);
                }
                session.resolveEntityCombat(
                    stalfosSlot ^ 1, link.romEntityX(), link.romEntityY(), false, true, true,
                    stalfos.x(), 0x08, stalfos.y(), 0x08);
                for (int recovery = 0; recovery < 0x20; recovery++) {
                    session.tickEntitiesWithProjectileEvents(
                        frame++, link.romEntityX(), link.romEntityY(),
                        0, 0, Link.DIRECTION_RIGHT, false);
                }
            }
        }
        int firstBottleKeyDeadline = frame + 0x300;
        while (session.activeRoomEventForTest() != 0 && frame < firstBottleKeyDeadline) {
            session.tickEntitiesWithProjectileEvents(
                frame++, link.romEntityX(), link.romEntityY(),
                0, 0, Link.DIRECTION_RIGHT, false);
        }
        assertEquals(0, session.activeRoomEventForTest(),
            session.activeRoom().entities().loadedEntities().toString());
        RoomEntity firstBottleKey = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x30).findFirst().orElseThrow();
        int bottleKeyLandingDeadline = frame + 0x200;
        while (firstBottleKey.z() != 0 && frame < bottleKeyLandingDeadline) {
            session.tickEntitiesWithProjectileEvents(
                frame++, link.romEntityX(), link.romEntityY(),
                0, 0, Link.DIRECTION_DOWN, false);
            firstBottleKey = session.activeRoom().entities().slots().get(firstBottleKey.slot());
        }
        assertEquals(0, firstBottleKey.z());
        List<int[]> keyPath = reachableEntityContactPath(
            collision, link.pixelX(), link.pixelY(), firstBottleKey);
        assertNotNull(keyPath, collisionGrid(collision));
        for (int[] position : keyPath) {
            link.setPixelPosition(position[0], position[1]);
        }
        int bottleKeyPickupFrame = (firstBottleKey.slot() & 0x01) == 0
            ? frame | 1 : frame & ~1;
        assertNotNull(session.collectEntityIfNeeded(
            bottleKeyPickupFrame, link.romEntityX(), link.romEntityY(),
            false, true, Link.DIRECTION_DOWN, 0));
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(18, playerState.magicPowderCount());

        // Room $33 is the first post-key Bottle Grotto room.  Enter it through
        // the live collision/scroll path so its ROM-authored state is part of
        // the same ordered-play trace.
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x33, session.currentRoomId());
        assertEquals(0x00, session.entitySwitchBlocksStateForTest());
        int room33RaisedBlockLocation = 0x62;
        assertEquals(0xDC, objectAt(session, room33RaisedBlockLocation));
        assertTrue(session.linkCollisionPointBlockedForTest(
            (room33RaisedBlockLocation & 0x0F) << 4, room33RaisedBlockLocation & 0xF0),
            "room $33 raised $DC must block before the switch animation at "
                + String.format("%02X", room33RaisedBlockLocation));
        assertEquals(6, java.util.stream.IntStream.range(0, 6)
            .filter(offset -> objectAt(session, 0x62 + offset) == 0xDC).count());
        assertEquals(0xC0, objectAt(session, 0x23));
        assertEquals(0xC0, objectAt(session, 0x26));
        assertEquals(1, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x66).count());
        assertEquals(1, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x42).count());
        assertEquals(1, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x16).count());
        tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());

        RoomEntity crystalSwitch = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x66).findFirst().orElseThrow();
        List<int[]> switchPath = reachableEntityContactPath(
            collision, link.pixelX(), link.pixelY(), crystalSwitch);
        assertNotNull(switchPath, collisionGrid(collision));
        for (int[] position : switchPath) {
            link.setPixelPosition(position[0], position[1]);
        }
        Sword liveSword = new Sword(romTables, null);
        liveSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            liveSword.tick(false);
        }
        link.setDirection(Link.DIRECTION_RIGHT);
        Sword.CollisionBox initialSwordBox = liveSword.enemyCollisionBox(
            link.romEntityX(), link.romSwordCollisionY(), link.direction());
        link.setPixelPosition(
            crystalSwitch.x() - (initialSwordBox.x() - link.pixelX()),
            crystalSwitch.y() - (initialSwordBox.y() - link.pixelY()));
        Sword.CollisionBox swordBox = liveSword.enemyCollisionBox(
            link.romEntityX(), link.romSwordCollisionY(), link.direction());
        assertTrue(swordBox.active());
        assertTrue(RoomEntityCombatRules.overlapsSword(
            crystalSwitch, swordBox.x(), swordBox.width(), swordBox.y(), swordBox.height()),
            "sword=" + swordBox + " crystal=" + crystalSwitch);
        List<EntityCombatEvent> switchHit = session.resolveEntityCombat(
            frame++, link.romEntityX(), link.romEntityY(), false, true, true,
            swordBox.x(), swordBox.width(), swordBox.y(), swordBox.height());
        assertEquals(EntityStatus.ACTIVE, crystalSwitch.status());
        assertTrue(switchHit.stream().anyMatch(event ->
            event.slot() == crystalSwitch.slot() && event.type() == 0x66
                && event.swordHit()), switchHit.toString());
        tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        assertEquals(0x01, session.switchableObjectAnimationStageForTest());
        assertTrue(session.consumeEntityEvents().stream().anyMatch(event ->
            event.type() == 0x66
                && event.soundChannel() == EntityCombatEvent.SoundChannel.WAVE
                && event.soundId() == 0x0E));

        assertTrue(session.linkCollisionPointBlockedForTest(0x20, 0x60));
        for (int stage = 0; stage < 9; stage++) {
            session.tickGameplayVBlank();
        }
        assertEquals(0x02, session.entitySwitchBlocksStateForTest());
        assertEquals(0x00, session.switchableObjectAnimationStageForTest());
        assertFalse(session.linkCollisionPointBlockedForTest(
            (room33RaisedBlockLocation & 0x0F) << 4, room33RaisedBlockLocation & 0xF0),
            "room $33 raised $DC must pass after the switch animation");

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x34, session.currentRoomId());
        assertEquals(0x81, session.activeRoomEventForTest());
        assertEquals(2, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x8F).count());
        assertEquals(1, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x2E).count());
        assertEquals(0x02, session.entitySwitchBlocksStateForTest());
        tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        assertEquals(2, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x8F).count());
        assertEquals(1, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x2E).count());

        List<Integer> maskedMimicSlots = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x8F)
            .map(RoomEntity::slot).toList();
        int defeatedMaskedMimics = 0;
        for (int maskedMimicSlot : maskedMimicSlots) {
            for (int hit = 0; hit < 2; hit++) {
                RoomEntity maskedMimic = session.activeRoom().entities().slots()
                    .get(maskedMimicSlot);
                assertEquals(EntityStatus.ACTIVE, maskedMimic.status());
                int linkEntityX = maskedMimic.x();
                int linkEntityY = (maskedMimic.y() + 0x20) & 0xFF;
                // The first handler tick mirrors Link's down input. The next
                // sees Link behind that facing and selects vulnerable options $08.
                session.setEntityPressedButtonsMask(0x08);
                tickInteractiveEntities(session, frame++, linkEntityX, linkEntityY);
                tickInteractiveEntities(session, frame++, linkEntityX, linkEntityY);
                maskedMimic = session.activeRoom().entities().slots().get(maskedMimicSlot);
                List<EntityCombatEvent> hitEvents = session.resolveEntityCombat(
                    frame++, linkEntityX, linkEntityY, false, true, true,
                    maskedMimic.x() + 0x08, 0x08,
                    maskedMimic.y() + 0x08, 0x08);
                assertTrue(hitEvents.stream().anyMatch(event ->
                    event.slot() == maskedMimicSlot && event.swordHit()
                        && event.enemyDamage() == 1), hitEvents.toString());
                session.setEntityPressedButtonsMask(0);
                for (int recovery = 0; recovery < 0x30; recovery++) {
                    session.tickEntitiesWithProjectileEvents(
                        frame++, link.romEntityX(), link.romEntityY(), 0, 0,
                        link.direction(), false);
                }
            }
            int mimicDeathDeadline = frame + 0x200;
            while (session.activeRoom().entities().slots().get(maskedMimicSlot).status()
                    != EntityStatus.DISABLED && frame < mimicDeathDeadline) {
                tickInteractiveEntities(
                    session, frame++, link.romEntityX(), link.romEntityY());
            }
            assertEquals(EntityStatus.DISABLED,
                session.activeRoom().entities().slots().get(maskedMimicSlot).status());
            defeatedMaskedMimics++;
            if (defeatedMaskedMimics == 1) {
                assertEquals(0x81, session.activeRoomEventForTest());
                assertEquals(0, session.activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x30).count());
            }
        }
        session.setEntityPressedButtonsMask(0);
        int mimicEventDeadline = frame + 0x300;
        while (session.activeRoomEventForTest() != 0 && frame < mimicEventDeadline) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        }
        assertEquals(0, session.activeRoomEventForTest());
        assertTrue(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x2E));
        RoomEntity secondBottleKey = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x30).findFirst().orElseThrow();
        int secondKeyLandingDeadline = frame + 0x200;
        while (secondBottleKey.z() != 0 && frame < secondKeyLandingDeadline) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            secondBottleKey = session.activeRoom().entities().slots().get(secondBottleKey.slot());
        }
        assertEquals(0, secondBottleKey.z());
        List<int[]> secondKeyPath = reachableEntityContactPath(
            collision, link.pixelX(), link.pixelY(), secondBottleKey);
        assertNotNull(secondKeyPath, collisionGrid(collision));
        for (int[] position : secondKeyPath) {
            link.setPixelPosition(position[0], position[1]);
        }
        int secondKeyPickupFrame = (secondBottleKey.slot() & 0x01) == 0
            ? frame | 1 : frame & ~1;
        assertNotNull(session.collectEntityIfNeeded(
            secondKeyPickupFrame, link.romEntityX(), link.romEntityY(),
            false, true, Link.DIRECTION_DOWN, 0));
        assertEquals(2, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(0x10, session.indoorRoomStatusForTest(0x01, 0x34) & 0x10);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x33, session.currentRoomId());
        tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        RoomEntity returnCrystalSwitch = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x66).findFirst().orElseThrow();
        List<int[]> returnSwitchPath = reachableEntityContactPath(
            collision, link.pixelX(), link.pixelY(), returnCrystalSwitch);
        assertNotNull(returnSwitchPath, collisionGrid(collision));
        for (int[] position : returnSwitchPath) {
            link.setPixelPosition(position[0], position[1]);
        }
        Sword returnSwitchSword = new Sword(romTables, null);
        returnSwitchSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            returnSwitchSword.tick(false);
        }
        link.setDirection(Link.DIRECTION_RIGHT);
        Sword.CollisionBox initialReturnSwordBox = returnSwitchSword.enemyCollisionBox(
            link.romEntityX(), link.romSwordCollisionY(), link.direction());
        link.setPixelPosition(
            returnCrystalSwitch.x() - (initialReturnSwordBox.x() - link.pixelX()),
            returnCrystalSwitch.y() - (initialReturnSwordBox.y() - link.pixelY()));
        Sword.CollisionBox returnSwordBox = returnSwitchSword.enemyCollisionBox(
            link.romEntityX(), link.romSwordCollisionY(), link.direction());
        assertTrue(returnSwordBox.active());
        List<EntityCombatEvent> returnSwitchHit = session.resolveEntityCombat(
            frame++, link.romEntityX(), link.romEntityY(), false, true, true,
            returnSwordBox.x(), returnSwordBox.width(),
            returnSwordBox.y(), returnSwordBox.height());
        RoomEntity currentReturnCrystalSwitch = session.activeRoom().entities().slots()
            .get(returnCrystalSwitch.slot());
        assertEquals(EntityStatus.ACTIVE, currentReturnCrystalSwitch.status());
        assertTrue(returnSwitchHit.stream().anyMatch(event ->
            event.slot() == returnCrystalSwitch.slot() && event.type() == 0x66
                && event.swordHit()), returnSwitchHit.toString());
        tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        assertEquals(0x01, session.switchableObjectAnimationStageForTest());
        assertTrue(session.consumeEntityEvents().stream().anyMatch(event ->
            event.type() == 0x66
                && event.soundChannel() == EntityCombatEvent.SoundChannel.WAVE
                && event.soundId() == 0x0E));
        for (int stage = 0; stage < 9; stage++) {
            session.tickGameplayVBlank();
        }
        assertEquals(0x00, session.entitySwitchBlocksStateForTest());
        assertEquals(0x00, session.switchableObjectAnimationStageForTest());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x32, session.currentRoomId());
        assertEquals(2, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);
        int bottomKeyDoorLocation = -1;
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW - 1; column++) {
                int location = (row << 4) | column;
                if (objectAt(session, location) == 0x2F
                    && objectAt(session, location + 1) == 0x30) {
                    bottomKeyDoorLocation = location;
                }
            }
        }
        assertEquals(0x74, bottomKeyDoorLocation);

        List<int[]> bottomDoorPath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(),
            (x, y) -> collision.objectPhysicsFlagAtPoint(x + 0x06, y + 1 + 0x0F) == 0x91
                || collision.objectPhysicsFlagAtPoint(x + 0x09, y + 1 + 0x0F) == 0x91);
        assertNotNull(bottomDoorPath, collisionGrid(collision));
        for (int[] position : bottomDoorPath) {
            link.setPixelPosition(position[0], position[1]);
        }
        int bottomDoorContactX = link.pixelX();
        int bottomDoorContactY = link.pixelY();
        link.setPixelPosition(bottomDoorContactX, bottomDoorContactY + 1);
        assertEquals(bottomDoorContactX, link.pixelX());
        assertEquals(bottomDoorContactY + 1, link.pixelY());
        assertTrue(collision.objectPhysicsFlagAtPoint(
            link.pixelX() + 0x06, link.pixelY() + 0x0F) == 0x91
            || collision.objectPhysicsFlagAtPoint(
                link.pixelX() + 0x09, link.pixelY() + 0x0F) == 0x91);
        assertTrue(session.tryUnlockIndoorKeyDoor(
            link.pixelX(), link.pixelY(), Link.DIRECTION_DOWN, 0x02));
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            session.tickEntities(frame++, link.pixelX(), link.pixelY());
            assertTrue(session.consumeWorldLinkMotionBlockRequest());
        }
        session.tickEntities(frame++, link.pixelX(), link.pixelY());
        assertFalse(session.consumeWorldLinkMotionBlockRequest());
        assertEquals(0x8C, objectAt(session, bottomKeyDoorLocation));
        assertEquals(0x08, objectAt(session, bottomKeyDoorLocation + 1));
        assertEquals(0x08, session.indoorRoomStatusForTest(0x01, 0x32) & 0x08);
        assertEquals(0x04, session.indoorRoomStatusForTest(0x01, 0x37) & 0x04);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.DOWN);
        assertEquals(0x37, session.currentRoomId());
        assertEquals(0x61, session.activeRoomEventForTest());
        assertEquals(1, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x8F).count());
        List<Integer> room37RupeeSlots = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x2E)
            .map(RoomEntity::slot)
            .toList();
        assertEquals(2, room37RupeeSlots.size());
        tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());

        RoomEntity maskedMimic = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x8F).findFirst().orElseThrow();
        int maskedMimicSlot = maskedMimic.slot();
        for (int hit = 0; hit < 2; hit++) {
            maskedMimic = session.activeRoom().entities().slots().get(maskedMimicSlot);
            assertEquals(EntityStatus.ACTIVE, maskedMimic.status());
            int linkEntityX = maskedMimic.x();
            int linkEntityY = (maskedMimic.y() + 0x20) & 0xFF;
            session.setEntityPressedButtonsMask(0x08);
            tickInteractiveEntities(session, frame++, linkEntityX, linkEntityY);
            tickInteractiveEntities(session, frame++, linkEntityX, linkEntityY);
            maskedMimic = session.activeRoom().entities().slots().get(maskedMimicSlot);
            List<EntityCombatEvent> hitEvents = session.resolveEntityCombat(
                frame++, linkEntityX, linkEntityY, false, true, true,
                maskedMimic.x() + 0x08, 0x08,
                maskedMimic.y() + 0x08, 0x08);
            assertTrue(hitEvents.stream().anyMatch(event ->
                event.slot() == maskedMimicSlot && event.swordHit()
                    && event.enemyDamage() == 1), hitEvents.toString());
            if (hit == 1) {
                assertEquals(0x61, session.activeRoomEventForTest());
                assertEquals(2, session.activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x2E).count());
            }
            session.setEntityPressedButtonsMask(0);
            for (int recovery = 0; recovery < 0x30; recovery++) {
                session.tickEntitiesWithProjectileEvents(
                    frame++, link.romEntityX(), link.romEntityY(), 0, 0,
                    link.direction(), false);
            }
        }
        int mimicDeathDeadline = frame + 0x200;
        while (session.activeRoom().entities().slots().get(maskedMimicSlot).status()
                != EntityStatus.DISABLED && frame < mimicDeathDeadline) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        }
        assertEquals(EntityStatus.DISABLED,
            session.activeRoom().entities().slots().get(maskedMimicSlot).status());

        int room37EventDeadline = frame + 0x300;
        while (session.activeRoomEventForTest() != 0 && frame < room37EventDeadline) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        }
        assertEquals(0, session.activeRoomEventForTest());
        int compassChestIndex = ROOM_OBJECTS_BASE + 0x28;
        int chestRevealDeadline = frame + 0x40;
        while (session.activeRoom().roomObjectsArea()[compassChestIndex] != 0xA0
            && frame < chestRevealDeadline) {
            session.tickEntities(frame++, link.romEntityX(), link.romEntityY());
        }
        assertEquals(0xA0, session.activeRoom().roomObjectsArea()[compassChestIndex]);
        assertEquals(2, room37RupeeSlots.stream()
            .filter(slot -> {
                RoomEntity entity = session.activeRoom().entities().slots().get(slot);
                return entity.loaded() && entity.type() == 0x2E;
            }).count());
        assertEquals(0, session.indoorRoomStatusForTest(0x01, 0x37) & 0x10);
        List<int[]> compassChestPath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(),
            (x, y) -> Math.floorDiv(x + 0x08, 0x10) == (0x28 & 0x0F)
                && Math.floorDiv(y - 0x02, 0x10) == ((0x28 & 0xF0) >>> 4));
        assertNotNull(compassChestPath, collisionGrid(collision));
        for (int[] position : compassChestPath) {
            link.setPixelPosition(position[0], position[1]);
        }
        int compassChestContactX = link.pixelX();
        int compassChestContactY = link.pixelY();
        link.setPixelPosition(compassChestContactX, compassChestContactY - 1);
        assertEquals(compassChestContactX, link.pixelX());
        assertEquals(compassChestContactY - 1, link.pixelY());
        int compassChestInteractionLocation =
            (Math.floorDiv(link.pixelY() - 0x02, 0x10) << 4)
                | Math.floorDiv(link.pixelX() + 0x08, 0x10);
        assertEquals(0x28, compassChestInteractionLocation);
        RoomSession.ChestOpenResult compassChest = session.tryOpenChest(
            link.pixelX(), link.pixelY(),
            Link.DIRECTION_UP, true, playerState.swordLevel());
        assertTrue(compassChest.opened());
        assertEquals(ChestContentsTable.CHEST_COMPASS, compassChest.itemType());
        assertEquals(0x28, compassChest.location());
        assertEquals(0x10, session.indoorRoomStatusForTest(0x01, 0x37) & 0x10);
        int compassChestSlot = compassChest.entitySlot();
        boolean compassRewardObserved = false;
        boolean compassDialogObserved = false;
        boolean compassChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            session.tickEntities(frame++, link.romEntityX(), link.romEntityY());
            for (RoomEntityRuntime.ChestRewardEvent reward
                    : session.consumeChestRewardEvents()) {
                compassRewardObserved |= reward.itemType() == ChestContentsTable.CHEST_COMPASS;
                playerState.applyChestReward(reward.itemType());
            }
            compassDialogObserved |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x0A7);
            RoomEntity chestEntity = session.activeRoom().entities().slots().get(compassChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                compassChestEnded = true;
                break;
            }
        }
        assertTrue(compassRewardObserved);
        assertTrue(compassDialogObserved);
        assertTrue(compassChestEnded);
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.COMPASS_INDEX]);
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);

        // MapLayout1 adjacency is not sufficient: room $37's east and west
        // edges are closed. Return north and follow room $32's west opening.
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x32, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x31, session.currentRoomId());
        // The right shutter was opened by room $31's torch event. Reloading
        // must preserve both the source room's right-door bit and room $32's
        // adjacent left-door bit, as ConfigureRoomObjects does in the ROM.
        assertEquals(0x01, session.indoorRoomStatusForTest(0x01, 0x31) & 0x01);
        assertEquals(0x02, session.indoorRoomStatusForTest(0x01, 0x32) & 0x02);
        assertEquals(0x00, session.activeRoom().shutterDoorMask());
        assertEquals(0x0B, objectAt(session, 0x39));
        assertEquals(0x0C, objectAt(session, 0x49));
        // Room $31's west key door is the last Small Key gate in this route.
        assertEquals(0x31, objectAt(session, 0x30));
        assertEquals(0x32, objectAt(session, 0x40));
        List<int[]> westDoorPath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(),
            (x, y) -> collision.objectPhysicsFlagAtPoint(x + 0x03, y + 0x09) == 0x92
                || collision.objectPhysicsFlagAtPoint(x + 0x03, y + 0x0C) == 0x92);
        assertNotNull(westDoorPath, collisionGrid(collision));
        for (int[] position : westDoorPath) {
            link.setPixelPosition(position[0], position[1]);
        }
        int westDoorContactX = link.pixelX();
        int westDoorContactY = link.pixelY();
        link.setPixelPosition(westDoorContactX - 1, westDoorContactY);
        assertEquals(westDoorContactX - 1, link.pixelX());
        assertEquals(westDoorContactY, link.pixelY());
        assertTrue(collision.objectPhysicsFlagAtPoint(
            link.pixelX() + 0x04, link.pixelY() + 0x09) == 0x92
            || collision.objectPhysicsFlagAtPoint(
                link.pixelX() + 0x04, link.pixelY() + 0x0C) == 0x92);
        assertTrue(session.tryUnlockIndoorKeyDoor(
            link.pixelX(), link.pixelY(), Link.DIRECTION_LEFT, 0x04));
        assertEquals(0, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            session.tickEntities(frame++, link.pixelX(), link.pixelY());
            assertTrue(session.consumeWorldLinkMotionBlockRequest());
        }
        session.tickEntities(frame++, link.pixelX(), link.pixelY());
        assertFalse(session.consumeWorldLinkMotionBlockRequest());
        assertEquals(0x09, objectAt(session, 0x30));
        assertEquals(0x0A, objectAt(session, 0x40));
        assertEquals(0x02, session.indoorRoomStatusForTest(0x01, 0x31) & 0x02);
        assertEquals(0x01, session.indoorRoomStatusForTest(0x01, 0x30) & 0x01);
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x30, session.currentRoomId());

        assertEquals(0x21, session.activeRoomEventForTest());
        assertEquals(4, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x27).count());
        assertEquals(2, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x19).count());
        assertTrue((session.activeRoom().shutterDoorMask() & 0x01) != 0);
        List<Integer> room30KeeseSlots = session.activeRoom().entities().loadedEntities()
            .stream().filter(entity -> entity.type() == 0x19)
            .map(RoomEntity::slot).toList();
        for (int keeseSlot : room30KeeseSlots) {
            RoomEntity keese = session.activeRoom().entities().slots().get(keeseSlot);
            List<int[]> keesePath = reachableEntityContactPath(
                collision, link.pixelX(), link.pixelY(), keese);
            assertNotNull(keesePath, collisionGrid(collision));
            for (int[] position : keesePath) {
                link.setPixelPosition(position[0], position[1]);
            }
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            keese = session.activeRoom().entities().slots().get(keeseSlot);
            int keeseDeltaX = signedByteDelta(keese.x(), link.romEntityX());
            int keeseDeltaY = signedByteDelta(keese.y(), link.romEntityY());
            link.setDirection(Math.abs(keeseDeltaX) >= Math.abs(keeseDeltaY)
                ? keeseDeltaX < 0 ? Link.DIRECTION_LEFT : Link.DIRECTION_RIGHT
                : keeseDeltaY < 0 ? Link.DIRECTION_UP : Link.DIRECTION_DOWN);
            Sword keeseSword = new Sword(romTables, null);
            keeseSword.onPress();
            for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
                keeseSword.tick(false);
            }
            Sword.CollisionBox keeseSwordBox = keeseSword.enemyCollisionBox(
                link.romEntityX(), link.romSwordCollisionY(), link.direction());
            assertTrue(RoomEntityCombatRules.overlapsSword(
                keese, keeseSwordBox.x(), keeseSwordBox.width(),
                keeseSwordBox.y(), keeseSwordBox.height()),
                "sword=" + keeseSwordBox + " keese=" + keese);
            List<EntityCombatEvent> keeseHit = session.resolveEntityCombat(
                frame++, link.romEntityX(), link.romEntityY(), false, true, true,
                keeseSwordBox.x(), keeseSwordBox.width(),
                keeseSwordBox.y(), keeseSwordBox.height());
            assertTrue(keeseHit.stream().anyMatch(event ->
                event.slot() == keeseSlot && event.swordHit() && event.enemyDamage() > 0),
                keeseHit.toString());
            for (int recovery = 0; recovery < 0x30; recovery++) {
                tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            }
            int keeseDeathDeadline = frame + 0x200;
            while (session.activeRoom().entities().slots().get(keeseSlot).status()
                    != EntityStatus.DISABLED && frame < keeseDeathDeadline) {
                tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            }
            assertEquals(EntityStatus.DISABLED,
                session.activeRoom().entities().slots().get(keeseSlot).status());
            assertEquals(4, session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x27).count());
            if (session.activeRoom().entities().loadedEntities().stream()
                    .anyMatch(entity -> entity.type() == 0x19)) {
                assertEquals(0x21, session.activeRoomEventForTest());
            }
        }
        session.setEntityPressedButtonsMask(0);
        int room30EventDeadline = frame + 0x300;
        while (session.activeRoomEventForTest() != 0 && frame < room30EventDeadline) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        }
        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(0, session.activeRoom().shutterDoorMask() & 0x01);
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x2E, session.currentRoomId());
        assertEquals(0x21, session.activeRoomEventForTest());
        assertEquals(1, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x20).count());
        assertEquals(0x0F, objectAt(session, 0x13));
        assertEquals(0x0F, objectAt(session, 0x23));
        assertEquals(0x0F, objectAt(session, 0x33));
        assertEquals(0xA0, objectAt(session, 0x24));

        RoomEntity hardhat = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x20).findFirst().orElseThrow();
        tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        hardhat = session.activeRoom().entities().slots().get(hardhat.slot());
        assertEquals(EntityStatus.ACTIVE, hardhat.status());
        assertEquals(0x21, session.activeRoomEventForTest());

        List<int[]> stoneBeakFeatherPath = reachablePositionPathWithFeather(
            collision, romTables, link.pixelX(), link.pixelY(),
            (x, y) -> Math.floorDiv(x + 0x08, 0x10) == (0x24 & 0x0F)
                && Math.floorDiv(y - 0x02, 0x10) == ((0x24 & 0xF0) >>> 4));
        assertNotNull(stoneBeakFeatherPath,
            "No Feather-valid path across room $2E's trench\n" + collisionGrid(collision));
        assertTrue(walkCollisionPathWithFeather(
            coordinator, transition, scroll, collision, link, inputState, inputConfig,
            itemRegistry, playerState, stoneBeakFeatherPath) > 0);

        int stoneBeakChestContactX = link.pixelX();
        int stoneBeakChestContactY = link.pixelY();
        link.setPixelPosition(stoneBeakChestContactX, stoneBeakChestContactY - 1);
        int stoneBeakChestInteractionLocation =
            (Math.floorDiv(link.pixelY() - 0x02, 0x10) << 4)
                | Math.floorDiv(link.pixelX() + 0x08, 0x10);
        assertEquals(0x24, stoneBeakChestInteractionLocation);
        RoomSession.ChestOpenResult stoneBeakChest = session.tryOpenChest(
            link.pixelX(), link.pixelY(), Link.DIRECTION_UP, true, playerState.swordLevel());
        assertTrue(stoneBeakChest.opened());
        assertEquals(ChestContentsTable.CHEST_STONE_BEAK, stoneBeakChest.itemType());
        assertEquals(0x24, stoneBeakChest.location());
        assertEquals(0x10, session.indoorRoomStatusForTest(0x01, 0x2E) & 0x10);
        int stoneBeakChestSlot = stoneBeakChest.entitySlot();
        int stoneBeakDialogId = new ChestContentsTable(rom).dialogLowIdFor(
            ChestContentsTable.CHEST_STONE_BEAK, playerState.shieldLevel(),
            playerState.swordLevel(), playerState.powerBraceletLevel(), 0x01, 0x2E);
        boolean stoneBeakRewardObserved = false;
        boolean stoneBeakDialogObserved = false;
        boolean stoneBeakChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            session.tickEntities(frame++, link.romEntityX(), link.romEntityY());
            for (RoomEntityRuntime.ChestRewardEvent reward
                    : session.consumeChestRewardEvents()) {
                stoneBeakRewardObserved |= reward.itemType() == ChestContentsTable.CHEST_STONE_BEAK;
                playerState.applyChestReward(reward.itemType());
            }
            stoneBeakDialogObserved |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == stoneBeakDialogId);
            RoomEntity chestEntity = session.activeRoom().entities().slots()
                .get(stoneBeakChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                stoneBeakChestEnded = true;
                break;
            }
        }
        assertTrue(stoneBeakRewardObserved);
        assertTrue(stoneBeakDialogObserved);
        assertTrue(stoneBeakChestEnded);
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.STONE_BEAK_INDEX]);
        assertEquals(0, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);

        // The post-Stone-Beak route backtracks south through room $30, then
        // east across the cleared $31/$32 shutters to the live switch in $33.
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.DOWN);
        assertEquals(0x30, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x31, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x32, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x33, session.currentRoomId());
        assertEquals(0x00, session.entitySwitchBlocksStateForTest());
        tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());

        RoomEntity postStoneCrystalSwitch = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x66).findFirst().orElseThrow();
        List<int[]> postStoneSwitchPath = reachableEntityContactPath(
            collision, link.pixelX(), link.pixelY(), postStoneCrystalSwitch);
        assertNotNull(postStoneSwitchPath, collisionGrid(collision));
        for (int[] position : postStoneSwitchPath) {
            link.setPixelPosition(position[0], position[1]);
        }
        Sword postStoneSword = new Sword(romTables, null);
        postStoneSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            postStoneSword.tick(false);
        }
        link.setDirection(Link.DIRECTION_RIGHT);
        Sword.CollisionBox postStoneInitialSwordBox = postStoneSword.enemyCollisionBox(
            link.romEntityX(), link.romSwordCollisionY(), link.direction());
        link.setPixelPosition(
            postStoneCrystalSwitch.x() - (postStoneInitialSwordBox.x() - link.pixelX()),
            postStoneCrystalSwitch.y() - (postStoneInitialSwordBox.y() - link.pixelY()));
        Sword.CollisionBox postStoneSwordBox = postStoneSword.enemyCollisionBox(
            link.romEntityX(), link.romSwordCollisionY(), link.direction());
        assertTrue(postStoneSwordBox.active());
        assertTrue(RoomEntityCombatRules.overlapsSword(
            postStoneCrystalSwitch, postStoneSwordBox.x(), postStoneSwordBox.width(),
            postStoneSwordBox.y(), postStoneSwordBox.height()));
        List<EntityCombatEvent> postStoneSwitchHit = session.resolveEntityCombat(
            frame++, link.romEntityX(), link.romEntityY(), false, true, true,
            postStoneSwordBox.x(), postStoneSwordBox.width(),
            postStoneSwordBox.y(), postStoneSwordBox.height());
        int postStoneCrystalSwitchSlot = postStoneCrystalSwitch.slot();
        assertTrue(postStoneSwitchHit.stream().anyMatch(event ->
            event.slot() == postStoneCrystalSwitchSlot && event.type() == 0x66
                && event.swordHit()), "events=" + postStoneSwitchHit
                    + " switch=" + postStoneCrystalSwitch
                    + " sword=" + postStoneSwordBox + " frame=" + frame);
        tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        for (int stage = 0; stage < 9; stage++) {
            session.tickGameplayVBlank();
        }
        assertEquals(0x02, session.entitySwitchBlocksStateForTest());

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.DOWN);
        assertEquals(0x38, session.currentRoomId());
        assertEquals(0x00, session.activeRoomEventForTest());
        assertEquals(0xA0, objectAt(session, 0x43));
        assertEquals(1, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x14).count());
        RoomEntity room38MoblinSword = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x14).findFirst().orElseThrow();
        assertEquals(0x28, room38MoblinSword.x());
        assertEquals(0x70, room38MoblinSword.y());
        RoomEntity room38CrystalSwitch = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x66).findFirst().orElseThrow();
        assertEquals(0x58, room38CrystalSwitch.x());
        assertEquals(0x50, room38CrystalSwitch.y());
        int room38LoweredBlockLocation = firstObjectLocation(session, 0xDB);
        assertTrue(room38LoweredBlockLocation >= 0);
        assertTrue(session.linkCollisionPointBlockedForTest(
            (room38LoweredBlockLocation & 0x0F) << 4, room38LoweredBlockLocation & 0xF0),
            "room $38 lowered $DB must block while global state is $02");

        // The raised $DB ring encloses room $38's own source switch.  A
        // Feather jump cannot cross a mismatched switch block (LinkTest
        // covers the source behavior), so use the source-valid sword range
        // from the reachable side of the ring instead of teleporting Link.
        SwordApproach room38SwitchApproach = reachableSwordContactPath(
            collision, link.pixelX(), link.pixelY(), room38CrystalSwitch, romTables);
        assertNotNull(room38SwitchApproach, collisionGrid(collision));
        for (int[] position : room38SwitchApproach.path()) {
            link.setPixelPosition(position[0], position[1]);
        }
        assertTrue(collision.objectUnderLinkFeet(link.pixelX(), link.pixelY()) != 0xDB,
            "sword approach must remain off the raised $DB ring");
        link.setDirection(room38SwitchApproach.direction());
        for (int tick = 0; tick < 0x10; tick++) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        }
        Sword postStoneLoweringSword = new Sword(romTables, null);
        postStoneLoweringSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            postStoneLoweringSword.tick(false);
        }
        Sword.CollisionBox postStoneLoweringBox = postStoneLoweringSword.enemyCollisionBox(
            link.romEntityX(), link.romSwordCollisionY(), link.direction());
        assertTrue(RoomEntityCombatRules.overlapsSword(
            room38CrystalSwitch, postStoneLoweringBox.x(), postStoneLoweringBox.width(),
            postStoneLoweringBox.y(), postStoneLoweringBox.height()));
        List<EntityCombatEvent> postStoneLoweringHit = session.resolveEntityCombat(
            frame++, link.romEntityX(), link.romEntityY(), false, true, true,
            postStoneLoweringBox.x(), postStoneLoweringBox.width(),
            postStoneLoweringBox.y(), postStoneLoweringBox.height());
        int postStoneLoweringSwitchSlot = room38CrystalSwitch.slot();
        assertTrue(postStoneLoweringHit.stream().anyMatch(event ->
            event.slot() == postStoneLoweringSwitchSlot && event.type() == 0x66
                && event.swordHit()), postStoneLoweringHit.toString());
        tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        for (int stage = 0; stage < 9; stage++) {
            session.tickGameplayVBlank();
        }
        assertEquals(0x00, session.entitySwitchBlocksStateForTest());
        assertFalse(session.linkCollisionPointBlockedForTest(
            (room38LoweredBlockLocation & 0x0F) << 4, room38LoweredBlockLocation & 0xF0),
            "room $38 lowered $DB must pass after the switch animation");

        List<int[]> room38ChestPath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(),
            (x, y) -> Math.floorDiv(x + 0x08, 0x10) == (0x43 & 0x0F)
                && Math.floorDiv(y - 0x01, 0x10) == ((0x43 & 0xF0) >>> 4));
        assertNotNull(room38ChestPath, collisionGrid(collision));
        for (int[] position : room38ChestPath) {
            link.setPixelPosition(position[0], position[1]);
        }
        link.setDirection(Link.DIRECTION_UP);
        RoomSession.ChestOpenResult room38Chest = session.tryOpenChest(
            link.pixelX(), link.pixelY(), Link.DIRECTION_UP, true, playerState.swordLevel());
        assertTrue(room38Chest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, room38Chest.itemType());
        assertEquals(0x43, room38Chest.location());
        assertEquals(0x10, session.indoorRoomStatusForTest(0x01, 0x38) & 0x10);
        int room38ChestSlot = room38Chest.entitySlot();
        int room38DialogId = new ChestContentsTable(rom).dialogLowIdFor(
            ChestContentsTable.CHEST_SMALL_KEY, playerState.shieldLevel(),
            playerState.swordLevel(), playerState.powerBraceletLevel(), 0x01, 0x38);
        boolean room38RewardObserved = false;
        boolean room38DialogObserved = false;
        boolean room38ChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            session.tickEntities(frame++, link.romEntityX(), link.romEntityY());
            for (RoomEntityRuntime.ChestRewardEvent reward
                    : session.consumeChestRewardEvents()) {
                room38RewardObserved |= reward.itemType() == ChestContentsTable.CHEST_SMALL_KEY;
                playerState.applyChestReward(reward.itemType());
            }
            room38DialogObserved |= session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == room38DialogId);
            RoomEntity chestEntity = session.activeRoom().entities().slots().get(room38ChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                room38ChestEnded = true;
                break;
            }
        }
        assertTrue(room38RewardObserved);
        assertTrue(room38DialogObserved);
        assertTrue(room38ChestEnded);
        assertEquals(0xA1, objectAt(session, 0x43));
        assertEquals(0x10, session.indoorRoomStatusForTest(0x01, 0x38) & 0x10);
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x39, session.currentRoomId());
        assertEquals(0x63, session.activeRoomEventForTest());

        List<int[]> room39ButtonPath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(),
            (x, y) -> collision.groundInteractionSample(x + 0x08, y + 0x10)
                .objectId() == 0xAA);
        assertNotNull(room39ButtonPath, collisionGrid(collision));
        for (int[] position : room39ButtonPath) {
            link.setPixelPosition(position[0], position[1]);
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        }
        int room39EventDeadline = frame + 0x40;
        while (session.activeRoomEventForTest() != 0 && frame < room39EventDeadline) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        }
        assertEquals(0, session.activeRoomEventForTest());
        int room39ChestDeadline = frame + 0x20;
        while (objectAt(session, 0x28) != 0xA0 && frame < room39ChestDeadline) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        }
        assertEquals(0xA0, objectAt(session, 0x28));
        List<int[]> room39ChestPath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(),
            (x, y) -> Math.floorDiv(x + 0x08, 0x10) == 0x08
                && Math.floorDiv(y - 0x01, 0x10) == 0x02);
        assertNotNull(room39ChestPath, collisionGrid(collision));
        for (int[] position : room39ChestPath) {
            link.setPixelPosition(position[0], position[1]);
        }
        link.setDirection(Link.DIRECTION_UP);
        RoomSession.ChestOpenResult room39Chest = session.tryOpenChest(
            link.pixelX(), link.pixelY(), Link.DIRECTION_UP, true, playerState.swordLevel());
        assertTrue(room39Chest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, room39Chest.itemType());
        int room39ChestSlot = room39Chest.entitySlot();
        boolean room39RewardObserved = false;
        boolean room39ChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            session.tickEntities(frame++, link.romEntityX(), link.romEntityY());
            for (RoomEntityRuntime.ChestRewardEvent reward
                    : session.consumeChestRewardEvents()) {
                room39RewardObserved |= reward.itemType() == ChestContentsTable.CHEST_SMALL_KEY;
                playerState.applyChestReward(reward.itemType());
            }
            RoomEntity chestEntity = session.activeRoom().entities().slots()
                .get(room39ChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                room39ChestEnded = true;
                break;
            }
        }
        assertTrue(room39RewardObserved);
        assertTrue(room39ChestEnded);
        assertEquals(2, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x38, session.currentRoomId());
        assertEquals(0xA1, objectAt(session, 0x43));
        assertEquals(0x10, session.indoorRoomStatusForTest(0x01, 0x38) & 0x10);
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x39, session.currentRoomId());
        assertEquals(0x63, session.activeRoomEventForTest());
        assertEquals(0xA1, objectAt(session, 0x28));
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x34, session.currentRoomId());
        assertTrue(link.pixelX() > 0x30,
            "room $34 entry must land east of its ROM $A6 partition: x="
                + String.format("%02X", link.pixelX()));
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.RIGHT);
        assertEquals(0x35, session.currentRoomId());
        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(2, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x2C).count());
        assertEquals(2, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);

        link.setPixelPosition(0x75, 0x05);
        assertTrue(session.tryUnlockIndoorKeyDoor(
            link.pixelX(), link.pixelY(), Link.DIRECTION_UP, 0x01));
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(0x40, session.indoorRoomStatusForTest(0x01, 0x35) & 0x40);
        for (int tick = 0; tick < 8; tick++) {
            session.tickEntities(frame++, link.pixelX(), link.pixelY());
            assertTrue(session.consumeWorldLinkMotionBlockRequest());
        }
        session.tickEntities(frame++, link.pixelX(), link.pixelY());
        assertFalse(session.consumeWorldLinkMotionBlockRequest());
        assertEquals(0x43, objectAt(session, 0x07));
        assertEquals(0x44, objectAt(session, 0x08));
        assertEquals(0x04, session.indoorRoomStatusForTest(0x01, 0x35) & 0x04);
        assertEquals(0x08, session.indoorRoomStatusForTest(0x01, 0x2F) & 0x08);

        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x2F, session.currentRoomId());
        assertEquals(0xA7, session.activeRoomEventForTest());
        assertEquals(0xA7, objectAt(session, 0x33));
        assertEquals(0xA7, objectAt(session, 0x36));
        assertEquals(0x0D, objectAt(session, 0x18));
        assertEquals(2, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x19).count());
        assertEquals(1, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x17).count());
        assertEquals(2, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x2D).count());
        assertEquals(1, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x42).count());

        link.setPixelPosition(0x25, 0x28);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(session.tryInteractWithIndoorBlock(
                link.pixelX(), link.pixelY(), Link.DIRECTION_RIGHT, 0x08));
        }
        assertEquals(0xA7, session.activeRoomEventForTest());
        for (int tick = 0; tick < 33; tick++) {
            session.tickEntities(frame++, link.romEntityX(), link.romEntityY());
        }
        assertEquals(0xA6, objectAt(session, 0x34));
        assertEquals(0xA7, session.activeRoomEventForTest());

        link.setPixelPosition(0x5C, 0x28);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(session.tryInteractWithIndoorBlock(
                link.pixelX(), link.pixelY(), Link.DIRECTION_LEFT, 0x04));
        }
        assertEquals(0xA7, session.activeRoomEventForTest());
        for (int tick = 0; tick < 33; tick++) {
            session.tickEntities(frame++, link.romEntityX(), link.romEntityY());
        }
        assertEquals(0xA6, objectAt(session, 0x34));
        assertEquals(0xA6, objectAt(session, 0x35));
        assertEquals(0xA7, session.activeRoomEventForTest());
        session.tickEntities(frame++, link.romEntityX(), link.romEntityY());
        assertEquals(0, session.activeRoomEventForTest());
        for (int tick = 0; tick < 11; tick++) {
            session.tickEntities(frame++, link.romEntityX(), link.romEntityY());
        }
        assertEquals(0xBE, objectAt(session, 0x18));
        assertEquals(0x10, session.indoorRoomStatusForTest(0x01, 0x2F) & 0x10);
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);

        assertTrue(session.activeRoom().hasWarps());
        List<int[]> staircaseExitPath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(),
            (x, y) -> x <= 0x60 && y == 0x10);
        assertNotNull(staircaseExitPath, collisionGrid(collision));
        for (int[] position : staircaseExitPath) {
            link.setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
        }
        assertFalse(transition.isActive());
        List<int[]> staircaseEntryPath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(),
            (x, y) -> x == 0x80 && y == 0x10);
        assertNotNull(staircaseEntryPath, collisionGrid(collision));
        for (int[] position : staircaseEntryPath) {
            link.setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
            if (transition.isActive()) {
                break;
            }
        }
        assertTrue(transition.isActive());
        while (transition.isActive()) {
            transition.tick();
        }
        assertEquals(Warp.CATEGORY_SIDESCROLL, session.mapCategory());
        assertEquals(0x3F, session.currentRoomId());
        assertEquals(0x88, link.romEntityX());
        assertEquals(0x10, link.romEntityY());
        assertEquals(0x01, link.romPhysicsModifier(),
            "side-view warp initializes hLinkPhysicsModifier=$01");

        List<int[]> sideViewExitPath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(),
            (x, y) -> x == 0);
        assertNotNull(sideViewExitPath, collisionGrid(collision));
        for (int[] position : sideViewExitPath) {
            link.setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
            assertFalse(scroll.isActive());
        }
        link.setPixelPosition(-1, link.pixelY());
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(scroll.isActive());
        assertEquals(ScrollController.LEFT, scroll.direction());
        while (scroll.isActive()) {
            scroll.tick(8);
        }
        assertEquals(Warp.CATEGORY_SIDESCROLL, session.mapCategory());
        assertEquals(0x3E, session.currentRoomId());
        assertEquals(1, session.activeRoom().warps().size());
        Warp sideViewExit = session.activeRoom().firstWarp();
        assertEquals(Warp.CATEGORY_INDOOR, sideViewExit.category());
        assertEquals(0x01, sideViewExit.destMap());
        assertEquals(0x2C, sideViewExit.destRoom());
        assertEquals(0x78, sideViewExit.destX());
        assertEquals(0x70, sideViewExit.destY());
        List<int[]> sideViewReturnPath = reachableBoundaryPath(
            collision, link.pixelX(), link.pixelY(), ScrollController.RIGHT);
        assertNotNull(sideViewReturnPath, collisionGrid(collision));
        for (int[] position : sideViewReturnPath) {
            link.setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
            assertFalse(scroll.isActive());
        }
        link.setPixelPosition(RoomConstants.ROOM_PIXEL_WIDTH, link.pixelY());
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(scroll.isActive());
        assertEquals(ScrollController.RIGHT, scroll.direction());
        while (scroll.isActive()) {
            scroll.tick(8);
        }
        assertEquals(0x3F, session.currentRoomId());
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x3E, session.currentRoomId());
        List<int[]> sideViewWarpPath = reachableBoundaryPath(
            collision, link.pixelX(), link.pixelY(), ScrollController.UP);
        assertNotNull(sideViewWarpPath, collisionGrid(collision));
        for (int[] position : sideViewWarpPath) {
            link.setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
        }
        link.setPixelPosition(link.pixelX(), -5);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(transition.isActive());
        while (transition.isActive()) {
            transition.tick();
        }
        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
        assertEquals(0x2C, session.currentRoomId());
        assertEquals(0x78, link.romEntityX());
        assertEquals(0x70, link.romEntityY());
        assertEquals(0, session.activeRoomEventForTest());
        RoomEntity room2CKeese = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x19).findFirst().orElseThrow();
        assertEquals(0x28, room2CKeese.x());
        assertEquals(0x30, room2CKeese.y());
        RoomEntity room2CSpark = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x16).findFirst().orElseThrow();
        assertEquals(0x18, room2CSpark.x());
        assertEquals(0x30, room2CSpark.y());
        RoomEntity room2CFloatingItem = session.activeRoom().entities().loadedEntities()
            .stream().filter(entity -> entity.type() == 0xE5).findFirst().orElseThrow();
        assertEquals(0x48, room2CFloatingItem.x());
        assertEquals(0x50, room2CFloatingItem.y());

        // Room-$28's source-authored south entry is the D2 ledge at column 7;
        // columns 4/5 are the solid $45/$46 objects. Reach that X in the
        // preceding room before crossing the shared north boundary.
        List<int[]> room28EntryColumnPath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(), (x, y) -> x == 0x70);
        assertNotNull(room28EntryColumnPath,
            "Room-$2C cannot reach the room-$28 entry column\n"
                + collisionGrid(collision));
        for (int[] position : room28EntryColumnPath) {
            link.setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
            assertFalse(scroll.isActive());
        }
        assertEquals(0x70, link.pixelX(), "entry-column path must end at room-$28 column 7");
        // Cross only after reaching that aligned X; the generic helper would
        // choose the first reachable top-edge column and lose this alignment.
        link.setPixelPosition(link.pixelX(), -1);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(scroll.isActive());
        assertEquals(ScrollController.UP, scroll.direction());
        assertEquals(0x70, link.pixelX());
        assertEquals(RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE, link.pixelY());
        while (scroll.isActive()) {
            scroll.tick(8);
        }
        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
        assertEquals(0x28, session.currentRoomId());
        assertEquals(0xC1, session.activeRoomEventForTest());
        RoomEntity hinox = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x89).findFirst().orElseThrow();
        assertEquals(0x58, hinox.x());
        assertEquals(0x30, hinox.y());
        RoomEntity unresolvedHinoxWarp = session.activeRoom().entities().loadedEntities()
            .stream().filter(entity -> entity.type() == 0x61).findFirst().orElseThrow();
        assertEquals(0x48, unresolvedHinoxWarp.x());
        assertEquals(0x40, unresolvedHinoxWarp.y());
        assertFalse(session.activeRoom().hasWarps());
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertFalse(transition.isActive());

        // The ROM's entity-$61 warp is present but inert until the Hinox's
        // ordinary miniboss clear path records room status bit $20.
        int hinoxSlot = hinox.slot();
        int hinoxSourceLoadOrder = hinox.sourceLoadOrder();
        // The source leaves Link on the south D2 ledge. Holding UP drives the
        // real 12-contact directional ledge jump (bank2.asm:7053-7127).
        setDirectionalInput(inputState, inputConfig, 0x04, GLFW_PRESS);
        boolean room28LedgeJumpObserved = false;
        boolean room28LedgeJumpLanded = false;
        for (int ledgeFrame = 0; ledgeFrame < 0x80; ledgeFrame++) {
            link.update();
            room28LedgeJumpObserved |= link.isAirborne();
            if (room28LedgeJumpObserved && !link.isAirborne()) {
                room28LedgeJumpLanded = true;
                break;
            }
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
            assertFalse(scroll.isActive());
        }
        setDirectionalInput(inputState, inputConfig, 0x04, GLFW_RELEASE);
        assertTrue(room28LedgeJumpObserved,
            "Room-$28 entry must trigger the source D2 ledge jump at ("
                + link.pixelX() + "," + link.pixelY() + ") object="
                + String.format("%02X", collision.objectIdAtPoint(
                    link.pixelX() + 6, link.pixelY() + 6)) + " physics="
                + String.format("%02X", collision.objectPhysicsFlagAtPoint(
                    link.pixelX() + 6, link.pixelY() + 6)));
        assertTrue(room28LedgeJumpLanded,
            "Room-$28 D2 ledge jump did not land");
        assertFalse(link.isAirborne());
        assertFalse(link.isFallingIntoPit());

        List<int[]> dormantWarpPath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(),
            (x, y) -> Math.abs(signedByteDelta(unresolvedHinoxWarp.x(), x + 8)) <= 0x02
                && Math.abs(signedByteDelta(unresolvedHinoxWarp.y(), y + 16)) <= 0x02);
        assertNotNull(dormantWarpPath,
            "pre-clear entity-$61 warp is not collision reachable");
        for (int index = 1; index < dormantWarpPath.size(); index++) {
            int[] position = dormantWarpPath.get(index);
            link.setPixelPosition(position[0], position[1]);
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
        }
        for (int dormantTick = 0; dormantTick < 4; dormantTick++) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertEquals(0,
                session.entityTransitionCountdownForTest(unresolvedHinoxWarp.slot()));
            assertFalse(transition.isActive(),
                "entity-$61 must remain gated before the Hinox clear");
        }

        SwordApproach hinoxContact = reachableSwordContactPath(
            collision, link.pixelX(), link.pixelY(), hinox, romTables);
        assertNotNull(hinoxContact,
            "No collision-valid path from room-$28 ledge landing to Hinox\n"
                + collisionGrid(collision));
        for (int[] position : hinoxContact.path()) {
            link.setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
            assertFalse(scroll.isActive());
        }
        link.setDirection(hinoxContact.direction());
        tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        hinox = session.activeRoom().entities().slots().get(hinoxSlot);
        int[] firstSwordPosition = {link.pixelX(), link.pixelY()};
        Sword hinoxSword = new Sword(romTables, null);
        hinoxSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            hinoxSword.tick(false);
        }
        Sword.CollisionBox hinoxSwordBox = hinoxSword.enemyCollisionBox(
            link.romEntityX(), link.romSwordCollisionY(), link.direction());
        assertTrue(hinoxSwordBox.active(), "sword must be live at the Hinox contact");
        assertTrue(RoomEntityCombatRules.overlapsSword(
            hinox, hinoxSwordBox.x(), hinoxSwordBox.width(),
            hinoxSwordBox.y(), hinoxSwordBox.height()),
            "position=" + java.util.Arrays.toString(firstSwordPosition)
                + " sword=" + hinoxSwordBox + " hinox=" + hinox);
        List<EntityCombatEvent> hinoxHit = session.resolveEntityCombat(
            frame++, link.romEntityX(), link.romEntityY(), false, true, true,
            hinoxSwordBox.x(), hinoxSwordBox.width(),
            hinoxSwordBox.y(), hinoxSwordBox.height());
        assertTrue(hinoxHit.stream().anyMatch(event -> event.slot() == hinoxSlot
            && event.type() == HinoxMotion.ENTITY_TYPE
            && event.swordHit() && event.enemyDamage() > 0), hinoxHit.toString());

        boolean hinoxBombResponseObserved = false;
        int bombDeadline = frame + 0x100;
        while (frame < bombDeadline && !hinoxBombResponseObserved) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            hinoxBombResponseObserved = session.activeRoom().entities().loadedEntities()
                .stream().anyMatch(entity -> entity.type() == HinoxMotion.ENTITY_BOMB);
        }
        assertTrue(hinoxBombResponseObserved,
            "a live sword flash must enter Hinox's bomb response");

        // Continue using the ordinary sword collision/death path. The test
        // never writes enemy health or replaces an entity slot.
        boolean hinoxDeathStarted = false;
        int deathDeadline = frame + 0x1000;
        while (frame < deathDeadline) {
            RoomEntity liveHinox = session.activeRoom().entities().slots().get(hinoxSlot);
            if (liveHinox.status() == EntityStatus.DISABLED) {
                break;
            }
            if (liveHinox.status() == EntityStatus.ACTIVE) {
                SwordApproach approach = reachableSwordContactPath(
                    collision, link.pixelX(), link.pixelY(), liveHinox, romTables);
                if (approach != null) {
                    for (int[] position : approach.path()) {
                        link.setPixelPosition(position[0], position[1]);
                    }
                    link.setDirection(approach.direction());
                    Sword swordStrike = new Sword(romTables, null);
                    swordStrike.onPress();
                    for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
                        swordStrike.tick(false);
                    }
                    Sword.CollisionBox box = swordStrike.enemyCollisionBox(
                        link.romEntityX(), link.romSwordCollisionY(), link.direction());
                    if (box.active() && RoomEntityCombatRules.overlapsSword(
                            liveHinox, box.x(), box.width(), box.y(), box.height())) {
                        List<EntityCombatEvent> hit = session.resolveEntityCombat(
                            frame++, link.romEntityX(), link.romEntityY(), false, true, true,
                            box.x(), box.width(), box.y(), box.height());
                        hinoxDeathStarted |= hit.stream().anyMatch(event ->
                            event.slot() == hinoxSlot && event.swordHit()
                                && event.enemyDamage() > 0);
                    }
                }
            }
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        }
        assertTrue(hinoxDeathStarted, "live sword combat must start Hinox death");
        assertEquals(EntityStatus.DISABLED,
            session.activeRoom().entities().slots().get(hinoxSlot).status());
        assertEquals(0x20, session.indoorRoomStatusForTest(0x01, 0x28) & 0x20);
        assertEquals(hinoxSourceLoadOrder, hinox.sourceLoadOrder());

        int clearEventDeadline = frame + 0x100;
        while (session.activeRoomEventForTest() != 0 && frame < clearEventDeadline) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        }
        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(0x20, session.indoorRoomStatusForTest(0x01, 0x28) & 0x20);
        assertEquals(0x01, session.indoorRoomStatusForTest(0x01, 0x28) & 0x01,
            "clear-midboss must persist room $28's open right door");

        RoomEntity clearedWarp = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x61).findFirst().orElseThrow();
        // The ROM handler deliberately remains in state 1 until Link leaves
        // its contact window.  The fight ends with Link already standing on
        // this warp, so first walk away through collision-valid positions and
        // give the handler a tick there before approaching it again.
        List<int[]> disengagePath = reachablePositionPath(collision, link.pixelX(), link.pixelY(),
            (x, y) -> Math.abs(signedByteDelta(clearedWarp.x(), x + 8)) > 0x08
                || Math.abs(signedByteDelta(clearedWarp.y(), y + 16)) > 0x08);
        assertNotNull(disengagePath, "cleared entity-$61 warp cannot be disengaged");
        for (int index = 1; index < disengagePath.size(); index++) {
            int[] position = disengagePath.get(index);
            link.setPixelPosition(position[0], position[1]);
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
            assertFalse(scroll.isActive());
        }
        int awayX = link.romEntityX();
        int awayY = link.romEntityY();
        assertTrue(Math.abs(signedByteDelta(clearedWarp.x(), awayX)) > 0x08
                || Math.abs(signedByteDelta(clearedWarp.y(), awayY)) > 0x08);

        List<int[]> warpPath = reachablePositionPath(collision, link.pixelX(), link.pixelY(),
            (x, y) -> Math.abs(signedByteDelta(clearedWarp.x(), x + 8)) <= 0x02
                && Math.abs(signedByteDelta(clearedWarp.y(), y + 16)) <= 0x02);
        assertNotNull(warpPath, "cleared entity-$61 warp is not collision reachable");
        for (int index = 1; index < warpPath.size(); index++) {
            int[] position = warpPath.get(index);
            link.setPixelPosition(position[0], position[1]);
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
            assertFalse(scroll.isActive());
        }
        assertTrue(Math.abs(signedByteDelta(clearedWarp.x(), link.romEntityX())) <= 0x02
                && Math.abs(signedByteDelta(clearedWarp.y(), link.romEntityY())) <= 0x02);

        // Contact can occur one or two pixels before the path's final target,
        // so some of the exact $50 countdown may already have elapsed. Wait
        // the remaining source countdown and require the fade only on zero.
        int warpCountdown = session.entityTransitionCountdownForTest(clearedWarp.slot());
        assertTrue(warpCountdown > 0 && warpCountdown <= 0x50,
            "warp did not enter countdown at contact link="
                + String.format("%02X/%02X entity=%02X/%02X", link.romEntityX(),
                    link.romEntityY(), clearedWarp.x(), clearedWarp.y()));
        for (int remaining = warpCountdown; remaining > 0; remaining--) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            coordinator.handleWarpAndIndoorBoundaries(link);
            if (remaining > 1) {
                assertFalse(transition.isActive());
            }
        }
        assertTrue(transition.isActive(), "cleared entity-$61 should start a fade: room="
            + String.format("%02X", session.currentRoomId())
            + " status=" + String.format("%02X", session.indoorRoomStatusForTest(0x01, 0x28))
            + " link=" + String.format("%02X/%02X/%02X", link.romEntityX(),
                link.romEntityY(), link.romEntityZ())
            + " progress=" + String.format("%02X", session.dungeonProgressFlagsSnapshot()[1])
            + " warpCountdown=" + session.entityTransitionCountdownForTest(clearedWarp.slot()));
        while (transition.isActive()) {
            transition.tick();
        }
        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
        assertEquals(0x36, session.currentRoomId());
        assertEquals(0x50, link.romEntityX());
        assertEquals(0x48, link.romEntityY());

        // Room $36's entity-$61 is the opposite endpoint of the cleared
        // Hinox miniboss warp.  Leave its initial contact window, let the
        // source handler arm, then approach it again through live collision.
        RoomEntity room36Warp = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x61).findFirst().orElseThrow();
        assertEquals(0x48, room36Warp.x());
        assertEquals(0x40, room36Warp.y());
        int room36WarpSlot = room36Warp.slot();
        tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
        List<int[]> room36WarpDisengagePath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(),
            (x, y) -> Math.abs(signedByteDelta(room36Warp.x(), x + 8)) > 0x08
                || Math.abs(signedByteDelta(room36Warp.y(), y + 16)) > 0x08);
        assertNotNull(room36WarpDisengagePath, collisionGrid(collision));
        for (int index = 1; index < room36WarpDisengagePath.size(); index++) {
            int[] position = room36WarpDisengagePath.get(index);
            link.setPixelPosition(position[0], position[1]);
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
            assertFalse(scroll.isActive());
        }
        assertEquals(0, session.entityTransitionCountdownForTest(room36WarpSlot));

        RoomEntity armedRoom36Warp = session.activeRoom().entities().slots()
            .get(room36WarpSlot);
        List<int[]> room36WarpContactPath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(),
            (x, y) -> {
                int linkX = x + 8;
                int linkY = y + 16;
                int deltaX = signedByteDelta(linkX, armedRoom36Warp.x());
                int deltaY = signedByteDelta(linkY, armedRoom36Warp.y());
                return deltaX >= -3 && deltaX <= 2
                    && deltaY >= -3 && deltaY <= 2;
            });
        assertNotNull(room36WarpContactPath, collisionGrid(collision));
        for (int index = 1; index < room36WarpContactPath.size(); index++) {
            int[] position = room36WarpContactPath.get(index);
            link.setPixelPosition(position[0], position[1]);
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
            assertFalse(scroll.isActive());
        }
        int room36WarpCountdown = session.entityTransitionCountdownForTest(room36WarpSlot);
        assertTrue(room36WarpCountdown > 0 && room36WarpCountdown <= 0x50,
            "warp contact did not arm: link="
                + String.format("%02X/%02X", link.romEntityX(), link.romEntityY())
                + " entity=" + String.format("%02X/%02X", armedRoom36Warp.x(),
                    armedRoom36Warp.y())
                + " pathEnd=" + java.util.Arrays.toString(
                    room36WarpContactPath.getLast()));
        for (int remaining = room36WarpCountdown; remaining > 0; remaining--) {
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            coordinator.handleWarpAndIndoorBoundaries(link);
            if (remaining > 1) {
                assertFalse(transition.isActive());
            }
        }
        assertTrue(transition.isActive());
        while (transition.isActive()) {
            transition.tick();
        }
        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
        assertEquals(0x28, session.currentRoomId());
        assertEquals(0x50, link.romEntityX());
        assertEquals(0x48, link.romEntityY());

        int[] routeToRoom21 = {
            ScrollController.RIGHT, ScrollController.UP, ScrollController.UP
        };
        int[] routeToRoom21Rooms = {0x29, 0x26, 0x21};
        for (int index = 0; index < routeToRoom21.length; index++) {
            walkToAndCrossIndoorBoundary(
                coordinator, transition, scroll, collision, link, routeToRoom21[index]);
            assertEquals(routeToRoom21Rooms[index], session.currentRoomId());
        }

        assertEquals(0x21, session.currentRoomId());
        assertEquals(0x00, session.activeRoomEventForTest());
        assertEquals(0xA0, objectAt(session, 0x27));
        assertEquals(ChestContentsTable.CHEST_RUPEES_20,
            new ChestContentsTable(rom).itemForSpawn(
                0x01, 0x21, playerState.swordLevel(), true));
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);

        List<int[]> braceletDoorPath = reachablePositionPath(
            collision, link.pixelX(), link.pixelY(),
            (x, y) -> collision.objectPhysicsFlagAtPoint(x + 0x03, y + 0x09) == 0x92
                || collision.objectPhysicsFlagAtPoint(x + 0x03, y + 0x0C) == 0x92);
        assertNotNull(braceletDoorPath, collisionGrid(collision));
        for (int[] position : braceletDoorPath) {
            link.setPixelPosition(position[0], position[1]);
        }
        link.setPixelPosition(link.pixelX() - 1, link.pixelY());
        assertTrue(session.tryUnlockIndoorKeyDoor(
            link.pixelX(), link.pixelY(), Link.DIRECTION_LEFT, 0x04));
        assertEquals(0, session.currentDungeonItemFlagsSnapshot()[
            DungeonItemState.SMALL_KEYS_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            session.tickEntities(frame++, link.pixelX(), link.pixelY());
            assertTrue(session.consumeWorldLinkMotionBlockRequest());
        }
        session.tickEntities(frame++, link.pixelX(), link.pixelY());
        assertFalse(session.consumeWorldLinkMotionBlockRequest());
        assertEquals(0x02, session.indoorRoomStatusForTest(0x01, 0x21) & 0x02);
        assertEquals(0x01, session.indoorRoomStatusForTest(0x01, 0x20) & 0x01);
        walkToAndCrossIndoorBoundary(
            coordinator, transition, scroll, collision, link, ScrollController.LEFT);
        assertEquals(0x20, session.currentRoomId());
        assertEquals(0x61, session.activeRoomEventForTest());
        assertEquals(2, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x50).count());
        assertTrue(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x50
                && entity.x() == 0x48 && entity.y() == 0x30));
        assertTrue(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == 0x50
                && entity.x() == 0x58 && entity.y() == 0x50));
        assertEquals(0x07, objectAt(session, 0x28));
        assertEquals(ChestContentsTable.CHEST_POWER_BRACELET,
            new ChestContentsTable(rom).itemForSpawn(
                0x01, 0x20, playerState.swordLevel(), true));
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

    private static void tickInteractiveEntities(RoomSession session, int frame,
                                                int linkEntityX, int linkEntityY) {
        session.tickEntitiesWithProjectileEvents(
            frame, linkEntityX, linkEntityY, 0, 0, Link.DIRECTION_DOWN, false);
    }

    private static int letBowWowEatAllGoponga(RoomSession session,
                                              OverworldCollision collision,
                                              Link link, int frame) {
        int deadline = frame + 0x4000;
        while (session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x7C || entity.type() == 0x7E)
            && frame < deadline) {
            RoomEntity target = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x7C || entity.type() == 0x7E)
                .findFirst().orElseThrow();
            RoomEntity bowWow = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x6D)
                .findFirst().orElseThrow();
            List<int[]> leashPath = reachableFollowerLeashPath(
                collision, link.pixelX(), link.pixelY(), bowWow);
            assertNotNull(leashPath, "No collision-valid Link path back into BowWow's leash\n"
                + collisionGrid(collision));
            for (int[] position : leashPath) {
                link.setPixelPosition(position[0], position[1]);
            }
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            session.consumeEntityEvents();
            List<int[]> path = reachableEntityWindowPath(
                collision, link.pixelX(), link.pixelY(), target);
            assertNotNull(path, "No collision-valid Link path into BowWow's target window for "
                + String.format("%02X@(%02X,%02X)\n", target.type(), target.x(), target.y())
                + collisionGrid(collision));
            for (int[] position : path) {
                link.setPixelPosition(position[0], position[1]);
                tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
                session.consumeEntityEvents();
                while (bowWowOutsideLeash(session, link) && frame < deadline) {
                    tickInteractiveEntities(
                        session, frame++, link.romEntityX(), link.romEntityY());
                    session.consumeEntityEvents();
                }
            }
            while (session.activeRoom().entities().loadedEntities().stream()
                    .anyMatch(entity -> entity.slot() == target.slot()
                        && (entity.type() == 0x7C || entity.type() == 0x7E))
                && frame < deadline) {
                tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
                session.consumeEntityEvents();
            }
        }
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x7C || entity.type() == 0x7E),
            session.activeRoom().entities().loadedEntities().stream()
                .map(entity -> String.format("slot%d:%02X@(%02X,%02X,z=%02X,v=%d,s=%s)",
                    entity.slot(), entity.type(), entity.x(), entity.y(), entity.z(),
                    entity.spriteVariant(), entity.status()))
                .toList().toString());
        return frame;
    }

    private static int signedByteDelta(int target, int source) {
        int delta = (target - source) & 0xFF;
        return delta < 0x80 ? delta : delta - 0x100;
    }

    private static int objectAt(RoomSession session, int location) {
        return session.activeRoom().roomObjectsArea()[ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F)];
    }

    private static int firstObjectLocation(RoomSession session, int objectId) {
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int location = (row << 4) | column;
                if (objectAt(session, location) == objectId) {
                    return location;
                }
            }
        }
        return -1;
    }

    private static int[] roomObjectTiles(RoomSession session, int location) {
        int tileX = (location & 0x0F) * 2;
        int tileY = ((location & 0xF0) >>> 4) * 2;
        int[] tiles = session.activeRoom().tileIds();
        int rowStride = RoomConstants.ROOM_TILE_WIDTH;
        return new int[] {
            tiles[tileY * rowStride + tileX],
            tiles[tileY * rowStride + tileX + 1],
            tiles[(tileY + 1) * rowStride + tileX],
            tiles[(tileY + 1) * rowStride + tileX + 1]
        };
    }

    private static boolean bowWowOutsideLeash(RoomSession session, Link link) {
        RoomEntity bowWow = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x6D)
            .findFirst().orElseThrow();
        return Math.abs(signedByteDelta(link.romEntityX(), bowWow.x())) > 0x18
            || Math.abs(signedByteDelta(link.romEntityY(), bowWow.y())) > 0x18;
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

    private static void walkToAndExitIndoorFrontDoor(
            RoomTransitionCoordinator coordinator, TransitionController transition,
            OverworldCollision collision, Link link) {
        List<int[]> path = reachableBoundaryPath(
            collision, link.pixelX(), link.pixelY(), ScrollController.DOWN);
        assertNotNull(path, "No collision path to indoor front door\n"
            + collisionGrid(collision));
        for (int[] position : path) {
            link.setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive(),
                "collision path crossed the front-door boundary early");
        }
        int[] exit = path.getLast();
        link.setPixelPosition(exit[0], RoomConstants.ROOM_PIXEL_HEIGHT);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(transition.isActive());
        while (transition.isActive()) {
            transition.tick();
        }
    }

    private static void walkToAndCrossIndoorBoundaryWithFeather(
            RoomTransitionCoordinator coordinator, TransitionController transition,
            ScrollController scroll, OverworldCollision collision, RomTables romTables, Link link,
            InputState inputState, InputConfig inputConfig, ItemRegistry itemRegistry,
            PlayerState playerState, int direction) {
        List<int[]> path = reachableBoundaryPathWithFeather(
            collision, romTables, link.pixelX(), link.pixelY(), direction);
        assertNotNull(path, "No Feather-valid collision path to indoor boundary "
            + direction + "\n" + collisionGrid(collision));
        int jumpCount = walkCollisionPathWithFeather(
            coordinator, transition, scroll, collision, link, inputState, inputConfig,
            itemRegistry, playerState, path);
        assertTrue(jumpCount > 0);

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
        while (scroll.isActive()) {
            scroll.tick(8);
        }
    }

    private static int walkCollisionPathWithFeather(
            RoomTransitionCoordinator coordinator, TransitionController transition,
            ScrollController scroll, OverworldCollision collision, Link link,
            InputState inputState, InputConfig inputConfig, ItemRegistry itemRegistry,
            PlayerState playerState, List<int[]> path) {
        int jumpCount = 0;
        for (int index = 1; index < path.size(); index++) {
            int[] step = path.get(index);
            int action = step[2];
            if (action < 4) {
                link.setPixelPosition(step[0], step[1]);
            } else {
                int encodedMasks = action - 4;
                int firstMask = encodedMasks & 0x0F;
                int secondMask = (encodedMasks >>> 4) & 0x0F;
                int thirdMask = (encodedMasks >>> 8) & 0x0F;
                setDirectionalInput(inputState, inputConfig, firstMask, GLFW_PRESS);
                int jumpStartX = link.pixelX();
                int jumpStartY = link.pixelY();
                itemRegistry.lookup(playerState.itemA()).onPress();
                assertTrue(link.isAirborne(), "Feather did not start at ("
                    + jumpStartX + "," + jumpStartY + ")");
                boolean crossedPit = false;
                int deadline = 0x40;
                int jumpFrame = 0;
                while (link.isAirborne() && deadline-- > 0) {
                    if (jumpFrame == 11) {
                        setDirectionalInput(
                            inputState, inputConfig, firstMask, GLFW_RELEASE);
                        setDirectionalInput(
                            inputState, inputConfig, secondMask, GLFW_PRESS);
                    } else if (jumpFrame == 22) {
                        setDirectionalInput(
                            inputState, inputConfig, secondMask, GLFW_RELEASE);
                        setDirectionalInput(
                            inputState, inputConfig, thirdMask, GLFW_PRESS);
                    }
                    link.update();
                    crossedPit |= collision.linkOnNormalPit(link.pixelX(), link.pixelY());
                    jumpFrame++;
                }
                setDirectionalInput(inputState, inputConfig, firstMask, GLFW_RELEASE);
                setDirectionalInput(inputState, inputConfig, secondMask, GLFW_RELEASE);
                setDirectionalInput(inputState, inputConfig, thirdMask, GLFW_RELEASE);
                assertTrue(crossedPit, "planned jump " + firstMask + "/"
                    + secondMask + "/" + thirdMask + " from (" + jumpStartX
                    + "," + jumpStartY + ") landed at (" + link.pixelX() + ","
                    + link.pixelY() + "), expected (" + step[0] + "," + step[1]
                    + ")\n" + collisionGrid(collision));
                assertFalse(link.isAirborne());
                assertFalse(link.isFallingIntoPit());
                assertFalse(collision.linkOnNormalPit(link.pixelX(), link.pixelY()));
                // The planner models integer pixels; Link retains subpixel
                // inertia during the real jump and can land one pixel beyond
                // that modelled point without changing the collision cell.
                assertTrue(Math.abs(step[0] - link.pixelX()) <= 1);
                assertTrue(Math.abs(step[1] - link.pixelY()) <= 1);
                link.update();
                assertFalse(link.isFallingIntoPit());
                jumpCount++;
            }
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
            assertFalse(scroll.isActive());
        }
        return jumpCount;
    }

    private static void jumpOverPitWithFeather(
            OverworldCollision collision, Link link, InputState inputState,
            InputConfig inputConfig, ItemRegistry itemRegistry, PlayerState playerState,
            int joypadMask) {
        setDirectionalInput(inputState, inputConfig, joypadMask, GLFW_PRESS);
        itemRegistry.lookup(playerState.itemA()).onPress();
        assertTrue(link.isAirborne());
        boolean crossedPit = false;
        int deadline = 0x40;
        while (link.isAirborne() && deadline-- > 0) {
            link.update();
            crossedPit |= collision.linkOnNormalPit(link.pixelX(), link.pixelY());
        }
        setDirectionalInput(inputState, inputConfig, joypadMask, GLFW_RELEASE);
        assertTrue(crossedPit);
        assertFalse(link.isAirborne());
        assertFalse(link.isFallingIntoPit());
        assertFalse(collision.linkOnNormalPit(link.pixelX(), link.pixelY()));
        link.update();
        assertFalse(link.isFallingIntoPit());
    }

    private static List<int[]> reachableBoundaryPathWithFeather(
            OverworldCollision collision, RomTables romTables,
            int startX, int startY, int targetDirection) {
        return reachablePositionPathWithFeather(
            collision, romTables, startX, startY,
            (x, y) -> isTargetBoundary(x, y,
                RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1,
                RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1,
                targetDirection));
    }

    private static List<int[]> reachablePositionPathWithFeather(
            OverworldCollision collision, RomTables romTables,
            int startX, int startY, java.util.function.BiPredicate<Integer, Integer> target) {
        int width = RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1;
        int height = RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1;
        boolean[][] visited = new boolean[height][width];
        int[][] parentX = new int[height][width];
        int[][] parentY = new int[height][width];
        int[][] parentAction = new int[height][width];
        for (int row = 0; row < height; row++) {
            java.util.Arrays.fill(parentX[row], -1);
            java.util.Arrays.fill(parentY[row], -1);
            java.util.Arrays.fill(parentAction[row], -1);
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
        int[] jumpMasks = {0x00, 0x04, 0x08, 0x02, 0x01, 0x06, 0x05, 0x0A, 0x09};

        while (!queue.isEmpty()) {
            int[] position = queue.removeFirst();
            if (target.test(position[0], position[1])) {
                ArrayDeque<int[]> reverse = new ArrayDeque<>();
                int x = position[0];
                int y = position[1];
                while (parentX[y][x] != x || parentY[y][x] != y) {
                    reverse.addFirst(new int[] {x, y, parentAction[y][x]});
                    int nextX = parentX[y][x];
                    int nextY = parentY[y][x];
                    x = nextX;
                    y = nextY;
                }
                reverse.addFirst(new int[] {x, y, -1});
                return List.copyOf(reverse);
            }

            for (int[] move : moves) {
                int x = position[0] + move[0];
                int y = position[1] + move[1];
                if (x < 0 || x >= width || y < 0 || y >= height
                    || visited[y][x]
                    || leadingEdgeBlocked(collision, x, y, move[2], true)
                    || collision.linkOnNormalPit(x, y)) {
                    continue;
                }
                visited[y][x] = true;
                parentX[y][x] = position[0];
                parentY[y][x] = position[1];
                parentAction[y][x] = move[2];
                queue.addLast(new int[] {x, y});
            }

            for (int firstMask : jumpMasks) {
                for (int secondMask : jumpMasks) {
                    for (int thirdMask : jumpMasks) {
                int subX = position[0] << Link.SUB_PIXEL_SHIFT;
                int subY = position[1] << Link.SUB_PIXEL_SHIFT;
                boolean crossedPit = false;
                for (int frame = 0; frame < 33; frame++) {
                    int joypadMask = frame < 11 ? firstMask
                        : frame < 22 ? secondMask : thirdMask;
                    int speedX = (byte) romTables.linkSpeedX(joypadMask);
                    int speedY = (byte) romTables.linkSpeedY(joypadMask);
                    if (speedX != 0) {
                        int candidateSubX = subX + speedX;
                        int nextX = candidateSubX >> Link.SUB_PIXEL_SHIFT;
                        int y = subY >> Link.SUB_PIXEL_SHIFT;
                        int moveDirection = speedX < 0
                            ? Link.DIRECTION_LEFT : Link.DIRECTION_RIGHT;
                        if (nextX >= 0 && nextX < width
                            && !leadingEdgeBlocked(
                                collision, nextX, y, moveDirection, false)) {
                            subX = candidateSubX;
                        }
                    }
                    if (speedY != 0) {
                        int candidateSubY = subY + speedY;
                        int x = subX >> Link.SUB_PIXEL_SHIFT;
                        int nextY = candidateSubY >> Link.SUB_PIXEL_SHIFT;
                        int moveDirection = speedY < 0
                            ? Link.DIRECTION_UP : Link.DIRECTION_DOWN;
                        if (nextY >= 0 && nextY < height
                            && !leadingEdgeBlocked(
                                collision, x, nextY, moveDirection, false)) {
                            subY = candidateSubY;
                        }
                    }
                    int x = subX >> Link.SUB_PIXEL_SHIFT;
                    int y = subY >> Link.SUB_PIXEL_SHIFT;
                    crossedPit |= collision.linkOnNormalPit(x, y);
                }
                int x = subX >> Link.SUB_PIXEL_SHIFT;
                int y = subY >> Link.SUB_PIXEL_SHIFT;
                if (!crossedPit || collision.linkOnNormalPit(x, y) || visited[y][x]) {
                    continue;
                }
                visited[y][x] = true;
                parentX[y][x] = position[0];
                parentY[y][x] = position[1];
                parentAction[y][x] = 4 + firstMask + (secondMask << 4)
                    + (thirdMask << 8);
                queue.addLast(new int[] {x, y});
                    }
                }
            }
        }
        return null;
    }

    private static void setDirectionalInput(InputState inputState, InputConfig inputConfig,
                                            int joypadMask, int action) {
        if ((joypadMask & 0x04) != 0) inputState.onKeyEvent(inputConfig.upKey(), action);
        if ((joypadMask & 0x08) != 0) inputState.onKeyEvent(inputConfig.downKey(), action);
        if ((joypadMask & 0x02) != 0) inputState.onKeyEvent(inputConfig.leftKey(), action);
        if ((joypadMask & 0x01) != 0) inputState.onKeyEvent(inputConfig.rightKey(), action);
    }

    private static List<int[]> reachableBoundaryPath(OverworldCollision collision,
                                                      int startX, int startY,
                                                      int targetDirection) {
        return reachableBoundaryPath(collision, startX, startY, targetDirection, false);
    }

    private static List<int[]> reachableEntityWindowPath(OverworldCollision collision,
                                                          int startX, int startY,
                                                          RoomEntity target) {
        return reachablePositionPath(collision, startX, startY,
            (x, y) -> Math.abs(signedByteDelta(target.x(), x + 8)) <= 0x28
                && Math.abs(signedByteDelta(target.y(), y + 16)) <= 0x28);
    }

    private static List<int[]> reachableFollowerLeashPath(OverworldCollision collision,
                                                           int startX, int startY,
                                                           RoomEntity bowWow) {
        return reachablePositionPath(collision, startX, startY,
            (x, y) -> Math.abs(signedByteDelta(bowWow.x(), x + 8)) <= 0x10
                && Math.abs(signedByteDelta(bowWow.y(), y + 16)) <= 0x10);
    }

    private static List<int[]> reachableEntityContactPath(OverworldCollision collision,
                                                           int startX, int startY,
                                                           RoomEntity entity) {
        return reachablePositionPath(collision, startX, startY,
            (x, y) -> Math.abs(signedByteDelta(entity.x(), x + 8)) <= 0x04
                && Math.abs(signedByteDelta(entity.y(), y + 16)) <= 0x04);
    }

    private record SwordApproach(List<int[]> path, int direction) {}

    private static SwordApproach reachableSwordContactPath(OverworldCollision collision,
                                                            int startX, int startY,
                                                            RoomEntity entity,
                                                            RomTables romTables) {
        Sword sword = new Sword(romTables, null);
        sword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            sword.tick(false);
        }
        for (int direction = Link.DIRECTION_DOWN;
                direction <= Link.DIRECTION_RIGHT; direction++) {
            int swordDirection = direction;
            List<int[]> path = reachablePositionPath(collision, startX, startY,
                (x, y) -> {
                    Sword.CollisionBox box = sword.enemyCollisionBox(
                        x + 8, y + 16, swordDirection);
                    return box.active() && RoomEntityCombatRules.overlapsSword(
                        entity, box.x(), box.width(), box.y(), box.height());
            });
            if (path != null) {
                return new SwordApproach(path, swordDirection);
            }
        }
        return null;
    }

    private static SwordApproach reachableSwordContactPathWithFeather(
            OverworldCollision collision, RomTables romTables,
            int startX, int startY, RoomEntity entity) {
        Sword sword = new Sword(romTables, null);
        sword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            sword.tick(false);
        }
        for (int direction = Link.DIRECTION_DOWN;
                direction <= Link.DIRECTION_RIGHT; direction++) {
            int swordDirection = direction;
            List<int[]> path = reachablePositionPathWithFeather(collision, romTables,
                startX, startY, (x, y) -> {
                    Sword.CollisionBox box = sword.enemyCollisionBox(
                        x + 8, y + 16, swordDirection);
                    return box.active() && RoomEntityCombatRules.overlapsSword(
                        entity, box.x(), box.width(), box.y(), box.height());
                });
            if (path != null) {
                return new SwordApproach(path, swordDirection);
            }
        }
        return null;
    }

    private static PowderApproach reachablePowderApproach(OverworldCollision collision,
                                                            int startX, int startY,
                                                            int targetLocation) {
        int[] xOffsets = {0x0E, -0x0E, 0x00, 0x00};
        int[] yOffsets = {0x00, 0x00, -0x0C, 0x0C};
        for (int romDirection = 0; romDirection < 4; romDirection++) {
            int direction = romDirection;
            List<int[]> path = reachablePositionPath(collision, startX, startY, (x, y) -> {
                int sprinkleX = (x + 0x08 + xOffsets[direction]) & 0xFF;
                int sprinkleY = (y + 0x10 + yOffsets[direction]) & 0xFF;
                int objectLeft = ((sprinkleX - 0x01) & 0xFF) & 0xF0;
                int objectTop = ((sprinkleY - 0x09) & 0xFF) & 0xF0;
                int sampledLeft = sprinkleX & 0xF0;
                int sampledTop = ((sprinkleY - 0x08) & 0xFF) & 0xF0;
                return (objectTop | (objectLeft >>> 4)) == targetLocation
                    && (sampledTop | (sampledLeft >>> 4)) == targetLocation;
            });
            if (path != null) {
                return new PowderApproach(path, direction);
            }
        }
        return null;
    }

    private record PowderApproach(List<int[]> path, int romDirection) {}

    private static List<int[]> reachableWarpPath(OverworldCollision collision,
                                                  int startX, int startY,
                                                  int tileLocation) {
        return reachablePositionPath(collision, startX, startY,
            (x, y) -> Warp.packTileLocation(x, y) == tileLocation);
    }

    private static List<int[]> reachablePositionPath(OverworldCollision collision,
                                                      int startX, int startY,
                                                      BiPredicate<Integer, Integer> goal) {
        int width = RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1;
        int height = RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1;
        boolean[][] visited = new boolean[height][width];
        int[][] parentX = new int[height][width];
        int[][] parentY = new int[height][width];
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
            if (goal.test(position[0], position[1])) {
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
                if (x < 0 || x >= width || y < 0 || y >= height || visited[y][x]
                    || leadingEdgeBlocked(collision, x, y, move[2], false)) {
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

    private static List<int[]> reachableBoundaryPath(OverworldCollision collision,
                                                      int startX, int startY,
                                                      int targetDirection,
                                                      boolean pitsBlock) {
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
                    || visited[y][x]
                    || leadingEdgeBlocked(collision, x, y, move[2], pitsBlock)) {
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
                    || leadingEdgeBlocked(collision, x, y, move[2], false)) {
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
                                              int x, int y, int direction,
                                              boolean pitsBlock) {
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
                && (pitsBlock || !collision.pointNormalPit(sampleX, sampleY))) {
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
