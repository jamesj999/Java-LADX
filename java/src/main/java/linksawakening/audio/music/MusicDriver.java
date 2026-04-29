package linksawakening.audio.music;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.rom.RomBank;

import java.util.Objects;

public final class MusicDriver {
    private static final int CHANNEL_COUNT = 4;
    private static final int HEADER_SIZE = 11;
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
    private static final int[] NOISE_FREQUENCY_TABLE = {
            0x00,
            0x00, 0x00, 0x00, 0x00, 0xC0,
            0x09, 0x00, 0x38, 0x34, 0xC0,
            0x19, 0x00, 0x38, 0x33, 0xC0,
            0x46, 0x00, 0x13, 0x10, 0xC0,
            0x23, 0x00, 0x20, 0x40, 0x80,
            0x51, 0x00, 0x20, 0x07, 0x80,
            0xA1, 0x00, 0x00, 0x18, 0x80,
            0xF2, 0x00, 0x00, 0x18, 0x80,
            0x81, 0x00, 0x3A, 0x10, 0xC0,
            0x80, 0x00, 0x00, 0x10, 0xC0,
            0x57, 0x00, 0x00, 0x60, 0x80,
            0x01, 0x02, 0x04, 0x08, 0x10,
            0x20, 0x06, 0x0C, 0x18, 0x01,
            0x01, 0x01, 0x01, 0x01, 0x30,
            0x01, 0x03, 0x06, 0x0C, 0x18,
            0x30, 0x09, 0x12, 0x24, 0x02,
            0x04, 0x08, 0x01, 0x01, 0x48
    };
    private static final int[] SOFTWARE_ENVELOPE_TABLE_1B = { 0xF0, 0x10, 0x32, 0x22, 0x47, 0x81, 0x20, 0x00 };
    private static final int[] SOFTWARE_ENVELOPE_TABLE_1E = { 0xF0, 0x10, 0x32, 0x22, 0x47, 0x60, 0x20, 0x00 };
    private static final int[] NOISE_FF_START_REGISTERS = { 0x3B, 0x80, 0x07, 0xC0 };
    private static final int[] NOISE_FF_FOLLOW_UP_REGISTERS = { 0x00, 0x42, 0x02, 0xC0 };
    private static final int NOISE_FF_START_TICKS = 2;
    private static final int NOISE_FF_FOLLOW_UP_TICKS = 4;

    private final byte[] romData;
    private final GameBoyApu apu;
    private final MusicChannelState[] channels = {
            new MusicChannelState(1),
            new MusicChannelState(2),
            new MusicChannelState(3),
            new MusicChannelState(4)
    };

    private boolean playing;
    private int trackBank;
    private int speedTablePointer;
    private final int[] softwareEnvelope = new int[CHANNEL_COUNT];
    private final int[] articulationTick = new int[CHANNEL_COUNT];
    private final int[] noteTicks = new int[CHANNEL_COUNT];
    private final boolean[] articulationApplied = new boolean[CHANNEL_COUNT];
    private int noiseFfPhase;
    private int noiseFfCountdown;

    public MusicDriver(byte[] romData, GameBoyApu apu) {
        this.romData = Objects.requireNonNull(romData, "romData");
        this.apu = Objects.requireNonNull(apu, "apu");
    }

    public void start(MusicTrack track) {
        Objects.requireNonNull(track, "track");
        resetChannelState();
        trackBank = track.bank();

        apu.reset();
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);

        int headerOffset = track.romOffset();
        requireReadable(headerOffset, HEADER_SIZE, trackBank, track.headerAddress());
        speedTablePointer = readWordAtOffset(headerOffset + 1);
        for (int channel = 1; channel <= CHANNEL_COUNT; channel++) {
            int streamPointer = readWordAtOffset(headerOffset + 1 + (channel * 2));
            MusicChannelState state = channels[channel - 1];
            if (streamPointer != 0) {
                state.streamPointer = streamPointer;
                loadNextDefinition(state);
            }
        }

