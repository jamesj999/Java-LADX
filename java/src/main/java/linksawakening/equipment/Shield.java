package linksawakening.equipment;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Port of the pressed-button {@code UseShield} sound boundary. */
public final class Shield implements EquippedItem {
    private final GameplaySoundSink soundSink;
    private final BooleanSupplier itemUseAllowed;

    public Shield(GameplaySoundSink soundSink) {
        this(soundSink, () -> true);
    }

    public Shield(GameplaySoundSink soundSink, BooleanSupplier itemUseAllowed) {
        this.soundSink = Objects.requireNonNull(soundSink, "soundSink");
        this.itemUseAllowed = Objects.requireNonNull(itemUseAllowed, "itemUseAllowed");
    }

    @Override
    public void onPress() {
        if (itemUseAllowed.getAsBoolean()) {
            soundSink.play(GameplaySoundEvent.SHIELD_DRAW);
        }
    }
}
