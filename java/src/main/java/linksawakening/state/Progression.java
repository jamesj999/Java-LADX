package linksawakening.state;

import static linksawakening.state.PlayerNumbers.*;
import static linksawakening.state.PlayerState.*;

/** Progression owns its player-state fields and their update rules. */
public final class Progression {
    int heartPieces;
    int seashells;
    boolean hasFlippers;
    boolean hasMedicine;
    int tradeSequenceItem;
    int medicineCount;
    int tailKeyCount;
    int anglerKeyCount;
    int faceKeyCount;
    int birdKeyCount;
    int goldenLeavesCount;
    int deathCount;
    final int[] chestItemCounts = new int[0x22];
    int ocarinaSongFlags;
    int selectedSongIndex;

    public int heartPieces() {
        return heartPieces;
    }

    public void setHeartPieces(int value) {
        heartPieces = clamp(value, 0, 4);
    }

    public int seashells() {
        return seashells;
    }

    public void setSeashells(int value) {
        seashells = clamp(value, 0, 99);
    }

    public boolean hasFlippers() {
        return hasFlippers;
    }

    public void setHasFlippers(boolean value) {
        hasFlippers = value;
    }

    public boolean hasMedicine() {
        return hasMedicine;
    }

    public void setHasMedicine(boolean value) {
        hasMedicine = value;
    }

    public int tradeSequenceItem() {
        return tradeSequenceItem;
    }

    public void setTradeSequenceItem(int value) {
        tradeSequenceItem = clamp(value, 0, 0xFF);
    }

    public int medicineCount() {
        return medicineCount;
    }

    public void setMedicineCount(int value) {
        medicineCount = clamp(value, 0, 0xFF);
    }

    public int tailKeyCount() {
        return tailKeyCount;
    }

    public void setTailKeyCount(int value) {
        tailKeyCount = clamp(value, 0, 0xFF);
    }

    public int anglerKeyCount() {
        return anglerKeyCount;
    }

    public void setAnglerKeyCount(int value) {
        anglerKeyCount = clamp(value, 0, 0xFF);
    }

    public int faceKeyCount() {
        return faceKeyCount;
    }

    public void setFaceKeyCount(int value) {
        faceKeyCount = clamp(value, 0, 0xFF);
    }

    public int birdKeyCount() {
        return birdKeyCount;
    }

    public void setBirdKeyCount(int value) {
        birdKeyCount = clamp(value, 0, 0xFF);
    }

    public int goldenLeavesCount() {
        return goldenLeavesCount;
    }

    public void setGoldenLeavesCount(int value) {
        goldenLeavesCount = clamp(value, 0, 0xFF);
    }

    public int deathCount() {
        return deathCount;
    }

    public void setDeathCount(int value) {
        deathCount = clamp(value, 0, 999);
    }

    public void incrementDeathCount() {
        setDeathCount(deathCount + 1);
    }

    public int deathCountBcd() {
        return (deathCount / 100 << 8) | (deathCount / 10 % 10 << 4) | deathCount % 10;
    }

    public int chestItemCount(int chestItem) {
        if (chestItem < 0 || chestItem >= chestItemCounts.length) {
            throw new IllegalArgumentException("Chest item count index out of range: " + chestItem);
        }
        return chestItemCounts[chestItem];
    }

    public int ocarinaSongFlags() {
        return ocarinaSongFlags;
    }

    public void setOcarinaSongFlags(int value) {
        ocarinaSongFlags = value & 0x07;
    }

    public int selectedSongIndex() {
        return selectedSongIndex;
    }

    public void setSelectedSongIndex(int value) {
        selectedSongIndex = clamp(value, 0, 2);
    }
}
