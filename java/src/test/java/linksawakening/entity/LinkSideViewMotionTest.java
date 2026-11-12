package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

import java.util.Arrays;
import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class LinkSideViewMotionTest {
    private final InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
    private final InputState input = new InputState();
    private final PlayerState player = new PlayerState();

    private Link room(int[] objects, int x, int y) throws Exception {
        byte[] rom;
        try (var stream = linksawakening.testing.TestRom.open()) {
            rom = stream.readAllBytes();
        }
        var tables = RomTables.loadFromRom(rom);
        var collision = new OverworldCollision(tables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        collision.setRoom(objects);
        var link = new LinkTestBuilder()
                .input(input)
                .config(config)
                .tables(tables)
                .collision(collision)
                .sprites(null)
                .player(player)
                .items(new ItemRegistry())
                .build();
        link.position().setPixelPosition(x - 8, y - 16);
        link.sideView().setSideScrolling(true);
        return link;
    }

    private int[] floor() {
        int[] objects = new int[0xA0];
        Arrays.fill(objects, 0xFF);
        for (int y = 0; y < 8; y++) for (int x = 0; x < 10; x++) objects[0x11 + y * 16 + x] = y == 7 ? 0x60 : 0x0D;
        return objects;
    }

    @Test
    void upCannotLevitateAndGravityLandsOnTheCenterFloorProbe() throws Exception {
        Link link = room(floor(), 80, 48);
        input.onKeyEvent(config.upKey(), GLFW_PRESS);
        for (int i = 0; i < 80; i++) link.motion().update();
        assertEquals(112, link.position().romEntityY());
        assertEquals(0, link.animation().romEntityZ());
        assertFalse(link.airborne().isAirborne());
    }

    @Test
    void ladderClimbsOnlyWhileOnItsObjectAndWalkingOffItFalls() throws Exception {
        int[] objects = floor();
        for (int y = 0; y < 7; y++) objects[0x11 + y * 16 + 4] = 0x63;
        Link link = room(objects, 72, 80);
        input.onKeyEvent(config.upKey(), GLFW_PRESS);
        for (int i = 0; i < 16; i++) link.motion().update();
        assertEquals(64, link.position().romEntityY());
        input.onKeyEvent(config.upKey(), GLFW_RELEASE);
        for (int i = 0; i < 12; i++) link.motion().update();
        assertEquals(64, link.position().romEntityY());
        input.onKeyEvent(config.rightKey(), GLFW_PRESS);
        for (int i = 0; i < 32; i++) link.motion().update();
        input.onKeyEvent(config.rightKey(), GLFW_RELEASE);
        for (int i = 0; i < 64; i++) link.motion().update();
        assertEquals(112, link.position().romEntityY());
        assertFalse(link.airborne().isAirborne());
    }

    @Test
    void featherAppliesItsExtraStepAfterGroundMotionChoosesHorizontalSpeed() throws Exception {
        Link link = room(floor(), 80, 112);
        link.motion().update();
        link.motion().update();
        input.onKeyEvent(config.rightKey(), GLFW_PRESS);
        input.onKeyEvent(config.bKey(), GLFW_PRESS);
        player.inventory().setItemB(InventoryItem.ROCS_FEATHER.romValue());
        link.airborne().useRocsFeather();
        assertEquals(80, link.position().romEntityX(), "Item dispatch precedes the movement phase in Main");
        link.motion().update();
        assertEquals(82, link.position().romEntityX(), "Normal movement plus UseRocsFeather's extra step");
        assertEquals(110, link.position().romEntityY());
        assertEquals(0xE8, link.position().romSpeedY());
        link.motion().update();
        assertEquals(83, link.position().romEntityX());
        assertEquals(0xE9, link.position().romSpeedY(), "Held Feather adds one gravity unit while rising");
    }

    @Test
    void featherMovesScreenYAndReturnsToTheFloor() throws Exception {
        player.inventory().setItemA(InventoryItem.ROCS_FEATHER.romValue());
        Link link = room(floor(), 80, 112);
        link.motion().update();
        link.motion().update();
        int startY = link.position().romEntityY();
        link.airborne().useRocsFeather();
        link.motion().update();
        assertTrue(link.position().romEntityY() < startY);
        assertEquals(0, link.animation().romEntityZ());
        input.onKeyEvent(config.aKey(), GLFW_PRESS);
        int minimum = link.position().romEntityY();
        for (int i = 0; i < 20; i++) {
            link.motion().update();
            minimum = Math.min(minimum, link.position().romEntityY());
        }
        assertTrue(minimum < 96);
        input.onKeyEvent(config.aKey(), GLFW_RELEASE);
        for (int i = 0; i < 64; i++) link.motion().update();
        assertEquals(112, link.position().romEntityY());
        assertFalse(link.airborne().isAirborne());
    }
}
