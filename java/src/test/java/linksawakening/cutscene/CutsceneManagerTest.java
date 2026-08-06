package linksawakening.cutscene;

import linksawakening.dialog.DialogController;
import linksawakening.scene.BackgroundScene;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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

    @Test
    void forwardsRomBackedFrameSnapshotsWhileKeepingSceneLoading() {
        DialogController dialog = new DialogController(16);
        List<String> loadedScenes = new ArrayList<>();
        CutsceneManager manager = new CutsceneManager(dialog, loadedScenes::add);

        manager.startIntro(loadRom(), CutsceneManagerTest::backgroundFor);

        assertEquals(IntroCutsceneScript.SCENE_SEA, manager.frameSnapshot().sceneId());
        assertEquals(0, manager.frameSnapshot().frameCounter());
        manager.tick();
        assertEquals(1, manager.frameSnapshot().frameCounter());
        assertEquals(List.of(IntroCutsceneScript.SCENE_SEA), loadedScenes);
    }

    private static BackgroundScene backgroundFor(String sceneId) {
        int[] tilemap = new int[32 * 32];
        int[] attrmap = new int[32 * 32];
        Arrays.fill(tilemap, IntroCutsceneScript.SCENE_TITLE.equals(sceneId) ? 0 : 0x7E);
        return new BackgroundScene(tilemap, attrmap, new int[8][4], new int[8][4]);
    }

    private static byte[] loadRom() {
        try (InputStream stream = CutsceneManagerTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load test ROM", exception);
        }
    }
}
