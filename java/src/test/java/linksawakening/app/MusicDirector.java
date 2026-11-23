package linksawakening.app;

/** Compatibility names for tests outside the app ownership boundary. */
public final class MusicDirector {
    private MusicDirector() {}

    public static int introCutsceneMusicTrack() {
        return linksawakening.app.audio.MusicDirector.introCutsceneMusicTrack();
    }

    public static int naturalIntroTitleMusicTrack() {
        return linksawakening.app.audio.MusicDirector.naturalIntroTitleMusicTrack();
    }

    public static int newGameHouseMusicTrack() {
        return linksawakening.app.audio.MusicDirector.newGameHouseMusicTrack();
    }

    public static int directTitleScreenMusicTrack() {
        return linksawakening.app.audio.MusicDirector.directTitleScreenMusicTrack();
    }

    public static int fileSelectionMusicTrack() {
        return linksawakening.app.audio.MusicDirector.fileSelectionMusicTrack();
    }
}
