package linksawakening.equipment;

import java.util.Objects;

/**
 * Top-view Roc's Feather behavior from {@code UseRocsFeather}
 * (bank0.asm:$14CB): item use is edge-triggered by the equipment controller,
 * and the item asks Link to start the jump if his current ground state allows it.
 */
public final class RocsFeather implements EquippedItem {

    public interface JumpTarget {
        void useRocsFeather();
    }

    private final JumpTarget target;

    public RocsFeather(JumpTarget target) {
        this.target = Objects.requireNonNull(target);
    }

    @Override
    public void onPress() {
        target.useRocsFeather();
    }
}
