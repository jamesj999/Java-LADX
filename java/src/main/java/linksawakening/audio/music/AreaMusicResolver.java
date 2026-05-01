package linksawakening.audio.music;

import linksawakening.rom.RomBank;

import java.util.Objects;

public final class AreaMusicResolver {
    private static final int SELECT_MUSIC_BANK = 0x02;
    private static final int OVERWORLD_MUSIC_TRACKS_ADDR = 0x4000;
    private static final int OVERWORLD_MUSIC_TRACKS_LENGTH = 0x100;
    private static final int HOUSE_MUSIC_TRACKS_ADDR = 0x4100;
    private static final int HOUSE_MUSIC_TRACKS_LENGTH = 0x20;
    private static final int MUSIC_OVERRIDES_POWER_UP_ADDR = 0x4120;
    private static final int MUSIC_OVERRIDES_POWER_UP_LENGTH = 0x26;

    private static final int MAP_COLOR_DUNGEON = 0xFF;
    private static final int MAP_HOUSE = 0x10;
    private static final int ROOM_INDOOR_B_CAMERA_SHOP = 0xB5;

    private final int[] overworldMusicTracks;
    private final int[] houseMusicTracks;
    private final int[] musicOverridesPowerUpTrack;

    private AreaMusicResolver(int[] overworldMusicTracks,
                              int[] houseMusicTracks,
                              int[] musicOverridesPowerUpTrack) {
        this.overworldMusicTracks = overworldMusicTracks;
        this.houseMusicTracks = houseMusicTracks;
        this.musicOverridesPowerUpTrack = musicOverridesPowerUpTrack;
    }

    public static AreaMusicResolver fromRom(byte[] romData) {
        Objects.requireNonNull(romData, "romData");
        return new AreaMusicResolver(
            readTable(romData, OVERWORLD_MUSIC_TRACKS_ADDR, OVERWORLD_MUSIC_TRACKS_LENGTH),
            readTable(romData, HOUSE_MUSIC_TRACKS_ADDR, HOUSE_MUSIC_TRACKS_LENGTH),
            readTable(romData, MUSIC_OVERRIDES_POWER_UP_ADDR, MUSIC_OVERRIDES_POWER_UP_LENGTH)
        );
    }

    public int resolve(RoomMusicContext context) {
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

        if (context.activePowerUp() != 0 && !overridesPowerUpTrack(trackId)) {
            return MusicTrackIds.MUSIC_ACTIVE_POWER_UP;
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

    private static int[] readTable(byte[] romData, int address, int length) {
        int offset = RomBank.romOffset(SELECT_MUSIC_BANK, address);
        if (offset < 0 || offset + length > romData.length) {
            throw new IllegalArgumentException("Music selection table out of ROM range: bank 0x"
                + Integer.toHexString(SELECT_MUSIC_BANK).toUpperCase()
                + " address 0x" + Integer.toHexString(address).toUpperCase());
        }
        int[] table = new int[length];
        for (int i = 0; i < length; i++) {
            table[i] = Byte.toUnsignedInt(romData[offset + i]);
        }
        return table;
    }
}
