package linksawakening.equipment;

import java.util.Objects;

/**
 * The edge-triggered hookshot item from {@code UseHookshot} and
 * {@code FireHookshot}. The room/entity target owns the ROM projectile slot;
 * this object only exposes the equipment-facing activation and motion gates.
 */
public final class Hookshot implements EquippedItem {

    public interface LaunchTarget {
        boolean fireHookshot();

        boolean hookshotActive();
    }

    private final LaunchTarget target;

    public Hookshot(LaunchTarget target) {
        this.target = Objects.requireNonNull(target, "target");
    }

    @Override
    public void onPress() {
        target.fireHookshot();
    }

    @Override
    public boolean blocksMotion() {
        return target.hookshotActive();
    }

    @Override
    public boolean locksFacing() {
        return target.hookshotActive();
    }
}
