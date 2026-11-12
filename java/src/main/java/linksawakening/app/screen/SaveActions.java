package linksawakening.app.screen;

import linksawakening.save.SaveSlotState;

/** Persistence and room-entry operations requested by menus and recovery. */
public interface SaveActions {
    void startNewGame();

    void startSavedGame(SaveSlotState saved);

    void saveCurrentPlayerState();

    void startFileSave();

    void finishGameOver(int option);

    void persistFileMenuMutation(Runnable mutation);
}
