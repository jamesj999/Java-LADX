package linksawakening.world;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TransitionControllerTest {

    @Test
    void ManboEntryRunsForTheRomC0FrameWindowThenStartsTheReturnFade() {
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
        assertEquals(TransitionController.State.FADING_IN, controller.state());
        assertTrue(controller.isInputBlocked());
        assertEquals(TransitionController.MAX_FADE_STEP, controller.fadeLevel());
    }

    @Test
    void ManboEntryCannotReplaceAnActiveRoomTransition() {
        TransitionController controller = new TransitionController();

        controller.startFadeOut(() -> {});

        assertFalse(controller.startManboIn(() -> {}));
        assertEquals(TransitionController.State.FADING_OUT, controller.state());
    }
}
