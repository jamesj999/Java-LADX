package linksawakening.app.state;

import linksawakening.app.screen.Screen;

/** Mutable screens state owned by one session. */
public final class ScreenState {
    public Screen currentScreen;
    public final FileMenuState fileMenu = new FileMenuState();
    public final FileSaveState fileSave = new FileSaveState();

    public int currentSaveSlot = -1;

    public void selectScreen(Screen screen) {
        currentScreen = screen;
    }

    public void selectSaveSlot(int slot) {
        currentSaveSlot = slot;
    }
}
