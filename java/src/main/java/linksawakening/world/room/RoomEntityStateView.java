package linksawakening.world.room;

import java.util.List;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.SwordPickupRewardEvent;

final class RoomEntityStateView implements RoomEntityState {
    private final Dependencies dependencies;

    RoomEntityStateView(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public void resetGuardianAcornCounterOnHurt() {
        dependencies.session().resetGuardianAcornCounterOnHurt();
    }

    @Override
    public void setEntityActionButtonsHeld(boolean actionButtonsHeld) {
        dependencies.session().setEntityActionButtonsHeld(actionButtonsHeld);
    }

    @Override
    public boolean gelClingingToLink() {
        return dependencies.session().gelClingingToLink();
    }

    @Override
    public void setEntityDialogActive(boolean active) {
        dependencies.session().setEntityDialogActive(active);
    }

    @Override
    public void setEntityTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        dependencies.session().setEntityTalkState(inventoryAppearing, dialogCooldown, windowY);
    }

    @Override
    public void setEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        dependencies.session().setEntityActionButtonsHeld(actionButtonAHeld, actionButtonBHeld);
    }

    @Override
    public void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        dependencies.session().setToadstoolPlayerState(hasToadstool, magicPowderCount);
    }

    @Override
    public void setEntityScreenShakeCountdown(int countdown) {
        dependencies.session().setEntityScreenShakeCountdown(countdown);
    }

    @Override
    public void setSecretSeashellPegasusCollisionState(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        dependencies
                .entityInputs()
                .setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY);
    }

    @Override
    public void setEntityPowerBraceletButtonHeld(boolean buttonHeld) {
        dependencies.session().setEntityPowerBraceletButtonHeld(buttonHeld);
    }

    @Override
    public void setEntityBombButtonHeld(boolean buttonHeld) {
        dependencies.session().setEntityBombButtonHeld(buttonHeld);
    }

    @Override
    public void setEntityAttackStepAnimationCountdown(int countdown) {
        dependencies.session().setEntityAttackStepAnimationCountdown(countdown);
    }

    @Override
    public void advanceBoomerangSoundClock(boolean worldGameplay) {
        dependencies.entityBridge().advanceBoomerangSoundClock(worldGameplay);
    }

    @Override
    public boolean deferContactRecoil(int slot, int linkX, int linkY) {
        return dependencies.session().deferContactRecoil(slot, linkX, linkY);
    }

    @Override
    public List<LinkAttackClearRequest> consumeLinkAttackClearRequests() {
        return dependencies.session().consumeLinkAttackClearRequests();
    }

    @Override
    public boolean gotItemPresentationActive() {
        return dependencies.session().gotItemPresentationActive();
    }

    @Override
    public boolean powerUpPickupSequenceActive() {
        return dependencies.session().powerUpPickupSequenceActive();
    }

    @Override
    public boolean swordPickupSequenceActive() {
        return dependencies.session().swordPickupSequenceActive();
    }

    @Override
    public List<SwordPickupRewardEvent> consumeSwordPickupRewards() {
        return dependencies.entityBridge().outbox.consumeSwordPickupRewards();
    }

    @Override
    public List<DialogRequest> consumeEntityDialogRequests() {
        return dependencies.session().consumeEntityDialogRequests();
    }

    @Override
    public void tickGameplayVBlank() {
        dependencies.entityBridge().tickGameplayVBlank();
    }

    @Override
    public int clearEntity(int slot) {
        return dependencies.session().clearEntity(slot);
    }

    @Override
    public int entityDefaultMusicTrack() {
        return dependencies.followers().entityDefaultMusicTrack;
    }

    @Override
    public int entityHealth(int slot) {
        return dependencies.session().entityHealth(slot);
    }

    @Override
    public void setColorShellSoundSink(GameplaySoundSink soundSink) {
        dependencies.roomSessionViews().setColorShellSoundSink(soundSink);
    }

    record Dependencies(
            RoomEntityBridge entityBridge,
            RoomEntityInputs entityInputs,
            RoomFollowerState followers,
            RoomSessionViews roomSessionViews,
            RoomSession session) {
        Dependencies {
            java.util.Objects.requireNonNull(entityBridge, "entityBridge");
            java.util.Objects.requireNonNull(entityInputs, "entityInputs");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
        }
    }
}
