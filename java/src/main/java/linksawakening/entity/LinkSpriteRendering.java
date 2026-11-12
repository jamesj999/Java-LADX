package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkAnimator.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.equipment.EquipmentItem;
import linksawakening.gpu.Tile;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;
import linksawakening.state.Tunic;

/** LinkSpriteRendering behavior on the shared LinkAnimator state. */
abstract class LinkSpriteRendering extends LinkPoseState {
    protected LinkSpriteRendering(Dependencies dependencies) {
        super(dependencies);
    }

    protected void render(byte[] displayBuffer, int offsetX, int offsetY) {
        if (linkEntityEffects.get().likeLikeCaptured) {
            return;
        }
        int animationState = resolveAnimationState();
        if (animationState == 0xFF) return;
        linkServices.get().spriteSheet.resolveTiles(animationState, composedTiles);

        boolean leftFlipX = linkServices.get().spriteSheet.leftHalfFlipX(animationState);
        boolean leftFlipY = linkServices.get().spriteSheet.leftHalfFlipY(animationState);
        boolean rightFlipX = linkServices.get().spriteSheet.rightHalfFlipX(animationState);
        boolean rightFlipY = linkServices.get().spriteSheet.rightHalfFlipY(animationState);

        int originX = linkMotion.get().pixelX() + offsetX;
        int renderY = renderY();
        int originY = renderY + offsetY;
        int[] palette = linkServices.get().playerState != null
                        && (linkServices.get().playerState.invincibilityCounter() & 0x04) != 0
                // DrawLinkSprite's GBC path selects object palette 4 from bit 2
                // of wInvincibilityCounter while Link is flashing.
                ? linkServices.get().tunicPalette.forObjectPalette(4)
                : linkServices
                        .get()
                        .tunicPalette
                        .forTunic(
                                linkServices.get().playerState == null
                                        ? Tunic.GREEN.romValue()
                                        : linkServices.get().playerState.tunicType());

        if (linkStateMachine.get().revolvingDoorPaletteVariant >= 0
                && linkStateMachine.get().revolvingDoorFrame < 0x38) {
            palette = linkServices
                    .get()
                    .romTables
                    .revolvingDoorPalette(linkStateMachine.get().revolvingDoorPaletteVariant);
        }

        // Column-major tile layout: [0]=UL, [1]=LL, [2]=UR, [3]=LR.
        // GB 8x16 flipY swaps the two stacked tiles inside a column and flips
        // each vertically.
        Tile leftTop = composedTiles[leftFlipY ? 1 : 0];
        Tile leftBottom = composedTiles[leftFlipY ? 0 : 1];
        Tile rightTop = composedTiles[rightFlipY ? 3 : 2];
        Tile rightBottom = composedTiles[rightFlipY ? 2 : 3];

        drawTile(displayBuffer, leftTop, originX, originY, leftFlipX, leftFlipY, palette);
        drawTile(displayBuffer, leftBottom, originX, originY + 8, leftFlipX, leftFlipY, palette);
        drawTile(displayBuffer, rightTop, originX + 8, originY, rightFlipX, rightFlipY, palette);
        drawTile(displayBuffer, rightBottom, originX + 8, originY + 8, rightFlipX, rightFlipY, palette);

        // Let equipped items paint their own sprites on top (e.g. the sword
        // blade during a swing). Matches the disassembly's ordering where
        // DrawLinkSprite writes Link's body to wLinkOAMBuffer slots 1-2 and
        // ApplyLinkMotionState/func_020_4AB3 writes the sword into slots 4-5.
        renderEquippedItems(displayBuffer, offsetX, offsetY);
    }

