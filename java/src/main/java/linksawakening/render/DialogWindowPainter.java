package linksawakening.render;

import linksawakening.dialog.DialogController;

/** Optional dialog-window/glyph submission. Null configuration retains the default ROM-backed painter. */
@FunctionalInterface
interface DialogWindowPainter {
    void submit(HardwareFrame frame, DialogController dialog, int[] palette);
}
