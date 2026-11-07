package linksawakening;

import linksawakening.app.GameSession;

/** Desktop entry point. Each launch owns an independent game session. */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        new GameSession().run(args);
    }
}
