package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import linksawakening.equipment.EquipmentItem;
import linksawakening.equipment.Hookshot;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Ocarina;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.Direction;
import linksawakening.state.PlayerState;
import linksawakening.state.PlayerStateTestBuilder;

@linksawakening.testing.RequiresRom
abstract class LinkFixture {

    protected static final int ROOM_OBJECTS_BASE = 0x11;
    protected static final int ROOM_OBJECT_ROW_STRIDE = 0x10;
    protected static final int OBJECT_TALL_GRASS = 0x0A;
    protected static final int OBJECT_TREE_TOP_LEFT = 0x25;
    protected static final int OBJECT_DEEP_WATER = 0x0E;
    protected static final int OBJECT_PIT = 0xE8;
    protected static final int OBJECT_INDOOR_PIT_WARP = 0x1C;

    protected static byte[] loadRom() throws IOException {
        try (InputStream in = linksawakening.testing.TestRom.open()) {
            if (in == null) {
                throw new IOException("Missing ROM resource /rom/azle.gbc");
            }
            return in.readAllBytes();
        }
    }

    protected static int pixelColor(byte[] buffer, int offset) {
        return ((buffer[offset] & 0xFF) << 16) | ((buffer[offset + 1] & 0xFF) << 8) | (buffer[offset + 2] & 0xFF);
    }

    protected static Link linkInRoom(InputConfig inputConfig, RomTables romTables, int[] roomObjectsArea) {
        return linkInRoom(
                inputConfig,
                romTables,
                roomObjectsArea,
                new PlayerStateTestBuilder().build(),
                RomTables.PHYSICS_TABLE_OVERWORLD);
    }

