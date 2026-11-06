package linksawakening.entity;

import java.util.Arrays;
import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

final class LinkSideViewMotionTest {
    private final InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
    private final InputState input = new InputState();
    private final PlayerState player = new PlayerState();

    private Link room(int[] objects, int x, int y) throws Exception {
        byte[] rom;
        try (var stream = getClass().getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            rom = stream.readAllBytes();
        }
        var tables = RomTables.loadFromRom(rom);
        var collision = new OverworldCollision(tables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        collision.setRoom(objects);
        var link = new Link(input, config, tables, collision, null, player, new ItemRegistry());
        link.setPixelPosition(x - 8, y - 16);
        link.setSideScrolling(true);
        return link;
    }

    private int[] floor() {
        int[] objects = new int[0xA0];
        Arrays.fill(objects, 0xFF);
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 10; x++) objects[0x11 + y * 16 + x] = y == 7 ? 0x60 : 0x0D;
        return objects;
    }

    @Test
    void upCannotLevitateAndGravityLandsOnTheCenterFloorProbe() throws Exception {
        Link link = room(floor(), 80, 48);
        input.onKeyEvent(config.upKey(), GLFW_PRESS);
        for (int i = 0; i < 80; i++) link.update();
        assertEquals(112, link.romEntityY());
        assertEquals(0, link.romEntityZ());
        assertFalse(link.isAirborne());
    }

    @Test
    void ladderClimbsOnlyWhileOnItsObjectAndWalkingOffItFalls() throws Exception {
        int[] objects = floor();
        for (int y = 0; y < 7; y++) objects[0x11 + y * 16 + 4] = 0x63;
        Link link = room(objects, 72, 80);
        input.onKeyEvent(config.upKey(), GLFW_PRESS);
        for (int i = 0; i < 16; i++) link.update();
        assertEquals(64, link.romEntityY());
        input.onKeyEvent(config.upKey(), GLFW_RELEASE);
        for (int i = 0; i < 12; i++) link.update();
        assertEquals(64, link.romEntityY());
        input.onKeyEvent(config.rightKey(), GLFW_PRESS);
        for (int i = 0; i < 32; i++) link.update();
        input.onKeyEvent(config.rightKey(), GLFW_RELEASE);
        for (int i = 0; i < 64; i++) link.update();
        assertEquals(112, link.romEntityY());
        assertFalse(link.isAirborne());
    }

    @Test
    void featherAppliesItsExtraStepAfterGroundMotionChoosesHorizontalSpeed() throws Exception {
        Link link = room(floor(), 80, 112);
        link.update(); link.update();
        input.onKeyEvent(config.rightKey(), GLFW_PRESS);
        input.onKeyEvent(config.bKey(), GLFW_PRESS);
        player.setItemB(PlayerState.INVENTORY_ROCS_FEATHER);
        link.useRocsFeather();
        assertEquals(80, link.romEntityX(), "Item dispatch precedes the movement phase in Main");
        link.update();
        assertEquals(82, link.romEntityX(), "Normal movement plus UseRocsFeather's extra step");
        assertEquals(110, link.romEntityY());
        assertEquals(0xE8, link.romSpeedY());
        link.update();
        assertEquals(83, link.romEntityX());
        assertEquals(0xE9, link.romSpeedY(), "Held Feather adds one gravity unit while rising");
    }

    @Test
    void featherMovesScreenYAndReturnsToTheFloor() throws Exception {
        player.setItemA(PlayerState.INVENTORY_ROCS_FEATHER);
        Link link = room(floor(), 80, 112);
        link.update(); link.update();
        int startY = link.romEntityY();
        link.useRocsFeather();
        link.update();
        assertTrue(link.romEntityY() < startY);
        assertEquals(0, link.romEntityZ());
        input.onKeyEvent(config.aKey(), GLFW_PRESS);
        int minimum = link.romEntityY();
        for (int i = 0; i < 20; i++) {
            link.update(); minimum = Math.min(minimum, link.romEntityY());
        }
        assertTrue(minimum < 96);
        input.onKeyEvent(config.aKey(), GLFW_RELEASE);
        for (int i = 0; i < 64; i++) link.update();
        assertEquals(112, link.romEntityY());
        assertFalse(link.isAirborne());
    }
}
