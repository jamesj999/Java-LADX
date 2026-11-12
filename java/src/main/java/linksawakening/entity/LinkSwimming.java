package linksawakening.entity;

import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;

/** ROM-faithful swimming collaborator for Link. */
final class LinkSwimming {
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkAnimator> linkAnimator;
    private final LinkComponentRef<LinkControls> linkControls;
    private final LinkComponentRef<LinkEquipment> linkEquipment;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkSignals> linkSignals;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;

    LinkSwimming(Dependencies dependencies) {
        linkAirborne = dependencies.linkAirborne;
        linkAnimationState = dependencies.linkAnimationState;
        linkAnimator = dependencies.linkAnimator;
        linkControls = dependencies.linkControls;
        linkEquipment = dependencies.linkEquipment;
        linkMotion = dependencies.linkMotion;
        linkServices = dependencies.linkServices;
        linkSignals = dependencies.linkSignals;
        linkStateMachine = dependencies.linkStateMachine;
    }

    static final class Dependencies {
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkAnimator> linkAnimator;
        LinkComponentRef<LinkControls> linkControls;
        LinkComponentRef<LinkEquipment> linkEquipment;
        LinkComponentRef<LinkMotion> linkMotion;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkSignals> linkSignals;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
    }

    int swimmingSpeedX;
    int swimmingSpeedY;
    int swimmingFastCountdown;
    int divingCountdown;

    boolean enterSwimmingIfNeeded() {
        if (linkServices.get().linkMotionTables == null || linkServices.get().collision == null) return false;
        int flag = linkServices
                .get()
                .collision
                .objectPhysicsFlagAtPoint(
                        linkMotion.get().accessPixelX() + 8, linkMotion.get().accessPixelY() + 12);
        if (flag != 7 && flag != 0x0B) return false;
        if (linkStateMachine.get().slowWalkingSpeed != 0) {
            applyWaterCurrent();
            return true;
        }
        if (linkStateMachine.get().itemUsageContext == 0x80) return false;
        if (linkStateMachine.get().motionState == LinkMotionState.RECOVER
                || linkStateMachine.get().motionState == LinkMotionState.SWIMMING) return true;
        int object = linkServices
                .get()
                .collision
                .objectIdAtPoint(
                        linkMotion.get().accessPixelX() + 8, linkMotion.get().accessPixelY() + 12);
        linkSignals.get().pendingWaterSplash = new Link.WaterSplash(
                linkMotion.get().accessRomEntityX(), (linkMotion.get().accessRomEntityY() - 2) & 0xFF);
        linkServices.get().soundSink.play(linksawakening.gameplay.GameplaySoundEvent.WATER_SPLASH);
        if (object == 6 || !linkControls.get().accessPlayerHasFlippers()) {
            linkStateMachine.get().recoverCountdown = 0x50;
            linkStateMachine.get().motionState = LinkMotionState.RECOVER;
            linkStateMachine.get().physicsModifier = object;
            linkMotion.get().subY += 2 << Link.SUB_PIXEL_SHIFT;
            linkStateMachine.get().recoveringInWater = true;
            return true;
        }

        linkStateMachine.get().motionState = LinkMotionState.SWIMMING;
        linkStateMachine.get().physicsModifier = 0;
        swimmingFastCountdown = 0;
        divingCountdown = 0;
        linkAirborne.get().airborne = false;
        linkAirborne.get().zSubPixels = 0;
        linkAirborne.get().zVelocity = 0;
        swimmingSpeedX = linkServices
                .get()
                .linkMotionTables
                .swimmingEntrySpeedX(
                        Direction.fromJava(linkAnimationState.get().direction).romIndex());
        swimmingSpeedY = linkServices
                .get()
                .linkMotionTables
                .swimmingEntrySpeedY(
                        Direction.fromJava(linkAnimationState.get().direction).romIndex());
        linkMotion.get().lastRomSpeedX = swimmingSpeedX & 0xFF;
        linkMotion.get().lastRomSpeedY = swimmingSpeedY & 0xFF;
        return true;
    }

