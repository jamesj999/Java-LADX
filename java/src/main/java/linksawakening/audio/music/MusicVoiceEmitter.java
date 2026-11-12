package linksawakening.audio.music;

import linksawakening.audio.apu.GameBoyApu;

/** Channel register emission, wave loading and timed articulation. */
final class MusicVoiceEmitter {
    private final MusicDriver driver;
    private final GameBoyApu apu;

    MusicVoiceEmitter(MusicDriver driver, GameBoyApu apu) {
        this.driver = driver;
        this.apu = apu;
    }

    private static final int WAVE_RAM_BYTES = 16;
    private static final int TRIGGER = 0x80;
    private static final int[] SQUARE_AND_WAVE_FREQUENCY_TABLE = {
        0x0F00, 0x002C, 0x009C, 0x0106,
        0x016B, 0x01C9, 0x0223, 0x0277,
        0x02C6, 0x0312, 0x0356, 0x039B,
        0x03DA, 0x0416, 0x044E, 0x0483,
        0x04B5, 0x04E5, 0x0511, 0x053B,
        0x0563, 0x0589, 0x05AC, 0x05CE,
        0x05ED, 0x060A, 0x0627, 0x0642,
        0x065B, 0x0672, 0x0689, 0x069E,
        0x06B2, 0x06C4, 0x06D6, 0x06E7,
        0x06F7, 0x0706, 0x0714, 0x0721,
        0x072D, 0x0739, 0x0744, 0x074F,
        0x0759, 0x0762, 0x076B, 0x0774,
        0x077B, 0x0783, 0x078A, 0x0790,
        0x0797, 0x079D, 0x07A2, 0x07A7,
        0x07AC, 0x07B1, 0x07B6, 0x07BA,
        0x07BE, 0x07C1, 0x07C5, 0x07C8,
        0x07CB, 0x07CE, 0x07D1, 0x07D4,
        0x07D6, 0x07D9, 0x07DB, 0x07DD,
        0x07DF
    };
    private static final int[] NOISE_FREQUENCY_TABLE_1B = {
        0x00, 0x00, 0x00, 0x00, 0x00, 0xC0, 0x09, 0x00, 0x38, 0x34, 0xC0, 0x19, 0x00, 0x38, 0x33, 0xC0, 0x46, 0x00,
        0x13, 0x10, 0xC0, 0x23, 0x00, 0x20, 0x40, 0x80, 0x51, 0x00, 0x20, 0x07, 0x80, 0xA1, 0x00, 0x00, 0x18, 0x80,
        0xF2, 0x00, 0x00, 0x18, 0x80, 0x81, 0x00, 0x3A, 0x10, 0xC0, 0x80, 0x00, 0x00, 0x10, 0xC0, 0x57, 0x00, 0x00,
        0x60, 0x80, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x06, 0x0C, 0x18, 0x01, 0x01, 0x01, 0x01, 0x01, 0x30, 0x01,
        0x03, 0x06, 0x0C, 0x18, 0x30, 0x09, 0x12, 0x24, 0x02, 0x04, 0x08, 0x01, 0x01, 0x48
    };
    private static final int[] NOISE_FREQUENCY_TABLE_1E = {
        0x00, 0x00, 0x00, 0x00, 0x00, 0xC0, 0x09, 0x00, 0x38, 0x34, 0xC0, 0x19, 0x00, 0x38, 0x33, 0xC0, 0x46, 0x00,
        0x13, 0x10, 0xC0, 0x23, 0x00, 0x20, 0x40, 0x80, 0x51, 0x00, 0x20, 0x07, 0x80, 0xA1, 0x00, 0x00, 0x18, 0x80,
        0xF2, 0x00, 0x00, 0x18, 0x80, 0x81, 0x00, 0x3A, 0x10, 0xC0, 0x80, 0x00, 0x00, 0x10, 0xC0, 0x57, 0x00, 0x00,
        0x60, 0x80, 0x10, 0x00, 0x00, 0x10, 0x80, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x06, 0x0C, 0x18, 0x01, 0x01,
        0x01, 0x01, 0x01, 0x30
    };
    private static final int[] SOFTWARE_ENVELOPE_TABLE_1B = {0xF0, 0x10, 0x32, 0x22, 0x47, 0x81, 0x20, 0x00};
    private static final int[] SOFTWARE_ENVELOPE_TABLE_1E = {0xF0, 0x10, 0x32, 0x22, 0x47, 0x60, 0x20, 0x00};
    private static final int[] NOISE_FF_START_REGISTERS = {0x3B, 0x80, 0x07, 0xC0};
    private static final int[] NOISE_FF_FOLLOW_UP_REGISTERS = {0x00, 0x42, 0x02, 0xC0};
    private static final int NOISE_FF_START_TICKS = 2;
    private static final int NOISE_FF_FOLLOW_UP_TICKS = 4;

    int waveformPointer;

    final int[] softwareEnvelope = new int[4];

    final int[] articulationTick = new int[4];

    final int[] noteTicks = new int[4];

