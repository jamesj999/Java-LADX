package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** Applies frame inputs in the original setter order. Values remain immutable. */
final class RoomFrameInputApplier {
    private final RoomEntityInputs entityInputs;
    private final RoomEntityBridge bridge;
    private final RoomEntityFrameState frame;
    private final RoomSessionViews views;
    private final RoomEntityState entityState;
    private final RoomStory story;
    private final RoomLoadCoordinator loads;

    RoomFrameInputApplier(
            RoomEntityInputs entityInputs,
            RoomEntityBridge bridge,
            RoomEntityFrameState frame,
            RoomSessionViews views,
            RoomEntityState entityState,
            RoomStory story,
            RoomLoadCoordinator loads) {
        this.entityInputs = entityInputs;
        this.bridge = bridge;
        this.frame = frame;
        this.views = views;
        this.entityState = entityState;
        this.story = story;
        this.loads = loads;
    }

    List<EntityProjectileEvent> tick(EntityFrameInputs inputs) {
        entityState.setEntityDialogActive(inputs.ui().dialogActive());
        entityState.setEntityTalkState(
                inputs.ui().inventoryAppearing(),
                inputs.ui().dialogCooldown(),
                inputs.ui().windowY());
        entityInputs.setEntityDialogAskSelectionIndex(inputs.ui().selectionIndex());
        views.setHeartPiecesCount(inputs.player().progress().heartPieces());
        entityInputs.setEntityMusicActive(inputs.ui().musicActive());
        entityState.setEntityActionButtonsHeld(
                inputs.buttons().aHeld(), inputs.buttons().bHeld());
        entityInputs.setEntityJoypadHeld(inputs.buttons().joypadHeld());
        entityInputs.setEntityPressedButtonsMask(inputs.buttons().pressedMask());
        entityState.setEntityPowerBraceletButtonHeld(inputs.buttons().braceletHeld());
        entityState.setEntityBombButtonHeld(inputs.buttons().bombHeld());
        entityState.setEntityAttackStepAnimationCountdown(inputs.link().attackCountdown());
        entityInputs.setEntityFinalPositionX(inputs.link().finalX());
        story.setBirdKeyOwned(inputs.player().progress().birdKey());
        story.setTailKeyOwned(inputs.player().progress().tailKey());
        entityInputs.setEntityInventorySlots(
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
        entityInputs.setEntityGoldenLeavesCount(inputs.player().progress().leaves());
        setEnemyDropPlayerState(
                inputs.player().health().maxHearts(),
                inputs.player().health().health(),
                inputs.player().health().powerUp(),
                inputs.player().health().lowHealth());
        bridge.outbox.setFreeMovementMode(inputs.link().freeMovement());
        entityInputs.setEntityLinkVelocityZ(inputs.link().velocityZ());
        return bridge.tickEntities(new EntityTickInputs(
                inputs.frameCounter(),
                inputs.link().position(),
                inputs.link().motion(),
                inputs.player().levels().shield(),
                inputs.sword()));
    }

    private void setLiftStrengthBoost(boolean redTunic, boolean pieceOfPower) {
        if (bridge.entityRuntime != null)
            bridge.entityRuntime.configuration().setLiftStrengthBoost(redTunic, pieceOfPower);
    }

    private void setEnemyDropPlayerState(int maxHearts, int health, boolean power, boolean low) {
        bridge.setEnemyDropPlayerState(maxHearts, health, power);
        frame.enemyDropLowHealth = low;
        loads.configureEnemyDropRuntime();
    }
}
