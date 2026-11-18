package linksawakening.app.state;

import linksawakening.scene.BackgroundScene;

/** Mutable scene state owned by one session. */
public final class SceneState {

    public int[] currentTilemap;
    public int[] currentAttrmap;
    public int[][] bgPalettes;
    public int[][] objPalettes;

    public void apply(BackgroundScene scene) {
        currentTilemap = scene.tilemap();
        currentAttrmap = scene.attrmap();
        bgPalettes = scene.palettes();
        objPalettes = scene.objectPalettes();
    }

    public int[] currentTilemap() {
        return currentTilemap;
    }

    public int[] currentAttrmap() {
        return currentAttrmap;
    }

    public int[][] bgPalettes() {
        return bgPalettes;
    }

    public int[][] objPalettes() {
        return objPalettes;
    }
    /** Installs a decoded scene's planes and palettes in their source order. */
    public void setPlanes(int[] tilemap, int[] attrmap, int[][] backgroundPalettes, int[][] objectPalettes) {
        currentTilemap = tilemap;
        currentAttrmap = attrmap;
        bgPalettes = backgroundPalettes;
        objPalettes = objectPalettes;
    }
}
