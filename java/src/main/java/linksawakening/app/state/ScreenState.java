package linksawakening.app.state;

import linksawakening.app.screen.Screen;
import linksawakening.ui.FileMenuController;
import linksawakening.ui.FileSaveController;

/** Mutable screens state owned by one session. */
public final class ScreenState {
    public Screen currentScreen;
    /** Optional menu-operation state, created on file-menu entry rather than with session services. */
    public FileMenuController fileMenuController;
    /** Optional save-menu operation, cleared when returning to gameplay. */
    public FileSaveController fileSaveController;

    public int currentSaveSlot = -1;
}
