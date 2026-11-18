package linksawakening.ui;

import linksawakening.cutscene.IntroSprite;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * Input/state model for the ROM-backed file-selection and New Game screens.
 */
public final class FileMenuController {

    public enum Mode {
        SELECT,
        CREATE,
        ERASE_PICK,
        ERASE_CONFIRM,
        COPY_SOURCE,
        COPY_TARGET,
        COPY_CONFIRM
    }

    private static final int NAME_LENGTH = 5;
    private static final int NAME_ENTRY_CHARACTER_COUNT = 0x40;
    private static final int BG_MAP_BASE = 0x9800;
    private static final int FILE_NEW_SAVE_SLOT_INDEX_OFFSET = 0x9849 - BG_MAP_BASE;
    private static final int FILE_NEW_NAME_OFFSET = 0x984A - BG_MAP_BASE;
    private static final int FILE_NEW_SPACING_OFFSET = 0x982A - BG_MAP_BASE;
    private static final int[] SAVE_NAME_OFFSETS = {
        0x98C5 - BG_MAP_BASE,
        0x9925 - BG_MAP_BASE,
        0x9985 - BG_MAP_BASE
    };
    private static final int FIRST_SAVE_HEART_OFFSET = 0xCB;
    private static final int FIRST_SAVE_DEATH_COUNT_OFFSET = 0xE7;
    private static final int SAVE_SLOT_ROW_STRIDE = 0x60;
    private static final int HEARTS_PER_ROW = 7;
    private static final int SAVE_HEART_TILE = 0xAE;
    private static final int SAVE_DIGIT_ZERO_TILE = 0xB0;
    private static final int[] COPY_SOURCE_NAME_OFFSETS = {0xC4, 0x124, 0x184};
    private static final int[] COPY_TARGET_NAME_OFFSETS = {0xCD, 0x12D, 0x18D};
    private static final int QUIT_OK_OFFSET = 0x99E4 - BG_MAP_BASE;
    private static final int[] QUIT_OK_TILES = {
        0x7E, 0x7E, 0x10, 0x14, 0x08, 0x13, 0x7E,
        0x7E, 0x7E, 0x7E, 0x0E, 0x0A, 0x7E, 0x7E
    };
    private static final int FILE_NEW_SAVE_SLOT_1_TILE = 0xAB;
    private static final int DARK_BACKGROUND_TILE = 0x7E;

    private final Function<String, BackgroundScene> sceneProvider;
    private final GameplaySoundSink soundSink;
    private final Map<String, BackgroundScene> sceneCache = new HashMap<>();
    private final int[] selectionCursorYPositions;
    private final int[] nameEntryCharacterTable;
    private final int[] nameCursorYPositions;
    private final int[] nameCursorXPositions;
    private final int[] namePositionXPositions;
    private final int[] codepointToTileMap;
    private final int saveFilesMask;
    private final int[][] savedNames;
    private final SlotStats[] slotStats;

    private int selectedSlot;
    private boolean commandRowArrowShifted;
    private Mode mode = Mode.SELECT;
    private int sourceSlot;
    private int targetSlot;
    private boolean confirmationOk;
    private int selectedCharacter;
    private int heldDirectionFrames;
    private int namePosition;
    private int frameCounter;
    private int[] currentName = new int[NAME_LENGTH];
    private FileMenuFrameSnapshot snapshot;

    public record SlotStats(int health, int maxHearts, int deathCount) {
    }

    public FileMenuController(FileMenuRomData romData,
                              Function<String, BackgroundScene> sceneProvider,
                              int saveFilesMask,
                              int[][] savedNames) {
        this(romData, sceneProvider, saveFilesMask, savedNames, GameplaySoundSink.none());
    }

    public FileMenuController(FileMenuRomData romData,
                              Function<String, BackgroundScene> sceneProvider,
                              int saveFilesMask,
                              int[][] savedNames,
                              GameplaySoundSink soundSink) {
        this(romData, sceneProvider, saveFilesMask, savedNames, new SlotStats[3], soundSink);
    }

