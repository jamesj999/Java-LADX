package linksawakening.state;

import static linksawakening.state.PlayerState.*;

/** PlayerInventoryCompatibility behavior on the shared PlayerState state. */
abstract class PlayerInventoryCompatibility extends PlayerProgressionCompatibility {
    PlayerInventoryCompatibility() {
        super();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int tradeSequenceItem() {
        return progression().tradeSequenceItem();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setTradeSequenceItem(int value) {
        progression().setTradeSequenceItem(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int medicineCount() {
        return progression().medicineCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setMedicineCount(int value) {
        progression().setMedicineCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int tailKeyCount() {
        return progression().tailKeyCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setTailKeyCount(int value) {
        progression().setTailKeyCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int anglerKeyCount() {
        return progression().anglerKeyCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setAnglerKeyCount(int value) {
        progression().setAnglerKeyCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int faceKeyCount() {
        return progression().faceKeyCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setFaceKeyCount(int value) {
        progression().setFaceKeyCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int birdKeyCount() {
        return progression().birdKeyCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setBirdKeyCount(int value) {
        progression().setBirdKeyCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int goldenLeavesCount() {
        return progression().goldenLeavesCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setGoldenLeavesCount(int value) {
        progression().setGoldenLeavesCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int chestItemCount(int chestItem) {
        return progression().chestItemCount(chestItem);
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public int arrowCount() {
        return ammunition().arrowCount();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setArrowCount(int value) {
        ammunition().setArrowCount(value);
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public int maxArrows() {
        return ammunition().maxArrows();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setMaxArrows(int value) {
        ammunition().setMaxArrows(value);
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public int bombCount() {
        return ammunition().bombCount();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setBombCount(int value) {
        ammunition().setBombCount(value);
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public int maxBombs() {
        return ammunition().maxBombs();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setMaxBombs(int value) {
        ammunition().setMaxBombs(value);
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public int magicPowderCount() {
        return ammunition().magicPowderCount();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setMagicPowderCount(int value) {
        ammunition().setMagicPowderCount(value);
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public boolean consumeMagicPowder() {
        return ammunition().consumeMagicPowder();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public int maxMagicPowder() {
        return ammunition().maxMagicPowder();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setMaxMagicPowder(int value) {
        ammunition().setMaxMagicPowder(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int heartPieces() {
        return progression().heartPieces();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setHeartPieces(int value) {
        progression().setHeartPieces(value);
    }
}
