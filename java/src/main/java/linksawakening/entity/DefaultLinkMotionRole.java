package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Delegates motion operations to their state-owning ROM implementations. */
final class DefaultLinkMotionRole implements LinkMotionRole {
    private final LinkGroundMotion linkGroundMotion;
    private final LinkStateMachine linkStateMachine;

    DefaultLinkMotionRole(LinkAssembly assembly) {
        linkGroundMotion = assembly.linkGroundMotion.get();
        linkStateMachine = assembly.linkStateMachine.get();
    }

    public boolean debugNoClip() {
        return linkStateMachine.accessDebugNoClip();
    }

    public boolean isSwimming() {
        return linkStateMachine.accessIsSwimming();
    }

    public boolean isDiving() {
        return linkStateMachine.accessIsDiving();
    }

    public int romPhysicsModifier() {
        return linkStateMachine.accessRomPhysicsModifier();
    }

    public int pegasusBootsChargeMeter() {
        return linkStateMachine.accessPegasusBootsChargeMeter();
    }

    public int pegasusBootsCollisionCountdown() {
        return linkStateMachine.accessPegasusBootsCollisionCountdown();
    }

    public int pegasusBootsCollisionPosX() {
        return linkStateMachine.accessPegasusBootsCollisionPosX();
    }

    public int pegasusBootsCollisionPosY() {
        return linkStateMachine.accessPegasusBootsCollisionPosY();
    }

    public void setGelClinging(boolean clinging, int worldFrame) {
        linkStateMachine.accessSetGelClinging(clinging, worldFrame);
    }

    public void update() {
        linkGroundMotion.accessUpdate();
    }

    public boolean grassGroundEffectActive() {
        return linkGroundMotion.accessGrassGroundEffectActive();
    }

    public void clearGroundEffects() {
        linkGroundMotion.accessClearGroundEffects();
    }

    public boolean consumeRevolvingDoorTransition() {
        return linkStateMachine.accessConsumeRevolvingDoorTransition();
    }

    public boolean shouldApplyGroundMotion() {
        return linkGroundMotion.accessShouldApplyGroundMotion();
    }

    public void setDebugNoClip(boolean enabled) {
        linkStateMachine.accessSetDebugNoClip(enabled);
    }

    public void resetTransientStateForDebug() {
        linkStateMachine.accessResetTransientStateForDebug();
    }

    public void finishRoomScroll(boolean downward) {
        linkStateMachine.accessFinishRoomScroll(downward);
    }

    public void writeRomInteractiveMotionBlock(int value) {
        linkStateMachine.accessWriteRomInteractiveMotionBlock(value);
    }

    public int romInteractiveMotionBlockValue() {
        return linkStateMachine.accessRomInteractiveMotionBlockValue();
    }

    public int romSlowWalkingSpeed() {
        return linkStateMachine.accessRomSlowWalkingSpeed();
    }

    public int romItemUsageContext() {
        return linkStateMachine.accessRomItemUsageContext();
    }

    public void blockNextRomMotionFrame() {
        linkStateMachine.accessBlockNextRomMotionFrame();
    }

    public void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        linkStateMachine.accessInitializeRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    public void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        linkStateMachine.accessSetRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    public void setRomPhysicsModifier(int modifier) {
        linkStateMachine.accessSetRomPhysicsModifier(modifier);
    }

    public ScreenShakeRequest consumePegasusScreenShakeRequest() {
        return linkStateMachine.accessConsumePegasusScreenShakeRequest();
    }

    public void resetPegasusBoots() {
        linkStateMachine.accessResetPegasusBoots();
    }
}
