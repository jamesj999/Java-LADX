package linksawakening.render;

import linksawakening.entity.EntitySpriteSelection;
import linksawakening.world.RoomConstants;
import linksawakening.world.EntityRoomLoader;
import linksawakening.world.RoomEntity;
import linksawakening.world.RoomEntitySnapshot;
import linksawakening.world.RoomRenderSnapshot;
import linksawakening.world.ScrollController;
import linksawakening.world.TransitionController;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    void overworldStateAddsEntityLayerWhenRoomCarriesEntitySelection() {
        GameFrameSceneBuilder builder = new GameFrameSceneBuilder();
        int[] tileIds = new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT];
        int[] tileAttrs = new int[tileIds.length];
        int[][] palettes = { { 0, 0, 0, 0 } };
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < 0x10; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        RoomEntitySnapshot entities = new RoomEntitySnapshot(slots,
            new EntitySpriteSelection(EntityRoomLoader.RoomTable.OVERWORLD, 0, 0,
                new int[] { 0xFF, 0xFF, 0xFF, 0xFF }, true,
                new int[][] { { 0, 0, 0, 0 } }));

        FrameScene scene = builder.build(GameFrameState.empty()
            .withScreen(RenderScreen.OVERWORLD)
            .withRoom(new RoomRenderSnapshot(tileIds, tileAttrs, palettes, entities),
                new ScrollController(), new TransitionController())
        );

        assertEquals(2, scene.layerCount());
    }
}
