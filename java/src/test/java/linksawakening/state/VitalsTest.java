package linksawakening.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VitalsTest {
    @Test
    void preservesOwnedBoundaryRules() {
        PlayerState player = new PlayerState();
        Vitals state = player.vitals();
        player.progression().setHasMedicine(true);
        player.progression().setMedicineCount(1);
        state.setHealth(1);
        state.addSubtractHealthBuffer(1);
        state.tickHealthBuffer();
        assertEquals(8, state.health());
        assertEquals(128, state.addHealthBuffer());
        assertEquals(160, state.invincibilityCounter());
        assertFalse(player.progression().hasMedicine());
    }
}
