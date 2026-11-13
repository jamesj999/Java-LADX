package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** Applies frame inputs in the original setter order. Values remain immutable. */
final class RoomFrameInputApplier {
    private final RoomEntityInputs entityInputs;
    private final RoomEntityBridge bridge;
    private final RoomEntitySessionState frame;
    private final RoomEntityState entityState;
    private final RoomStory story;
    private final RoomLoadCoordinator loads;
    private final RoomSession session;

    RoomFrameInputApplier(
            RoomEntityInputs entityInputs,
            RoomEntityBridge bridge,
            RoomEntitySessionState frame,
            RoomEntityState entityState,
            RoomStory story,
            RoomLoadCoordinator loads,
            RoomSession session) {
        this.entityInputs = entityInputs;
        this.bridge = bridge;
        this.frame = frame;
        this.entityState = entityState;
        this.story = story;
        this.loads = loads;
        this.session = session;
    }

    List<EntityProjectileEvent> tick(EntityFrameInputs inputs) {
        entityState.setEntityDialogActive(inputs.ui().dialogActive());
        entityState.setEntityTalkState(
                inputs.ui().inventoryAppearing(),
                inputs.ui().dialogCooldown(),
                inputs.ui().windowY());
        session.setEntityDialogAskSelectionIndex(inputs.ui().selectionIndex());
        session.setHeartPiecesCount(inputs.player().progress().heartPieces());
        session.setEntityMusicActive(inputs.ui().musicActive());
        entityState.setEntityActionButtonsHeld(
                inputs.buttons().aHeld(), inputs.buttons().bHeld());
        session.setEntityJoypadHeld(inputs.buttons().joypadHeld());
        session.setEntityPressedButtonsMask(inputs.buttons().pressedMask());
        entityState.setEntityPowerBraceletButtonHeld(inputs.buttons().braceletHeld());
        entityState.setEntityBombButtonHeld(inputs.buttons().bombHeld());
        entityState.setEntityAttackStepAnimationCountdown(inputs.link().attackCountdown());
        session.setEntityFinalPositionX(inputs.link().finalX());
        story.setBirdKeyOwned(inputs.player().progress().birdKey());
        story.setTailKeyOwned(inputs.player().progress().tailKey());
        session.setEntityInventorySlots(
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
        session.setEntityGoldenLeavesCount(inputs.player().progress().leaves());
        setEnemyDropPlayerState(
                inputs.player().health().maxHearts(),
                inputs.player().health().health(),
                inputs.player().health().powerUp(),
                inputs.player().health().lowHealth());
        session.setFreeMovementMode(inputs.link().freeMovement());
        entityInputs.setEntityLinkVelocityZ(inputs.link().velocityZ());
        return session.tickEntities(new EntityTickInputs(
                inputs.frameCounter(),
                inputs.link().position(),
                inputs.link().motion(),
                inputs.player().levels().shield(),
                inputs.sword()));
    }

    private void setLiftStrengthBoost(boolean redTunic, boolean pieceOfPower) {
        if (bridge.runtime() != null) bridge.runtime().configuration().setLiftStrengthBoost(redTunic, pieceOfPower);
    }

    private void setEnemyDropPlayerState(int maxHearts, int health, boolean power, boolean low) {
        session.setEnemyDropPlayerState(maxHearts, health, power);
        frame.enemyDropLowHealth = low;
        session.configureEnemyDropRuntime();
    }
}
