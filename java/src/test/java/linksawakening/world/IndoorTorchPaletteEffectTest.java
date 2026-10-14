package linksawakening.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import linksawakening.rom.RomBank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IndoorTorchPaletteEffectTest {

    @ParameterizedTest
    @CsvSource({"1, 0, 0x53E3", "2, 0, 0x53EB", "1, 11, 0x53DB"})
    void eachLightingLevelReadsItsEightScalingFactorsFromTheRom(
        int torches, int fadeTicks, int tableAddress) {
        byte[] rom = loadRom();
        int offset = RomBank.romOffset(0x21, tableAddress);
        for (int palette = 0; palette < 8; palette++) {
            rom[offset + palette] = (byte) (palette + 1);
        }
        IndoorTorchPaletteEffect effect = new IndoorTorchPaletteEffect(rom);
        effect.reset(solidPalettes(31, 17, 1), torches);
        if (fadeTicks != 0) {
            effect.igniteTorch();
            for (int tick = 0; tick < fadeTicks; tick++) {
                effect.tick(false, false);
            }
        }
        for (int palette = 0; palette < 8; palette++) {
            int factor = palette + 1;
            assertEquals(rgb555(31 * factor / 16, 17 * factor / 16, 0),
                effect.palettes()[palette][0], "Palette " + palette);
        }
    }

    @Test
    void scalingPreservesTheRomTruncationForAllRgb555ChannelValues() {
        byte[] rom = loadRom();
        for (int channel = 0; channel < 32; channel++) {
            IndoorTorchPaletteEffect effect = new IndoorTorchPaletteEffect(rom);
            effect.reset(solidPalettes(channel, 31 - channel, channel), 1);
            for (int palette = 0; palette < 8; palette++) {
                int factor = Byte.toUnsignedInt(rom[0x853E3 + palette]);
                // $21:54F9 forms channel<<4, adds it factor times, then
                // returns the high byte. Fractional channel bits are discarded.
                int red = multiplyLikeRom(channel, factor);
                int green = multiplyLikeRom(31 - channel, factor);
                assertEquals(rgb555(red, green, red), effect.palettes()[palette][0]);
            }
        }
    }

    private static int multiplyLikeRom(int channel, int factor) {
        int accumulator = 0;
        for (int iteration = 0; iteration < factor; iteration++) {
            accumulator = (accumulator + (channel << 4)) & 0xFFFF;
        }
        return accumulator >>> 8;
    }

    @Test
    void unlitTorchStartsAtTheSourceFourStepDarkness() {
        int[][] base = solidPalettes(31, 31, 31);
        IndoorTorchPaletteEffect effect = new IndoorTorchPaletteEffect(loadRom());

        effect.reset(base, 1);

        assertEquals(0x04, effect.effectAddress());
        assertEquals(0x04, effect.targetAddress());
        assertEquals(rgb555(13, 13, 13), effect.palettes()[0][0]);
        assertEquals(rgb555(17, 17, 17), effect.palettes()[1][0]);
    }

    @Test
    void lightingTorchAdvancesOncePerCycleAndCopiesPalettesInTwoHalves() {
        IndoorTorchPaletteEffect effect = new IndoorTorchPaletteEffect(loadRom());
        effect.reset(solidPalettes(31, 31, 31), 1);

        effect.igniteTorch();
        for (int tick = 0; tick < 9; tick++) {
            effect.tick(false, false);
        }
        assertEquals(0x04, effect.effectAddress());
        assertEquals(0, effect.paletteDataFlags());

        effect.tick(false, false);
        assertEquals(0x02, effect.effectAddress());
        assertEquals(rgb555(21, 21, 21), effect.palettes()[0][0]);
        assertEquals(rgb555(25, 25, 25), effect.palettes()[1][0]);
        assertEquals(rgb555(13, 13, 13), effect.palettes()[4][0]);

        effect.tick(false, false);
        assertEquals(0x02, effect.effectAddress());
        assertEquals(0x81, effect.paletteDataFlags());
        assertEquals(rgb555(21, 21, 21), effect.palettes()[4][0]);
        assertTrue(effect.transitionActive());

        for (int tick = 0; tick < 10; tick++) {
            effect.tick(false, false);
        }
        assertEquals(0x00, effect.effectAddress());
        assertEquals(rgb555(31, 31, 31), effect.palettes()[0][0]);
        assertEquals(rgb555(21, 21, 21), effect.palettes()[4][0]);
        effect.tick(false, false);
        assertEquals(rgb555(31, 31, 31), effect.palettes()[4][0]);
        assertFalse(effect.transitionActive());

        effect.tick(false, false);
        assertEquals(0x00, effect.paletteDataFlags());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void nondefaultLinkMotionPausesThePaletteCountdown(int motion) {
        IndoorTorchPaletteEffect effect = new IndoorTorchPaletteEffect(loadRom());
        effect.reset(solidPalettes(31, 31, 31), 1);
        effect.igniteTorch();
        for (int tick = 0; tick < 20; tick++) {
            effect.tick(false, false, motion, 0);
        }
        assertEquals(0x0B, effect.transitionCountdown());
        assertEquals(4, effect.effectAddress());
        assertEquals(0, effect.paletteDataFlags());
    }

    @Test
    void extinctionCapsTheTargetWithoutRestartingTheCountdown() {
        IndoorTorchPaletteEffect effect = new IndoorTorchPaletteEffect(loadRom());
        effect.reset(solidPalettes(31, 31, 31), 2);
        effect.extinguishTorch();
        effect.tick(false, false);
        effect.extinguishTorch();
        assertEquals(0x0C, effect.targetAddress());
        assertEquals(0x0A, effect.transitionCountdown());
    }

    @Test
    void revolvingDoorExtinctionUpdatesTheTargetWithoutStartingAFade() {
        IndoorTorchPaletteEffect effect = new IndoorTorchPaletteEffect(loadRom());
        effect.reset(solidPalettes(31, 31, 31), 0);
        effect.extinguishTorch(false);
        assertEquals(4, effect.targetAddress());
        assertFalse(effect.transitionActive());
        effect.tick(false, false, 5, 0);
        assertEquals(0, effect.effectAddress());
        assertEquals(0, effect.transitionCountdown());
    }

    @Test
    void turtleRockReloadsTheSubsequentCycleAtTenFrames() {
        IndoorTorchPaletteEffect effect = new IndoorTorchPaletteEffect(loadRom());
        effect.reset(solidPalettes(31, 31, 31), 1);
        effect.igniteTorch();
        for (int tick = 0; tick < 11; tick++) {
            effect.tick(false, false, 0, 7);
        }
        assertEquals(2, effect.effectAddress());
        assertEquals(10, effect.transitionCountdown());
        for (int tick = 0; tick < 9; tick++) {
            effect.tick(false, false, 0, 7);
        }
        assertEquals(0, effect.effectAddress());
    }

    @Test
    void dialogPausesThePaletteTransitionLikeGameplayPaletteHandler() {
        IndoorTorchPaletteEffect effect = new IndoorTorchPaletteEffect(loadRom());
        effect.reset(solidPalettes(31, 31, 31), 1);
        effect.igniteTorch();

        for (int tick = 0; tick < 20; tick++) {
            effect.tick(true, false);
        }

        assertEquals(0x04, effect.effectAddress());
        assertEquals(0x0B, effect.transitionCountdown());
    }

    private static byte[] loadRom() {
        try (var stream = IndoorTorchPaletteEffectTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new AssertionError("Missing ROM resource");
            }
            return stream.readAllBytes();
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static int[][] solidPalettes(int red, int green, int blue) {
        int[][] palettes = new int[8][4];
        for (int palette = 0; palette < palettes.length; palette++) {
            for (int color = 0; color < palettes[palette].length; color++) {
                palettes[palette][color] = rgb555(red, green, blue);
            }
        }
        return palettes;
    }

    private static int rgb555(int red, int green, int blue) {
        return red * 255 / 31 << 16 | green * 255 / 31 << 8 | blue * 255 / 31;
    }
}
