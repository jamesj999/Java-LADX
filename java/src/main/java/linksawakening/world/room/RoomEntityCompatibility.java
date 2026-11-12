package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.SwordPickupRewardEvent;

public interface RoomEntityCompatibility {
    RoomSession facade();
    /** @deprecated Use {@code entityState().resetGuardianAcornCounterOnHurt()}. */
    @Deprecated(forRemoval = true)
    default void resetGuardianAcornCounterOnHurt() {
        facade().entityState().resetGuardianAcornCounterOnHurt();
    }
    /** @deprecated Use {@code entityState().setEntityActionButtonsHeld()}. */
    @Deprecated(forRemoval = true)
    default void setEntityActionButtonsHeld(boolean actionButtonsHeld) {
        facade().entityState().setEntityActionButtonsHeld(actionButtonsHeld);
    }
    /** @deprecated Use {@code entityState().gelClingingToLink()}. */
    @Deprecated(forRemoval = true)
    default boolean gelClingingToLink() {
        return facade().entityState().gelClingingToLink();
    }

    /** @deprecated Use {@code entityState().setEntityDialogActive()}. */
    @Deprecated(forRemoval = true)
    default void setEntityDialogActive(boolean active) {
        facade().entityState().setEntityDialogActive(active);
    }
    /** @deprecated Use {@code entityState().setEntityTalkState()}. */
    @Deprecated(forRemoval = true)
    default void setEntityTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        facade().entityState().setEntityTalkState(inventoryAppearing, dialogCooldown, windowY);
    }
    /** @deprecated Use {@code entityState().setEntityActionButtonsHeld()}. */
    @Deprecated(forRemoval = true)
    default void setEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        facade().entityState().setEntityActionButtonsHeld(actionButtonAHeld, actionButtonBHeld);
    }
    /** @deprecated Use {@code entityState().setToadstoolPlayerState()}. */
    @Deprecated(forRemoval = true)
    default void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        facade().entityState().setToadstoolPlayerState(hasToadstool, magicPowderCount);
    }

    /** @deprecated Use {@code entities().inBossBattle()}. */
    @Deprecated(forRemoval = true)
    default boolean inBossBattle() {
        return facade().entities().inBossBattle();
    }
    /** @deprecated Use {@code entityState().setEntityScreenShakeCountdown()}. */
    @Deprecated(forRemoval = true)
    default void setEntityScreenShakeCountdown(int countdown) {
        facade().entityState().setEntityScreenShakeCountdown(countdown);
    }
    /** @deprecated Use {@code entityState().setSecretSeashellPegasusCollisionState()}. */
    @Deprecated(forRemoval = true)
    default void setSecretSeashellPegasusCollisionState(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        facade().entityState()
                .setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY);
    }
    /** @deprecated Use {@code entityState().setEntityPowerBraceletButtonHeld()}. */
    @Deprecated(forRemoval = true)
    default void setEntityPowerBraceletButtonHeld(boolean buttonHeld) {
        facade().entityState().setEntityPowerBraceletButtonHeld(buttonHeld);
    }
    /** @deprecated Use {@code entityState().setEntityBombButtonHeld()}. */
    @Deprecated(forRemoval = true)
    default void setEntityBombButtonHeld(boolean buttonHeld) {
        facade().entityState().setEntityBombButtonHeld(buttonHeld);
    }
    /** @deprecated Use {@code entityState().setEntityAttackStepAnimationCountdown()}. */
    @Deprecated(forRemoval = true)
    default void setEntityAttackStepAnimationCountdown(int countdown) {
        facade().entityState().setEntityAttackStepAnimationCountdown(countdown);
    }
    /** @deprecated Use {@code entityState().advanceBoomerangSoundClock()}. */
    @Deprecated(forRemoval = true)
    default void advanceBoomerangSoundClock(boolean worldGameplay) {
        facade().entityState().advanceBoomerangSoundClock(worldGameplay);
    }

    /** @deprecated Use {@code entityState().deferContactRecoil()}. */
    @Deprecated(forRemoval = true)
    default boolean deferContactRecoil(int slot, int linkX, int linkY) {
        return facade().entityState().deferContactRecoil(slot, linkX, linkY);
    }

    /** @deprecated Use {@code entityState().consumeLinkAttackClearRequests()}. */
    @Deprecated(forRemoval = true)
    default List<LinkAttackClearRequest> consumeLinkAttackClearRequests() {
        return facade().entityState().consumeLinkAttackClearRequests();
    }

    /** @deprecated Use {@code entityState().gotItemPresentationActive()}. */
    @Deprecated(forRemoval = true)
    default boolean gotItemPresentationActive() {
        return facade().entityState().gotItemPresentationActive();
    }

    /** @deprecated Use {@code entityState().powerUpPickupSequenceActive()}. */
    @Deprecated(forRemoval = true)
    default boolean powerUpPickupSequenceActive() {
        return facade().entityState().powerUpPickupSequenceActive();
    }

    /** @deprecated Use {@code entityState().swordPickupSequenceActive()}. */
    @Deprecated(forRemoval = true)
    default boolean swordPickupSequenceActive() {
        return facade().entityState().swordPickupSequenceActive();
    }

    /** @deprecated Use {@code entityState().consumeSwordPickupRewards()}. */
    @Deprecated(forRemoval = true)
    default List<SwordPickupRewardEvent> consumeSwordPickupRewards() {
        return facade().entityState().consumeSwordPickupRewards();
    }
    /** @deprecated Use {@code entityState().consumeEntityDialogRequests()}. */
    @Deprecated(forRemoval = true)
    default List<DialogRequest> consumeEntityDialogRequests() {
        return facade().entityState().consumeEntityDialogRequests();
    }
    /** @deprecated Use {@code entityState().tickGameplayVBlank()}. */
    @Deprecated(forRemoval = true)
    default void tickGameplayVBlank() {
        facade().entityState().tickGameplayVBlank();
    }

    /** @deprecated Use {@code entityState().clearEntity()}. */
    @Deprecated(forRemoval = true)
    default int clearEntity(int slot) {
        return facade().entityState().clearEntity(slot);
    }
}
