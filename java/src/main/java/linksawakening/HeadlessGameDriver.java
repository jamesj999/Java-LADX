package linksawakening;

/**
 * Owns an isolated, window-free game session for input replay and capture.
 * Frames use the same input, simulation and composition paths as the desktop.
 */
public final class HeadlessGameDriver {
    private final GameSession session = new GameSession();

    public void initializeHeadlessNewGame() { session.initializeHeadlessNewGame(); }
    public void headlessButton(String button, boolean pressed) { session.headlessButton(button, pressed); }
    public byte[] headlessFrame() { return session.headlessFrame(); }
    public boolean headlessHasInventoryItem(int item) { return session.headlessHasInventoryItem(item); }
    public String headlessInstrumentTrace() { return session.headlessInstrumentTrace(); }
    public String headlessMoblinKingTrace() { return session.headlessMoblinKingTrace(); }
    public int headlessBowWowState() { return session.headlessBowWowState(); }
    public String headlessCombatTrace() { return session.headlessCombatTrace(); }
    public String headlessEntityPositions() { return session.headlessEntityPositions(); }
    public State headlessState() { return session.headlessState(); }

    public record State(int frame, int map, int room, int x, int y,
                         boolean dialog, boolean pageWait, String text, int shield, int health, int sword, int urchins, boolean toadstool, int powder, int inventoryTick, int tarinX, int tarinY, boolean forestCleared, int tailKey, int bgTick, int smallKeys, int compass, int dungeonMap, int nightmareKey, int animation, int direction, int swordAnimation, int z, int velocityZ, int speedX, int speedY, boolean loading, int maxHearts, int instrument1Flags) {}
}
