package linksawakening.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import linksawakening.cutscene.IntroSequence;
import linksawakening.cutscene.IntroSprite;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.ui.FileMenuFrameSnapshot;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class GameFrameSceneBuilderFileMenuTest {

    @Test
    void fileMenuIgnoresRetainedIntroScanlinesAndTileUploads() {
        IntroSequence intro = new IntroSequence();
        for (int i = 0; i < 128; i++) intro.tick();
        int[] map = new int[32 * 32];
        int[][] palettes = {{0, 0, 0, 0}};
        FileMenuFrameSnapshot menu = new FileMenuFrameSnapshot.Builder()
                .sceneId("selection")
                .selectedSlot(0)
                .commandRowArrowShifted(false)
                .creation(false)
                .selectedCharacter(0)
                .namePosition(0)
                .nameBytes(new int[5])
                .tilemap(map)
                .attrmap(map)
                .bgPalettes(palettes)
                .objectPalettes(palettes)
                .sprites(List.of())
                .build();
        GPU gpu = new GPU();
        FrameScene scene = new GameFrameSceneBuilder()
                .build(GameFrameState.empty()
                        .withScreen(RenderScreen.FILE_MENU)
                        .withIntroFrameSnapshot(intro.snapshot())
                        .withFileMenuFrame(menu));
        scene.drawTo(new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4], gpu, TestDrawObservers.CURRENT.get());
        assertEquals(2, scene.layerCount());
        for (int tile = 0x90; tile < 0x98; tile++)
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 8; x++) assertEquals(0, gpu.getTile(tile).getPixel(x, y));
    }

    @Test
    void fileMenuSnapshotBuildsBackgroundAndOamLayers() {
        int[] tilemap = new int[32 * 32];
        int[] attrmap = new int[32 * 32];
        int[][] palettes = {{0, 0, 0, 0}};
        FileMenuFrameSnapshot snapshot = new FileMenuFrameSnapshot.Builder()
                .sceneId("selection")
                .selectedSlot(0)
                .commandRowArrowShifted(false)
                .creation(false)
                .selectedCharacter(0)
                .namePosition(0)
                .nameBytes(new int[5])
                .tilemap(tilemap)
                .attrmap(attrmap)
                .bgPalettes(palettes)
                .objectPalettes(palettes)
                .sprites(List.of(new IntroSprite(0, 0, 0, 0, false, false)))
                .build();

        FrameScene scene = new GameFrameSceneBuilder()
                .build(GameFrameState.empty().withScreen(RenderScreen.FILE_MENU).withFileMenuFrame(snapshot));

        assertEquals(2, scene.layerCount());
    }
}
