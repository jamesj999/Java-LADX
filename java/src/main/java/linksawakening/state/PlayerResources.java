package linksawakening.state;

import static linksawakening.state.PlayerNumbers.*;
import static linksawakening.state.PlayerState.*;

/** PlayerResources owns its player-state fields and their update rules. */
public final class PlayerResources {

    private static final int[] LOW_HEALTH_THRESHOLDS = {0x00, 0x22, 0xC9, 5, 5, 5, 9, 9, 9, 17, 17, 17, 25, 25, 25};
    private final PlayerSounds playerSounds;
    private final Vitals vitals;
    private final Wallet wallet;

    PlayerResources(PlayerSounds playerSounds, Vitals vitals, Wallet wallet) {
        this.playerSounds = playerSounds;
        this.vitals = vitals;
        this.wallet = wallet;
    }

    public void clearResourceBuffersForDebug() {
        vitals.addHealthBuffer = 0;
        vitals.subtractHealthBuffer = 0;
        wallet.addRupeeBuffer = 0;
        wallet.rupeeUpdateDelay = 0;
        playerSounds.clear();
        vitals.onLowHealth = false;
        vitals.timeToNextLowHealthSound = 0;
    }

    public void tickResourceBuffers(int frameCounter) {
        if ((frameCounter & 0x01) == 0) {
            wallet.tickRupeeBuffer();
        }
        vitals.onLowHealth = vitals.health < LOW_HEALTH_THRESHOLDS[vitals.maxHearts];
        if (vitals.onLowHealth) {
            vitals.timeToNextLowHealthSound = (vitals.timeToNextLowHealthSound - 1) & 0xFF;
            if (vitals.timeToNextLowHealthSound == 0xFF) {
                playerSounds.write(ResourceSound.LOW_HEARTS);
                vitals.timeToNextLowHealthSound = 0x30;
            }
        }
        if ((frameCounter & 1) != 0) vitals.tickHealthBuffer();
    }
}
