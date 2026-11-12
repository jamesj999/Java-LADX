package linksawakening.state;

import static linksawakening.state.PlayerNumbers.*;
import static linksawakening.state.PlayerState.*;

import java.util.Objects;

/** Equipment upgrade, tunic and power-up state used by ROM {@code wSwordLevel}. */
public final class EquipmentLevels {
    int swordLevel = 1;
    int shieldLevel = 1;
    int powerBraceletLevel = 1;
    PowerUp activePowerUp = PowerUp.NONE;
    Tunic tunicType = Tunic.GREEN;
    boolean runningWithPegasusBoots;
    int powerUpHits;

    public int swordLevel() {
        return swordLevel;
    }

    public void setSwordLevel(int value) {
        swordLevel = clamp(value, 0, 2);
    }

    public int shieldLevel() {
        return shieldLevel;
    }

    public void setShieldLevel(int value) {
        shieldLevel = clamp(value, 0, 2);
    }

    public int powerBraceletLevel() {
        return powerBraceletLevel;
    }

    public void setPowerBraceletLevel(int value) {
        powerBraceletLevel = clamp(value, 0, 2);
    }

    public PowerUp powerUp() {
        return activePowerUp;
    }

    public void setPowerUp(PowerUp value) {
        activePowerUp = Objects.requireNonNull(value, "value");
        powerUpHits = 0;
    }

    public int activePowerUp() {
        return powerUp().romValue();
    }

    public void setActivePowerUp(int value) {
        setPowerUp(PowerUp.fromRom(clamp(value, 0, 2)));
    }

    public void clearActivePowerUpForWarp() {
        activePowerUp = PowerUp.NONE;
    }

    public int powerUpHits() {
        return powerUpHits;
    }

    public Tunic tunic() {
        return tunicType;
    }

    public void setTunic(Tunic value) {
        tunicType = Objects.requireNonNull(value, "value");
    }

    public int tunicType() {
        return tunic().romValue();
    }

    public void setTunicType(int value) {
        setTunic(Tunic.fromRom(clamp(value, 0, 2)));
    }

    public boolean runningWithPegasusBoots() {
        return runningWithPegasusBoots;
    }

    public void setRunningWithPegasusBoots(boolean value) {
        runningWithPegasusBoots = value;
    }
}
