package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.LinkPositionHistory;

final class RoomFollowerState {
    final LinkPositionHistory followingLinkPositionHistory = new LinkPositionHistory();
    FollowingNpcState followingNpcState = FollowingNpcState.none();
    int followingLinkX = 8;
    int followingLinkY = 16;
    int followingLinkZ;
    int followingEntityYOffset;
    int followingLinkDirection;
    boolean followingNpcRoomNeedsSync;
    boolean hasBirdKey;
    boolean hasTailKey;
    int bowWowState;
    int richardSpokenFlag;
    /** WRAM wTarinFlag; persisted NPC/world progression, not player inventory state. */
    int tarinFlag;
    /** WRAM wShieldLevel retained for room-load-time palette selection. */
    int playerShieldLevel;

    int playerSwordLevel;
    int playerBraceletLevel;
    int houseNpcTradeItem;
    int entityDefaultMusicTrack = 5;
}
