package linksawakening.ui;

import linksawakening.input.InputConfig;
import linksawakening.input.InputState;

/** Name cursor and five-byte entry buffer for ROM FileCreationInteractiveHandler ($01:4AB2). */
final class FileNameEntry {
    private static final int NAME_LENGTH = 5;

    FileNameEntry(int[] characters) {
        nameEntryCharacterTable = characters;
    }

    void reset() {
        selectedCharacter = 0;
        namePosition = 0;
        currentName = new int[NAME_LENGTH];
    }

    int selectedCharacter() {
        return selectedCharacter;
    }

    int namePosition() {
        return namePosition;
    }
    /** Borrowed name bytes; the frame publisher and action copy them before publishing. */
    int[] currentName() {
        return currentName;
    }

    private static final int NAME_ENTRY_CHARACTER_COUNT = 0x40;

    private final int[] nameEntryCharacterTable;

    private int selectedCharacter;

    private int namePosition;

    private int[] currentName = new int[NAME_LENGTH];

    FileMenuAction tick(InputState inputState, InputConfig inputConfig, int selectedSlot) {
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
