package linksawakening.world.room;

import java.util.Objects;

/** Cached player inputs and gates supplied to each room entity runtime. */
final class RoomEntityInputs {
    void clearDialogAfterWarp() {
        entityDialogActive = false;
        entityDialogCooldown = 0;
        entityInventoryAppearing = false;
    }

    private final LoadedRoomHandle rooms;

    RoomEntityInputs(LoadedRoomHandle rooms) {
        this.rooms = Objects.requireNonNull(rooms);
    }

    void updateLiftStrengthBoost(boolean redTunic, boolean power) {
        rooms.updateFlags(redTunic, power, (room, redTunicActive, powerActive) -> room.entities()
                .configuration()
                .setLiftStrengthBoost(redTunicActive, powerActive));
    }

    void updateEntityScreenShakeCountdown(int countdown) {
        rooms.updateInt(
                countdown, (room, value) -> room.entities().configuration().setScreenShakeCountdown(value));
    }

    void updateHeartPiecesCount(int count) {
        rooms.updateInt(count, (room, value) -> room.entities().configuration().setHeartPiecesCount(value));
    }

    void updateFreeMovementMode(boolean enabled) {
        rooms.update(enabled, (room, value) -> room.entities().configuration().setFreeMovementMode(value));
    }

    void updateEntityActionButtonsHeld(boolean held) {
        updateEntityActionButtonsHeld(held, held);
    }

    void updateEntityFinalPositionX(int positionX) {
        setEntityFinalPositionX(positionX);
        rooms.updateInt(
                positionX, (room, value) -> room.entities().configuration().setLinkFinalPositionX(value));
    }

    void updateEntityAttackStepAnimationCountdown(int countdown) {
        setEntityAttackStepAnimationCountdown(countdown);
        rooms.updateInt(
                countdown, (room, value) -> room.entities().configuration().setLinkAttackStepAnimationCountdown(value));
    }

    void updateEntityBombButtonHeld(boolean buttonHeld) {
        setEntityBombButtonHeld(buttonHeld);
        rooms.update(
                buttonHeld, (room, value) -> room.entities().configuration().setBombButtonHeld(value));
    }

    void updateEntityPowerBraceletButtonHeld(boolean buttonHeld) {
        setEntityPowerBraceletButtonHeld(buttonHeld);
        rooms.update(
                buttonHeld, (room, value) -> room.entities().configuration().setPowerBraceletButtonHeld(value));
    }

