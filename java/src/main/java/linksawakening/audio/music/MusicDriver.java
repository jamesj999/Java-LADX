package linksawakening.audio.music;

import java.util.Objects;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;

/** ROM music stream sequencing at the original frame cadence. */
public final class MusicDriver {
    private static final int CHANNEL_COUNT = 4;
    private static final int HEADER_SIZE = 11;
    private final byte[] romData;
    private final Rom romView;
    private final GameBoyApu apu;
    private final MusicVoiceEmitter voice;
    private final MusicChannelState[] channels = {
        new MusicChannelState(1), new MusicChannelState(2), new MusicChannelState(3), new MusicChannelState(4)
    };

    java.util.function.IntPredicate sfxOwnsChannel = channel -> false;
    java.util.function.BooleanSupplier takeWaveRestoreRequest = () -> false;

    public void setSoundEffectState(
            java.util.function.IntPredicate ownsChannel, java.util.function.BooleanSupplier waveRestoreRequest) {
        sfxOwnsChannel = Objects.requireNonNull(ownsChannel);
        takeWaveRestoreRequest = Objects.requireNonNull(waveRestoreRequest);
    }

    void writeRegister(int register, int value) {
        int channel = register <= GameBoyApu.NR14
                ? 1
                : register <= GameBoyApu.NR24
                        ? 2
                        : register <= GameBoyApu.NR34 ? 3 : register <= GameBoyApu.NR44 ? 4 : 0;
        // Music continues sequencing while bit 7 of D3xF inhibits hardware writes.
        // WriteChannelRegisters (1B:46BF), func_01B_4787 (1B:47A4).
        if (channel == 0 || !sfxOwnsChannel.test(channel)) apu.writeRegister(register, value);
    }

    private boolean playing;
    int trackBank;
    private int speedTablePointer;
    private int musicTranspose;

    public MusicDriver(byte[] romData, GameBoyApu apu) {
        this.romData = Objects.requireNonNull(romData, "romData");
        this.romView = new Rom(this.romData);
        this.apu = Objects.requireNonNull(apu, "apu");
        voice = new MusicVoiceEmitter(this, apu);
    }

    public void start(MusicTrack track) {
        Objects.requireNonNull(track, "track");
        resetChannelState();
        trackBank = track.bank();

        apu.reset();
        writeRegister(GameBoyApu.NR52, 0x80);
        writeRegister(GameBoyApu.NR50, 0x77);
        writeRegister(GameBoyApu.NR51, 0xFF);

        int headerOffset = track.romOffset();
        requireReadable(headerOffset, HEADER_SIZE, trackBank, track.headerAddress());
        musicTranspose = (byte) romView.u8(RomAddress.fromOffset(headerOffset));
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

        voice.tickNoiseFfSequence();

        for (MusicChannelState state : channels) {
            if (!state.active) {
                continue;
            }
            if (state.lengthCounter > 0) {
                voice.tickArticulation(state);
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

    private final ParsePositions visited = new ParsePositions();

    private void parseUntilTimeConsumed(MusicChannelState state) {
        visited.clear();
        while (state.active) {
            if (!visited.add(state.streamPointer, state.programCounter, state.loopStart, state.loopRemaining)) {
                throw new IllegalStateException("Music channel loop consumed no time: channel "
                        + state.channel + " bank " + hex(trackBank)
                        + " address " + hex(state.programCounter));
            }
            int opcodeAddress = state.programCounter;
            int opcode = readByte(trackBank, state.programCounter);
            state.programCounter++;

            if (MusicOpcode.isEnd(opcode)) {
                loadNextDefinition(state);
            } else if (MusicOpcode.isRest(opcode)) {
                voice.silenceChannel(state);
                consumeTime(state);
                return;
            } else if (MusicOpcode.isDriverFlag(opcode)) {
                voice.applyDriverFlag(opcode);
            } else if (MusicOpcode.isBeginLoop(opcode)) {
                int count = readByte(trackBank, state.programCounter);
                state.programCounter++;
                state.loopStart = state.programCounter;
                state.loopRemaining = count;
            } else if (MusicOpcode.isNextLoop(opcode)) {
                applyLoop(state);
            } else if (MusicOpcode.isSetEnvelopeOrWaveform(opcode)) {
                voice.applyEnvelopeOrWaveform(state);
            } else if (MusicOpcode.isSetSpeed(opcode)) {
                speedTablePointer = readWord(trackBank, state.programCounter);
                state.programCounter += 2;
            } else if (MusicOpcode.isSetTranspose(opcode)) {
                musicTranspose = (byte) readByte(trackBank, state.programCounter);
                state.programCounter++;
            } else if (MusicOpcode.isNoteLength(opcode)) {
                state.selectedLength = readSpeed((opcode & 0x0F));
                voice.articulationTick[state.channel - 1] =
                        voice.articulationTickFor(state.selectedLength, voice.softwareEnvelope[state.channel - 1]);
            } else if (state.channel == 4 && (MusicOpcode.isPitchedNote(opcode) || MusicOpcode.isNoiseNote(opcode))) {
                voice.writeNoiseNote(state, opcode);
                consumeTime(state);
                return;
            } else if (MusicOpcode.isPitchedNote(opcode)) {
                voice.writePitchedNote(state, opcode + musicTranspose);
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

    private void loadNextDefinition(MusicChannelState state) {
        int guard = 0;
        while (true) {
            if (state.streamPointer == 0) {
                voice.silenceChannel(state);
                state.active = false;
                return;
            }
            int pointer = readWord(trackBank, state.streamPointer);
            state.streamPointer += 2;
            if (pointer == 0x0000) {
                voice.silenceChannel(state);
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

    private void consumeTime(MusicChannelState state) {
        state.lengthCounter = Math.max(1, state.selectedLength);
        int index = state.channel - 1;
        voice.noteTicks[index] = 0;
        voice.articulationApplied[index] = false;
    }

    private int readSpeed(int index) {
        return readByte(trackBank, speedTablePointer + index);
    }

    int readByte(int bank, int address) {
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
            throw new IllegalStateException(
                    "Music ROM read out of range: bank " + hex(bank) + " address " + hex(address));
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
            voice.softwareEnvelope[i] = 0;
            voice.articulationTick[i] = 0;
            voice.noteTicks[i] = 0;
            voice.articulationApplied[i] = false;
        }
        voice.waveformPointer = 0;
        musicTranspose = 0;
        voice.noiseFfPhase = 0;
        voice.noiseFfCountdown = 0;
        voice.channel3VolumeEffectMode = 0;
        voice.channel3VolumeEffectCounter = 0;
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

    static String hex(int value) {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }
}
