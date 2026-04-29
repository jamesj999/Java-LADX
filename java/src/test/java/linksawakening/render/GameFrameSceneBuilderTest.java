package linksawakening.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GameFrameSceneBuilderTest {

    @Test
    void emptyStateBuildsEmptyScene() {
        GameFrameSceneBuilder builder = new GameFrameSceneBuilder();

        FrameScene scene = builder.build(GameFrameState.empty());

        assertEquals(0, scene.layerCount());
    }

    @Test
    void backgroundStateBuildsBackgroundSceneLayer() {
        GameFrameSceneBuilder builder = new GameFrameSceneBuilder();
        int[] tilemap = new int[32 * 32];
        int[] attrmap = new int[32 * 32];
        int[][] palettes = { { 0, 0, 0, 0 } };

        FrameScene scene = builder.build(GameFrameState.empty()
            .withScreen(RenderScreen.TITLE)
            .withBackground(tilemap, attrmap, palettes, null)
        );

        assertEquals(1, scene.layerCount());
    }
}
