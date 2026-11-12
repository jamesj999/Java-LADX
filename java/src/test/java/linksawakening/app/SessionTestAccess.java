package linksawakening.app;

import java.lang.reflect.Field;

public final class SessionTestAccess {
    private SessionTestAccess() {}

    public static Object owner(GameSession session, String name) throws Exception {
        try {
            GameSession.class.getDeclaredField(name);
            return session;
        } catch (NoSuchFieldException ignored) {
            try {
                session.context.getClass().getDeclaredField(name);
                return session.context;
            } catch (NoSuchFieldException absent) {
            }
            for (String holder : new String[] {"services", "scene", "debug", "screens"}) {
                Field field = SessionContext.class.getDeclaredField(holder);
                field.setAccessible(true);
                Object value = field.get(session.context);
                try {
                    value.getClass().getDeclaredField(name);
                    return value;
                } catch (NoSuchFieldException absent) {
                }
            }
            throw new NoSuchFieldException(name);
        }
    }

    public static Field field(GameSession session, String name) throws Exception {
        return owner(session, name).getClass().getDeclaredField(name);
    }
}
