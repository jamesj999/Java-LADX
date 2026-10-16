package linksawakening.save;

import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Source: bank1.asm .initNewGame; bank0.asm PlaceBomb; bank20.asm inventory digits. */
final class SaveDecimalRegressionTest {
    @Test
    void writingPlayerStateEncodesRomPackedDecimalFields() {
        PlayerState player = new PlayerState();
        player.initializeNewGame(60, 60, 40);
        player.setArrowCount(29);
        player.setBombCount(37);
        player.setMagicPowderCount(19);
        player.setSeashells(20);
        SaveRamImage image = SaveRamImage.empty();
        image.writePlayerState(0, player);
        byte[] bytes = image.bytes();
        int main = SaveRamLayout.slotOffset(0) + SaveRamLayout.mainOffset();
        assertEquals(0x29, Byte.toUnsignedInt(bytes[main + SaveRamLayout.MAIN_ARROWS_OFFSET]));
        assertEquals(0x37, Byte.toUnsignedInt(bytes[main + SaveRamLayout.MAIN_BOMBS_OFFSET]));
        assertEquals(0x19, Byte.toUnsignedInt(bytes[main + SaveRamLayout.MAIN_MAGIC_POWDER_OFFSET]));
        assertEquals(0x20, Byte.toUnsignedInt(bytes[main + SaveRamLayout.MAIN_SEASHELLS_OFFSET]));
        assertEquals(0x60, Byte.toUnsignedInt(bytes[main + SaveRamLayout.MAIN_MAX_ARROWS_OFFSET]));
        assertEquals(0x60, Byte.toUnsignedInt(bytes[main + SaveRamLayout.MAIN_MAX_BOMBS_OFFSET]));
        assertEquals(0x40, Byte.toUnsignedInt(bytes[main + SaveRamLayout.MAIN_MAX_MAGIC_POWDER_OFFSET]));
    }

    @Test
    void readingOriginalRomSaveDecodesPackedDecimalCountsAndCapacities() {
        byte[] bytes = SaveRamImage.empty().bytes();
        int main = SaveRamLayout.slotOffset(0) + SaveRamLayout.mainOffset();
        bytes[main + SaveRamLayout.MAIN_ARROWS_OFFSET] = 0x29;
        bytes[main + SaveRamLayout.MAIN_BOMBS_OFFSET] = 0x37;
        bytes[main + SaveRamLayout.MAIN_MAGIC_POWDER_OFFSET] = 0x19;
        bytes[main + SaveRamLayout.MAIN_SEASHELLS_OFFSET] = 0x20;
        bytes[main + SaveRamLayout.MAIN_MAX_ARROWS_OFFSET] = 0x60;
        bytes[main + SaveRamLayout.MAIN_MAX_BOMBS_OFFSET] = 0x60;
        bytes[main + SaveRamLayout.MAIN_MAX_MAGIC_POWDER_OFFSET] = 0x40;
        SaveSlotState saved = SaveRamImage.fromBytes(bytes).readSlot(0);
        assertEquals(29, saved.arrowCount());
        assertEquals(37, saved.bombCount());
        assertEquals(19, saved.magicPowderCount());
        assertEquals(20, saved.seashells());
        assertEquals(60, saved.maxArrows());
        assertEquals(60, saved.maxBombs());
        assertEquals(40, saved.maxMagicPowder());
    }

    @Test
    void reloadPreservesWitchPowderAndFloatingArrowsAboveCapacity() {
        PlayerState original = new PlayerState();
        original.initializeNewGame(30, 30, 20);
        original.setMagicPowderCount(10);
        original.applyWitchMagicPowderReward();
        original.setArrowCount(30);
        original.applyFloatingItemPickup(0xE5, 5);
        SaveRamImage image = SaveRamImage.empty();
        image.writePlayerState(0, original);
        PlayerState restored = new PlayerState();
        restored.applySavedGame(image.readSlot(0));
        assertEquals(original.magicPowderCount(), restored.magicPowderCount());
        assertEquals(original.arrowCount(), restored.arrowCount());
    }
}
