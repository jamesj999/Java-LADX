package linksawakening.entity;

import linksawakening.equipment.EquipmentItem;
import linksawakening.gpu.Tile;
import linksawakening.gpu.Framebuffer;
import linksawakening.state.PlayerState;
import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

/** ROM pose selection and sprite data for both rendering paths. */
final class LinkAnimator {
    private final Link link;
    LinkAnimator(Link link) { this.link = link; }

    static final int[][] ANIMATION_STATE = {
        { 0x00, 0x01 },  // DOWN:  standing, walking
        { 0x04, 0x05 },  // UP
        { 0x06, 0x07 },  // LEFT
        { 0x0A, 0x0B },  // RIGHT
    };

    static final int[][] SHIELD_USE_ANIMATION_STATE = {
        { 0x24, 0x25 },  // DOWN
        { 0x30, 0x31 },  // UP
        { 0x28, 0x29 },  // LEFT
        { 0x2A, 0x2B },  // RIGHT
    };

    static final int[][] MIRROR_SHIELD_USE_ANIMATION_STATE = {
        { 0x26, 0x27 },  // DOWN
        { 0x32, 0x33 },  // UP
        { 0x28, 0x29 },  // LEFT
        { 0x2E, 0x2F },  // RIGHT
    };

    static final int[][] LIFTING_ANIMATION_STATE = {
        { 0x44, 0x45 },  // DOWN
        { 0x42, 0x43 },  // UP
        { 0x40, 0x41 },  // LEFT
        { 0x3E, 0x3F },  // RIGHT
    };

    static final int[][] SWIMMING_ANIMATION_STATE = {
        { 0x4C, 0x4D },  // DOWN
        { 0x4A, 0x4B },  // UP
        { 0x48, 0x49 },  // LEFT
        { 0x46, 0x47 },  // RIGHT
    };

    static final int[] DIVING_ANIMATION_STATE = { 0x4E, 0x4F };

    static final int[][] JUMP_ANIMATION_STATE = {
        { 0x64, 0x65, 0x66 }, // DOWN
        { 0x67, 0x68, 0x69 }, // UP
        { 0x5E, 0x5F, 0x60 }, // LEFT
        { 0x61, 0x62, 0x63 }, // RIGHT
    };

    static final int[] FALL_ANIMATION_STATE = { 0x55, 0x56, 0x57, 0x57 };

    final Tile[] composedTiles = new Tile[4];
    boolean tarinShieldPresentation;
    int[] tarinShieldPalette;
    int romAnimationStateOverride = -1;
    int swordAcquisitionBaseRomDirection = -1;
    int swordAcquisitionAnimationState = -1;
    int marinWakeUpBedVariant = -1;
    int bedJumpRenderZ = -1;
    /** DrawLinkSprite uses wC145, cached before ledge motion ($00:1033). */
    int ledgeJumpRenderY = Integer.MIN_VALUE;
    int jumpAnimationCounter;
    int jumpAnimationFrame;

