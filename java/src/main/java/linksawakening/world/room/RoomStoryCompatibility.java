package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

interface RoomStoryCompatibility {
    RoomSession facade();

    default boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        return facade().story().applyMarinWakeUpPresentation(x, y, spriteVariant);
    }
    /** Tarin's indoor handler updates his facing every 32 frames, even during the bed scene. */
    default void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        facade().story().updateHouseTarinPresentation(frameCounter, linkX, linkY);
    }
    /** Supplies the patched EntityInitKeyDropPoint inventory byte. */
    default void setBirdKeyOwned(boolean owned) {
        facade().story().setBirdKeyOwned(owned);
    }
    /** Supplies wHasTailKey to the active owl-event handler. */
    default void setTailKeyOwned(boolean owned) {
        facade().story().setTailKeyOwned(owned);
    }
    /**
     * Clears the persistent WRAM-shaped world state created for a fresh save.
     * The source new-file path starts with zeroed room-status, entity-clear,
     * dungeon-item, and dungeon-progress regions; retaining any of these from
     * a previously played file can incorrectly unload opening entities such
     * as the beach sword and owl.
     */
    default void initializeNewGameWorldState() {
        facade().story().initializeNewGameWorldState();
    }
    /** Restores the room-status WRAM tables loaded by the source save path. */
    default void restoreRoomStatuses(
            byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) {
        facade().story().restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
    }
    /** Mirrors wIsBowWowFollowingLink, including BOW_WOW_KIDNAPPED=$80. */
    default int bowWowState() {
        return facade().story().bowWowState();
    }

    default void setBowWowState(int state) {
        facade().story().setBowWowState(state);
    }

    default int richardSpokenFlag() {
        return facade().story().richardSpokenFlag();
    }

    default void setRichardSpokenFlag(int flag) {
        facade().story().setRichardSpokenFlag(flag);
    }
    /** Mirrors the unsigned-byte world progression flag at wTarinFlag. */
    default int tarinFlag() {
        return facade().story().tarinFlag();
    }

    default void setTarinFlag(int flag) {
        facade().story().setTarinFlag(flag);
    }
    /** Supplies the upgrade bytes consumed by the live chest dialog handler. */
    default void setHouseNpcTradeItem(int tradeItem) {
        facade().story().setHouseNpcTradeItem(tradeItem);
    }

    default void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        facade().story().setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
    }

    default void setEntityDefaultMusicTrack(int trackId) {
        facade().story().setEntityDefaultMusicTrack(trackId);
    }
    /** Returns wShouldGetLostInMysteriousWoods from the latest entity pass. */
    default boolean shouldGetLostInMysteriousWoods() {
        return facade().story().shouldGetLostInMysteriousWoods();
    }
}
