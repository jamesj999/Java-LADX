package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.Map;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.FollowingNpcState;

public interface RoomStory {
    boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant);

    void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY);

    void setBirdKeyOwned(boolean owned);

    void setTailKeyOwned(boolean owned);

    void initializeNewGameWorldState();

    void restoreRoomStatuses(
            byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus);

    void setFollowerSpriteOverrides(Map<Integer, EntitySpriteDefinition> overrides);

    void setFollowingNpcState(
            FollowingNpcState state, int linkX, int linkY, int linkZ, int entityYOffset, int linkDirection);

    FollowingNpcState followingNpcState();

    int bowWowState();

    void setBowWowState(int state);

    int richardSpokenFlag();

    void setRichardSpokenFlag(int flag);

    int tarinFlag();

    void setTarinFlag(int flag);

    void setHouseNpcTradeItem(int tradeItem);

    void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel);

    void setEntityDefaultMusicTrack(int trackId);

    boolean shouldGetLostInMysteriousWoods();
}
