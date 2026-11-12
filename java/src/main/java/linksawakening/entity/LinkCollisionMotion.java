package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkMotion.*;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.PhysicsFlags;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;

/** Link collision motion state and operations used by ROM {@code ApplyLinkMotionState}. */
abstract class LinkCollisionMotion extends LinkPosition {
    protected LinkCollisionMotion(Dependencies dependencies) {
        super(dependencies);
    }

    protected void tryMoveAxis(int signedSpeed, boolean isXAxis) {
        int previousSubX = subX;
        int previousSubY = subY;
        int candidateSubX = isXAxis ? subX + signedSpeed : subX;
        int candidateSubY = isXAxis ? subY : subY + signedSpeed;

        int candidatePixelX = candidateSubX >> Link.SUB_PIXEL_SHIFT;
        int candidatePixelY = candidateSubY >> Link.SUB_PIXEL_SHIFT;

        int moveDirection;
        if (isXAxis) {
            moveDirection = signedSpeed > 0 ? Direction.RIGHT.javaIndex() : Direction.LEFT.javaIndex();
        } else {
            moveDirection = signedSpeed > 0 ? Direction.DOWN.javaIndex() : Direction.UP.javaIndex();
        }

        // MarinLinkAwakeInBedHandler ($05:530E) sets wC10A too:
        // BackgroundCollisionHandler ($02:6E45) skips all probes until landing.
        int probeMask = linkServices.get().collision != null
                        && !linkStateMachine.get().debugNoClip
                        && !linkEntityEffects.get().marinBedJumpMotionLocked
                        && !linkAirborne.get().directionalLedgeJumpMotionLocked
                ? leadingEdgeCollisionMask(candidatePixelX, candidatePixelY, moveDirection)
                : 0;
        if (probeMask != 0) {
            // BackgroundCollisionHandler $02:6EC6 / $02:6F01 restores the
            // blocked axis, then Data_002_6E39 shifts the other axis by
            // [0, +1, -1, 0] according to the two leading-edge probes.
            int adjustment = probeMask == 1 ? 1 : probeMask == 2 ? -1 : 0;
            if (isXAxis) subX = previousSubX;
            else subY = previousSubY;
            if (isXAxis) subY += adjustment << Link.SUB_PIXEL_SHIFT;
            else subX += adjustment << Link.SUB_PIXEL_SHIFT;
            collisionProbePixelX[moveDirection] = candidatePixelX;
            collisionProbePixelY[moveDirection] = candidatePixelY;
            romCollisionType |= switch (Direction.fromJavaOrUnknown(moveDirection)) {
                case UP -> LinkRomConstants.COLLISION_TYPE_UP;
                case DOWN -> LinkRomConstants.COLLISION_TYPE_DOWN;
                case LEFT -> LinkRomConstants.COLLISION_TYPE_LEFT;
                case RIGHT -> LinkRomConstants.COLLISION_TYPE_RIGHT;
                default -> 0;
            };
            return;
        }

        subX = candidateSubX;
        subY = candidateSubY;
    }

    /**
     * Point-based collision check on the leading edge of movement,
     * mirroring {@code LinkCollisionPointsX/Y} (bank2.asm:6302). Tests
     * two specific points on Link's sprite for the given direction; if
     * either is in a blocked cell, the move is rejected. This is what
     * lets Link step up and out of a scrolled-into-a-tree-row
     * situation — his upper-edge collision points are in the clear row
     * above even while his feet still overlap the tree cells below.
     */
    protected boolean leadingEdgeBlocked(int spriteX, int spriteY, int dir) {
        return leadingEdgeCollisionMask(spriteX, spriteY, dir) != 0;
    }

