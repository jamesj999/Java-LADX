package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.equipment.EquipmentItem;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.Tile;
import linksawakening.state.InventoryItem;
import linksawakening.state.Tunic;

/** Owns body tile composition and equipment sprite order for ROM DrawLinkSprite ($00:1D4F). */
final class LinkSpriteComposer {
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;

    LinkSpriteComposer(
            LinkComponentRef<LinkAirborne> linkAirborne,
            LinkComponentRef<LinkAnimationState> linkAnimationState,
            LinkComponentRef<LinkMotion> linkMotion,
            LinkComponentRef<LinkServices> linkServices,
            LinkComponentRef<LinkStateMachine> linkStateMachine) {
        this.linkAirborne = linkAirborne;
        this.linkAnimationState = linkAnimationState;
        this.linkMotion = linkMotion;
        this.linkServices = linkServices;
        this.linkStateMachine = linkStateMachine;
    }

    private final Tile[] composedTiles = new Tile[4];

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

    void render(byte[] displayBuffer, int offsetX, int offsetY, int animationState, int bodyY, boolean acquisition) {

        if (animationState == 0xFF) return;
        linkServices.get().spriteSheet.resolveTiles(animationState, composedTiles);

        boolean leftFlipX = linkServices.get().spriteSheet.leftHalfFlipX(animationState);
        boolean leftFlipY = linkServices.get().spriteSheet.leftHalfFlipY(animationState);
        boolean rightFlipX = linkServices.get().spriteSheet.rightHalfFlipX(animationState);
        boolean rightFlipY = linkServices.get().spriteSheet.rightHalfFlipY(animationState);

        int originX = linkMotion.get().position().pixelX() + offsetX;
        int renderY = bodyY;
        int originY = renderY + offsetY;
        int[] palette = linkServices.get().playerState != null
                        && (linkServices.get().playerState.vitals().invincibilityCounter() & 0x04) != 0
                // DrawLinkSprite's GBC path selects object palette 4 from bit 2
                // of wInvincibilityCounter while Link is flashing.
                ? linkServices.get().tunicPalette.objectPaletteView(4)
                : linkServices
                        .get()
                        .tunicPalette
                        .tunicView(
                                linkServices.get().playerState == null
                                        ? Tunic.GREEN.romValue()
                                        : linkServices
                                                .get()
                                                .playerState
                                                .equipmentLevels()
                                                .tunicType());

        if (linkStateMachine.get().door().revolvingDoorPaletteVariant() >= 0
                && linkStateMachine.get().door().revolvingDoorFrame() < 0x38) {
            palette = linkServices
                    .get()
                    .linkMotionTables
                    .revolvingDoorPalette(linkStateMachine.get().door().revolvingDoorPaletteVariant());
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
        renderEquippedItems(displayBuffer, offsetX, offsetY, acquisition);
    }

    private void renderEquippedItems(byte[] displayBuffer, int offsetX, int offsetY, boolean acquisition) {
        if (linkServices.get().itemRegistry == null || linkServices.get().playerState == null) {
            return;
        }
        EquipmentItem a = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.inventory().itemA());
        if (a != null) {
            a.render(
                    displayBuffer,
                    linkMotion.get().position().pixelX(),
                    linkMotion.get().position().pixelY() - linkAirborne.get().accessZPixels(),
                    linkAnimationState.get().direction.javaIndex(),
                    offsetX,
                    offsetY);
        }
        EquipmentItem b = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.inventory().itemB());
        if (b != null && b != a) {
            b.render(
                    displayBuffer,
                    linkMotion.get().position().pixelX(),
                    linkMotion.get().position().pixelY() - linkAirborne.get().accessZPixels(),
                    linkAnimationState.get().direction.javaIndex(),
                    offsetX,
                    offsetY);
        }
        if (acquisition) {
            // ApplyLinkMotionState draws the sword during the pickup spin
            // before GiveInventoryItem awards it to slot A/B. Reuse the ROM
            // sword handler from the registry, but do not draw it twice if a
            // save already has the sword equipped.
            EquipmentItem acquisitionSword = linkServices.get().itemRegistry.lookup(InventoryItem.SWORD.romValue());
            if (acquisitionSword != null && acquisitionSword != a && acquisitionSword != b) {
                acquisitionSword.render(
                        displayBuffer,
                        linkMotion.get().position().pixelX(),
                        linkMotion.get().position().pixelY()
                                - linkAirborne.get().accessZPixels(),
                        linkAnimationState.get().direction.javaIndex(),
                        offsetX,
                        offsetY);
            }
        }
    }

    void submitOam(
            linksawakening.render.OamSink sink,
            int offsetX,
            int offsetY,
            int animationState,
            int bodyY,
            boolean acquisition) {

        if (animationState == 0xFF) return;
        linkServices.get().spriteSheet.resolveTiles(animationState, composedTiles);

        boolean leftFlipX = linkServices.get().spriteSheet.leftHalfFlipX(animationState);
        boolean leftFlipY = linkServices.get().spriteSheet.leftHalfFlipY(animationState);
        boolean rightFlipX = linkServices.get().spriteSheet.rightHalfFlipX(animationState);
        boolean rightFlipY = linkServices.get().spriteSheet.rightHalfFlipY(animationState);

        int originX = linkMotion.get().position().pixelX() + offsetX;
        int renderY = bodyY;
        int originY = renderY + offsetY;
        int[] palette = linkServices.get().playerState != null
                        && (linkServices.get().playerState.vitals().invincibilityCounter() & 0x04) != 0
                // DrawLinkSprite's GBC path selects object palette 4 from bit 2
                // of wInvincibilityCounter while Link is flashing.
                ? linkServices.get().tunicPalette.objectPaletteView(4)
                : linkServices
                        .get()
                        .tunicPalette
                        .tunicView(
                                linkServices.get().playerState == null
                                        ? Tunic.GREEN.romValue()
                                        : linkServices
                                                .get()
                                                .playerState
                                                .equipmentLevels()
                                                .tunicType());

        if (linkStateMachine.get().door().revolvingDoorPaletteVariant() >= 0
                && linkStateMachine.get().door().revolvingDoorFrame() < 0x38) {
            palette = linkServices
                    .get()
                    .linkMotionTables
                    .revolvingDoorPalette(linkStateMachine.get().door().revolvingDoorPaletteVariant());
        }

        // Column-major tile layout: [0]=UL, [1]=LL, [2]=UR, [3]=LR.
        // GB 8x16 flipY swaps the two stacked tiles inside a column and flips
        // each vertically.
        Tile leftTop = composedTiles[leftFlipY ? 1 : 0];
        Tile leftBottom = composedTiles[leftFlipY ? 0 : 1];
        Tile rightTop = composedTiles[rightFlipY ? 3 : 2];
        Tile rightBottom = composedTiles[rightFlipY ? 2 : 3];

        // DrawLinkSprite ($00:1D4F-$1D59) rejects both body columns when
        // the byte-sized visual OAM Y is >= $88. Equipped items have their
        // own OAM writers and do not inherit this body-only return.
        if (((originY + 0x10) & 0xFF) < 0x88) {
            sink.append8x16(leftTop, leftBottom, originX, originY, leftFlipX, leftFlipY, palette, false);
            sink.append8x16(rightTop, rightBottom, originX + 8, originY, rightFlipX, rightFlipY, palette, false);
        }

        // Let equipped items paint their own sprites on top (e.g. the sword
        // blade during a swing). Matches the disassembly's ordering where
        // DrawLinkSprite writes Link's body to wLinkOAMBuffer slots 1-2 and
        // ApplyLinkMotionState/func_020_4AB3 writes the sword into slots 4-5.
        submitEquippedOam(sink, offsetX, offsetY, acquisition);
    }

    private void submitEquippedOam(linksawakening.render.OamSink sink, int offsetX, int offsetY, boolean acquisition) {
        if (linkServices.get().itemRegistry == null || linkServices.get().playerState == null) {
            return;
        }
        EquipmentItem a = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.inventory().itemA());
        if (a != null) {
            a.submitOam(
                    sink,
                    linkMotion.get().position().pixelX(),
                    linkMotion.get().position().pixelY() - linkAirborne.get().accessZPixels(),
                    linkAnimationState.get().direction.javaIndex(),
                    offsetX,
                    offsetY);
        }
        EquipmentItem b = linkServices
                .get()
                .itemRegistry
                .lookup(linkServices.get().playerState.inventory().itemB());
        if (b != null && b != a) {
            b.submitOam(
                    sink,
                    linkMotion.get().position().pixelX(),
                    linkMotion.get().position().pixelY() - linkAirborne.get().accessZPixels(),
                    linkAnimationState.get().direction.javaIndex(),
                    offsetX,
                    offsetY);
        }
        if (acquisition) {
            // ApplyLinkMotionState draws the sword during the pickup spin
            // before GiveInventoryItem awards it to slot A/B. Reuse the ROM
            // sword handler from the registry, but do not draw it twice if a
            // save already has the sword equipped.
            EquipmentItem acquisitionSword = linkServices.get().itemRegistry.lookup(InventoryItem.SWORD.romValue());
            if (acquisitionSword != null && acquisitionSword != a && acquisitionSword != b) {
                acquisitionSword.submitOam(
                        sink,
                        linkMotion.get().position().pixelX(),
                        linkMotion.get().position().pixelY()
                                - linkAirborne.get().accessZPixels(),
                        linkAnimationState.get().direction.javaIndex(),
                        offsetX,
                        offsetY);
            }
        }
    }
}
