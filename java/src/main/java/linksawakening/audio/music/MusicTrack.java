package linksawakening.audio.music;

/** Immutable music identity and source ROM addressing metadata. */
public record MusicTrack(int id, String name, int bank, int pointerTableAddress, int headerAddress, int romOffset) {}
