package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gpu.Tile;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.render.OamSink;
import linksawakening.rom.Rom;
import linksawakening.rom.RomSymbols;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

/** Scalar expectations from bank2.asm, also checked in a local PyBoy 2.9.0 trace. */
@RequiresRom
final class LinkLedgeParityTest {
    @Test
    void overworldCliffMatchesEveryFrameOfUnstuckingAndItsLandingPose() throws Exception {
        var fixture = fixture(false);
        Link link = fixture.link;
        link.setPixelPosition(32, 40);
        link.update();
        assertEquals(2, link.romMotionState());
        assertFalse(link.isAirborne());
        assertEquals(40, link.pixelY(), "$02:6EDB restores the blocked Y axis");
        for (int frame = 1; frame <= 20; frame++) {
            int previousY = link.pixelY() - link.zPixels();
            link.update();
            assertEquals(32, link.pixelX(), "X frame " + frame);
            assertEquals(61, link.pixelY(), "Y frame " + frame);
            // $496A-$49A8 searches 24 pixels below, then subtracts 3 from Y.
            // $44FA adds signed Z velocity / 16 before subtracting 2.
            int z = Math.max(0, (24 * 16 - frame * (frame - 1)) >> 4);
            assertEquals(z, link.romEntityZ(), "Z frame " + frame);
            assertEquals(frame == 20 ? 0 : (-2 * frame) & 255, link.zVelocity(), "velocity frame " + frame);
            assertEquals(Math.min(16, frame - 1), link.romSpeedY(), "speed frame " + frame);
            assertEquals(frame == 20 ? 0 : 2, link.romMotionState(), "motion frame " + frame);
            assertTrue(link.isAirborne(), "$49B6 writes airborne even on landing");
            int state = frame == 20 ? 0x64 : 0x64 + (frame - 1) / 8;
            assertEquals(state, link.romAnimationState(), "animation frame " + frame);
            assertOam(fixture, state, previousY);
        }
        link.update();
        assertFalse(link.isAirborne());
        assertEquals(62, link.pixelY());
        assertEquals(1, link.romPhysicsModifier());
    }

    @Test
    void tallCliffRetainsTheThirdFlipPoseUntilTheLandingResetsIt() throws Exception {
        var fixture = fixture(false);
        // Extend the same ledge through rows 4 and 5. The upward collision
        // probes in $498D search to row 6, giving initial Z=$38.
        for (int row = 4; row <= 5; row++) fixture.room[0x11 + row * 16 + 2] = 0x43;
        Link link = fixture.link;
        link.setPixelPosition(32, 40);
        link.update();
        for (int frame = 1; frame <= 31; frame++) {
            link.update();
            assertEquals(93, link.pixelY());
            assertEquals(Math.max(0, (56 * 16 - frame * (frame - 1)) >> 4), link.romEntityZ());
            assertEquals(
                    frame == 31 ? 0x64 : 0x64 + Math.min(2, (frame - 1) / 8),
                    link.romAnimationState(),
                    "frame " + frame);
        }
        assertEquals(0, link.romMotionState());
    }

    @Test
    void directionalLedgeMatchesEveryFrameOfVelocityFlipAndLanding() throws Exception {
        var fixture = fixture(true);
        Link link = fixture.link;
        link.setPixelPosition(32, 32);
        for (int frame = 0; frame < 5; frame++) {
            link.update();
            assertFalse(link.isAirborne());
        }
        for (int frame = 1; frame <= 29; frame++) {
            int previousY = link.pixelY() - link.zPixels();
            link.update();
            assertEquals(32, link.pixelX());
            assertEquals(32 + frame - 1, link.pixelY(), "Y frame " + frame);
            assertEquals((frame * (29 - frame)) >> 4, link.romEntityZ(), "Z frame " + frame);
            assertEquals(frame == 29 ? 0 : (28 - 2 * frame) & 255, link.zVelocity());
            assertEquals(16, link.romSpeedY());
            assertEquals(frame < 29, link.isAirborne());
            int state = frame <= 24 ? 0x64 + (frame - 1) / 8 : 1;
            assertEquals(state, link.romAnimationState(), "animation frame " + frame);
            assertOam(fixture, state, previousY);
        }
    }

