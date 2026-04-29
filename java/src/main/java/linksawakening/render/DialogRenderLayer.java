package linksawakening.render;

import linksawakening.dialog.DialogController;
import linksawakening.dialog.DialogRenderer;

public final class DialogRenderLayer implements RenderLayer {
    private final DialogController dialogController;

    public DialogRenderLayer(DialogController dialogController) {
        this.dialogController = dialogController;
    }

    @Override
    public void render(RenderContext context) {
        if (dialogController.isActive()) {
            DialogRenderer.render(context.buffer(), dialogController);
        }
    }
}
