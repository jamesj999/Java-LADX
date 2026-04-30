package linksawakening.audio.sfx;

import linksawakening.audio.apu.GameBoyApu;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SoundEffectPlayerTest {
    @Test
    void supportedEffectsExposeFullCatalogInCatalogOrder() throws IOException {
        SoundEffectCatalog catalog = SoundEffectCatalog.fromRom(loadRom());

        List<SoundEffect> supported = SoundEffectPlayer.supportedEffects(catalog);

        assertEquals(catalog.effects(), supported);
        assertEquals(164, supported.size());
        assertEquals("JINGLE_TREASURE_FOUND", supported.getFirst().name());
        assertTrue(supported.stream().anyMatch(effect -> effect.name().equals("WAVE_SFX_TEXT_PRINT")));
        assertTrue(supported.stream().anyMatch(effect -> effect.name().equals("NOISE_SFX_PHOTO")));
    }

    @Test
    void mappedSfxArePlayableFromRomTables() throws IOException {
        byte[] romData = loadRom();
        SoundEffectCatalog catalog = SoundEffectCatalog.fromRom(romData);
        for (SoundEffect effect : catalog.effects()) {
            GameBoyApu apu = new GameBoyApu(48_000);
            SoundEffectPlayer player = new SoundEffectPlayer(apu, romData, catalog);
            assertTrue(player.supports(effect), effect.name());
            assertDoesNotThrow(() -> {
                player.play(effect);
                for (int i = 0; i < 600 && player.isPlaying(); i++) {
                    player.tick60Hz();
                }
                player.stop();
            }, effect.name());
        }
    }

    @Test
    void formerlyUnimplementedMappedSfxNowStartsFromRom() {
        GameBoyApu apu = new GameBoyApu(48_000);
        SoundEffectPlayer player = new SoundEffectPlayer(apu);
        SoundEffect treasure = player.catalog()
                .find(SoundEffectNamespace.JINGLE, 0x01)
                .orElseThrow();

        assertTrue(player.supports(treasure));
        player.play(treasure);

        assertTrue(player.isPlaying());
        assertEquals(0x80, apu.readRegister(GameBoyApu.NR52) & 0x80);
        assertEquals(0xC0, apu.readRegister(GameBoyApu.NR22));
        assertEquals(0x06, apu.readRegister(GameBoyApu.NR23));
        assertEquals(0x87, apu.readRegister(GameBoyApu.NR24));
    }

    @Test
    void triggeringNoiseEffectEnablesApuAndWritesInitialNoiseRegisters() {
        GameBoyApu apu = new GameBoyApu(48_000);
        SoundEffectPlayer player = new SoundEffectPlayer(apu);
        SoundEffect swordSwing = player.catalog()
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
        SoundEffect swordSwing = player.catalog()
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
        SoundEffect textPrint = player.catalog()
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
        SoundEffect rupee = player.catalog()
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
    void rupeeLoadsItsWavePatternFromRomBeforeTriggeringChannel3() {
        GameBoyApu apu = new GameBoyApu(48_000);
        SoundEffectPlayer player = new SoundEffectPlayer(apu);
        SoundEffect rupee = player.catalog()
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
    void lowHeartsAndTextPrintLoadTheirWavePatternFromRomBeforeTriggeringChannel3() {
        GameBoyApu apu = new GameBoyApu(48_000);
        SoundEffectPlayer player = new SoundEffectPlayer(apu);
        SoundEffect lowHearts = player.catalog()
                .find(SoundEffectNamespace.WAVE, 0x04)
                .orElseThrow();
        SoundEffect textPrint = player.catalog()
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
        SoundEffect explosion = player.catalog()
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

    private static byte[] loadRom() throws IOException {
        try (InputStream stream = SoundEffectPlayerTest.class.getClassLoader()
                .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
