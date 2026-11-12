package linksawakening.ui;

import static linksawakening.ui.FileMenuController.*;

import java.util.Objects;
import java.util.function.Function;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.scene.BackgroundScene;

/**
 * Input/state model for the ROM-backed file-selection and New Game screens.
 */

/** FileMenuFlow behavior on the shared FileMenuController state. */
abstract class FileMenuFlow extends FileMenuState {
    protected FileMenuFlow(
            FileMenuRomData romData,
            Function<String, BackgroundScene> sceneProvider,
            int saveFilesMask,
            int[][] savedNames) {
        super(romData, sceneProvider, saveFilesMask, savedNames);
    }

    protected FileMenuFlow(
            FileMenuRomData romData,
            Function<String, BackgroundScene> sceneProvider,
            int saveFilesMask,
            int[][] savedNames,
            int[] maxHearts,
            int[] deathCountsBcd) {
        super(romData, sceneProvider, saveFilesMask, savedNames, maxHearts, deathCountsBcd);
    }

    public FileMenuFrameSnapshot snapshot() {
        return snapshot;
    }

    public Mode mode() {
        return mode;
    }

    public FileMenuAction tick(InputState inputState, InputConfig inputConfig) {
        Objects.requireNonNull(inputState, "inputState");
        Objects.requireNonNull(inputConfig, "inputConfig");
        frameCounter = (frameCounter + 1) & 0xFF;

        FileMenuAction action =
                switch (mode) {
                    case SELECT -> tickSelection(inputState, inputConfig);
                    case CREATE -> tickCreation(inputState, inputConfig);
                    case ERASE_PICK -> tickErasePick(inputState, inputConfig);
                    case ERASE_CONFIRM -> tickEraseConfirm(inputState, inputConfig);
                    case COPY_SOURCE -> tickCopySource(inputState, inputConfig);
                    case COPY_TARGET -> tickCopyTarget(inputState, inputConfig);
                    case COPY_CONFIRM -> tickCopyConfirm(inputState, inputConfig);
                };
        refreshSnapshot();
        return action;
    }

    protected FileMenuAction tickSelection(InputState inputState, InputConfig inputConfig) {
        int maximumSlot = hasSavedFile() ? 3 : 2;
        if (inputState.wasPressed(inputConfig.upKey())) {
            selectedSlot = selectedSlot == 0 ? maximumSlot : selectedSlot - 1;
        } else if (inputState.wasPressed(inputConfig.downKey())) {
            selectedSlot = selectedSlot == maximumSlot ? 0 : selectedSlot + 1;
        }

        if (selectedSlot == 3) {
            if (inputState.wasPressed(inputConfig.leftKey()) || inputState.wasPressed(inputConfig.rightKey())) {
                commandRowArrowShifted = !commandRowArrowShifted;
            }
        }

        if (inputState.wasPressed(inputConfig.aKey()) || inputState.wasPressed(inputConfig.menuOpenKey())) {
            if (selectedSlot < 3) {
                int[] name = savedNames[selectedSlot];
                if (hasStoredName(name)) {
                    return new FileMenuAction(FileMenuAction.Type.LOAD_GAME, selectedSlot, name);
                }
                mode = Mode.CREATE;
                selectedCharacter = 0;
                namePosition = 0;
                currentName = new int[NAME_LENGTH];
            } else {
                mode = commandRowArrowShifted ? Mode.COPY_SOURCE : Mode.ERASE_PICK;
                selectedSlot = 0;
            }
        }
        return FileMenuAction.none(selectedSlot);
    }

    protected FileMenuAction tickErasePick(InputState inputState, InputConfig inputConfig) {
        if (cancelled(inputState, inputConfig)) {
            return returnToSelection();
        }
        moveFourRowCursor(inputState, inputConfig);
        if (activated(inputState, inputConfig)) {
            if (selectedSlot == 3) {
                return returnToSelection();
            }
            if (hasStoredName(savedNames[selectedSlot])) {
                sourceSlot = selectedSlot;
                confirmationOk = false;
                mode = Mode.ERASE_CONFIRM;
            }
        }
        return FileMenuAction.none(selectedSlot);
    }

    protected FileMenuAction tickEraseConfirm(InputState inputState, InputConfig inputConfig) {
        if (cancelled(inputState, inputConfig)) {
            mode = Mode.ERASE_PICK;
            selectedSlot = sourceSlot;
            return FileMenuAction.none(selectedSlot);
        }
        toggleConfirmation(inputState, inputConfig);
        if (!activated(inputState, inputConfig)) {
            return FileMenuAction.none(sourceSlot);
        }
        mode = Mode.SELECT;
        selectedSlot = 0;
        return confirmationOk
                ? new FileMenuAction(FileMenuAction.Type.ERASE_SLOT, sourceSlot, new int[0])
                : FileMenuAction.none(selectedSlot);
    }

