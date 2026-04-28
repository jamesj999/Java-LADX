package linksawakening.scene;

import linksawakening.cutscene.IntroCutsceneScript;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BackgroundSceneLoaderTest {

    @Test
    void introSceneSpecsPointAtDisassemblyBackgrounds() {
        assertEquals(new BackgroundSceneSpec(0x08, 0x6C37, 0x24, 0x5D18, 0x21, 0x7536),
            BackgroundSceneCatalog.forCutsceneScene(IntroCutsceneScript.SCENE_SEA));
        assertEquals(new BackgroundSceneSpec(0x08, 0x6D80, 0x24, 0x5D69, 0x21, 0x7536),
            BackgroundSceneCatalog.forCutsceneScene(IntroCutsceneScript.SCENE_LINK_FACE));
        assertEquals(new BackgroundSceneSpec(0x08, 0x6F8B, 0x24, 0x7BA7, 0x21, 0x7DEE),
            BackgroundSceneCatalog.forCutsceneScene(IntroCutsceneScript.SCENE_BEACH));
        assertEquals(new BackgroundSceneSpec(0x08, 0x710A, 0x24, 0x7BF0, 0x21, 0x7DEE),
            BackgroundSceneCatalog.forCutsceneScene(IntroCutsceneScript.SCENE_TITLE));
    }

    @Test
    void titleTilesetIsNeededOnlyAfterLinkFace() {
        assertFalse(BackgroundSceneCatalog.requiresTitleTileset(IntroCutsceneScript.SCENE_SEA));
        assertFalse(BackgroundSceneCatalog.requiresTitleTileset(IntroCutsceneScript.SCENE_LINK_FACE));
        assertTrue(BackgroundSceneCatalog.requiresTitleTileset(IntroCutsceneScript.SCENE_BEACH));
        assertTrue(BackgroundSceneCatalog.requiresTitleTileset(IntroCutsceneScript.SCENE_TITLE));
    }

    @Test
    void decodedBackgroundsPreserveFullGameBoyBgMap() {
        BackgroundSceneLoader loader = new BackgroundSceneLoader(loadRom());

        BackgroundScene title = loader.load(BackgroundSceneCatalog.TITLE);

        assertEquals(32 * 32, title.tilemap().length);
        assertEquals(32 * 32, title.attrmap().length);
        assertEquals(0x7E, title.tilemap()[0]);
        assertEquals(0x7E, title.tilemap()[1 * 32 + 31]);
        assertEquals(0x80, title.tilemap()[2 * 32 + 2]);
        assertEquals(0x8F, title.tilemap()[2 * 32 + 17]);
    }

    @Test
    void decodedBackgroundsExposeObjectPalettesAfterBgPalettes() {
        BackgroundSceneLoader loader = new BackgroundSceneLoader(loadRom());

        BackgroundScene beach = loader.load(BackgroundSceneCatalog.INTRO_BEACH);

        assertEquals(8, beach.palettes().length);
        assertEquals(8, beach.objectPalettes().length);
        assertEquals(0x000094FF, beach.objectPalettes()[3][0]);
        assertEquals(0x00000000, beach.objectPalettes()[3][1]);
        assertEquals(0x00CD8B31, beach.objectPalettes()[3][2]);
        assertEquals(0x00FFBD9C, beach.objectPalettes()[3][3]);
    }

    private static byte[] loadRom() {
        try (var stream = BackgroundSceneLoaderTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }
}
