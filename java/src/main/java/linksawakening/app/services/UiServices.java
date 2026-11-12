package linksawakening.app.services;

import java.util.Objects;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.ui.InventoryController;
import linksawakening.ui.InventoryMenu;

/** Long-lived ui services constructed before the first frame. */
public record UiServices(
        InventoryMenu inventoryMenu,
        InventoryController inventoryController,
        DialogController dialogController,
        DialogTextLoader dialogTextLoader,
        OverworldDialogInteraction overworldDialogInteraction,
        CutsceneManager cutsceneManager) {
    public UiServices {
        Objects.requireNonNull(inventoryMenu);
        Objects.requireNonNull(inventoryController);
        Objects.requireNonNull(dialogController);
        Objects.requireNonNull(dialogTextLoader);
        Objects.requireNonNull(overworldDialogInteraction);
        Objects.requireNonNull(cutsceneManager);
    }
}
