package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

final class EntityBossInspectionFixture {
    private final RoomEntityRuntime runtime;

    EntityBossInspectionFixture(RoomEntityRuntime room) {
        runtime = room;
    }

    int geniePrivateState1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie.geniePrivateState1(slot) & 0xFF;
    }

    int genieActiveStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie.genieActiveState(slot) & 0xFF;
    }

    int geniePrivateState3ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie.geniePrivateState3(slot) & 0xFF;
    }

    int geniePrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie.geniePrivateCountdown1(slot) & 0xFF;
    }

    int geniePrivateCountdown2ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie.geniePrivateCountdown2(slot) & 0xFF;
    }

    int genieSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie.genieSpeedX(slot) & 0xFF;
    }

    int genieSpeedZForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie.genieSpeedZ(slot) & 0xFF;
    }

    int geniePrivateState4ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.genie.geniePrivateState4(slot) & 0xFF;
    }

    int rollingBonesBarStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState
                .motions
                .get(RollingBonesMotion.class, RollingBonesMotion::new)
                .barState(slot);
    }

    int rollingBonesBarSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState
                .motions
                .get(RollingBonesMotion.class, RollingBonesMotion::new)
                .barSpeedX(slot);
    }
}
