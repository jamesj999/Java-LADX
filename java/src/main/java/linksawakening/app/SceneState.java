package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

/** Mutable scene state owned by one session. */
public final class SceneState {
    public void apply(linksawakening.scene.BackgroundScene scene) {
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
