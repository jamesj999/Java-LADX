package linksawakening.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import linksawakening.cutscene.IntroFrameSnapshot;
import linksawakening.cutscene.IntroSprite;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.room.RoomConstants;
import linksawakening.world.room.RoomRenderSnapshot;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import org.junit.jupiter.api.Test;

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
        int[][] palettes = {{0, 0, 0, 0}};

        FrameScene scene = builder.build(
                GameFrameState.empty().withScreen(RenderScreen.TITLE).withBackground(tilemap, attrmap, palettes, null));

        assertEquals(1, scene.layerCount());
    }

    @Test
    void backgroundStateUsesTheDynamicIntroSnapshotAndSpriteLayer() {
        GameFrameSceneBuilder builder = new GameFrameSceneBuilder();
        int[] tilemap = new int[32 * 32];
        int[] attrmap = new int[32 * 32];
        int[][] palettes = {{0, 0, 0, 0}};
        IntroFrameSnapshot snapshot = new IntroFrameSnapshot(
                "intro-sea",
                "SEA",
                3,
                4,
                0,
                null,
                0,
                tilemap,
                attrmap,
                palettes,
                palettes,
                List.of(new IntroSprite(0, 0, 0, 0, false, false)),
                0);

        FrameScene scene = builder.build(GameFrameState.empty()
                .withScreen(RenderScreen.CUTSCENE)
                .withBackground(tilemap, attrmap, palettes, palettes)
                .withIntroFrameSnapshot(snapshot));

        assertEquals(2, scene.layerCount());
    }

    @Test
    void overworldStateBuildsFromRoomSnapshotWithoutBackgroundScenePalettes() {
        GameFrameSceneBuilder builder = new GameFrameSceneBuilder();
        int[] tileIds = new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT];
        int[] tileAttrs = new int[tileIds.length];
        int[][] palettes = {{0, 0, 0, 0}};

        FrameScene scene = builder.build(GameFrameState.empty()
                .withScreen(RenderScreen.OVERWORLD)
                .withRoom(
                        new RoomRenderSnapshot(tileIds, tileAttrs, palettes),
                        new ScrollController(),
                        new TransitionController()));

        assertEquals(1, scene.layerCount());
    }

    @Test
    void wakeBedPresentationSuppressesOrdinaryLinkLayer() {
        int[] tileIds = new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT];
        Link link = new Link(
                new InputState(),
                new InputConfig(1, 2, 3, 4, 5, 6, 7),
                null,
                null,
                null,
                new PlayerState(),
                new ItemRegistry());
        link.showMarinWakeUpBed(3);

        FrameScene scene = new GameFrameSceneBuilder()
                .build(GameFrameState.empty()
                        .withScreen(RenderScreen.OVERWORLD)
                        .withRoom(
                                new RoomRenderSnapshot(tileIds, tileIds, new int[][] {{0, 0, 0, 0}}),
                                new ScrollController(),
                                new TransitionController())
                        .withLink(link));

        assertEquals(1, scene.layerCount());
    }

    @Test
    void overworldStateAddsEntityLayerWhenRoomCarriesEntitySelection() {
        GameFrameSceneBuilder builder = new GameFrameSceneBuilder();
        int[] tileIds = new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT];
        int[] tileAttrs = new int[tileIds.length];
        int[][] palettes = {{0, 0, 0, 0}};
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < 0x10; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        RoomEntitySnapshot entities = new RoomEntitySnapshot(
                slots,
                new EntitySpriteSelection(
                        EntityRoomLoader.RoomTable.OVERWORLD,
                        0,
                        0,
                        new int[] {0xFF, 0xFF, 0xFF, 0xFF},
                        true,
                        new int[][] {{0, 0, 0, 0}}));

        FrameScene scene = builder.build(GameFrameState.empty()
                .withScreen(RenderScreen.OVERWORLD)
                .withRoom(
                        new RoomRenderSnapshot(tileIds, tileAttrs, palettes, entities),
                        new ScrollController(),
                        new TransitionController()));

        assertEquals(2, scene.layerCount());
    }
}
