package linksawakening.render;

import linksawakening.dialog.DialogController;

/** Optional dialog-window/glyph submission. Null configuration retains the default ROM-backed painter. */
@FunctionalInterface
interface DialogWindowPainter {
    DialogWindowPainter ROM = linksawakening.dialog.DialogRenderer::submitWindow;

    void submit(HardwareFrame frame, DialogController dialog, int[] palette);
}