    void render(byte[] displayBuffer, int offsetX, int offsetY) {
        if (link.linkEntityEffects.likeLikeCaptured) {
            return;
        }
        int animationState = resolveAnimationState();
        if (animationState == 0xFF) return;
        link.spriteSheet.resolveTiles(animationState, composedTiles);

        boolean leftFlipX = link.spriteSheet.leftHalfFlipX(animationState);
        boolean leftFlipY = link.spriteSheet.leftHalfFlipY(animationState);
        boolean rightFlipX = link.spriteSheet.rightHalfFlipX(animationState);
        boolean rightFlipY = link.spriteSheet.rightHalfFlipY(animationState);

        int originX = link.linkMotion.pixelX() + offsetX;
        int renderY = renderY();
        int originY = renderY + offsetY;
        int[] palette = link.playerState != null
            && (link.playerState.invincibilityCounter() & 0x04) != 0
            // DrawLinkSprite's GBC path selects object palette 4 from bit 2
            // of wInvincibilityCounter while Link is flashing.
            ? link.tunicPalette.forObjectPalette(4)
            : link.tunicPalette.forTunic(
                link.playerState == null ? PlayerState.TUNIC_GREEN : link.playerState.tunicType());

        if (link.linkStateMachine.revolvingDoorPaletteVariant >= 0 && link.linkStateMachine.revolvingDoorFrame < 0x38) {
            palette = link.romTables.revolvingDoorPalette(link.linkStateMachine.revolvingDoorPaletteVariant);
        }

        // Column-major tile layout: [0]=UL, [1]=LL, [2]=UR, [3]=LR.
        // GB 8x16 flipY swaps the two stacked tiles inside a column and flips
        // each vertically.
        Tile leftTop = composedTiles[leftFlipY ? 1 : 0];
        Tile leftBottom = composedTiles[leftFlipY ? 0 : 1];
        Tile rightTop = composedTiles[rightFlipY ? 3 : 2];
        Tile rightBottom = composedTiles[rightFlipY ? 2 : 3];

        drawTile(displayBuffer, leftTop,     originX,     originY,     leftFlipX,  leftFlipY,
            palette);
        drawTile(displayBuffer, leftBottom,  originX,     originY + 8, leftFlipX,  leftFlipY,
            palette);
        drawTile(displayBuffer, rightTop,    originX + 8, originY,     rightFlipX, rightFlipY,
            palette);
        drawTile(displayBuffer, rightBottom, originX + 8, originY + 8, rightFlipX, rightFlipY,
            palette);

        // Let equipped items paint their own sprites on top (e.g. the sword
        // blade during a swing). Matches the disassembly's ordering where
        // DrawLinkSprite writes Link's body to wLinkOAMBuffer slots 1-2 and
        // ApplyLinkMotionState/func_020_4AB3 writes the sword into slots 4-5.
        renderEquippedItems(displayBuffer, offsetX, offsetY);
    }

    void renderEquippedItems(byte[] displayBuffer, int offsetX, int offsetY) {
        if (link.itemRegistry == null || link.playerState == null) {
            return;
        }
        EquipmentItem a = link.itemRegistry.lookup(link.playerState.itemA());
        if (a != null) {
            a.render(displayBuffer, link.linkMotion.pixelX(), link.linkMotion.pixelY() - link.zPixels(), link.direction, offsetX, offsetY);
        }
        EquipmentItem b = link.itemRegistry.lookup(link.playerState.itemB());
        if (b != null && b != a) {
            b.render(displayBuffer, link.linkMotion.pixelX(), link.linkMotion.pixelY() - link.zPixels(), link.direction, offsetX, offsetY);
        }
        if (swordAcquisitionAnimationState >= 0) {
            // ApplyLinkMotionState draws the sword during the pickup spin
            // before GiveInventoryItem awards it to slot A/B. Reuse the ROM
            // sword handler from the registry, but do not draw it twice if a
            // save already has the sword equipped.
            EquipmentItem acquisitionSword = link.itemRegistry.lookup(PlayerState.INVENTORY_SWORD);
            if (acquisitionSword != null && acquisitionSword != a && acquisitionSword != b) {
                acquisitionSword.render(displayBuffer, link.linkMotion.pixelX(), link.linkMotion.pixelY() - link.zPixels(),
                    link.direction, offsetX, offsetY);
            }
        }
    }

    static void drawTile(byte[] buffer, Tile tile, int screenX, int screenY,
                                 boolean flipX, boolean flipY, int[] palette) {
        if (tile == null) {
            return;
        }
        for (int ty = 0; ty < 8; ty++) {
            for (int tx = 0; tx < 8; tx++) {
                int px = screenX + tx;
                int py = screenY + ty;
                if (px < 0 || px >= Framebuffer.WIDTH || py < 0 || py >= Framebuffer.HEIGHT) {
                    continue;
                }
                int sx = flipX ? 7 - tx : tx;
                int sy = flipY ? 7 - ty : ty;
                int colorIndex = tile.getPixel(sx, sy);
                if (colorIndex == 0) {
                    continue; // transparent
                }
                int color = palette[colorIndex];
                int offset = (py * Framebuffer.WIDTH + px) * 4;
                buffer[offset] = (byte) ((color >> 16) & 0xFF);
                buffer[offset + 1] = (byte) ((color >> 8) & 0xFF);
                buffer[offset + 2] = (byte) (color & 0xFF);
                buffer[offset + 3] = (byte) 0xFF;
            }
        }
    }

