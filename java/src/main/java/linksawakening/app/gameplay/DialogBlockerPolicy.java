package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.app.GameplayInputState;
import linksawakening.dialog.DialogController;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.ui.InventoryController;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Computes the source-shaped dialog gates from current state. */
public final class DialogBlockerPolicy implements DialogBlockers {
    private final InventoryController inventory;
    private final DialogController dialog;
    private final ScrollController scroll;
    private final TransitionController transition;
    private final GameplayInputState input;

    public DialogBlockerPolicy(
            InventoryController inventory,
            DialogController dialog,
            ScrollController scroll,
            TransitionController transition,
            GameplayInputState input) {
        this.inventory = Objects.requireNonNull(inventory);
        this.dialog = Objects.requireNonNull(dialog);
        this.scroll = Objects.requireNonNull(scroll);
        this.transition = Objects.requireNonNull(transition);
        this.input = Objects.requireNonNull(input);
    }

    public OverworldDialogBlockers current() {
        return new OverworldDialogBlockers(
                inventory.shouldBlockOverworldInput(),
                scroll.isActive(),
                transition.isInputBlocked(),
                GameplayDialogInput.blocksGameplay(dialog),
                input.consumed());
    }
}