    public FileMenuController(FileMenuRomData romData,
                              Function<String, BackgroundScene> sceneProvider,
                              int saveFilesMask,
                              int[][] savedNames,
                              SlotStats[] slotStats,
                              GameplaySoundSink soundSink) {
        Objects.requireNonNull(romData, "romData");
        this.sceneProvider = Objects.requireNonNull(sceneProvider, "sceneProvider");
        this.soundSink = Objects.requireNonNull(soundSink, "soundSink");
        this.selectionCursorYPositions = romData.selectionCursorYPositions();
        this.nameEntryCharacterTable = romData.nameEntryCharacterTable();
        this.nameCursorYPositions = romData.nameCursorYPositions();
        this.nameCursorXPositions = romData.nameCursorXPositions();
        this.namePositionXPositions = romData.namePositionXPositions();
        this.codepointToTileMap = romData.codepointToTileMap();
        this.saveFilesMask = saveFilesMask & 0x07;
        this.savedNames = copySavedNames(savedNames);
        if (slotStats.length != this.savedNames.length) {
            throw new IllegalArgumentException("Expected one stats entry per save slot");
        }
        this.slotStats = slotStats.clone();
        refreshSnapshot();
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

        FileMenuAction action = switch (mode) {
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

    private FileMenuAction tickSelection(InputState inputState, InputConfig inputConfig) {
        int maximumSlot = hasSavedFile() ? 3 : 2;
        int previousSlot = selectedSlot;
        boolean previousArrowShift = commandRowArrowShifted;
        if (inputState.wasPressed(inputConfig.upKey())) {
            selectedSlot = selectedSlot == 0 ? maximumSlot : selectedSlot - 1;
        } else if (inputState.wasPressed(inputConfig.downKey())) {
            selectedSlot = selectedSlot == maximumSlot ? 0 : selectedSlot + 1;
        }

        if (selectedSlot == 3) {
            if (inputState.wasPressed(inputConfig.leftKey())
                || inputState.wasPressed(inputConfig.rightKey())) {
                commandRowArrowShifted = !commandRowArrowShifted;
            }
        }

        if (selectedSlot != previousSlot || commandRowArrowShifted != previousArrowShift) {
            soundSink.play(GameplaySoundEvent.MENU_MOVE);
        }
        if (activated(inputState, inputConfig)) {
            soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
            if (selectedSlot < 3) {
                int[] name = savedNames[selectedSlot];
                if (hasStoredName(name)) {
                    return new FileMenuAction(FileMenuAction.Type.LOAD_GAME, selectedSlot, name);
                }
                mode = Mode.CREATE;
                selectedCharacter = 0;
                heldDirectionFrames = 0;
                namePosition = 0;
                currentName = new int[NAME_LENGTH];
            } else {
                mode = commandRowArrowShifted ? Mode.COPY_SOURCE : Mode.ERASE_PICK;
                selectedSlot = 0;
            }
        }
        return FileMenuAction.none(selectedSlot);
    }

    private FileMenuAction tickErasePick(InputState inputState, InputConfig inputConfig) {
        if (cancelled(inputState, inputConfig)) {
            return returnToSelection();
        }
        moveFourRowCursor(inputState, inputConfig);
        if (activated(inputState, inputConfig)) {
            if (selectedSlot == 3) {
                return returnToSelection();
            }
            if (hasStoredName(savedNames[selectedSlot])) {
                soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
                sourceSlot = selectedSlot;
                confirmationOk = false;
                mode = Mode.ERASE_CONFIRM;
            }
        }
        return FileMenuAction.none(selectedSlot);
    }

    private FileMenuAction tickEraseConfirm(InputState inputState, InputConfig inputConfig) {
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
        if (confirmationOk) {
            soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
        }
        return confirmationOk
            ? new FileMenuAction(FileMenuAction.Type.ERASE_SLOT, sourceSlot, new int[0])
            : FileMenuAction.none(selectedSlot);
    }

    private FileMenuAction tickCopySource(InputState inputState, InputConfig inputConfig) {
        if (cancelled(inputState, inputConfig)) {
            return returnToSelection();
        }
        moveFourRowCursor(inputState, inputConfig);
        if (activated(inputState, inputConfig)) {
            if (selectedSlot == 3) {
                return returnToSelection();
            }
            if (hasStoredName(savedNames[selectedSlot])) {
                soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
                sourceSlot = selectedSlot;
                targetSlot = 0;
                selectedSlot = targetSlot;
                mode = Mode.COPY_TARGET;
            }
        }
        return FileMenuAction.none(selectedSlot);
    }

    private FileMenuAction tickCopyTarget(InputState inputState, InputConfig inputConfig) {
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
            soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
            targetSlot = selectedSlot;
            confirmationOk = false;
            mode = Mode.COPY_CONFIRM;
        }
        return FileMenuAction.none(selectedSlot);
    }

    private FileMenuAction tickCopyConfirm(InputState inputState, InputConfig inputConfig) {
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
        if (confirmationOk) {
            soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
        }
        return confirmationOk
            ? new FileMenuAction(FileMenuAction.Type.COPY_SLOT, sourceSlot, targetSlot, new int[0])
            : FileMenuAction.none(selectedSlot);
    }

    private void moveFourRowCursor(InputState inputState, InputConfig inputConfig) {
        int previousSlot = selectedSlot;
        if (inputState.wasPressed(inputConfig.upKey())) {
            selectedSlot = (selectedSlot + 3) & 3;
        } else if (inputState.wasPressed(inputConfig.downKey())) {
            selectedSlot = (selectedSlot + 1) & 3;
        }
        if (previousSlot != selectedSlot) {
            soundSink.play(GameplaySoundEvent.MENU_MOVE);
        }
    }

    private void toggleConfirmation(InputState inputState, InputConfig inputConfig) {
        if (inputState.wasPressed(inputConfig.leftKey())
            || inputState.wasPressed(inputConfig.rightKey())) {
            confirmationOk = !confirmationOk;
            soundSink.play(GameplaySoundEvent.MENU_MOVE);
        }
    }

    private FileMenuAction returnToSelection() {
        mode = Mode.SELECT;
        selectedSlot = 0;
        return FileMenuAction.none(selectedSlot);
    }

    private static boolean activated(InputState inputState, InputConfig inputConfig) {
        return inputState.wasPressed(inputConfig.aKey())
            || inputState.wasPressed(inputConfig.menuOpenKey());
    }

    private static boolean cancelled(InputState inputState, InputConfig inputConfig) {
        return inputState.wasPressed(inputConfig.bKey());
    }

    private FileMenuAction tickCreation(InputState inputState, InputConfig inputConfig) {
        if (inputState.wasPressed(inputConfig.menuOpenKey())) {
            soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
            FileMenuAction action = new FileMenuAction(FileMenuAction.Type.REGISTER_FILE,
                selectedSlot, currentName);
            returnToSelection();
            return action;
        }

        int step = nameCursorStep(inputState::wasPressed, inputConfig);
        if (step == 0) {
            int heldStep = nameCursorStep(inputState::isDown, inputConfig);
            if (heldStep == 0) {
                heldDirectionFrames = 0;
            } else if (++heldDirectionFrames == 0x18) {
                // FileCreationInteractiveHandler repeats every three frames after the initial delay.
                heldDirectionFrames = 0x15;
                step = heldStep;
            }
        }
        if (step != 0) {
            selectedCharacter = Math.floorMod(selectedCharacter + step, NAME_ENTRY_CHARACTER_COUNT);
            soundSink.play(GameplaySoundEvent.MENU_MOVE);
        }

        boolean backspace = inputState.wasPressed(inputConfig.bKey());
        boolean insert = inputState.wasPressed(inputConfig.aKey());
        if (backspace || insert) {
            soundSink.play(GameplaySoundEvent.MENU_VALIDATE);
        }
        if (backspace) {
            namePosition = Math.max(0, namePosition - 1);
        } else if (insert) {
            currentName[namePosition] = nameEntryCharacterTable[selectedCharacter];
            namePosition = Math.min(NAME_LENGTH - 1, namePosition + 1);
        }

        return FileMenuAction.none(selectedSlot);
    }

    private static int nameCursorStep(IntPredicate pressed, InputConfig keys) {
        if (pressed.test(keys.upKey())) return -0x10;
        if (pressed.test(keys.downKey())) return 0x10;
        if (pressed.test(keys.leftKey())) return -1;
        if (pressed.test(keys.rightKey())) return 1;
        return 0;
    }

    private void refreshSnapshot() {
        boolean creation = mode == Mode.CREATE;
        String sceneId = switch (mode) {
            case CREATE -> BackgroundSceneCatalog.FILE_CREATION_SCENE;
            case ERASE_PICK, ERASE_CONFIRM -> BackgroundSceneCatalog.FILE_ERASE_SCENE;
            case COPY_SOURCE, COPY_TARGET, COPY_CONFIRM -> BackgroundSceneCatalog.FILE_COPY_SCENE;
            case SELECT -> hasSavedFile()
                ? BackgroundSceneCatalog.FILE_SELECTION_COMMANDS_SCENE
                : BackgroundSceneCatalog.FILE_SELECTION_SCENE;
        };
        BackgroundScene scene = sceneCache.computeIfAbsent(sceneId, id ->
            Objects.requireNonNull(sceneProvider.apply(id), "sceneProvider returned null for " + id));
        int[] tilemap = scene.tilemap().clone();
        int[] attrmap = scene.attrmap().clone();
        int visibleSlot = selectedSlot < 3 ? selectedSlot : 0;
        int[] visibleName = creation ? currentName.clone() : savedNames[visibleSlot].clone();

        if (creation) {
            writeTile(tilemap, FILE_NEW_SAVE_SLOT_INDEX_OFFSET,
                FILE_NEW_SAVE_SLOT_1_TILE + selectedSlot);
            drawSaveSlotName(tilemap, FILE_NEW_NAME_OFFSET, currentName);
        } else if (mode == Mode.COPY_SOURCE || mode == Mode.COPY_TARGET
            || mode == Mode.COPY_CONFIRM) {
            for (int slot = 0; slot < 3; slot++) {
                drawSaveSlotName(tilemap, COPY_SOURCE_NAME_OFFSETS[slot], savedNames[slot]);
                drawSaveSlotName(tilemap, COPY_TARGET_NAME_OFFSETS[slot], savedNames[slot]);
            }
        } else {
            for (int slot = 0; slot < 3; slot++) {
                if ((saveFilesMask & (1 << slot)) != 0) {
                    drawSaveSlotName(tilemap, SAVE_NAME_OFFSETS[slot], savedNames[slot]);
                    if (mode == Mode.SELECT && slotStats[slot] != null) {
                        drawSaveSlotStats(tilemap, slot, slotStats[slot]);
                    }
                }
            }
        }
        if (mode == Mode.ERASE_CONFIRM || mode == Mode.COPY_CONFIRM) {
            for (int index = 0; index < QUIT_OK_TILES.length; index++) {
                writeTile(tilemap, QUIT_OK_OFFSET + index, QUIT_OK_TILES[index]);
            }
        }

        snapshot = new FileMenuFrameSnapshot(
            sceneId,
            selectedSlot,
            commandRowArrowShifted,
            creation,
            selectedCharacter,
            namePosition,
            visibleName,
            tilemap,
            attrmap,
            scene.palettes(),
            scene.objectPalettes(),
            buildSprites());
    }

    private List<IntroSprite> buildSprites() {
        List<IntroSprite> sprites = new ArrayList<>();
        if (mode == Mode.CREATE) {
            int oamX = nameCursorXPositions[selectedCharacter] + 0x04;
            int oamY = nameCursorYPositions[selectedCharacter] + 0x0B;
            sprites.add(sprite8x16FromOam(0xE0, oamX, oamY, false));
            if ((frameCounter & 0x10) != 0) {
                int positionX = namePositionXPositions[namePosition] + 0x0C;
                sprites.add(sprite8x16FromOam(0xE0, positionX, 0x18 + 0x0B, false));
            }
            return List.copyOf(sprites);
        }

        if (mode == Mode.SELECT && selectedSlot == 3 && (frameCounter & 0x10) == 0) {
            int oamX = commandRowArrowShifted ? 0x64 : 0x2C;
            sprites.add(spriteFromOam(0xBE, oamX, 0x88, false));
        }

        if (mode == Mode.SELECT || mode == Mode.ERASE_PICK || mode == Mode.COPY_SOURCE
            || mode == Mode.ERASE_CONFIRM) {
            addCursorPair(sprites, selectedSlot, 0x18, 0x20);
        } else {
            sprites.add(spriteFromOam(0xBE, 0x14,
                selectionCursorYPositions[sourceSlot] + 5, false));
            addCursorPair(sprites, mode == Mode.COPY_TARGET ? selectedSlot : targetSlot,
                0x58, 0x60);
        }
        if ((mode == Mode.ERASE_CONFIRM || mode == Mode.COPY_CONFIRM)
            && (frameCounter & 0x10) == 0) {
            sprites.add(spriteFromOam(0xBE, confirmationOk ? 0x6C : 0x28, 0x88, false));
        }
        return List.copyOf(sprites);
    }

    private void addCursorPair(List<IntroSprite> sprites, int slot, int firstX, int secondX) {
        int y = selectionCursorYPositions[slot];
        boolean flipped = (frameCounter & 0x08) == 0;
        sprites.add(sprite8x16FromOam(flipped ? 0x02 : 0x00, firstX, y, flipped));
        sprites.add(sprite8x16FromOam(flipped ? 0x00 : 0x02, secondX, y, flipped));
    }

    private static IntroSprite spriteFromOam(int tile, int oamX, int oamY, boolean flipX) {
        return new IntroSprite(tile, oamX - 8, oamY - 8, 0, flipX, false);
    }

    private static IntroSprite sprite8x16FromOam(int tile, int oamX, int oamY, boolean flipX) {
        return new IntroSprite(tile, oamX - 8, oamY - 16, 0, flipX, false, 16);
    }

    private static void drawSaveSlotStats(int[] tilemap, int slot, SlotStats stats) {
        int heartOffset = FIRST_SAVE_HEART_OFFSET + slot * SAVE_SLOT_ROW_STRIDE;
        int hearts = Math.clamp(stats.maxHearts(), 0, HEARTS_PER_ROW * 2);
        for (int heart = 0; heart < hearts; heart++) {
            writeTile(tilemap, heartOffset + (heart / HEARTS_PER_ROW) * 32 + heart % HEARTS_PER_ROW,
                SAVE_HEART_TILE);
        }

        int deathOffset = FIRST_SAVE_DEATH_COUNT_OFFSET + slot * SAVE_SLOT_ROW_STRIDE;
        int deaths = Math.clamp(stats.deathCount(), 0, 999);
        writeTile(tilemap, deathOffset, SAVE_DIGIT_ZERO_TILE + deaths / 100);
        writeTile(tilemap, deathOffset + 1, SAVE_DIGIT_ZERO_TILE + (deaths / 10) % 10);
        writeTile(tilemap, deathOffset + 2, SAVE_DIGIT_ZERO_TILE + deaths % 10);
    }

    private void drawSaveSlotName(int[] tilemap, int targetOffset, int[] name) {
        for (int index = 0; index < NAME_LENGTH; index++) {
            writeTile(tilemap, targetOffset + index, tileForStoredName(name[index]));
            writeTile(tilemap, targetOffset - 0x20 + index, DARK_BACKGROUND_TILE);
        }
        writeTile(tilemap, targetOffset + NAME_LENGTH, DARK_BACKGROUND_TILE);
        writeTile(tilemap, targetOffset - 0x20 + NAME_LENGTH, DARK_BACKGROUND_TILE);
    }

    private int tileForStoredName(int storedCodepoint) {
        if (storedCodepoint == 0) {
            return DARK_BACKGROUND_TILE;
        }
        int codepointIndex = (storedCodepoint - 1) & 0xFF;
        return codepointToTileMap[codepointIndex];
    }

    private static void writeTile(int[] tilemap, int offset, int tile) {
        if (offset < 0 || offset >= tilemap.length) {
            throw new IllegalArgumentException("BG map write outside decoded map: " + offset);
        }
        tilemap[offset] = tile & 0xFF;
    }

    private boolean hasSavedFile() {
        return saveFilesMask != 0;
    }

    private static boolean hasStoredName(int[] name) {
        for (int value : name) {
            if ((value & 0xFF) != 0) {
                return true;
            }
        }
        return false;
    }

    private static int[][] copySavedNames(int[][] names) {
        int[][] copy = new int[3][NAME_LENGTH];
        if (names == null) {
            return copy;
        }
        for (int slot = 0; slot < Math.min(copy.length, names.length); slot++) {
            if (names[slot] != null) {
                System.arraycopy(names[slot], 0, copy[slot], 0,
                    Math.min(NAME_LENGTH, names[slot].length));
            }
        }
        return copy;
    }
}
