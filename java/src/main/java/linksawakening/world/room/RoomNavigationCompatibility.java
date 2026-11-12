package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

interface RoomNavigationCompatibility {
    RoomSession facade();

    default void loadInitialOverworld(int roomId) {
        facade().context.roomLoadCoordinator.loadInitialOverworld(roomId);
    }

    default void loadWarpDestination(Warp warp) {
        facade().transitions().loadWarpDestination(warp);
    }
    /** $00:0F53 countdown and $01:61EE dungeon-title dialog gates. */
    default int tickDungeonTitleMessage(
            boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement) {
        return facade().transitions()
                .tickDungeonTitleMessage(dialogActive, inventoryClosed, linkMotionState, freeMovement);
    }

    default void startAdjacentOverworldScroll(
            ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        facade().transitions().startAdjacentOverworldScroll(scrollController, direction, linkScreenX, linkScreenY);
    }
    /** Whether the most recent load follows room_transition.asm rather than the warp loader. */
    default boolean musicSelectionIsScroll() {
        return facade().context.roomLoadCoordinator.musicSelectionIsScroll;
    }

    default void loadOverworld(int roomId) {
        facade().context.roomLoadCoordinator.loadOverworld(roomId);
    }

    default void loadIndoor(int mapId, int roomId) {
        facade().context.roomLoadCoordinator.loadIndoor(mapId, roomId);
    }
    /** Restores hMapRoom and the independently saved source wIndoorRoom byte. */
    default void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition) {
        facade().context.roomLoadCoordinator.loadIndoorFromSavedPosition(mapId, roomId, savedIndoorMapPosition);
    }

    default void loadIndoor(int mapId, int roomId, int mapCategory) {
        facade().context.roomLoadCoordinator.loadIndoor(mapId, roomId, mapCategory);
    }
    /** Starts the source UseOcarina countdown consumed by active entities. */
    default boolean startOcarina(int countdown, int songFlags, int selectedSong) {
        return facade().transitions().startOcarina(countdown, songFlags, selectedSong);
    }
    /** Mirrors wLinkPlayingOcarinaCountdown for Link's motion gate. */
    default boolean ocarinaPlaying() {
        return facade().transitions().ocarinaPlaying();
    }
    /** Mirrors hLinkAnimationState $75/$76 while LinkPlayingOcarinaHandler runs. */
    default int ocarinaAnimationState() {
        return facade().transitions().ocarinaAnimationState();
    }
}
