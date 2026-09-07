package linksawakening.render;

import linksawakening.cutscene.IntroFrameSnapshot;
import linksawakening.cutscene.IntroSequence;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

final class DumpIntroFramesHarness {

    @Test
    void dumpFrames() throws Exception {
        byte[] rom = loadRom();
        GPU gpu = new GPU();
        gpu.loadIntroSequenceTiles(rom);
        BackgroundSceneLoader loader = new BackgroundSceneLoader(rom);
        IntroSequence sequence = new IntroSequence(rom, sceneId -> loader.load(
            BackgroundSceneCatalog.forCutsceneScene(sceneId)));
        GameFrameSceneBuilder builder = new GameFrameSceneBuilder();

        File outDir = new File(System.getProperty("java.io.tmpdir"), "intro-frames");
        outDir.mkdirs();

        int[] frames = {
            0, 60, 200, 400, 600, 800, 880,
            900, 1000, 1060, 1100, 1150, 1200,
            1250, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000,
            2100, 2200, 2300, 2400, 2500, 2600, 2700, 2800
        };
        int next = 0;
        for (int frame = 0; frame <= 2800 && next < frames.length; frame++) {
            if (frame == frames[next]) {
                String scene = sequence.snapshot().sceneId();
                boolean titleTiles = BackgroundSceneCatalog.requiresTitleTileset(scene);
                GPU frameGpu = titleTiles ? gpu : gpu;
                byte[] buffer = render(builder, frameGpu, sequence.snapshot());
                writePng(buffer, new File(outDir, String.format("f%04d_%s.png", frame, scene)));
                next++;
            }
            sequence.tick();
            String scene = sequence.snapshot().sceneId();
            if (BackgroundSceneCatalog.requiresTitleTileset(scene)) {
                gpu.loadTitleScreenTiles(rom);
            }
        }
    }

    private static byte[] render(GameFrameSceneBuilder builder, GPU gpu, IntroFrameSnapshot snapshot) {
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        builder.build(GameFrameState.empty()
            .withScreen(RenderScreen.CUTSCENE)
            .withIntroFrameSnapshot(snapshot))
            .drawTo(buffer, gpu);
        return buffer;
    }

    private static void writePng(byte[] argb, File file) throws Exception {
        BufferedImage image = new BufferedImage(Framebuffer.WIDTH, Framebuffer.HEIGHT,
            BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < Framebuffer.HEIGHT; y++) {
            for (int x = 0; x < Framebuffer.WIDTH; x++) {
                int i = (y * Framebuffer.WIDTH + x) * 4;
                int r = argb[i] & 0xFF;
                int g = argb[i + 1] & 0xFF;
                int b = argb[i + 2] & 0xFF;
                int a = argb[i + 3] & 0xFF;
                image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        ImageIO.write(image, "png", file);
    }

    private static byte[] loadRom() throws Exception {
        try (InputStream stream = DumpIntroFramesHarness.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
