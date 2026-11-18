package linksawakening.ui;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.rom.RomBank;
import linksawakening.save.SaveRamImage;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

final class FileMenuControllerTest {

    private static final InputConfig INPUT = new InputConfig(
        GLFW_KEY_ENTER, GLFW_KEY_UP, GLFW_KEY_DOWN, GLFW_KEY_LEFT, GLFW_KEY_RIGHT,
        GLFW_KEY_A, GLFW_KEY_B);

    @Test
    void fileSelectionAndNameInputEmitRomJingles() {
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        FileMenuController controller = newController(0, new int[3][5], sounds::add);

        press(controller, GLFW_KEY_RIGHT);
        press(controller, GLFW_KEY_DOWN);
        press(controller, GLFW_KEY_A);
        press(controller, GLFW_KEY_RIGHT);
        press(controller, GLFW_KEY_A);
        press(controller, GLFW_KEY_B);
        assertEquals(FileMenuAction.Type.REGISTER_FILE, press(controller, GLFW_KEY_ENTER).type());

        assertEquals(List.of(GameplaySoundEvent.MENU_MOVE, GameplaySoundEvent.MENU_VALIDATE,
            GameplaySoundEvent.MENU_MOVE, GameplaySoundEvent.MENU_VALIDATE,
            GameplaySoundEvent.MENU_VALIDATE, GameplaySoundEvent.MENU_VALIDATE), sounds);
    }

    @Test
    void emptySlotAEntersCreationAndWritesTheSelectedSlotAndBlankName() {
        FileMenuController controller = newController(0, new int[3][5]);

        press(controller, GLFW_KEY_A);

        assertTrue(controller.snapshot().creation());
        assertEquals("creation", controller.snapshot().sceneId());
        assertEquals(0xAB, controller.snapshot().tilemap()[0x49]);
        assertArrayEquals(new int[] { 0x7E, 0x7E, 0x7E, 0x7E, 0x7E },
            Arrays.copyOfRange(controller.snapshot().tilemap(), 0x4A, 0x4F));
        assertEquals(1, controller.snapshot().sprites().size());
        assertEquals(0xE0, controller.snapshot().sprites().get(0).tileIndex());
        assertEquals(0x20 + 0x0B - 0x10, controller.snapshot().sprites().get(0).y());
        assertEquals(16, controller.snapshot().sprites().get(0).height());
    }

    @Test
    void quickTapActivatesSelectedSlotOnce() {
        FileMenuController controller = newController(0, new int[3][5]);
        InputState input = new InputState();
        input.onKeyEvent(GLFW_KEY_A, GLFW_PRESS);
        input.onKeyEvent(GLFW_KEY_A, GLFW_RELEASE);

        controller.tick(input, INPUT);
        assertTrue(controller.snapshot().creation());
        assertFalse(input.isDown(GLFW_KEY_A));

        input.tickEdges();
        assertFalse(input.wasPressed(GLFW_KEY_A));
        assertFalse(input.wasReleased(GLFW_KEY_A));
    }

    @Test
    void selectionWrapsAcrossTheCommandRowOnlyWhenAFileExists() {
        FileMenuController noFiles = newController(0, new int[3][5]);
        press(noFiles, GLFW_KEY_UP);
        assertEquals(2, noFiles.snapshot().selectedSlot());

        FileMenuController withFile = newController(1, new int[3][5]);
        press(withFile, GLFW_KEY_UP);
        assertEquals(3, withFile.snapshot().selectedSlot());
        assertEquals(0x24, withFile.snapshot().sprites().get(0).x());
        press(withFile, GLFW_KEY_DOWN);
        assertEquals(0, withFile.snapshot().selectedSlot());
    }

    @Test
    void commandRowLeftAndRightToggleTheRomArrowPosition() {
        FileMenuController controller = newController(1, new int[3][5]);
        press(controller, GLFW_KEY_UP);

        assertFalse(controller.snapshot().commandRowArrowShifted());
        assertEquals(0x24, controller.snapshot().sprites().get(0).x());

        press(controller, GLFW_KEY_RIGHT);

        assertTrue(controller.snapshot().commandRowArrowShifted());
        assertEquals(0x5C, controller.snapshot().sprites().get(0).x());
        assertEquals(0xBE, controller.snapshot().sprites().get(0).tileIndex());
    }

