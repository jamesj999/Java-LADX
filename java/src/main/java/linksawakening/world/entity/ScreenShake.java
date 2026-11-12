package linksawakening.world.entity;

import linksawakening.world.entity.motion.ScreenShakeMotion;

/** Entity-owned screen-shake service consumed by room transitions. */
public interface ScreenShake {
    static ScreenShake create() {
        return new ScreenShakeMotion();
    }

    void start(int countdown, int phase);

    void tick();

    int countdown();

    int horizontal();

    int vertical();
}
