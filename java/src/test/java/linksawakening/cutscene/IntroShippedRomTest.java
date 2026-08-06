package linksawakening.cutscene;

import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IntroShippedRomTest {

    @Test
    void shippedRomRecordsAndInitialFrameMatchTheIntroTables() {
        byte[] rom = loadRom();
        IntroRomData data = new IntroRomData(rom);

        assertEquals(6, data.shipTiles().size());
        assertEquals(0x1C, data.shipTiles().get(0).tileIndex());
        assertEquals(-8, data.shipTiles().get(2).xOffset());
        assertEquals(0x34, data.lightningTiles().get(0).get(0).tileIndex());
        assertEquals(0x03, data.marinVariants().get(0).firstAttributes());
        assertEquals(0x38, data.sparkleVariants().get(0).firstTileIndex());
        assertEquals(7, data.titleRows().size());
        assertArrayEquals(new int[] {
            0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7,
            0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF
        }, data.titleRows().get(0).tileBytes());

        BackgroundSceneLoader loader = new BackgroundSceneLoader(rom);
        IntroSequence sequence = new IntroSequence(rom, sceneId -> loader.load(
            BackgroundSceneCatalog.forCutsceneScene(sceneId)));
        assertEquals(22, sequence.snapshot().sprites().size());
        assertEquals(0x1C, sequence.snapshot().sprites().get(16).tileIndex());
        sequence.tick();
        assertEquals(1, sequence.snapshot().frameCounter());
    }

    @Test
    void lightningAppliesRomDefinedSeaPaletteModifierToTheDynamicSnapshot() {
        byte[] rom = loadRom();
        BackgroundSceneLoader loader = new BackgroundSceneLoader(rom);
        IntroSequence sequence = new IntroSequence(rom, sceneId -> loader.load(
            BackgroundSceneCatalog.forCutsceneScene(sceneId)));
        int[][] initial = sequence.snapshot().bgPalettes();

        for (int frame = 0; frame < 128; frame++) {
            sequence.tick();
        }

        assertTrue(sequence.snapshot().sprites().stream().anyMatch(sprite -> sprite.tileIndex() == 0x34));
        assertNotEquals(initial[0][0], sequence.snapshot().bgPalettes()[0][0]);
    }

    @Test
    void animatedFrameKeepsStaticBackgroundMapAndChangesOnlyAnimatedState() {
        byte[] rom = loadRom();
        BackgroundSceneLoader loader = new BackgroundSceneLoader(rom);
        BackgroundScene scene = loader.load(BackgroundSceneCatalog.INTRO_SEA);
        IntroSequence sequence = new IntroSequence(rom, sceneId -> scene);
        int[] initialMap = sequence.snapshot().tilemap();

        sequence.tick();
        assertArrayEquals(initialMap, sequence.snapshot().tilemap());
        assertTrue(sequence.snapshot().frameCounter() > 0);
    }

    @Test
    void titleDxFadeUsesTheRomPaletteRowsInTheDynamicSnapshot() {
        byte[] rom = loadRom();
        BackgroundSceneLoader loader = new BackgroundSceneLoader(rom);
        IntroSequence sequence = new IntroSequence(rom, sceneId -> loader.load(
            BackgroundSceneCatalog.forCutsceneScene(sceneId)));

        for (int frame = 0; frame < 5000 && !"TITLE_DX".equals(sequence.snapshot().substate()); frame++) {
            sequence.tick();
        }
        assertEquals("TITLE_DX", sequence.snapshot().substate());
        int[][] initial = sequence.snapshot().objPalettes();

        sequence.tick();
        sequence.tick();
        sequence.tick();
        sequence.tick();
        sequence.tick();
        sequence.tick();
        sequence.tick();
        sequence.tick();

        assertNotEquals(initial[6][1], sequence.snapshot().objPalettes()[6][1]);
    }

    private static byte[] loadRom() {
        try (InputStream stream = IntroShippedRomTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load test ROM", exception);
        }
    }
}
