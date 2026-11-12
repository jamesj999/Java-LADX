package linksawakening.entity;

import static linksawakening.entity.Link.*;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.PhysicsFlags;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;

final class LinkMotion {
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
    }

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

    int subX;
    int subY;
    int lastRomSpeedX;
    int lastRomSpeedY;
    boolean forcedSpeedPending;
    boolean forcedSpeedXOnly;
    int forcedSpeedX;
    int forcedSpeedY;
    int romCollisionType;
    boolean doorWarpPending;
    final int[] collisionProbePixelX = new int[4];
    final int[] collisionProbePixelY = new int[4];

    void tryMoveAxis(int signedSpeed, boolean isXAxis) {
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
                    && linkStateMachine.get().motionState != LinkMotionState.REVOLVING_DOOR) {
                linkStateMachine.get().motionState = LinkMotionState.REVOLVING_DOOR;
                linkStateMachine.get().revolvingDoorFrame = 0;
                linkStateMachine.get().revolvingDoorTransitionPending = false;
                linkStateMachine.get().revolvingDoorPaletteVariant = -1;
                linkAirborne.get().zSubPixels = linkAirborne.get().zVelocity = 0;
                linkAirborne.get().airborne = false;
                linkDamage.get().collisionIgnoreFramesRemaining = 0;
                if (linkServices.get().playerState != null)
                    linkServices.get().playerState.setInvincibilityCounter(0);
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
                        && linkServices.get().playerState.runningWithPegasusBoots();
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

    boolean applyDirectionalLedgeCollision(int physicsFlag, int moveDirection) {
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
                && linkServices.get().playerState.runningWithPegasusBoots();
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

    boolean directionalLedgeHalfIsPassable(int physicsFlag, int pointX) {
        if (linkServices.get().collision.usesIndoorPhysicsTable()) {
            return false;
        }
        int ledgeDirection = physicsFlag - PhysicsFlags.CAT_LEDGE;
        int xWithinTile = pointX & 0x0F;
        return (ledgeDirection == 0 && xWithinTile < 8) || (ledgeDirection == 1 && xWithinTile >= 8);
    }

    boolean applyOverworldLedgeCollision(int pointY) {
        linkAirborne.get().directionalLedgeCollisionCounter = 0;
        if (linkEntityEffects.get().roosterCarryActive || linkAirborne.get().overworldLedgeFallActive) {
            return true;
        }
        if ((pointY & 0x0F) < 8) {
            return false;
        }

        linkStateMachine.get().resetPegasusBoots();
        linkAnimator.get().ledgeJumpRenderY = pixelY() - linkAirborne.get().accessZPixels();
        subX = (accessRomEntityX() & 0xF0) << Link.SUB_PIXEL_SHIFT;
        subY -= 8 << Link.SUB_PIXEL_SHIFT;
        linkStateMachine.get().motionState = LinkMotionState.UNSTUCKING;
        linkStateMachine.get().physicsModifier = 0;
        linkAirborne.get().overworldLedgeFallActive = true;
        // The collision changes wLinkMotionState only. The dispatcher runs
        // LinkMotionUnstuckingHandler on the following frame ($02:4960).
        linkServices.get().soundSink.play(GameplaySoundEvent.LEDGE_FALL);
        return true;
    }

    /** Ports LinkMotionUnstuckingHandler's initial downward search. */
    void beginOverworldLedgeUnstuckingMotion() {
        lastRomSpeedX = 0;
        lastRomSpeedY = 0xFF;
        subY += 16 << Link.SUB_PIXEL_SHIFT;
        linkAirborne.get().zSubPixels += 16 << Link.SUB_PIXEL_SHIFT;
        for (int step = 0; step < 16; step++) {
            subY += 8 << Link.SUB_PIXEL_SHIFT;
            linkAirborne.get().zSubPixels += 8 << Link.SUB_PIXEL_SHIFT;
            if (!ledgeUnstuckingPositionBlocked()) {
                break;
            }
        }
        linkStateMachine.get().physicsModifier = 1;
        subY -= 3 << Link.SUB_PIXEL_SHIFT;
        linkAirborne.get().airborne = true;
        linkAirborne.get().zVelocity = 0;
        linkAnimator.get().jumpAnimationCounter = 0;
        linkAnimator.get().jumpAnimationFrame = 0;
    }

    boolean ledgeUnstuckingPositionBlocked() {
        int spriteX = accessPixelX();
        int spriteY = accessPixelY();
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

    static boolean isDirectionalLedgePhysics(int physicsFlag) {
        return physicsFlag >= PhysicsFlags.CAT_LEDGE && physicsFlag <= PhysicsFlags.CAT_LEDGE + 3;
    }

    static int joypadBitForDirection(int direction) {
        return switch (Direction.fromJavaOrUnknown(direction)) {
            case DOWN -> LinkRomConstants.JOY_DOWN;
            case UP -> LinkRomConstants.JOY_UP;
            case LEFT -> LinkRomConstants.JOY_LEFT;
            case RIGHT -> LinkRomConstants.JOY_RIGHT;
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
        linkSideView.get().clearSideViewPlatformState();
        setPixelPosition(pixelX, pixelY);
        markRoomEntryPosition();
    }

    void setRoomScrollEntryPixelPosition(int pixelX, int pixelY) {
        linkSideView.get().clearSideViewPlatformState();
        subX += (pixelX - pixelX()) << Link.SUB_PIXEL_SHIFT;
        subY += (pixelY - pixelY()) << Link.SUB_PIXEL_SHIFT;
        markRoomEntryPosition();
    }

    void markRoomEntryPosition() {
        doorWarpPending = false;
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
        Link.validateRomByte(speedX, "Link X speed");
        forcedSpeedX = (byte) speedX;
        // The pot handler leaves hLinkSpeedY untouched; Link computes its
        // ordinary Y input on the next motion linkGroundMotion.get().accessUpdate.
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
        return hasRomFinalPosition ? (romFinalSubX >> Link.SUB_PIXEL_SHIFT) + 0x08 : romEntityX();
    }

    /** Mirrors ClearLinkPositionIncrement for handlers that clear HRAM speed bytes. */
    void clearRomPositionIncrement() {
        forcedSpeedX = 0;
        forcedSpeedY = 0;
        forcedSpeedPending = false;
        forcedSpeedXOnly = false;
        linkSideView.get().sideViewPlatformVerticalSpeedPending = -1;
        lastRomSpeedX = 0;
        lastRomSpeedY = 0;
    }

    boolean accessConsumeDoorWarp() {
        boolean pending = this.doorWarpPending;
        this.doorWarpPending = false;
        return pending;
    }

    int accessRomCollisionType() {
        return this.romCollisionType & 0xFF;
    }

    int accessRomCollisionProbePixelX() {
        return linkAnimationState.get().accessHasFacingCollision()
                ? this.collisionProbePixelX[linkAnimationState.get().direction]
                : this.pixelX();
    }

    int accessRomCollisionProbePixelY() {
        return linkAnimationState.get().accessHasFacingCollision()
                ? this.collisionProbePixelY[linkAnimationState.get().direction]
                : this.pixelY();
    }

    int accessRomSpeedX() {
        return this.lastRomSpeedX & 0xFF;
    }

    int accessRomSpeedY() {
        return this.lastRomSpeedY & 0xFF;
    }

    int accessRomSwordCollisionY() {
        return this.romEntityY() - linkAirborne.get().accessZPixels();
    }

    void accessSetPixelPosition(int pixelX, int pixelY) {
        this.setPixelPosition(pixelX, pixelY);
    }

    void accessSetRoomScrollEntryPixelPosition(int pixelX, int pixelY) {
        this.setRoomScrollEntryPixelPosition(pixelX, pixelY);
    }

    void accessSetRoomEntryPixelPosition(int pixelX, int pixelY) {
        this.setRoomEntryPixelPosition(pixelX, pixelY);
    }

    void accessMarkRoomEntryPosition() {
        this.markRoomEntryPosition();
    }

    int accessRoomEntryPixelX() {
        return this.roomEntryPixelX();
    }

    int accessRoomEntryPixelY() {
        return this.roomEntryPixelY();
    }

    int accessRoomEntryRomPositionX() {
        return this.roomEntryRomPositionX();
    }

    int accessRoomEntryRomPositionY() {
        return this.roomEntryRomPositionY();
    }

    void accessSetRoomEntryRomPosition(int romPositionX, int romPositionY) {
        this.setRoomEntryRomPosition(romPositionX, romPositionY);
    }

    void accessApplyRomSpeed(int speedX, int speedY) {
        this.applyRomSpeed(speedX, speedY);
    }

    void accessApplyRomSpeedX(int speedX) {
        this.applyRomSpeedX(speedX);
    }

    void accessApplyRomFinalPosition(int speedX, int speedY) {
        this.applyRomFinalPosition(speedX, speedY);
    }

    void accessCaptureRomFinalPosition() {
        this.captureRomFinalPosition();
    }

    void accessRestoreRomFinalPosition() {
        this.restoreRomFinalPosition();
    }

    void accessRestoreRomFinalPositionX() {
        this.restoreRomFinalPositionX();
    }

    void accessClearRomPositionIncrement() {
        this.clearRomPositionIncrement();
    }

    int accessPixelX() {
        return this.pixelX();
    }

    int accessPixelY() {
        return this.pixelY();
    }

    int accessRomEntityX() {
        return this.romEntityX();
    }

    int accessRomFinalPositionX() {
        return this.romFinalPositionX();
    }

    int accessRomEntityY() {
        return this.romEntityY();
    }
}
