package linksawakening.entity;

import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;

/** ROM-faithful swimming collaborator for Link. */
final class LinkSwimming {
    private final Link link;
    int swimmingSpeedX;
    int swimmingSpeedY;
    int swimmingFastCountdown;
    int divingCountdown;

    LinkSwimming(Link link) {
        this.link = link;
    }

    boolean enterSwimmingIfNeeded() {
        if (link.romTables == null || link.collision == null) return false;
        int flag = link.collision.objectPhysicsFlagAtPoint(link.pixelX() + 8, link.pixelY() + 12);
        if (flag != 7 && flag != 0x0B) return false;
        if (link.linkStateMachine.slowWalkingSpeed != 0) {
            applyWaterCurrent();
            return true;
        }
        if (link.linkStateMachine.itemUsageContext == 0x80) return false;
        if (link.linkStateMachine.motionState == LinkMotionState.RECOVER
                || link.linkStateMachine.motionState == LinkMotionState.SWIMMING) return true;
        int object = link.collision.objectIdAtPoint(link.pixelX() + 8, link.pixelY() + 12);
        link.pendingWaterSplash = new Link.WaterSplash(link.romEntityX(), (link.romEntityY() - 2) & 0xFF);
        link.soundSink.play(linksawakening.gameplay.GameplaySoundEvent.WATER_SPLASH);
        if (object == 6 || !link.playerHasFlippers()) {
            link.linkStateMachine.recoverCountdown = 0x50;
            link.linkStateMachine.motionState = LinkMotionState.RECOVER;
            link.linkStateMachine.physicsModifier = object;
            link.linkMotion.subY += 2 << Link.SUB_PIXEL_SHIFT;
            link.linkStateMachine.recoveringInWater = true;
            return true;
        }

        link.linkStateMachine.motionState = LinkMotionState.SWIMMING;
        link.linkStateMachine.physicsModifier = 0;
        swimmingFastCountdown = 0;
        divingCountdown = 0;
        link.linkAirborne.airborne = false;
        link.linkAirborne.zSubPixels = 0;
        link.linkAirborne.zVelocity = 0;
        swimmingSpeedX = link.romTables.swimmingEntrySpeedX(
                Direction.fromJava(link.direction).romIndex());
        swimmingSpeedY = link.romTables.swimmingEntrySpeedY(
                Direction.fromJava(link.direction).romIndex());
        link.linkMotion.lastRomSpeedX = swimmingSpeedX & 0xFF;
        link.linkMotion.lastRomSpeedY = swimmingSpeedY & 0xFF;
        return true;
    }

    // label_002_7C50: rapids/current movement bypasses swimming entry.
    private void applyWaterCurrent() {
        int object = link.collision.objectIdAtPoint(link.pixelX() + 8, link.pixelY() + 12);
        int index = object - 0xE7;
        if (object == 0x0E) {
            index = switch (link.linkStateMachine.physicsRoomId) {
                case 0x3E -> 1;
                case 0x3D -> 2;
                case 0x3C, 0x3F -> 3;
                default -> -1;
            };
        }
        if (index < 0) return;
        int x = link.romTables.waterCurrentSpeedX(index);
        int y = link.romTables.waterCurrentSpeedY(index);
        link.linkMotion.lastRomSpeedX = x & 255;
        link.linkMotion.lastRomSpeedY = y & 255;
        if (x != 0) link.linkMotion.tryMoveAxis(x, true);
        if (y != 0) link.linkMotion.tryMoveAxis(y, false);
    }

    void updateRecover() {
        if (link.itemRegistry != null
                && link.itemRegistry.lookup(InventoryItem.SWORD.romValue())
                        instanceof linksawakening.equipment.Sword sword) sword.resetSpinAttack();
        link.linkMotion.lastRomSpeedX = link.linkMotion.lastRomSpeedY = 0;
        link.movingThisFrame = false;
        if (link.linkStateMachine.recoverCountdown > 0) link.linkStateMachine.recoverCountdown--;
        int countdown = link.linkStateMachine.recoverCountdown;
        if (countdown != 0) {
            link.linkAnimator.romAnimationStateOverride = countdown >= 0x40 ? 0x4C : countdown >= 0x30 ? 0x4E : 0xFF;
            if (countdown == 0x40) link.soundSink.play(linksawakening.gameplay.GameplaySoundEvent.LINK_HURT);
            return;
        }
        if (link.playerState != null) {
            if (link.linkStateMachine.physicsModifier == 6) link.playerState.addSubtractHealthBuffer(4);
            link.playerState.setInvincibilityCounter(0x40);
        }
        link.linkStateMachine.recoveringInWater = false;
        link.linkStateMachine.physicsModifier = 0;
        if (!link.collision.usesIndoorPhysicsTable() && link.linkStateMachine.physicsRoomId == 0x2B) {
            link.linkMotion.roomEntrySubX = (0x48 - 8) << Link.SUB_PIXEL_SHIFT;
            link.linkMotion.roomEntrySubY = (0x30 - 16) << Link.SUB_PIXEL_SHIFT;
            link.linkMotion.hasRoomEntryPosition = true;
        }
        if (link.linkMotion.hasRoomEntryPosition) {
            link.linkMotion.subX = link.linkMotion.roomEntrySubX;
            link.linkMotion.subY = link.linkMotion.roomEntrySubY;
        }
        link.linkStateMachine.motionState = LinkMotionState.DEFAULT;
        link.linkAnimator.romAnimationStateOverride = -1;
    }

