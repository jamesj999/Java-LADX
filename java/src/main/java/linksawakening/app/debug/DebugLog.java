package linksawakening.app.debug;

/** Small shared destination for application diagnostic messages. */
public final class DebugLog {
    private static final System.Logger LOGGER = System.getLogger("linksawakening.app");

    private DebugLog() {}

    public static void log(String message) {
        LOGGER.log(System.Logger.Level.INFO, message);
    }
}
