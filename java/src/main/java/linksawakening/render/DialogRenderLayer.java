package linksawakening.render;

import linksawakening.dialog.DialogController;
import linksawakening.dialog.DialogRenderer;
import linksawakening.world.RoomRenderSnapshot;

public final class DialogRenderLayer implements RenderLayer {
    private static final int CONTINUE_BLINK_MASK = 0x10;
    private static final int CONTINUE_TILE = 0xA2;
    private static final int CONTINUE_X = 0x97 - 8;
    private static final int CONTINUE_Y_OFFSET = 31;

    private final DialogController dialogController;
    private final RoomRenderSnapshot room;
    private final int frameCounter;

    public DialogRenderLayer(DialogController dialogController, RoomRenderSnapshot room, int frameCounter) {
        this.dialogController = dialogController;
        this.room = room;
        this.frameCounter = frameCounter;
    }

    @Override
    public void render(RenderContext context) {
        if (!dialogController.isActive()) {
            return;
        }
        if (room == null) {
            DialogRenderer.render(context.buffer(), dialogController);
        } else {
            // DialogOpenAnimationEnd assigns background palette 1 to each box row.
            DialogRenderer.render(context.buffer(), dialogController, room.palettes()[1]);
            if (dialogController.isWaitingForPageAdvance() && (frameCounter & CONTINUE_BLINK_MASK) == 0
                && room.entities() != null && room.entities().spriteSelection() != null) {
                int[] palette = room.entities().spriteSelection().objectPalettes()[2];
                IndexedRenderer.drawSpriteTile8x16(context.buffer(), context.gpu(), CONTINUE_TILE,
                    CONTINUE_X, dialogController.boxY() + CONTINUE_Y_OFFSET, false, true, palette);
            }
        }
    }
}
