package linksawakening.state;

import static linksawakening.state.PlayerState.*;

/** @deprecated Temporary external API migration layer. */
@Deprecated(forRemoval = true)
abstract class PlayerInventoryCompatibility extends PlayerProgressionCompatibility {
    @Deprecated(forRemoval = true)
    PlayerInventoryCompatibility() {
        super();
    }

    /** @deprecated Use progression().tradeSequenceItem(). */
    @Deprecated(forRemoval = true)
    public int tradeSequenceItem() {
        return progression().tradeSequenceItem();
    }

    /** @deprecated Use progression().setTradeSequenceItem(value). */
    @Deprecated(forRemoval = true)
    public void setTradeSequenceItem(int value) {
        progression().setTradeSequenceItem(value);
    }

    /** @deprecated Use progression().medicineCount(). */
    @Deprecated(forRemoval = true)
    public int medicineCount() {
        return progression().medicineCount();
    }

    /** @deprecated Use progression().setMedicineCount(value). */
    @Deprecated(forRemoval = true)
    public void setMedicineCount(int value) {
        progression().setMedicineCount(value);
    }

    /** @deprecated Use progression().tailKeyCount(). */
    @Deprecated(forRemoval = true)
    public int tailKeyCount() {
        return progression().tailKeyCount();
    }

    /** @deprecated Use progression().setTailKeyCount(value). */
    @Deprecated(forRemoval = true)
    public void setTailKeyCount(int value) {
        progression().setTailKeyCount(value);
    }

    /** @deprecated Use progression().anglerKeyCount(). */
    @Deprecated(forRemoval = true)
    public int anglerKeyCount() {
        return progression().anglerKeyCount();
    }

    /** @deprecated Use progression().setAnglerKeyCount(value). */
    @Deprecated(forRemoval = true)
    public void setAnglerKeyCount(int value) {
        progression().setAnglerKeyCount(value);
    }

    /** @deprecated Use progression().faceKeyCount(). */
    @Deprecated(forRemoval = true)
    public int faceKeyCount() {
        return progression().faceKeyCount();
    }

    /** @deprecated Use progression().setFaceKeyCount(value). */
    @Deprecated(forRemoval = true)
    public void setFaceKeyCount(int value) {
        progression().setFaceKeyCount(value);
    }

    /** @deprecated Use progression().birdKeyCount(). */
    @Deprecated(forRemoval = true)
    public int birdKeyCount() {
        return progression().birdKeyCount();
    }

    /** @deprecated Use progression().setBirdKeyCount(value). */
    @Deprecated(forRemoval = true)
    public void setBirdKeyCount(int value) {
        progression().setBirdKeyCount(value);
    }

    /** @deprecated Use progression().goldenLeavesCount(). */
    @Deprecated(forRemoval = true)
    public int goldenLeavesCount() {
        return progression().goldenLeavesCount();
    }

    /** @deprecated Use progression().setGoldenLeavesCount(value). */
    @Deprecated(forRemoval = true)
    public void setGoldenLeavesCount(int value) {
        progression().setGoldenLeavesCount(value);
    }

    /** @deprecated Use progression().chestItemCount(chestItem). */
    @Deprecated(forRemoval = true)
    public int chestItemCount(int chestItem) {
        return progression().chestItemCount(chestItem);
    }

    /** @deprecated Use ammunition().arrowCount(). */
    @Deprecated(forRemoval = true)
    public int arrowCount() {
        return ammunition().arrowCount();
    }

    /** @deprecated Use ammunition().setArrowCount(value). */
    @Deprecated(forRemoval = true)
    public void setArrowCount(int value) {
        ammunition().setArrowCount(value);
    }

    /** @deprecated Use ammunition().maxArrows(). */
    @Deprecated(forRemoval = true)
    public int maxArrows() {
        return ammunition().maxArrows();
    }

    /** @deprecated Use ammunition().setMaxArrows(value). */
    @Deprecated(forRemoval = true)
    public void setMaxArrows(int value) {
        ammunition().setMaxArrows(value);
    }

    /** @deprecated Use ammunition().bombCount(). */
    @Deprecated(forRemoval = true)
    public int bombCount() {
        return ammunition().bombCount();
    }

    /** @deprecated Use ammunition().setBombCount(value). */
    @Deprecated(forRemoval = true)
    public void setBombCount(int value) {
        ammunition().setBombCount(value);
    }

    /** @deprecated Use ammunition().maxBombs(). */
    @Deprecated(forRemoval = true)
    public int maxBombs() {
        return ammunition().maxBombs();
    }

    /** @deprecated Use ammunition().setMaxBombs(value). */
    @Deprecated(forRemoval = true)
    public void setMaxBombs(int value) {
        ammunition().setMaxBombs(value);
    }

    /** @deprecated Use ammunition().magicPowderCount(). */
    @Deprecated(forRemoval = true)
    public int magicPowderCount() {
        return ammunition().magicPowderCount();
    }

    /** @deprecated Use ammunition().setMagicPowderCount(value). */
    @Deprecated(forRemoval = true)
    public void setMagicPowderCount(int value) {
        ammunition().setMagicPowderCount(value);
    }

    /** @deprecated Use ammunition().consumeMagicPowder(). */
    @Deprecated(forRemoval = true)
    public boolean consumeMagicPowder() {
        return ammunition().consumeMagicPowder();
    }

    /** @deprecated Use ammunition().maxMagicPowder(). */
    @Deprecated(forRemoval = true)
    public int maxMagicPowder() {
        return ammunition().maxMagicPowder();
    }

    /** @deprecated Use ammunition().setMaxMagicPowder(value). */
    @Deprecated(forRemoval = true)
    public void setMaxMagicPowder(int value) {
        ammunition().setMaxMagicPowder(value);
    }

    /** @deprecated Use progression().heartPieces(). */
    @Deprecated(forRemoval = true)
    public int heartPieces() {
        return progression().heartPieces();
    }

    /** @deprecated Use progression().setHeartPieces(value). */
    @Deprecated(forRemoval = true)
    public void setHeartPieces(int value) {
        progression().setHeartPieces(value);
    }
}
