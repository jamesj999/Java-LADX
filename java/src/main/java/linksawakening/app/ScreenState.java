package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.ui.FileMenuController;
import linksawakening.ui.FileSaveController;

/** Mutable screens state owned by one session. */
public final class ScreenState {
    public FileMenuController fileMenuController;
    public FileSaveController fileSaveController;
    public int currentSaveSlot = -1;
}
