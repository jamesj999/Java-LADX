package linksawakening.render;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

/** Verifies the reusable path also preserves the real ROM-backed dialog-font boundary. */
@RequiresRom
final class ReusableSceneRomParityTest {
    @Test
    void defaultRomWindowMatchesOwnedSceneForEveryPromptPhase() throws Exception {
        try (var input = TestRom.open()) {
            assertTrue(input.read() >= 0);
        }
        var fixture = new SceneConstructionFixture();
        var state = new ReusableGameFrameState().copyFrom(fixture.state);
        var live = new GameFrameSceneBuilder();
        var owned = new GameFrameSceneBuilder();
        byte[] expected = new byte[fixture.output.length];
        for (int frame = 0; frame < 48; frame++) {
            state.withFrameCounter(frame);
            owned.build(state.snapshot()).drawTo(expected, fixture.gpu);
            live.build(state).drawTo(fixture.output, fixture.gpu);
            assertArrayEquals(expected, fixture.output, "ROM window phase " + frame);
        }
    }
}
