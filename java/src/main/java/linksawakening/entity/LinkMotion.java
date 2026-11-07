package linksawakening.entity;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.PhysicsFlags;
import linksawakening.state.PlayerState;

/** ROM-faithful motion collaborator for Link. */
final class LinkMotion {
    private final Link link;
    int subX;
    int subY;
    int lastRomSpeedX;
    int lastRomSpeedY;
    boolean forcedSpeedPending;
    boolean forcedSpeedXOnly;
    int forcedSpeedX;
    int forcedSpeedY;
    int romCollisionType;
    final int[] collisionProbePixelX = new int[4];
    final int[] collisionProbePixelY = new int[4];

    LinkMotion(Link link) { this.link = link; }

    void tryMoveAxis(int signedSpeed, boolean isXAxis) {
        int candidateSubX = isXAxis ? subX + signedSpeed : subX;
        int candidateSubY = isXAxis ? subY : subY + signedSpeed;

        int candidatePixelX = candidateSubX >> Link.SUB_PIXEL_SHIFT;
        int candidatePixelY = candidateSubY >> Link.SUB_PIXEL_SHIFT;

        int moveDirection;
        if (isXAxis) {
            moveDirection = signedSpeed > 0 ? Link.DIRECTION_RIGHT : Link.DIRECTION_LEFT;
        } else {
            moveDirection = signedSpeed > 0 ? Link.DIRECTION_DOWN : Link.DIRECTION_UP;
        }

        int probeMask = link.collision != null && !link.debugNoClip && !link.directionalLedgeJumpMotionLocked
            ? leadingEdgeCollisionMask(candidatePixelX, candidatePixelY, moveDirection) : 0;
        if (probeMask != 0) {
            // BackgroundCollisionHandler $02:6EC6 / $02:6F01 restores the
            // blocked axis, then Data_002_6E39 shifts the other axis by
            // [0, +1, -1, 0] according to the two leading-edge probes.
            int adjustment = probeMask == 1 ? 1 : probeMask == 2 ? -1 : 0;
            if (isXAxis) subY += adjustment << Link.SUB_PIXEL_SHIFT;
            else subX += adjustment << Link.SUB_PIXEL_SHIFT;
            collisionProbePixelX[moveDirection] = candidatePixelX;
            collisionProbePixelY[moveDirection] = candidatePixelY;
            romCollisionType |= switch (moveDirection) {
                case Link.DIRECTION_UP -> Link.COLLISION_TYPE_UP;
                case Link.DIRECTION_DOWN -> Link.COLLISION_TYPE_DOWN;
                case Link.DIRECTION_LEFT -> Link.COLLISION_TYPE_LEFT;
                case Link.DIRECTION_RIGHT -> Link.COLLISION_TYPE_RIGHT;
                default -> 0;
            };
            return;
        }

        subX = candidateSubX;
        subY = candidateSubY;
    }

    boolean leadingEdgeBlocked(int spriteX, int spriteY, int dir) {
        return leadingEdgeCollisionMask(spriteX, spriteY, dir) != 0;
    }

    int leadingEdgeCollisionMask(int spriteX, int spriteY, int dir) {
        int[] xs = Link.COLLISION_POINTS_X[dir];
        int[] ys = Link.COLLISION_POINTS_Y[dir];
        int blockedMask = 0;
        for (int i = 0; i < xs.length; i++) {
            boolean blocked = false;
            int pointX = spriteX + xs[i];
            int pointY = spriteY + ys[i];
            int physicsFlag = link.collision.objectPhysicsFlagAtPoint(pointX, pointY);
            int objectId = link.collision.objectIdAtPoint(pointX, pointY);
            // $02:716B/$7468: the top six pixels of the revolving doorway.
            if (link.collision.usesIndoorPhysicsTable() && (physicsFlag == 0x7C || physicsFlag == 0x7D)
                && (objectId == 0xB1 || objectId == 0xB2) && (pointY & 15) < 6
                && link.motionState != 0x05) {
                link.motionState = 0x05;
                link.revolvingDoorFrame = 0;
                link.revolvingDoorTransitionPending = false;
                link.revolvingDoorPaletteVariant = -1;
                link.zSubPixels = link.zVelocity = 0;
                link.airborne = false;
                link.linkDamage.collisionIgnoreFramesRemaining = 0;
                if (link.playerState != null) link.playerState.setInvincibilityCounter(0);
                if (link.itemRegistry != null && link.itemRegistry.lookup(PlayerState.INVENTORY_SWORD)
                    instanceof linksawakening.equipment.Sword sword) sword.resetSpinAttack();
                link.soundSink.play(GameplaySoundEvent.REVOLVING_DOOR);
            }
            if (physicsFlag == PhysicsFlags.CAT_LEDGE_OVERWORLD) {
                blocked |= applyOverworldLedgeCollision(pointY);
            } else if (isDirectionalLedgePhysics(physicsFlag)) {
                if (directionalLedgeHalfIsPassable(physicsFlag, pointX)) {
                    link.directionalLedgeCollisionCounter = 0;
                    continue;
                }
                blocked |= applyDirectionalLedgeCollision(physicsFlag, dir);
            } else if (link.collision.pointBlockedForLink(pointX, pointY, link.playerHasFlippers())
                && !link.collision.pointNormalPit(pointX, pointY)) {
                blocked = true;
            } else {
                // ApplyCollisionWithObject reaches label_002_7461 for an
                // ordinary passable probe, which clears wC191.
                link.directionalLedgeCollisionCounter = 0;
            }
            if (blocked) blockedMask |= 1 << i;
        }
        return blockedMask;
    }

