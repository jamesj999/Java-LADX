package linksawakening.world.room;

/** Across-room puzzle flags plus the current room-load preparation slot. */
final class RoomPuzzleState {
    int switchButtonPressed;
    int dungeonTitleMessageCountdown;
    int tailCaveResumeMusicTrack = 5;
    // Preparation alias only; installed generations own the actual state object.
    PerRoom current = new PerRoom();

    void beginRoom() {
        current = new PerRoom();
    }

    static final class PerRoom {
        int roomTriggerCount;
        int activeRoomEvent;
        boolean roomEventEffectExecuted;
        boolean thrownAtDoorTriggerResolved;
        int roomEventChestRevealCountdown;
        int roomEventStairRevealCountdown;
        int staircaseState;
        int staircaseX;
        int staircaseY;
        int switchButtonPressCounter;
        int indoorKeyDoorAnimationCountdown;
        int indoorKeyDoorDirection = -1;
        int indoorKeyDoorLocation = -1;
        boolean indoorBossDoorOpening;
        int pushBlockContactTicks;
        int indoorPotPullFrames;
        int pushedBlockMotionFrames;
        int pushedBlockSourceLocation = -1;
        int pushedBlockDestinationLocation = -1;
        int pushedBlockDirection = -1;
        int pushedBlockEntitySlot = -1;
        boolean pushedBlocksTriggerEffectPending;
        int pendingShutterCloseMask;
        int closedShutterMask;
        int shutterCloseDirection = -1;
        int shutterCloseTicks;
        int shutterCloseLocation = -1;
        boolean shutterCloseStarted;
        final java.util.Map<Integer, Integer> shutterTileOverrides = new java.util.HashMap<>();
        int tailCaveKeyholeCountdown;
        boolean tailCaveFinalMotionBlockPending;
        int nextWorldMusicTrackCountdown;

        void configureStaircase(int location) {
            if (location < 0) {
                staircaseState = RoomSessionRules.STAIRCASE_NONE;
                staircaseX = staircaseY = 0;
                return;
            }
            staircaseState = RoomSessionRules.STAIRCASE_INACTIVE;
            staircaseY = (location & 0xF0) + 0x10;
            staircaseX = ((location & 0x0F) << 4) + 0x08;
        }
    }
}
