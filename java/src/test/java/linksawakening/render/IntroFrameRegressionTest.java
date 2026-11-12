package linksawakening.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import linksawakening.cutscene.IntroFrameSnapshot;
import linksawakening.cutscene.IntroSequence;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class IntroFrameRegressionTest {

    @Test
    void changingOnlyTheIntroFrameChangesTheRenderedOpeningFrame() {
        byte[] rom = loadRom();
        GPU gpu = new GPU();
        gpu.loadIntroSequenceTiles(rom);
        BackgroundSceneLoader loader = new BackgroundSceneLoader(rom);
        IntroSequence sequence =
                new IntroSequence(rom, sceneId -> loader.load(BackgroundSceneCatalog.forCutsceneScene(sceneId)));
        GameFrameSceneBuilder builder = new GameFrameSceneBuilder();

        IntroFrameSnapshot initial = sequence.snapshot();
        byte[] initialFrame = render(builder, gpu, initial);
        for (int frame = 0; frame < 128; frame++) {
            sequence.tick();
        }
        IntroFrameSnapshot lightning = sequence.snapshot();
        byte[] lightningFrame = render(builder, gpu, lightning);

        assertArrayEquals(initial.tilemap(), lightning.tilemap());
        assertFalse(Arrays.equals(initialFrame, lightningFrame));
    }

    private static byte[] render(GameFrameSceneBuilder builder, GPU gpu, IntroFrameSnapshot snapshot) {
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        builder.build(GameFrameState.empty().withScreen(RenderScreen.CUTSCENE).withIntroFrameSnapshot(snapshot))
                .drawTo(buffer, gpu, TestDrawObservers.CURRENT.get());
        return buffer;
    }

    private static byte[] loadRom() {
        try (InputStream stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load test ROM", exception);
        }
    }
}
