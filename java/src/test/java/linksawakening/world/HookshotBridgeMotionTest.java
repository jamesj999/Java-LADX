package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HookshotBridgeMotionTest {

    @Test
    void bridgeDirectionsUseTheRomVerticalSpeeds() {
        assertEquals(0x30, HookshotBridgeMotion.speedForDirection(0));
        assertEquals(0xD0, HookshotBridgeMotion.speedForDirection(1));

        HookshotBridgeMotion.Step down = HookshotBridgeMotion.advance(
            HookshotBridgeMotion.spawn(0x28, 0x30, 0));
        HookshotBridgeMotion.Step up = HookshotBridgeMotion.advance(
            HookshotBridgeMotion.spawn(0x28, 0x30, 1));

        assertEquals(0x33, down.state().y());
        assertEquals(0x2D, up.state().y());
    }

    @Test
    void bridgePreservesFixedPointCarryForRomStyleSpeedBytes() {
        HookshotBridgeMotion.State state = new HookshotBridgeMotion.State(
            0x28, 0x30, 0, 0x3F, 0xF0);

        HookshotBridgeMotion.Step step = HookshotBridgeMotion.advance(state);

        assertEquals(0x34, step.state().y());
        assertEquals(0xE0, step.state().speedAccumulator());
    }

    @Test
    void bridgeUsesThePreMoveVisualPositionToFindItsObjectCell() {
        HookshotBridgeMotion.ObjectCell cell = HookshotBridgeMotion.objectCell(
            HookshotBridgeMotion.spawn(0x28, 0x30, 0));

        assertEquals(0x20, cell.objectLeft());
        assertEquals(0x20, cell.objectTop());
    }
}