    boolean applyDirectionalLedgeCollision(int physicsFlag, int moveDirection) {
        int ledgeDirection = switch (physicsFlag - PhysicsFlags.CAT_LEDGE) {
            case 0 -> Link.DIRECTION_RIGHT;
            case 1 -> Link.DIRECTION_LEFT;
            case 2 -> Link.DIRECTION_UP;
            case 3 -> Link.DIRECTION_DOWN;
            default -> -1;
        };
        if (link.airborne || link.direction != ledgeDirection || moveDirection != ledgeDirection
            || (link.buildJoypadMask() & joypadBitForDirection(ledgeDirection)) == 0) {
            link.directionalLedgeCollisionCounter = 0;
            return true;
        }

        boolean pegasusRunning = link.playerState != null && link.playerState.runningWithPegasusBoots();
        link.directionalLedgeCollisionCounter++;
        if (!pegasusRunning && link.directionalLedgeCollisionCounter < 0x0C) {
            return true;
        }

        link.resetPegasusBoots();
        link.directionalLedgeCollisionCounter = 0;
        link.directionalLedgeJumpMotionLocked = true;
        link.directionalLedgeJumpSpeedX = switch (ledgeDirection) {
            case Link.DIRECTION_RIGHT -> 0x10;
            case Link.DIRECTION_LEFT -> -0x10;
            default -> 0;
        };
        link.directionalLedgeJumpSpeedY = switch (ledgeDirection) {
            case Link.DIRECTION_DOWN -> 0x10;
            case Link.DIRECTION_UP -> -0x10;
            default -> 0;
        };
        lastRomSpeedX = link.directionalLedgeJumpSpeedX & 0xFF;
        lastRomSpeedY = link.directionalLedgeJumpSpeedY & 0xFF;
        link.airborne = true;
        link.zSubPixels = 0;
        link.zVelocity = 0x1C;
        link.jumpAnimationCounter = 0;
        link.jumpAnimationFrame = 0;
        link.soundSink.play(GameplaySoundEvent.LEDGE_FALL);
        return true;
    }

    boolean directionalLedgeHalfIsPassable(int physicsFlag, int pointX) {
        if (link.collision.usesIndoorPhysicsTable()) {
            return false;
        }
        int ledgeDirection = physicsFlag - PhysicsFlags.CAT_LEDGE;
        int xWithinTile = pointX & 0x0F;
        return (ledgeDirection == 0 && xWithinTile < 8)
            || (ledgeDirection == 1 && xWithinTile >= 8);
    }

    boolean applyOverworldLedgeCollision(int pointY) {
        link.directionalLedgeCollisionCounter = 0;
        if (link.roosterCarryActive || link.overworldLedgeFallActive) {
            return true;
        }
        if ((pointY & 0x0F) < 8) {
            return false;
        }

        link.resetPegasusBoots();
        subX = (link.romEntityX() & 0xF0) << Link.SUB_PIXEL_SHIFT;
        subY -= 8 << Link.SUB_PIXEL_SHIFT;
        link.motionState = 0x02;
        link.physicsModifier = 0;
        link.overworldLedgeFallActive = true;
        beginOverworldLedgeUnstuckingMotion();
        link.soundSink.play(GameplaySoundEvent.LEDGE_FALL);
        return true;
    }

    void beginOverworldLedgeUnstuckingMotion() {
        subY += 16 << Link.SUB_PIXEL_SHIFT;
        link.zSubPixels += 16 << Link.SUB_PIXEL_SHIFT;
        for (int step = 0; step < 16; step++) {
            subY += 8 << Link.SUB_PIXEL_SHIFT;
            link.zSubPixels += 8 << Link.SUB_PIXEL_SHIFT;
            if (!ledgeUnstuckingPositionBlocked()) {
                break;
            }
        }
        link.physicsModifier = 1;
        subY -= 3 << Link.SUB_PIXEL_SHIFT;
        link.airborne = true;
        link.zVelocity = 0;
        link.jumpAnimationCounter = 0;
        link.jumpAnimationFrame = 0;
    }

    boolean ledgeUnstuckingPositionBlocked() {
        int spriteX = link.pixelX();
        int spriteY = link.pixelY();
        for (int index = 0; index < 2; index++) {
            int pointX = spriteX + Link.COLLISION_POINTS_X[Link.DIRECTION_UP][index];
            int pointY = spriteY + Link.COLLISION_POINTS_Y[Link.DIRECTION_UP][index];
            if (link.collision.pointBlockedForLink(pointX, pointY, link.playerHasFlippers())) {
                return true;
            }
        }
        return false;
    }

    static boolean isDirectionalLedgePhysics(int physicsFlag) {
        return physicsFlag >= PhysicsFlags.CAT_LEDGE
            && physicsFlag <= PhysicsFlags.CAT_LEDGE + 3;
    }

    static int joypadBitForDirection(int direction) {
        return switch (direction) {
            case Link.DIRECTION_DOWN -> Link.JOY_DOWN;
            case Link.DIRECTION_UP -> Link.JOY_UP;
            case Link.DIRECTION_LEFT -> Link.JOY_LEFT;
            case Link.DIRECTION_RIGHT -> Link.JOY_RIGHT;
            default -> 0;
        };
    }
}
