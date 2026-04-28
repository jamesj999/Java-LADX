package linksawakening.scene;

import linksawakening.cutscene.IntroCutsceneScript;

public final class BackgroundSceneCatalog {

    public static final BackgroundSceneSpec TITLE =
        new BackgroundSceneSpec(0x08, 0x710A, 0x24, 0x7BF0, 0x21, 0x7DEE);
    public static final BackgroundSceneSpec INTRO_SEA =
        new BackgroundSceneSpec(0x08, 0x6C37, 0x24, 0x5D18, 0x21, 0x7536);
    public static final BackgroundSceneSpec INTRO_LINK_FACE =
        new BackgroundSceneSpec(0x08, 0x6D80, 0x24, 0x5D69, 0x21, 0x7536);
    public static final BackgroundSceneSpec INTRO_BEACH =
        new BackgroundSceneSpec(0x08, 0x6F8B, 0x24, 0x7BA7, 0x21, 0x7DEE);
    private BackgroundSceneCatalog() {
    }

    public static BackgroundSceneSpec forCutsceneScene(String sceneId) {
        if (IntroCutsceneScript.SCENE_SEA.equals(sceneId)) {
            return INTRO_SEA;
        }
        if (IntroCutsceneScript.SCENE_LINK_FACE.equals(sceneId)) {
            return INTRO_LINK_FACE;
        }
        if (IntroCutsceneScript.SCENE_BEACH.equals(sceneId)) {
            return INTRO_BEACH;
        }
        if (IntroCutsceneScript.SCENE_TITLE.equals(sceneId)) {
            return TITLE;
        }
        return null;
    }

    public static boolean requiresTitleTileset(String sceneId) {
        return IntroCutsceneScript.SCENE_BEACH.equals(sceneId)
            || IntroCutsceneScript.SCENE_TITLE.equals(sceneId);
    }
}
