package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

public interface RoomNavigationCompatibility {
    RoomSession facade();

    /** @deprecated Use {@code rooms().loadInitialOverworld()}. */
    @Deprecated(forRemoval = true)
    default void loadInitialOverworld(int roomId) {
        facade().rooms().loadInitialOverworld(roomId);
    }

    /** @deprecated Use {@code transitions().loadWarpDestination()}. */
    @Deprecated(forRemoval = true)
    default void loadWarpDestination(Warp warp) {
        facade().transitions().loadWarpDestination(warp);
    }
    /** @deprecated Use {@code transitions().tickDungeonTitleMessage()}. */
    @Deprecated(forRemoval = true)
    default int tickDungeonTitleMessage(
            boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement) {
        return facade().transitions()
                .tickDungeonTitleMessage(dialogActive, inventoryClosed, linkMotionState, freeMovement);
    }

    /** @deprecated Use {@code transitions().startAdjacentOverworldScroll()}. */
    @Deprecated(forRemoval = true)
    default void startAdjacentOverworldScroll(
            ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        facade().transitions().startAdjacentOverworldScroll(scrollController, direction, linkScreenX, linkScreenY);
    }
    /** @deprecated Use {@code rooms().musicSelectionIsScroll()}. */
    @Deprecated(forRemoval = true)
    default boolean musicSelectionIsScroll() {
        return facade().rooms().musicSelectionIsScroll();
    }

    /** @deprecated Use {@code rooms().loadOverworld()}. */
    @Deprecated(forRemoval = true)
    default void loadOverworld(int roomId) {
        facade().rooms().loadOverworld(roomId);
    }

    /** @deprecated Use {@code rooms().loadIndoor()}. */
    @Deprecated(forRemoval = true)
    default void loadIndoor(int mapId, int roomId) {
        facade().rooms().loadIndoor(mapId, roomId);
    }
    /** @deprecated Use {@code rooms().loadIndoorFromSavedPosition()}. */
    @Deprecated(forRemoval = true)
    default void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition) {
        facade().rooms().loadIndoorFromSavedPosition(mapId, roomId, savedIndoorMapPosition);
    }

    /** @deprecated Use {@code rooms().loadIndoor()}. */
    @Deprecated(forRemoval = true)
    default void loadIndoor(int mapId, int roomId, int mapCategory) {
        facade().rooms().loadIndoor(mapId, roomId, mapCategory);
    }
    /** @deprecated Use {@code transitions().startOcarina()}. */
    @Deprecated(forRemoval = true)
    default boolean startOcarina(int countdown, int songFlags, int selectedSong) {
        return facade().transitions().startOcarina(countdown, songFlags, selectedSong);
    }
    /** @deprecated Use {@code transitions().ocarinaPlaying()}. */
    @Deprecated(forRemoval = true)
    default boolean ocarinaPlaying() {
        return facade().transitions().ocarinaPlaying();
    }
    /** @deprecated Use {@code transitions().ocarinaAnimationState()}. */
    @Deprecated(forRemoval = true)
    default int ocarinaAnimationState() {
        return facade().transitions().ocarinaAnimationState();
    }
}
