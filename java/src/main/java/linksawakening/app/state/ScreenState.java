package linksawakening.app.state;

import linksawakening.app.screen.Screen;
import linksawakening.ui.FileMenuController;
import linksawakening.ui.FileSaveController;

/** Mutable screens state owned by one session. */
public final class ScreenState {
    public Screen currentScreen;
    public FileMenuController fileMenuController;
    public FileSaveController fileSaveController;
    public int currentSaveSlot = -1;
}