    int resolveAnimationState() {
        if (romAnimationStateOverride >= 0) {
            return romAnimationStateOverride;
        }
        if (swordAcquisitionAnimationState >= 0) {
            return swordAcquisitionAnimationState;
        }
        if (link.linkAirborne.fallingIntoPit) {
            int frame = Math.min(LinkAnimator.FALL_ANIMATION_STATE.length - 1, link.linkAirborne.fallingFrameCounter / LinkRomConstants.FALL_FRAME_TICKS);
            return LinkAnimator.FALL_ANIMATION_STATE[frame];
        }

        if (link.linkStateMachine.motionState == Link.LINK_MOTION_SWIMMING) {
            return link.linkStateMachine.physicsModifier != 0
                ? LinkAnimator.DIVING_ANIMATION_STATE[link.walkFrame]
                : LinkAnimator.SWIMMING_ANIMATION_STATE[link.direction][link.walkFrame];
        }

        if (link.linkEntityEffects.grabbingObject) {
            return 0x36 + 2 * Link.romDirectionForJavaDirection(link.direction)
                + (link.linkEntityEffects.pullingObject ? 1 : 0);
        }
        // func_002_4338 copies the intermediate ROM carry animation state
        // directly (for example $37/$39/$3B/$3D). The final carry state is
        // the regular two-frame lifting list below.
        if (link.linkEntityEffects.carryingLiftedObjectState >= 2) {
            return link.linkEntityEffects.carryingLiftedObjectState;
        }
        // ApplyLinkGroundMotion ($02:4503) sets the step counter to $FF
        // while link.linkAirborne.airborne, selecting the alternate normal pose after the flip.
        int normalFrame = link.linkAirborne.airborne
            || (ledgeJumpRenderY != Integer.MIN_VALUE && link.romConsecutiveStepsCount == 0xFF)
                ? 1 : link.walkFrame;
        int baseState = LinkAnimator.ANIMATION_STATE[link.direction][normalFrame];
        if (link.linkEntityEffects.carryingLiftedObjectState == 1) {
            return LinkAnimator.LIFTING_ANIMATION_STATE[link.direction][link.walkFrame];
        }
        int override = queryOverride(link.linkEquipment.itemInSlotA());
        if (override >= 0) {
            return override;
        }
        override = queryOverride(link.linkEquipment.itemInSlotB());
        if (override >= 0) {
            return override;
        }
        if (link.linkAirborne.airborne && !link.linkAirborne.spinningPitWarpArrival && jumpAnimationFrame < 3 && !link.linkAirborne.swordSuppressesJumpAnimation()) {
            return LinkAnimator.JUMP_ANIMATION_STATE[link.direction][jumpAnimationFrame];
        }
        if (link.spriteSheet != null) {
            int carriedShield = !link.linkStateMachine.gelClinging && link.playerState != null
                && (link.playerState.itemA() == PlayerState.INVENTORY_SHIELD
                    || link.playerState.itemB() == PlayerState.INVENTORY_SHIELD)
                        ? link.playerState.shieldLevel() : 0;
            return link.spriteSheet.walkingAnimationState(Link.romDirectionForJavaDirection(link.direction),
                normalFrame, carriedShield, link.linkEquipment.isUsingShield());
        }
        if (link.linkEquipment.isUsingShield() && link.playerState != null && link.playerState.shieldLevel() > 0) {
            int[][] shieldTable = link.playerState.shieldLevel() >= 2
                ? LinkAnimator.MIRROR_SHIELD_USE_ANIMATION_STATE : LinkAnimator.SHIELD_USE_ANIMATION_STATE;
            return shieldTable[link.direction][normalFrame];
        }
        return baseState;
    }

    int queryOverride(EquipmentItem item) {
        if (item == null) {
            return -1;
        }
        return item.overrideAnimationState(link.direction, link.walkFrame);
    }

    /** Applies the ROM's explicit hLinkAnimationState=$6A fallen-pose write. */
    void showRomFallenPose() {
        link.linkStateMachine.romInteractiveMotionBlocked = true;
        romAnimationStateOverride = 0x6A;
        link.movingThisFrame = false;
    }

