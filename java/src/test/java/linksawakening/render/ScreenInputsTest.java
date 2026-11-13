package linksawakening.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Section presence is an explicit binding contract and is cleared when screens change. */
final class ScreenInputsTest {
    @Test
    void resetClearsPresentationSectionsWithoutReplacingThem() {
        var fixture = new SceneConstructionFixture();
        var inputs = new GameFrameState().copyFrom(fixture.state);
        var world = inputs.overworld;
        var background = inputs.background;
        assertTrue(world.hasLink());
        assertTrue(world.hasRoom());
        inputs.reset().withScreen(RenderScreen.FILE_MENU);
        assertSame(world, inputs.overworld);
        assertSame(background, inputs.background);
        assertFalse(world.hasLink());
        assertFalse(world.hasRoom());
        assertFalse(background.hasIntroFrameSnapshot());
    }

    @Test
    void independentCapturePreservesRoomPixelsWhenBorrowedInputsChange() {
        var fixture = new SceneConstructionFixture();
        var inputs = new GameFrameState().copyFrom(fixture.state);
        var capture = FrameSnapshots.copy(inputs);
        inputs.roomInputs.tiles[0] = 7;
        assertNotEquals(7, capture.room().tileIds()[0]);
    }
}
