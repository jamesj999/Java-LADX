package linksawakening.world.room;

/** ROM puzzle/title/music registers that survive room replacement; PerRoom is owned by RoomContents. */
final class RoomPuzzleState {
    private int switchButtonPressed;
    private int dungeonTitleMessageCountdown;
    private int tailCaveResumeMusicTrack = 5;

    static final class PerRoom {
        private int telephoneLeafHint;

        int nextTelephoneLeafHint() {
            telephoneLeafHint = (telephoneLeafHint + 1) % 3;
            return telephoneLeafHint;
        }

        private int roomTriggerCount;
        private int activeRoomEvent;
        private boolean roomEventEffectExecuted;
        private boolean thrownAtDoorTriggerResolved;
        private int roomEventChestRevealCountdown;
        private int roomEventStairRevealCountdown;
        private int staircaseState;
        private int staircaseX;
        private int staircaseY;
        private int switchButtonPressCounter;
        private int indoorKeyDoorAnimationCountdown;
        private int indoorKeyDoorDirection = -1;
        private int indoorKeyDoorLocation = -1;
        private boolean indoorBossDoorOpening;
        private int pushBlockContactTicks;
        private int indoorPotPullFrames;
        private int pushedBlockMotionFrames;
        private int pushedBlockSourceLocation = -1;
        private int pushedBlockDestinationLocation = -1;
        private int pushedBlockDirection = -1;
        private int pushedBlockEntitySlot = -1;
        private boolean pushedBlocksTriggerEffectPending;
        private int pendingShutterCloseMask;
        private int closedShutterMask;
        private int shutterCloseDirection = -1;
        private int shutterCloseTicks;
        private int shutterCloseLocation = -1;
        private boolean shutterCloseStarted;
        private int pendingShutterOpenMask;
        private boolean shutterOpening;
        private int tailCaveKeyholeCountdown;
        private boolean tailCaveFinalMotionBlockPending;
        private int nextWorldMusicTrackCountdown;

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

        int closedShutterMask() {
            return this.closedShutterMask;
        }

        int closedShutterMask(int value) {
            this.closedShutterMask = value;
            return this.closedShutterMask;
        }

        int orClosedShutterMask(int value) {
            this.closedShutterMask |= value;
            return this.closedShutterMask;
        }

        int pendingShutterCloseMask() {
            return this.pendingShutterCloseMask;
        }

        int pendingShutterCloseMask(int value) {
            this.pendingShutterCloseMask = value;
            return this.pendingShutterCloseMask;
        }

        int andPendingShutterCloseMask(int value) {
            this.pendingShutterCloseMask &= value;
            return this.pendingShutterCloseMask;
        }

        int shutterCloseTicks() {
            return this.shutterCloseTicks;
        }

        int shutterCloseTicks(int value) {
            this.shutterCloseTicks = value;
            return this.shutterCloseTicks;
        }

        int incrementShutterCloseTicks() {
            return ++this.shutterCloseTicks;
        }

        int shutterCloseDirection() {
            return this.shutterCloseDirection;
        }

        int shutterCloseDirection(int value) {
            this.shutterCloseDirection = value;
            return this.shutterCloseDirection;
        }

        int shutterCloseLocation() {
            return this.shutterCloseLocation;
        }

        int shutterCloseLocation(int value) {
            this.shutterCloseLocation = value;
            return this.shutterCloseLocation;
        }

        boolean shutterCloseStarted() {
            return this.shutterCloseStarted;
        }

        boolean shutterCloseStarted(boolean value) {
            this.shutterCloseStarted = value;
            return this.shutterCloseStarted;
        }

        int pendingShutterOpenMask() {
            return pendingShutterOpenMask;
        }

        void pendingShutterOpenMask(int value) {
            pendingShutterOpenMask = value;
        }

        boolean shutterOpening() {
            return shutterOpening;
        }