    protected int leadingEdgeCollisionMask(int spriteX, int spriteY, int dir) {
        int[] xs = LinkRomConstants.COLLISION_POINTS_X[dir];
        int[] ys = LinkRomConstants.COLLISION_POINTS_Y[dir];
        int blockedMask = 0;
        for (int i = 0; i < xs.length; i++) {
            boolean blocked = false;
            int pointX = spriteX + xs[i];
            int pointY = spriteY + ys[i];
            int physicsFlag = linkServices.get().collision.objectPhysicsFlagAtPoint(pointX, pointY);
            int objectId = linkServices.get().collision.objectIdAtPoint(pointX, pointY);
            // $02:716B/$7468: the top six pixels of the revolving doorway.
            if (linkServices.get().collision.usesIndoorPhysicsTable()
                    && (physicsFlag == 0x7C || physicsFlag == 0x7D)
                    && (objectId == 0xB1 || objectId == 0xB2)
                    && (pointY & 15) < 6
                    && linkStateMachine.get().motionState != LinkMotionState.REVOLVING_DOOR) {
                linkStateMachine.get().motionState = LinkMotionState.REVOLVING_DOOR;
                linkStateMachine.get().revolvingDoorFrame = 0;
                linkStateMachine.get().revolvingDoorTransitionPending = false;
                linkStateMachine.get().revolvingDoorPaletteVariant = -1;
                linkAirborne.get().zSubPixels = linkAirborne.get().zVelocity = 0;
                linkAirborne.get().airborne = false;
                linkDamage.get().collisionIgnoreFramesRemaining = 0;
                if (linkServices.get().playerState != null)
                    linkServices.get().playerState.vitals().setInvincibilityCounter(0);
                if (linkServices.get().itemRegistry != null
                        && linkServices.get().itemRegistry.lookup(InventoryItem.SWORD.romValue())
                                instanceof linksawakening.equipment.Sword sword) sword.resetSpinAttack();
                linkServices.get().soundSink.play(GameplaySoundEvent.REVOLVING_DOOR);
            }
            if (physicsFlag == PhysicsFlags.DOOR) {
                // ApplyCollisionWithDoor ($02:71BB-723A): only the top six
                // pixels initiate a warp. The rest is a narrow walkable lane.
                if (linkEntityEffects.get().accessIsCarryingLiftedObject()) {
                    blocked = true;
                } else if ((pointY & 15) < 6) {
                    blocked = linkDamage.get().collisionIgnoreFramesRemaining != 0;
                    if (!blocked) doorWarpPending = true;
                } else {
                    blocked = (pointX & 15) < 6 || (pointX & 15) >= 11;
                }
            } else if (physicsFlag == PhysicsFlags.CAT_LEDGE_OVERWORLD) {
                blocked |= applyOverworldLedgeCollision(pointY);
            } else if (isDirectionalLedgePhysics(physicsFlag)) {
                if (directionalLedgeHalfIsPassable(physicsFlag, pointX)) {
                    linkAirborne.get().directionalLedgeCollisionCounter = 0;
                    continue;
                }
                blocked |= applyDirectionalLedgeCollision(physicsFlag, dir);
            } else if (linkServices
                            .get()
                            .collision
                            .pointBlockedForLink(
                                    pointX, pointY, linkControls.get().accessPlayerHasFlippers())
                    && !linkServices.get().collision.pointNormalPit(pointX, pointY)) {
                blocked = true;
                // ApplyCollisionWithSolid $02:72FA-7338: holding the sword
                // follows its poke path; a raised shield suppresses the write.
                var sword = linkServices.get().itemRegistry == null
                        ? null
                        : linkServices.get().itemRegistry.lookup(InventoryItem.SWORD.romValue());
                boolean swordHolding = sword instanceof linksawakening.equipment.Sword blade
                        && blade.state() == linksawakening.equipment.Sword.STATE_HOLDING;
                boolean running = linkServices.get().playerState != null
                        && linkServices.get().playerState.equipmentLevels().runningWithPegasusBoots();
                if (!linkEquipment.get().isUsingShield() && (!swordHolding || running)) {
                    linkItemState.get().accessMarkRomLinkPushing(3);
                }
            } else {
                // ApplyCollisionWithObject reaches label_002_7461 for an
                // ordinary passable probe, which clears wC191.
                linkAirborne.get().directionalLedgeCollisionCounter = 0;
            }
            if (blocked) blockedMask |= 1 << i;
        }
        return blockedMask;
    }

    protected boolean applyDirectionalLedgeCollision(int physicsFlag, int moveDirection) {
        int ledgeDirection =
                Direction.fromRomOrUnknown(physicsFlag - PhysicsFlags.CAT_LEDGE).javaIndex();
        if (linkAirborne.get().airborne
                || linkAnimationState.get().direction != ledgeDirection
                || moveDirection != ledgeDirection
                || (linkControls.get().accessBuildJoypadMask() & joypadBitForDirection(ledgeDirection)) == 0) {
            linkAirborne.get().directionalLedgeCollisionCounter = 0;
            return true;
        }

        boolean pegasusRunning = linkServices.get().playerState != null
                && linkServices.get().playerState.equipmentLevels().runningWithPegasusBoots();
        linkAirborne.get().directionalLedgeCollisionCounter++;
        if (!pegasusRunning && linkAirborne.get().directionalLedgeCollisionCounter < 0x0C) {
            return true;
        }

        linkStateMachine.get().resetPegasusBoots();
        linkAirborne.get().directionalLedgeCollisionCounter = 0;
        linkAirborne.get().directionalLedgeJumpMotionLocked = true;
        linkAirborne.get().directionalLedgeJumpSpeedX = switch (Direction.fromJavaOrUnknown(ledgeDirection)) {
            case RIGHT -> 0x10;
            case LEFT -> -0x10;
            default -> 0;
        };
        linkAirborne.get().directionalLedgeJumpSpeedY = switch (Direction.fromJavaOrUnknown(ledgeDirection)) {
            case DOWN -> 0x10;
            case UP -> -0x10;
            default -> 0;
        };
        lastRomSpeedX = linkAirborne.get().directionalLedgeJumpSpeedX & 0xFF;
        lastRomSpeedY = linkAirborne.get().directionalLedgeJumpSpeedY & 0xFF;
        linkAirborne.get().airborne = true;
        linkAirborne.get().zSubPixels = 0;
        linkAirborne.get().zVelocity = 0x1C;
        linkAnimator.get().ledgeJumpRenderY = pixelY() - linkAirborne.get().accessZPixels();
        linkAnimator.get().jumpAnimationCounter = 0;
        linkAnimator.get().jumpAnimationFrame = 0;
        linkServices.get().soundSink.play(GameplaySoundEvent.LEDGE_FALL);
        return true;
    }

    protected boolean directionalLedgeHalfIsPassable(int physicsFlag, int pointX) {
        if (linkServices.get().collision.usesIndoorPhysicsTable()) {
            return false;
        }
        int ledgeDirection = physicsFlag - PhysicsFlags.CAT_LEDGE;
        int xWithinTile = pointX & 0x0F;
        return (ledgeDirection == 0 && xWithinTile < 8) || (ledgeDirection == 1 && xWithinTile >= 8);
    }
}
