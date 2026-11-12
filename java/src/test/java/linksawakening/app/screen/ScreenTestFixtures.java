package linksawakening.app.screen;

import linksawakening.app.*;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.input.*;
import linksawakening.save.*;
import linksawakening.ui.FileMenuFrameSnapshot;

/** ROM-free screen instances for boundary/type assertions. Unexpected lifecycle work fails. */
public final class ScreenTestFixtures {
    private ScreenTestFixtures() {}

    public static TitleScreen title() {
        return new TitleScreen(new TitleScreen.Dependencies(
                new CutsceneManager(new DialogController(16), scene -> {}),
                new InputState(),
                new SessionNavigator(new ScreenState())));
    }

    public static GameplayScreen gameplay() {
        return new GameplayScreen(new GameplayScreenDriver() {
            public boolean interceptFrameInput() {
                throw new AssertionError("Unexpected input");
            }

            public void onKey(int key, int action) {
                throw new AssertionError("Unexpected key");
            }

            public void tick() {
                throw new AssertionError("Unexpected tick");
            }

            public FileMenuFrameSnapshot gameOver() {
                return null;
            }
        });
    }

    public static FileMenuScreen fileMenu() {
        var actions = new SaveActions() {
            public void startNewGame() {
                throw new AssertionError("Unexpected new game");
            }

            public void startSavedGame(SaveSlotState saved) {
                throw new AssertionError("Unexpected load");
            }

            public void saveCurrentPlayerState() {
                throw new AssertionError("Unexpected save");
            }

            public void startFileSave() {
                throw new AssertionError("Unexpected menu");
            }

            public void finishGameOver(int option) {
                throw new AssertionError("Unexpected game over");
            }

            public void persistFileMenuMutation(Runnable mutation) {
                throw new AssertionError("Unexpected mutation");
            }
        };
        return new FileMenuScreen(new FileMenuScreen.Dependencies(
                new ScreenState(),
                new InputState(),
                AppConfigInput.config(),
                new SaveState(SaveRamStore.inMemory()),
                actions,
                () -> {}));
    }

    private static final class AppConfigInput {
        static InputConfig config() {
            return linksawakening.config.AppConfig.defaults().inputConfig();
        }
    }
}
