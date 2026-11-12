package linksawakening.state;

import static linksawakening.state.PlayerNumbers.*;
import static linksawakening.state.PlayerState.*;

/** Ammunition owns its player-state fields and their update rules. */
public final class Ammunition {
    int arrowCount;
    int maxArrows;
    int bombCount;
    int maxBombs;
    int magicPowderCount;
    int maxMagicPowder;
    boolean hasToadstool;
    private final Inventory inventory;

    Ammunition(Inventory inventory) {
        this.inventory = inventory;
    }

    public int arrowCount() {
        return arrowCount;
    }

    public void setArrowCount(int value) {
        arrowCount = clamp(value, 0, maxArrows);
    }

    public int maxArrows() {
        return maxArrows;
    }

    public void setMaxArrows(int value) {
        maxArrows = clamp(value, 0, 99);
        arrowCount = Math.min(arrowCount, maxArrows);
    }

    public int bombCount() {
        return bombCount;
    }

    public void setBombCount(int value) {
        bombCount = clamp(value, 0, maxBombs);
    }

    public int maxBombs() {
        return maxBombs;
    }

    public void setMaxBombs(int value) {
        maxBombs = clamp(value, 0, 99);
        bombCount = Math.min(bombCount, maxBombs);
    }

    public int magicPowderCount() {
        return magicPowderCount;
    }

    public void setMagicPowderCount(int value) {
        magicPowderCount = clamp(value, 0, maxMagicPowder);
    }

    public int maxMagicPowder() {
        return maxMagicPowder;
    }

    public void setMaxMagicPowder(int value) {
        maxMagicPowder = clamp(value, 0, 99);
        magicPowderCount = Math.min(magicPowderCount, maxMagicPowder);
    }

    public boolean consumeMagicPowder() {
        if (magicPowderCount == 0) {
            return false;
        }
        magicPowderCount--;
        if (magicPowderCount == 0) {
            if (inventory.itemA == InventoryItem.MAGIC_POWDER.romValue()) {
                inventory.itemA = InventoryItem.EMPTY.romValue();
            }
            if (inventory.itemB == InventoryItem.MAGIC_POWDER.romValue()) {
                inventory.itemB = InventoryItem.EMPTY.romValue();
            }
        }
        return true;
    }

    public boolean hasToadstool() {
        return hasToadstool;
    }

    public void setHasToadstool(boolean value) {
        hasToadstool = value;
    }
}
