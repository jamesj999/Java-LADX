package linksawakening.equipment;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.state.PlayerState;

/** Port of {@code UseMagicPowder} and {@code SprinkleMagicPowder}. */
public final class MagicPowder implements EquipmentItem {

    public interface SprinkleTarget {
        /** Allocates the ROM's type-$08 sprinkle entity. */
        boolean sprinkleMagicPowder();

        /** Writes the ROM's generic item attack-step countdown ($0E). */
        void startMagicPowderAttackStep();

        /** Mirrors the shared Link attack-step gate before UseMagicPowder. */
        boolean magicPowderAttackStepActive();

        int linkPositionZ();

        void presentToadstool();
    }

    private final PlayerState playerState;
    private final GameplaySoundSink soundSink;
    private final SprinkleTarget target;
    private final BooleanSupplier itemUseAllowed;

    public MagicPowder(PlayerState playerState, GameplaySoundSink soundSink, SprinkleTarget target) {
        this(playerState, soundSink, target, () -> true);
    }

    public MagicPowder(
            PlayerState playerState,
            GameplaySoundSink soundSink,
            SprinkleTarget target,
            BooleanSupplier itemUseAllowed) {
        this.playerState = Objects.requireNonNull(playerState, "playerState");
        this.soundSink = Objects.requireNonNull(soundSink, "soundSink");
        this.target = Objects.requireNonNull(target, "target");
        this.itemUseAllowed = Objects.requireNonNull(itemUseAllowed, "itemUseAllowed");
    }

    @Override
    public void onPress() {
        if (!itemUseAllowed.getAsBoolean() || target.magicPowderAttackStepActive()) {
            return;
        }
        // UseMagicPowder $00:1492-14A6 checks the mushroom before the
        // powder count. Grounded use starts DIALOG_GOT_TOADSTOOL/$2A;
        // ApplyGotItem later shows the item and opens Dialog00E.
        if (playerState.ammunition().hasToadstool()) {
            if (target.linkPositionZ() != 0) return;
            target.presentToadstool();
            return;
        }
        if (playerState.ammunition().magicPowderCount() == 0) {
            soundSink.play(GameplaySoundEvent.WRONG_ANSWER);
            return;
        }

        // UseMagicPowder allocates the entity before SprinkleMagicPowder
        // spends the count. A failed room allocation therefore changes no
        // inventory or Link attack state.
        if (!target.sprinkleMagicPowder()) {
            return;
        }
        if (!playerState.ammunition().consumeMagicPowder()) {
            throw new IllegalStateException("Magic Powder disappeared after a successful spawn");
        }
        target.startMagicPowderAttackStep();
        soundSink.play(GameplaySoundEvent.MAGIC_POWDER);
    }

    @Override
    public boolean blocksMotion() {
        return target.magicPowderAttackStepActive();
    }

    @Override
    public boolean locksFacing() {
        return target.magicPowderAttackStepActive();
    }
}
