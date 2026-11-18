package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import linksawakening.world.transition.TransitionController;
import org.junit.jupiter.api.Test;

final class TransitionControllerTest {

    @Test
    void ordinaryWarpUploadsBackgroundAndObjectColorsOnAlternateFrames() {
        TransitionController controller = new TransitionController();
        controller.startFadeOut(() -> {});
        controller.tick();
        assertEquals(0x212121, controller.mapFadeColor(0, false));
        assertEquals(0, controller.mapFadeColor(0, true));
        for (int frame = 1; frame < 16; frame++) controller.tick();
        assertEquals(0xFFFFFF, controller.mapFadeColor(0, false));
        assertEquals(0xFFFFFF, controller.mapFadeColor(0, true));
        controller.tick();
        assertEquals(0xFFDEDE, controller.mapFadeColor(0xFF0000, false));
        assertEquals(0xFFFFFF, controller.mapFadeColor(0xFF0000, true));
    }

    @Test
    void ManboEntryRunsForTheRomC0FrameWindowThenStartsTheRomOutEffect() {
        TransitionController controller = new TransitionController();
        AtomicInteger completed = new AtomicInteger();

        assertTrue(controller.startManboIn(completed::incrementAndGet));
        assertEquals(TransitionController.State.MANBO_IN, controller.state());
        assertEquals(0, controller.transitionFrame());
        assertEquals(0, controller.fadeLevel());

        for (int frame = 0; frame < 0xBF; frame++) {
            controller.tick();
        }
        assertEquals(0, completed.get());
        assertEquals(0xBF, controller.transitionFrame());

        controller.tick();

        assertEquals(1, completed.get());
        assertEquals(TransitionController.State.MANBO_OUT, controller.state());
        assertTrue(controller.isInputBlocked());
        assertEquals(TransitionController.MANBO_OUT_INITIAL_FRAME, controller.transitionFrame());
        assertEquals(TransitionController.MAX_FADE_STEP, controller.fadeLevel());

        for (int frame = TransitionController.MANBO_OUT_INITIAL_FRAME;
                frame < TransitionController.MANBO_TRANSITION_FRAMES;
                frame++) {
            controller.tick();
        }
        assertEquals(TransitionController.State.IDLE, controller.state());
        assertFalse(controller.isInputBlocked());
    }

    @Test
    void ManboEntryCannotReplaceAnActiveRoomTransition() {
        TransitionController controller = new TransitionController();

        controller.startFadeOut(() -> {});

        assertFalse(controller.startManboIn(() -> {}));
        assertEquals(TransitionController.State.FADING_OUT, controller.state());
    }
}
