package linksawakening.audio.sfx;

import linksawakening.rom.RomBank;

import java.util.Objects;

public final class RomSoundEffectTables {
    static final int SFX_BANK = 0x1F;
    static final int JINGLE_BEGIN_TABLE = 0x4100;
    static final int JINGLE_CONTINUE_TABLE = 0x4182;
    static final int WAVE_BEGIN_TABLE = 0x541B;
    static final int WAVE_CONTINUE_TABLE = 0x5461;
    static final int NOISE_BEGIN_TABLE = 0x63EC;
    static final int NOISE_CONTINUE_TABLE = 0x646C;

    private final byte[] romData;

    public RomSoundEffectTables(byte[] romData) {
        this.romData = Objects.requireNonNull(romData, "romData");
    }

    public int beginHandlerAddress(SoundEffect effect) {
        return handlerAddress(effect.namespace(), effect.id(), true);
    }

    public int continueHandlerAddress(SoundEffect effect) {
        return handlerAddress(effect.namespace(), effect.id(), false);
    }

    int tableBase(SoundEffectNamespace namespace, boolean begin) {
        return switch (namespace) {
            case JINGLE -> begin ? JINGLE_BEGIN_TABLE : JINGLE_CONTINUE_TABLE;
            case WAVE -> begin ? WAVE_BEGIN_TABLE : WAVE_CONTINUE_TABLE;
            case NOISE -> begin ? NOISE_BEGIN_TABLE : NOISE_CONTINUE_TABLE;
        };
    }

    public int handlerAddress(SoundEffectNamespace namespace, int id, boolean begin) {
        Objects.requireNonNull(namespace, "namespace");
        if (id <= 0 || id > 0xFF) {
            throw new IllegalArgumentException("SFX id out of range: " + id);
        }
        int table = tableBase(namespace, begin);
        int offset = RomBank.romOffset(SFX_BANK, table + ((id - 1) * 2));
        requireReadable(offset, 2);
        int low = Byte.toUnsignedInt(romData[offset]);
        int high = Byte.toUnsignedInt(romData[offset + 1]);
        return low | (high << 8);
    }

    private void requireReadable(int offset, int length) {
        if (offset < 0 || offset + length > romData.length) {
            throw new IllegalArgumentException("SFX table read outside ROM at offset 0x"
                    + Integer.toHexString(offset).toUpperCase());
        }
    }
}
