package linksawakening.audio.music;

public record MusicTrack(
        int id,
        String name,
        int bank,
        int pointerTableAddress,
        int headerAddress,
        int romOffset) {
}
