package linksawakening.equipment;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Port of {@code UseShovel} and the Link-side shovel timer in bank $02. */
public final class Shovel implements EquippedItem {

    public static final int ACTIVE_TIMER_END = 0x18;

    /** The result of the ROM's adjacent-object probe at the moment of use. */
    public record StartResult(boolean started, boolean poking) {
    }

    public interface DigTarget {
        /** Starts the source shovel state and returns whether the probe was a poke. */
        StartResult startShovel();

        /** Applies the source Link-side shovel update for the supplied timer value. */
        void advanceShovel(int timer);

        /** Resolves the ROM Link animation state for the current shovel timer. */
        int shovelAnimationState(int javaDirection, int timer);
    }

    private final GameplaySoundSink soundSink;
    private final DigTarget target;
    private final BooleanSupplier itemUseAllowed;
    private boolean active;
    private int timer;

    public Shovel(GameplaySoundSink soundSink, DigTarget target) {
        this(soundSink, target, () -> true);
    }

    public Shovel(GameplaySoundSink soundSink, DigTarget target,
                  BooleanSupplier itemUseAllowed) {
        this.soundSink = Objects.requireNonNull(soundSink, "soundSink");
        this.target = Objects.requireNonNull(target, "target");
        this.itemUseAllowed = Objects.requireNonNull(itemUseAllowed, "itemUseAllowed");
    }

    @Override
    public void onPress() {
        if (!itemUseAllowed.getAsBoolean() || active) {
            return;
        }
        StartResult result = Objects.requireNonNull(target.startShovel(), "startResult");
        if (!result.started()) {
            return;
        }
        active = true;
        timer = 0;
        soundSink.play(result.poking()
            ? GameplaySoundEvent.SWORD_POKE : GameplaySoundEvent.SHOVEL_DIG);
    }

    @Override
    public void tick(boolean buttonHeld, int frameCounter) {
        if (!active) {
            return;
        }
        timer = (timer + 1) & 0xFF;
        target.advanceShovel(timer);
        if (timer == ACTIVE_TIMER_END) {
            active = false;
        }
    }

    @Override
    public boolean blocksMotion() {
        return active;
    }

    @Override
    public boolean locksFacing() {
        return active;
    }

    @Override
    public int overrideAnimationState(int direction, int walkFrame) {
        return active ? target.shovelAnimationState(direction, timer) : -1;
    }

    int timerForTest() {
        return timer;
    }
}
