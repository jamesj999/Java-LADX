package linksawakening.render;

import linksawakening.cutscene.IntroFrameSnapshot;
import linksawakening.cutscene.IntroSequence;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

final class IntroEmulatorCapture {

    public static void main(String[] args) throws Exception {
        byte[] rom = loadRom();
        GPU gpu = new GPU();
        gpu.loadIntroSequenceTiles(rom);
        BackgroundSceneLoader loader = new BackgroundSceneLoader(rom);
        IntroSequence sequence = new IntroSequence(rom, sceneId -> loader.load(
            BackgroundSceneCatalog.forCutsceneScene(sceneId)));
        GameFrameSceneBuilder builder = new GameFrameSceneBuilder();

        File outDir = new File(args[0]);
        outDir.mkdirs();
        int count = args.length > 1 ? Integer.parseInt(args[1]) : 3800;
        try (java.io.PrintWriter metadata = new java.io.PrintWriter(new File(outDir, "frames.jsonl"))) {
            for (int frame = 0; frame < count; frame++) {
                IntroFrameSnapshot snapshot = sequence.snapshot();
                if (BackgroundSceneCatalog.requiresTitleTileset(snapshot.sceneId())) gpu.loadTitleScreenTiles(rom);
                byte[] pixels = render(builder, gpu, snapshot);
                writePng(pixels, new File(outDir, String.format("%04d.png", frame)));
                java.util.List<String> rain = new java.util.ArrayList<>();
                for (var sprite : snapshot.sprites()) {
                    if (sprite.tileIndex() == 0x28 || (sprite.tileIndex() >= 0x70 && sprite.tileIndex() <= 0x76))
                        rain.add("[" + sprite.x() + "," + sprite.y() + "," + sprite.tileIndex() + "]");
                }
                metadata.printf("{\"frame\":%d,\"stage\":\"%s\",\"scrollX\":%d,\"scrollY\":%d,\"rain\":%s}%n",
                    frame, snapshot.substate(), snapshot.scrollX(), snapshot.scrollY(), rain);
                sequence.tick();
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
        try (InputStream stream = IntroEmulatorCapture.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
