package linksawakening.vfx;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

class TransientVfxLiveViewTest {
    private static volatile int retained;

    @Test
    void liveViewTracksSlotOrderWhileSnapshotsRetainHistory() {
        var system = new TransientVfxSystem(2);
        var view = system.liveSlots();
        assertTrue(view.isEmpty());
        system.spawn(TransientVfxType.SWORD_POKE, 10, 20, 3);
        system.spawn(TransientVfxType.BUSH_LEAVES, 30, 40);
        var snapshot = system.activeSlots();
        system.tick();
        assertEquals(14, view.getFirst().countdown());
        assertEquals(15, snapshot.getFirst().countdown());
        assertEquals(3, view.getFirst().variant());
        assertThrows(UnsupportedOperationException.class, view::clear);
        assertThrows(UnsupportedOperationException.class, () -> view.removeFirst());
        for (int i = 0; i < 14; i++) system.tick();
        assertEquals(1, view.size());
        assertEquals(1, view.getFirst().slotIndex());
        system.spawn(TransientVfxType.SWORD_POKE, 50, 60);
        assertEquals(0, view.getFirst().slotIndex());
        assertEquals(50, view.getFirst().worldX());
        assertEquals(10, snapshot.getFirst().worldX());
        system.clear();
        assertTrue(view.isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> view.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> view.get(-1));
        assertThrows(UnsupportedOperationException.class, view::clear);
    }

    @Test
    void immediateSlotReadsDoNotAllocate() {
        var system = new TransientVfxSystem(2);
        system.spawn(TransientVfxType.BUSH_LEAVES, 10, 20);
        double bytes = AllocationProbe.bytesPerIteration(4000, 1000, () -> {
            var view = system.liveSlots();
            for (int i = 0; i < view.size(); i++) retained = view.get(i).countdown();
        });
        assertEquals(31, retained);
        assertEquals(0, bytes);
    }
}
