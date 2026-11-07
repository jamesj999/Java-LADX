package linksawakening.entity;


/** ROM-faithful swimming collaborator for Link. */
final class LinkSwimming {
    private final Link link;
    int swimmingSpeedX;
    int swimmingSpeedY;
    int swimmingFastCountdown;
    int divingCountdown;

    LinkSwimming(Link link) { this.link = link; }

    boolean enterSwimmingIfNeeded() {
        if (!link.playerHasFlippers() || link.romTables == null || link.collision == null
            || !link.collision.linkOnDeepWater(link.pixelX(), link.pixelY())) {
            return false;
        }

        link.linkStateMachine.motionState = Link.LINK_MOTION_SWIMMING;
        link.linkStateMachine.physicsModifier = 0;
        swimmingFastCountdown = 0;
        divingCountdown = 0;
        link.linkAirborne.airborne = false;
        link.linkAirborne.zSubPixels = 0;
        link.linkAirborne.zVelocity = 0;
        swimmingSpeedX = link.romTables.swimmingEntrySpeedX(link.romDirectionForJavaDirection(link.direction));
        swimmingSpeedY = link.romTables.swimmingEntrySpeedY(link.romDirectionForJavaDirection(link.direction));
        link.linkMotion.lastRomSpeedX = swimmingSpeedX & 0xFF;
        link.linkMotion.lastRomSpeedY = swimmingSpeedY & 0xFF;
        return true;
    }

    void updateSwimming() {
        if (link.collision == null || !link.collision.linkOnDeepWater(link.pixelX(), link.pixelY())) {
            leaveSwimming();
            return;
        }

        int mask = link.buildJoypadMask();
        int newDirection = Link.JOYPAD_TO_DIRECTION[mask];
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

        if (link.itemsBlockMotion()) {
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
        } else if ((link.frameCounter & 0x01) == 0) {
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

        if (!link.collision.linkOnDeepWater(link.pixelX(), link.pixelY())) {
            leaveSwimming();
            return;
        }

        if (link.movingThisFrame) {
            link.walkTickCounter++;
            if (link.walkTickCounter >= Link.WALK_FRAME_TICKS) {
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
        link.linkStateMachine.motionState = Link.LINK_MOTION_DEFAULT;
        link.linkStateMachine.physicsModifier = 0;
        swimmingSpeedX = 0;
        swimmingSpeedY = 0;
        swimmingFastCountdown = 0;
        divingCountdown = 0;
        link.walkTickCounter = 0;
        link.walkFrame = 0;
    }
}
