package linksawakening.world.entity;

/** Entity-owned screen-shake service consumed by room transitions. */
public interface ScreenShake {
    static ScreenShake create() { return new linksawakening.world.entity.motion.ScreenShakeMotion(); }
    void start(int countdown, int phase);
    void tick();
    int countdown();
    int horizontal();
    int vertical();
}