    @Test
    void eraseRequiresAStoredFileAndExplicitOkConfirmation() {
        int[][] names = {{1, 2, 3, 4, 5}, {}, {}};
        FileMenuController controller = newController(1, names);
        press(controller, GLFW_KEY_UP);
        press(controller, GLFW_KEY_A);

        assertEquals(FileMenuController.Mode.ERASE_PICK, controller.mode());
        assertEquals(BackgroundSceneCatalog.FILE_ERASE_SCENE, controller.snapshot().sceneId());
        press(controller, GLFW_KEY_DOWN);
        assertEquals(FileMenuAction.Type.NONE, press(controller, GLFW_KEY_A).type());
        assertEquals(FileMenuController.Mode.ERASE_PICK, controller.mode());
        press(controller, GLFW_KEY_UP);
        press(controller, GLFW_KEY_A);
        assertEquals(FileMenuController.Mode.ERASE_CONFIRM, controller.mode());

        assertEquals(FileMenuAction.Type.NONE, press(controller, GLFW_KEY_A).type());
        assertEquals(FileMenuController.Mode.SELECT, controller.mode());

        press(controller, GLFW_KEY_UP);
        press(controller, GLFW_KEY_A);
        press(controller, GLFW_KEY_A);
        press(controller, GLFW_KEY_RIGHT);
        FileMenuAction action = press(controller, GLFW_KEY_A);
        assertEquals(FileMenuAction.Type.ERASE_SLOT, action.type());
        assertEquals(0, action.selectedSlot());
    }

    @Test
    void eraseBReturnsFromConfirmationThenCancelsToSelection() {
        FileMenuController controller = newController(1, new int[][] {{1}, {}, {}});
        press(controller, GLFW_KEY_UP);
        press(controller, GLFW_KEY_A);
        press(controller, GLFW_KEY_A);

        press(controller, GLFW_KEY_B);
        assertEquals(FileMenuController.Mode.ERASE_PICK, controller.mode());
        press(controller, GLFW_KEY_B);
        assertEquals(FileMenuController.Mode.SELECT, controller.mode());
    }

    @Test
    void copySelectsNonemptySourceDestinationAndExplicitOk() {
        FileMenuController controller = newController(1, new int[][] {{1, 2}, {}, {}});
        press(controller, GLFW_KEY_UP);
        press(controller, GLFW_KEY_RIGHT);
        press(controller, GLFW_KEY_A);

        assertEquals(FileMenuController.Mode.COPY_SOURCE, controller.mode());
        assertEquals(BackgroundSceneCatalog.FILE_COPY_SCENE, controller.snapshot().sceneId());
        press(controller, GLFW_KEY_A);
        assertEquals(FileMenuController.Mode.COPY_TARGET, controller.mode());
        press(controller, GLFW_KEY_DOWN);
        assertEquals(0x53 - 16, controller.snapshot().sprites().get(1).y());
        press(controller, GLFW_KEY_A);
        assertEquals(FileMenuController.Mode.COPY_CONFIRM, controller.mode());
        press(controller, GLFW_KEY_RIGHT);

        FileMenuAction action = press(controller, GLFW_KEY_A);
        assertEquals(FileMenuAction.Type.COPY_SLOT, action.type());
        assertEquals(0, action.selectedSlot());
        assertEquals(1, action.targetSlot());
    }

    @Test
    void copyAllowsOverwritingAnOccupiedDestination() {
        FileMenuController controller = newController(0x03,
            new int[][] {{1, 2}, {3, 4}, {}});
        enterCopySourceSelection(controller);
        press(controller, GLFW_KEY_A);
        press(controller, GLFW_KEY_DOWN);
        press(controller, GLFW_KEY_A);
        press(controller, GLFW_KEY_RIGHT);

        FileMenuAction action = press(controller, GLFW_KEY_A);
        assertEquals(FileMenuAction.Type.COPY_SLOT, action.type());
        assertEquals(0, action.selectedSlot());
        assertEquals(1, action.targetSlot());
    }

