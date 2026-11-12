package linksawakening.world.room;

import java.util.Map;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.room.RoomSession.*;

final class RoomStoryView implements RoomStory {
    private final RoomStoryState delegate;

    RoomStoryView(RoomStoryState delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        return delegate.applyMarinWakeUpPresentation(x, y, spriteVariant);
    }

    @Override
    public void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        delegate.updateHouseTarinPresentation(frameCounter, linkX, linkY);
    }

    @Override
    public void setBirdKeyOwned(boolean owned) {
        delegate.setBirdKeyOwned(owned);
    }

    @Override
    public void setTailKeyOwned(boolean owned) {
        delegate.setTailKeyOwned(owned);
    }

    @Override
    public void initializeNewGameWorldState() {
        delegate.initializeNewGameWorldState();
    }

    @Override
    public void restoreRoomStatuses(
            byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) {
        delegate.restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
    }

    @Override
    public void setFollowerSpriteOverrides(Map<Integer, EntitySpriteDefinition> overrides) {
        delegate.setFollowerSpriteOverrides(overrides);
    }

    @Override
    public void setFollowingNpcState(
            FollowingNpcState state, int linkX, int linkY, int linkZ, int entityYOffset, int linkDirection) {
        delegate.setFollowingNpcState(state, linkX, linkY, linkZ, entityYOffset, linkDirection);
    }

    @Override
    public FollowingNpcState followingNpcState() {
        return delegate.followingNpcState();
    }

    @Override
    public int bowWowState() {
        return delegate.bowWowState();
    }

    @Override
    public void setBowWowState(int state) {
        delegate.setBowWowState(state);
    }

    @Override
    public int richardSpokenFlag() {
        return delegate.richardSpokenFlag();
    }

    @Override
    public void setRichardSpokenFlag(int flag) {
        delegate.setRichardSpokenFlag(flag);
    }

    @Override
    public int tarinFlag() {
        return delegate.tarinFlag();
    }

    @Override
    public void setTarinFlag(int flag) {
        delegate.setTarinFlag(flag);
    }

    @Override
    public void setHouseNpcTradeItem(int tradeItem) {
        delegate.setHouseNpcTradeItem(tradeItem);
    }

    @Override
    public void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        delegate.setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
    }

    @Override
    public void setEntityDefaultMusicTrack(int trackId) {
        delegate.setEntityDefaultMusicTrack(trackId);
    }

    @Override
    public boolean shouldGetLostInMysteriousWoods() {
        return delegate.shouldGetLostInMysteriousWoods();
    }
}