    protected void renderEquippedItems(byte[] displayBuffer, int offsetX, int offsetY) {
        if (linkServices.get().itemRegistry == null || linkServices.get().playerState == null) {
            return;
        }
        EquipmentItem a = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.itemA());
        if (a != null) {
            a.render(
                    displayBuffer,
                    linkMotion.get().pixelX(),
                    linkMotion.get().pixelY() - linkAirborne.get().accessZPixels(),
                    linkAnimationState.get().direction,
                    offsetX,
                    offsetY);
        }
        EquipmentItem b = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.itemB());
        if (b != null && b != a) {
            b.render(
                    displayBuffer,
                    linkMotion.get().pixelX(),
                    linkMotion.get().pixelY() - linkAirborne.get().accessZPixels(),
                    linkAnimationState.get().direction,
                    offsetX,
                    offsetY);
        }
        if (swordAcquisitionAnimationState >= 0) {
            // ApplyLinkMotionState draws the sword during the pickup spin
            // before GiveInventoryItem awards it to slot A/B. Reuse the ROM
            // sword handler from the registry, but do not draw it twice if a
            // save already has the sword equipped.
            EquipmentItem acquisitionSword = linkServices.get().itemRegistry.lookup(InventoryItem.SWORD.romValue());
            if (acquisitionSword != null && acquisitionSword != a && acquisitionSword != b) {
                acquisitionSword.render(
                        displayBuffer,
                        linkMotion.get().pixelX(),
                        linkMotion.get().pixelY() - linkAirborne.get().accessZPixels(),
                        linkAnimationState.get().direction,
                        offsetX,
                        offsetY);
            }
        }
    }

    protected int resolveAnimationState() {
        if (romAnimationStateOverride >= 0) {
            return romAnimationStateOverride;
        }
        if (swordAcquisitionAnimationState >= 0) {
            return swordAcquisitionAnimationState;
        }
        if (linkAirborne.get().fallingIntoPit) {
            int frame = Math.min(
                    LinkAnimator.FALL_ANIMATION_STATE.length - 1,
                    linkAirborne.get().fallingFrameCounter / LinkRomConstants.FALL_FRAME_TICKS);
            return LinkAnimator.FALL_ANIMATION_STATE[frame];
        }

        if (linkStateMachine.get().motionState == LinkMotionState.SWIMMING) {
            return linkStateMachine.get().physicsModifier != 0
                    ? LinkAnimator.DIVING_ANIMATION_STATE[linkAnimationState.get().walkFrame]
                    : LinkAnimator.SWIMMING_ANIMATION_STATE[linkAnimationState.get().direction][
                            linkAnimationState.get().walkFrame];
        }

        if (linkEntityEffects.get().grabbingObject) {
            return 0x36
                    + 2 * Direction.fromJava(linkAnimationState.get().direction).romIndex()
                    + (linkEntityEffects.get().pullingObject ? 1 : 0);
        }
        // func_002_4338 copies the intermediate ROM carry animation state
        // directly (for example $37/$39/$3B/$3D). The final carry state is
        // the regular two-frame lifting list below.
        if (linkEntityEffects.get().carryingLiftedObjectState >= 2) {
            return linkEntityEffects.get().carryingLiftedObjectState;
        }
        return resolveNormalAnimation();
    }

    private int resolveNormalAnimation() {
        // ApplyLinkGroundMotion ($02:4503) sets the step counter to $FF
        // while airborne, selecting the alternate normal pose after the flip.
        int normalFrame = linkAirborne.get().airborne
                        || (ledgeJumpRenderY != Integer.MIN_VALUE
                                && linkAnimationState.get().romConsecutiveStepsCount == 0xFF)
                ? 1
                : linkAnimationState.get().walkFrame;
        int baseState = LinkAnimator.ANIMATION_STATE[linkAnimationState.get().direction][normalFrame];
        if (linkEntityEffects.get().carryingLiftedObjectState == 1) {
            return LinkAnimator.LIFTING_ANIMATION_STATE[linkAnimationState.get().direction][
                    linkAnimationState.get().walkFrame];
        }
        int override = queryOverride(linkEquipment.get().itemInSlotA());
        if (override >= 0) {
            return override;
        }
        override = queryOverride(linkEquipment.get().itemInSlotB());
        if (override >= 0) {
            return override;
        }
        if (linkAirborne.get().airborne
                && !linkAirborne.get().spinningPitWarpArrival
                && jumpAnimationFrame < 3
                && !linkAirborne.get().swordSuppressesJumpAnimation()) {
            return LinkAnimator.JUMP_ANIMATION_STATE[linkAnimationState.get().direction][jumpAnimationFrame];
        }
        if (pushingAnimationState >= 0) return pushingAnimationState;
        if (linkServices.get().spriteSheet != null) {
            int carriedShield = !linkStateMachine.get().gelClinging
                            && linkServices.get().playerState != null
                            && (linkServices.get().playerState.itemA() == InventoryItem.SHIELD.romValue()
                                    || linkServices.get().playerState.itemB() == InventoryItem.SHIELD.romValue())
                    ? linkServices.get().playerState.shieldLevel()
                    : 0;
            return linkServices
                    .get()
                    .spriteSheet
                    .walkingAnimationState(
                            Direction.fromJava(linkAnimationState.get().direction)
                                    .romIndex(),
                            normalFrame,
                            carriedShield,
                            linkEquipment.get().isUsingShield());
        }
        if (linkEquipment.get().isUsingShield()
                && linkServices.get().playerState != null
                && linkServices.get().playerState.shieldLevel() > 0) {
            int[][] shieldTable = linkServices.get().playerState.shieldLevel() >= 2
                    ? LinkAnimator.MIRROR_SHIELD_USE_ANIMATION_STATE
                    : LinkAnimator.SHIELD_USE_ANIMATION_STATE;
            return shieldTable[linkAnimationState.get().direction][normalFrame];
        }
        return baseState;
    }
}