    @Test
    void copyAllowsTheSourceSlotAsItsDestination() {
        FileMenuController controller = newController(1, new int[][] {{1, 2}, {}, {}});
        enterCopySourceSelection(controller);
        press(controller, GLFW_KEY_A);
        press(controller, GLFW_KEY_A);
        press(controller, GLFW_KEY_RIGHT);

        FileMenuAction action = press(controller, GLFW_KEY_A);
        assertEquals(FileMenuAction.Type.COPY_SLOT, action.type());
        assertEquals(0, action.selectedSlot());
        assertEquals(0, action.targetSlot());
    }

    @Test
    void copyRejectsEmptySourceAndBBacktracksEachStage() {
        FileMenuController controller = newController(1, new int[][] {{1}, {}, {}});
        press(controller, GLFW_KEY_UP);
        press(controller, GLFW_KEY_RIGHT);
        press(controller, GLFW_KEY_A);
        press(controller, GLFW_KEY_DOWN);
        assertEquals(FileMenuAction.Type.NONE, press(controller, GLFW_KEY_A).type());
        assertEquals(FileMenuController.Mode.COPY_SOURCE, controller.mode());
        press(controller, GLFW_KEY_UP);
        press(controller, GLFW_KEY_A);
        press(controller, GLFW_KEY_DOWN);
        press(controller, GLFW_KEY_A);
        press(controller, GLFW_KEY_B);
        assertEquals(FileMenuController.Mode.COPY_TARGET, controller.mode());
        press(controller, GLFW_KEY_B);
        assertEquals(FileMenuController.Mode.COPY_SOURCE, controller.mode());
        press(controller, GLFW_KEY_B);
        assertEquals(FileMenuController.Mode.SELECT, controller.mode());
    }

    @Test
    void quitRowCancelsErasePickerAndCopyTarget() {
        FileMenuController erase = newController(1, new int[][] {{1}, {}, {}});
        press(erase, GLFW_KEY_UP);
        press(erase, GLFW_KEY_A);
        press(erase, GLFW_KEY_UP);
        press(erase, GLFW_KEY_A);
        assertEquals(FileMenuController.Mode.SELECT, erase.mode());

        FileMenuController copy = newController(1, new int[][] {{1}, {}, {}});
        press(copy, GLFW_KEY_UP);
        press(copy, GLFW_KEY_RIGHT);
        press(copy, GLFW_KEY_A);
        press(copy, GLFW_KEY_A);
        press(copy, GLFW_KEY_UP);
        press(copy, GLFW_KEY_A);
        assertEquals(FileMenuController.Mode.SELECT, copy.mode());
    }

    @Test
    void creationCharacterMovementUsesTheRomTableGridAndWrapsAtFortyCharacters() {
        FileMenuController controller = newController(0, new int[3][5]);
        press(controller, GLFW_KEY_A);

        assertEquals(0, controller.snapshot().selectedCharacter());
        press(controller, GLFW_KEY_LEFT);
        assertEquals(0x3F, controller.snapshot().selectedCharacter());
        press(controller, GLFW_KEY_UP);
        assertEquals(0x2F, controller.snapshot().selectedCharacter());
        press(controller, GLFW_KEY_DOWN);
        assertEquals(0x3F, controller.snapshot().selectedCharacter());
        press(controller, GLFW_KEY_RIGHT);
        assertEquals(0, controller.snapshot().selectedCharacter());
    }

    @Test
    void holdingDirectionInNameEntryRepeatsAfterTwentyFourFramesThenEveryThree() {
        FileMenuController controller = newController(0, new int[3][5]);
        press(controller, GLFW_KEY_A);
        InputState input = new InputState();
        input.onKeyEvent(GLFW_KEY_RIGHT, GLFW_PRESS);
        controller.tick(input, INPUT);
        input.tickEdges();
        for (int frame = 0; frame < 23; frame++) {
            controller.tick(input, INPUT);
        }
        assertEquals(1, controller.snapshot().selectedCharacter());
        controller.tick(input, INPUT);
        assertEquals(2, controller.snapshot().selectedCharacter());
        for (int frame = 0; frame < 3; frame++) {
            controller.tick(input, INPUT);
        }
        assertEquals(3, controller.snapshot().selectedCharacter());
        input.onKeyEvent(GLFW_KEY_RIGHT, GLFW_RELEASE);
        controller.tick(input, INPUT);
        input.onKeyEvent(GLFW_KEY_RIGHT, GLFW_PRESS);
        input.onKeyEvent(GLFW_KEY_DOWN, GLFW_PRESS);
        controller.tick(input, INPUT);
        assertEquals(0x13, controller.snapshot().selectedCharacter());
    }

