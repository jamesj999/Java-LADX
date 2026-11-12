package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Delegates motion operations to state owners used by ROM {@code ApplyLinkMotionState}. */
final class DefaultLinkMotionRole implements LinkMotionRole {
    private final LinkGroundMotion linkGroundMotion;
    private final LinkStateMachine linkStateMachine;

    DefaultLinkMotionRole(LinkAssembly assembly) {
        linkGroundMotion = assembly.linkGroundMotion.get();
        linkStateMachine = assembly.linkStateMachine.get();
    }

    public boolean debugNoClip() {
        return linkStateMachine.status().accessDebugNoClip();
    }

    public boolean isSwimming() {
        return linkStateMachine.status().accessIsSwimming();
    }

    public boolean isDiving() {
        return linkStateMachine.accessIsDiving();
    }

    public int romPhysicsModifier() {
        return linkStateMachine.roomPhysics().accessRomPhysicsModifier();
    }

    public int pegasusBootsChargeMeter() {
        return linkStateMachine.dash().accessPegasusBootsChargeMeter();
    }

    public int pegasusBootsCollisionCountdown() {
        return linkStateMachine.dash().accessPegasusBootsCollisionCountdown();
    }

    public int pegasusBootsCollisionPosX() {
        return linkStateMachine.dash().accessPegasusBootsCollisionPosX();
    }

    public int pegasusBootsCollisionPosY() {
        return linkStateMachine.dash().accessPegasusBootsCollisionPosY();
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
        return linkStateMachine.door().accessConsumeRevolvingDoorTransition();
    }

    public boolean shouldApplyGroundMotion() {
        return linkGroundMotion.accessShouldApplyGroundMotion();
    }

    public void setDebugNoClip(boolean enabled) {
        linkStateMachine.setDebugNoClip(enabled);
    }

    public void resetTransientStateForDebug() {
        linkStateMachine.resetTransientStateForDebug();
    }

    public void finishRoomScroll(boolean downward) {
        linkStateMachine.roomPhysics().finishRoomScroll(downward);
    }

    public void writeRomInteractiveMotionBlock(int value) {
        linkStateMachine.status().writeRomInteractiveMotionBlock(value);
    }

    public int romInteractiveMotionBlockValue() {
        return linkStateMachine.status().romInteractiveMotionBlockValue();
    }

    public int romSlowWalkingSpeed() {
        return linkStateMachine.status().slowWalkingSpeed();
    }

    public int romItemUsageContext() {
        return linkStateMachine.status().itemUsageContext();
    }

    public void blockNextRomMotionFrame() {
        linkStateMachine.status().blockNextRomMotionFrame();
    }

    public void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        linkStateMachine.roomPhysics().initializeRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    public void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        linkStateMachine.roomPhysics().setRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory);
    }

    public void setRomPhysicsModifier(int modifier) {
        linkStateMachine.roomPhysics().setRomPhysicsModifier(modifier);
    }

    public ScreenShakeRequest consumePegasusScreenShakeRequest() {
        return linkStateMachine.dash().consumePegasusScreenShakeRequest();
    }

    public void resetPegasusBoots() {
        linkStateMachine.dash().resetPegasusBoots();
    }
}
