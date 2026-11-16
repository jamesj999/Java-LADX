package linksawakening.world.entity;

import linksawakening.world.entity.motion.RollingBonesMotion;

final class EntityBossInspectionFixture {
    private final RoomEntityRuntime runtime;

    EntityBossInspectionFixture(RoomEntityRuntime room) {
        runtime = room;
    }

    void setSlimeEyeState(int slot, int value) {
        runtime.execution().slimeEye().encounter.state[slot] = value;
    }

    void setSlimeEyeHalf(int slot, int value) {
        runtime.execution().slimeEye().encounter.half[slot] = value;
    }

    int geniePrivateState1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().genie().geniePrivateState1(slot) & 0xFF;
    }

    int genieActiveStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().genie().genieActiveState(slot) & 0xFF;
    }

    int geniePrivateState3ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().genie().geniePrivateState3(slot) & 0xFF;
    }

    int geniePrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().genie().geniePrivateCountdown1(slot) & 0xFF;
    }

    int geniePrivateCountdown2ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().genie().geniePrivateCountdown2(slot) & 0xFF;
    }

    int genieSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().genie().genieSpeedX(slot) & 0xFF;
    }

    int genieSpeedZForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().genie().genieSpeedZ(slot) & 0xFF;
    }

    int geniePrivateState4ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().genie().geniePrivateState4(slot) & 0xFF;
    }

    int rollingBonesBarStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection()
                .motions
                .get(RollingBonesMotion.class, RollingBonesMotion::new)
                .barState(slot);
    }

    int rollingBonesBarSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection()
                .motions
                .get(RollingBonesMotion.class, RollingBonesMotion::new)
                .barSpeedX(slot);
    }
}
