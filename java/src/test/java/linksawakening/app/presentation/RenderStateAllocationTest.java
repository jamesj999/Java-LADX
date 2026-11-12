package linksawakening.app.presentation;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.app.AllocationSessions;
import linksawakening.render.FrameScene;
import linksawakening.render.GameFrameSceneBuilder;
import linksawakening.testing.AllocationProbe;
import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;

/** Covers frame-state assembly and scene construction, excluding GPU composition. */
@RequiresRom
class RenderStateAllocationTest {
    private static volatile FrameScene observed;
    private static volatile Object snapshot;

    @Test
    void assemblyAndSceneAllocation() throws Exception {
        try (var session = AllocationSessions.active()) {
            var assembler = session.flows().renderer();
            var builder = new GameFrameSceneBuilder();
            double bytes = AllocationProbe.bytesPerIteration(600, 120, () -> {
                session.frameClock().advance();
                observed = builder.build(assembler.currentGameFrameState());
            });
            System.out.println("Assembly + scene bytes/frame: " + bytes);
            assertTrue(observed.layerCount() > 0);
            double roomBytes = AllocationProbe.bytesPerIteration(
                    600,
                    120,
                    () -> snapshot =
                            session.services().world().roomSession().rooms().renderSnapshot());
            double stateBytes =
                    AllocationProbe.bytesPerIteration(600, 120, () -> snapshot = assembler.currentGameFrameState());
            var frame = assembler.currentGameFrameState();
            double sceneBytes = AllocationProbe.bytesPerIteration(600, 120, () -> observed = builder.build(frame));
            System.out.println("Room snapshot / assembled state / scene-only bytes: " + roomBytes + " / " + stateBytes
                    + " / " + sceneBytes);
            assertTrue(stateBytes - roomBytes <= 100, "App frame-state overhead: " + (stateBytes - roomBytes));
            // EA-off baseline 17,840; achieved 16,960. Includes world snapshot defensive copies.
            assertTrue(bytes <= 21_200, "Assembly + scene bytes/frame: " + bytes);
        }
    }
}
