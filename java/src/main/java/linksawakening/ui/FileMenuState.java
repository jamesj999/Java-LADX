package linksawakening.ui;

import static linksawakening.ui.FileMenuController.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import linksawakening.cutscene.IntroSprite;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.scene.BackgroundScene;

/**
 * Input/state model for the ROM-backed file-selection and New Game screens.
 */

/** Shared state and dependency ownership for FileMenuController. */
abstract class FileMenuState {

    protected static final int NAME_LENGTH = 5;
    protected static final int NAME_ENTRY_CHARACTER_COUNT = 0x40;
    protected static final int BG_MAP_BASE = 0x9800;
    protected static final int FILE_NEW_SAVE_SLOT_INDEX_OFFSET = 0x9849 - BG_MAP_BASE;
    protected static final int FILE_NEW_NAME_OFFSET = 0x984A - BG_MAP_BASE;
    protected static final int[] SAVE_NAME_OFFSETS = {0x98C5 - BG_MAP_BASE, 0x9925 - BG_MAP_BASE, 0x9985 - BG_MAP_BASE};
    protected static final int[] COPY_SOURCE_NAME_OFFSETS = {0xC4, 0x124, 0x184};
    protected static final int[] COPY_TARGET_NAME_OFFSETS = {0xCD, 0x12D, 0x18D};
    protected static final int QUIT_OK_OFFSET = 0x99E4 - BG_MAP_BASE;
    protected static final int FILE_NEW_SAVE_SLOT_1_TILE = 0xAB;
    protected static final int DARK_BACKGROUND_TILE = 0x7E;

    protected final Function<String, BackgroundScene> sceneProvider;
    protected final Map<String, BackgroundScene> sceneCache = new HashMap<>();
    protected final int[] selectionCursorYPositions;
    protected final int[] nameEntryCharacterTable;
    protected final int[] nameCursorYPositions;
    protected final int[] nameCursorXPositions;
    protected final int[] namePositionXPositions;
    protected final int[] codepointToTileMap;
    protected final int saveFilesMask;
    protected final int[][] savedNames;
    protected final int[] maxHearts;
    protected final int[] deathCountsBcd;
    protected final int[] deathDigitTiles;
    protected final int[] quitOkTiles;

    protected int selectedSlot;
    protected boolean commandRowArrowShifted;
    protected Mode mode = Mode.SELECT;
    protected int sourceSlot;
    protected int targetSlot;
    protected boolean confirmationOk;
    protected int selectedCharacter;
    protected int namePosition;
    protected int frameCounter;
    protected int[] currentName = new int[NAME_LENGTH];
    protected FileMenuFrameSnapshot snapshot;

    public FileMenuState(
            FileMenuRomData romData,
            Function<String, BackgroundScene> sceneProvider,
            int saveFilesMask,
            int[][] savedNames) {
        this(romData, sceneProvider, saveFilesMask, savedNames, new int[3], new int[3]);
    }

    public FileMenuState(
            FileMenuRomData romData,
            Function<String, BackgroundScene> sceneProvider,
            int saveFilesMask,
            int[][] savedNames,
            int[] maxHearts,
            int[] deathCountsBcd) {
        Objects.requireNonNull(romData, "romData");
        if (maxHearts.length != 3 || deathCountsBcd.length != 3) {
            throw new IllegalArgumentException("File summaries must contain three slots");
        }
        this.maxHearts = maxHearts.clone();
        this.deathCountsBcd = deathCountsBcd.clone();
        this.deathDigitTiles = romData.deathDigitTiles();
        this.quitOkTiles = romData.quitOkTiles();
        this.sceneProvider = Objects.requireNonNull(sceneProvider, "sceneProvider");
        this.selectionCursorYPositions = romData.selectionCursorYPositions();
        this.nameEntryCharacterTable = romData.nameEntryCharacterTable();
        this.nameCursorYPositions = romData.nameCursorYPositions();
        this.nameCursorXPositions = romData.nameCursorXPositions();
        this.namePositionXPositions = romData.namePositionXPositions();
        this.codepointToTileMap = romData.codepointToTileMap();
        this.saveFilesMask = saveFilesMask & 0x07;
        this.savedNames = copySavedNames(savedNames);
        refreshSnapshot();
    }

    protected static boolean activated(InputState inputState, InputConfig inputConfig) {
        return inputState.wasPressed(inputConfig.aKey()) || inputState.wasPressed(inputConfig.menuOpenKey());
    }

    protected static boolean cancelled(InputState inputState, InputConfig inputConfig) {
        return inputState.wasPressed(inputConfig.bKey());
    }

    protected static IntroSprite spriteFromOam(int tile, int oamX, int oamY, boolean flipX) {
        return new IntroSprite(tile, oamX - 8, oamY - 16, 0, flipX, false, 16);
    }

    protected static void writeTile(int[] tilemap, int offset, int tile) {
        if (offset < 0 || offset >= tilemap.length) {
            throw new IllegalArgumentException("BG map write outside decoded map: " + offset);
        }
        tilemap[offset] = tile & 0xFF;
    }

    protected static boolean hasStoredName(int[] name) {
        for (int value : name) {
            if ((value & 0xFF) != 0) {
                return true;
            }
        }
        return false;
    }

    protected static int[][] copySavedNames(int[][] names) {
        int[][] copy = new int[3][NAME_LENGTH];
        if (names == null) {
            return copy;
        }
        for (int slot = 0; slot < Math.min(copy.length, names.length); slot++) {
            if (names[slot] != null) {
                System.arraycopy(names[slot], 0, copy[slot], 0, Math.min(NAME_LENGTH, names[slot].length));
            }
        }
        return copy;
    }

    public abstract FileMenuFrameSnapshot snapshot();

    public abstract Mode mode();

    public abstract FileMenuAction tick(InputState inputState, InputConfig inputConfig);

    protected abstract FileMenuAction tickSelection(InputState inputState, InputConfig inputConfig);

    protected abstract FileMenuAction tickErasePick(InputState inputState, InputConfig inputConfig);

    protected abstract FileMenuAction tickEraseConfirm(InputState inputState, InputConfig inputConfig);

    protected abstract FileMenuAction tickCopySource(InputState inputState, InputConfig inputConfig);

    protected abstract FileMenuAction tickCopyTarget(InputState inputState, InputConfig inputConfig);

    protected abstract FileMenuAction tickCopyConfirm(InputState inputState, InputConfig inputConfig);

    protected abstract void moveFourRowCursor(InputState inputState, InputConfig inputConfig);

    protected abstract void toggleConfirmation(InputState inputState, InputConfig inputConfig);

    protected abstract FileMenuAction returnToSelection();

    protected abstract FileMenuAction tickCreation(InputState inputState, InputConfig inputConfig);

    protected abstract void refreshSnapshot();

    protected abstract List<IntroSprite> buildSprites();

    protected abstract void addCursorPair(List<IntroSprite> sprites, int slot, int firstX, int secondX);

    protected abstract void drawSaveSlotName(int[] tilemap, int targetOffset, int[] name);

    protected abstract int tileForStoredName(int storedCodepoint);

    protected abstract boolean hasSavedFile();
}
