package linksawakening.render;

import linksawakening.cutscene.IntroSequence;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Golden pixels recorded by running the shipped ROM, independently of the Java renderer. */
final class IntroEmulatorParityTest {
    @Test
    void matchesEmulatorAcrossFadesBeachPanTitleAndFrameBoundaries() throws Exception {
        byte[] rom;
        Map<Integer, String> golden = new TreeMap<>();
        try (var input = getClass().getResourceAsStream("/rom/azle.gbc")) {
            rom = input.readAllBytes();
        }
        try (var input = getClass().getResourceAsStream("/intro/emulator-rgb555-sha256.csv")) {
            for (String line : new String(input.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] fields = line.split(",");
                golden.put(Integer.parseInt(fields[0]), fields[1]);
            }
        }
        assertEquals("c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60", hash(rom),
            "Emulator fixtures are tied to the shipped ROM revision");
        GPU gpu = new GPU();
        gpu.loadIntroSequenceTiles(rom);
        BackgroundSceneLoader loader = new BackgroundSceneLoader(rom);
        IntroSequence sequence = new IntroSequence(rom,
            id -> loader.load(BackgroundSceneCatalog.forCutsceneScene(id)));
        GameFrameSceneBuilder builder = new GameFrameSceneBuilder();
        int last = golden.keySet().stream().mapToInt(Integer::intValue).max().orElseThrow();
        for (int frame = 0; frame <= last; frame++) {
            if (BackgroundSceneCatalog.requiresTitleTileset(sequence.sceneId())) gpu.loadTitleScreenTiles(rom);
            if (golden.containsKey(frame)) {
                byte[] rgba = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
                builder.build(GameFrameState.empty().withScreen(RenderScreen.CUTSCENE)
                    .withIntroFrameSnapshot(sequence.snapshot())).drawTo(rgba, gpu);
                byte[] rgb555 = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 3];
                for (int pixel = 0; pixel < Framebuffer.WIDTH * Framebuffer.HEIGHT; pixel++) {
                    for (int channel = 0; channel < 3; channel++)
                        rgb555[pixel * 3 + channel] = (byte) ((rgba[pixel * 4 + channel] & 255) >>> 3);
                }
                assertEquals(golden.get(frame), hash(rgb555), "Emulator parity at intro frame " + frame);
            }
            sequence.tick();
        }
    }

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
