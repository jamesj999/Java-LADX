package linksawakening.state;

import static linksawakening.state.PlayerState.*;

import linksawakening.save.SaveSlotState;

/** PlayerLifecycleCompatibility behavior on the shared PlayerState state. */
abstract class PlayerLifecycleCompatibility extends PlayerInventoryCompatibility {
    PlayerLifecycleCompatibility() {
        super();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int seashells() {
        return progression().seashells();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setSeashells(int value) {
        progression().setSeashells(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public PowerUp powerUp() {
        return equipmentLevels().powerUp();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setPowerUp(PowerUp value) {
        equipmentLevels().setPowerUp(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public int activePowerUp() {
        return equipmentLevels().activePowerUp();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setActivePowerUp(int value) {
        equipmentLevels().setActivePowerUp(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void clearActivePowerUpForWarp() {
        equipmentLevels().clearActivePowerUpForWarp();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public int powerUpHits() {
        return equipmentLevels().powerUpHits();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public Tunic tunic() {
        return equipmentLevels().tunic();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setTunic(Tunic value) {
        equipmentLevels().setTunic(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public int tunicType() {
        return equipmentLevels().tunicType();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setTunicType(int value) {
        equipmentLevels().setTunicType(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public boolean runningWithPegasusBoots() {
        return equipmentLevels().runningWithPegasusBoots();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setRunningWithPegasusBoots(boolean value) {
        equipmentLevels().setRunningWithPegasusBoots(value);
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public int itemA() {
        return inventory().itemA();
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public void setItemA(int inventoryId) {
        inventory().setItemA(inventoryId);
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public int itemB() {
        return inventory().itemB();
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public void setItemB(int inventoryId) {
        inventory().setItemB(inventoryId);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int ocarinaSongFlags() {
        return progression().ocarinaSongFlags();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setOcarinaSongFlags(int value) {
        progression().setOcarinaSongFlags(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int selectedSongIndex() {
        return progression().selectedSongIndex();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setSelectedSongIndex(int value) {
        progression().setSelectedSongIndex(value);
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public void setSubscreenItems(int[] items) {
        inventory().setSubscreenItems(items);
    }

    /** @deprecated Use {@link #playerSaveLoader()}. */
    @Deprecated(forRemoval = true)
    public void applySavedGame(SaveSlotState saved) {
        playerSaveLoader().applySavedGame(saved);
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public int subscreenItem(int slotIndex) {
        return inventory().subscreenItem(slotIndex);
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public void swapItemAWithSubscreen(int slotIndex) {
        inventory().swapItemAWithSubscreen(slotIndex);
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public void swapItemBWithSubscreen(int slotIndex) {
        inventory().swapItemBWithSubscreen(slotIndex);
    }
}
