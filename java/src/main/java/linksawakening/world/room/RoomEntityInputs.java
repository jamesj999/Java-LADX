package linksawakening.world.room;

/** Cached player inputs and gates supplied to each room entity runtime. */
final class RoomEntityInputs {
    private final RoomEntitySessionState state;

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

    RoomEntityInputs(RoomEntitySessionState state) {
        this.state = java.util.Objects.requireNonNull(state);
    }

    void setEntityActionButtonsHeld(boolean actionButtonsHeld) {
        setEntityActionButtonsHeld(actionButtonsHeld, actionButtonsHeld);
    }

    void setEntityLinkVelocityZ(int velocity) {
        entityLinkVelocityZ = velocity & 0xFF;
    }

    void setEntityDialogActive(boolean active) {
        entityDialogActive = active;
        if (state.runtime != null) {
            state.runtime.configuration().setDialogActive(active);
        }
    }

    void setEntityTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        if ((dialogCooldown & ~0xFF) != 0 || (windowY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity talk state values must be unsigned bytes");
        }
        entityInventoryAppearing = inventoryAppearing;
        entityDialogCooldown = dialogCooldown;
        entityWindowY = windowY;
        if (state.runtime != null) {
            state.runtime.configuration().setTalkState(inventoryAppearing, dialogCooldown, windowY);
        }
    }

    void setEntityDialogAskSelectionIndex(int selectionIndex) {
        if (selectionIndex < 0 || selectionIndex > 1) {
            throw new IllegalArgumentException("Dialog selection must be 0 or 1");
        }
        entityDialogAskSelectionIndex = selectionIndex;
        if (state.runtime != null) {
            state.runtime.configuration().setDialogAskSelectionIndex(selectionIndex);
        }
    }

    void setEntityMusicActive(boolean active) {
        entityMusicActive = active;
        if (state.runtime != null) {
            state.runtime.configuration().setActiveMusic(active);
        }
    }

    void setEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        this.actionButtonAHeld = actionButtonAHeld;
        this.actionButtonBHeld = actionButtonBHeld;
        this.actionButtonsHeld = actionButtonAHeld || actionButtonBHeld;
        if (state.runtime != null) {
            state.runtime.configuration().setActionButtonsHeld(actionButtonAHeld, actionButtonBHeld);
        }
    }

    void setEntityJoypadHeld(boolean joypadHeld) {
        this.joypadHeld = joypadHeld;
        if (state.runtime != null) {
            state.runtime.configuration().setJoypadHeld(joypadHeld);
        }
    }

    void setEntityPressedButtonsMask(int pressedButtonsMask) {
        if ((pressedButtonsMask & ~0xFF) != 0) {
            throw new IllegalArgumentException(
                    "Entity pressed-buttons mask must be an unsigned byte: " + pressedButtonsMask);
        }
        entityPressedButtonsMask = pressedButtonsMask;
        if (state.runtime != null) {
            state.runtime.configuration().setPressedButtonsMask(pressedButtonsMask);
        }
    }

    void setEntityInventorySlots(int itemA, int itemB) {
        if ((itemA & ~0xFF) != 0 || (itemB & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity inventory slots must be unsigned bytes");
        }
        if (state.runtime != null) {
            state.runtime.configuration().setLikeLikeLinkInventory(itemA, itemB);
        }
    }

    void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        if (state.runtime != null) {
            state.runtime.configuration().setToadstoolPlayerState(hasToadstool, magicPowderCount);
        }
    }

    void setEntityGoldenLeavesCount(int goldenLeavesCount) {
        if (goldenLeavesCount < 0 || goldenLeavesCount > 0xFF) {
            throw new IllegalArgumentException("Golden leaves count must be an unsigned byte: " + goldenLeavesCount);
        }
        entityGoldenLeavesCount = goldenLeavesCount;
        if (state.runtime != null) {
            state.runtime.configuration().setGoldenLeavesCount(goldenLeavesCount);
        }
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
        if (state.runtime != null) {
            state.runtime
                    .configuration()
                    .setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY);
        }
    }

    void setEntityPowerBraceletButtonHeld(boolean buttonHeld) {
        this.powerBraceletButtonHeld = buttonHeld;
        if (state.runtime != null) {
            state.runtime.configuration().setPowerBraceletButtonHeld(buttonHeld);
        }
    }

    void setEntityBombButtonHeld(boolean buttonHeld) {
        this.bombButtonHeld = buttonHeld;
        if (state.runtime != null) {
            state.runtime.configuration().setBombButtonHeld(buttonHeld);
        }
    }

    void setEntityAttackStepAnimationCountdown(int countdown) {
        if (countdown < 0 || countdown > 0xFF) {
            throw new IllegalArgumentException(
                    "Link attack-step animation countdown must be an unsigned byte: " + countdown);
        }
        if (state.runtime != null) {
            state.runtime.configuration().setLinkAttackStepAnimationCountdown(countdown);
        }
    }

    void setEntityFinalPositionX(int positionX) {
        if ((positionX & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity final Link X must be an unsigned byte: " + positionX);
        }
        if (state.runtime != null) {
            state.runtime.configuration().setLinkFinalPositionX(positionX);
        }
    }
}
