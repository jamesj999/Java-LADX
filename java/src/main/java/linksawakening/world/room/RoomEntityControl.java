package linksawakening.world.room;

import java.util.List;
import java.util.Objects;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.SwordPickupRewardEvent;

final class RoomEntityControl implements RoomEntityState {
    private final Dependencies dependencies;

    RoomEntityControl(Dependencies dependencies) {
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies");
    }

    @Override
    public void resetGuardianAcornCounterOnHurt() {
        dependencies.entityBridge().resetGuardianAcornCounterOnHurt();
    }

    @Override
    public void setEntityActionButtonsHeld(boolean actionButtonsHeld) {
        dependencies.entityInputs().updateEntityActionButtonsHeld(actionButtonsHeld);
    }

    @Override
    public boolean gelClingingToLink() {
        return dependencies.entityBridge().gelClingingToLink();
    }

    @Override
    public void setEntityDialogActive(boolean active) {
        dependencies.entityInputs().updateEntityDialogActive(active);
    }

    @Override
    public void setEntityTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        dependencies.entityInputs().updateEntityTalkState(inventoryAppearing, dialogCooldown, windowY);
    }

    @Override
    public void setEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        dependencies.entityInputs().updateEntityActionButtonsHeld(actionButtonAHeld, actionButtonBHeld);
    }

    @Override
    public void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        dependencies.entityInputs().updateToadstoolPlayerState(hasToadstool, magicPowderCount);
    }

    @Override
    public void setEntityScreenShakeCountdown(int countdown) {
        dependencies.entityInputs().updateEntityScreenShakeCountdown(countdown);
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
        dependencies.entityInputs().updateEntityPowerBraceletButtonHeld(buttonHeld);
    }

    @Override
    public void setEntityBombButtonHeld(boolean buttonHeld) {
        dependencies.entityInputs().updateEntityBombButtonHeld(buttonHeld);
    }

    @Override
    public void setEntityAttackStepAnimationCountdown(int countdown) {
        dependencies.entityInputs().updateEntityAttackStepAnimationCountdown(countdown);
    }

    @Override
    public void advanceBoomerangSoundClock(boolean worldGameplay) {
        dependencies.entityBridge().advanceBoomerangSoundClock(worldGameplay);
    }

    @Override
    public boolean deferContactRecoil(int slot, int linkX, int linkY) {
        return dependencies.entityBridge().deferContactRecoil(slot, linkX, linkY);
    }

    @Override
    public List<LinkAttackClearRequest> consumeLinkAttackClearRequests() {
        return dependencies.entityBridge().consumeLinkAttackClearRequests();
    }

    @Override
    public boolean gotItemPresentationActive() {
        return dependencies.entityBridge().gotItemPresentationActive();
    }

    @Override
    public boolean powerUpPickupSequenceActive() {
        return dependencies.entityBridge().powerUpPickupSequenceActive();
    }

    @Override
    public boolean swordPickupSequenceActive() {
        return dependencies.entityBridge().swordPickupSequenceActive();
    }

    @Override
    public List<SwordPickupRewardEvent> consumeSwordPickupRewards() {
        return dependencies.entityBridge().outbox.consumeSwordPickupRewards();
    }

    @Override
    public List<DialogRequest> consumeEntityDialogRequests() {
        return dependencies.entityBridge().consumeEntityDialogRequests();
    }

    @Override
    public void tickGameplayVBlank() {
        dependencies.entityBridge().tickGameplayVBlank();
    }

    @Override
    public int clearEntity(int slot) {
        return dependencies.entityBridge().clearEntity(slot);
    }

    @Override
    public int entityDefaultMusicTrack() {
        return dependencies.followers().entityDefaultMusicTrack;
    }

    @Override
    public int entityHealth(int slot) {
        return dependencies.entityBridge().entityHealth(slot);
    }

    @Override
    public void setColorShellSoundSink(GameplaySoundSink soundSink) {
        dependencies.entityFrame().setColorShellSoundSink(soundSink);
    }

    record Dependencies(
            RoomEntityBridge entityBridge,
            RoomEntityInputs entityInputs,
            RoomFollowerState followers,
            RoomEntitySessionState entityFrame) {
        Dependencies {
            Objects.requireNonNull(entityBridge, "entityBridge");
            Objects.requireNonNull(entityInputs, "entityInputs");
            Objects.requireNonNull(followers, "followers");
            Objects.requireNonNull(entityFrame, "entityFrame");
        }
    }
}
