package linksawakening.world.entity.motion;

import linksawakening.world.entity.LinkPositionHistory;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Inputs for FollowingNpcMotion.advance. */
public record FollowingNpcFrame(
        RoomEntity entity,
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        int linkEntityZ,
        int linkDirection,
        int entityYOffset,
        LinkPositionHistory linkPositionHistory,
        RoomEntityBackgroundCollision backgroundCollision) {}