    final boolean[] articulationApplied = new boolean[4];

    int noiseFfPhase;

    int noiseFfCountdown;

    int channel3VolumeEffectMode;

    int channel3VolumeEffectCounter;

    void loadWaveform() {
        if (driver.sfxOwnsChannel.test(3)) return;
        driver.writeRegister(GameBoyApu.NR30, 0);
        for (int i = 0; i < WAVE_RAM_BYTES; i++) {
            apu.writeWaveRam(i, driver.readByte(driver.trackBank, waveformPointer + i));
        }
    }

    void silenceChannel(MusicChannelState state) {
        state.playingRest = true;
        if (state.channel == 1) {
            driver.writeRegister(GameBoyApu.NR12, 0x08);
            driver.writeRegister(GameBoyApu.NR14, 0x80);
        } else if (state.channel == 2) {
            driver.writeRegister(GameBoyApu.NR22, 0x08);
            driver.writeRegister(GameBoyApu.NR24, 0x80);
        } else if (state.channel == 3) {
            driver.writeRegister(GameBoyApu.NR30, 0);
        } else if (state.channel == 4) {
            driver.writeRegister(GameBoyApu.NR42, 0x08);
            driver.writeRegister(GameBoyApu.NR44, TRIGGER);
        }
    }

    void applyEnvelopeOrWaveform(MusicChannelState state) {
        int first = driver.readByte(driver.trackBank, state.programCounter);
        int second = driver.readByte(driver.trackBank, state.programCounter + 1);
        int third = driver.readByte(driver.trackBank, state.programCounter + 2);
        state.programCounter += 3;

        if (state.channel == 3) {
            waveformPointer = first | (second << 8);
            loadWaveform();
            state.waveOutputLevel = third;
            driver.writeRegister(GameBoyApu.NR30, 0x80);
            driver.writeRegister(GameBoyApu.NR32, third);
            return;
        }

        state.envelope = first;
        softwareEnvelope[state.channel - 1] = second;
        state.dutyLength = third;
        if (state.channel == 1) {
            driver.writeRegister(GameBoyApu.NR11, state.dutyLength);
            driver.writeRegister(GameBoyApu.NR12, state.envelope);
        } else if (state.channel == 2) {
            driver.writeRegister(GameBoyApu.NR21, state.dutyLength);
            driver.writeRegister(GameBoyApu.NR22, state.envelope);
        }
    }

    void writePitchedNote(MusicChannelState state, int noteCode) {
        state.playingRest = false;
        int frequency = frequencyRegister(noteCode);
        int low = frequency & 0xFF;
        int high = ((frequency >>> 8) & 0x07) | TRIGGER;

        if (state.channel == 1) {
            driver.writeRegister(GameBoyApu.NR11, state.dutyLength);
            driver.writeRegister(GameBoyApu.NR12, state.envelope);
            driver.writeRegister(GameBoyApu.NR13, low);
            driver.writeRegister(GameBoyApu.NR14, high);
        } else if (state.channel == 2) {
            driver.writeRegister(GameBoyApu.NR21, state.dutyLength);
            driver.writeRegister(GameBoyApu.NR22, state.envelope);
            driver.writeRegister(GameBoyApu.NR23, low);
            driver.writeRegister(GameBoyApu.NR24, high);
        } else if (state.channel == 3) {
            if (channel3VolumeEffectMode != 0) {
                channel3VolumeEffectMode = 1;
                channel3VolumeEffectCounter = 0;
            }
            // label_01B_42AB restores the music waveform on the next wave note.
            if (!driver.sfxOwnsChannel.test(3) && driver.takeWaveRestoreRequest.getAsBoolean()) loadWaveform();
            driver.writeRegister(GameBoyApu.NR30, 0x80);
            driver.writeRegister(GameBoyApu.NR32, state.waveOutputLevel);
            driver.writeRegister(GameBoyApu.NR33, low);
            driver.writeRegister(GameBoyApu.NR34, high);
        } else if (state.channel != 4) {
            throw new IllegalArgumentException("Channel out of range: " + state.channel);
        }
    }

    void writeNoiseNote(MusicChannelState state, int noteCode) {
        state.playingRest = false;
        if (noteCode == 0xFF) {
            writeNoiseRegisters(NOISE_FF_START_REGISTERS);
            noiseFfPhase = 1;
            noiseFfCountdown = NOISE_FF_START_TICKS;
            return;
        }

        int[] noiseTable = noiseFrequencyTable();
        int tableOffset = noiseTableOffset(noteCode, noiseTable);
        driver.writeRegister(GameBoyApu.NR41, noiseTable[tableOffset + 2]);
        driver.writeRegister(GameBoyApu.NR42, noiseTable[tableOffset]);
        driver.writeRegister(GameBoyApu.NR43, noiseTable[tableOffset + 3]);
        driver.writeRegister(GameBoyApu.NR44, noiseTable[tableOffset + 4] | TRIGGER);
    }

