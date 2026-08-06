package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SwitchBlockAnimationTest {

    @Test
    void advancesTheRomStageSequenceAndTogglesAtTheThirdUpdate() {
        int state = 0;

        SwitchBlockAnimation.Step stage1 = SwitchBlockAnimation.advance(0x01, state);
        assertStep(stage1, 0x02, 0x00, -1, -1);

        SwitchBlockAnimation.Step stage2 = SwitchBlockAnimation.advance(
            stage1.nextStage(), stage1.switchBlocksState());
        assertStep(stage2, 0x03, 0x02, -1, -1);

        SwitchBlockAnimation.Step stage3 = SwitchBlockAnimation.advance(
            stage2.nextStage(), stage2.switchBlocksState());
        assertStep(stage3, 0x04, 0x02, 0x40, 0x104);

        SwitchBlockAnimation.Step stage4 = SwitchBlockAnimation.advance(
            stage3.nextStage(), stage3.switchBlocksState());
        assertStep(stage4, 0x05, 0x02, -1, -1);

        SwitchBlockAnimation.Step stage5 = SwitchBlockAnimation.advance(
            stage4.nextStage(), stage4.switchBlocksState());
        assertStep(stage5, 0x06, 0x02, 0x40, 0x108);

        SwitchBlockAnimation.Step stage6 = SwitchBlockAnimation.advance(
            stage5.nextStage(), stage5.switchBlocksState());
        assertStep(stage6, 0x07, 0x02, -1, -1);

        SwitchBlockAnimation.Step stage7 = SwitchBlockAnimation.advance(
            stage6.nextStage(), stage6.switchBlocksState());
        assertStep(stage7, 0x08, 0x02, 0x80, 0x104);

        SwitchBlockAnimation.Step stage8 = SwitchBlockAnimation.advance(
            stage7.nextStage(), stage7.switchBlocksState());
        assertStep(stage8, 0x09, 0x02, -1, -1);

        SwitchBlockAnimation.Step stage9 = SwitchBlockAnimation.advance(
            stage8.nextStage(), stage8.switchBlocksState());
        assertStep(stage9, 0x00, 0x02, 0x00, 0x108);
    }

    @Test
    void initialCopiesSelectTheRomFinalTableForEitherState() {
        assertEquals(
            java.util.List.of(
                new SwitchBlockAnimation.TileCopy(0x00, 0x104),
                new SwitchBlockAnimation.TileCopy(0x80, 0x108)),
            SwitchBlockAnimation.initialCopies(0x00));
        assertEquals(
            java.util.List.of(
                new SwitchBlockAnimation.TileCopy(0x80, 0x104),
                new SwitchBlockAnimation.TileCopy(0x00, 0x108)),
            SwitchBlockAnimation.initialCopies(0x02));
    }

    @Test
    void onlyStagesOneThroughNineCanAdvance() {
        assertTrue(SwitchBlockAnimation.isAnimating(0x01));
        assertTrue(SwitchBlockAnimation.isAnimating(0x09));
        assertFalse(SwitchBlockAnimation.isAnimating(0x00));
        assertFalse(SwitchBlockAnimation.isAnimating(0x0A));
    }

    private static void assertStep(SwitchBlockAnimation.Step step, int nextStage, int state,
                                   int sourceOffset, int destinationTile) {
        assertEquals(nextStage, step.nextStage());
        assertEquals(state, step.switchBlocksState());
        assertEquals(sourceOffset, step.tileCopy() == null ? -1 : step.tileCopy().sourceOffset());
        assertEquals(destinationTile,
            step.tileCopy() == null ? -1 : step.tileCopy().destinationTile());
    }
}
