package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

public interface RoomStoryCompatibility {
    RoomSession facade();

    /** @deprecated Use {@code story().applyMarinWakeUpPresentation()}. */
    @Deprecated(forRemoval = true)
    default boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        return facade().story().applyMarinWakeUpPresentation(x, y, spriteVariant);
    }
    /** @deprecated Use {@code story().updateHouseTarinPresentation()}. */
    @Deprecated(forRemoval = true)
    default void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        facade().story().updateHouseTarinPresentation(frameCounter, linkX, linkY);
    }
    /** @deprecated Use {@code story().setBirdKeyOwned()}. */
    @Deprecated(forRemoval = true)
    default void setBirdKeyOwned(boolean owned) {
        facade().story().setBirdKeyOwned(owned);
    }
    /** @deprecated Use {@code story().setTailKeyOwned()}. */
    @Deprecated(forRemoval = true)
    default void setTailKeyOwned(boolean owned) {
        facade().story().setTailKeyOwned(owned);
    }
    /** @deprecated Use {@code story().initializeNewGameWorldState()}. */
    @Deprecated(forRemoval = true)
    default void initializeNewGameWorldState() {
        facade().story().initializeNewGameWorldState();
    }
    /** @deprecated Use {@code story().restoreRoomStatuses()}. */
    @Deprecated(forRemoval = true)
    default void restoreRoomStatuses(
            byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) {
        facade().story().restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
    }
    /** @deprecated Use {@code story().bowWowState()}. */
    @Deprecated(forRemoval = true)
    default int bowWowState() {
        return facade().story().bowWowState();
    }

    /** @deprecated Use {@code story().setBowWowState()}. */
    @Deprecated(forRemoval = true)
    default void setBowWowState(int state) {
        facade().story().setBowWowState(state);
    }

    /** @deprecated Use {@code story().richardSpokenFlag()}. */
    @Deprecated(forRemoval = true)
    default int richardSpokenFlag() {
        return facade().story().richardSpokenFlag();
    }

    /** @deprecated Use {@code story().setRichardSpokenFlag()}. */
    @Deprecated(forRemoval = true)
    default void setRichardSpokenFlag(int flag) {
        facade().story().setRichardSpokenFlag(flag);
    }
    /** @deprecated Use {@code story().tarinFlag()}. */
    @Deprecated(forRemoval = true)
    default int tarinFlag() {
        return facade().story().tarinFlag();
    }

    /** @deprecated Use {@code story().setTarinFlag()}. */
    @Deprecated(forRemoval = true)
    default void setTarinFlag(int flag) {
        facade().story().setTarinFlag(flag);
    }
    /** @deprecated Use {@code story().setHouseNpcTradeItem()}. */
    @Deprecated(forRemoval = true)
    default void setHouseNpcTradeItem(int tradeItem) {
        facade().story().setHouseNpcTradeItem(tradeItem);
    }

    /** @deprecated Use {@code story().setChestPlayerLevels()}. */
    @Deprecated(forRemoval = true)
    default void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        facade().story().setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
    }

    /** @deprecated Use {@code story().setEntityDefaultMusicTrack()}. */
    @Deprecated(forRemoval = true)
    default void setEntityDefaultMusicTrack(int trackId) {
        facade().story().setEntityDefaultMusicTrack(trackId);
    }
    /** @deprecated Use {@code story().shouldGetLostInMysteriousWoods()}. */
    @Deprecated(forRemoval = true)
    default boolean shouldGetLostInMysteriousWoods() {
        return facade().story().shouldGetLostInMysteriousWoods();
    }
}
