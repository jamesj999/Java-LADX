package linksawakening.world.room;

import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.LinkPositionHistory;

final class RoomFollowerState {
    void observeLink(int x, int y, int z, int direction) {
        followingLinkX = x & 0xFF;
        followingLinkY = y & 0xFF;
        followingLinkZ = z & 0xFF;
        followingLinkDirection = direction & 0xFF;
    }

    void observePickup(int z, int direction) {
        followingLinkZ = z & 0xFF;
        followingLinkDirection = direction & 0xFF;
    }

    private final LinkPositionHistory followingLinkPositionHistory = new LinkPositionHistory();
    private FollowingNpcState followingNpcState = FollowingNpcState.none();
    private int followingLinkX = 8;
    private int followingLinkY = 16;
    private int followingLinkZ;
    private int followingEntityYOffset;
    private int followingLinkDirection;
    private boolean followingNpcRoomNeedsSync;
    private boolean hasBirdKey;
    private boolean hasTailKey;
    private int bowWowState;
    private int richardSpokenFlag;
    /** WRAM wTarinFlag; persisted NPC/world progression, not player inventory state. */
    private int tarinFlag;
    /** WRAM wShieldLevel retained for room-load-time palette selection. */
    private int playerShieldLevel;

    private int playerSwordLevel;
    private int playerBraceletLevel;
    private int houseNpcTradeItem;
    private int entityDefaultMusicTrack = 5;

    FollowingNpcState followingNpcState() {
        return this.followingNpcState;
    }

    FollowingNpcState followingNpcState(FollowingNpcState value) {
        this.followingNpcState = value;
        return this.followingNpcState;
    }

    boolean hasBirdKey() {
        return this.hasBirdKey;
    }

    boolean hasBirdKey(boolean value) {
        this.hasBirdKey = value;
        return this.hasBirdKey;
    }

    int tarinFlag() {
        return this.tarinFlag;
    }

    int tarinFlag(int value) {
        this.tarinFlag = value;
        return this.tarinFlag;
    }

    int playerShieldLevel() {
        return this.playerShieldLevel;
    }

    int playerShieldLevel(int value) {
        this.playerShieldLevel = value;
        return this.playerShieldLevel;
    }

    boolean followingNpcRoomNeedsSync() {
        return this.followingNpcRoomNeedsSync;
    }

    boolean followingNpcRoomNeedsSync(boolean value) {
        this.followingNpcRoomNeedsSync = value;
        return this.followingNpcRoomNeedsSync;
    }

    int followingEntityYOffset() {
        return this.followingEntityYOffset;
    }

    int followingEntityYOffset(int value) {
        this.followingEntityYOffset = value;
        return this.followingEntityYOffset;
    }

    boolean hasTailKey() {
        return this.hasTailKey;
    }

    boolean hasTailKey(boolean value) {
        this.hasTailKey = value;
        return this.hasTailKey;
    }

    int bowWowState() {
        return this.bowWowState;
    }

    int bowWowState(int value) {
        this.bowWowState = value;
        return this.bowWowState;
    }

    int richardSpokenFlag() {
        return this.richardSpokenFlag;
    }

    int richardSpokenFlag(int value) {
        this.richardSpokenFlag = value;
        return this.richardSpokenFlag;
    }

    int followingLinkDirection() {
        return this.followingLinkDirection;
    }

    int followingLinkDirection(int value) {
        this.followingLinkDirection = value;
        return this.followingLinkDirection;
    }

    int followingLinkZ() {
        return this.followingLinkZ;
    }

    int followingLinkZ(int value) {
        this.followingLinkZ = value;
        return this.followingLinkZ;
    }

    int playerSwordLevel() {
        return this.playerSwordLevel;
    }

    int playerSwordLevel(int value) {
        this.playerSwordLevel = value;
        return this.playerSwordLevel;
    }

    int playerBraceletLevel() {
        return this.playerBraceletLevel;
    }

    int playerBraceletLevel(int value) {
        this.playerBraceletLevel = value;
        return this.playerBraceletLevel;
    }

    LinkPositionHistory followingLinkPositionHistory() {
        return this.followingLinkPositionHistory;
    }

    int houseNpcTradeItem() {
        return this.houseNpcTradeItem;
    }

    int houseNpcTradeItem(int value) {
        this.houseNpcTradeItem = value;
        return this.houseNpcTradeItem;
    }

    int followingLinkX() {
        return this.followingLinkX;
    }

    int followingLinkX(int value) {
        this.followingLinkX = value;
        return this.followingLinkX;
    }

    int followingLinkY() {
        return this.followingLinkY;
    }

    int followingLinkY(int value) {
        this.followingLinkY = value;
        return this.followingLinkY;
    }

    int entityDefaultMusicTrack() {
        return this.entityDefaultMusicTrack;
    }

    int entityDefaultMusicTrack(int value) {
        this.entityDefaultMusicTrack = value;
        return this.entityDefaultMusicTrack;
    }
}
