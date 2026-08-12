package linksawakening.entity;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

final class MarinWakeUpLinkStateTest {
    @Test
    void leavingBedAppliesMarinsSourceLinkMotion() {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());

        link.showMarinWakeUpBed(4);
        assertTrue(link.isMarinWakeUpBedVisible());
        assertEquals(4, link.marinWakeUpBedVariant());
        link.leaveMarinWakeUpBed();

        assertFalse(link.isMarinWakeUpBedVisible());
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
        Link link = new Link(input, config, tables, collision, null,
            new PlayerState(), new ItemRegistry());
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
        link.update();
        assertEquals(Link.DIRECTION_LEFT, link.direction());
        assertEquals(0xF0, link.romSpeedX());
    }

    private static byte[] loadRom() throws IOException {
        try (InputStream input = MarinWakeUpLinkStateTest.class
            .getResourceAsStream("/rom/azle.gbc")) {
            if (input == null) throw new IOException("Missing ROM resource /rom/azle.gbc");
            return input.readAllBytes();
        }
    }
}
