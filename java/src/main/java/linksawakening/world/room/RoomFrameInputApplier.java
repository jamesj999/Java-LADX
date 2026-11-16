package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** Owns ordered frame input application and synchronous ticking; borrowed values are never retained. */
final class RoomFrameInputApplier {
    private final RoomEntityInputs entityInputs;

    private final RoomEntitySessionState frame;
    private final RoomStory story;

    private final RoomEntityBridge bridge;

    RoomFrameInputApplier(
            RoomEntityInputs entityInputs, RoomEntityBridge bridge, RoomEntitySessionState frame, RoomStory story) {
        this.entityInputs = java.util.Objects.requireNonNull(entityInputs);
        this.bridge = java.util.Objects.requireNonNull(bridge);

        this.frame = java.util.Objects.requireNonNull(frame);
        this.story = java.util.Objects.requireNonNull(story);
    }

    List<EntityProjectileEvent> tick(EntityTickValues inputs) {
        return bridge.tickEntities(inputs);
    }

    List<EntityProjectileEvent> tick(EntityFrameValues inputs) {
        entityInputs.updateEntityDialogActive(inputs.ui().dialogActive());
        entityInputs.updateEntityTalkState(
                inputs.ui().inventoryAppearing(),
                inputs.ui().dialogCooldown(),
                inputs.ui().windowY());
        entityInputs.updateEntityDialogAskSelectionIndex(inputs.ui().selectionIndex());
        entityInputs.updateHeartPiecesCount(inputs.player().progress().heartPieces());
        entityInputs.updateEntityMusicActive(inputs.ui().musicActive());
        entityInputs.updateEntityActionButtonsHeld(
                inputs.buttons().aHeld(), inputs.buttons().bHeld());
        entityInputs.updateEntityJoypadHeld(inputs.buttons().joypadHeld());
        entityInputs.updateEntityPressedButtonsMask(inputs.buttons().pressedMask());
        entityInputs.updateEntityPowerBraceletButtonHeld(inputs.buttons().braceletHeld());
        entityInputs.updateEntityBombButtonHeld(inputs.buttons().bombHeld());
        entityInputs.updateEntityAttackStepAnimationCountdown(inputs.link().attackCountdown());
        entityInputs.updateEntityFinalPositionX(inputs.link().finalX());
        story.setBirdKeyOwned(inputs.player().progress().birdKey());
        story.setTailKeyOwned(inputs.player().progress().tailKey());
        entityInputs.updateEntityInventorySlots(
                inputs.player().inventory().itemA(), inputs.player().inventory().itemB());
        entityInputs.updateLiftStrengthBoost(
                inputs.player().inventory().redTunic(),
                inputs.player().inventory().pieceOfPower());
        story.setHouseNpcTradeItem(inputs.player().progress().tradeItem());
        story.setChestPlayerLevels(
                inputs.player().levels().shield(),
                inputs.player().levels().sword(),
                inputs.player().levels().bracelet());
        entityInputs.updateToadstoolPlayerState(
                inputs.player().progress().hasToadstool(),
                inputs.player().progress().powder());
        entityInputs.updateEntityGoldenLeavesCount(inputs.player().progress().leaves());
        setEnemyDropPlayerState(
                inputs.player().health().maxHearts(),
                inputs.player().health().health(),
                inputs.player().health().powerUp(),
                inputs.player().health().lowHealth());
        entityInputs.updateFreeMovementMode(inputs.link().freeMovement());
        entityInputs.setEntityLinkVelocityZ(inputs.link().velocityZ());
        // Inputs have already been applied; select the low-level tick overload without a new wrapper.
        return bridge.tickEntities((EntityTickValues) inputs);
    }

    private void setEnemyDropPlayerState(int maxHearts, int health, boolean power, boolean low) {
        bridge.updateEnemyDropPlayerState(maxHearts, health, power);
        frame.overrideEnemyDropLowHealth(low);
        bridge.configureEnemyDropRuntime();
    }
}