    /** Applies HoldEntityAboveLink's got-item pose and interaction lock. */
    void showHeldItemPose() {
        link.linkStateMachine.romInteractiveMotionBlocked = true;
        romAnimationStateOverride = 0x6C;
        link.direction = Link.DIRECTION_DOWN;
        link.romAttackStepAnimationCountdown = 0;
        link.movingThisFrame = false;
    }

    /** HandleGotItemB's power-up pose, distinct from lifting a physical object. */
    void showPowerUpPickupPose() {
        link.linkStateMachine.romInteractiveMotionBlocked = true;
        romAnimationStateOverride = 0x6B;
        link.linkDamage.collisionIgnoreFramesRemaining = 0;
        link.linkMotion.forcedSpeedPending = false;
        link.romAttackStepAnimationCountdown = 0;
        link.movingThisFrame = false;
    }

    /** Applies TarinShield2Handler's shield-specific got-item presentation. */
    void showTarinShieldPresentation(int[] palette) {
        if (palette == null || palette.length < 4) {
            throw new IllegalArgumentException("Tarin shield palette must contain four colors");
        }
        showHeldItemPose();
        tarinShieldPresentation = true;
        tarinShieldPalette = palette.clone();
    }

    void clearTarinShieldPresentation() {
        tarinShieldPresentation = false;
    }

    /** Mirrors TarinShield2Handler's final LINK_ANIMATION_STATE_STANDING_SHIELD_DOWN write. */
    void showStandingShieldDownPose() {
        tarinShieldPresentation = false;
        romAnimationStateOverride = 0x22;
        link.linkStateMachine.romInteractiveMotionBlocked = false;
        link.direction = Link.DIRECTION_DOWN;
        link.movingThisFrame = false;
    }

    /** Applies one ROM spin sector while obtaining the beach sword. */
    void showSwordAcquisitionSpinPose(int countdown) {
        if (countdown < 0 || countdown > 0x20) {
            throw new IllegalArgumentException("Sword-spin countdown must be between 0 and 32");
        }
        if (link.romTables == null) {
            throw new IllegalStateException("Sword acquisition spin requires ROM tables");
        }
        if (swordAcquisitionBaseRomDirection < 0) {
            swordAcquisitionBaseRomDirection = Link.romDirectionForJavaDirection(link.direction);
            swordAcquisitionAnimationState = resolveAnimationState();
        }
        // The spin handler writes a fresh hLinkAnimationState. Do not let a
        // preceding generic motion-block request leave its $6A butt pose in
        // front of this ROM-driven sword pose.
        romAnimationStateOverride = -1;
        int sector = Math.min(7, countdown >> 2);
        int swordState = link.romTables.swordSpinAnimationState(
            swordAcquisitionBaseRomDirection, sector);
        int absoluteRomDirection = link.romTables.swordSpinAbsoluteDirection(
            swordAcquisitionBaseRomDirection, sector);
        int animationState = link.romTables.swordDirectionAnimState(
            absoluteRomDirection, swordState);
        // UpdateSpinAttackAnimation leaves hLinkAnimationState unchanged for
        // LINK_ANIMATION_STATE_HIDDEN, so retain the previous ROM pose.
        if (animationState != 0xFF) {
            swordAcquisitionAnimationState = animationState;
        }
        link.linkStateMachine.romInteractiveMotionBlocked = true;
        link.direction = Link.javaDirectionForRomDirection(absoluteRomDirection);
        link.movingThisFrame = false;
    }

    /** Mirrors the sword handler's final hLinkAnimationState=$6B write. */
    void showSwordAcquisitionFinalPose() {
        link.linkStateMachine.romInteractiveMotionBlocked = true;
        swordAcquisitionBaseRomDirection = -1;
        swordAcquisitionAnimationState = -1;
        romAnimationStateOverride = 0x6B;
        link.direction = Link.DIRECTION_DOWN;
        link.movingThisFrame = false;
    }

    /** Hides ordinary Link OAM and selects Marin's dedicated bed display list. */
    void showMarinWakeUpBed(int spriteVariant) {
        if (spriteVariant < 0 || spriteVariant > 4) {
            throw new IllegalArgumentException("Bed sprite variant must be between 0 and 4");
        }
        marinWakeUpBedVariant = spriteVariant;
    }

