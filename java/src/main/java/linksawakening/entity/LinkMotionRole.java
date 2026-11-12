package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Motion operations from LinkMotionDefault and its related ROM routines. */
public interface LinkMotionRole {
    boolean debugNoClip();

    boolean isSwimming();

    boolean isDiving();

    int romPhysicsModifier();

    int pegasusBootsChargeMeter();

    int pegasusBootsCollisionCountdown();

    int pegasusBootsCollisionPosX();

    int pegasusBootsCollisionPosY();

    void setGelClinging(boolean clinging, int worldFrame);

    void update();

    boolean grassGroundEffectActive();

    void clearGroundEffects();

    boolean consumeRevolvingDoorTransition();

    boolean shouldApplyGroundMotion();

    void setDebugNoClip(boolean enabled);

    void resetTransientStateForDebug();

    void finishRoomScroll(boolean downward);

    void writeRomInteractiveMotionBlock(int value);

    int romInteractiveMotionBlockValue();

    int romSlowWalkingSpeed();

    int romItemUsageContext();

    void blockNextRomMotionFrame();

    void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory);

    void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory);

    void setRomPhysicsModifier(int modifier);

    ScreenShakeRequest consumePegasusScreenShakeRequest();

    void resetPegasusBoots();
}
