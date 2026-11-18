package linksawakening.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

import java.io.InputStream;
import java.util.Arrays;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.HardwareFrame;
import linksawakening.rom.Rom;
import linksawakening.save.SaveRamImage;
import linksawakening.save.SaveRamLayout;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class InventoryControllerTest {

    @Test
    void ownsMenuToggleTickAndCursorInput() throws Exception {
        InputState input = new InputState();
        InputConfig inputConfig = new InputConfig(
                GLFW_KEY_ENTER, GLFW_KEY_UP, GLFW_KEY_DOWN, GLFW_KEY_LEFT, GLFW_KEY_RIGHT, GLFW_KEY_A, GLFW_KEY_B);
        PlayerState playerState = new PlayerState();
        InventoryMenu menu = new InventoryMenu(InventoryTilemapLoader.loadFromRom(Rom.of(loadRom())), playerState);
        InventoryController controller = new InventoryController(input, inputConfig, menu);

        input.onKeyEvent(GLFW_KEY_ENTER, GLFW_PRESS);
        controller.dispatchToggleInput();
        for (int i = 0; i < 60; i++) {
            controller.tick();
        }

        assertTrue(controller.shouldBlockOverworldInput());
        assertTrue(menu.isFullyOpen());

        input.tickEdges();
        input.onKeyEvent(GLFW_KEY_DOWN, GLFW_PRESS);
        controller.dispatchMenuInput();

        assertEquals(2, menu.cursorSlot());
    }

    @Test
    void selectSlidesStatusAndQuestItemsReachTheIndexedFrame() throws Exception {
        Rom rom = Rom.of(loadRom());
        PlayerState player = new PlayerState();
        InventoryMenu menu =
                new InventoryMenu(InventoryTilemapLoader.loadFromRom(rom), player, GameplaySoundSink.none(), rom);
        InputState input = new InputState();
        InputConfig keys = new InputConfig(
                GLFW_KEY_ENTER, GLFW_KEY_UP, GLFW_KEY_DOWN, GLFW_KEY_LEFT, GLFW_KEY_RIGHT, GLFW_KEY_A, GLFW_KEY_B);
        InventoryController controller = new InventoryController(input, keys, menu);
        GPU gpu = new GPU();
        gpu.loadBaseTiles(rom);
        menu.requestToggle();
        for (int i = 0; i < 60; i++) controller.tick();
        byte[] empty = frame(controller, gpu);
        player.progression().setTradeSequenceItem(14);
        player.progression().setMedicineCount(1);
        player.progression().setSeashells(12);
        player.progression().setGoldenLeavesCount(5);
        byte[] populated = frame(controller, gpu);
        int questStart = 24 * Framebuffer.WIDTH * 4;
        int questEnd = 56 * Framebuffer.WIDTH * 4;
        assertFalse(Arrays.equals(empty, questStart, questEnd, populated, questStart, questEnd));
        player.progression().setGoldenLeavesCount(6);
        byte[] slimeKey = frame(controller, gpu);
        assertFalse(Arrays.equals(populated, questStart, questEnd, slimeKey, questStart, questEnd));

        menu.setWorldContext(true, 0, new byte[8]);
        menu.setDungeonContext(new byte[] {1, 1, 1, 1, 3}, new byte[64], 0, 0);
        byte[] dungeon = frame(controller, gpu);
        assertFalse(Arrays.equals(slimeKey, questStart, questEnd, dungeon, questStart, questEnd));

        input.onKeyEvent(keys.selectKey(), GLFW_PRESS);
        controller.dispatchMenuInput();
        for (int i = 0; i < 6; i++) controller.tick();
        assertEquals(0x78, menu.statusPanelY());
        menu.requestToggle();
        menu.moveCursor(0, 1);
        assertTrue(menu.isFullyOpen());
        assertEquals(0, menu.cursorSlot());
        int statusStart = 104 * Framebuffer.WIDTH * 4;
        int statusEnd = 128 * Framebuffer.WIDTH * 4;
        byte[] status = frame(controller, gpu);
        assertFalse(Arrays.equals(dungeon, statusStart, statusEnd, status, statusStart, statusEnd));
        input.onKeyEvent(keys.selectKey(), org.lwjgl.glfw.GLFW.GLFW_RELEASE);
        for (int i = 0; i < 7; i++) controller.tick();
        assertFalse(menu.isStatusPanelActive());
    }

    @Test
    void restoresOnlyTheTwelvePhotoFlagsAndClearsTheCountForNewGame() {
        SaveRamImage image = SaveRamImage.empty();
        image.createNewGame(0, new int[SaveRamLayout.NAME_LENGTH]);
        byte[] bytes = image.bytes();
        int dx3 = SaveRamLayout.slotOffset(0) + SaveRamLayout.dx3Offset();
        bytes[dx3 + 1] = (byte) 0xA5;
        bytes[dx3 + 2] = (byte) 0xF3;
        PlayerState player = new PlayerState();
        player.playerSaveLoader().applySavedGame(SaveRamImage.fromBytes(bytes).readSlot(0));
        assertEquals(6, player.progression().photoCount());
        player.playerInitialization().initializeNewGame(20, 20, 20);
        assertEquals(0, player.progression().photoCount());
    }

    private static byte[] frame(InventoryController controller, GPU gpu) {
        HardwareFrame frame = new HardwareFrame(true);
        byte[] pixels = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        controller.submit(frame, gpu, null);
        frame.compose(pixels);
        return pixels;
    }

    private static byte[] loadRom() throws Exception {
        try (InputStream stream = linksawakening.testing.TestRom.open()) {
            return stream.readAllBytes();
        }
    }
}