    // label_002_7C50: rapids/current movement bypasses swimming entry.
    private void applyWaterCurrent() {
        int object = linkServices
                .get()
                .collision
                .objectIdAtPoint(
                        linkMotion.get().accessPixelX() + 8, linkMotion.get().accessPixelY() + 12);
        int index = object - 0xE7;
        if (object == 0x0E) {
            index = switch (linkStateMachine.get().physicsRoomId) {
                case 0x3E -> 1;
                case 0x3D -> 2;
                case 0x3C, 0x3F -> 3;
                default -> -1;
            };
        }
        if (index < 0) return;
        int x = linkServices.get().linkMotionTables.waterCurrentSpeedX(index);
        int y = linkServices.get().linkMotionTables.waterCurrentSpeedY(index);
        linkMotion.get().lastRomSpeedX = x & 255;
        linkMotion.get().lastRomSpeedY = y & 255;
        if (x != 0) linkMotion.get().tryMoveAxis(x, true);
        if (y != 0) linkMotion.get().tryMoveAxis(y, false);
    }

    void updateRecover() {
        if (linkServices.get().itemRegistry != null
                && linkServices.get().itemRegistry.lookup(InventoryItem.SWORD.romValue())
                        instanceof linksawakening.equipment.Sword sword) sword.resetSpinAttack();
        linkMotion.get().lastRomSpeedX = linkMotion.get().lastRomSpeedY = 0;
        linkAnimationState.get().movingThisFrame = false;
        if (linkStateMachine.get().recoverCountdown > 0) linkStateMachine.get().recoverCountdown--;
        int countdown = linkStateMachine.get().recoverCountdown;
        if (countdown != 0) {
            linkAnimator.get().romAnimationStateOverride = countdown >= 0x40 ? 0x4C : countdown >= 0x30 ? 0x4E : 0xFF;
            if (countdown == 0x40)
                linkServices.get().soundSink.play(linksawakening.gameplay.GameplaySoundEvent.LINK_HURT);
            return;
        }
        if (linkServices.get().playerState != null) {
            if (linkStateMachine.get().physicsModifier == 6)
                linkServices.get().playerState.vitals().addSubtractHealthBuffer(4);
            linkServices.get().playerState.vitals().setInvincibilityCounter(0x40);
        }
        linkStateMachine.get().recoveringInWater = false;
        linkStateMachine.get().physicsModifier = 0;
        if (!linkServices.get().collision.usesIndoorPhysicsTable() && linkStateMachine.get().physicsRoomId == 0x2B) {
            linkMotion.get().roomEntrySubX = (0x48 - 8) << Link.SUB_PIXEL_SHIFT;
            linkMotion.get().roomEntrySubY = (0x30 - 16) << Link.SUB_PIXEL_SHIFT;
            linkMotion.get().hasRoomEntryPosition = true;
        }
        if (linkMotion.get().hasRoomEntryPosition) {
            linkMotion.get().subX = linkMotion.get().roomEntrySubX;
            linkMotion.get().subY = linkMotion.get().roomEntrySubY;
        }
        linkStateMachine.get().motionState = LinkMotionState.DEFAULT;
        linkAnimator.get().romAnimationStateOverride = -1;
    }