    void updateSecretSeashellPegasusCollisionState(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY);
        rooms.update(
                this,
                (room, inputs) -> inputs.applySecretSeashellPegasusCollisionState(
                        room,
                        inputs.secretSeashellScreenShakeActive,
                        inputs.secretSeashellPegasusCollisionActive,
                        inputs.secretSeashellPegasusCollisionX,
                        inputs.secretSeashellPegasusCollisionY));
    }

    void updateEntityGoldenLeavesCount(int goldenLeavesCount) {
        setEntityGoldenLeavesCount(goldenLeavesCount);
        rooms.updateInt(
                goldenLeavesCount,
                (room, value) -> room.entities().configuration().setGoldenLeavesCount(value));
    }

    void updateToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        setToadstoolPlayerState(hasToadstool, magicPowderCount);
        rooms.updateFlagInt(hasToadstool, magicPowderCount, (room, owned, count) -> room.entities()
                .configuration()
                .setToadstoolPlayerState(owned, count));
    }

    void updateEntityInventorySlots(int itemA, int itemB) {
        setEntityInventorySlots(itemA, itemB);
        rooms.updateInts(itemA, itemB, (room, first, second) -> room.entities()
                .configuration()
                .setLikeLikeLinkInventory(first, second));
    }

    void updateEntityPressedButtonsMask(int pressedButtonsMask) {
        setEntityPressedButtonsMask(pressedButtonsMask);
        rooms.updateInt(
                pressedButtonsMask,
                (room, value) -> room.entities().configuration().setPressedButtonsMask(value));
    }

    void updateEntityJoypadHeld(boolean joypadHeld) {
        setEntityJoypadHeld(joypadHeld);
        rooms.update(
                joypadHeld, (room, value) -> room.entities().configuration().setJoypadHeld(value));
    }

    void updateEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        setEntityActionButtonsHeld(actionButtonAHeld, actionButtonBHeld);
        rooms.update(
                this,
                (room, inputs) ->
                        inputs.applyActionButtonsHeld(room, inputs.actionButtonAHeld, inputs.actionButtonBHeld));
    }

    void updateEntityMusicActive(boolean active) {
        setEntityMusicActive(active);
        rooms.update(active, (room, value) -> room.entities().configuration().setActiveMusic(value));
    }

    void updateEntityDialogAskSelectionIndex(int selectionIndex) {
        setEntityDialogAskSelectionIndex(selectionIndex);
        rooms.updateInt(
                selectionIndex, (room, value) -> room.entities().configuration().setDialogAskSelectionIndex(value));
    }

    void updateEntityTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        setEntityTalkState(inventoryAppearing, dialogCooldown, windowY);
        rooms.update(
                this,
                (room, inputs) -> inputs.applyTalkState(
                        room, inputs.entityInventoryAppearing, inputs.entityDialogCooldown, inputs.entityWindowY));
    }

    void updateEntityDialogActive(boolean active) {
        setEntityDialogActive(active);
        rooms.update(active, (room, value) -> room.entities().configuration().setDialogActive(value));
    }

    private int entityGoldenLeavesCount;
    private boolean actionButtonAHeld;
    private boolean actionButtonBHeld;
    private boolean joypadHeld;
    private boolean entityDialogActive;
    private boolean entityInventoryAppearing;
    private int entityDialogCooldown;
    private int entityWindowY = 0x80;
    private int entityDialogAskSelectionIndex;
    private boolean entityMusicActive;
    private int entityPressedButtonsMask;
    private boolean powerBraceletButtonHeld;
    private boolean bombButtonHeld;
    private int entityLinkVelocityZ;
    private boolean secretSeashellScreenShakeActive;
    private boolean secretSeashellPegasusCollisionActive;
    private int secretSeashellPegasusCollisionX;
    private int secretSeashellPegasusCollisionY;

    void setEntityActionButtonsHeld(boolean actionButtonsHeld) {
        setEntityActionButtonsHeld(actionButtonsHeld, actionButtonsHeld);
    }

    void setEntityLinkVelocityZ(int velocity) {
        entityLinkVelocityZ = velocity & 0xFF;
    }

    void setEntityDialogActive(boolean active) {
        entityDialogActive = active;
    }

    void setEntityTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        if ((dialogCooldown & ~0xFF) != 0 || (windowY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity talk state values must be unsigned bytes");
        }
        entityInventoryAppearing = inventoryAppearing;
        entityDialogCooldown = dialogCooldown;
        entityWindowY = windowY;
    }

    void setEntityDialogAskSelectionIndex(int selectionIndex) {
        if (selectionIndex < 0 || selectionIndex > 1) {
            throw new IllegalArgumentException("Dialog selection must be 0 or 1");
        }
        entityDialogAskSelectionIndex = selectionIndex;
    }

    void setEntityMusicActive(boolean active) {
        entityMusicActive = active;
    }

    void setEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        this.actionButtonAHeld = actionButtonAHeld;
        this.actionButtonBHeld = actionButtonBHeld;
    }

    void setEntityJoypadHeld(boolean joypadHeld) {
        this.joypadHeld = joypadHeld;
    }

    void setEntityPressedButtonsMask(int pressedButtonsMask) {
        if ((pressedButtonsMask & ~0xFF) != 0) {
            throw new IllegalArgumentException(
                    "Entity pressed-buttons mask must be an unsigned byte: " + pressedButtonsMask);
        }
        entityPressedButtonsMask = pressedButtonsMask;
    }

    void setEntityInventorySlots(int itemA, int itemB) {
        if ((itemA & ~0xFF) != 0 || (itemB & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity inventory slots must be unsigned bytes");
        }
    }

    void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {}

    void setEntityGoldenLeavesCount(int goldenLeavesCount) {
        if (goldenLeavesCount < 0 || goldenLeavesCount > 0xFF) {
            throw new IllegalArgumentException("Golden leaves count must be an unsigned byte: " + goldenLeavesCount);
        }
        entityGoldenLeavesCount = goldenLeavesCount;
    }

    void setSecretSeashellPegasusCollisionState(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        if ((collisionX & ~0xFF) != 0 || (collisionY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Pegasus collision coordinates must be bytes");
        }
        secretSeashellScreenShakeActive = screenShakeActive;
        secretSeashellPegasusCollisionActive = collisionActive;
        secretSeashellPegasusCollisionX = collisionX;
        secretSeashellPegasusCollisionY = collisionY;
    }

    void setEntityPowerBraceletButtonHeld(boolean buttonHeld) {
        this.powerBraceletButtonHeld = buttonHeld;
    }

    void setEntityBombButtonHeld(boolean buttonHeld) {
        this.bombButtonHeld = buttonHeld;
    }

    void setEntityAttackStepAnimationCountdown(int countdown) {
        if (countdown < 0 || countdown > 0xFF) {
            throw new IllegalArgumentException(
                    "Link attack-step animation countdown must be an unsigned byte: " + countdown);
        }
    }

    void setEntityFinalPositionX(int positionX) {
        if ((positionX & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity final Link X must be an unsigned byte: " + positionX);
        }
    }

    void applyDialogActive(RoomRuntime room, boolean active) {
        room.entities().configuration().setDialogActive(active);
    }

    void applyTalkState(RoomRuntime room, boolean inventoryAppearing, int dialogCooldown, int windowY) {
        room.entities().configuration().setTalkState(inventoryAppearing, dialogCooldown, windowY);
    }

    void applyDialogAskSelectionIndex(RoomRuntime room, int selectionIndex) {
        room.entities().configuration().setDialogAskSelectionIndex(selectionIndex);
    }

    void applyMusicActive(RoomRuntime room, boolean active) {
        room.entities().configuration().setActiveMusic(active);
    }

    void applyActionButtonsHeld(RoomRuntime room, boolean actionButtonAHeld, boolean actionButtonBHeld) {
        room.entities().configuration().setActionButtonsHeld(actionButtonAHeld, actionButtonBHeld);
    }

    void applyJoypadHeld(RoomRuntime room, boolean joypadHeld) {
        room.entities().configuration().setJoypadHeld(joypadHeld);
    }

    void applyPressedButtonsMask(RoomRuntime room, int pressedButtonsMask) {
        room.entities().configuration().setPressedButtonsMask(pressedButtonsMask);
    }

    void applyInventorySlots(RoomRuntime room, int itemA, int itemB) {
        room.entities().configuration().setLikeLikeLinkInventory(itemA, itemB);
    }

    void applyToadstoolPlayerState(RoomRuntime room, boolean hasToadstool, int magicPowderCount) {
        room.entities().configuration().setToadstoolPlayerState(hasToadstool, magicPowderCount);
    }

    void applyGoldenLeavesCount(RoomRuntime room, int goldenLeavesCount) {
        room.entities().configuration().setGoldenLeavesCount(goldenLeavesCount);
    }

    void applySecretSeashellPegasusCollisionState(
            RoomRuntime room, boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        room.entities()
                .configuration()
                .setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY);
    }

    void applyPowerBraceletButtonHeld(RoomRuntime room, boolean buttonHeld) {
        room.entities().configuration().setPowerBraceletButtonHeld(buttonHeld);
    }

    void applyBombButtonHeld(RoomRuntime room, boolean buttonHeld) {
        room.entities().configuration().setBombButtonHeld(buttonHeld);
    }

    void applyAttackStepAnimationCountdown(RoomRuntime room, int countdown) {
        room.entities().configuration().setLinkAttackStepAnimationCountdown(countdown);
    }

    void applyFinalPositionX(RoomRuntime room, int positionX) {
        room.entities().configuration().setLinkFinalPositionX(positionX);
    }

    boolean secretSeashellScreenShakeActive() {
        return this.secretSeashellScreenShakeActive;
    }

    boolean secretSeashellScreenShakeActive(boolean value) {
        this.secretSeashellScreenShakeActive = value;
        return this.secretSeashellScreenShakeActive;
    }

    boolean secretSeashellPegasusCollisionActive() {
        return this.secretSeashellPegasusCollisionActive;
    }

    boolean secretSeashellPegasusCollisionActive(boolean value) {
        this.secretSeashellPegasusCollisionActive = value;
        return this.secretSeashellPegasusCollisionActive;
    }

    int secretSeashellPegasusCollisionX() {
        return this.secretSeashellPegasusCollisionX;
    }

    int secretSeashellPegasusCollisionX(int value) {
        this.secretSeashellPegasusCollisionX = value;
        return this.secretSeashellPegasusCollisionX;
    }

    int secretSeashellPegasusCollisionY() {
        return this.secretSeashellPegasusCollisionY;
    }

    int secretSeashellPegasusCollisionY(int value) {
        this.secretSeashellPegasusCollisionY = value;
        return this.secretSeashellPegasusCollisionY;
    }

    boolean actionButtonAHeld() {
        return this.actionButtonAHeld;
    }

    boolean actionButtonBHeld() {
        return this.actionButtonBHeld;
    }

    boolean entityInventoryAppearing() {
        return this.entityInventoryAppearing;
    }

    int entityDialogCooldown() {
        return this.entityDialogCooldown;
    }

    int entityWindowY() {
        return this.entityWindowY;
    }

    boolean joypadHeld() {
        return this.joypadHeld;
    }

    int entityPressedButtonsMask() {
        return this.entityPressedButtonsMask;
    }

    boolean powerBraceletButtonHeld() {
        return this.powerBraceletButtonHeld;
    }

    boolean bombButtonHeld() {
        return this.bombButtonHeld;
    }

    int entityGoldenLeavesCount() {
        return this.entityGoldenLeavesCount;
    }

    int entityGoldenLeavesCount(int value) {
        this.entityGoldenLeavesCount = value;
        return this.entityGoldenLeavesCount;
    }

    boolean entityDialogActive() {
        return this.entityDialogActive;
    }

    int entityDialogAskSelectionIndex() {
        return this.entityDialogAskSelectionIndex;
    }

    boolean entityMusicActive() {
        return this.entityMusicActive;
    }

    int entityLinkVelocityZ() {
        return this.entityLinkVelocityZ;
    }
}
