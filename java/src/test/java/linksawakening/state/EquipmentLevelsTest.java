package linksawakening.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EquipmentLevelsTest {
    @Test
    void preservesOwnedBoundaryRules() {
        EquipmentLevels state = new EquipmentLevels();
        state.setSwordLevel(3);
        assertEquals(2, state.swordLevel());
        state.setActivePowerUp(255);
        assertEquals(PowerUp.GUARDIAN_ACORN, state.powerUp());
        state.powerUpHits = 2;
        state.clearActivePowerUpForWarp();
        assertEquals(2, state.powerUpHits());
        state.setPowerUp(PowerUp.PIECE_OF_POWER);
        assertEquals(0, state.powerUpHits());
    }
}
