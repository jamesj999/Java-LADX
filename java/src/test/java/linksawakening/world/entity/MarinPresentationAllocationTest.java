package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

class MarinPresentationAllocationTest {
    @Test
    void repeatingMarinVariantsReuseExactImmutableValuesButPositionChangesRemainVisible() {
        var runtime = new RoomEntityRuntimeBuilder(TestEntities.room(
                        TestEntities.active(0x3E).slot(0).at(64, 64).build()))
                .build();
        assertTrue(runtime.commands().applyMarinWakeUpPresentation(56, 56, 0));
        var first = runtime.inspection().slots.get(0);
        assertTrue(runtime.commands().applyMarinWakeUpPresentation(56, 56, 1));
        assertNotSame(first, runtime.inspection().slots.get(0));
        assertTrue(runtime.commands().applyMarinWakeUpPresentation(56, 56, 0));
        assertSame(first, runtime.inspection().slots.get(0));
        runtime.commands().applyMarinWakeUpPresentation(57, 56, 0);
        assertEquals(57, runtime.inspection().slots.get(0).x());
        assertEquals(56, first.x());
        int[] frame = {0};
        Runnable repeat = () -> runtime.commands().applyMarinWakeUpPresentation(56, 56, frame[0]++ & 7);
        assertEquals(0.0, AllocationProbe.bytesPerIteration(4000, 1000, repeat));
    }
}
