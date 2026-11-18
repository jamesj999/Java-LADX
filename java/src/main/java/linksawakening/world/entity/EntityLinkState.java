package linksawakening.world.entity;

/** Link-relative input, cached motion, and interaction gates. */
final class EntityLinkState {
    private int followingLinkZ;
    private int followingLinkDirection;
    private int lastRomLinkDirection;
    private int followingEntityYOffset;
    private int linkAttackStepAnimationCountdown;
    private boolean actionButtonsHeld;
    private boolean actionButtonAHeld;
    private boolean actionButtonBHeld;
    private boolean joypadHeld;
    private boolean dialogActive;
    private boolean inventoryAppearing;
    private int dialogCooldown;
    private int windowY = 0x80;
    private int dialogAskSelectionIndex;
    private int linkPressedButtonsMask;
    private int linkItemA;
    private int linkItemB;
    private boolean linkInventoryConfigured;
    private boolean powerBraceletButtonHeld;
    private boolean fastLiftTransition;
    private boolean bombButtonHeld;
    private boolean runningWithPegasusBoots;
    private int linkPlayingOcarinaCountdown;
    private int ocarinaSongFlags;
    private int selectedSongIndex;
    private int ocarinaAnimationCounter;
    private int ocarinaAnimationPhase;
    private int currentLinkVelocityZ;
    private int currentLinkSpeedX;
    private int currentLinkSpeedY;
    private int currentLinkFinalPositionX;
    private boolean currentLinkFinalPositionXConfigured;

    boolean entityTimersInteractive() {
        return !dialogActive && !inventoryAppearing && linkPlayingOcarinaCountdown == 0;
    }

    int readFollowingLinkZ() {
        return followingLinkZ;
    }

    int writeFollowingLinkZ(int value) {
        followingLinkZ = value;
        return followingLinkZ;
    }

    int readFollowingLinkDirection() {
        return followingLinkDirection;
    }

    int writeFollowingLinkDirection(int value) {
        followingLinkDirection = value;
        return followingLinkDirection;
    }

    int readLastRomLinkDirection() {
        return lastRomLinkDirection;
    }

    int writeLastRomLinkDirection(int value) {
        lastRomLinkDirection = value;
        return lastRomLinkDirection;
    }

    int readFollowingEntityYOffset() {
        return followingEntityYOffset;
    }

    int writeFollowingEntityYOffset(int value) {
        followingEntityYOffset = value;
        return followingEntityYOffset;
    }

    int readLinkAttackStepAnimationCountdown() {
        return linkAttackStepAnimationCountdown;
    }

    int writeLinkAttackStepAnimationCountdown(int value) {
        linkAttackStepAnimationCountdown = value;
        return linkAttackStepAnimationCountdown;
    }

    boolean readActionButtonsHeld() {
        return actionButtonsHeld;
    }

    boolean writeActionButtonsHeld(boolean value) {
        actionButtonsHeld = value;
        return actionButtonsHeld;
    }

    boolean readActionButtonAHeld() {
        return actionButtonAHeld;
    }

    boolean writeActionButtonAHeld(boolean value) {
        actionButtonAHeld = value;
        return actionButtonAHeld;
    }

    boolean readActionButtonBHeld() {
        return actionButtonBHeld;
    }

    boolean writeActionButtonBHeld(boolean value) {
        actionButtonBHeld = value;
        return actionButtonBHeld;
    }

    boolean readJoypadHeld() {
        return joypadHeld;
    }

    boolean writeJoypadHeld(boolean value) {
        joypadHeld = value;
        return joypadHeld;
    }

    boolean readDialogActive() {
        return dialogActive;
    }

    boolean writeDialogActive(boolean value) {
        dialogActive = value;
        return dialogActive;
    }

    boolean readInventoryAppearing() {
        return inventoryAppearing;
    }

    boolean writeInventoryAppearing(boolean value) {
        inventoryAppearing = value;
        return inventoryAppearing;
    }

    int readDialogCooldown() {
        return dialogCooldown;
    }

    int writeDialogCooldown(int value) {
        dialogCooldown = value;
        return dialogCooldown;
    }

    int readWindowY() {
        return windowY;
    }

    int writeWindowY(int value) {
        windowY = value;
        return windowY;
    }

    int readDialogAskSelectionIndex() {
        return dialogAskSelectionIndex;
    }

