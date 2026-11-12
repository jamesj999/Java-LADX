package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.SwordPickupRewardEvent;

final class RoomEntityStateView implements RoomEntityState {
    private final Dependencies dependencies;

    RoomEntityStateView(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    public void resetGuardianAcornCounterOnHurt() {
        var runtime = dependencies.entityBridge().entityRuntime;
        var counters = runtime == null
                ? dependencies.entityFrame().enemyDropCounters
                : runtime.inspection().enemyDropCounters();
        dependencies.entityFrame().enemyDropCounters = EnemyDropResolver.resetGuardianAcornCounter(counters);
        if (runtime != null) runtime.configuration().setEnemyDropCounters(dependencies.entityFrame().enemyDropCounters);
    }

    public void setEntityActionButtonsHeld(boolean actionButtonsHeld) {
        dependencies.entityInputs().setEntityActionButtonsHeld(actionButtonsHeld);
    }

    public boolean gelClingingToLink() {
        return dependencies.entityBridge().entityRuntime != null
                && dependencies.entityBridge().entityRuntime.gelClingingToLink();
    }

    public void setEntityDialogActive(boolean active) {
        dependencies.entityInputs().setEntityDialogActive(active);
    }

    public void setEntityTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        dependencies.entityInputs().setEntityTalkState(inventoryAppearing, dialogCooldown, windowY);
    }

    public void setEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        dependencies.entityInputs().setEntityActionButtonsHeld(actionButtonAHeld, actionButtonBHeld);
    }

    public void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        dependencies.entityInputs().setToadstoolPlayerState(hasToadstool, magicPowderCount);
    }

    public void setEntityScreenShakeCountdown(int countdown) {
        if (dependencies.entityBridge().entityRuntime != null)
            dependencies.entityBridge().entityRuntime.configuration().setScreenShakeCountdown(countdown);
    }

    public void setSecretSeashellPegasusCollisionState(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        dependencies
                .entityInputs()
                .setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY);
    }

    public void setEntityPowerBraceletButtonHeld(boolean buttonHeld) {
        dependencies.entityInputs().setEntityPowerBraceletButtonHeld(buttonHeld);
    }

    public void setEntityBombButtonHeld(boolean buttonHeld) {
        dependencies.entityInputs().setEntityBombButtonHeld(buttonHeld);
    }

    public void setEntityAttackStepAnimationCountdown(int countdown) {
        dependencies.entityInputs().setEntityAttackStepAnimationCountdown(countdown);
    }

    public void advanceBoomerangSoundClock(boolean worldGameplay) {
        dependencies.entityBridge().advanceBoomerangSoundClock(worldGameplay);
    }

    public boolean deferContactRecoil(int slot, int linkX, int linkY) {
        return dependencies.entityBridge().combat.deferContactRecoil(slot, linkX, linkY);
    }

    public List<LinkAttackClearRequest> consumeLinkAttackClearRequests() {
        return dependencies.entityBridge().outbox.consumeLinkAttackClearRequests();
    }

    public boolean gotItemPresentationActive() {
        return dependencies.entityBridge().outbox.gotItemPresentationActive();
    }

    public boolean powerUpPickupSequenceActive() {
        return dependencies.entityBridge().outbox.powerUpPickupSequenceActive();
    }

    public boolean swordPickupSequenceActive() {
        return dependencies.entityBridge().outbox.swordPickupSequenceActive();
    }

    public List<SwordPickupRewardEvent> consumeSwordPickupRewards() {
        return dependencies.entityBridge().outbox.consumeSwordPickupRewards();
    }

    public List<DialogRequest> consumeEntityDialogRequests() {
        return dependencies.entityBridge().outbox.consumeEntityDialogRequests();
    }

    public void tickGameplayVBlank() {
        dependencies.entityBridge().tickGameplayVBlank();
    }

    public int clearEntity(int slot) {
        return dependencies.entityBridge().combat.clearEntity(slot);
    }

    public int entityDefaultMusicTrack() {
        return dependencies.followers().entityDefaultMusicTrack;
    }

    public int entityHealth(int slot) {
        return dependencies.roomSessionViews().entityHealth(slot);
    }

    public void setColorShellSoundSink(GameplaySoundSink soundSink) {
        dependencies.roomSessionViews().setColorShellSoundSink(soundSink);
    }

    record Dependencies(
            RoomEntityBridge entityBridge,
            RoomEntityFrameState entityFrame,
            RoomEntityInputs entityInputs,
            RoomFollowerState followers,
            RoomSessionViews roomSessionViews) {}
}
