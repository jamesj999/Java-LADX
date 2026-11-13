package linksawakening.world.room;

/** Cached player inputs and gates supplied to each room entity runtime. */
final class RoomEntityInputs {

    int entityGoldenLeavesCount;
    boolean actionButtonsHeld;
    boolean actionButtonAHeld;
    boolean actionButtonBHeld;
    boolean joypadHeld;
    boolean entityDialogActive;
    boolean entityInventoryAppearing;
    int entityDialogCooldown;
    int entityWindowY = 0x80;
    int entityDialogAskSelectionIndex;
    boolean entityMusicActive;
    int entityPressedButtonsMask;
    boolean powerBraceletButtonHeld;
    boolean bombButtonHeld;
    int entityLinkVelocityZ;
    boolean secretSeashellScreenShakeActive;
    boolean secretSeashellPegasusCollisionActive;
    int secretSeashellPegasusCollisionX;
    int secretSeashellPegasusCollisionY;

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
        this.actionButtonsHeld = actionButtonAHeld || actionButtonBHeld;
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
        room.entities.configuration().setDialogActive(active);
    }

    void applyTalkState(RoomRuntime room, boolean inventoryAppearing, int dialogCooldown, int windowY) {
        room.entities.configuration().setTalkState(inventoryAppearing, dialogCooldown, windowY);
    }

    void applyDialogAskSelectionIndex(RoomRuntime room, int selectionIndex) {
        room.entities.configuration().setDialogAskSelectionIndex(selectionIndex);
    }

    void applyMusicActive(RoomRuntime room, boolean active) {
        room.entities.configuration().setActiveMusic(active);
    }

    void applyActionButtonsHeld(RoomRuntime room, boolean actionButtonAHeld, boolean actionButtonBHeld) {
        room.entities.configuration().setActionButtonsHeld(actionButtonAHeld, actionButtonBHeld);
    }

    void applyJoypadHeld(RoomRuntime room, boolean joypadHeld) {
        room.entities.configuration().setJoypadHeld(joypadHeld);
    }

    void applyPressedButtonsMask(RoomRuntime room, int pressedButtonsMask) {
        room.entities.configuration().setPressedButtonsMask(pressedButtonsMask);
    }

    void applyInventorySlots(RoomRuntime room, int itemA, int itemB) {
        room.entities.configuration().setLikeLikeLinkInventory(itemA, itemB);
    }

    void applyToadstoolPlayerState(RoomRuntime room, boolean hasToadstool, int magicPowderCount) {
        room.entities.configuration().setToadstoolPlayerState(hasToadstool, magicPowderCount);
    }

    void applyGoldenLeavesCount(RoomRuntime room, int goldenLeavesCount) {
        room.entities.configuration().setGoldenLeavesCount(goldenLeavesCount);
    }

    void applySecretSeashellPegasusCollisionState(
            RoomRuntime room, boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        room.entities
                .configuration()
                .setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY);
    }

    void applyPowerBraceletButtonHeld(RoomRuntime room, boolean buttonHeld) {
        room.entities.configuration().setPowerBraceletButtonHeld(buttonHeld);
    }

    void applyBombButtonHeld(RoomRuntime room, boolean buttonHeld) {
        room.entities.configuration().setBombButtonHeld(buttonHeld);
    }

    void applyAttackStepAnimationCountdown(RoomRuntime room, int countdown) {
        room.entities.configuration().setLinkAttackStepAnimationCountdown(countdown);
    }

    void applyFinalPositionX(RoomRuntime room, int positionX) {
        room.entities.configuration().setLinkFinalPositionX(positionX);
    }
}