        void shutterOpening(boolean value) {
            shutterOpening = value;
        }

        int roomTriggerCount() {
            return this.roomTriggerCount;
        }

        int roomTriggerCount(int value) {
            this.roomTriggerCount = value;
            return this.roomTriggerCount;
        }

        int activeRoomEvent() {
            return this.activeRoomEvent;
        }

        int activeRoomEvent(int value) {
            this.activeRoomEvent = value;
            return this.activeRoomEvent;
        }

        boolean roomEventEffectExecuted() {
            return this.roomEventEffectExecuted;
        }

        boolean roomEventEffectExecuted(boolean value) {
            this.roomEventEffectExecuted = value;
            return this.roomEventEffectExecuted;
        }

        boolean thrownAtDoorTriggerResolved() {
            return this.thrownAtDoorTriggerResolved;
        }

        boolean thrownAtDoorTriggerResolved(boolean value) {
            this.thrownAtDoorTriggerResolved = value;
            return this.thrownAtDoorTriggerResolved;
        }

        int roomEventChestRevealCountdown() {
            return this.roomEventChestRevealCountdown;
        }

        int roomEventChestRevealCountdown(int value) {
            this.roomEventChestRevealCountdown = value;
            return this.roomEventChestRevealCountdown;
        }

        int postDecrementRoomEventChestRevealCountdown() {
            return this.roomEventChestRevealCountdown--;
        }

        int roomEventStairRevealCountdown() {
            return this.roomEventStairRevealCountdown;
        }

        int roomEventStairRevealCountdown(int value) {
            this.roomEventStairRevealCountdown = value;
            return this.roomEventStairRevealCountdown;
        }

        int postDecrementRoomEventStairRevealCountdown() {
            return this.roomEventStairRevealCountdown--;
        }

        int staircaseState() {
            return this.staircaseState;
        }

        int staircaseState(int value) {
            this.staircaseState = value;
            return this.staircaseState;
        }

        int staircaseX() {
            return this.staircaseX;
        }

        int staircaseX(int value) {
            this.staircaseX = value;
            return this.staircaseX;
        }

        int staircaseY() {
            return this.staircaseY;
        }

        int staircaseY(int value) {
            this.staircaseY = value;
            return this.staircaseY;
        }

        int switchButtonPressCounter() {
            return this.switchButtonPressCounter;
        }

        int switchButtonPressCounter(int value) {
            this.switchButtonPressCounter = value;
            return this.switchButtonPressCounter;
        }

        int indoorKeyDoorAnimationCountdown() {
            return this.indoorKeyDoorAnimationCountdown;
        }

        int indoorKeyDoorAnimationCountdown(int value) {
            this.indoorKeyDoorAnimationCountdown = value;
            return this.indoorKeyDoorAnimationCountdown;
        }

        int postDecrementIndoorKeyDoorAnimationCountdown() {
            return this.indoorKeyDoorAnimationCountdown--;
        }

        int indoorKeyDoorDirection() {
            return this.indoorKeyDoorDirection;
        }

        int indoorKeyDoorDirection(int value) {
            this.indoorKeyDoorDirection = value;
            return this.indoorKeyDoorDirection;
        }

        int indoorKeyDoorLocation() {
            return this.indoorKeyDoorLocation;
        }

        int indoorKeyDoorLocation(int value) {
            this.indoorKeyDoorLocation = value;
            return this.indoorKeyDoorLocation;
        }

        boolean indoorBossDoorOpening() {
            return this.indoorBossDoorOpening;
        }

        boolean indoorBossDoorOpening(boolean value) {
            this.indoorBossDoorOpening = value;
            return this.indoorBossDoorOpening;
        }

        int pushBlockContactTicks() {
            return this.pushBlockContactTicks;
        }

        int pushBlockContactTicks(int value) {
            this.pushBlockContactTicks = value;
            return this.pushBlockContactTicks;
        }

