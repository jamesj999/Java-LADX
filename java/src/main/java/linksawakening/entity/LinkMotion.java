package linksawakening.entity;

import static linksawakening.entity.Link.*;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.PhysicsFlags;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;

/** Collision probes and ledge motion for ROM BackgroundCollisionHandler ($02:6E45). */
final class LinkMotion {
    private final LinkCollisionState collisionState = new LinkCollisionState();
    private final LinkPosition position;

    LinkCollisionState collisionState() {
        return collisionState;
    }

    LinkPosition position() {
        return position;
    }

    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkAnimator> linkAnimator;
    private final LinkComponentRef<LinkControls> linkControls;
    private final LinkComponentRef<LinkDamage> linkDamage;
    private final LinkComponentRef<LinkEntityEffects> linkEntityEffects;
    private final LinkComponentRef<LinkEquipment> linkEquipment;
    private final LinkComponentRef<LinkGroundMotion> linkGroundMotion;
    private final LinkComponentRef<LinkItemState> linkItemState;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkSideView> linkSideView;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;

    static final class Dependencies {
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkAnimator> linkAnimator;
        LinkComponentRef<LinkControls> linkControls;
        LinkComponentRef<LinkDamage> linkDamage;
        LinkComponentRef<LinkEntityEffects> linkEntityEffects;
        LinkComponentRef<LinkEquipment> linkEquipment;
        LinkComponentRef<LinkGroundMotion> linkGroundMotion;
        LinkComponentRef<LinkItemState> linkItemState;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkSideView> linkSideView;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
    }

    LinkMotion(Dependencies dependencies) {
        linkAirborne = dependencies.linkAirborne;
        linkAnimationState = dependencies.linkAnimationState;
        linkAnimator = dependencies.linkAnimator;
        linkControls = dependencies.linkControls;
        linkDamage = dependencies.linkDamage;
        linkEntityEffects = dependencies.linkEntityEffects;
        linkEquipment = dependencies.linkEquipment;
        linkGroundMotion = dependencies.linkGroundMotion;
        linkItemState = dependencies.linkItemState;
        linkServices = dependencies.linkServices;
        linkSideView = dependencies.linkSideView;
        linkStateMachine = dependencies.linkStateMachine;
        position = new LinkPosition(linkSideView, collisionState);
    }

    static boolean isDirectionalLedgePhysics(int physicsFlag) {
        return physicsFlag >= PhysicsFlags.CAT_LEDGE && physicsFlag <= PhysicsFlags.CAT_LEDGE + 3;
    }

    static int joypadBitForDirection(Direction direction) {
        return switch (direction) {
            case DOWN -> LinkRomConstants.JOY_DOWN;
            case UP -> LinkRomConstants.JOY_UP;
            case LEFT -> LinkRomConstants.JOY_LEFT;
            case RIGHT -> LinkRomConstants.JOY_RIGHT;
            default -> 0;
        };
    }

