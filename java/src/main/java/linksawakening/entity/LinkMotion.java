package linksawakening.entity;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import linksawakening.equipment.EquipmentItem;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.Tile;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import static linksawakening.entity.Link.*;

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

        int probeMask = link.collision != null && !link.linkStateMachine.debugNoClip && !link.linkAirborne.directionalLedgeJumpMotionLocked
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
                case Link.DIRECTION_UP -> LinkRomConstants.COLLISION_TYPE_UP;
                case Link.DIRECTION_DOWN -> LinkRomConstants.COLLISION_TYPE_DOWN;
                case Link.DIRECTION_LEFT -> LinkRomConstants.COLLISION_TYPE_LEFT;
                case Link.DIRECTION_RIGHT -> LinkRomConstants.COLLISION_TYPE_RIGHT;
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
        int[] xs = LinkRomConstants.COLLISION_POINTS_X[dir];
        int[] ys = LinkRomConstants.COLLISION_POINTS_Y[dir];
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
                && link.linkStateMachine.motionState != 0x05) {
                link.linkStateMachine.motionState = 0x05;
                link.linkStateMachine.revolvingDoorFrame = 0;
                link.linkStateMachine.revolvingDoorTransitionPending = false;
                link.linkStateMachine.revolvingDoorPaletteVariant = -1;
                link.linkAirborne.zSubPixels = link.linkAirborne.zVelocity = 0;
                link.linkAirborne.airborne = false;
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
                    link.linkAirborne.directionalLedgeCollisionCounter = 0;
                    continue;
                }
                blocked |= applyDirectionalLedgeCollision(physicsFlag, dir);
            } else if (link.collision.pointBlockedForLink(pointX, pointY, link.playerHasFlippers())
                && !link.collision.pointNormalPit(pointX, pointY)) {
                blocked = true;
            } else {
                // ApplyCollisionWithObject reaches label_002_7461 for an
                // ordinary passable probe, which clears wC191.
                link.linkAirborne.directionalLedgeCollisionCounter = 0;
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
        if (link.linkAirborne.airborne || link.direction != ledgeDirection || moveDirection != ledgeDirection
            || (link.buildJoypadMask() & joypadBitForDirection(ledgeDirection)) == 0) {
            link.linkAirborne.directionalLedgeCollisionCounter = 0;
            return true;
        }

        boolean pegasusRunning = link.playerState != null && link.playerState.runningWithPegasusBoots();
        link.linkAirborne.directionalLedgeCollisionCounter++;
        if (!pegasusRunning && link.linkAirborne.directionalLedgeCollisionCounter < 0x0C) {
            return true;
        }

        link.linkStateMachine.resetPegasusBoots();
        link.linkAirborne.directionalLedgeCollisionCounter = 0;
        link.linkAirborne.directionalLedgeJumpMotionLocked = true;
        link.linkAirborne.directionalLedgeJumpSpeedX = switch (ledgeDirection) {
            case Link.DIRECTION_RIGHT -> 0x10;
            case Link.DIRECTION_LEFT -> -0x10;
            default -> 0;
        };
        link.linkAirborne.directionalLedgeJumpSpeedY = switch (ledgeDirection) {
            case Link.DIRECTION_DOWN -> 0x10;
            case Link.DIRECTION_UP -> -0x10;
            default -> 0;
        };
        lastRomSpeedX = link.linkAirborne.directionalLedgeJumpSpeedX & 0xFF;
        lastRomSpeedY = link.linkAirborne.directionalLedgeJumpSpeedY & 0xFF;
        link.linkAirborne.airborne = true;
        link.linkAirborne.zSubPixels = 0;
        link.linkAirborne.zVelocity = 0x1C;
        link.linkAnimator.jumpAnimationCounter = 0;
        link.linkAnimator.jumpAnimationFrame = 0;
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
        link.linkAirborne.directionalLedgeCollisionCounter = 0;
        if (link.linkEntityEffects.roosterCarryActive || link.linkAirborne.overworldLedgeFallActive) {
            return true;
        }
        if ((pointY & 0x0F) < 8) {
            return false;
        }

        link.linkStateMachine.resetPegasusBoots();
        subX = (link.romEntityX() & 0xF0) << Link.SUB_PIXEL_SHIFT;
        subY -= 8 << Link.SUB_PIXEL_SHIFT;
        link.linkStateMachine.motionState = 0x02;
        link.linkStateMachine.physicsModifier = 0;
        link.linkAirborne.overworldLedgeFallActive = true;
        beginOverworldLedgeUnstuckingMotion();
        link.soundSink.play(GameplaySoundEvent.LEDGE_FALL);
        return true;
    }

    void beginOverworldLedgeUnstuckingMotion() {
        subY += 16 << Link.SUB_PIXEL_SHIFT;
        link.linkAirborne.zSubPixels += 16 << Link.SUB_PIXEL_SHIFT;
        for (int step = 0; step < 16; step++) {
            subY += 8 << Link.SUB_PIXEL_SHIFT;
            link.linkAirborne.zSubPixels += 8 << Link.SUB_PIXEL_SHIFT;
            if (!ledgeUnstuckingPositionBlocked()) {
                break;
            }
        }
        link.linkStateMachine.physicsModifier = 1;
        subY -= 3 << Link.SUB_PIXEL_SHIFT;
        link.linkAirborne.airborne = true;
        link.linkAirborne.zVelocity = 0;
        link.linkAnimator.jumpAnimationCounter = 0;
        link.linkAnimator.jumpAnimationFrame = 0;
    }

    boolean ledgeUnstuckingPositionBlocked() {
        int spriteX = link.pixelX();
        int spriteY = link.pixelY();
        for (int index = 0; index < 2; index++) {
            int pointX = spriteX + LinkRomConstants.COLLISION_POINTS_X[Link.DIRECTION_UP][index];
            int pointY = spriteY + LinkRomConstants.COLLISION_POINTS_Y[Link.DIRECTION_UP][index];
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
            case Link.DIRECTION_DOWN -> LinkRomConstants.JOY_DOWN;
            case Link.DIRECTION_UP -> LinkRomConstants.JOY_UP;
            case Link.DIRECTION_LEFT -> LinkRomConstants.JOY_LEFT;
            case Link.DIRECTION_RIGHT -> LinkRomConstants.JOY_RIGHT;
            default -> 0;
        };
    }

    int lastSafeSubX;
    int lastSafeSubY;
    boolean hasLastSafePosition;
    int romFinalSubX;
    int romFinalSubY;
    boolean hasRomFinalPosition;
    int roomEntrySubX;
    int roomEntrySubY;
    boolean hasRoomEntryPosition;

    void setPixelPosition(int pixelX, int pixelY) {
        subX = pixelX << Link.SUB_PIXEL_SHIFT;
        subY = pixelY << Link.SUB_PIXEL_SHIFT;
    }

    void setRoomEntryPixelPosition(int pixelX, int pixelY) {
        link.clearSideViewPlatformState();
        setPixelPosition(pixelX, pixelY);
        markRoomEntryPosition();
    }

    void markRoomEntryPosition() {
        roomEntrySubX = subX;
        roomEntrySubY = subY;
        hasRoomEntryPosition = true;
    }

    /** Returns the Java top-left X coordinate recorded for the current room entry. */
    int roomEntryPixelX() {
        return hasRoomEntryPosition ? roomEntrySubX >> Link.SUB_PIXEL_SHIFT : pixelX();
    }

    /** Returns the Java top-left Y coordinate recorded for the current room entry. */
    int roomEntryPixelY() {
        return hasRoomEntryPosition ? roomEntrySubY >> Link.SUB_PIXEL_SHIFT : pixelY();
    }

    /**
     * Returns the source {@code hLinkPositionX} value for the recorded entry.
     * The Game Boy stores Link's OAM origin, which is eight pixels to the right
     * of this host model's top-left sprite coordinate.
     */
    int roomEntryRomPositionX() {
        return roomEntryPixelX() + 0x08;
    }

    /**
     * Returns the source {@code hLinkPositionY} value for the recorded entry.
     * The Game Boy stores the lower OAM origin, sixteen pixels below this host
     * model's top-left sprite coordinate.
     */
    int roomEntryRomPositionY() {
        return roomEntryPixelY() + 0x10;
    }

    /** Applies a source warp/save position (OAM origin) to the host model. */
    void setRoomEntryRomPosition(int romPositionX, int romPositionY) {
        setRoomEntryPixelPosition(romPositionX - 0x08, romPositionY - 0x10);
    }

    /**
     * Applies the next-frame hLinkSpeedX/hLinkSpeedY values written by an
     * entity handler such as the mirror-shield laser reflection path.
     *
     * <p>The ROM stores these as signed bytes in HRAM.  They are consumed by
     * the following Link motion update, after AnimateEntities has finished
     * writing them for the current frame.</p>
     */
    void applyRomSpeed(int speedX, int speedY) {
        if ((speedX & ~0xFF) != 0 || (speedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Link response speeds must be unsigned bytes");
        }
        forcedSpeedX = (byte) speedX;
        forcedSpeedY = (byte) speedY;
        forcedSpeedXOnly = false;
        forcedSpeedPending = true;
    }

    /** Applies only the hLinkSpeedX write emitted by a side-view pot push. */
    void applyRomSpeedX(int speedX) {
        link.validateRomByte(speedX, "Link X speed");
        forcedSpeedX = (byte) speedX;
        // The pot handler leaves hLinkSpeedY untouched; Link computes its
        // ordinary Y input on the next motion link.update.
        forcedSpeedXOnly = true;
        forcedSpeedPending = true;
    }

    /**
     * Applies the speed bytes written by a handler that immediately calls
     * {@code UpdateFinalLinkPosition}, bypassing input and collision probes.
     */
    void applyRomFinalPosition(int speedX, int speedY) {
        if ((speedX & ~0xFF) != 0 || (speedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Link final-position speeds must be unsigned bytes");
        }
        subY += (byte) speedY;
        subX += (byte) speedX;
    }

    /** Captures hLinkPositionX/Y into the ROM's per-frame final-position shadow. */
    void captureRomFinalPosition() {
        romFinalSubX = subX;
        romFinalSubY = subY;
        hasRomFinalPosition = true;
    }

    /** Copies the captured ROM final position back over an entity push. */
    void restoreRomFinalPosition() {
        if (!hasRomFinalPosition) {
            return;
        }
        subX = romFinalSubX;
        subY = romFinalSubY;
    }

    /** Copies only hLinkFinalPositionX back over a side-view pot push. */
    void restoreRomFinalPositionX() {
        if (hasRomFinalPosition) {
            subX = romFinalSubX;
        }
    }

    int pixelX() {
        return subX >> Link.SUB_PIXEL_SHIFT;
    }

    int pixelY() {
        return subY >> Link.SUB_PIXEL_SHIFT;
    }

    /** ROM hLinkPositionX: the first 8x16 OAM column's X origin. */
    int romEntityX() {
        return pixelX() + 0x08;
    }

    /** ROM hLinkPositionY before the separate vertical Z offset is applied. */
    int romEntityY() {
        return pixelY() + 0x10;
    }

    /** ROM hLinkFinalPositionX captured before the current Link update. */
    int romFinalPositionX() {
        return hasRomFinalPosition ? (romFinalSubX >> Link.SUB_PIXEL_SHIFT) + 0x08
            : romEntityX();
    }

    /** Mirrors ClearLinkPositionIncrement for handlers that clear HRAM speed bytes. */
    void clearRomPositionIncrement() {
        forcedSpeedX = 0;
        forcedSpeedY = 0;
        forcedSpeedPending = false;
        forcedSpeedXOnly = false;
        link.linkSideView.sideViewPlatformVerticalSpeedPending = -1;
        lastRomSpeedX = 0;
        lastRomSpeedY = 0;
    }
}
