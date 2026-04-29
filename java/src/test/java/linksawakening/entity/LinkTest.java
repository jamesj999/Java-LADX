package linksawakening.entity;

import java.io.IOException;
import java.io.InputStream;
import linksawakening.equipment.EquippedItem;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

final class LinkTest {

    private static final int ROOM_OBJECTS_BASE = 0x11;
    private static final int ROOM_OBJECT_ROW_STRIDE = 0x10;
    private static final int OBJECT_TALL_GRASS = 0x0A;

    @Test
    void blockedMotionStillRefreshesFacingFromHeldSingleDirection() {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        Link link = new Link(inputState, inputConfig, null, null, null, playerState, itemRegistry);

        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        link.update();

        assertEquals(Link.DIRECTION_RIGHT, link.direction());
    }

    @Test
    void blockedMotionKeepsPreviousFacingForDiagonalInput() {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        Link link = new Link(inputState, inputConfig, null, null, null, playerState, itemRegistry);

        inputState.onKeyEvent(inputConfig.leftKey(), GLFW_PRESS);
        link.update();
        assertEquals(Link.DIRECTION_LEFT, link.direction());

        inputState.onKeyEvent(inputConfig.upKey(), GLFW_PRESS);
        link.update();

        assertEquals(Link.DIRECTION_LEFT, link.direction());
    }

    @Test
    void holdingChargedSwordDoesNotBlockMovement() {
        Sword sword = new Sword(null, null);

        sword.onPress();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }

        assertEquals(false, sword.blocksMotion());
    }

    @Test
    void holdingSwordKeepsOriginalFacingDirection() throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        Sword sword = new Sword(null, null);
        itemRegistry.register(playerState.itemA(), sword);
        RomTables romTables = RomTables.loadFromRom(loadRom());
        Link link = new Link(inputState, inputConfig, romTables,
            new OverworldCollision(romTables), null, playerState, itemRegistry);

        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        link.update();
        assertEquals(Link.DIRECTION_RIGHT, link.direction());

        sword.onPress();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }

        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_RELEASE);
        inputState.onKeyEvent(inputConfig.upKey(), GLFW_PRESS);
        link.update();

        assertEquals(Link.DIRECTION_RIGHT, link.direction());
    }

    @Test
    void tallGrassSlowsLinkMovement() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);

        Link normal = linkInRoom(inputConfig, romTables, emptyRoomObjectsArea());
        int[] grassyRoomObjects = emptyRoomObjectsArea();
        grassyRoomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_TALL_GRASS;
        Link slowed = linkInRoom(inputConfig, romTables, grassyRoomObjects);

        normal.setPixelPosition(0x20, 0x20);
        slowed.setPixelPosition(0x20, 0x20);

        for (int i = 0; i < 8; i++) {
            normal.update();
            slowed.update();
        }

        assertEquals(0x28, normal.pixelX());
        assertEquals(0x26, slowed.pixelX());
    }

    private static byte[] loadRom() throws IOException {
        try (InputStream in = LinkTest.class.getResourceAsStream("/rom/azle.gbc")) {
            if (in == null) {
                throw new IOException("Missing ROM resource /rom/azle.gbc");
            }
            return in.readAllBytes();
        }
    }

    private static Link linkInRoom(InputConfig inputConfig, RomTables romTables, int[] roomObjectsArea) {
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjectsArea);
        return new Link(inputState, inputConfig, romTables, collision, null, new PlayerState(), new ItemRegistry());
    }

    private static int[] emptyRoomObjectsArea() {
        int[] roomObjectsArea = new int[0x100];
        java.util.Arrays.fill(roomObjectsArea, 0xFF);
        return roomObjectsArea;
    }

    private static final class BlockingItem implements EquippedItem {
        @Override
        public boolean blocksMotion() {
            return true;
        }
    }
}
