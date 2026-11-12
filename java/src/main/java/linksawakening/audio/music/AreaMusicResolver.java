package linksawakening.audio.music;

import java.util.Objects;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;

/** Resolves area music through ROM tables and source-shaped overrides. */
public final class AreaMusicResolver {
    private static final int SELECT_MUSIC_BANK = RomSymbols.OverworldMusicTracks.bank();
    private static final int OVERWORLD_MUSIC_TRACKS_ADDR = RomSymbols.OverworldMusicTracks.address();
    private static final int OVERWORLD_MUSIC_TRACKS_LENGTH = 0x100;
    private static final int HOUSE_MUSIC_TRACKS_ADDR = RomSymbols.HouseMusicTracks.address();
    private static final int HOUSE_MUSIC_TRACKS_LENGTH = 0x20;
    private static final int MUSIC_OVERRIDES_POWER_UP_ADDR = RomSymbols.MusicOverridesPowerUpTrack.address();
    private static final int MUSIC_OVERRIDES_POWER_UP_LENGTH = 0x26;

    private static final int MAP_COLOR_DUNGEON = 0xFF;
    private static final int MAP_HOUSE = 0x10;
    private static final int ROOM_INDOOR_B_CAMERA_SHOP = 0xB5;

    private final int[] overworldMusicTracks;
    private final int[] houseMusicTracks;
    private final int[] musicOverridesPowerUpTrack;

    private AreaMusicResolver(int[] overworldMusicTracks, int[] houseMusicTracks, int[] musicOverridesPowerUpTrack) {
        this.overworldMusicTracks = overworldMusicTracks;
        this.houseMusicTracks = houseMusicTracks;
        this.musicOverridesPowerUpTrack = musicOverridesPowerUpTrack;
    }

    public static AreaMusicResolver fromRom(Rom romData) {
        Objects.requireNonNull(romData, "romData");
        return new AreaMusicResolver(
                readTable(romData, OVERWORLD_MUSIC_TRACKS_ADDR, OVERWORLD_MUSIC_TRACKS_LENGTH),
                readTable(romData, HOUSE_MUSIC_TRACKS_ADDR, HOUSE_MUSIC_TRACKS_LENGTH),
                readTable(romData, MUSIC_OVERRIDES_POWER_UP_ADDR, MUSIC_OVERRIDES_POWER_UP_LENGTH));
    }

    public int resolve(RoomMusicContext context) {
        int trackId = resolveDefault(context);
        if (context.activePowerUp() != 0 && !overridesPowerUpTrack(trackId)) {
            return MusicTrackIds.MUSIC_ACTIVE_POWER_UP;
        }
        return trackId;
    }

    /** hDefaultMusicTrack before the optional power-up override ($02:41AA). */
    public int resolveDefault(RoomMusicContext context) {
        Objects.requireNonNull(context, "context");
        if (context.swordLevel() == 0) {
            return MusicTrackIds.MUSIC_OVERWORLD_SWORDLESS;
        }

        int trackId;
        if (!context.indoor()) {
            trackId = overworldMusicTracks[context.roomId()];
        } else if (context.bossDefeated()) {
            trackId = MusicTrackIds.MUSIC_AFTER_BOSS;
        } else {
            int mapIndex = houseMusicMapIndex(context.mapId(), context.roomId());
            trackId = houseMusicTracks[mapIndex];
            if (context.sideScrolling() && mapIndex < 0x0A) {
                trackId = MusicTrackIds.MUSIC_2D_UNDERGROUND;
            }
        }

        return trackId;
    }

    public boolean overridesPowerUpTrack(int trackId) {
        int id = trackId & 0xFF;
        return id < musicOverridesPowerUpTrack.length && musicOverridesPowerUpTrack[id] != 0;
    }

    private static int houseMusicMapIndex(int mapId, int roomId) {
        int map = mapId & 0xFF;
        if (map == MAP_COLOR_DUNGEON) {
            return 0x09;
        }
        if (map == MAP_HOUSE && (roomId & 0xFF) == ROOM_INDOOR_B_CAMERA_SHOP) {
            return 0x0F;
        }
        if (map >= HOUSE_MUSIC_TRACKS_LENGTH) {
            throw new IllegalArgumentException("Indoor map id out of music table range: 0x"
                    + Integer.toHexString(map).toUpperCase());
        }
        return map;
    }

    private static int[] readTable(Rom romData, int address, int length) {
        int offset = new RomAddress(SELECT_MUSIC_BANK, address).offset();
        if (offset < 0 || offset + length > romData.size()) {
            throw new IllegalArgumentException("Music selection table out of ROM range: bank 0x"
                    + Integer.toHexString(SELECT_MUSIC_BANK).toUpperCase()
                    + " address 0x" + Integer.toHexString(address).toUpperCase());
        }
        return romData.u8Table(RomAddress.fromOffset(offset), length);
    }

    @Deprecated(forRemoval = true)
    public static AreaMusicResolver fromRom(byte[] romData) {
        return fromRom(new Rom(romData));
    }
}