    void updateSwimming() {
        if (link.collision == null
                || !link.collision.linkOnDeepWater(link.linkMotion.pixelX(), link.linkMotion.pixelY())) {
            leaveSwimming();
            return;
        }

        int mask = link.buildJoypadMask();
        int newDirection = LinkRomConstants.JOYPAD_TO_DIRECTION[mask];
        if (newDirection != -1) {
            link.direction = newDirection;
        }

        if (swimmingFastCountdown > 0) {
            swimmingFastCountdown--;
        }
        if (divingCountdown > 0) {
            divingCountdown--;
            if (divingCountdown == 0) {
                link.linkStateMachine.physicsModifier = 0;
            }
        }

        if (link.buttonWasPressed(link.inputConfig == null ? -1 : link.inputConfig.bKey())) {
            link.linkStateMachine.physicsModifier ^= 1;
            divingCountdown = link.linkStateMachine.physicsModifier == 0 ? 0 : 0xA0;
        }
        if (link.buttonWasPressed(link.inputConfig == null ? -1 : link.inputConfig.aKey())) {
            swimmingFastCountdown = 0x20;
        }

        if (link.linkEquipment.itemsBlockMotion()) {
            link.movingThisFrame = false;
            link.walkTickCounter = 0;
            link.walkFrame = 0;
            link.linkMotion.lastRomSpeedX = 0;
            link.linkMotion.lastRomSpeedY = 0;
            return;
        }

        if (link.linkMotion.forcedSpeedPending) {
            swimmingSpeedX = link.linkMotion.forcedSpeedX;
            swimmingSpeedY = link.linkMotion.forcedSpeedY;
            link.linkMotion.forcedSpeedPending = false;
            link.linkMotion.forcedSpeedXOnly = false;
        } else if ((link.linkStateMachine.frameCounter & 0x01) == 0) {
            boolean fast = swimmingFastCountdown >= 0x10;
            int targetX = link.romTables.swimmingSpeedX(mask, fast);
            int targetY = link.romTables.swimmingSpeedY(mask, fast);
            swimmingSpeedX = approachSignedSpeed(swimmingSpeedX, targetX);
            swimmingSpeedY = approachSignedSpeed(swimmingSpeedY, targetY);
        }

        link.linkMotion.lastRomSpeedX = swimmingSpeedX & 0xFF;
        link.linkMotion.lastRomSpeedY = swimmingSpeedY & 0xFF;
        link.movingThisFrame = swimmingSpeedX != 0 || swimmingSpeedY != 0;
        if (swimmingSpeedX != 0) {
            link.linkMotion.tryMoveAxis(swimmingSpeedX, true);
        }
        if (swimmingSpeedY != 0) {
            link.linkMotion.tryMoveAxis(swimmingSpeedY, false);
        }

        if (!link.collision.linkOnDeepWater(link.linkMotion.pixelX(), link.linkMotion.pixelY())) {
            leaveSwimming();
            return;
        }

        if (link.movingThisFrame) {
            link.walkTickCounter++;
            if (link.walkTickCounter >= LinkRomConstants.WALK_FRAME_TICKS) {
                link.walkTickCounter = 0;
                link.walkFrame ^= 1;
            }
        } else {
            link.walkTickCounter = 0;
            link.walkFrame = 0;
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
        link.linkStateMachine.motionState = LinkMotionState.DEFAULT;
        link.linkStateMachine.physicsModifier = 0;
        swimmingSpeedX = 0;
        swimmingSpeedY = 0;
        swimmingFastCountdown = 0;
        divingCountdown = 0;
        link.walkTickCounter = 0;
        link.walkFrame = 0;
    }
}