    void updateSwimming() {
        if (linkServices.get().collision == null
                || !linkServices
                        .get()
                        .collision
                        .linkOnDeepWater(
                                linkMotion.get().pixelX(), linkMotion.get().pixelY())) {
            leaveSwimming();
            return;
        }

        int mask = linkControls.get().accessBuildJoypadMask();
        int newDirection = LinkRomConstants.JOYPAD_TO_DIRECTION[mask];
        if (newDirection != -1) {
            linkAnimationState.get().direction = newDirection;
        }

        if (swimmingFastCountdown > 0) {
            swimmingFastCountdown--;
        }
        if (divingCountdown > 0) {
            divingCountdown--;
            if (divingCountdown == 0) {
                linkStateMachine.get().physicsModifier = 0;
            }
        }

        if (linkControls
                .get()
                .accessButtonWasPressed(
                        linkServices.get().inputConfig == null
                                ? -1
                                : linkServices.get().inputConfig.bKey())) {
            linkStateMachine.get().physicsModifier ^= 1;
            divingCountdown = linkStateMachine.get().physicsModifier == 0 ? 0 : 0xA0;
        }
        if (linkControls
                .get()
                .accessButtonWasPressed(
                        linkServices.get().inputConfig == null
                                ? -1
                                : linkServices.get().inputConfig.aKey())) {
            swimmingFastCountdown = 0x20;
        }

        if (linkEquipment.get().itemsBlockMotion()) {
            linkAnimationState.get().movingThisFrame = false;
            linkAnimationState.get().walkTickCounter = 0;
            linkAnimationState.get().walkFrame = 0;
            linkMotion.get().lastRomSpeedX = 0;
            linkMotion.get().lastRomSpeedY = 0;
            return;
        }

        advanceSwimmingMotion(mask);
    }

    private void advanceSwimmingMotion(int mask) {
        if (linkMotion.get().forcedSpeedPending) {
            swimmingSpeedX = linkMotion.get().forcedSpeedX;
            swimmingSpeedY = linkMotion.get().forcedSpeedY;
            linkMotion.get().forcedSpeedPending = false;
            linkMotion.get().forcedSpeedXOnly = false;
        } else if ((linkStateMachine.get().frameCounter & 0x01) == 0) {
            boolean fast = swimmingFastCountdown >= 0x10;
            int targetX = linkServices.get().linkMotionTables.swimmingSpeedX(mask, fast);
            int targetY = linkServices.get().linkMotionTables.swimmingSpeedY(mask, fast);
            swimmingSpeedX = approachSignedSpeed(swimmingSpeedX, targetX);
            swimmingSpeedY = approachSignedSpeed(swimmingSpeedY, targetY);
        }

        linkMotion.get().lastRomSpeedX = swimmingSpeedX & 0xFF;
        linkMotion.get().lastRomSpeedY = swimmingSpeedY & 0xFF;
        linkAnimationState.get().movingThisFrame = swimmingSpeedX != 0 || swimmingSpeedY != 0;
        if (swimmingSpeedX != 0) {
            linkMotion.get().tryMoveAxis(swimmingSpeedX, true);
        }
        if (swimmingSpeedY != 0) {
            linkMotion.get().tryMoveAxis(swimmingSpeedY, false);
        }

        if (!linkServices
                .get()
                .collision
                .linkOnDeepWater(linkMotion.get().pixelX(), linkMotion.get().pixelY())) {
            leaveSwimming();
            return;
        }

        if (linkAnimationState.get().movingThisFrame) {
            linkAnimationState.get().walkTickCounter++;
            if (linkAnimationState.get().walkTickCounter >= LinkRomConstants.WALK_FRAME_TICKS) {
                linkAnimationState.get().walkTickCounter = 0;
                linkAnimationState.get().walkFrame ^= 1;
            }
        } else {
            linkAnimationState.get().walkTickCounter = 0;
            linkAnimationState.get().walkFrame = 0;
        }
    }

    static int approachSignedSpeed(int current, int target) {
        current = (byte) current;
        target = (byte) target;
        int difference = target - current;
        if (difference == 0) {
            return current;
        }
        return (byte) (current + (difference < 0 ? -1 : 1));
    }

    void leaveSwimming() {
        linkStateMachine.get().motionState = LinkMotionState.DEFAULT;
        linkStateMachine.get().physicsModifier = 0;
        swimmingSpeedX = 0;
        swimmingSpeedY = 0;
        swimmingFastCountdown = 0;
        divingCountdown = 0;
        linkAnimationState.get().walkTickCounter = 0;
        linkAnimationState.get().walkFrame = 0;
    }
}
