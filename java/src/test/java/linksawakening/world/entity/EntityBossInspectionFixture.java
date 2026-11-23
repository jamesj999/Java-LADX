package linksawakening.world.entity;

import java.lang.reflect.Field;
import linksawakening.world.entity.motion.RollingBonesMotion;

final class EntityBossInspectionFixture {
    private static final Field GENIE = controllerField(EntityLiftThrowController.class, "genie");
    private static final Field SLIME_EYE = controllerField(EntityCombatResolver.class, "slimeEye");

    private final RoomEntityRuntime runtime;

    static GenieEntityController genie(RoomEntityRuntime runtime) {
        return GenieEntityController.class.cast(
                controller(GENIE, runtime.execution().liftThrow()));
    }

    static SlimeEyeController slimeEye(RoomEntityRuntime runtime) {
        return SlimeEyeController.class.cast(controller(SLIME_EYE, runtime.combat()));
    }

    private static Field controllerField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException failure) {
            throw new AssertionError(failure);
        }
    }

    private static Object controller(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException failure) {
            throw new AssertionError(failure);
        }
    }

    EntityBossInspectionFixture(RoomEntityRuntime room) {
        runtime = room;
    }

    void setSlimeEyeState(int slot, int value) {
        slimeEye(runtime).encounter().state()[slot] = value;
    }

    void setSlimeEyeHalf(int slot, int value) {
        slimeEye(runtime).encounter().half()[slot] = value;
    }

    int geniePrivateState1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return genie(runtime).geniePrivateState1(slot) & 0xFF;
    }

    int genieActiveStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return genie(runtime).genieActiveState(slot) & 0xFF;
    }

    int geniePrivateState3ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return genie(runtime).geniePrivateState3(slot) & 0xFF;
    }

    int geniePrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return genie(runtime).geniePrivateCountdown1(slot) & 0xFF;
    }

    int geniePrivateCountdown2ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return genie(runtime).geniePrivateCountdown2(slot) & 0xFF;
    }

    int genieSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return genie(runtime).genieSpeedX(slot) & 0xFF;
    }

    int genieSpeedZForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return genie(runtime).genieSpeedZ(slot) & 0xFF;
    }

    int geniePrivateState4ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return genie(runtime).geniePrivateState4(slot) & 0xFF;
    }

    int rollingBonesBarStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection()
                .motions()
                .get(RollingBonesMotion.class, RollingBonesMotion::new)
                .barState(slot);
    }

    int rollingBonesBarSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection()
                .motions()
                .get(RollingBonesMotion.class, RollingBonesMotion::new)
                .barSpeedX(slot);
    }
}
