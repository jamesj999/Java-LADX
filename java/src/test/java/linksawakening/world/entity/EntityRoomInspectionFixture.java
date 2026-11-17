package linksawakening.world.entity;

import linksawakening.world.entity.motion.HinoxMotion;
import linksawakening.world.entity.motion.SideViewPlatformMotion;
import linksawakening.world.entity.motion.TarinRaccoonMotion;
import linksawakening.world.entity.motion.ThreeOfAKindMotion;

final class EntityRoomInspectionFixture {
    private final RoomEntityRuntime runtime;

    EntityRoomInspectionFixture(RoomEntityRuntime room) {
        runtime = room;
    }

    int bombArrowCooldownForTest() {
        return runtime.execution().bombInteractions().state.readBombArrowCooldown();
    }

    boolean isBombArrowForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().bombInteractions().state.readPlayerArrowBombArrow(slot);
    }

    int bombCollisionGraceCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().bombInteractions().state.readBombCollisionGraceCountdown(slot) & 0xFF;
    }

    int swordPickupStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.pickups().state.readSwordPickupState(slot);
    }

    int slowTransitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection().physicsTables.readSlowTransitionCountdown(slot);
    }

    int threeOfAKindStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection()
                .motions
                .get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new)
                .state(slot);
    }

    int transitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection().physicsTables.readEnemyTransitionCountdown(slot);
    }

    int hinoxStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection()
                .motions
                .get(HinoxMotion.class, HinoxMotion::new)
                .state(slot);
    }

    int hinoxSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection()
                .motions
                .get(HinoxMotion.class, HinoxMotion::new)
                .speedX(slot);
    }

    int hinoxSpeedYForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection()
                .motions
                .get(HinoxMotion.class, HinoxMotion::new)
                .speedY(slot);
    }

    int enemyHealthForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection().combatTables.readEnemyHealth(slot);
    }

    int tarinRaccoonStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection()
                .motions
                .get(TarinRaccoonMotion.class, TarinRaccoonMotion::new)
                .state(slot);
    }

    int bombOriginForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().bombInteractions().state.readBombOrigin(slot) & 0xFF;
    }

    int entityRoomStatusForTest() {
        return runtime.inspection().roomVariables.readEntityRoomStatus() & 0xFF;
    }

    int warpTransitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.inspection().warpState.readWarpTransitionCountdown(slot) & 0xFF;
    }

    int owlEventStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().story().state.owlState().readOwlEventState(slot);
    }

    int owlPrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().story().state.owlState().readOwlPrivateCountdown1(slot);
    }

    int sideViewPlatformSpeedYForTest(int slot) {
        return runtime.inspection()
                .motions
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .speedY(slot);
    }

    int sideViewPlatformSpeedXForTest(int slot) {
        return runtime.inspection()
                .motions
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .speedX(slot);
    }

    int sideViewPlatformPrivateState2ForTest(int slot) {
        return runtime.inspection()
                .motions
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .rumbleCountdown(slot);
    }

    int sideViewPlatformPrivateState4ForTest(int slot) {
        return runtime.inspection()
                .motions
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .pressureTicks(slot);
    }

    int droppedItemForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.execution().bossRewards().state.readDroppedItemBySlot(slot);
    }
}
