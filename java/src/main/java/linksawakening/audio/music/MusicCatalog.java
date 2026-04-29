package linksawakening.audio.music;

import linksawakening.rom.RomBank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MusicCatalog {
    private static final int BANK_1B = 0x1B;
    private static final int BANK_1B_TABLE = 0x4077;
    private static final int BANK_1B_ENTRIES = 0x30;
    private static final int BANK_1E = 0x1E;
    private static final int BANK_1E_TABLE = 0x407F;
    private static final int BANK_1E_ENTRIES = 0x40;

    private static final String[] TRACK_NAMES = {
            null,
            "MUSIC_TITLE_SCREEN",
            "MUSIC_MINIGAME",
            "MUSIC_GAME_OVER",
            "MUSIC_MABE_VILLAGE",
            "MUSIC_OVERWORLD",
            "MUSIC_TAL_TAL_RANGE",
            "MUSIC_SHOP",
            "MUSIC_RAFT_RIDE_RAPIDS",
            "MUSIC_MYSTERIOUS_FOREST",
            "MUSIC_INSIDE_BUILDING",
            "MUSIC_ANIMAL_VILLAGE",
            "MUSIC_FAIRY_FOUNTAIN",
            "MUSIC_TITLE_SCREEN_NO_INTRO",
            "MUSIC_BOWWOW_KIDNAPPED",
            "MUSIC_OBTAIN_SWORD",
            "MUSIC_OBTAIN_ITEM",
            "MUSIC_FILE_SELECT",
            "MUSIC_EGG_MAZE",
            "MUSIC_KANALET_CASTLE",
            "MUSIC_TAIL_CAVE",
            "MUSIC_BOTTLE_GROTTO",
            "MUSIC_KEY_CAVERN",
            "MUSIC_ANGLERS_TUNNEL",
            "MUSIC_AFTER_BOSS",
            "MUSIC_BOSS",
            "MUSIC_TITLE_CUTSCENE",
            "MUSIC_OBTAIN_INSTRUMENT",
            "MUSIC_INTRO_WAKE_UP",
            "MUSIC_OVERWORLD_SWORDLESS",
            "MUSIC_DREAM_SHRINE_SLEEP",
            "MUSIC_SOUTHERN_SHRINE",
            "MUSIC_INSTRUMENT_FULL_MOON_CELLO",
            "MUSIC_2D_UNDERGROUND",
            "MUSIC_OWL",
            "MUSIC_FINAL_BOSS",
            "MUSIC_DREAM_SHRINE_BED",
            "MUSIC_HEART_CONTAINER",
            "MUSIC_CAVE",
            "MUSIC_OBTAIN_POWERUP",
            "MUSIC_INSTRUMENT_CONCH_HORN",
            "MUSIC_INSTRUMENT_SEA_LILYS_BELL",
            "MUSIC_INSTRUMENT_SURF_HARP",
            "MUSIC_INSTRUMENT_WIND_MARIMBA",
            "MUSIC_INSTRUMENT_CORAL_TRIANGLE",
            "MUSIC_INSTRUMENT_ORGAN_OF_EVENING_CALM",
            "MUSIC_INSTRUMENT_THUNDER_DRUM",
            "MUSIC_MARIN_SING",
            "MUSIC_MANBOS_MAMBO",
            "MUSIC_OVERWORLD_INTRO",
            "MUSIC_MR_WRITE_HOUSE",
            "MUSIC_ULRIRA",
            "MUSIC_TARIN_BEES",
            "MUSIC_MAMU_FROG_SONG",
            "MUSIC_MONKEYS_BUILDING_BRIDGE",
            "MUSIC_CHRISTINE_HOUSE",
            "MUSIC_TOTAKA_UNUSED",
            "MUSIC_TURTLE_ROCK_ENTRANCE_BOSS",
            "MUSIC_FISHERMAN_UNDER_BRIDGE",
            "MUSIC_OBTAIN_ITEM_UNUSED",
            "MUSIC_FILE_SELECT_TOTAKA",
            "MUSIC_ENDING",
            "MUSIC_MOBLIN_HIDEOUT",
            "MUSIC_ISLAND_DISAPPEAR",
            "MUSIC_RICHARD_HOUSE",
            "MUSIC_EGG_BALLAD_DEFAULT",
            "MUSIC_EGG_BALLAD_BELL",
            "MUSIC_EGG_BALLAD_HARP",
            "MUSIC_EGG_BALLAD_MARIMBA",
            "MUSIC_EGG_BALLAD_TRIANGLE",
            "MUSIC_EGG_BALLAD_ORGAN",
            "MUSIC_EGG_BALLAD_ALL",
            "MUSIC_GHOST_HOUSE",
            "MUSIC_ACTIVE_POWER_UP",
            "MUSIC_LEARN_BALLAD",
            "MUSIC_CATFISHS_MAW",
            "MUSIC_OPEN_ANGLERS_TUNNEL",
            "MUSIC_MARIN_ON_BEACH",
            "MUSIC_MARIN_BEACH_TALK",
            "MUSIC_MARIN_UNUSED",
            "MUSIC_MINIBOSS",
            "MUSIC_KANALET_CASTLE_COPY",
            "MUSIC_TAIL_CAVE_COPY",
            "MUSIC_DREAM_SHRINE_DREAM",
            "MUSIC_EAGLE_BOSS_TRANSITION",
            "MUSIC_ROOSTER_REVIVAL",
            "MUSIC_L2_SWORD",
            "MUSIC_HENHOUSE",
            "MUSIC_FACE_SHRINE",
            "MUSIC_WIND_FISH",
            "MUSIC_TURTLE_ROCK",
            "MUSIC_EAGLES_TOWER",
            "MUSIC_EAGLE_BOSS_LOOP",
            "MUSIC_FINAL_BOSS_INTRO",
            "MUSIC_BOSS_DEFEAT",
            "MUSIC_FINAL_BOSS_DEFEAT",
            "MUSIC_FILE_SELECT_ZELDA",
            "MUSIC_COLOR_DUNGEON",
            "MUSIC_UNKNOWN_62",
            "MUSIC_UNKNOWN_63",
            "MUSIC_UNKNOWN_64",
            "MUSIC_UNKNOWN_65",
            "MUSIC_UNKNOWN_66",
            "MUSIC_UNKNOWN_67",
            "MUSIC_UNKNOWN_68",
            "MUSIC_UNKNOWN_69",
            "MUSIC_UNKNOWN_6A",
            "MUSIC_UNKNOWN_6B",
            "MUSIC_UNKNOWN_6C",
            "MUSIC_UNKNOWN_6D",
            "MUSIC_UNKNOWN_6E",
            "MUSIC_UNKNOWN_6F",
            "MUSIC_UNKNOWN_70"
    };

    private final List<MusicTrack> tracks;
    private final Map<Integer, MusicTrack> tracksById;

    private MusicCatalog(List<MusicTrack> tracks) {
        this.tracks = List.copyOf(tracks);
        Map<Integer, MusicTrack> byId = new HashMap<>();
        for (MusicTrack track : tracks) {
            MusicTrack previous = byId.put(track.id(), track);
            if (previous != null) {
                throw new IllegalStateException("Duplicate music track id " + formatHex(track.id()));
            }
        }
        this.tracksById = Map.copyOf(byId);
    }

    public static MusicCatalog fromRom(byte[] romData) {
        Objects.requireNonNull(romData, "romData");
        List<MusicTrack> tracks = new ArrayList<>(0x70);
        addBank1BTracks(romData, tracks);
        addBank1ETracks(romData, tracks);
        tracks.sort(Comparator.comparingInt(MusicTrack::id));
        return new MusicCatalog(tracks);
    }

    public List<MusicTrack> tracks() {
        return tracks;
    }

    public Optional<MusicTrack> track(int id) {
        return Optional.ofNullable(tracksById.get(id));
    }

    public MusicTrack requireTrack(int id) {
        MusicTrack track = tracksById.get(id);
        if (track == null) {
            throw new IllegalArgumentException("Unknown music track id " + formatHex(id));
        }
        return track;
    }

    private static void addBank1BTracks(byte[] romData, List<MusicTrack> tracks) {
        for (int inputId = 1; inputId <= BANK_1B_ENTRIES; inputId++) {
            int publicId;
            if (inputId <= 0x10) {
                publicId = inputId;
            } else if (inputId <= 0x20) {
                publicId = inputId + 0x20;
            } else {
                publicId = inputId + 0x40;
            }
            tracks.add(trackFromPointerTable(romData, publicId, BANK_1B, BANK_1B_TABLE, inputId));
        }
    }

    private static void addBank1ETracks(byte[] romData, List<MusicTrack> tracks) {
        for (int inputId = 1; inputId <= BANK_1E_ENTRIES; inputId++) {
            int publicId = inputId <= 0x20 ? inputId + 0x10 : inputId + 0x20;
            tracks.add(trackFromPointerTable(romData, publicId, BANK_1E, BANK_1E_TABLE, inputId));
        }
    }

    private static MusicTrack trackFromPointerTable(byte[] romData, int id, int bank, int tableStart, int inputId) {
        int pointerAddress = tableStart + ((inputId - 1) * 2);
        int pointerOffset = RomBank.romOffset(bank, pointerAddress);
        requireReadableWord(romData, pointerOffset, bank, pointerAddress);
        int headerAddress = readLittleEndianWord(romData, pointerOffset);
        int romOffset = RomBank.romOffset(bank, headerAddress);
        return new MusicTrack(id, nameFor(id), bank, pointerAddress, headerAddress, romOffset);
    }

    private static int readLittleEndianWord(byte[] romData, int offset) {
        return Byte.toUnsignedInt(romData[offset]) | (Byte.toUnsignedInt(romData[offset + 1]) << 8);
    }

    private static void requireReadableWord(byte[] romData, int offset, int bank, int address) {
        if (offset < 0 || offset + 1 >= romData.length) {
            throw new IllegalArgumentException(
                    "Music pointer table entry out of ROM range: bank " + formatHex(bank)
                            + " address " + formatHex(address));
        }
    }

    private static String nameFor(int id) {
        if (id <= 0 || id >= TRACK_NAMES.length || TRACK_NAMES[id] == null) {
            throw new IllegalArgumentException("No music track name for id " + formatHex(id));
        }
        return TRACK_NAMES[id];
    }

    private static String formatHex(int value) {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }
}