    void applyDriverFlag(int opcode) {
        if (opcode == 0x99) {
            channel3VolumeEffectMode = 1;
        } else if (opcode == 0x9A) {
            channel3VolumeEffectMode = 0;
        }
    }

    void tickArticulation(MusicChannelState state) {
        if (state.channel == 3) {
            tickChannel3VolumeEffect(state);
            return;
        }
        if (state.channel > 2 || state.playingRest) {
            return;
        }

        int index = state.channel - 1;
        noteTicks[index]++;
        int threshold = articulationTick[index];
        if (threshold == 0 || articulationApplied[index] || noteTicks[index] != threshold) {
            return;
        }

        int envelopeIndex = softwareEnvelope[index] & 0x0F;
        int[] envelopeTable = softwareEnvelopeTable();
        int envelope = envelopeTable[Math.min(envelopeIndex, envelopeTable.length - 1)];
        articulationApplied[index] = true;
        if (state.channel == 1) {
            driver.writeRegister(GameBoyApu.NR12, envelope);
            driver.writeRegister(GameBoyApu.NR14, apu.readRegister(GameBoyApu.NR14) | TRIGGER);
        } else {
            driver.writeRegister(GameBoyApu.NR22, envelope);
            driver.writeRegister(GameBoyApu.NR24, apu.readRegister(GameBoyApu.NR24) | TRIGGER);
        }
    }

    void tickChannel3VolumeEffect(MusicChannelState state) {
        if (state.playingRest || channel3VolumeEffectMode == 0 || channel3VolumeEffectMode == 2) {
            return;
        }

        channel3VolumeEffectCounter++;
        if (channel3VolumeEffectCounter == 3) {
            driver.writeRegister(GameBoyApu.NR32, 0x40);
        } else if (channel3VolumeEffectCounter == 5 || channel3VolumeEffectCounter == 10) {
            driver.writeRegister(GameBoyApu.NR32, 0x60);
        } else if (channel3VolumeEffectCounter == 7) {
            driver.writeRegister(GameBoyApu.NR32, 0x00);
        } else if (channel3VolumeEffectCounter == 13) {
            channel3VolumeEffectMode = 2;
            driver.writeRegister(GameBoyApu.NR32, 0x00);
        }
    }

    void tickNoiseFfSequence() {
        if (noiseFfPhase == 0) {
            return;
        }
        noiseFfCountdown--;
        if (noiseFfCountdown > 0) {
            return;
        }
        if (noiseFfPhase == 1) {
            writeNoiseRegisters(NOISE_FF_FOLLOW_UP_REGISTERS);
            noiseFfPhase = 2;
            noiseFfCountdown = NOISE_FF_FOLLOW_UP_TICKS;
        } else {
            noiseFfPhase = 0;
            noiseFfCountdown = 0;
        }
    }

    void writeNoiseRegisters(int[] registers) {
        driver.writeRegister(GameBoyApu.NR41, registers[0]);
        driver.writeRegister(GameBoyApu.NR42, registers[1]);
        driver.writeRegister(GameBoyApu.NR43, registers[2]);
        driver.writeRegister(GameBoyApu.NR44, registers[3] | TRIGGER);
    }

    static int frequencyRegister(int noteCode) {
        if (noteCode < 0x02 || noteCode > 0x90 || (noteCode & 0x01) != 0) {
            throw new IllegalArgumentException("Invalid pitched music note: " + MusicDriver.hex(noteCode));
        }
        return SQUARE_AND_WAVE_FREQUENCY_TABLE[noteCode / 2];
    }

    static int noiseTableOffset(int noteCode, int[] noiseTable) {
        if (noteCode < 0x01 || noteCode > 0x51 || (noteCode - 0x01) % 5 != 0 || noteCode + 4 >= noiseTable.length) {
            throw new IllegalArgumentException("Invalid noise music note: " + MusicDriver.hex(noteCode));
        }
        return noteCode;
    }

    static int articulationTickFor(int selectedLength, int softwareEnvelope) {
        int envelopeControl = softwareEnvelope & 0xF0;
        if (envelopeControl == 0) {
            return 0;
        }

        int tick = selectedLength >>> 1;
        int shiftedControl = (envelopeControl << 1) & 0xFF;
        if ((envelopeControl & 0x80) != 0) {
            return nonZeroArticulationTick(tick);
        }

        int halfLength = tick;
        tick >>>= 1;
        if ((shiftedControl & 0x80) != 0) {
            return nonZeroArticulationTick(tick);
        }

        return nonZeroArticulationTick(tick + halfLength);
    }

    static int nonZeroArticulationTick(int tick) {
        return tick == 0 ? 2 : tick;
    }

    int[] softwareEnvelopeTable() {
        return driver.trackBank == 0x1E ? SOFTWARE_ENVELOPE_TABLE_1E : SOFTWARE_ENVELOPE_TABLE_1B;
    }

    int[] noiseFrequencyTable() {
        return driver.trackBank == 0x1E ? NOISE_FREQUENCY_TABLE_1E : NOISE_FREQUENCY_TABLE_1B;
    }
}
