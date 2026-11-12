package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

final class EntityRoomInspectionFixture {
    private final RoomEntityRuntime runtime;

    EntityRoomInspectionFixture(RoomEntityRuntime room) {
        runtime = room;
    }

    int bombArrowCooldownForTest() {
        return runtime.bombInteractions.readBombArrowCooldown();
    }

    boolean isBombArrowForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bombInteractions.readPlayerArrowBombArrow(slot);
    }

    int bombPrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bombInteractions.readBombPrivateCountdown1(slot) & 0xFF;
    }

    int swordPickupStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.rewards.readSwordPickupState(slot);
    }

    int slowTransitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.physicsTables.readSlowTransitionCountdown(slot);
    }

    int threeOfAKindStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState
                .motions
                .get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new)
                .state(slot);
    }

    int transitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.physicsTables.readEnemyTransitionCountdown(slot);
    }

    int hinoxStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState
                .motions
                .get(HinoxMotion.class, HinoxMotion::new)
                .state(slot);
    }

    int hinoxSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState
                .motions
                .get(HinoxMotion.class, HinoxMotion::new)
                .speedX(slot);
    }

    int hinoxSpeedYForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState
                .motions
                .get(HinoxMotion.class, HinoxMotion::new)
                .speedY(slot);
    }

    int enemyHealthForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.combatTables.readEnemyHealth(slot);
    }

    int tarinRaccoonStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState
                .motions
                .get(TarinRaccoonMotion.class, TarinRaccoonMotion::new)
                .state(slot);
    }

    int bombPrivateState4ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bombInteractions.readBombPrivateState4(slot) & 0xFF;
    }

    int entityRoomStatusForTest() {
        return runtime.entityState.roomVariables.readEntityRoomStatus() & 0xFF;
    }

    int warpTransitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.warpState.readWarpTransitionCountdown(slot) & 0xFF;
    }

    int owlEventStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.story.readOwlEventState(slot);
    }

    int owlPrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.story.readOwlPrivateCountdown1(slot);
    }

    int sideViewPlatformSpeedYForTest(int slot) {
        return runtime.entityState
                .motions
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .speedY(slot);
    }

    int sideViewPlatformSpeedXForTest(int slot) {
        return runtime.entityState
                .motions
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .speedX(slot);
    }

    int sideViewPlatformPrivateState2ForTest(int slot) {
        return runtime.entityState
                .motions
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .privateState2(slot);
    }

    int sideViewPlatformPrivateState4ForTest(int slot) {
        return runtime.entityState
                .motions
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .privateState4(slot);
    }

    int droppedItemForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bossRewards.readDroppedItemBySlot(slot);
    }
}
