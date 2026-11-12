package linksawakening.audio.sfx;

import java.util.Arrays;
import java.util.Objects;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.rom.Rom;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomSymbols;

final class RomSoundEffectEngine {
    private static final int PLAY_ACTIVE_JINGLE = RomSymbols.PlayActiveJingle.address();
    private static final int PLAY_ACTIVE_WAVE = RomSymbols.PlayActiveWaveSfx.address();
    private static final int PLAY_ACTIVE_NOISE = RomSymbols.PlayActiveNoiseSfx.address();

    private static final int W_ACTIVE_JINGLE = 0xD360;
    private static final int W_ACTIVE_WAVE = 0xD370;
    private static final int W_ACTIVE_NOISE = 0xD378;
    private static final int AUDIO_STATE_START = 0xD300;
    private static final int AUDIO_STATE_END = 0xD3FF;
    static final int MAX_INSTRUCTIONS_PER_TICK = 20_000;

    // Range-checked real-time reads retain the caller-owned bytes without mutation.
    // Avoid RomAddress construction on every interpreted instruction/operand fetch.
    private final linksawakening.rom.RomSlice romBytes;
    private final GameBoyApu apu;
    private static final SoundEffectNamespace[] NAMESPACES = SoundEffectNamespace.values();
    private final SfxCpu cpu = new SfxCpu(this);
    private final int[] memory = new int[0x10000];
    private final RomSoundEffectTables tables;

    RomSoundEffectEngine(Rom romData, GameBoyApu apu) {
        Objects.requireNonNull(romData, "romData");
        romBytes = romData.view(linksawakening.rom.RomAddress.fromOffset(0), romData.size());
        this.apu = Objects.requireNonNull(apu, "apu");
        tables = new RomSoundEffectTables(romData);
    }

    void start(SoundEffect effect) {
        Objects.requireNonNull(effect, "effect");
        tables.beginHandlerAddress(effect);
        tables.continueHandlerAddress(effect);
        // Requests and continuation ids are distinct in _PlaySfx (1F:401E).
        // Keep the other namespaces alive, including their ownership flags.
        write8(activeAddress(effect.namespace()), effect.id());
        if ((apu.readRegister(GameBoyApu.NR52) & 0x80) == 0) enableApu();
        cpu.runFrom(entryAddress(effect.namespace()));
        write8(activeAddress(effect.namespace()), 0);
    }

    void tick60Hz() {
        // ROM order matters: jingles can cancel wave effects.
        for (SoundEffectNamespace namespace : NAMESPACES) {
            cpu.runFrom(entryAddress(namespace));
        }
        write8(W_ACTIVE_JINGLE, 0);
        write8(W_ACTIVE_WAVE, 0);
        write8(W_ACTIVE_NOISE, 0);
    }

    boolean isPlaying() {
        return read8(0xD361) != 0
                || read8(0xD371) != 0
                || read8(0xD379) != 0
                || read8(W_ACTIVE_JINGLE) != 0
                || read8(W_ACTIVE_WAVE) != 0
                || read8(W_ACTIVE_NOISE) != 0;
    }

    boolean ownsChannel(int channel) {
        if (channel < 1 || channel > 4) throw new IllegalArgumentException("Channel out of range: " + channel);
        return (read8(0xD30F + channel * 0x10) & 0x80) != 0;
    }

    boolean takeWaveRestoreRequest() {
        boolean requested = read8(0xD3E7) != 0;
        write8(0xD3E7, 0);
        return requested;
    }

    void stop() {
        // Apply the ROM shutdown register sequence only to channels owned by SFX.
        // func_01F_7B5C / label_01F_632D; leave music-owned channels alone.
        boolean restoreWave = ownsChannel(3) || read8(0xD3E7) != 0;
        for (int channel = 1; channel <= 4; channel++) {
            if (!ownsChannel(channel)) continue;
            if (channel == 3) {
                apu.writeRegister(GameBoyApu.NR30, 0);
            } else {
                int envelope =
                        switch (channel) {
                            case 1 -> GameBoyApu.NR12;
                            case 2 -> GameBoyApu.NR22;
                            default -> GameBoyApu.NR42;
                        };
                apu.writeRegister(envelope, 0x08);
                apu.writeRegister(envelope + 2, 0x80);
                if (channel == 1) apu.writeRegister(GameBoyApu.NR10, 0);
            }
        }
        clearAudioState();
        if (restoreWave) write8(0xD3E7, 1);
    }

    private void enableApu() {
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        memory[0xFF00 | GameBoyApu.NR52] = 0x80;
        memory[0xFF00 | GameBoyApu.NR50] = 0x77;
        memory[0xFF00 | GameBoyApu.NR51] = 0xFF;
    }

    private void clearAudioState() {
        Arrays.fill(memory, AUDIO_STATE_START, AUDIO_STATE_END + 1, 0);
    }

    private static int entryAddress(SoundEffectNamespace namespace) {
        return switch (namespace) {
            case JINGLE -> PLAY_ACTIVE_JINGLE;
            case WAVE -> PLAY_ACTIVE_WAVE;
            case NOISE -> PLAY_ACTIVE_NOISE;
        };
    }

    private static int activeAddress(SoundEffectNamespace namespace) {
        return switch (namespace) {
            case JINGLE -> W_ACTIVE_JINGLE;
            case WAVE -> W_ACTIVE_WAVE;
            case NOISE -> W_ACTIVE_NOISE;
        };
    }

    int read8(int address) {
        address &= 0xFFFF;
        if (address >= 0x4000 && address <= 0x7FFF) {
            int offset = RomBank.romOffset(RomSoundEffectTables.SFX_BANK, address);
            if (offset < 0 || offset >= romBytes.size()) {
                throw new IllegalStateException("SFX ROM read outside bank 0x1F at 0x" + hex16(address));
            }
            return romBytes.u8(offset);
        }
        if (address >= 0xFF10 && address <= 0xFF26) {
            return apu.readRegister(address & 0xFF);
        }
        if (address >= 0xFF30 && address <= 0xFF3F) {
            return apu.readWaveRam(address - 0xFF30);
        }
        return memory[address] & 0xFF;
    }

    void write8(int address, int value) {
        address &= 0xFFFF;
        int unsigned = value & 0xFF;
        memory[address] = unsigned;
        if (address >= 0xFF10 && address <= 0xFF26) {
            apu.writeRegister(address & 0xFF, unsigned);
        } else if (address >= 0xFF30 && address <= 0xFF3F) {
            apu.writeWaveRam(address - 0xFF30, unsigned);
        }
    }

    static int signed8(int value) {
        return (byte) (value & 0xFF);
    }

    static int unsigned16(int value) {
        return value & 0xFFFF;
    }

    static String hex8(int value) {
        return String.format("%02X", value & 0xFF);
    }

    static String hex16(int value) {
        return String.format("%04X", value & 0xFFFF);
    }
}
