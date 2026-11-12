package linksawakening.render;

import linksawakening.dialog.DialogController;
import linksawakening.dialog.DialogRenderer;

/** Submits dialog window pixels and the timed page-advance prompt. */
public final class DialogRenderLayer implements HardwareRenderLayer {
    private final DialogController dialogController;
    private final int[] backgroundPalette;
    private final int[][] objectPalettes;
    private final int frameCounter;

    public DialogRenderLayer(DialogController dialogController) {
        this(dialogController, null, null, 0);
    }

    public DialogRenderLayer(
            DialogController dialogController, int[] backgroundPalette, int[][] objectPalettes, int frameCounter) {
        this.dialogController = dialogController;
        this.backgroundPalette = backgroundPalette;
        this.objectPalettes = objectPalettes;
        this.frameCounter = frameCounter;
    }

    @Override
    public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        if (!dialogController.isActive()) return;
        frame.hideSpritesUnderDialog(dialogController.boxPosition() == DialogController.BoxPosition.BOTTOM);
        DialogRenderer.submitWindow(frame, dialogController, backgroundPalette);
        if (objectPalettes != null && dialogController.isWaitingForPageAdvance() && (frameCounter & 0x10) == 0)
            OamDrawing.draw8x16(
                    RenderContext.indexed(gpu, frame),
                    gpu,
                    0xA2,
                    0x97 - 8,
                    dialogController.boxY() + 31,
                    false,
                    true,
                    objectPalettes[2]);
    }
}
