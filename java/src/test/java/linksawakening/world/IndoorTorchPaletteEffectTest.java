package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class IndoorTorchPaletteEffectTest {

    @Test
    void unlitTorchStartsAtTheSourceFourStepDarkness() {
        int[][] base = solidPalettes(31, 31, 31);
        IndoorTorchPaletteEffect effect = new IndoorTorchPaletteEffect();

        effect.reset(base, 1);

        assertEquals(0x04, effect.effectAddress());
        assertEquals(0x04, effect.targetAddress());
        assertEquals(rgb555(13, 13, 13), effect.palettes()[0][0]);
        assertEquals(rgb555(17, 17, 17), effect.palettes()[1][0]);
    }

    @Test
    void lightingTorchFollowsTheGbcElevenFrameTwoStepTransition() {
        IndoorTorchPaletteEffect effect = new IndoorTorchPaletteEffect();
        effect.reset(solidPalettes(31, 31, 31), 1);

        effect.igniteTorch();
        for (int tick = 0; tick < 9; tick++) {
            effect.tick(false, false);
        }
        assertEquals(0x04, effect.effectAddress());

        effect.tick(false, false);
        assertEquals(0x02, effect.effectAddress());
        assertEquals(rgb555(21, 21, 21), effect.palettes()[0][0]);
        assertEquals(rgb555(25, 25, 25), effect.palettes()[1][0]);

        effect.tick(false, false);
        assertEquals(0x00, effect.effectAddress());
        assertEquals(0x81, effect.paletteDataFlags());
        assertEquals(rgb555(31, 31, 31), effect.palettes()[0][0]);

        effect.tick(false, false);
        assertEquals(0x00, effect.paletteDataFlags());
    }

    @Test
    void dialogPausesThePaletteTransitionLikeGameplayPaletteHandler() {
        IndoorTorchPaletteEffect effect = new IndoorTorchPaletteEffect();
        effect.reset(solidPalettes(31, 31, 31), 1);
        effect.igniteTorch();

        for (int tick = 0; tick < 20; tick++) {
            effect.tick(true, false);
        }

        assertEquals(0x04, effect.effectAddress());
        assertEquals(0x0B, effect.transitionCountdown());
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
