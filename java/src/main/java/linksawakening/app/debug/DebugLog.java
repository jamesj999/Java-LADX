package linksawakening.app.debug;

/**
 * Small shared destination for application diagnostic messages. Stateless utility; no session state is
 * retained.
 */
public final class DebugLog {
    private static final System.Logger LOGGER = System.getLogger("linksawakening.app");

    private DebugLog() {}

    public static void log(String message) {
        LOGGER.log(System.Logger.Level.INFO, message);
    }

    public static void warning(String message) {
        LOGGER.log(System.Logger.Level.WARNING, message);
    }

    public static void failure(String message, Throwable failure) {
        LOGGER.log(System.Logger.Level.WARNING, message, failure);
    }
}
