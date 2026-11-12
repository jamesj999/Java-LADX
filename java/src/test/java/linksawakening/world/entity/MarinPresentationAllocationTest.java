package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

class MarinPresentationAllocationTest {
    @Test
    void repeatingMarinVariantsReuseExactImmutableValuesButPositionChangesRemainVisible() {
        var runtime = RoomEntityRuntime.builder(TestEntities.room(
                        TestEntities.active(0x3E).slot(0).at(64, 64).build()))
                .build();
        assertTrue(runtime.applyMarinWakeUpPresentation(56, 56, 0));
        var first = runtime.slots[0];
        assertTrue(runtime.applyMarinWakeUpPresentation(56, 56, 1));
        assertNotSame(first, runtime.slots[0]);
        assertTrue(runtime.applyMarinWakeUpPresentation(56, 56, 0));
        assertSame(first, runtime.slots[0]);
        runtime.applyMarinWakeUpPresentation(57, 56, 0);
        assertEquals(57, runtime.slots[0].x());
        assertEquals(56, first.x());
        int[] frame = {0};
        Runnable repeat = () -> runtime.applyMarinWakeUpPresentation(56, 56, (frame[0]++) & 7);
        assertEquals(0.0, AllocationProbe.bytesPerIteration(4000, 1000, repeat));
    }
}
