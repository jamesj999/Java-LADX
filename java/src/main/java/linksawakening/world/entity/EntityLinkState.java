package linksawakening.world.entity;

/** Link-relative input, cached motion, and interaction gates. */
public final class EntityLinkState {
    int followingLinkZ;
    int followingLinkDirection;
    int lastRomLinkDirection;
    int followingEntityYOffset;
    public int linkAttackStepAnimationCountdown;
    public boolean actionButtonsHeld;
    public boolean actionButtonAHeld;
    public boolean actionButtonBHeld;
    public boolean joypadHeld;
    public boolean dialogActive;
    public boolean inventoryAppearing;
    public int dialogCooldown;
    public int windowY = 0x80;
    int dialogAskSelectionIndex;
    public int linkPressedButtonsMask;
    public int linkItemA;
    public int linkItemB;
    boolean linkInventoryConfigured;
    boolean powerBraceletButtonHeld;
    boolean fastLiftTransition;
    boolean bombButtonHeld;
    public boolean runningWithPegasusBoots;
    public int linkPlayingOcarinaCountdown;
    int ocarinaSongFlags;
    int selectedSongIndex;
    int ocarinaAnimationCounter;
    int ocarinaAnimationPhase;
    int currentLinkVelocityZ;
    public int currentLinkSpeedX;
    public int currentLinkSpeedY;
    public int currentLinkFinalPositionX;
    public boolean currentLinkFinalPositionXConfigured;
}
