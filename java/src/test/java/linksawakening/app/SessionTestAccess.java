package linksawakening.app;

import linksawakening.app.flow.SessionContext;

public final class SessionTestAccess {
    private SessionTestAccess() {}

    public static void step(GameSession session) {
        session.stepWithoutRender();
    }

    public static void cleanup(GameSession session) {
        session.cleanup();
    }

    public static SessionContext context(GameSession session) {
        return session.context;
    }
}
