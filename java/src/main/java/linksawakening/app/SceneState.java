package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

/** Mutable scene state owned by one session. */
public final class SceneState {
    public int[] currentTilemap;
    public int[] currentAttrmap;
    public int[][] bgPalettes;
    public int[][] objPalettes;
}
