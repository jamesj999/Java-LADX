package linksawakening.app.state;

import linksawakening.app.screen.Screen;
import linksawakening.ui.FileLoadTransition;
import linksawakening.ui.MenuTransition;

/** Mutable screens state owned by one session. */
public final class ScreenState {
    public Screen currentScreen;
    public final FileMenuState fileMenu = new FileMenuState();
    public final FileSaveState fileSave = new FileSaveState();

    public FileLoadTransition fileLoad;
    public MenuTransition menuTransition;
    public int currentSaveSlot = -1;

    public void selectScreen(Screen screen) {
        currentScreen = screen;
    }

    public void selectSaveSlot(int slot) {
        currentSaveSlot = slot;
    }

    public Screen currentScreen() {
        return currentScreen;
    }

    public FileMenuState fileMenu() {
        return fileMenu;
    }

    public FileSaveState fileSave() {
        return fileSave;
    }

    public int currentSaveSlot() {
        return currentSaveSlot;
    }
}
