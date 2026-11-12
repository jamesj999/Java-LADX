package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.SwordPickupRewardEvent;

interface RoomEntityCompatibility {
    RoomSession facade();
    /** Accepted contact hurt, including enemy projectiles ($03:6DB8). */
    default void resetGuardianAcornCounterOnHurt() {
        var runtime = facade().components.entityBridge().entityRuntime;
        var counters = runtime == null
                ? facade().components.entityFrame().enemyDropCounters
                : runtime.inspection().enemyDropCounters();
        facade().components.entityFrame().enemyDropCounters = EnemyDropResolver.resetGuardianAcornCounter(counters);
        if (runtime != null)
            runtime.configuration().setEnemyDropCounters(facade().components.entityFrame().enemyDropCounters);
    }
    /** Supplies the held J_A|J_B state consumed by input-driven entity handlers. */
    default void setEntityActionButtonsHeld(boolean actionButtonsHeld) {
        facade().components.entityInputs().setEntityActionButtonsHeld(actionButtonsHeld);
    }
    /** The cling flag produced by the previous entity-handler pass. */
    default boolean gelClingingToLink() {
        return facade().components.entityBridge().entityRuntime != null
                && facade().components.entityBridge().entityRuntime.gelClingingToLink();
    }

    default void setEntityDialogActive(boolean active) {
        facade().components.entityInputs().setEntityDialogActive(active);
    }
    /** Supplies the remaining WRAM gates read by ShouldLinkTalkToEntity_05. */
    default void setEntityTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        facade().components.entityInputs().setEntityTalkState(inventoryAppearing, dialogCooldown, windowY);
    }
    /** Supplies the distinct held A/B states consumed by ROM input handlers. */
    default void setEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        facade().components.entityInputs().setEntityActionButtonsHeld(actionButtonAHeld, actionButtonBHeld);
    }
    /** Supplies the two WRAM values checked by SleepyToadstoolEntityHandler. */
    default void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        facade().components.entityInputs().setToadstoolPlayerState(hasToadstool, magicPowderCount);
    }

    default boolean inBossBattle() {
        return facade().components.entityBridge().entityRuntime != null
                && facade().components.entityBridge().entityRuntime.inspection().inBossBattle();
    }
    /** Supplies wScreenShakeCountdown to ceiling-triggered entity handlers. */
    default void setEntityScreenShakeCountdown(int countdown) {
        if (facade().components.entityBridge().entityRuntime != null)
            facade().components.entityBridge().entityRuntime.configuration().setScreenShakeCountdown(countdown);
    }
    /** Supplies the live shake/collision values used by tree shells. */
    default void setSecretSeashellPegasusCollisionState(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        facade().components
                .entityInputs()
                .setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY);
    }
    /** Supplies the held A/B state used by EntityGetLiftedUp. */
    default void setEntityPowerBraceletButtonHeld(boolean buttonHeld) {
        facade().components.entityInputs().setEntityPowerBraceletButtonHeld(buttonHeld);
    }
    /** Supplies the held A/B state for the equipped-bomb lift/throw path. */
    default void setEntityBombButtonHeld(boolean buttonHeld) {
        facade().components.entityInputs().setEntityBombButtonHeld(buttonHeld);
    }
    /** Supplies wLinkAttackStepAnimationCountdown to input-driven entity handlers. */
    default void setEntityAttackStepAnimationCountdown(int countdown) {
        facade().components.entityInputs().setEntityAttackStepAnimationCountdown(countdown);
    }
    /** WorldHandler's shared sound clock persists across entity and room loads. */
    default void advanceBoomerangSoundClock(boolean worldGameplay) {
        facade().components.entityBridge().advanceBoomerangSoundClock(worldGameplay);
    }

    default boolean deferContactRecoil(int slot, int linkX, int linkY) {
        return facade().components.entityBridge().combat.deferContactRecoil(slot, linkX, linkY);
    }

    default List<LinkAttackClearRequest> consumeLinkAttackClearRequests() {
        return facade().components.entityBridge().outbox.consumeLinkAttackClearRequests();
    }

    default boolean gotItemPresentationActive() {
        return facade().components.entityBridge().outbox.gotItemPresentationActive();
    }

    default boolean powerUpPickupSequenceActive() {
        return facade().components.entityBridge().outbox.powerUpPickupSequenceActive();
    }

    default boolean swordPickupSequenceActive() {
        return facade().components.entityBridge().outbox.swordPickupSequenceActive();
    }

    default List<SwordPickupRewardEvent> consumeSwordPickupRewards() {
        return facade().components.entityBridge().outbox.consumeSwordPickupRewards();
    }
    /** Returns and clears ROM dialog requests emitted by the last entity tick. */
    default List<DialogRequest> consumeEntityDialogRequests() {
        return facade().components.entityBridge().outbox.consumeEntityDialogRequests();
    }
    /** Advances the ROM's VBlank tile path, including switch blocks. */
    default void tickGameplayVBlank() {
        facade().components.entityBridge().tickGameplayVBlank();
    }

    default int clearEntity(int slot) {
        return facade().components.entityBridge().combat.clearEntity(slot);
    }
}
