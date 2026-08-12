package linksawakening.world;

import linksawakening.rom.RomBank;

/** Resolves label_2A07's two-stage overworld-room to owl-dialog lookup. */
public final class OwlEventDialogResolver {
    private static final int BANK = 0x01;
    private static final int LOOKUP_TABLE_ADDRESS = 0x5909;
    private static final int ROOM_TABLE_ADDRESS = 0x5959;
    private static final int OVERWORLD_MUSIC_TABLE_ADDRESS = 0x4000;

    private final byte[] romData;

    public OwlEventDialogResolver(byte[] romData) {
        int required = RomBank.romOffset(BANK, ROOM_TABLE_ADDRESS) + 0x100;
        if (romData == null || romData.length < required) {
            throw new IllegalArgumentException("ROM is too small for owl dialog tables");
        }
        this.romData = romData;
    }

    public int globalDialogId(int roomId) {
        if ((roomId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Owl room id must be an unsigned byte");
        }
        int roomTable = RomBank.romOffset(BANK, ROOM_TABLE_ADDRESS);
        int lookupIndex = Byte.toUnsignedInt(romData[roomTable + roomId]);
        int lookupTable = RomBank.romOffset(BANK, LOOKUP_TABLE_ADDRESS);
        int lowId = Byte.toUnsignedInt(romData[lookupTable + lookupIndex]);
        return lowId;
    }

    /** Returns the room's source default track from OverworldMusicTracks. */
    public int defaultMusicTrack(int roomId) {
        if ((roomId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Owl room id must be an unsigned byte");
        }
        return Byte.toUnsignedInt(romData[
            RomBank.romOffset(0x02, OVERWORLD_MUSIC_TABLE_ADDRESS) + roomId]);
    }
}
