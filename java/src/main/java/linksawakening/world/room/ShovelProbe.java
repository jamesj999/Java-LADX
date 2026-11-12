package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

record ShovelProbe(boolean valid, int location, int objectLeft, int objectTop) {
    static ShovelProbe invalid() {
        return new ShovelProbe(false, -1, -1, -1);
    }
}
