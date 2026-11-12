package linksawakening.state;

import static linksawakening.state.PlayerState.*;

import linksawakening.save.SaveSlotState;

/** @deprecated Temporary external API migration layer. */
@Deprecated(forRemoval = true)
abstract class PlayerLifecycleCompatibility extends PlayerInventoryCompatibility {
    @Deprecated(forRemoval = true)
    PlayerLifecycleCompatibility() {
        super();
    }

    /** @deprecated Use progression().seashells(). */
    @Deprecated(forRemoval = true)
    public int seashells() {
        return progression().seashells();
    }

    /** @deprecated Use progression().setSeashells(value). */
    @Deprecated(forRemoval = true)
    public void setSeashells(int value) {
        progression().setSeashells(value);
    }

    /** @deprecated Use equipmentLevels().powerUp(). */
    @Deprecated(forRemoval = true)
    public PowerUp powerUp() {
        return equipmentLevels().powerUp();
    }

    /** @deprecated Use equipmentLevels().setPowerUp(value). */
    @Deprecated(forRemoval = true)
    public void setPowerUp(PowerUp value) {
        equipmentLevels().setPowerUp(value);
    }

    /** @deprecated Use equipmentLevels().activePowerUp(). */
    @Deprecated(forRemoval = true)
    public int activePowerUp() {
        return equipmentLevels().activePowerUp();
    }

    /** @deprecated Use equipmentLevels().setActivePowerUp(value). */
    @Deprecated(forRemoval = true)
    public void setActivePowerUp(int value) {
        equipmentLevels().setActivePowerUp(value);
    }

    /** @deprecated Use equipmentLevels().clearActivePowerUpForWarp(). */
    @Deprecated(forRemoval = true)
    public void clearActivePowerUpForWarp() {
        equipmentLevels().clearActivePowerUpForWarp();
    }

    /** @deprecated Use equipmentLevels().powerUpHits(). */
    @Deprecated(forRemoval = true)
    public int powerUpHits() {
        return equipmentLevels().powerUpHits();
    }

    /** @deprecated Use equipmentLevels().tunic(). */
    @Deprecated(forRemoval = true)
    public Tunic tunic() {
        return equipmentLevels().tunic();
    }

    /** @deprecated Use equipmentLevels().setTunic(value). */
    @Deprecated(forRemoval = true)
    public void setTunic(Tunic value) {
        equipmentLevels().setTunic(value);
    }

    /** @deprecated Use equipmentLevels().tunicType(). */
    @Deprecated(forRemoval = true)
    public int tunicType() {
        return equipmentLevels().tunicType();
    }

    /** @deprecated Use equipmentLevels().setTunicType(value). */
    @Deprecated(forRemoval = true)
    public void setTunicType(int value) {
        equipmentLevels().setTunicType(value);
    }

    /** @deprecated Use equipmentLevels().runningWithPegasusBoots(). */
    @Deprecated(forRemoval = true)
    public boolean runningWithPegasusBoots() {
        return equipmentLevels().runningWithPegasusBoots();
    }

    /** @deprecated Use equipmentLevels().setRunningWithPegasusBoots(value). */
    @Deprecated(forRemoval = true)
    public void setRunningWithPegasusBoots(boolean value) {
        equipmentLevels().setRunningWithPegasusBoots(value);
    }

    /** @deprecated Use inventory().itemA(). */
    @Deprecated(forRemoval = true)
    public int itemA() {
        return inventory().itemA();
    }

    /** @deprecated Use inventory().setItemA(inventoryId). */
    @Deprecated(forRemoval = true)
    public void setItemA(int inventoryId) {
        inventory().setItemA(inventoryId);
    }

    /** @deprecated Use inventory().itemB(). */
    @Deprecated(forRemoval = true)
    public int itemB() {
        return inventory().itemB();
    }

    /** @deprecated Use inventory().setItemB(inventoryId). */
    @Deprecated(forRemoval = true)
    public void setItemB(int inventoryId) {
        inventory().setItemB(inventoryId);
    }

    /** @deprecated Use progression().ocarinaSongFlags(). */
    @Deprecated(forRemoval = true)
    public int ocarinaSongFlags() {
        return progression().ocarinaSongFlags();
    }

    /** @deprecated Use progression().setOcarinaSongFlags(value). */
    @Deprecated(forRemoval = true)
    public void setOcarinaSongFlags(int value) {
        progression().setOcarinaSongFlags(value);
    }

    /** @deprecated Use progression().selectedSongIndex(). */
    @Deprecated(forRemoval = true)
    public int selectedSongIndex() {
        return progression().selectedSongIndex();
    }

    /** @deprecated Use progression().setSelectedSongIndex(value). */
    @Deprecated(forRemoval = true)
    public void setSelectedSongIndex(int value) {
        progression().setSelectedSongIndex(value);
    }

    /** @deprecated Use inventory().setSubscreenItems(items). */
    @Deprecated(forRemoval = true)
    public void setSubscreenItems(int[] items) {
        inventory().setSubscreenItems(items);
    }

    /** @deprecated Use playerSaveLoader().applySavedGame(saved). */
    @Deprecated(forRemoval = true)
    public void applySavedGame(SaveSlotState saved) {
        playerSaveLoader().applySavedGame(saved);
    }

    /** @deprecated Use inventory().subscreenItem(slotIndex). */
    @Deprecated(forRemoval = true)
    public int subscreenItem(int slotIndex) {
        return inventory().subscreenItem(slotIndex);
    }

    /** @deprecated Use inventory().swapItemAWithSubscreen(slotIndex). */
    @Deprecated(forRemoval = true)
    public void swapItemAWithSubscreen(int slotIndex) {
        inventory().swapItemAWithSubscreen(slotIndex);
    }

    /** @deprecated Use inventory().swapItemBWithSubscreen(slotIndex). */
    @Deprecated(forRemoval = true)
    public void swapItemBWithSubscreen(int slotIndex) {
        inventory().swapItemBWithSubscreen(slotIndex);
    }
}
