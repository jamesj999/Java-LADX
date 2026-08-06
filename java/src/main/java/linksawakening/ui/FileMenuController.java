package linksawakening.ui;

import linksawakening.cutscene.IntroSprite;
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

/**
 * Input/state model for the ROM-backed file-selection and New Game screens.
 */
public final class FileMenuController {

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
    private static final int FILE_NEW_SAVE_SLOT_1_TILE = 0xAB;
    private static final int DARK_BACKGROUND_TILE = 0x7E;

    private final Function<String, BackgroundScene> sceneProvider;
    private final Map<String, BackgroundScene> sceneCache = new HashMap<>();
    private final int[] selectionCursorYPositions;
    private final int[] nameEntryCharacterTable;
    private final int[] nameCursorYPositions;
    private final int[] nameCursorXPositions;
    private final int[] namePositionXPositions;
    private final int[] codepointToTileMap;
    private final int saveFilesMask;
    private final int[][] savedNames;

    private int selectedSlot;
    private boolean commandRowArrowShifted;
    private boolean creation;
    private int selectedCharacter;
    private int namePosition;
    private int frameCounter;
    private int[] currentName = new int[NAME_LENGTH];
    private FileMenuFrameSnapshot snapshot;

    public FileMenuController(FileMenuRomData romData,
                              Function<String, BackgroundScene> sceneProvider,
                              int saveFilesMask,
                              int[][] savedNames) {
        Objects.requireNonNull(romData, "romData");
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

    public FileMenuFrameSnapshot snapshot() {
        return snapshot;
    }

    public FileMenuAction tick(InputState inputState, InputConfig inputConfig) {
        Objects.requireNonNull(inputState, "inputState");
        Objects.requireNonNull(inputConfig, "inputConfig");
        frameCounter = (frameCounter + 1) & 0xFF;

        FileMenuAction action = creation
            ? tickCreation(inputState, inputConfig)
            : tickSelection(inputState, inputConfig);
        refreshSnapshot();
        return action;
    }

    private FileMenuAction tickSelection(InputState inputState, InputConfig inputConfig) {
        int maximumSlot = hasSavedFile() ? 3 : 2;
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

        if (inputState.wasPressed(inputConfig.aKey())
            || inputState.wasPressed(inputConfig.menuOpenKey())) {
            if (selectedSlot < 3) {
                int[] name = savedNames[selectedSlot];
                if (hasStoredName(name)) {
                    return new FileMenuAction(FileMenuAction.Type.LOAD_GAME, selectedSlot, name);
                }
                creation = true;
                selectedCharacter = 0;
                namePosition = 0;
                currentName = new int[NAME_LENGTH];
            }
        }
        return FileMenuAction.none(selectedSlot);
    }

    private FileMenuAction tickCreation(InputState inputState, InputConfig inputConfig) {
        if (inputState.wasPressed(inputConfig.leftKey())) {
            selectedCharacter = (selectedCharacter + NAME_ENTRY_CHARACTER_COUNT - 1)
                % NAME_ENTRY_CHARACTER_COUNT;
        } else if (inputState.wasPressed(inputConfig.rightKey())) {
            selectedCharacter = (selectedCharacter + 1) % NAME_ENTRY_CHARACTER_COUNT;
        } else if (inputState.wasPressed(inputConfig.upKey())) {
            selectedCharacter = (selectedCharacter + NAME_ENTRY_CHARACTER_COUNT - 0x10)
                % NAME_ENTRY_CHARACTER_COUNT;
        } else if (inputState.wasPressed(inputConfig.downKey())) {
            selectedCharacter = (selectedCharacter + 0x10) % NAME_ENTRY_CHARACTER_COUNT;
        }

        if (inputState.wasPressed(inputConfig.bKey())) {
            namePosition = Math.max(0, namePosition - 1);
        } else if (inputState.wasPressed(inputConfig.aKey())) {
            currentName[namePosition] = nameEntryCharacterTable[selectedCharacter];
            namePosition = Math.min(NAME_LENGTH - 1, namePosition + 1);
        }

        if (inputState.wasPressed(inputConfig.menuOpenKey())) {
            savedNames[selectedSlot] = currentName.clone();
            return new FileMenuAction(FileMenuAction.Type.START_NEW_GAME, selectedSlot, currentName);
        }
        return FileMenuAction.none(selectedSlot);
    }

    private void refreshSnapshot() {
        String sceneId = creation
            ? BackgroundSceneCatalog.FILE_CREATION_SCENE
            : hasSavedFile()
                ? BackgroundSceneCatalog.FILE_SELECTION_COMMANDS_SCENE
                : BackgroundSceneCatalog.FILE_SELECTION_SCENE;
        BackgroundScene scene = sceneCache.computeIfAbsent(sceneId, id ->
            Objects.requireNonNull(sceneProvider.apply(id), "sceneProvider returned null for " + id));
        int[] tilemap = scene.tilemap().clone();
        int[] attrmap = scene.attrmap().clone();
        int[] visibleName = creation ? currentName.clone() : savedNames[selectedSlot < 3 ? selectedSlot : 0].clone();

        if (creation) {
            writeTile(tilemap, FILE_NEW_SAVE_SLOT_INDEX_OFFSET,
                FILE_NEW_SAVE_SLOT_1_TILE + selectedSlot);
            drawSaveSlotName(tilemap, FILE_NEW_NAME_OFFSET, currentName);
        } else {
            for (int slot = 0; slot < 3; slot++) {
                if ((saveFilesMask & (1 << slot)) != 0) {
                    drawSaveSlotName(tilemap, SAVE_NAME_OFFSETS[slot], savedNames[slot]);
                }
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
        if (creation) {
            int oamX = nameCursorXPositions[selectedCharacter] + 0x04;
            int oamY = nameCursorYPositions[selectedCharacter] + 0x0B;
            sprites.add(spriteFromOam(0xE0, oamX, oamY, false));
            if ((frameCounter & 0x10) != 0) {
                int positionX = namePositionXPositions[namePosition] + 0x0C;
                sprites.add(spriteFromOam(0xE0, positionX, 0x18 + 0x0B, false));
            }
            return List.copyOf(sprites);
        }

        if (selectedSlot == 3 && (frameCounter & 0x10) == 0) {
            int oamX = commandRowArrowShifted ? 0x64 : 0x2C;
            sprites.add(spriteFromOam(0xBE, oamX, 0x88, false));
        }

        int y = selectionCursorYPositions[selectedSlot];
        boolean flipped = (frameCounter & 0x08) == 0;
        int firstTile = flipped ? 0x02 : 0x00;
        int secondTile = flipped ? 0x00 : 0x02;
        sprites.add(spriteFromOam(firstTile, 0x18, y, flipped));
        sprites.add(spriteFromOam(secondTile, 0x20, y, flipped));
        return List.copyOf(sprites);
    }

    private static IntroSprite spriteFromOam(int tile, int oamX, int oamY, boolean flipX) {
        return new IntroSprite(tile, oamX - 8, oamY - 8, 0, flipX, false);
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
