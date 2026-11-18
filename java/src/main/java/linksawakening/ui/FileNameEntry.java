package linksawakening.ui;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;

/** Name cursor and five-byte entry buffer for ROM FileCreationInteractiveHandler ($01:4AB2). */
final class FileNameEntry {
    private static final int NAME_LENGTH = 5;

    private static final int NAME_ENTRY_CHARACTER_COUNT = 0x40;

    private final int[] nameEntryCharacterTable;

    private int selectedCharacter;

    private int namePosition;
    private int heldDirectionFrames;

    private int[] currentName = new int[NAME_LENGTH];

    FileNameEntry(int[] characters) {
        nameEntryCharacterTable = characters;
    }

    void reset() {
        selectedCharacter = 0;
        namePosition = 0;
        heldDirectionFrames = 0;
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

    FileMenuAction tick(InputState inputState, InputConfig inputConfig, int selectedSlot, GameplaySoundSink sounds) {
        // FileCreationInteractiveHandler $01:4AB2 checks START before editing.
        // Even an all-zero name is written, then $01:4B26 returns to selection;
        // func_5DC0 leaves that slot empty. There is no empty-name error path.
        if (inputState.wasPressed(inputConfig.menuOpenKey())) {
            sounds.play(GameplaySoundEvent.MENU_VALIDATE);
            return new FileMenuAction(FileMenuAction.Type.CREATE_FILE, selectedSlot, currentName);
        }
        int step = cursorStep(inputState::wasPressed, inputConfig);
        int heldStep = cursorStep(inputState::isDown, inputConfig);
        if (step != 0 || heldStep == 0) {
            heldDirectionFrames = 0;
        } else if (++heldDirectionFrames == 24) {
            heldDirectionFrames = 21;
            step = heldStep;
        }
        if (step != 0) {
            selectedCharacter = Math.floorMod(selectedCharacter + step, NAME_ENTRY_CHARACTER_COUNT);
            sounds.play(GameplaySoundEvent.MENU_MOVE);
        }
        if (inputState.wasPressed(inputConfig.bKey())) {
            sounds.play(GameplaySoundEvent.MENU_VALIDATE);
            namePosition = Math.max(0, namePosition - 1);
        } else if (inputState.wasPressed(inputConfig.aKey())) {
            sounds.play(GameplaySoundEvent.MENU_VALIDATE);
            currentName[namePosition] = nameEntryCharacterTable[selectedCharacter];
            namePosition = Math.min(NAME_LENGTH - 1, namePosition + 1);
        }

        return FileMenuAction.none(selectedSlot);
    }

    private static int cursorStep(java.util.function.IntPredicate pressed, InputConfig keys) {
        if (pressed.test(keys.upKey())) return -0x10;
        if (pressed.test(keys.downKey())) return 0x10;
        if (pressed.test(keys.leftKey())) return -1;
        if (pressed.test(keys.rightKey())) return 1;
        return 0;
    }
}
