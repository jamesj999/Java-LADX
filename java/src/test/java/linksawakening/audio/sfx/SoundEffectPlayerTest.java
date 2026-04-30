package linksawakening.audio.sfx;

import linksawakening.audio.apu.GameBoyApu;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SoundEffectPlayerTest {
    @Test
    void triggeringNoiseEffectEnablesApuAndWritesInitialNoiseRegisters() {
        GameBoyApu apu = new GameBoyApu(48_000);
        SoundEffectPlayer player = new SoundEffectPlayer(apu);
        SoundEffect swordSwing = SoundEffectCatalog.firstPass()
                .find(SoundEffectNamespace.NOISE, 0x02)
                .orElseThrow();

        player.play(swordSwing);

        assertTrue(player.isPlaying());
        assertEquals(0x80, apu.readRegister(GameBoyApu.NR52) & 0x80);
        assertEquals(0x77, apu.readRegister(GameBoyApu.NR50));
        assertEquals(0xFF, apu.readRegister(GameBoyApu.NR51));
        assertEquals(0x00, apu.readRegister(GameBoyApu.NR41));
        assertEquals(0x40, apu.readRegister(GameBoyApu.NR42));
        assertEquals(0x21, apu.readRegister(GameBoyApu.NR43));
        assertEquals(0x80, apu.readRegister(GameBoyApu.NR44) & 0xC0);
    }

    @Test
    void noiseSequenceAdvancesOnFrameTicksAndStopsAfterFinalStep() {
        GameBoyApu apu = new GameBoyApu(48_000);
        SoundEffectPlayer player = new SoundEffectPlayer(apu);
        SoundEffect swordSwing = SoundEffectCatalog.firstPass()
                .find(SoundEffectNamespace.NOISE, 0x02)
                .orElseThrow();

        player.play(swordSwing);
        player.tick60Hz();

        assertEquals(0x22, apu.readRegister(GameBoyApu.NR43));
        assertTrue(player.isPlaying());

        for (int i = 0; i < 6; i++) {
            player.tick60Hz();
        }

        assertFalse(player.isPlaying());
        assertEquals(0xA0, apu.readRegister(GameBoyApu.NR42));
    }

    @Test
    void triggeringWaveEffectWritesWaveChannelRegisters() {
        GameBoyApu apu = new GameBoyApu(48_000);
        SoundEffectPlayer player = new SoundEffectPlayer(apu);
        SoundEffect textPrint = SoundEffectCatalog.firstPass()
                .find(SoundEffectNamespace.WAVE, 0x0F)
                .orElseThrow();

        player.play(textPrint);

        assertEquals(0x80, apu.readRegister(GameBoyApu.NR30));
        assertEquals(0xFB, apu.readRegister(GameBoyApu.NR31));
        assertEquals(0x60, apu.readRegister(GameBoyApu.NR32));
        assertEquals(0xD2, apu.readRegister(GameBoyApu.NR33));
        assertEquals(0xC7, apu.readRegister(GameBoyApu.NR34));

        player.tick60Hz();

        assertEquals(0x40, apu.readRegister(GameBoyApu.NR32));
    }

    @Test
    void rupeeStartsWithInitialFullWriteThenAlternatesContinuationFrequencies() {
        GameBoyApu apu = new GameBoyApu(48_000);
        SoundEffectPlayer player = new SoundEffectPlayer(apu);
        SoundEffect rupee = SoundEffectCatalog.firstPass()
                .find(SoundEffectNamespace.WAVE, 0x05)
                .orElseThrow();

        player.play(rupee);

        assertEquals(0x20, apu.readRegister(GameBoyApu.NR32));
        assertEquals(0xDA, apu.readRegister(GameBoyApu.NR33));
        assertEquals(0xC7, apu.readRegister(GameBoyApu.NR34));

        player.tick60Hz();
        assertEquals(0xDA, apu.readRegister(GameBoyApu.NR33));

        player.tick60Hz();
        assertEquals(0x20, apu.readRegister(GameBoyApu.NR32));
        assertEquals(0xEA, apu.readRegister(GameBoyApu.NR33));
        assertEquals(0xC7, apu.readRegister(GameBoyApu.NR34));

        player.tick60Hz();
        player.tick60Hz();
        assertEquals(0x40, apu.readRegister(GameBoyApu.NR32));
        assertEquals(0xDA, apu.readRegister(GameBoyApu.NR33));
    }

    @Test
    void rupeeLoadsItsDisassemblyWavePatternBeforeTriggeringChannel3() {
        GameBoyApu apu = new GameBoyApu(48_000);
        SoundEffectPlayer player = new SoundEffectPlayer(apu);
        SoundEffect rupee = SoundEffectCatalog.firstPass()
                .find(SoundEffectNamespace.WAVE, 0x05)
                .orElseThrow();

        player.play(rupee);

        int[] expectedData01F639C = {
                0x00, 0x22, 0x44, 0x66, 0x88, 0xAA, 0xCC, 0xCC,
                0x00, 0x22, 0x44, 0x66, 0x88, 0xAA, 0xCC, 0xCC
        };
        for (int i = 0; i < expectedData01F639C.length; i++) {
            assertEquals(expectedData01F639C[i], apu.readWaveRam(i), "wave RAM byte " + i);
        }
    }

    @Test
    void lowHeartsAndTextPrintLoadTheirDisassemblyWavePatternBeforeTriggeringChannel3() {
        GameBoyApu apu = new GameBoyApu(48_000);
        SoundEffectPlayer player = new SoundEffectPlayer(apu);
        SoundEffect lowHearts = SoundEffectCatalog.firstPass()
                .find(SoundEffectNamespace.WAVE, 0x04)
                .orElseThrow();
        SoundEffect textPrint = SoundEffectCatalog.firstPass()
                .find(SoundEffectNamespace.WAVE, 0x0F)
                .orElseThrow();

        player.play(lowHearts);
        assertWavePattern63Cc(apu);

        apu.reset();
        player.play(textPrint);
        assertWavePattern63Cc(apu);
    }

    @Test
    void explosionContinuationUpdatesNoiseFrequencyWithoutRetriggering() {
        GameBoyApu apu = new GameBoyApu(48_000);
        SoundEffectPlayer player = new SoundEffectPlayer(apu);
        SoundEffect explosion = SoundEffectCatalog.firstPass()
                .find(SoundEffectNamespace.NOISE, 0x0C)
                .orElseThrow();

        player.play(explosion);

        assertEquals(0x6A, apu.readRegister(GameBoyApu.NR43));
        assertEquals(0x80, apu.readRegister(GameBoyApu.NR44));

        for (int i = 0; i < 4; i++) {
            player.tick60Hz();
        }

        assertEquals(0x6B, apu.readRegister(GameBoyApu.NR43));
        assertEquals(0x00, apu.readRegister(GameBoyApu.NR44));
    }

    @Test
    void stopDoesNotWriteChannelOffRegistersOverPotentialMusicState() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR30, 0x80);
        apu.writeRegister(GameBoyApu.NR42, 0x55);
        SoundEffectPlayer player = new SoundEffectPlayer(apu);

        player.stop();

        assertEquals(0x80, apu.readRegister(GameBoyApu.NR30));
        assertEquals(0x55, apu.readRegister(GameBoyApu.NR42));
    }

    private static void assertWavePattern63Cc(GameBoyApu apu) {
        int[] expectedData01F63Cc = {
                0xFF, 0xFF, 0xEE, 0xDD, 0xEE, 0xDD, 0xEE, 0xFF,
                0xFF, 0xC9, 0x63, 0x21, 0x00, 0x00, 0x04, 0x8C
        };
        for (int i = 0; i < expectedData01F63Cc.length; i++) {
            assertEquals(expectedData01F63Cc[i], apu.readWaveRam(i), "wave RAM byte " + i);
        }
    }
}
