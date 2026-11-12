package linksawakening.app.headless;

/** Public immutable checkpoint contract retained by HeadlessGameDriver. */
public interface HeadlessState {
    int frame();

    int map();

    int room();

    int x();

    int y();

    boolean dialog();

    boolean pageWait();

    String text();

    int shield();

    int health();

    int sword();

    int urchins();

    boolean toadstool();

    int powder();

    int inventoryTick();

    int tarinX();

    int tarinY();

    boolean forestCleared();

    int tailKey();

    int bgTick();

    int smallKeys();

    int compass();

    int dungeonMap();

    int nightmareKey();

    int animation();

    int direction();

    int swordAnimation();

    int z();

    int velocityZ();

    int speedX();

    int speedY();

    boolean loading();

    int maxHearts();

    int instrument1Flags();
}
