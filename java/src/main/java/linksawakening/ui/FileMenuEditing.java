package linksawakening.ui;

import static linksawakening.ui.FileMenuController.*;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import linksawakening.cutscene.IntroSprite;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;

/** FileMenuEditing behavior on the shared FileMenuController state. */
abstract class FileMenuEditing extends FileMenuFlow {

    protected FileMenuEditing(
            FileMenuRomData romData,
            Function<String, BackgroundScene> sceneProvider,
            int saveFilesMask,
            int[][] savedNames) {
        super(romData, sceneProvider, saveFilesMask, savedNames);
    }

    protected FileMenuEditing(
            FileMenuRomData romData,
            Function<String, BackgroundScene> sceneProvider,
            int saveFilesMask,
            int[][] savedNames,
            int[] maxHearts,
            int[] deathCountsBcd) {
        super(romData, sceneProvider, saveFilesMask, savedNames, maxHearts, deathCountsBcd);
    }

    protected void refreshSnapshot() {
        boolean creation = mode == Mode.CREATE;
        String sceneId =
                switch (mode) {
                    case CREATE -> BackgroundSceneCatalog.FILE_CREATION_SCENE;
                    case ERASE_PICK, ERASE_CONFIRM -> BackgroundSceneCatalog.FILE_ERASE_SCENE;
                    case COPY_SOURCE, COPY_TARGET, COPY_CONFIRM -> BackgroundSceneCatalog.FILE_COPY_SCENE;
                    case SELECT -> hasSavedFile()
                            ? BackgroundSceneCatalog.FILE_SELECTION_COMMANDS_SCENE
                            : BackgroundSceneCatalog.FILE_SELECTION_SCENE;
                };
        BackgroundScene scene = sceneCache.computeIfAbsent(
                sceneId,
                id -> Objects.requireNonNull(sceneProvider.apply(id), "sceneProvider returned null for " + id));
        int[] tilemap = scene.tilemap().clone();
        int[] attrmap = scene.attrmap().clone();
        int visibleSlot = selectedSlot < 3 ? selectedSlot : 0;
        int[] visibleName = creation ? currentName.clone() : savedNames[visibleSlot].clone();

        if (creation) {
            writeTile(tilemap, FILE_NEW_SAVE_SLOT_INDEX_OFFSET, FILE_NEW_SAVE_SLOT_1_TILE + selectedSlot);
            drawSaveSlotName(tilemap, FILE_NEW_NAME_OFFSET, currentName);
        } else if (mode == Mode.COPY_SOURCE || mode == Mode.COPY_TARGET || mode == Mode.COPY_CONFIRM) {
            for (int slot = 0; slot < 3; slot++) {
                drawSaveSlotName(tilemap, COPY_SOURCE_NAME_OFFSETS[slot], savedNames[slot]);
                drawSaveSlotName(tilemap, COPY_TARGET_NAME_OFFSETS[slot], savedNames[slot]);
            }
        } else {
            for (int slot = 0; slot < 3; slot++) {
                if ((saveFilesMask & (1 << slot)) != 0) {
                    drawSaveSlotName(tilemap, SAVE_NAME_OFFSETS[slot], savedNames[slot]);
                    // BuildSaveSlotHeartsDrawCommand draws full containers, even at low health.
                    for (int heart = 0; heart < Math.min(14, maxHearts[slot]); heart++) {
                        writeTile(tilemap, 0xCB + slot * 0x60 + (heart / 7) * 0x20 + heart % 7, 0xAE);
                    }
                    for (int digit = 0; digit < 3; digit++) {
                        int value = (deathCountsBcd[slot] >> ((2 - digit) * 4)) & 15;
                        writeTile(tilemap, 0xE7 + slot * 0x60 + digit, deathDigitTiles[Math.min(9, value)]);
                    }
                }
            }
        }
        if (mode == Mode.ERASE_CONFIRM || mode == Mode.COPY_CONFIRM) {
            for (int index = 0; index < quitOkTiles.length; index++) {
                writeTile(tilemap, QUIT_OK_OFFSET + index, quitOkTiles[index]);
            }
        }

        // FileDeletionState11Handler / func_001_512C blink the selected names.
        if (mode == Mode.ERASE_CONFIRM && (frameCounter & 0x10) == 0) {
            drawSaveSlotName(tilemap, SAVE_NAME_OFFSETS[sourceSlot], new int[5]);
        }
        if ((mode == Mode.COPY_TARGET || mode == Mode.COPY_CONFIRM) && (frameCounter & 0x10) != 0) {
            drawSaveSlotName(tilemap, COPY_SOURCE_NAME_OFFSETS[sourceSlot], new int[5]);
            if (mode == Mode.COPY_CONFIRM) {
                drawSaveSlotName(tilemap, COPY_TARGET_NAME_OFFSETS[targetSlot], new int[5]);
            }
        }

        snapshot = snapshotBuilder
                .sceneId(sceneId)
                .selectedSlot(selectedSlot)
                .commandRowArrowShifted(commandRowArrowShifted)
                .creation(creation)
                .selectedCharacter(selectedCharacter)
                .namePosition(namePosition)
                .nameBytes(visibleName)
                .tilemap(tilemap)
                .attrmap(attrmap)
                .bgPalettes(scene.palettes())
                .objectPalettes(scene.objectPalettes())
                .sprites(buildSprites())
                .build();
    }

