package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class MarinWakeUpLinkStateTest {
    @Test
    void freshIntroLeavesTheSolidBedAtTheRomLandingPosition() throws Exception {
        // Drive the real story/dialog path, including the room's solid bed.
        // MarinLinkAwakeInBedHandler $05:52F2-$530E launches from ($38,$34)
        // with X speed $0C, Z=1, velocity $12, and wC10A=1. The ROM's
        // opening-route checkpoint after right 8 / idle 55 is ($46,$34).
        loadRom();
        var game = new linksawakening.app.HeadlessGameDriver();
        game.initializeHeadlessNewGame();
        for (int i = 0; i < 620; i++) game.headlessFrame();
        assertTrue(game.headlessState().dialog());
        for (int page = 0; page < 6; page++) {
            game.headlessButton("a", true);
            for (int i = 0; i < 8; i++) game.headlessFrame();
            game.headlessButton("a", false);
            for (int i = 0; i < (page == 5 ? 32 : 122); i++) game.headlessFrame();
        }
        assertFalse(game.headlessState().dialog());
        game.headlessButton("right", true);
        for (int i = 0; i < 8; i++) game.headlessFrame();
        game.headlessButton("right", false);
        for (int i = 0; i < 55; i++) game.headlessFrame();
        assertEquals(0x46, game.headlessState().x());
        assertEquals(0x34, game.headlessState().y());
        assertEquals(0, game.headlessState().z());
        assertEquals(0xA3, game.headlessState().room());
    }

    @Test
    void leavingBedAppliesMarinsSourceLinkMotion() {
        Link link = new Link(
                new InputState(),
                new InputConfig(1, 2, 3, 4, 5, 6, 7),
                null,
                null,
                null,
                new PlayerState(),
                new ItemRegistry());

        link.showMarinWakeUpBed(4);
        assertTrue(link.isMarinWakeUpBedVisible());
        assertEquals(4, link.marinWakeUpBedVariant());
        link.leaveMarinWakeUpBed();

        // Marin has already emitted bed OAM on the launch frame.
        assertTrue(link.isMarinWakeUpBedVisible());
        assertEquals(3, link.marinWakeUpBedVariant());
        assertEquals(Link.DIRECTION_RIGHT, link.direction());
        assertTrue(link.isAirborne());
        assertEquals(0x12, link.zVelocity());
        assertEquals(0x0C, link.romSpeedX());
        assertEquals(0, link.romSpeedY());
        assertEquals(1, link.romEntityZ());
    }

    @Test
    void bedJumpLocksRightwardSpeedUntilLandingThenReturnsJoypadControl() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        OverworldCollision collision = new OverworldCollision(tables);
        int[] room = new int[0x100];
        Arrays.fill(room, 0xFF);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        collision.setRoom(room);
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState input = new InputState();
        input.onKeyEvent(config.leftKey(), GLFW_PRESS);
        Link link = new Link(input, config, tables, collision, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x40, 0x40);
        link.leaveMarinWakeUpBed();

        int airborneFrames = 0;
        do {
            link.update();
            assertEquals(0x0C, link.romSpeedX());
            assertEquals(0, link.romSpeedY());
            assertEquals(Link.DIRECTION_RIGHT, link.direction());
            airborneFrames++;
        } while (link.isAirborne() && airborneFrames < 100);

        assertTrue(airborneFrames > 1);
        assertFalse(link.isAirborne());
        assertFalse(link.isMarinWakeUpBedVisible());
        assertEquals(0x40 + 14, link.pixelX(), "No horizontal movement on the landing frame");
        link.update();
        assertEquals(Link.DIRECTION_LEFT, link.direction());
        assertEquals(0xF0, link.romSpeedX());
    }

    private static byte[] loadRom() throws IOException {
        try (InputStream input = linksawakening.testing.TestRom.open()) {
            if (input == null) throw new IOException("Missing ROM resource /rom/azle.gbc");
            return input.readAllBytes();
        }
    }
}
