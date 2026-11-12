package linksawakening.app;

import java.lang.reflect.Field;

public final class SessionTestAccess {
    private SessionTestAccess() {}

    public static void cleanup(GameSession session) {
        session.cleanup();
    }

    public static SessionContext context(GameSession session) {
        return session.context;
    }

    public static Object owner(GameSession session, String name) throws Exception {
        var c = session.context;
        var s = c.services();
        for (Object value : new Object[] {
            session,
            c.lifecycle(),
            c.runtime().saves(),
            c.state().options(),
            c.state().scene(),
            c.state().debug(),
            c.state().screens(),
            s.rom(),
            s.audio(),
            s.input(),
            s.world(),
            s.player(),
            s.ui(),
            s.visual()
        }) {
            try {
                value.getClass().getDeclaredField(name);
                return value;
            } catch (NoSuchFieldException absent) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    public static Field field(GameSession session, String name) throws Exception {
        return owner(session, name).getClass().getDeclaredField(name);
    }
}
