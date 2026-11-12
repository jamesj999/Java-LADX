package linksawakening.world.room;

import linksawakening.rom.*;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;

/** Resolves label_2A07's two-stage overworld-room to owl-dialog lookup. */
public final class OwlEventDialogResolver {
    private static final int BANK = RomSymbols.MapSpecialLocationNamesLookupTable.bank();
    private static final int LOOKUP_TABLE_ADDRESS = RomSymbols.MapSpecialLocationNamesLookupTable.address();
    private static final int ROOM_TABLE_ADDRESS = RomSymbols.MapSpecialLocationNamesTable.address();
    private static final int OVERWORLD_MUSIC_TABLE_ADDRESS = RomSymbols.OverworldMusicTracks.address();

    private final Rom romData;

    public OwlEventDialogResolver(Rom romData) {
        int required = Math.max(
                RomBank.romOffset(BANK, ROOM_TABLE_ADDRESS) + 0x100,
                RomBank.romOffset(RomSymbols.OverworldMusicTracks.bank(), OVERWORLD_MUSIC_TABLE_ADDRESS) + 0x100);
        if (romData == null || romData.size() < required) {
            throw new IllegalArgumentException("ROM is too small for owl dialog tables");
        }
        this.romData = romData;
    }

    public int globalDialogId(int roomId) {
        if ((roomId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Owl room id must be an unsigned byte");
        }
        int roomTable = RomBank.romOffset(BANK, ROOM_TABLE_ADDRESS);
        int lookupIndex = romData.u8(RomAddress.fromOffset(roomTable + roomId));
        int lookupTable = RomBank.romOffset(BANK, LOOKUP_TABLE_ADDRESS);
        int lowId = romData.u8(RomAddress.fromOffset(lookupTable + lookupIndex));
        return lowId;
    }

    /** Returns the room's source default track from OverworldMusicTracks. */
    public int defaultMusicTrack(int roomId) {
        if ((roomId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Owl room id must be an unsigned byte");
        }
        return romData.u8(RomAddress.fromOffset(
                RomBank.romOffset(RomSymbols.OverworldMusicTracks.bank(), OVERWORLD_MUSIC_TABLE_ADDRESS) + roomId));
    }
}