    protected List<IntroSprite> buildSprites() {
        List<IntroSprite> sprites = spriteScratch;
        sprites.clear();
        if (mode == Mode.CREATE) {
            int oamX = nameCursorXPositions[selectedCharacter] + 0x04;
            int oamY = nameCursorYPositions[selectedCharacter] + 0x0B;
            sprites.add(spriteFromOam(0xE0, oamX, oamY, false));
            if ((frameCounter & 0x10) != 0) {
                int positionX = namePositionXPositions[namePosition] + 0x0C;
                sprites.add(spriteFromOam(0xE0, positionX, 0x18 + 0x0B, false));
            }
            return List.copyOf(sprites);
        }

        if (mode == Mode.SELECT && selectedSlot == 3 && (frameCounter & 0x10) == 0) {
            int oamX = commandRowArrowShifted ? 0x64 : 0x2C;
            sprites.add(spriteFromOam(0xBE, oamX, 0x88, false));
        }

        if (mode == Mode.SELECT || mode == Mode.ERASE_PICK || mode == Mode.ERASE_CONFIRM) {
            addCursorPair(sprites, selectedSlot, 0x18, 0x20);
        } else if (mode == Mode.COPY_SOURCE) {
            addCursorPair(sprites, selectedSlot, 0x10, 0x18);
        } else {
            sprites.add(spriteFromOam(0xBE, 0x14, selectionCursorYPositions[sourceSlot] + 5, false));
            if (mode == Mode.COPY_CONFIRM) {
                sprites.add(spriteFromOam(0xBE, 0x5C, selectionCursorYPositions[targetSlot] + 5, false));
            } else {
                addCursorPair(sprites, selectedSlot, selectedSlot == 3 ? 0x10 : 0x58, selectedSlot == 3 ? 0x18 : 0x60);
            }
        }
        if ((mode == Mode.ERASE_CONFIRM || mode == Mode.COPY_CONFIRM) && (frameCounter & 0x10) == 0) {
            sprites.add(spriteFromOam(0xBE, confirmationOk ? 0x6C : 0x28, 0x88, false));
        }
        return List.copyOf(sprites);
    }

    protected void addCursorPair(List<IntroSprite> sprites, int slot, int firstX, int secondX) {
        int y = selectionCursorYPositions[slot];
        boolean flipped = (frameCounter & 0x08) == 0;
        sprites.add(spriteFromOam(flipped ? 0x02 : 0x00, firstX, y, flipped));
        sprites.add(spriteFromOam(flipped ? 0x00 : 0x02, secondX, y, flipped));
    }

    protected void drawSaveSlotName(int[] tilemap, int targetOffset, int[] name) {
        for (int index = 0; index < NAME_LENGTH; index++) {
            writeTile(tilemap, targetOffset + index, tileForStoredName(name[index]));
            writeTile(tilemap, targetOffset - 0x20 + index, DARK_BACKGROUND_TILE);
        }
    }

    protected int tileForStoredName(int storedCodepoint) {
        if (storedCodepoint == 0) {
            return DARK_BACKGROUND_TILE;
        }
        int codepointIndex = (storedCodepoint - 1) & 0xFF;
        return codepointToTileMap[codepointIndex];
    }

    protected boolean hasSavedFile() {
        return saveFilesMask != 0;
    }
}
