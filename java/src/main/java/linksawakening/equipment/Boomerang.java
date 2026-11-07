package linksawakening.equipment;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Edge-triggered {@code UseBoomerang} equipment bridge. */
public final class Boomerang implements EquippedItem {

    public interface LaunchTarget {
        boolean fireBoomerang();

        boolean boomerangActive();
    }

    private final LaunchTarget target;
    private final BooleanSupplier itemUseAllowed;

    public Boomerang(LaunchTarget target) {
        this(target, () -> true);
    }

    public Boomerang(LaunchTarget target, BooleanSupplier itemUseAllowed) {
        this.target = Objects.requireNonNull(target, "target");
        this.itemUseAllowed = Objects.requireNonNull(itemUseAllowed, "itemUseAllowed");
    }

    @Override
    public void onPress() {
        if (!itemUseAllowed.getAsBoolean() || target.boomerangActive()) {
            return;
        }
        target.fireBoomerang();
    }

    @Override
    public boolean blocksMotion() {
        return target.boomerangActive();
    }

    @Override
    public boolean locksFacing() {
        return target.boomerangActive();
    }
}
