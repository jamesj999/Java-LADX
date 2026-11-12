package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkAnimator.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.equipment.EquipmentItem;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.Tile;

/** Shared state and dependency ownership for LinkAnimator. */
abstract class LinkPoseState {

    protected final LinkComponentRef<DialogGotItemSequence> dialogGotItemSequence;
    protected final LinkComponentRef<LinkAirborne> linkAirborne;
    protected final LinkComponentRef<LinkAnimationState> linkAnimationState;
    protected final LinkComponentRef<LinkDamage> linkDamage;
    protected final LinkComponentRef<LinkEntityEffects> linkEntityEffects;
    protected final LinkComponentRef<LinkEquipment> linkEquipment;
    protected final LinkComponentRef<LinkItemState> linkItemState;
    protected final LinkComponentRef<LinkMotion> linkMotion;
    protected final LinkComponentRef<LinkServices> linkServices;
    protected final LinkComponentRef<LinkStateMachine> linkStateMachine;

    static final class Dependencies {
        LinkComponentRef<DialogGotItemSequence> dialogGotItemSequence;
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkDamage> linkDamage;
        LinkComponentRef<LinkEntityEffects> linkEntityEffects;
        LinkComponentRef<LinkEquipment> linkEquipment;
        LinkComponentRef<LinkItemState> linkItemState;
        LinkComponentRef<LinkMotion> linkMotion;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
    }

    static final int[][] ANIMATION_STATE = {
        {0x00, 0x01}, // DOWN:  standing, walking
        {0x04, 0x05}, // UP
        {0x06, 0x07}, // LEFT
        {0x0A, 0x0B}, // RIGHT
    };

    static final int[][] SHIELD_USE_ANIMATION_STATE = {
        {0x24, 0x25}, // DOWN
        {0x30, 0x31}, // UP
        {0x28, 0x29}, // LEFT
        {0x2A, 0x2B}, // RIGHT
    };

    static final int[][] MIRROR_SHIELD_USE_ANIMATION_STATE = {
        {0x26, 0x27}, // DOWN
        {0x32, 0x33}, // UP
        {0x28, 0x29}, // LEFT
        {0x2E, 0x2F}, // RIGHT
    };

    static final int[][] LIFTING_ANIMATION_STATE = {
        {0x44, 0x45}, // DOWN
        {0x42, 0x43}, // UP
        {0x40, 0x41}, // LEFT
        {0x3E, 0x3F}, // RIGHT
    };

    static final int[][] SWIMMING_ANIMATION_STATE = {
        {0x4C, 0x4D}, // DOWN
        {0x4A, 0x4B}, // UP
        {0x48, 0x49}, // LEFT
        {0x46, 0x47}, // RIGHT
    };

    static final int[] DIVING_ANIMATION_STATE = {0x4E, 0x4F};

    static final int[][] JUMP_ANIMATION_STATE = {
        {0x64, 0x65, 0x66}, // DOWN
        {0x67, 0x68, 0x69}, // UP
        {0x5E, 0x5F, 0x60}, // LEFT
        {0x61, 0x62, 0x63}, // RIGHT
    };

    static final int[] FALL_ANIMATION_STATE = {0x55, 0x56, 0x57, 0x57};

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

    int pushingAnimationState = -1;

    LinkPoseState(Dependencies dependencies) {
        dialogGotItemSequence = dependencies.dialogGotItemSequence;
        linkAirborne = dependencies.linkAirborne;
        linkAnimationState = dependencies.linkAnimationState;
        linkDamage = dependencies.linkDamage;
        linkEntityEffects = dependencies.linkEntityEffects;
        linkEquipment = dependencies.linkEquipment;
        linkItemState = dependencies.linkItemState;
        linkMotion = dependencies.linkMotion;
        linkServices = dependencies.linkServices;
        linkStateMachine = dependencies.linkStateMachine;
    }

    static void drawTile(
            byte[] buffer, Tile tile, int screenX, int screenY, boolean flipX, boolean flipY, int[] palette) {
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

    protected abstract void render(byte[] displayBuffer, int offsetX, int offsetY);

    protected abstract void renderEquippedItems(byte[] displayBuffer, int offsetX, int offsetY);

    protected abstract int resolveAnimationState();

    protected abstract void cachePushingAnimation();

    protected abstract int queryOverride(EquipmentItem item);

    protected abstract void showRomFallenPose();

    protected abstract void showHeldItemPose();

    protected abstract void showPowerUpPickupPose();

    protected abstract void showTarinShieldPresentation(int[] palette);

    protected abstract void clearTarinShieldPresentation();

    protected abstract void showStandingShieldDownPose();

    protected abstract void showSwordAcquisitionSpinPose(int countdown);

    protected abstract void showSwordAcquisitionFinalPose();

    protected abstract void showMarinWakeUpBed(int spriteVariant);

    protected abstract void submitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY);

    protected abstract void submitEquippedOam(linksawakening.render.OamSink sink, int offsetX, int offsetY);

    protected abstract int renderY();

    protected abstract boolean accessIsTarinShieldPresentationVisible();

    protected abstract int[] accessTarinShieldPalette();

    protected abstract boolean accessIsMarinWakeUpBedVisible();

    protected abstract int accessMarinWakeUpBedVariant();

    protected abstract int accessRomAnimationState();

    protected abstract void accessShowRomFallenPose();

    protected abstract boolean accessDialogGotItemActive();

    protected abstract int accessDialogGotItem();

    protected abstract int accessDialogGotItemCountdown();

    protected abstract int accessPresentedDialogGotItem();

    protected abstract void accessPresentToadstool();

    protected abstract void accessClearDialogGotItem();

    protected abstract void accessShowHeldItemPose();

    protected abstract void accessShowPowerUpPickupPose();

    protected abstract void accessShowTarinShieldPresentation(int[] palette);

    protected abstract void accessClearTarinShieldPresentation();

    protected abstract void accessShowStandingShieldDownPose();

    protected abstract void accessShowSwordAcquisitionSpinPose(int countdown);

    protected abstract void accessShowSwordAcquisitionFinalPose();

    protected abstract void accessShowMarinWakeUpBed(int spriteVariant);

    protected abstract void accessRender(byte[] displayBuffer, int offsetX, int offsetY);

    protected abstract void accessSubmitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY);
}
