package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.io.IOException;
import java.util.List;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class LinkJumpBehaviorTest extends LinkFixture {

    @Test
    void airborneJumpAnimationStartsByDirectionAndAdvancesEveryEightFrames() throws Exception {
        assertJumpAnimation(Link.DIRECTION_RIGHT, 0x61, 0x62, 0x63);
        assertJumpAnimation(Link.DIRECTION_LEFT, 0x5E, 0x5F, 0x60);
        assertJumpAnimation(Link.DIRECTION_UP, 0x67, 0x68, 0x69);
        assertJumpAnimation(Link.DIRECTION_DOWN, 0x64, 0x65, 0x66);
    }

    @Test
    void acceptedRocsFeatherJumpPlaysJumpJingleOnce() {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        RecordingGameplaySoundSink soundSink = new RecordingGameplaySoundSink();
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(inputConfig)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .sounds(soundSink)
                .build();

        link.useRocsFeather();
        link.useRocsFeather();

        assertEquals(List.of(GameplaySoundEvent.ROC_FEATHER_JUMP), soundSink.events);
    }

    @Test
    void likeLikeCanHideLinkWithoutReplacingHisPositionOrAirborneMotion() throws IOException {
        Link link = linkWithEmptyRoom(new InputState());
        link.setPixelPosition(0x20, 0x20);
        link.useRocsFeather();
        link.update();
        int beforeZ = link.romEntityZ();
        assertTrue(beforeZ > 0);

        link.applyLikeLikeCapture(0x40, 0x50, false);
        assertTrue(link.isLikeLikeCaptured());
        assertEquals(0x20, link.pixelX());
        assertEquals(0x20, link.pixelY());
        assertEquals(beforeZ, link.romEntityZ());
        // Hidden Link returns before touching the absent sprite sheet/buffer.
        link.render(null, 0, 0);
        link.update();
        assertTrue(link.romEntityZ() > beforeZ);

        link.applyLikeLikeCapture(0x40, 0x50, true);
        assertEquals(0x38, link.pixelX());
        assertEquals(0x40, link.pixelY());
        assertEquals(0, link.romEntityZ());
        link.releaseLikeLikeCapture();
        assertFalse(link.isLikeLikeCaptured());
    }

    @Test
    void blockedMotionStillTicksJumpGravity() throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(RomTables.loadFromRom(loadRom()))
                .collision(null)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();

        link.useRocsFeather();
        link.update();

        assertTrue(link.isAirborne());
        assertEquals(0x1E, link.zVelocity());
    }

    @Test
    void airborneSteeringRetainsMomentumAndChangesSpeedAfterMovement() throws Exception {
        InputState input = new InputState();
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = linkWithEmptyRoom(input);
        link.setPixelPosition(40, 40);
        input.onKeyEvent(config.rightKey(), GLFW_PRESS);
        link.useRocsFeather();
        link.update();
        assertEquals(41, link.pixelX());
        assertEquals(0x10, link.romSpeedX());
        input.onKeyEvent(config.rightKey(), GLFW_RELEASE);
        link.update();
        assertEquals(42, link.pixelX(), "Releasing input must not stop airborne momentum");
        assertEquals(0x10, link.romSpeedX());
        input.onKeyEvent(config.leftKey(), GLFW_PRESS);
        input.onKeyEvent(config.upKey(), GLFW_PRESS);
        link.update();
        assertEquals(43, link.pixelX(), "XY movement precedes the steering adjustment");
        assertEquals(40, link.pixelY());
        assertEquals(0x0F, link.romSpeedX());
        assertEquals(0xFF, link.romSpeedY());
        assertEquals(
                Link.DIRECTION_RIGHT, link.direction(), "Airborne steering must preserve the takeoff facing direction");
        link.update();
        assertEquals(0x0E, link.romSpeedX());
        assertEquals(0xFE, link.romSpeedY());
        input.onKeyEvent(config.leftKey(), GLFW_RELEASE);
        input.onKeyEvent(config.upKey(), GLFW_RELEASE);
        link.update();
        assertEquals(0x0E, link.romSpeedX());
        assertEquals(0xFE, link.romSpeedY());
    }

    /**
     * Bank-$02 ApplyCollisionWithOceanOrSwitchBlock does not consult
     * wIsLinkInTheAir. A Roc's Feather jump therefore remains blocked by a
     * mismatched raised switch block; only pits are explicitly jump-clearable.
     */
    @Test
    void rocsFeatherCannotCrossAMismatchedRaisedSwitchBlock() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 1] = 0xDC;
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        collision.setRoom(roomObjects);
        collision.setSwitchBlocksState(0x00);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.setPixelPosition(0x07, 0x20);

        link.useRocsFeather();
        assertTrue(link.isAirborne());
        int jumpFrames = 0;
        while (link.isAirborne() && jumpFrames++ < 0x50) {
            link.update();
            assertEquals(0x07, link.pixelX(), "airborne Link must remain on the original side of the raised block");
        }

        assertTrue(jumpFrames < 0x50, "Roc's Feather jump did not land");
        assertFalse(link.isAirborne());
        assertEquals(0x07, link.pixelX());
    }
}
