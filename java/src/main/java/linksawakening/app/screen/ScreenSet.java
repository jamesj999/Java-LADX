package linksawakening.app.screen;
/** The five lifecycle instances retained by one session. */
public record ScreenSet(
        TitleScreen title,
        CutsceneScreen cutscene,
        FileMenuScreen fileMenu,
        FileSaveScreen fileSave,
        GameplayScreen gameplay) {
    public Screen screen(ScreenKind kind) {
        return switch (kind) {
            case TITLE -> title;
            case CUTSCENE -> cutscene;
            case FILE_MENU -> fileMenu;
            case FILE_SAVE -> fileSave;
            case GAMEPLAY -> gameplay;
        };
    }
}
