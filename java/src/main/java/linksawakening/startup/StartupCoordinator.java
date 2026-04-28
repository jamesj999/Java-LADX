package linksawakening.startup;

import linksawakening.config.AppConfig;

public final class StartupCoordinator {

    public static final int NEW_GAME_START_ROOM_ID = 0x92;

    private StartupCoordinator() {
    }

    public static boolean shouldStartIntroCutscene(AppConfig config) {
        return config.showTitleScreen() && config.playIntroStory();
    }

    public static int gameplayStartRoomId(AppConfig config) {
        if (config.startMode() == AppConfig.StartMode.NORMAL) {
            return NEW_GAME_START_ROOM_ID;
        }
        return config.startLocation().roomId();
    }
}
