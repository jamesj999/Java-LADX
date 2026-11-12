package linksawakening.app;

/** Small shared destination for application diagnostic messages. */
final class DebugLog {
    private static final System.Logger LOGGER = System.getLogger("linksawakening.app");

    private DebugLog() {}

    static void log(String message) {
        LOGGER.log(System.Logger.Level.INFO, message);
    }
}
