package linksawakening.app;

import linksawakening.app.screen.*;

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
