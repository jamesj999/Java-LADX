package linksawakening.scene;

import linksawakening.cutscene.IntroCutsceneScript;
import linksawakening.rom.RomSymbols;

public final class BackgroundSceneCatalog {

    public static final String FILE_SELECTION_SCENE = "selection";
    public static final String FILE_SELECTION_COMMANDS_SCENE = "selection_commands";
    public static final String FILE_CREATION_SCENE = "creation";
    public static final String FILE_ERASE_SCENE = "erase";
    public static final String FILE_COPY_SCENE = "copy";
    public static final String FILE_SAVE_SCENE = "save";

    public static final BackgroundSceneSpec TITLE =
        new BackgroundSceneSpec(RomSymbols.TitleTilemap.bank(), RomSymbols.TitleTilemap.address(), RomSymbols.TitleAttrmap.bank(), RomSymbols.TitleAttrmap.address(), RomSymbols.Title_BG_Palettes.bank(), RomSymbols.Title_BG_Palettes.address());
    public static final BackgroundSceneSpec INTRO_SEA =
        new BackgroundSceneSpec(RomSymbols.IntroSeaCGBTilemap.bank(), RomSymbols.IntroSeaCGBTilemap.address(), RomSymbols.IntroSeaCGBAttrmap.bank(), RomSymbols.IntroSeaCGBAttrmap.address(), RomSymbols.Data_021_7536.bank(), RomSymbols.Data_021_7536.address());
    public static final BackgroundSceneSpec INTRO_LINK_FACE =
        new BackgroundSceneSpec(RomSymbols.IntroLinkFaceTilemap.bank(), RomSymbols.IntroLinkFaceTilemap.address(), RomSymbols.IntroLinkFaceAttrmap.bank(), RomSymbols.IntroLinkFaceAttrmap.address(), RomSymbols.Data_021_7536.bank(), RomSymbols.Data_021_7536.address());
    public static final BackgroundSceneSpec INTRO_BEACH =
        new BackgroundSceneSpec(RomSymbols.IntroBeachTilemap.bank(), RomSymbols.IntroBeachTilemap.address(), RomSymbols.IntroBeachAttrmap.bank(), RomSymbols.IntroBeachAttrmap.address(), RomSymbols.Title_BG_Palettes.bank(), RomSymbols.Title_BG_Palettes.address());
    // Bank $20 holds pointers; the command streams themselves live in bank $08.
    // File menus use Data_021_75B6 (the intro uses Data_021_7536).
    public static final BackgroundSceneSpec FILE_SELECTION =
        new BackgroundSceneSpec(RomSymbols.MenuFileSelectionTilemap.bank(), RomSymbols.MenuFileSelectionTilemap.address(), RomSymbols.MenuFileSelectionAttrmap.bank(), RomSymbols.MenuFileSelectionAttrmap.address(), RomSymbols.Data_021_75B6.bank(), RomSymbols.Data_021_75B6.address());
    public static final BackgroundSceneSpec FILE_SELECTION_COMMANDS =
        new BackgroundSceneSpec(RomSymbols.MenuFileSelectionCommandsTilemap.bank(), RomSymbols.MenuFileSelectionCommandsTilemap.address(), RomSymbols.MenuFileSelectionCommandsAttrmap.bank(), RomSymbols.MenuFileSelectionCommandsAttrmap.address(), RomSymbols.Data_021_75B6.bank(), RomSymbols.Data_021_75B6.address());
    public static final BackgroundSceneSpec FILE_CREATION =
        new BackgroundSceneSpec(RomSymbols.MenuFileCreationTilemap.bank(), RomSymbols.MenuFileCreationTilemap.address(), RomSymbols.MenuFileCreationAttrmap.bank(), RomSymbols.MenuFileCreationAttrmap.address(), RomSymbols.Data_021_75B6.bank(), RomSymbols.Data_021_75B6.address());
    public static final BackgroundSceneSpec FILE_ERASE =
        new BackgroundSceneSpec(RomSymbols.MenuFileEraseTilemap.bank(), RomSymbols.MenuFileEraseTilemap.address(), RomSymbols.MenuFileEraseAttrmap.bank(), RomSymbols.MenuFileEraseAttrmap.address(), RomSymbols.Data_021_75B6.bank(), RomSymbols.Data_021_75B6.address());
    public static final BackgroundSceneSpec FILE_COPY =
        new BackgroundSceneSpec(RomSymbols.MenuFileCopyTilemap.bank(), RomSymbols.MenuFileCopyTilemap.address(), RomSymbols.MenuFileCopyAttrmap.bank(), RomSymbols.MenuFileCopyAttrmap.address(), RomSymbols.Data_021_75B6.bank(), RomSymbols.Data_021_75B6.address());
    public static final BackgroundSceneSpec FILE_SAVE =
        new BackgroundSceneSpec(RomSymbols.MenuFileSaveTilemap.bank(), RomSymbols.MenuFileSaveTilemap.address(), RomSymbols.MenuFileSaveAttrmap.bank(), RomSymbols.MenuFileSaveAttrmap.address(), RomSymbols.Data_021_75B6.bank(), RomSymbols.Data_021_75B6.address());
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
        if (FILE_ERASE_SCENE.equals(sceneId)) {
            return FILE_ERASE;
        }
        if (FILE_COPY_SCENE.equals(sceneId)) {
            return FILE_COPY;
        }
        if (FILE_SAVE_SCENE.equals(sceneId)) {
            return FILE_SAVE;
        }
        return null;
    }
}