    int writeDialogAskSelectionIndex(int value) {
        dialogAskSelectionIndex = value;
        return dialogAskSelectionIndex;
    }

    int readLinkPressedButtonsMask() {
        return linkPressedButtonsMask;
    }

    int writeLinkPressedButtonsMask(int value) {
        linkPressedButtonsMask = value;
        return linkPressedButtonsMask;
    }

    int readLinkItemA() {
        return linkItemA;
    }

    int writeLinkItemA(int value) {
        linkItemA = value;
        return linkItemA;
    }

    int readLinkItemB() {
        return linkItemB;
    }

    int writeLinkItemB(int value) {
        linkItemB = value;
        return linkItemB;
    }

    boolean readLinkInventoryConfigured() {
        return linkInventoryConfigured;
    }

    boolean writeLinkInventoryConfigured(boolean value) {
        linkInventoryConfigured = value;
        return linkInventoryConfigured;
    }

    boolean readPowerBraceletButtonHeld() {
        return powerBraceletButtonHeld;
    }

    boolean writePowerBraceletButtonHeld(boolean value) {
        powerBraceletButtonHeld = value;
        return powerBraceletButtonHeld;
    }

    boolean readFastLiftTransition() {
        return fastLiftTransition;
    }

    boolean writeFastLiftTransition(boolean value) {
        fastLiftTransition = value;
        return fastLiftTransition;
    }

    boolean readBombButtonHeld() {
        return bombButtonHeld;
    }

    boolean writeBombButtonHeld(boolean value) {
        bombButtonHeld = value;
        return bombButtonHeld;
    }

    boolean readRunningWithPegasusBoots() {
        return runningWithPegasusBoots;
    }

    boolean writeRunningWithPegasusBoots(boolean value) {
        runningWithPegasusBoots = value;
        return runningWithPegasusBoots;
    }

    int readLinkPlayingOcarinaCountdown() {
        return linkPlayingOcarinaCountdown;
    }

    int writeLinkPlayingOcarinaCountdown(int value) {
        linkPlayingOcarinaCountdown = value;
        return linkPlayingOcarinaCountdown;
    }

    int readOcarinaSongFlags() {
        return ocarinaSongFlags;
    }

    int writeOcarinaSongFlags(int value) {
        ocarinaSongFlags = value;
        return ocarinaSongFlags;
    }

    int readSelectedSongIndex() {
        return selectedSongIndex;
    }

    int writeSelectedSongIndex(int value) {
        selectedSongIndex = value;
        return selectedSongIndex;
    }

    int readOcarinaAnimationCounter() {
        return ocarinaAnimationCounter;
    }

    int writeOcarinaAnimationCounter(int value) {
        ocarinaAnimationCounter = value;
        return ocarinaAnimationCounter;
    }

    int readOcarinaAnimationPhase() {
        return ocarinaAnimationPhase;
    }

    int writeOcarinaAnimationPhase(int value) {
        ocarinaAnimationPhase = value;
        return ocarinaAnimationPhase;
    }

    int readCurrentLinkVelocityZ() {
        return currentLinkVelocityZ;
    }

    int writeCurrentLinkVelocityZ(int value) {
        currentLinkVelocityZ = value;
        return currentLinkVelocityZ;
    }

    int readCurrentLinkSpeedX() {
        return currentLinkSpeedX;
    }

    int writeCurrentLinkSpeedX(int value) {
        currentLinkSpeedX = value;
        return currentLinkSpeedX;
    }

    int readCurrentLinkSpeedY() {
        return currentLinkSpeedY;
    }

    int writeCurrentLinkSpeedY(int value) {
        currentLinkSpeedY = value;
        return currentLinkSpeedY;
    }

    int readCurrentLinkFinalPositionX() {
        return currentLinkFinalPositionX;
    }

    int writeCurrentLinkFinalPositionX(int value) {
        currentLinkFinalPositionX = value;
        return currentLinkFinalPositionX;
    }

    boolean readCurrentLinkFinalPositionXConfigured() {
        return currentLinkFinalPositionXConfigured;
    }

    boolean writeCurrentLinkFinalPositionXConfigured(boolean value) {
        currentLinkFinalPositionXConfigured = value;
        return currentLinkFinalPositionXConfigured;
    }
}
