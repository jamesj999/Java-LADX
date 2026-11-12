package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

final class RoomPuzzleState {
    /** WRAM wC1A2; ResetRoomVariables clears the room trigger counter. */
    int roomTriggerCount;

    int activeRoomEvent;
    /** wHasInstrument1..: bit 0 records each dungeon miniboss defeat. */
    boolean roomEventEffectExecuted;

    boolean thrownAtDoorTriggerResolved;
    int roomEventChestRevealCountdown;
    int roomEventStairRevealCountdown;
    int staircaseState;
    int staircaseX;
    int staircaseY;
    int switchButtonPressCounter;
    int switchButtonPressed;
    int indoorKeyDoorAnimationCountdown;
    int indoorKeyDoorDirection = -1;
    int indoorKeyDoorLocation = -1;
    boolean indoorBossDoorOpening;
    /** wC191 for OBJECT_PUSHABLE_BLOCK's sustained collision branch. */
    int pushBlockContactTicks;

    int indoorPotPullFrames;
    int pushedBlockMotionFrames;
    int pushedBlockSourceLocation = -1;
    int pushedBlockDestinationLocation = -1;
    int pushedBlockDirection = -1;
    int pushedBlockEntitySlot = -1;
    /** Defers trigger-$07's common effect until the frame after settlement. */
    boolean pushedBlocksTriggerEffectPending;

    int pendingShutterCloseMask;
    int closedShutterMask;
    int shutterCloseDirection = -1;
    int shutterCloseTicks;
    int shutterCloseLocation = -1;
    boolean shutterCloseStarted;
    final java.util.Map<Integer, Integer> shutterTileOverrides = new java.util.HashMap<>();
    int dungeonTitleMessageCountdown;
    int tailCaveKeyholeCountdown;
    boolean tailCaveFinalMotionBlockPending;
    int nextWorldMusicTrackCountdown;
    int tailCaveResumeMusicTrack = 5;
}
