package linksawakening.scene;

import linksawakening.cutscene.IntroCutsceneScript;

public final class BackgroundSceneCatalog {

    public static final String FILE_SELECTION_SCENE = "selection";
    public static final String FILE_SELECTION_COMMANDS_SCENE = "selection_commands";
    public static final String FILE_CREATION_SCENE = "creation";

    public static final BackgroundSceneSpec TITLE =
        new BackgroundSceneSpec(0x08, 0x710A, 0x24, 0x7BF0, 0x21, 0x7DEE);
    public static final BackgroundSceneSpec INTRO_SEA =
        new BackgroundSceneSpec(0x08, 0x6C37, 0x24, 0x5D18, 0x21, 0x7536);
    public static final BackgroundSceneSpec INTRO_LINK_FACE =
        new BackgroundSceneSpec(0x08, 0x6D80, 0x24, 0x5D69, 0x21, 0x7536);
    public static final BackgroundSceneSpec INTRO_BEACH =
        new BackgroundSceneSpec(0x08, 0x6F8B, 0x24, 0x7BA7, 0x21, 0x7DEE);
    public static final BackgroundSceneSpec FILE_SELECTION =
        new BackgroundSceneSpec(0x20, 0x6336, 0x24, 0x5F80, 0x21, 0x7536);
    public static final BackgroundSceneSpec FILE_SELECTION_COMMANDS =
        new BackgroundSceneSpec(0x20, 0x6328, 0x24, 0x5F74, 0x21, 0x7536);
    public static final BackgroundSceneSpec FILE_CREATION =
        new BackgroundSceneSpec(0x20, 0x644D, 0x24, 0x6045, 0x21, 0x7536);
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

    public static BackgroundSceneSpec forFileMenuScene(String sceneId) {
        if (FILE_SELECTION_SCENE.equals(sceneId)) {
            return FILE_SELECTION;
        }
        if (FILE_SELECTION_COMMANDS_SCENE.equals(sceneId)) {
            return FILE_SELECTION_COMMANDS;
        }
        if (FILE_CREATION_SCENE.equals(sceneId)) {
            return FILE_CREATION;
        }
        return null;
    }
}