    @Test
    void creationAAddsCharactersBBackspacesAndStartCommitsFiveBytes() {
        FileMenuController controller = newController(0, new int[3][5]);
        press(controller, GLFW_KEY_A);

        for (int i = 0; i < 5; i++) {
            press(controller, GLFW_KEY_A);
        }
        assertEquals(4, controller.snapshot().namePosition());
        assertArrayEquals(new int[] { 1, 1, 1, 1, 1 }, controller.snapshot().nameBytes());

        press(controller, GLFW_KEY_B);
        assertEquals(3, controller.snapshot().namePosition());
        FileMenuAction action = press(controller, GLFW_KEY_ENTER);

        assertEquals(FileMenuAction.Type.REGISTER_FILE, action.type());
        assertEquals(0, action.selectedSlot());
        assertArrayEquals(new int[] { 1, 1, 1, 1, 1 }, action.nameBytes());
        assertEquals(FileMenuController.Mode.SELECT, controller.mode());

        SaveRamImage save = SaveRamImage.empty();
        save.createNewGame(action.selectedSlot(), action.nameBytes());
        FileMenuController reloaded = newController(save.saveFilesMask(), save.savedNames());
        assertEquals(FileMenuAction.Type.LOAD_GAME, press(reloaded, GLFW_KEY_A).type());
    }

    @Test
    void submittingAnUneditedNameKeepsTheSlotEmptyEvenWhenInsertIsPressed() {
        FileMenuController controller = newController(0, new int[3][5]);
        press(controller, GLFW_KEY_A);
        InputState input = new InputState();
        input.onKeyEvent(GLFW_KEY_A, GLFW_PRESS);
        input.onKeyEvent(GLFW_KEY_ENTER, GLFW_PRESS);

        FileMenuAction action = controller.tick(input, INPUT);
        assertEquals(FileMenuAction.Type.REGISTER_FILE, action.type());
        assertArrayEquals(new int[5], action.nameBytes());
        assertEquals(FileMenuController.Mode.SELECT, controller.mode());

        SaveRamImage save = SaveRamImage.empty();
        save.createNewGame(action.selectedSlot(), action.nameBytes());
        assertEquals(0, save.saveFilesMask());
        FileMenuController reloaded = newController(save.saveFilesMask(), save.savedNames());
        assertEquals(FileMenuAction.Type.NONE, press(reloaded, GLFW_KEY_A).type());
        assertEquals(FileMenuController.Mode.CREATE, reloaded.mode());
    }

    @Test
    void existingNamesUseTheRomCodepointTileMapAndSelectionCursorUsesRomOamData() {
        int[][] names = {
            { 1, 2, 0, 0, 0 },
            { 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0 }
        };
        FileMenuController controller = newController(1, names);

        int[] tilemap = controller.snapshot().tilemap();
        assertEquals(0xA0, tilemap[0xC5]);
        assertEquals(0xA1, tilemap[0xC6]);
        assertEquals(0x7E, tilemap[0xC7]);
        assertEquals(0x3B - 16, controller.snapshot().sprites().get(0).y());
        assertEquals(0x10, controller.snapshot().sprites().get(0).x());
        assertEquals(0x02, controller.snapshot().sprites().get(0).tileIndex());
        assertTrue(controller.snapshot().sprites().get(0).flipX());
    }