    void tryMoveAxis(int signedSpeed, boolean isXAxis) {
        int previousSubX = position.subX();
        int previousSubY = position.subY();
        int candidateSubX = isXAxis ? position.subX() + signedSpeed : position.subX();
        int candidateSubY = isXAxis ? position.subY() : position.subY() + signedSpeed;

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
                        && !linkStateMachine.get().status().debugNoClip()
                        && !linkEntityEffects.get().marinBedJumpMotionLocked
                        && !linkAirborne.get().directionalLedgeJumpMotionLocked()
                ? leadingEdgeCollisionMask(candidatePixelX, candidatePixelY, moveDirection)
                : 0;
        if (probeMask != 0) {
            // BackgroundCollisionHandler $02:6EC6 / $02:6F01 restores the
            // blocked axis, then Data_002_6E39 shifts the other axis by
            // [0, +1, -1, 0] according to the two leading-edge probes.
            int adjustment = probeMask == 1 ? 1 : probeMask == 2 ? -1 : 0;
            if (isXAxis) position.subX(previousSubX);
            else position.subY(previousSubY);
            if (isXAxis) position.subY(position.subY() + (adjustment << Link.SUB_PIXEL_SHIFT));
            else position.subX(position.subX() + (adjustment << Link.SUB_PIXEL_SHIFT));
            collisionState.collisionProbePixelX()[moveDirection] = candidatePixelX;
            collisionState.collisionProbePixelY()[moveDirection] = candidatePixelY;
            collisionState.romCollisionType(collisionState.romCollisionType()
                    | (switch (Direction.fromJavaOrUnknown(moveDirection)) {
                        case UP -> LinkRomConstants.COLLISION_TYPE_UP;
                        case DOWN -> LinkRomConstants.COLLISION_TYPE_DOWN;
                        case LEFT -> LinkRomConstants.COLLISION_TYPE_LEFT;
                        case RIGHT -> LinkRomConstants.COLLISION_TYPE_RIGHT;
                        default -> 0;
                    }));
            return;
        }

        position.subX(candidateSubX);
        position.subY(candidateSubY);
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
            int physicsFlag = linkServices.get().collision.objectPhysicsFlagAtPoint(pointX, pointY);
            int objectId = linkServices.get().collision.objectIdAtPoint(pointX, pointY);
            // $02:716B/$7468: the top six pixels of the revolving doorway.
            if (linkServices.get().collision.usesIndoorPhysicsTable()
                    && (physicsFlag == 0x7C || physicsFlag == 0x7D)
                    && (objectId == 0xB1 || objectId == 0xB2)
                    && (pointY & 15) < 6
                    && linkStateMachine.get().status().motionState() != LinkMotionState.REVOLVING_DOOR) {
                linkStateMachine.get().status().motionState(LinkMotionState.REVOLVING_DOOR);
                linkStateMachine.get().door().revolvingDoorFrame(0);
                linkStateMachine.get().door().revolvingDoorTransitionPending(false);
                linkStateMachine.get().door().revolvingDoorPaletteVariant(-1);
                linkAirborne
                        .get()
                        .elevation()
                        .zSubPixels(linkAirborne.get().elevation().zVelocity(0));
                linkAirborne.get().elevation().airborne(false);
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
                    if (!blocked) collisionState.doorWarpPending(true);
                } else {
                    blocked = (pointX & 15) < 6 || (pointX & 15) >= 11;
                }
            } else if (physicsFlag == PhysicsFlags.CAT_LEDGE_OVERWORLD) {
                blocked |= applyOverworldLedgeCollision(pointY);
            } else if (isDirectionalLedgePhysics(physicsFlag)) {
                if (directionalLedgeHalfIsPassable(physicsFlag, pointX)) {
                    linkAirborne.get().directionalLedgeCollisionCounter(0);
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
                linkAirborne.get().directionalLedgeCollisionCounter(0);
            }
            if (blocked) blockedMask |= 1 << i;
        }
        return blockedMask;
    }

    boolean applyDirectionalLedgeCollision(int physicsFlag, int moveDirection) {
        int ledgeDirection =
                Direction.fromRomOrUnknown(physicsFlag - PhysicsFlags.CAT_LEDGE).javaIndex();
        if (linkAirborne.get().elevation().airborne()
                || linkAnimationState.get().direction.javaIndex() != ledgeDirection
                || moveDirection != ledgeDirection
                || (linkControls.get().accessBuildJoypadMask()
                                & joypadBitForDirection(Direction.fromJavaOrUnknown(ledgeDirection)))
                        == 0) {
            linkAirborne.get().directionalLedgeCollisionCounter(0);
            return true;
        }

        boolean pegasusRunning = linkServices.get().playerState != null
                && linkServices.get().playerState.equipmentLevels().runningWithPegasusBoots();
        linkAirborne.get().directionalLedgeCollisionCounter(linkAirborne.get().directionalLedgeCollisionCounter() + 1);
        if (!pegasusRunning && linkAirborne.get().directionalLedgeCollisionCounter() < 0x0C) {
            return true;
        }

        linkStateMachine.get().dash().resetPegasusBoots();
        linkAirborne.get().directionalLedgeCollisionCounter(0);
        linkAirborne.get().directionalLedgeJumpMotionLocked(true);
        linkAirborne
                .get()
                .directionalLedgeJumpSpeedX(
                        switch (Direction.fromJavaOrUnknown(ledgeDirection)) {
                            case RIGHT -> 0x10;
                            case LEFT -> -0x10;
                            default -> 0;
                        });
        linkAirborne
                .get()
                .directionalLedgeJumpSpeedY(
                        switch (Direction.fromJavaOrUnknown(ledgeDirection)) {
                            case DOWN -> 0x10;
                            case UP -> -0x10;
                            default -> 0;
                        });
        position.lastRomSpeedX(linkAirborne.get().directionalLedgeJumpSpeedX() & 0xFF);
        position.lastRomSpeedY(linkAirborne.get().directionalLedgeJumpSpeedY() & 0xFF);
        linkAirborne.get().elevation().airborne(true);
        linkAirborne.get().elevation().zSubPixels(0);
        linkAirborne.get().elevation().zVelocity(0x1C);
        linkAnimator
                .get()
                .ledgeJumpRenderY(position.pixelY() - linkAirborne.get().accessZPixels());
        linkAnimator.get().jumpAnimationCounter(0);
        linkAnimator.get().jumpAnimationFrame(0);
        linkServices.get().soundSink.play(GameplaySoundEvent.LEDGE_FALL);
        return true;
    }

    boolean directionalLedgeHalfIsPassable(int physicsFlag, int pointX) {
        if (linkServices.get().collision.usesIndoorPhysicsTable()) {
            return false;
        }
        int ledgeDirection = physicsFlag - PhysicsFlags.CAT_LEDGE;
        int xWithinTile = pointX & 0x0F;
        return (ledgeDirection == 0 && xWithinTile < 8) || (ledgeDirection == 1 && xWithinTile >= 8);
    }

    boolean applyOverworldLedgeCollision(int pointY) {
        linkAirborne.get().directionalLedgeCollisionCounter(0);
        if (linkEntityEffects.get().roosterCarryActive || linkAirborne.get().overworldLedgeFallActive()) {
            return true;
        }
        if ((pointY & 0x0F) < 8) {
            return false;
        }

        linkStateMachine.get().dash().resetPegasusBoots();
        linkAnimator
                .get()
                .ledgeJumpRenderY(position.pixelY() - linkAirborne.get().accessZPixels());
        position.subX((position.romEntityX() & 0xF0) << Link.SUB_PIXEL_SHIFT);
        position.subY(position.subY() - (8 << Link.SUB_PIXEL_SHIFT));
        linkStateMachine.get().status().motionState(LinkMotionState.UNSTUCKING);
        linkStateMachine.get().roomPhysics().physicsModifier(0);
        linkAirborne.get().overworldLedgeFallActive(true);
        // The collision changes wLinkMotionState only. The dispatcher runs
        // LinkMotionUnstuckingHandler on the following frame ($02:4960).
        linkServices.get().soundSink.play(GameplaySoundEvent.LEDGE_FALL);
        return true;
    }

    /** Ports LinkMotionUnstuckingHandler's initial downward search. */
    void beginOverworldLedgeUnstuckingMotion() {
        position.lastRomSpeedX(0);
        position.lastRomSpeedY(0xFF);
        position.subY(position.subY() + (16 << Link.SUB_PIXEL_SHIFT));
        linkAirborne
                .get()
                .elevation()
                .zSubPixels(linkAirborne.get().elevation().zSubPixels() + (16 << Link.SUB_PIXEL_SHIFT));
        for (int step = 0; step < 16; step++) {
            position.subY(position.subY() + (8 << Link.SUB_PIXEL_SHIFT));
            linkAirborne
                    .get()
                    .elevation()
                    .zSubPixels(linkAirborne.get().elevation().zSubPixels() + (8 << Link.SUB_PIXEL_SHIFT));
            if (!ledgeUnstuckingPositionBlocked()) {
                break;
            }
        }
        linkStateMachine.get().roomPhysics().physicsModifier(1);
        position.subY(position.subY() - (3 << Link.SUB_PIXEL_SHIFT));
        linkAirborne.get().elevation().airborne(true);
        linkAirborne.get().elevation().zVelocity(0);
        linkAnimator.get().jumpAnimationCounter(0);
        linkAnimator.get().jumpAnimationFrame(0);
    }

    boolean ledgeUnstuckingPositionBlocked() {
        int spriteX = position.pixelX();
        int spriteY = position.pixelY();
        boolean blocked = false;
        int lastObject = 0xFF;
        for (int index = 0; index < 2; index++) {
            int pointX = spriteX + LinkRomConstants.COLLISION_POINTS_X[Direction.UP.javaIndex()][index];
            int pointY = spriteY + LinkRomConstants.COLLISION_POINTS_Y[Direction.UP.javaIndex()][index];
            blocked |= linkServices
                    .get()
                    .collision
                    .pointBlockedForLink(pointX, pointY, linkControls.get().accessPlayerHasFlippers());
            lastObject = linkServices.get().collision.objectIdAtPoint(pointX, pointY);
        }
        // LinkMotionUnstuckingHandler ($02:4990-499E) continues over $E1,
        // but explicitly stops over OBJECT_WELL despite its solid physics.
        if (lastObject == 0xE1) return true;
        if (lastObject == 0x61) return false;
        return blocked;
    }

    boolean accessConsumeDoorWarp() {
        boolean pending = collisionState.doorWarpPending();
        collisionState.doorWarpPending(false);
        return pending;
    }

    int accessRomCollisionType() {
        return collisionState.romCollisionType() & 0xFF;
    }

    int accessRomCollisionProbePixelX() {
        return linkAnimationState.get().accessHasFacingCollision()
                ? collisionState
                        .collisionProbePixelX()[
                        linkAnimationState.get().direction.javaIndex()]
                : position.pixelX();
    }

    int accessRomCollisionProbePixelY() {
        return linkAnimationState.get().accessHasFacingCollision()
                ? collisionState
                        .collisionProbePixelY()[
                        linkAnimationState.get().direction.javaIndex()]
                : position.pixelY();
    }

    int romSwordCollisionY() {
        return position.romEntityY() - linkAirborne.get().accessZPixels();
    }
}
