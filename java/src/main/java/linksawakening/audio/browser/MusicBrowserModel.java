package linksawakening.audio.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import linksawakening.audio.music.MusicTrack;

final class MusicBrowserModel {
    private final List<MusicTrack> tracks;

    public MusicBrowserModel(List<MusicTrack> tracks) {
        this.tracks = List.copyOf(Objects.requireNonNull(tracks, "tracks"));
    }

    public List<MusicTrack> filteredTracks(String filter) {
        String normalizedFilter = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        if (normalizedFilter.isEmpty()) {
            return tracks;
        }

        List<MusicTrack> filtered = new ArrayList<>();
        for (MusicTrack track : tracks) {
            String id = hexByte(track.id()).toLowerCase(Locale.ROOT);
            String name = track.name().toLowerCase(Locale.ROOT);
            if (id.contains(normalizedFilter) || name.contains(normalizedFilter)) {
                filtered.add(track);
            }
        }
        return filtered;
    }

    public String metadata(MusicTrack track) {
        Objects.requireNonNull(track, "track");
        return hexByte(track.id()) + " " + track.name()
                + System.lineSeparator()
                + "bank " + hexByte(track.bank())
                + System.lineSeparator()
                + "header " + hexWord(track.headerAddress())
                + System.lineSeparator()
                + "rom " + hexOffset(track.romOffset());
    }

    static String hexByte(int value) {
        return String.format(Locale.ROOT, "0x%02X", value & 0xFF);
    }

    private static String hexWord(int value) {
        return String.format(Locale.ROOT, "0x%04X", value & 0xFFFF);
    }

    private static String hexOffset(int value) {
        return String.format(Locale.ROOT, "0x%05X", value);
    }
}
