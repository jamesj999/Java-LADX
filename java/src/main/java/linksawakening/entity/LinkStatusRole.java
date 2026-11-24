package linksawakening.entity;

import linksawakening.entity.Link.WaterSplash;

/** Status operations from LinkMotionDefault and its related ROM routines. */
public interface LinkStatusRole {
    void setCollisionIgnoreFrames(int frames);

    boolean ignoringContactCollisions();

    void setNormalRecoilRoomBoundaries(boolean normalBoundaries);

    boolean recoveryBlocksInput();

    boolean consumePitWarp();

    boolean pitWarpPending();

    WaterSplash consumeWaterSplash();

    boolean isPassingOut();

    boolean gameOverScreenPending();

    int gameOverStage();

    int gameOverCountdown();

    void presentGameOverStage(int stage, int countdown);

    int passOutCountdown();

    int passOutBgPaletteEffectAddress();

    int passOutPaletteEffect();

    int passOutPaletteFrames();

    void finishGameOver();

    void beginPassOut();

    void presentPassOut(int countdown, int animationState, int paletteEffect);

    boolean isCollisionDamageImmune();
}