        int postIncrementPushBlockContactTicks() {
            return this.pushBlockContactTicks++;
        }

        int indoorPotPullFrames() {
            return this.indoorPotPullFrames;
        }

        int indoorPotPullFrames(int value) {
            this.indoorPotPullFrames = value;
            return this.indoorPotPullFrames;
        }

        int postIncrementIndoorPotPullFrames() {
            return this.indoorPotPullFrames++;
        }

        int pushedBlockMotionFrames() {
            return this.pushedBlockMotionFrames;
        }

        int pushedBlockMotionFrames(int value) {
            this.pushedBlockMotionFrames = value;
            return this.pushedBlockMotionFrames;
        }

        int postDecrementPushedBlockMotionFrames() {
            return this.pushedBlockMotionFrames--;
        }

        int pushedBlockSourceLocation() {
            return this.pushedBlockSourceLocation;
        }

        int pushedBlockSourceLocation(int value) {
            this.pushedBlockSourceLocation = value;
            return this.pushedBlockSourceLocation;
        }

        int pushedBlockDestinationLocation() {
            return this.pushedBlockDestinationLocation;
        }

        int pushedBlockDestinationLocation(int value) {
            this.pushedBlockDestinationLocation = value;
            return this.pushedBlockDestinationLocation;
        }

        int pushedBlockDirection() {
            return this.pushedBlockDirection;
        }

        int pushedBlockDirection(int value) {
            this.pushedBlockDirection = value;
            return this.pushedBlockDirection;
        }

        int pushedBlockEntitySlot() {
            return this.pushedBlockEntitySlot;
        }

        int pushedBlockEntitySlot(int value) {
            this.pushedBlockEntitySlot = value;
            return this.pushedBlockEntitySlot;
        }

        boolean pushedBlocksTriggerEffectPending() {
            return this.pushedBlocksTriggerEffectPending;
        }

        boolean pushedBlocksTriggerEffectPending(boolean value) {
            this.pushedBlocksTriggerEffectPending = value;
            return this.pushedBlocksTriggerEffectPending;
        }

        int tailCaveKeyholeCountdown() {
            return this.tailCaveKeyholeCountdown;
        }

        int tailCaveKeyholeCountdown(int value) {
            this.tailCaveKeyholeCountdown = value;
            return this.tailCaveKeyholeCountdown;
        }

        boolean tailCaveFinalMotionBlockPending() {
            return this.tailCaveFinalMotionBlockPending;
        }

        boolean tailCaveFinalMotionBlockPending(boolean value) {
            this.tailCaveFinalMotionBlockPending = value;
            return this.tailCaveFinalMotionBlockPending;
        }

        int nextWorldMusicTrackCountdown() {
            return this.nextWorldMusicTrackCountdown;
        }

        int nextWorldMusicTrackCountdown(int value) {
            this.nextWorldMusicTrackCountdown = value;
            return this.nextWorldMusicTrackCountdown;
        }

        int postDecrementNextWorldMusicTrackCountdown() {
            return this.nextWorldMusicTrackCountdown--;
        }
    }

    int dungeonTitleMessageCountdown() {
        return this.dungeonTitleMessageCountdown;
    }

    int dungeonTitleMessageCountdown(int value) {
        this.dungeonTitleMessageCountdown = value;
        return this.dungeonTitleMessageCountdown;
    }

    int decrementDungeonTitleMessageCountdown() {
        return --this.dungeonTitleMessageCountdown;
    }

    int switchButtonPressed() {
        return this.switchButtonPressed;
    }

    int switchButtonPressed(int value) {
        this.switchButtonPressed = value;
        return this.switchButtonPressed;
    }

    int tailCaveResumeMusicTrack() {
        return this.tailCaveResumeMusicTrack;
    }

    int tailCaveResumeMusicTrack(int value) {
        this.tailCaveResumeMusicTrack = value;
        return this.tailCaveResumeMusicTrack;
    }
}
