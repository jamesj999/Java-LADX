package linksawakening.world;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TransitionControllerTest {

    @Test
    void ManboMatchesOutdoorRomTraceThroughDestinationMusicAndExit() {
        TransitionController controller = new TransitionController(new byte[0x52000]);
        AtomicInteger loads = new AtomicInteger();

        AtomicInteger music = new AtomicInteger();
        assertTrue(controller.startManboIn(false, () -> {
            loads.incrementAndGet();
            controller.whenRoomReady(music::incrementAndGet);
        })); // ROM frame 255
        assertEquals(TransitionController.State.MANBO_IN, controller.state());
        assertEquals(1, controller.transitionFrame());

        tick(controller, 95); // 350
        assertEquals(0x60, controller.transitionFrame());
        controller.tick(); // 351
        assertEquals(0x60, controller.transitionFrame());

        tick(controller, 126); // 477
        assertEquals(0xBF, controller.transitionFrame());
        controller.tick(); // 478
        assertEquals(TransitionController.State.MANBO_ROOM_LOAD, controller.state());
        assertNull(controller.manboWaveOffsets(1));
        assertTrue(controller.isInputBlocked());

        tick(controller, 16); // 494, ROM updates the room ID during loading.
        assertEquals(1, loads.get());
        tick(controller, 26); // 520
        assertEquals(0, music.get());
        controller.tick(); // 521, room ready and destination music starts.
        assertEquals(1, music.get());
        assertEquals(TransitionController.State.MANBO_ROOM_LOAD, controller.state());

        controller.tick(); // 522
        assertEquals(TransitionController.State.MANBO_OUT, controller.state());
        assertEquals(0x31, controller.transitionFrame());
        assertTrue(controller.isInputBlocked());

        tick(controller, 24); // 546
        assertEquals(0x48, controller.transitionFrame());
        tick(controller, 131); // 677
        assertEquals(0xBF, controller.transitionFrame());
        controller.tick(); // 678
        assertEquals(TransitionController.State.IDLE, controller.state());
        assertFalse(controller.isInputBlocked());
        assertEquals(1, loads.get());
    }

    @Test
    void manboUploadsBackgroundAndObjectPalettesInGroupsOfFour() {
        TransitionController controller = new TransitionController();
        controller.startManboIn(false, () -> {});
        int[][] black = new int[8][4];
        tick(controller, 115); // Logical frame 106 in the CGB trace.
        int[][] bg = controller.applyFade(black);
        int[][] obj = controller.applyObjectFade(black);
        assertEquals(gray555(9), bg[0][0]);
        assertEquals(gray555(10), bg[4][0]);
        assertEquals(gray555(7), obj[0][0]);
        assertEquals(gray555(8), obj[4][0]);
        assertEquals(0, black[0][0]);
    }

    private static int gray555(int value) {
        return (value * 255 / 31) * 0x010101;
    }

    @Test
    void ordinaryDoorwayHoldsWhiteUntilDestinationMusicStarts() {
        TransitionController controller = new TransitionController();
        AtomicInteger loads = new AtomicInteger();
        AtomicInteger music = new AtomicInteger();
        controller.startFadeOut(() -> {
            loads.incrementAndGet();
            controller.whenRoomReady(music::incrementAndGet);
        });

        int[][] black = new int[8][4];
        controller.tick();
        assertEquals(gray555(4), controller.applyFade(black)[4][0]);
        assertEquals(0, controller.applyObjectFade(black)[4][0]);
        tick(controller, 15);
        assertEquals(TransitionController.State.ROOM_LOAD, controller.state());
        assertEquals(0xFFFFFF, controller.applyObjectFade(black)[0][0]);
        tick(controller, 3);
        assertEquals(1, loads.get());
        tick(controller, 27);
        assertEquals(0, music.get());
        controller.tick();
        assertEquals(1, music.get());
        assertEquals(TransitionController.State.FADING_IN, controller.state());
        tick(controller, 15);
        assertTrue(controller.isInputBlocked());
        controller.tick();
        assertFalse(controller.isInputBlocked());
    }

    @Test
    void immediatelyReturningUpTheLadderWaitsForBothSoundTimers() {
        TransitionController controller = new TransitionController();
        AtomicInteger changes = new AtomicInteger();
        controller.startDoorFade(true, changes::incrementAndGet);
        while (controller.isActive()) controller.tick();
        tick(controller, 5);
        controller.startDoorFade(true, changes::incrementAndGet);

        tick(controller, 15);
        assertEquals(6, controller.doorVolume());
        tick(controller, 12);
        assertEquals(0, controller.doorVolume());
        assertEquals(1, changes.get());
        controller.tick();
        assertEquals(2, changes.get());
        controller.tick();
        assertTrue(controller.stopDoorMusic());
    }

    private static void tick(TransitionController controller, int ticks) {
        for (int frame = 0; frame < ticks; frame++) {
            controller.tick();
        }
    }

    @Test
    void ManboEntryCannotReplaceAnActiveRoomTransition() {
        TransitionController controller = new TransitionController();

        controller.startFadeOut(() -> {});

        assertFalse(controller.startManboIn(false, () -> {}));
        assertEquals(TransitionController.State.FADING_OUT, controller.state());
    }
}
