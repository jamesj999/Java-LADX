package linksawakening.audio.sfx;

import java.util.Objects;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;

/** Reads banked ROM SFX handler pointer tables. */
public final class RomSoundEffectTables {
    static final int SFX_BANK = RomSymbols.SfxPointersTable1.bank();
    static final int JINGLE_BEGIN_TABLE = RomSymbols.SfxPointersTable1.address();
    static final int JINGLE_CONTINUE_TABLE = RomSymbols.SfxPointersTable2.address();
    static final int WAVE_BEGIN_TABLE = RomSymbols.WaveSfxHandlersTable1.address();
    static final int WAVE_CONTINUE_TABLE = RomSymbols.WaveSfxHandlersTable2.address();
    static final int NOISE_BEGIN_TABLE = RomSymbols.NoiseSfxHandlersTable1.address();
    static final int NOISE_CONTINUE_TABLE = RomSymbols.NoiseSfxHandlersTable2.address();

    private final Rom romData;

    public RomSoundEffectTables(Rom romData) {
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
        int offset = new RomAddress(SFX_BANK, table + ((id - 1) * 2)).offset();
        requireReadable(offset, 2);
        return romData.u16le(new RomAddress(SFX_BANK, table + ((id - 1) * 2)));
    }

    private void requireReadable(int offset, int length) {
        if (offset < 0 || offset + length > romData.size()) {
            throw new IllegalArgumentException("SFX table read outside ROM at offset 0x"
                    + Integer.toHexString(offset).toUpperCase());
        }
    }
}