    protected FileMenuAction tickCopySource(InputState inputState, InputConfig inputConfig) {
        if (cancelled(inputState, inputConfig)) {
            return returnToSelection();
        }
        moveFourRowCursor(inputState, inputConfig);
        if (activated(inputState, inputConfig)) {
            if (selectedSlot == 3) {
                return returnToSelection();
            }
            if (hasStoredName(savedNames[selectedSlot])) {
                sourceSlot = selectedSlot;
                targetSlot = 0;
                selectedSlot = targetSlot;
                mode = Mode.COPY_TARGET;
            }
        }
        return FileMenuAction.none(selectedSlot);
    }

    protected FileMenuAction tickCopyTarget(InputState inputState, InputConfig inputConfig) {
        if (cancelled(inputState, inputConfig)) {
            mode = Mode.COPY_SOURCE;
            selectedSlot = sourceSlot;
            return FileMenuAction.none(selectedSlot);
        }
        moveFourRowCursor(inputState, inputConfig);
        if (activated(inputState, inputConfig)) {
            if (selectedSlot == 3) {
                return returnToSelection();
            }
            targetSlot = selectedSlot;
            confirmationOk = false;
            mode = Mode.COPY_CONFIRM;
        }
        return FileMenuAction.none(selectedSlot);
    }

    protected FileMenuAction tickCopyConfirm(InputState inputState, InputConfig inputConfig) {
        if (cancelled(inputState, inputConfig)) {
            mode = Mode.COPY_TARGET;
            selectedSlot = targetSlot;
            return FileMenuAction.none(selectedSlot);
        }
        toggleConfirmation(inputState, inputConfig);
        if (!activated(inputState, inputConfig)) {
            return FileMenuAction.none(sourceSlot);
        }
        mode = Mode.SELECT;
        selectedSlot = 0;
        return confirmationOk
                ? new FileMenuAction(FileMenuAction.Type.COPY_SLOT, sourceSlot, targetSlot, new int[0])
                : FileMenuAction.none(selectedSlot);
    }

    protected void moveFourRowCursor(InputState inputState, InputConfig inputConfig) {
        if (inputState.wasPressed(inputConfig.upKey())) {
            selectedSlot = (selectedSlot + 3) & 3;
        } else if (inputState.wasPressed(inputConfig.downKey())) {
            selectedSlot = (selectedSlot + 1) & 3;
        }
    }

    protected void toggleConfirmation(InputState inputState, InputConfig inputConfig) {
        if (inputState.wasPressed(inputConfig.leftKey()) || inputState.wasPressed(inputConfig.rightKey())) {
            confirmationOk = !confirmationOk;
        }
    }

    protected FileMenuAction returnToSelection() {
        mode = Mode.SELECT;
        selectedSlot = 0;
        return FileMenuAction.none(selectedSlot);
    }

    protected FileMenuAction tickCreation(InputState inputState, InputConfig inputConfig) {
        // FileCreationInteractiveHandler $01:4AB2 checks START before editing.
        // Even an all-zero name is written, then $01:4B26 returns to selection;
        // func_5DC0 leaves that slot empty. There is no empty-name error path.
        if (inputState.wasPressed(inputConfig.menuOpenKey())) {
            return new FileMenuAction(FileMenuAction.Type.CREATE_FILE, selectedSlot, currentName);
        }
        if (inputState.wasPressed(inputConfig.leftKey())) {
            selectedCharacter = (selectedCharacter + NAME_ENTRY_CHARACTER_COUNT - 1) % NAME_ENTRY_CHARACTER_COUNT;
        } else if (inputState.wasPressed(inputConfig.rightKey())) {
            selectedCharacter = (selectedCharacter + 1) % NAME_ENTRY_CHARACTER_COUNT;
        } else if (inputState.wasPressed(inputConfig.upKey())) {
            selectedCharacter = (selectedCharacter + NAME_ENTRY_CHARACTER_COUNT - 0x10) % NAME_ENTRY_CHARACTER_COUNT;
        } else if (inputState.wasPressed(inputConfig.downKey())) {
            selectedCharacter = (selectedCharacter + 0x10) % NAME_ENTRY_CHARACTER_COUNT;
        }

        if (inputState.wasPressed(inputConfig.bKey())) {
            namePosition = Math.max(0, namePosition - 1);
        } else if (inputState.wasPressed(inputConfig.aKey())) {
            currentName[namePosition] = nameEntryCharacterTable[selectedCharacter];
            namePosition = Math.min(NAME_LENGTH - 1, namePosition + 1);
        }

        return FileMenuAction.none(selectedSlot);
    }
}
