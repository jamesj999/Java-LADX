package linksawakening.state;

import static linksawakening.state.PlayerNumbers.*;
import static linksawakening.state.PlayerState.*;

/** Wallet owns its player-state fields and their update rules. */
public final class Wallet {
    int rupees = 0;
    int addRupeeBuffer;
    int rupeeUpdateDelay;
    private final PlayerSounds playerSounds;

    Wallet(PlayerSounds playerSounds) {
        this.playerSounds = playerSounds;
    }

    public int rupees() {
        return rupees;
    }

    public void setRupees(int value) {
        rupees = clamp(value, 0, MAX_RUPEES);
    }

    public int addRupeeBuffer() {
        return addRupeeBuffer;
    }

    public void tickRupeeBuffer() {
        if (rupeeUpdateDelay != 0) {
            rupeeUpdateDelay--;
            return;
        }
        if (addRupeeBuffer == 0) {
            return;
        }
        playerSounds.write(ResourceSound.RUPEE);
        int step = Math.min(9, (addRupeeBuffer >> 5) + 1);
        addRupeeBuffer -= step;
        rupees = Math.min(MAX_RUPEES, rupees + step);
        if (rupees == MAX_RUPEES) addRupeeBuffer = 0;
    }
}
