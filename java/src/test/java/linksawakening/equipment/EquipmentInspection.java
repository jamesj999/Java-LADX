package linksawakening.equipment;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/** Private timer inspection for ROM timing assertions, excluded from production API. */
final class EquipmentInspection {
    private EquipmentInspection() {}

    private static VarHandle timer(Class<?> type, String field) {
        try {
            return MethodHandles.privateLookupIn(type, MethodHandles.lookup()).findVarHandle(type, field, int.class);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static final VarHandle TIMER = timer(Shovel.class, "timer");

    static int timer(Shovel item) {
        return (int) TIMER.get(item);
    }

    private static final VarHandle SHOOTINGCOUNTDOWN = timer(Arrow.class, "shootingCountdown");

    static int shootingCountdown(Arrow item) {
        return (int) SHOOTINGCOUNTDOWN.get(item);
    }

    private static final VarHandle ATTACKSTEPCOUNTDOWN = timer(MagicRod.class, "attackStepCountdown");

    static int attackStepCountdown(MagicRod item) {
        return (int) ATTACKSTEPCOUNTDOWN.get(item);
    }
}
