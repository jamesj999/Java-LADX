package linksawakening.app.state;

import linksawakening.scene.BackgroundScene;

/** Mutable scene state owned by one session. */
public final class SceneState {
    public void apply(BackgroundScene scene) {
        currentTilemap = scene.tilemap();
        currentAttrmap = scene.attrmap();
        bgPalettes = scene.palettes();
        objPalettes = scene.objectPalettes();
    }

    public int[] currentTilemap;
    public int[] currentAttrmap;
    public int[][] bgPalettes;
    public int[][] objPalettes;
}
