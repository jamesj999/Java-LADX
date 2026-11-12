package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityBossRewards. */
final class BossKeyDropBehavior {
    static void advanceBossKeyDropProducer(EntityBossRewards owner, RoomEntity source, IntSupplier randomByteSupplier) {
        new BossKeyDropStep(owner, source, randomByteSupplier).run();
    }
}
