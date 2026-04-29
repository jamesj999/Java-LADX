package linksawakening.cutscene;

import linksawakening.dialog.DialogController;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CutsceneManagerTest {

    @Test
    void ownsIntroLifecycleSceneLoadingAndDialogContext() {
        DialogController dialog = new DialogController(16);
        List<String> loadedScenes = new ArrayList<>();
        CutsceneManager manager = new CutsceneManager(dialog, loadedScenes::add);

        manager.startIntro();
        manager.tick();

        assertTrue(manager.isActive());
        assertEquals(List.of(IntroCutsceneScript.SCENE_SEA), loadedScenes);

        manager.showDialog("HELLO");
        assertTrue(manager.isDialogActive());
        dialog.advance();
        dialog.advance();
        assertFalse(manager.isDialogActive());
    }

    @Test
    void skipIntroCompletesSequenceAndLoadsTitleScene() {
        DialogController dialog = new DialogController(16);
        List<String> loadedScenes = new ArrayList<>();
        CutsceneManager manager = new CutsceneManager(dialog, loadedScenes::add);

        manager.startIntro();

        assertTrue(manager.skipIntroToTitle());
        assertFalse(manager.isActive());
        assertTrue(manager.isShowingTitleScene());
        assertEquals(List.of(IntroCutsceneScript.SCENE_SEA, IntroCutsceneScript.SCENE_TITLE),
            loadedScenes);
    }
}