    void submitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
        if (link.linkEntityEffects.likeLikeCaptured) {
            return;
        }
        Tile[] composedTiles = new Tile[4];
        int animationState = resolveAnimationState();
        if (animationState == 0xFF) return;
        link.spriteSheet.resolveTiles(animationState, composedTiles);

        boolean leftFlipX = link.spriteSheet.leftHalfFlipX(animationState);
        boolean leftFlipY = link.spriteSheet.leftHalfFlipY(animationState);
        boolean rightFlipX = link.spriteSheet.rightHalfFlipX(animationState);
        boolean rightFlipY = link.spriteSheet.rightHalfFlipY(animationState);

        int originX = link.pixelX() + offsetX;
        int renderY = renderY();
        int originY = renderY + offsetY;
        int[] palette = link.playerState != null
            && (link.playerState.invincibilityCounter() & 0x04) != 0
            // DrawLinkSprite's GBC path selects object palette 4 from bit 2
            // of wInvincibilityCounter while Link is flashing.
            ? link.tunicPalette.forObjectPalette(4)
            : link.tunicPalette.forTunic(
                link.playerState == null ? PlayerState.TUNIC_GREEN : link.playerState.tunicType());

        if (link.linkStateMachine.revolvingDoorPaletteVariant >= 0 && link.linkStateMachine.revolvingDoorFrame < 0x38) {
            palette = link.romTables.revolvingDoorPalette(link.linkStateMachine.revolvingDoorPaletteVariant);
        }

        // Column-major tile layout: [0]=UL, [1]=LL, [2]=UR, [3]=LR.
        // GB 8x16 flipY swaps the two stacked tiles inside a column and flips
        // each vertically.
        Tile leftTop = composedTiles[leftFlipY ? 1 : 0];
        Tile leftBottom = composedTiles[leftFlipY ? 0 : 1];
        Tile rightTop = composedTiles[rightFlipY ? 3 : 2];
        Tile rightBottom = composedTiles[rightFlipY ? 2 : 3];

        sink.append8x16(leftTop, leftBottom, originX, originY, leftFlipX, leftFlipY, palette, false);
        sink.append8x16(rightTop, rightBottom, originX + 8, originY, rightFlipX, rightFlipY, palette, false);

        // Let equipped items paint their own sprites on top (e.g. the sword
        // blade during a swing). Matches the disassembly's ordering where
        // DrawLinkSprite writes Link's body to wLinkOAMBuffer slots 1-2 and
        // ApplyLinkMotionState/func_020_4AB3 writes the sword into slots 4-5.
        submitEquippedOam(sink, offsetX, offsetY);
    }

    void submitEquippedOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) {
        if (link.itemRegistry == null || link.playerState == null) {
            return;
        }
        EquipmentItem a = link.itemRegistry.lookup(link.playerState.itemA());
        if (a != null) {
            a.submitOam(sink, link.pixelX(), link.pixelY() - link.zPixels(), link.direction, offsetX, offsetY);
        }
        EquipmentItem b = link.itemRegistry.lookup(link.playerState.itemB());
        if (b != null && b != a) {
            b.submitOam(sink, link.pixelX(), link.pixelY() - link.zPixels(), link.direction, offsetX, offsetY);
        }
        if (swordAcquisitionAnimationState >= 0) {
            // ApplyLinkMotionState draws the sword during the pickup spin
            // before GiveInventoryItem awards it to slot A/B. Reuse the ROM
            // sword handler from the registry, but do not draw it twice if a
            // save already has the sword equipped.
            EquipmentItem acquisitionSword = link.itemRegistry.lookup(PlayerState.INVENTORY_SWORD);
            if (acquisitionSword != null && acquisitionSword != a && acquisitionSword != b) {
                acquisitionSword.submitOam(sink, link.pixelX(), link.pixelY() - link.zPixels(),
                    link.direction, offsetX, offsetY);
            }
        }
    }

    private int renderY() {
        return ledgeJumpRenderY != Integer.MIN_VALUE ? ledgeJumpRenderY
            : link.pixelY() - (bedJumpRenderZ >= 0 ? bedJumpRenderZ : link.zPixels());
    }
}
