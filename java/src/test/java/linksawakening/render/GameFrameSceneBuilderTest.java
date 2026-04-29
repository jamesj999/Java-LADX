package linksawakening.render;

import linksawakening.world.RoomConstants;
import linksawakening.world.RoomRenderSnapshot;
import linksawakening.world.ScrollController;
import linksawakening.world.TransitionController;
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

    @Test
    void overworldStateBuildsFromRoomSnapshotWithoutBackgroundScenePalettes() {
        GameFrameSceneBuilder builder = new GameFrameSceneBuilder();
        int[] tileIds = new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT];
        int[] tileAttrs = new int[tileIds.length];
        int[][] palettes = { { 0, 0, 0, 0 } };

        FrameScene scene = builder.build(GameFrameState.empty()
            .withScreen(RenderScreen.OVERWORLD)
            .withRoom(new RoomRenderSnapshot(tileIds, tileAttrs, palettes),
                new ScrollController(), new TransitionController())
        );

        assertEquals(1, scene.layerCount());
    }
}
