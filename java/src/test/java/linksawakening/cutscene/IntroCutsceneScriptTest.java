package linksawakening.cutscene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IntroCutsceneScriptTest {

    @Test
    void introScriptStartsAtStormSceneThenReachesBeachAndTitle() {
        RecordingContext context = new RecordingContext();
        CutsceneController controller = new CutsceneController(context);

        controller.start(IntroCutsceneScript.create());
        controller.tick();

        assertEquals(IntroCutsceneScript.SCENE_SEA, context.scene());
        assertTrue(controller.isActive());

        tick(controller, IntroCutsceneScript.SEA_DURATION_FRAMES);
        assertEquals(IntroCutsceneScript.SCENE_LINK_FACE, context.scene());

        tick(controller, IntroCutsceneScript.LINK_FACE_DURATION_FRAMES);
        assertEquals(IntroCutsceneScript.SCENE_BEACH, context.scene());

        tick(controller, IntroCutsceneScript.BEACH_DURATION_FRAMES);
        assertEquals(IntroCutsceneScript.SCENE_TITLE, context.scene());

        tick(controller, IntroCutsceneScript.TITLE_DURATION_FRAMES);
        assertFalse(controller.isActive());
    }

    private static void tick(CutsceneController controller, int count) {
        for (int i = 0; i < count; i++) {
            controller.tick();
        }
    }

    private static final class RecordingContext implements CutsceneContext {
        private String scene = "";

        @Override
        public void setScene(String sceneId) {
            scene = sceneId;
        }

        @Override
        public void showDialog(String text) {
        }

        @Override
        public boolean isDialogActive() {
            return false;
        }

        String scene() {
            return scene;
        }
    }
}