    @Test
    void allFourFlipSequencesUseTheRomSpriteColumnsAndFlipAttributes() throws Exception {
        var fixture = fixture(true);
        for (int direction = 0; direction < 4; direction++) {
            Link link = fixture.link;
            link.direction = direction;
            link.linkAirborne.airborne = true;
            link.linkAnimator.jumpAnimationCounter = 0;
            for (int frame = 0; frame < 24; frame++) {
                link.linkAirborne.tickJumpAnimation();
                int state = new Rom(fixture.rom)
                        .u8(new linksawakening.rom.RomAddress(0x02, 0x46B5)
                                .plus(Link.romDirectionForJavaDirection(direction) * 4 + frame / 8));
                assertEquals(state, link.romAnimationState());
                assertOam(fixture, state, link.pixelY());
            }
        }
    }

    private static void assertOam(Fixture fixture, int state, int y) {
        record Column(Tile top, Tile bottom, int x, int y, boolean fx, boolean fy) {}
        List<Column> columns = new ArrayList<>();
        fixture.link.submitOam(
                new OamSink() {
                    @Override
                    public void append(
                            Tile tile, int x, int py, boolean fx, boolean fy, int[] palette, boolean behind) {
                        fail("Expected 8x16 entries");
                    }

                    @Override
                    public void append8x16(
                            Tile top,
                            Tile bottom,
                            int x,
                            int py,
                            boolean fx,
                            boolean fy,
                            int[] palette,
                            boolean behind) {
                        assertFalse(behind);
                        columns.add(new Column(top, bottom, x, py, fx, fy));
                    }
                },
                0,
                0);
        assertEquals(2, columns.size());
        Rom rom = new Rom(fixture.rom);
        for (int half = 0; half < 2; half++) {
            int tile = rom.u8(RomSymbols.LinkAnimationStateTable.plus(state * 2 + half));
            int attrs = rom.u8(RomSymbols.data_020_5407.plus(state * 2 + half));
            var actual = columns.get(half);
            // The sink receives the two 8x8 halves in screen order: an 8x16
            // Y flip swaps the halves as well as flipping each tile's pixels.
            assertSame(fixture.link.spriteSheet.tile(tile + ((attrs & 0x40) != 0 ? 1 : 0)), actual.top());
            assertSame(fixture.link.spriteSheet.tile(tile + ((attrs & 0x40) != 0 ? 0 : 1)), actual.bottom());
            assertEquals((attrs & 0x20) != 0, actual.fx());
            assertEquals((attrs & 0x40) != 0, actual.fy());
            assertEquals(fixture.link.pixelX() + half * 8, actual.x());
            assertEquals(y, actual.y(), "DrawLinkSprite uses pre-motion wC145");
        }
    }

    private record Fixture(Link link, byte[] rom, int[] room) {}

    private static Fixture fixture(boolean indoor) throws Exception {
        byte[] rom;
        try (var in = TestRom.open()) {
            rom = in.readAllBytes();
        }
        RomTables tables = RomTables.loadFromRom(rom);
        var collision = new OverworldCollision(tables);
        if (indoor) collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        int[] room = new int[256];
        Arrays.fill(room, 0xFF);
        room[0x11 + 3 * 16 + 2] = indoor ? 0x21 : 0x43;
        collision.setRoom(room);
        var input = new InputState();
        var config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        input.onKeyEvent(config.downKey(), GLFW_PRESS);
        return new Fixture(
                new Link(
                        input,
                        config,
                        tables,
                        collision,
                        LinkSpriteSheet.loadFromRom(rom),
                        new PlayerState(),
                        new ItemRegistry()),
                rom,
                room);
    }
}
