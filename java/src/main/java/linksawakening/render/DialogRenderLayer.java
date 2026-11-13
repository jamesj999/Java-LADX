package linksawakening.render;

import linksawakening.dialog.DialogController;

/** Submits dialog window pixels and the timed page-advance prompt.  Hardware analogue: window text and OBJ page prompt. */
final class DialogRenderLayer implements HardwareRenderLayer {
    private DialogWindowPainter windowPainter = DialogWindowPainter.ROM;

    void windowPainter(DialogWindowPainter painter) {
        windowPainter = painter == null ? DialogWindowPainter.ROM : painter;
    }

    private DialogController dialogController;
    private int[] backgroundPalette;
    private int[][] objectPalettes;
    private int frameCounter;

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

    void configure(
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
        windowPainter.submit(frame, dialogController, backgroundPalette);
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
