package linksawakening.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PincerBodyOamTest {

    @Test
    void bodyUsesTheRomQuarterDisplacementAndThreeTileChain() {
        List<PincerBodyOam.Entry> entries = PincerBodyOam.entries(
            3, 0x40, 0x50, 0x4C, 0x58, 3);

        assertEquals(3, entries.size());
        assertEquals(new PincerBodyOam.Entry(3, 0x44, 0x50, 0x6A, 0x02), entries.get(0));
        assertEquals(new PincerBodyOam.Entry(3, 0x47, 0x52, 0x6A, 0x02), entries.get(1));
        assertEquals(new PincerBodyOam.Entry(3, 0x4A, 0x54, 0x6A, 0x02), entries.get(2));
    }

    @Test
    void hiddenAndPreparingStatesDoNotEmitBodyOam() {
        assertEquals(List.of(), PincerBodyOam.entries(0, 0x40, 0x50, 0x48, 0x58, 2));
    }
}
