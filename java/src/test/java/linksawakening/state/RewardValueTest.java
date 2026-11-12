package linksawakening.state;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.world.entity.event.FloatingItemPickupEffect;
import linksawakening.world.room.ChestContentsTable;
import org.junit.jupiter.api.Test;

class RewardValueTest {
    @Test
    void floatingClassificationPreservesEveryByteAndFailure() {
        for (int type : new int[] {0x86, 0xE5, -1, 256}) {
            for (int variant = -1; variant <= 256; variant++) {
                String expected;
                try {
                    expected =
                            FloatingItemPickupEffect.pickupEffect(type, variant).name();
                } catch (IllegalArgumentException failure) {
                    final int checkedVariant = variant;
                    var actual = assertThrows(
                            IllegalArgumentException.class, () -> FloatingPickup.pickupEffect(type, checkedVariant));
                    assertEquals(failure.getMessage(), actual.getMessage());
                    continue;
                }
                assertEquals(
                        expected, FloatingPickup.pickupEffect(type, variant).name());
            }
        }
    }

    @Test
    void chestVariantsRetainWorldContractValues() throws Exception {
        for (var field : ChestItemValues.class.getDeclaredFields()) {
            assertEquals(ChestContentsTable.class.getField(field.getName()).getInt(null), field.getInt(null));
        }
    }
}
