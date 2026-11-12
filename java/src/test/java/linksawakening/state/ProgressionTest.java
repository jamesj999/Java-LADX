package linksawakening.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProgressionTest {
    @Test
    void preservesOwnedBoundaryRules() {
        Progression state = new Progression();
        state.setDeathCount(998);
        state.incrementDeathCount();
        state.incrementDeathCount();
        assertEquals(0x999, state.deathCountBcd());
        state.setTailKeyCount(256);
        assertEquals(255, state.tailKeyCount());
        state.setOcarinaSongFlags(255);
        assertEquals(7, state.ocarinaSongFlags());
        state.setSelectedSongIndex(-1);
        assertEquals(0, state.selectedSongIndex());
    }
}
