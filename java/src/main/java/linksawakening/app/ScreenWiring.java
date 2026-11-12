package linksawakening.app;

import linksawakening.app.screen.CutsceneScreen;
import linksawakening.app.screen.FileMenuScreen;
import linksawakening.app.screen.FileSaveScreen;
import linksawakening.app.screen.GameplayScreen;
import linksawakening.app.screen.GameplayScreenDriver;
import linksawakening.app.screen.InputCompletion;
import linksawakening.app.screen.SaveActions;
import linksawakening.app.screen.ScreenNavigator;
import linksawakening.app.screen.ScreenSet;
import linksawakening.app.screen.TitleScreen;

/** Builds the five screens after their concrete collaborators have been constructed. */
final class ScreenWiring {
    private ScreenWiring() {}

    static ScreenSet build(
            ScreenDependencies c,
            ScreenNavigator navigator,
            SaveActions saves,
            InputCompletion completion,
            GameplayScreenDriver driver) {
        return new ScreenSet(
                new TitleScreen(new TitleScreen.Dependencies(
                        c.ui().cutsceneManager(), c.input().inputState(), navigator)),
                new CutsceneScreen(new CutsceneScreen.Dependencies(
                        c.ui().cutsceneManager(),
                        c.input().inputState(),
                        c.ui().dialogController(),
                        c.input().inputConfig(),
                        c.dialogSounds(),
                        navigator,
                        c.music())),
                new FileMenuScreen(new FileMenuScreen.Dependencies(
                        c.screens(),
                        c.input().inputState(),
                        c.input().inputConfig(),
                        c.saveState(),
                        saves,
                        completion)),
                new FileSaveScreen(new FileSaveScreen.Dependencies(
                        c.screens(), c.input().inputState(), c.input().inputConfig(), saves, navigator, completion)),
                new GameplayScreen(driver));
    }
}