    protected static Link linkWithEmptyRoom(InputState inputState) throws IOException {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        RomTables romTables = RomTables.loadFromRom(loadRom());
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(emptyRoomObjectsArea());
        return new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerStateTestBuilder().build())
                .items(new ItemRegistry())
                .build();
    }

    protected static Link pitLink(InputConfig inputConfig) throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerStateTestBuilder().build())
                .items(new ItemRegistry())
                .build();
        link.position().setPixelPosition(0x20, 0x20);
        return link;
    }

    protected static Link linkInRoom(
            InputConfig inputConfig,
            RomTables romTables,
            int[] roomObjectsArea,
            PlayerState playerState,
            int physicsTableIndex) {
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setPhysicsTable(physicsTableIndex);
        collision.setRoom(roomObjectsArea);
        return new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(playerState)
                .items(new ItemRegistry())
                .build();
    }

    protected static void assertJumpAnimation(int direction, int frame0, int frame1, int frame2) throws Exception {
        Link link = linkFacing(direction);
        link.airborne().useRocsFeather();

        assertEquals(frame0, resolvedAnimationState(link));
        int[] poses = {frame0, frame1, frame2};
        for (int frame = 0; frame < 24; frame++) {
            link.motion().update();
            assertTrue(link.airborne().isAirborne(), "frame " + frame);
            assertEquals(poses[frame / 8], resolvedAnimationState(link), "frame " + frame);
        }
        link.motion().update();
        assertTrue(link.airborne().isAirborne());
        assertEquals(new int[] {0x01, 0x05, 0x07, 0x0B}[direction], resolvedAnimationState(link));
    }

    protected static Link linkFacing(int direction) throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerStateTestBuilder().build();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.inventory().itemA(), new BlockingItem());
        switch (Direction.fromJavaOrUnknown(direction)) {
            case RIGHT:
                inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
                break;
            case LEFT:
                inputState.onKeyEvent(inputConfig.leftKey(), GLFW_PRESS);
                break;
            case UP:
                inputState.onKeyEvent(inputConfig.upKey(), GLFW_PRESS);
                break;
            case DOWN:
                inputState.onKeyEvent(inputConfig.downKey(), GLFW_PRESS);
                break;
            default:
                throw new IllegalArgumentException("direction " + direction);
        }
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(RomTables.loadFromRom(loadRom()))
                .collision(null)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();
        link.motion().update();
        return link;
    }

    protected static int resolvedAnimationState(Link link) throws Exception {
        return link.animation().romAnimationState();
    }

    protected static int javaDirectionForRomDirection(int romDirection) {
        return switch (romDirection) {
            case 0 -> Direction.RIGHT.javaIndex();
            case 1 -> Direction.LEFT.javaIndex();
            case 2 -> Direction.UP.javaIndex();
            case 3 -> Direction.DOWN.javaIndex();
            default -> throw new AssertionError(romDirection);
        };
    }

    protected static void runUntilPitFallStarts(Link link) {
        int guard = 0;
        while (!link.airborne().isFallingIntoPit() && guard++ < 128) {
            link.motion().update();
        }
        assertTrue(link.airborne().isFallingIntoPit());
    }

    protected static void runUntilPitFallFinishes(Link link) {
        int guard = 0;
        while (link.airborne().isFallingIntoPit() && guard++ < 128) {
            link.motion().update();
        }
        assertFalse(link.airborne().isFallingIntoPit());
    }

    protected static void pressDirection(InputState inputState, InputConfig inputConfig, int direction) {
        switch (Direction.fromJavaOrUnknown(direction)) {
            case DOWN:
                inputState.onKeyEvent(inputConfig.downKey(), GLFW_PRESS);
                break;
            case UP:
                inputState.onKeyEvent(inputConfig.upKey(), GLFW_PRESS);
                break;
            case LEFT:
                inputState.onKeyEvent(inputConfig.leftKey(), GLFW_PRESS);
                break;
            case RIGHT:
                inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
                break;
            default:
                throw new IllegalArgumentException("direction " + direction);
        }
    }

    protected static void placeAtRoomEntry(Link link, int pixelX, int pixelY) {
        link.position().setRoomEntryPixelPosition(pixelX, pixelY);
    }

    protected static int[] emptyRoomObjectsArea() {
        int[] roomObjectsArea = new int[0x100];
        java.util.Arrays.fill(roomObjectsArea, 0xFF);
        return roomObjectsArea;
    }

    protected record CollisionAttempt(
            int direction,
            int sourceBit,
            int startX,
            int startY,
            int blockRow,
            int blockColumn,
            int probeX,
            int probeY) {}

    protected static final class BlockingItem implements EquipmentItem {
        @Override
        public boolean blocksMotion() {
            return true;
        }
    }

    protected static final class RecordingRenderItem implements EquipmentItem {
        protected int renderCalls;

        @Override
        public void render(
                byte[] displayBuffer, int linkPixelX, int linkPixelY, int direction, int offsetX, int offsetY) {
            renderCalls++;
        }
    }

    protected static final class HookshotTarget implements Hookshot.LaunchTarget {
        protected boolean active;

        @Override
        public boolean fireHookshot() {
            return true;
        }

        @Override
        public boolean hookshotActive() {
            return active;
        }
    }

    protected static final class AnimationOverrideItem implements EquipmentItem {
        protected final int animationState;

        protected AnimationOverrideItem(int animationState) {
            this.animationState = animationState;
        }

        @Override
        public int overrideAnimationState(int direction, int walkFrame) {
            return animationState;
        }
    }

    protected static final class OcarinaAnimationTarget implements Ocarina.PlaybackTarget {
        protected boolean playing;
        protected int animationState = -1;

        @Override
        public boolean startOcarina(int countdown, int songFlags, int selectedSong) {
            playing = true;
            return true;
        }

        @Override
        public boolean ocarinaPlaying() {
            return playing;
        }

        @Override
        public int ocarinaAnimationState() {
            return playing ? animationState : -1;
        }
    }

    protected static final class RecordingGameplaySoundSink implements GameplaySoundSink {
        protected final List<GameplaySoundEvent> events = new ArrayList<>();

        @Override
        public void play(GameplaySoundEvent event) {
            events.add(event);
        }
    }
}
