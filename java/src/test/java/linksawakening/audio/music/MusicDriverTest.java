package linksawakening.audio.music;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MusicDriverTest {
    @Test
    void startsTrackAndWritesSquareRegistersForFirstNote() {
        byte[] rom = syntheticRomWithOneChannel(new byte[] {
                (byte) 0x9D, (byte) 0xF0, 0x00, (byte) 0x80,
                (byte) 0xA2,
                0x4A,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();

        assertEquals(0x80, apu.readRegister(GameBoyApu.NR11));
        assertEquals(0xF0, apu.readRegister(GameBoyApu.NR12));
        assertTrue((apu.readRegister(GameBoyApu.NR14) & 0x80) != 0);
        assertTrue(driver.isChannelActive(1));
    }

    @Test
    void loopOpcodeRepeatsChannelDefinitions() {
        byte[] rom = syntheticRomWithOneChannel(new byte[] {
                (byte) 0xA1,
                (byte) 0x9B, 0x02,
                0x4A,
                (byte) 0x9C,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();
        assertTrue(driver.isChannelActive(1));
        driver.tick60Hz();
        assertTrue(driver.isChannelActive(1));
    }

    @Test
    void channel3SetWaveformCopiesWaveRam() {
        byte[] rom = syntheticRomWithChannel3(new byte[] {
                (byte) 0x9D, 0x00, 0x56, 0x20,
                (byte) 0xA1,
                0x4A,
                0x00
        });
        int waveformOffset = RomBank.romOffset(0x1B, 0x5600);
        for (int i = 0; i < 16; i++) {
            rom[waveformOffset + i] = (byte) i;
        }
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();

        assertEquals(0x80, apu.readRegister(GameBoyApu.NR30));
        assertEquals(0x20, apu.readRegister(GameBoyApu.NR32));
    }

    @Test
    void channel4NoiseNoteWritesNoiseRegisters() {
        byte[] rom = syntheticRomWithChannel4(new byte[] {
                (byte) 0x9D, (byte) 0xF2, 0x00, (byte) 0x80,
                (byte) 0xA1,
                0x1A,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();

        assertEquals(0x51, apu.readRegister(GameBoyApu.NR42));
        assertTrue(apu.readRegister(GameBoyApu.NR43) != 0);
        assertTrue((apu.readRegister(GameBoyApu.NR44) & 0x80) != 0);
    }

    @Test
    void squareNoteAfterRestRestoresEnvelopeBeforeTriggering() {
        byte[] rom = syntheticRomWithOneChannel(new byte[] {
                (byte) 0x9D, (byte) 0xF0, 0x00, (byte) 0x80,
                (byte) 0xA0,
                0x4A,
                0x01,
                0x4A,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();
        assertEquals(0xF0, apu.readRegister(GameBoyApu.NR12));
        driver.tick60Hz();
        assertEquals(0, apu.readRegister(GameBoyApu.NR12));
        driver.tick60Hz();

        assertEquals(0x80, apu.readRegister(GameBoyApu.NR11));
        assertEquals(0xF0, apu.readRegister(GameBoyApu.NR12));
        assertTrue((apu.readRegister(GameBoyApu.NR14) & 0x80) != 0);
    }

    @Test
    void squareSoftwareEnvelopeSecondOperandAppliesDelayedArticulation() {
        byte[] rom = syntheticRomWithOneChannel(new byte[] {
                (byte) 0x9D, (byte) 0x70, (byte) 0x81, (byte) 0x80,
                (byte) 0xA7,
                0x4A,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();
        assertEquals(0x70, apu.readRegister(GameBoyApu.NR12));
        driver.tick60Hz();
        driver.tick60Hz();
        driver.tick60Hz();
        assertEquals(0x70, apu.readRegister(GameBoyApu.NR12));
        driver.tick60Hz();

        assertEquals(0x10, apu.readRegister(GameBoyApu.NR12));
        assertTrue((apu.readRegister(GameBoyApu.NR14) & 0x80) != 0);
    }

    @Test
    void channel2SoftwareEnvelopeCanUseThreeQuarterArticulationPoint() {
        byte[] rom = syntheticRomWithChannel2(new byte[] {
                (byte) 0x9D, (byte) 0x90, 0x26, (byte) 0x40,
                (byte) 0xA7,
                0x4A,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();
        for (int i = 0; i < 5; i++) {
            driver.tick60Hz();
        }
        assertEquals(0x90, apu.readRegister(GameBoyApu.NR22));
        driver.tick60Hz();

        assertEquals(0x20, apu.readRegister(GameBoyApu.NR22));
        assertTrue((apu.readRegister(GameBoyApu.NR24) & 0x80) != 0);
    }

    @Test
    void softwareEnvelopeFollowUpTableComesFromActiveMusicBank() {
        byte[] rom = syntheticRomWithOneChannelInBank(0x1E, new byte[] {
                (byte) 0x9D, (byte) 0x70, (byte) 0x85, (byte) 0x80,
                (byte) 0xA7,
                0x4A,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1E, 0x4077, 0x5000, RomBank.romOffset(0x1E, 0x5000)));
        driver.tick60Hz();
        driver.tick60Hz();
        driver.tick60Hz();
        driver.tick60Hz();
        driver.tick60Hz();

        assertEquals(0x60, apu.readRegister(GameBoyApu.NR12));
    }

    @Test
    void waveNoteAfterRestRestoresDacAndOutputLevelBeforeTriggering() {
        byte[] rom = syntheticRomWithChannel3(new byte[] {
                (byte) 0x9D, 0x00, 0x56, 0x20,
                (byte) 0xA0,
                0x4A,
                0x01,
                0x4A,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();
        assertEquals(0x80, apu.readRegister(GameBoyApu.NR30));
        driver.tick60Hz();
        assertEquals(0, apu.readRegister(GameBoyApu.NR30));
        driver.tick60Hz();

        assertEquals(0x80, apu.readRegister(GameBoyApu.NR30));
        assertEquals(0x20, apu.readRegister(GameBoyApu.NR32));
        assertTrue((apu.readRegister(GameBoyApu.NR34) & 0x80) != 0);
    }

    @Test
    void pitchedNoteCodesAdvanceOneFrequencyEntryPerTwoOpcodeBytes() {
        byte[] rom = syntheticRomWithOneChannel(new byte[] {
                (byte) 0x9D, (byte) 0xF0, 0x00, (byte) 0x80,
                (byte) 0xA0,
                0x02,
                0x04,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();
        assertEquals(0x2C, apu.readRegister(GameBoyApu.NR13));
        assertEquals(0x80, apu.readRegister(GameBoyApu.NR14));
        driver.tick60Hz();

        assertEquals(0x9C, apu.readRegister(GameBoyApu.NR13));
        assertEquals(0x80, apu.readRegister(GameBoyApu.NR14));
    }

    @Test
    void transposeOpcodeAppliesGloballyAcrossMusicChannels() {
        byte[] rom = syntheticRomWithChannels(
                new byte[] {
                        (byte) 0x9D, (byte) 0xF0, 0x00, (byte) 0x80,
                        (byte) 0xA0,
                        0x01,
                        (byte) 0x84,
                        0x00
                },
                new byte[] {
                        (byte) 0x9F, (byte) 0xD0,
                        (byte) 0xA0,
                        0x01,
                        0x00
                });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();
        driver.tick60Hz();

        assertEquals(0x44, apu.readRegister(GameBoyApu.NR13));
        assertEquals(0x87, apu.readRegister(GameBoyApu.NR14));
    }

    @Test
    void oddPitchedNoteCodeIsRejectedSafely() {
        byte[] rom = syntheticRomWithOneChannel(new byte[] {
                (byte) 0x9D, (byte) 0xF0, 0x00, (byte) 0x80,
                (byte) 0xA0,
                0x03,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));

        assertThrows(IllegalArgumentException.class, driver::tick60Hz);
    }

    @Test
    void channel4NoiseNoteWithoutSetupUsesNoiseTableAndProducesPcm() {
        byte[] rom = syntheticRomWithChannel4(new byte[] {
                (byte) 0xA0,
                0x1A,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();

        assertTrue(apu.readRegister(GameBoyApu.NR42) != 0);
        assertTrue(apu.readRegister(GameBoyApu.NR43) != 0);
        assertTrue((apu.readRegister(GameBoyApu.NR44) & 0x80) != 0);
        assertTrue(hasNonZeroSample(apu.render(512)));
    }

    @Test
    void channel4NoiseFfAdvancesToFollowUpNoiseRegistersAfterCountdown() {
        byte[] rom = syntheticRomWithChannel4(new byte[] {
                (byte) 0xA5,
                (byte) 0xFF,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();
        assertEquals(0x3B, apu.readRegister(GameBoyApu.NR41));
        assertEquals(0x80, apu.readRegister(GameBoyApu.NR42));
        assertEquals(0x07, apu.readRegister(GameBoyApu.NR43));
        assertEquals(0xC0, apu.readRegister(GameBoyApu.NR44));
        driver.tick60Hz();
        assertEquals(0x80, apu.readRegister(GameBoyApu.NR42));
        driver.tick60Hz();

        assertEquals(0x00, apu.readRegister(GameBoyApu.NR41));
        assertEquals(0x42, apu.readRegister(GameBoyApu.NR42));
        assertEquals(0x02, apu.readRegister(GameBoyApu.NR43));
        assertEquals(0xC0, apu.readRegister(GameBoyApu.NR44));
    }

    @Test
    void restMutesPreviouslyPlayingSquareChannel() {
        byte[] rom = syntheticRomWithOneChannel(new byte[] {
                (byte) 0x9D, (byte) 0xF0, 0x00, (byte) 0x80,
                (byte) 0xA1,
                0x4A,
                0x01,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();
        assertEquals(0xF0, apu.readRegister(GameBoyApu.NR12));
        driver.tick60Hz();
        driver.tick60Hz();

        assertEquals(0, apu.readRegister(GameBoyApu.NR12));
    }

    @Test
    void squareSoftwareEnvelopeDoesNotRetriggerDuringRest() {
        byte[] rom = syntheticRomWithOneChannel(new byte[] {
                (byte) 0x9D, (byte) 0x70, (byte) 0x81, (byte) 0x80,
                (byte) 0xA0,
                0x4A,
                (byte) 0xA3,
                0x01,
                0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();
        assertEquals(0x70, apu.readRegister(GameBoyApu.NR12));
        driver.tick60Hz();
        assertEquals(0, apu.readRegister(GameBoyApu.NR12));
        driver.tick60Hz();
        driver.tick60Hz();

        assertEquals(0, apu.readRegister(GameBoyApu.NR12));
    }

    private static byte[] syntheticRomWithOneChannel(byte[] definition) {
        return syntheticRomWithOneChannelInBank(0x1B, definition);
    }

    private static byte[] syntheticRomWithOneChannelInBank(int bank, byte[] definition) {
        byte[] rom = new byte[0x80000];
        int header = RomBank.romOffset(bank, 0x5000);
        writeHeader(rom, header, 0x5100, 0x5200, 0, 0, 0);
        writeSpeedTable(rom, bank, 0x5100);
        writeWord(rom, RomBank.romOffset(bank, 0x5200), 0x5300);
        System.arraycopy(definition, 0, rom, RomBank.romOffset(bank, 0x5300), definition.length);
        return rom;
    }

    private static byte[] syntheticRomWithChannels(byte[] channel1Definition, byte[] channel2Definition) {
        byte[] rom = new byte[0x80000];
        int header = RomBank.romOffset(0x1B, 0x5000);
        writeHeader(rom, header, 0x5100, 0x5200, 0x5220, 0, 0);
        writeWord(rom, RomBank.romOffset(0x1B, 0x5200), 0x5300);
        writeWord(rom, RomBank.romOffset(0x1B, 0x5220), 0x5400);
        System.arraycopy(channel1Definition, 0, rom, RomBank.romOffset(0x1B, 0x5300), channel1Definition.length);
        System.arraycopy(channel2Definition, 0, rom, RomBank.romOffset(0x1B, 0x5400), channel2Definition.length);
        return rom;
    }

    private static byte[] syntheticRomWithChannel2(byte[] definition) {
        byte[] rom = new byte[0x80000];
        int header = RomBank.romOffset(0x1B, 0x5000);
        writeHeader(rom, header, 0x5100, 0, 0x5200, 0, 0);
        writeWord(rom, RomBank.romOffset(0x1B, 0x5200), 0x5300);
        System.arraycopy(definition, 0, rom, RomBank.romOffset(0x1B, 0x5300), definition.length);
        return rom;
    }

    private static byte[] syntheticRomWithChannel4(byte[] definition) {
        byte[] rom = new byte[0x80000];
        int header = RomBank.romOffset(0x1B, 0x5000);
        writeHeader(rom, header, 0x5100, 0, 0, 0, 0x5200);
        writeWord(rom, RomBank.romOffset(0x1B, 0x5200), 0x5300);
        System.arraycopy(definition, 0, rom, RomBank.romOffset(0x1B, 0x5300), definition.length);
        return rom;
    }

    private static byte[] syntheticRomWithChannel3(byte[] definition) {
        byte[] rom = new byte[0x80000];
        int header = RomBank.romOffset(0x1B, 0x5000);
        writeHeader(rom, header, 0x5100, 0, 0, 0x5200, 0);
        writeWord(rom, RomBank.romOffset(0x1B, 0x5200), 0x5300);
        System.arraycopy(definition, 0, rom, RomBank.romOffset(0x1B, 0x5300), definition.length);
        return rom;
    }

    private static void writeHeader(byte[] rom, int offset, int speed, int ch1, int ch2, int ch3, int ch4) {
        rom[offset] = 0;
        writeWordAtOffset(rom, offset + 1, speed);
        writeWordAtOffset(rom, offset + 3, ch1);
        writeWordAtOffset(rom, offset + 5, ch2);
        writeWordAtOffset(rom, offset + 7, ch3);
        writeWordAtOffset(rom, offset + 9, ch4);
        writeSpeedTable(rom, 0x1B, speed);
    }

    private static void writeSpeedTable(byte[] rom, int bank, int speed) {
        int speedOffset = RomBank.romOffset(bank, speed);
        for (int i = 0; i < 16; i++) {
            rom[speedOffset + i] = (byte) (i + 1);
        }
    }

    private static void writeWord(byte[] rom, int offset, int value) {
        writeWordAtOffset(rom, offset, value);
    }

    private static void writeWordAtOffset(byte[] rom, int offset, int value) {
        rom[offset] = (byte) (value & 0xFF);
        rom[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    private static boolean hasNonZeroSample(short[] pcm) {
        for (short sample : pcm) {
            if (sample != 0) {
                return true;
            }
        }
        return false;
    }
}
