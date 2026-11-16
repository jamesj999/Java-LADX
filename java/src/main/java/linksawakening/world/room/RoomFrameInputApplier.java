package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** Applies frame inputs in the original setter order. Values remain immutable. */
final class RoomFrameInputApplier {
    private final RoomEntityInputs entityInputs;

    private final RoomEntitySessionState frame;
    private final RoomEntityState entityState;
    private final RoomStory story;

    private final RoomSession session;
    private final RoomEntityBridge bridge;

    RoomFrameInputApplier(
            RoomEntityInputs entityInputs,
            RoomEntityBridge bridge,
            RoomEntitySessionState frame,
            RoomEntityState entityState,
            RoomStory story,
            RoomLoadCoordinator loads,
            RoomSession session) {
        this.entityInputs = entityInputs;
        this.bridge = java.util.Objects.requireNonNull(bridge);

        this.frame = frame;
        this.entityState = entityState;
        this.story = story;

        this.session = session;
    }

    List<EntityProjectileEvent> tick(EntityFrameValues inputs) {
        entityState.setEntityDialogActive(inputs.ui().dialogActive());
        entityState.setEntityTalkState(
                inputs.ui().inventoryAppearing(),
                inputs.ui().dialogCooldown(),
                inputs.ui().windowY());
        entityInputs.updateSetEntityDialogAskSelectionIndex(inputs.ui().selectionIndex());
        entityInputs.updateSetHeartPiecesCount(inputs.player().progress().heartPieces());
        entityInputs.updateSetEntityMusicActive(inputs.ui().musicActive());
        entityState.setEntityActionButtonsHeld(
                inputs.buttons().aHeld(), inputs.buttons().bHeld());
        entityInputs.updateSetEntityJoypadHeld(inputs.buttons().joypadHeld());
        entityInputs.updateSetEntityPressedButtonsMask(inputs.buttons().pressedMask());
        entityState.setEntityPowerBraceletButtonHeld(inputs.buttons().braceletHeld());
        entityState.setEntityBombButtonHeld(inputs.buttons().bombHeld());
        entityState.setEntityAttackStepAnimationCountdown(inputs.link().attackCountdown());
        entityInputs.updateSetEntityFinalPositionX(inputs.link().finalX());
        story.setBirdKeyOwned(inputs.player().progress().birdKey());
        story.setTailKeyOwned(inputs.player().progress().tailKey());
        entityInputs.updateSetEntityInventorySlots(
                inputs.player().inventory().itemA(), inputs.player().inventory().itemB());
        setLiftStrengthBoost(
                inputs.player().inventory().redTunic(),
                inputs.player().inventory().pieceOfPower());
        story.setHouseNpcTradeItem(inputs.player().progress().tradeItem());
        story.setChestPlayerLevels(
                inputs.player().levels().shield(),
                inputs.player().levels().sword(),
                inputs.player().levels().bracelet());
        entityState.setToadstoolPlayerState(
                inputs.player().progress().hasToadstool(),
                inputs.player().progress().powder());
        entityInputs.updateSetEntityGoldenLeavesCount(inputs.player().progress().leaves());
        setEnemyDropPlayerState(
                inputs.player().health().maxHearts(),
                inputs.player().health().health(),
                inputs.player().health().powerUp(),
                inputs.player().health().lowHealth());
        entityInputs.updateSetFreeMovementMode(inputs.link().freeMovement());
        entityInputs.setEntityLinkVelocityZ(inputs.link().velocityZ());
        // Inputs have already been applied; select the low-level tick overload without a new wrapper.
        return session.tickEntities((EntityTickValues) inputs);
    }

    private void setLiftStrengthBoost(boolean redTunic, boolean pieceOfPower) {
        entityInputs.updateSetLiftStrengthBoost(redTunic, pieceOfPower);
    }

    private void setEnemyDropPlayerState(int maxHearts, int health, boolean power, boolean low) {
        bridge.updateSetEnemyDropPlayerState(maxHearts, health, power);
        frame.overrideEnemyDropLowHealth(low);
        bridge.configureEnemyDropRuntime();
    }
}
