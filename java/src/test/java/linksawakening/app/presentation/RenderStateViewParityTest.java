package linksawakening.app.presentation;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.app.AllocationSessions;
import linksawakening.render.GameFrameSceneBuilder;
import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;

/** Compares the running borrowed path with the retained owned reference over inventory transitions. */
@RequiresRom
class RenderStateViewParityTest {
    @Test
    void sessionCompositionMatchesOwnedScenes() throws Exception {
        try (var session = AllocationSessions.active()) {
            var ownedBuilder = new GameFrameSceneBuilder();
            var pixels = session.services().visual().indexedDisplayBuffer();
            byte[] actual = new byte[pixels.length], expected = new byte[pixels.length];
            for (int frame = 0; frame < 80; frame++) {
                if (frame == 8 || frame == 55) session.headlessButton("start", true);
                if (frame == 9 || frame == 56) session.headlessButton("start", false);
                session.headlessFrame(actual);
                ownedBuilder
                        .build(session.flows().renderer().currentGameFrameState())
                        .drawTo(expected, session.services().rom().gpu());
                assertArrayEquals(expected, actual, "Frame " + frame);
            }
        }
    }
}
