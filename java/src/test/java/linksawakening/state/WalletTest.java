package linksawakening.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WalletTest {
    @Test
    void preservesOwnedBoundaryRules() {
        Wallet state = new Wallet(new PlayerSounds());
        state.setRupees(998);
        state.addRupeeBuffer = 64;
        state.tickRupeeBuffer();
        assertEquals(999, state.rupees());
        assertEquals(0, state.addRupeeBuffer());
        state.rupeeUpdateDelay = 1;
        state.addRupeeBuffer = 10;
        state.tickRupeeBuffer();
        assertEquals(10, state.addRupeeBuffer());
    }
}
