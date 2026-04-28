package linksawakening.cutscene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CutsceneControllerTest {

    @Test
    void waitFramesBlocksUntilDurationElapsesThenRunsNextStep() {
        RecordingCutsceneContext context = new RecordingCutsceneContext();
        CutsceneController controller = new CutsceneController(context);

        controller.start(CutsceneScript.of(
            CutsceneStep.waitFrames(3),
            CutsceneStep.run(ctx -> ctx.setScene("intro-beach"))
        ));

        assertTrue(controller.isActive());
        controller.tick();
        controller.tick();
        assertEquals("", context.scene());

        controller.tick();

        assertFalse(controller.isActive());
        assertEquals("intro-beach", context.scene());
    }

    @Test
    void showDialogStepKeepsCutsceneActiveUntilDialogFinishes() {
        RecordingCutsceneContext context = new RecordingCutsceneContext();
        CutsceneController controller = new CutsceneController(context);

        controller.start(CutsceneScript.of(
            CutsceneStep.showDialog("WAKE UP"),
            CutsceneStep.run(ctx -> ctx.setScene("done"))
        ));

        controller.tick();

        assertTrue(controller.isActive());
        assertEquals("WAKE UP", context.dialogText());
        assertEquals("", context.scene());

        context.finishDialog();
        controller.tick();

        assertFalse(controller.isActive());
        assertEquals("done", context.scene());
    }

    private static final class RecordingCutsceneContext implements CutsceneContext {
        private String scene = "";
        private String dialogText = "";
        private boolean dialogActive;

        @Override
        public void setScene(String sceneId) {
            scene = sceneId;
        }

        @Override
        public void showDialog(String text) {
            dialogText = text;
            dialogActive = true;
        }

        @Override
        public boolean isDialogActive() {
            return dialogActive;
        }

        String scene() {
            return scene;
        }

        String dialogText() {
            return dialogText;
        }

        void finishDialog() {
            dialogActive = false;
        }
    }
}
