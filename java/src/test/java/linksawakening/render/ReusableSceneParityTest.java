package linksawakening.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class ReusableSceneParityTest {
    @Test
    void ownedAndBorrowedScenesHaveIdenticalPixelsAcrossChangingInputs() {
        var fixture = new SceneConstructionFixture();
        var liveBuilder = new GameFrameSceneBuilder();
        var ownedBuilder = new GameFrameSceneBuilder();
        fixture.configure(liveBuilder);
        fixture.configure(ownedBuilder);
        var state = new ReusableGameFrameState().copyFrom(fixture.state);
        byte[] expected = new byte[fixture.output.length];
        FrameScene reused = null;
        for (int frame = 0; frame < 96; frame++) {
            state.withFrameCounter(frame);
            state.link().position().setPixelPosition(40 + (frame & 7), 40);
            state.roomInputs.tiles[0] = frame & 3;
            state.roomInputs.attributes[1] = (frame & 7) | ((frame & 3) << 5);
            if (frame == 32) state.withDialogController(null);
            ownedBuilder.build(state.snapshot()).drawTo(expected, fixture.gpu);
            FrameScene scene = liveBuilder.build(state);
            if (reused != null) assertSame(reused, scene);
            reused = scene;
            scene.drawTo(fixture.output, fixture.gpu);
            assertArrayEquals(expected, fixture.output, "frame " + frame);
        }
    }

    @Test
    void resetRestoresAllEmptyInputs() {
        var fixture = new SceneConstructionFixture();
        var state = new ReusableGameFrameState().copyFrom(fixture.state);
        assertSame(state, state.reset());
        assertEquals(GameFrameState.empty(), state.snapshot());
        fixture.refill(state, 0x107);
        assertEquals(7, state.frameCounter());
        assertNotNull(state.snapshot().room());
    }

    @Test
    void clearingOrRebindingBorrowedRoomInputsCannotLeaveStalePlanes() {
        var fixture = new SceneConstructionFixture();
        var state = new ReusableGameFrameState().copyFrom(fixture.state);
        int[] tiles = state.roomInputs.tiles, attrs = state.roomInputs.attributes;
        int[][] palettes = state.roomInputs.palettes;
        state.withRoomData(tiles, attrs, palettes, state.roomInputs.entities, null, null, 0);
        state.withRoom(null, fixture.state.scrollController(), fixture.state.transitionController());
        assertNull(state.snapshot().room());
        state.withRoom(fixture.state.room(), fixture.state.scrollController(), fixture.state.transitionController());
        assertNotNull(state.snapshot().room());
        var snapshot = state.snapshot();
        state.roomInputs.tiles[0] = 3;
        assertNotEquals(3, snapshot.room().tileIds()[0]);
    }
}