        playing = hasActiveChannel();
    }

    public void stop() {
        playing = false;
        resetChannelState();
        apu.reset();
    }

    public void tick60Hz() {
        if (!playing) {
            return;
        }

        tickNoiseFfSequence();

        for (MusicChannelState state : channels) {
            if (!state.active) {
                continue;
            }
            if (state.lengthCounter > 0) {
                tickArticulation(state);
                state.lengthCounter--;
                if (state.lengthCounter > 0) {
                    continue;
                }
            }
            parseUntilTimeConsumed(state);
        }

        playing = hasActiveChannel();
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isChannelActive(int channel) {
        validateChannel(channel);
        return channels[channel - 1].active;
    }

    private void parseUntilTimeConsumed(MusicChannelState state) {
        while (state.active) {
            int opcodeAddress = state.programCounter;
            int opcode = readByte(trackBank, state.programCounter);
            state.programCounter++;

            if (MusicOpcode.isEnd(opcode)) {
                loadNextDefinition(state);
            } else if (MusicOpcode.isRest(opcode)) {
                silenceChannel(state);
                consumeTime(state);
                return;
            } else if (MusicOpcode.isDriverFlag(opcode)) {
                // Mode flags affect finer driver behavior that is outside this first playback slice.
            } else if (MusicOpcode.isBeginLoop(opcode)) {
                int count = readByte(trackBank, state.programCounter);
                state.programCounter++;
                state.loopStart = state.programCounter;
                state.loopRemaining = count;
            } else if (MusicOpcode.isNextLoop(opcode)) {
                applyLoop(state);
            } else if (MusicOpcode.isSetEnvelopeOrWaveform(opcode)) {
                applyEnvelopeOrWaveform(state);
            } else if (MusicOpcode.isSetSpeed(opcode)) {
                speedTablePointer = readWord(trackBank, state.programCounter);
                state.programCounter += 2;
            } else if (MusicOpcode.isSetTranspose(opcode)) {
                state.transpose = (byte) readByte(trackBank, state.programCounter);
                state.programCounter++;
            } else if (MusicOpcode.isNoteLength(opcode)) {
                state.selectedLength = readSpeed((opcode & 0x0F));
                articulationTick[state.channel - 1] = articulationTickFor(state.selectedLength,
                        softwareEnvelope[state.channel - 1]);
            } else if (state.channel == 4 && (MusicOpcode.isPitchedNote(opcode) || MusicOpcode.isNoiseNote(opcode))) {
                writeNoiseNote(state, opcode);
                consumeTime(state);
                return;
            } else if (MusicOpcode.isPitchedNote(opcode)) {
                writePitchedNote(state, opcode + state.transpose);
                consumeTime(state);
                return;
            } else if (MusicOpcode.isNoiseNote(opcode) && state.channel == 4) {
                consumeTime(state);
                return;
            } else {
                throw unsupportedOpcode(state, opcode, opcodeAddress);
            }
        }
    }

    private void silenceChannel(MusicChannelState state) {
        if (state.channel == 1) {
            apu.writeRegister(GameBoyApu.NR12, 0);
        } else if (state.channel == 2) {
            apu.writeRegister(GameBoyApu.NR22, 0);
        } else if (state.channel == 3) {
            apu.writeRegister(GameBoyApu.NR30, 0);
        } else if (state.channel == 4) {
            apu.writeRegister(GameBoyApu.NR42, 0);
            apu.writeRegister(GameBoyApu.NR44, TRIGGER);
        }
    }

    private void loadNextDefinition(MusicChannelState state) {
        int guard = 0;
        while (true) {
            if (state.streamPointer == 0) {
                state.active = false;
                return;
            }
            int pointer = readWord(trackBank, state.streamPointer);
            state.streamPointer += 2;
            if (pointer == 0x0000) {
                state.active = false;
                return;
            }
            if (pointer == 0xFFFF) {
                int loopPointer = readWord(trackBank, state.streamPointer);
                state.streamPointer = loopPointer;
                if (++guard > 8) {
                    throw new IllegalStateException("Music channel pointer loop did not reach a definition: channel "
                            + state.channel + " bank " + hex(trackBank) + " address " + hex(state.streamPointer));
                }
                continue;
            }

            state.definitionPointer = pointer;
            state.programCounter = pointer;
            state.loopStart = 0;
            state.loopRemaining = 0;
            state.lengthCounter = 0;
            state.active = true;
            return;
        }
    }

    private void applyLoop(MusicChannelState state) {
        if (state.loopRemaining > 1) {
            state.loopRemaining--;
            state.programCounter = state.loopStart;
        } else {
            state.loopRemaining = 0;
        }
    }

    private void applyEnvelopeOrWaveform(MusicChannelState state) {
        int first = readByte(trackBank, state.programCounter);
        int second = readByte(trackBank, state.programCounter + 1);
        int third = readByte(trackBank, state.programCounter + 2);
        state.programCounter += 3;

        if (state.channel == 3) {
            int waveformPointer = first | (second << 8);
            for (int i = 0; i < WAVE_RAM_BYTES; i++) {
                apu.writeWaveRam(i, readByte(trackBank, waveformPointer + i));
            }
            state.waveOutputLevel = third;
            apu.writeRegister(GameBoyApu.NR30, 0x80);
            apu.writeRegister(GameBoyApu.NR32, third);
            return;
        }

        state.envelope = first;
        softwareEnvelope[state.channel - 1] = second;
        state.dutyLength = third;
        if (state.channel == 1) {
            apu.writeRegister(GameBoyApu.NR11, state.dutyLength);
            apu.writeRegister(GameBoyApu.NR12, state.envelope);
        } else if (state.channel == 2) {
            apu.writeRegister(GameBoyApu.NR21, state.dutyLength);
            apu.writeRegister(GameBoyApu.NR22, state.envelope);
        }
    }

    private void writePitchedNote(MusicChannelState state, int noteCode) {
        int frequency = frequencyRegister(noteCode);
        int low = frequency & 0xFF;
        int high = ((frequency >>> 8) & 0x07) | TRIGGER;

        if (state.channel == 1) {
            apu.writeRegister(GameBoyApu.NR11, state.dutyLength);
            apu.writeRegister(GameBoyApu.NR12, state.envelope);
            apu.writeRegister(GameBoyApu.NR13, low);
            apu.writeRegister(GameBoyApu.NR14, high);
        } else if (state.channel == 2) {
            apu.writeRegister(GameBoyApu.NR21, state.dutyLength);
            apu.writeRegister(GameBoyApu.NR22, state.envelope);
            apu.writeRegister(GameBoyApu.NR23, low);
            apu.writeRegister(GameBoyApu.NR24, high);
        } else if (state.channel == 3) {
            apu.writeRegister(GameBoyApu.NR30, 0x80);
            apu.writeRegister(GameBoyApu.NR32, state.waveOutputLevel);
            apu.writeRegister(GameBoyApu.NR33, low);
            apu.writeRegister(GameBoyApu.NR34, high);
        } else if (state.channel != 4) {
            throw new IllegalArgumentException("Channel out of range: " + state.channel);
        }
    }

    private void writeNoiseNote(MusicChannelState state, int noteCode) {
        if (noteCode == 0xFF) {
            writeNoiseRegisters(NOISE_FF_START_REGISTERS);
            noiseFfPhase = 1;
            noiseFfCountdown = NOISE_FF_START_TICKS;
            return;
        }

        int tableOffset = noiseTableOffset(noteCode);
        apu.writeRegister(GameBoyApu.NR41, NOISE_FREQUENCY_TABLE[tableOffset + 2]);
        apu.writeRegister(GameBoyApu.NR42, NOISE_FREQUENCY_TABLE[tableOffset]);
        apu.writeRegister(GameBoyApu.NR43, NOISE_FREQUENCY_TABLE[tableOffset + 3]);
        apu.writeRegister(GameBoyApu.NR44, NOISE_FREQUENCY_TABLE[tableOffset + 4] | TRIGGER);
    }

    private void consumeTime(MusicChannelState state) {
        state.lengthCounter = Math.max(1, state.selectedLength);
        int index = state.channel - 1;
        noteTicks[index] = 0;
        articulationApplied[index] = false;
    }

    private void tickArticulation(MusicChannelState state) {
        if (state.channel > 2) {
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
            apu.writeRegister(GameBoyApu.NR12, envelope);
            apu.writeRegister(GameBoyApu.NR14, apu.readRegister(GameBoyApu.NR14) | TRIGGER);
        } else {
            apu.writeRegister(GameBoyApu.NR22, envelope);
            apu.writeRegister(GameBoyApu.NR24, apu.readRegister(GameBoyApu.NR24) | TRIGGER);
        }
    }

    private void tickNoiseFfSequence() {
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

    private void writeNoiseRegisters(int[] registers) {
        apu.writeRegister(GameBoyApu.NR41, registers[0]);
        apu.writeRegister(GameBoyApu.NR42, registers[1]);
        apu.writeRegister(GameBoyApu.NR43, registers[2]);
        apu.writeRegister(GameBoyApu.NR44, registers[3] | TRIGGER);
    }

    private int readSpeed(int index) {
        return readByte(trackBank, speedTablePointer + index);
    }

    private int readByte(int bank, int address) {
        int offset = RomBank.romOffset(bank, address);
        requireReadable(offset, 1, bank, address);
        return Byte.toUnsignedInt(romData[offset]);
    }

    private int readWord(int bank, int address) {
        int offset = RomBank.romOffset(bank, address);
        requireReadable(offset, 2, bank, address);
        return readWordAtOffset(offset);
    }

    private int readWordAtOffset(int offset) {
        return Byte.toUnsignedInt(romData[offset]) | (Byte.toUnsignedInt(romData[offset + 1]) << 8);
    }

    private void requireReadable(int offset, int length, int bank, int address) {
        if (offset < 0 || offset + length > romData.length) {
            throw new IllegalStateException("Music ROM read out of range: bank " + hex(bank)
                    + " address " + hex(address));
        }
    }

    private boolean hasActiveChannel() {
        for (MusicChannelState state : channels) {
            if (state.active) {
                return true;
            }
        }
        return false;
    }

    private void resetChannelState() {
        for (MusicChannelState state : channels) {
            state.reset();
        }
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            softwareEnvelope[i] = 0;
            articulationTick[i] = 0;
            noteTicks[i] = 0;
            articulationApplied[i] = false;
        }
        noiseFfPhase = 0;
        noiseFfCountdown = 0;
    }

    private static int frequencyRegister(int noteCode) {
        if (noteCode < 0x02 || noteCode > 0x90 || (noteCode & 0x01) != 0) {
            throw new IllegalArgumentException("Invalid pitched music note: " + hex(noteCode));
        }
        return SQUARE_AND_WAVE_FREQUENCY_TABLE[noteCode / 2];
    }

    private static int noiseTableOffset(int noteCode) {
        if (noteCode < 0x01 || noteCode > 0x51 || (noteCode - 0x01) % 5 != 0
                || noteCode + 4 >= NOISE_FREQUENCY_TABLE.length) {
            throw new IllegalArgumentException("Invalid noise music note: " + hex(noteCode));
        }
        return noteCode;
    }

    private static int articulationTickFor(int selectedLength, int softwareEnvelope) {
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

    private static int nonZeroArticulationTick(int tick) {
        return tick == 0 ? 2 : tick;
    }

    private int[] softwareEnvelopeTable() {
        return trackBank == 0x1E ? SOFTWARE_ENVELOPE_TABLE_1E : SOFTWARE_ENVELOPE_TABLE_1B;
    }

    private static void validateChannel(int channel) {
        if (channel < 1 || channel > CHANNEL_COUNT) {
            throw new IllegalArgumentException("Channel out of range: " + channel);
        }
    }

    private IllegalStateException unsupportedOpcode(MusicChannelState state, int opcode, int address) {
        return new IllegalStateException("Unsupported music opcode " + hex(opcode)
                + " on channel " + state.channel + " bank " + hex(trackBank)
                + " address " + hex(address));
    }

    private static String hex(int value) {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }
}
