package linksawakening.render;

import linksawakening.cutscene.IntroSprite;
import linksawakening.ui.FileMenuFrameSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GameFrameSceneBuilderFileMenuTest {

    @Test
    void fileMenuSnapshotBuildsBackgroundAndOamLayers() {
        int[] tilemap = new int[32 * 32];
        int[] attrmap = new int[32 * 32];
        int[][] palettes = { { 0, 0, 0, 0 } };
        FileMenuFrameSnapshot snapshot = new FileMenuFrameSnapshot(
            "selection", 0, false, false, 0, 0, new int[5],
            tilemap, attrmap, palettes, palettes,
            List.of(new IntroSprite(0, 0, 0, 0, false, false)));

        FrameScene scene = new GameFrameSceneBuilder().build(GameFrameState.empty()
            .withScreen(RenderScreen.FILE_MENU)
            .withFileMenuFrame(snapshot));

        assertEquals(2, scene.layerCount());
    }
}
