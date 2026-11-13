package linksawakening.world.room;

import java.util.Map;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.FollowingNpcState;

final class RoomStoryView implements RoomStory {
    private final RoomStoryState delegate;
    private final RoomSession session;

    RoomStoryView(RoomStoryState delegate, RoomSession session) {
        this.delegate = delegate;
        this.session = session;
    }

    @Override
    public boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        return session.applyMarinWakeUpPresentation(x, y, spriteVariant);
    }

    @Override
    public void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        session.updateHouseTarinPresentation(frameCounter, linkX, linkY);
    }

    @Override
    public void setBirdKeyOwned(boolean owned) {
        delegate.setBirdKeyOwned(owned);
    }

    @Override
    public void setTailKeyOwned(boolean owned) {
        session.setTailKeyOwned(owned);
    }

    @Override
    public void initializeNewGameWorldState() {
        session.initializeNewGameWorldState();
    }

    @Override
    public void restoreRoomStatuses(
            byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) {
        session.restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
    }

    @Override
    public void setFollowerSpriteOverrides(Map<Integer, EntitySpriteDefinition> overrides) {
        session.setFollowerSpriteOverrides(overrides);
    }

    @Override
    public void setFollowingNpcState(
            FollowingNpcState state, int linkX, int linkY, int linkZ, int entityYOffset, int linkDirection) {
        session.setFollowingNpcState(state, linkX, linkY, linkZ, entityYOffset, linkDirection);
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
        session.setBowWowState(state);
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
        session.setHouseNpcTradeItem(tradeItem);
    }

    @Override
    public void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        session.setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
    }

    @Override
    public void setEntityDefaultMusicTrack(int trackId) {
        session.setEntityDefaultMusicTrack(trackId);
    }

    @Override
    public boolean shouldGetLostInMysteriousWoods() {
        return delegate.shouldGetLostInMysteriousWoods();
    }
}
