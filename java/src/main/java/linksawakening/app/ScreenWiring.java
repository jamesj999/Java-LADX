package linksawakening.app;

import linksawakening.app.screen.*;

/** Builds the five screens after their concrete collaborators have been constructed. */
final class ScreenWiring {
    private ScreenWiring() {}

    static ScreenSet build(
            SessionContext c,
            ScreenNavigator navigator,
            SaveActions saves,
            InputCompletion completion,
            GameplayScreenDriver driver) {
        return new ScreenSet(
                new TitleScreen(new TitleScreen.Dependencies(c.cutsceneManager(), c.inputState(), navigator)),
                new CutsceneScreen(new CutsceneScreen.Dependencies(
                        c.cutsceneManager(),
                        c.inputState(),
                        c.dialogController(),
                        c.inputConfig(),
                        c.dialogSounds(),
                        navigator,
                        c.music())),
                new FileMenuScreen(new FileMenuScreen.Dependencies(
                        c.screens, c.inputState(), c.inputConfig(), c.saveState, saves, completion)),
                new FileSaveScreen(new FileSaveScreen.Dependencies(
                        c.screens, c.inputState(), c.inputConfig(), saves, navigator, completion)),
                new GameplayScreen(driver));
    }
}
