package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Position operations from LinkMotionDefault and its related ROM routines. */
public interface LinkPositionRole {
    boolean consumeDoorWarp();

    int romCollisionType();

    int romCollisionProbePixelX();

    int romCollisionProbePixelY();

    int romSpeedX();

    int romSpeedY();

    int romSwordCollisionY();

    void setPixelPosition(int pixelX, int pixelY);

    void setRoomScrollEntryPixelPosition(int pixelX, int pixelY);

    void setRoomEntryPixelPosition(int pixelX, int pixelY);

    void markRoomEntryPosition();

    int roomEntryPixelX();

    int roomEntryPixelY();

    int roomEntryRomPositionX();

    int roomEntryRomPositionY();

    void setRoomEntryRomPosition(int romPositionX, int romPositionY);

    void applyRomSpeed(int speedX, int speedY);

    void applyRomSpeedX(int speedX);

    void applyRomFinalPosition(int speedX, int speedY);

    void captureRomFinalPosition();

    void restoreRomFinalPosition();

    void restoreRomFinalPositionX();

    void clearRomPositionIncrement();

    int pixelX();

    int pixelY();

    int romEntityX();

    int romFinalPositionX();

    int romEntityY();
}