    @Test
    void snapshotsDoNotExposeMutableBackgroundArrays() {
        FileMenuController controller = newController(0, new int[3][5]);
        FileMenuFrameSnapshot first = controller.snapshot();
        int[] map = first.tilemap();
        int[] name = first.nameBytes();

        map[0] = 0;
        name[0] = 0xFF;

        assertEquals(0x55, controller.snapshot().tilemap()[0]);
        assertEquals(0, controller.snapshot().nameBytes()[0]);
        assertNotSame(map, first.tilemap());
        assertNotSame(name, first.nameBytes());
    }

    @Test
    void occupiedSlotsShowMaxHeartsAndThreeDeathDigits() {
        FileMenuController controller = new FileMenuController(
            new FileMenuRomData(syntheticRom()),
            id -> new BackgroundScene(backgroundMap(), new int[32 * 32],
                new int[][] {{0, 0, 0, 0}}, new int[][] {{0, 0, 0, 0}}),
            0b101, new int[3][5], new FileMenuController.SlotStats[] {
                new FileMenuController.SlotStats(4, 8, 123),
                new FileMenuController.SlotStats(0, 3, 999),
                new FileMenuController.SlotStats(0, 2, 5)
            }, GameplaySoundSink.none());

        int[] map = controller.snapshot().tilemap();
        assertEquals(0xAE, map[0xD1]);
        assertEquals(0xAE, map[0xEB]);
        assertEquals(0, map[0xEC]);
        assertArrayEquals(new int[] {0xB1, 0xB2, 0xB3}, Arrays.copyOfRange(map, 0xE7, 0xEA));
        assertEquals(0, map[0x12B]);
        assertEquals(0, map[0x147]);
        assertEquals(0xAE, map[0x18C]);
        assertArrayEquals(new int[] {0xB0, 0xB0, 0xB5}, Arrays.copyOfRange(map, 0x1A7, 0x1AA));
    }

    private static FileMenuController newController(int saveMask, int[][] names) {
        return newController(saveMask, names, GameplaySoundSink.none());
    }

    private static FileMenuController newController(int saveMask, int[][] names, GameplaySoundSink soundSink) {
        return new FileMenuController(
            new FileMenuRomData(syntheticRom()),
            sceneId -> new BackgroundScene(backgroundMap(), new int[32 * 32],
                new int[][] { { 0, 0, 0, 0 } }, new int[][] { { 0, 0, 0, 0 } }),
            saveMask,
            names,
            soundSink);
    }

    private static void enterCopySourceSelection(FileMenuController controller) {
        press(controller, GLFW_KEY_UP);
        press(controller, GLFW_KEY_RIGHT);
        press(controller, GLFW_KEY_A);
        assertEquals(FileMenuController.Mode.COPY_SOURCE, controller.mode());
    }

    private static int[] backgroundMap() {
        int[] map = new int[32 * 32];
        map[0] = 0x55;
        return map;
    }

    private static FileMenuAction press(FileMenuController controller, int key) {
        InputState input = new InputState();
        input.onKeyEvent(key, GLFW_PRESS);
        FileMenuAction action = controller.tick(input, INPUT);
        input.tickEdges();
        input.onKeyEvent(key, GLFW_RELEASE);
        input.tickEdges();
        return action;
    }

    private static byte[] syntheticRom() {
        byte[] rom = new byte[RomBank.romOffset(0x1C, 0x4641) + 0x100];
        writeBytes(rom, 0x01, 0x48E4, 0x3B, 0x53, 0x6B, 0x83);
        writeBytes(rom, 0x01, 0x4BB5, sequence(0x40, 1));
        writeBytes(rom, 0x01, 0x4B30, sequence(0x40, 0x20));
        writeBytes(rom, 0x01, 0x4B70, sequence(0x40, 0x60));
        writeBytes(rom, 0x01, 0x4BB0, 0x4C, 0x54, 0x5C, 0x64, 0x6C);
        writeBytes(rom, 0x1C, 0x4641, sequence(0x100, 0xA0));
        return rom;
    }

    private static int[] sequence(int length, int start) {
        int[] bytes = new int[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (start + i) & 0xFF;
        }
        return bytes;
    }

    private static void writeBytes(byte[] rom, int bank, int address, int... values) {
        int offset = RomBank.romOffset(bank, address);
        for (int i = 0; i < values.length; i++) {
            rom[offset + i] = (byte) values[i];
        }
    }
}
