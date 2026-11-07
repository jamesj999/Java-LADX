package linksawakening.render;

import linksawakening.dialog.DialogController;
import linksawakening.dialog.DialogRenderer;

public final class DialogRenderLayer implements HardwareRenderLayer {
    private final DialogController dialogController;
    private final int[] backgroundPalette;
    private final int[][] objectPalettes;
    private final int frameCounter;

    public DialogRenderLayer(DialogController dialogController) {
        this(dialogController, null, null, 0);
    }

    public DialogRenderLayer(DialogController dialogController, int[] backgroundPalette,
                             int[][] objectPalettes, int frameCounter) {
        this.dialogController = dialogController;
        this.backgroundPalette = backgroundPalette;
        this.objectPalettes = objectPalettes;
        this.frameCounter = frameCounter;
    }

    @Override
    public void render(RenderContext context) {
        if (dialogController.isActive()) {
            if (backgroundPalette == null) {
                DialogRenderer.render(context.buffer(), dialogController);
            } else {
                DialogRenderer.render(context.buffer(), dialogController, backgroundPalette);
            }
            // DrawDialogArrow ($17:7D7C): OAM $97/$37 or $97/$7F,
            // tile $A2, Y flip, OBJ palette 2, hidden on frame-counter bit 4.
            if (objectPalettes != null && dialogController.isWaitingForPageAdvance()
                && (frameCounter & 0x10) == 0) {
                IndexedRenderer.drawSpriteTile8x16(context.buffer(), context.gpu(),
                    0xA2, 0x97 - 8, dialogController.boxY() + 31,
                    false, true, objectPalettes[2]);
            }
        }
    }
    @Override public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        if (!dialogController.isActive()) return;
        DialogRenderer.submitWindow(frame, dialogController, backgroundPalette);
        if (objectPalettes != null && dialogController.isWaitingForPageAdvance() && (frameCounter & 0x10) == 0)
            OamDrawing.draw8x16(new RenderContext(new byte[0], gpu, frame), gpu,
                0xA2, 0x97 - 8, dialogController.boxY() + 31, false, true, objectPalettes[2]);
    }
}
