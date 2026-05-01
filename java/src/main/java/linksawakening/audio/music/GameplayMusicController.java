package linksawakening.audio.music;

import java.util.Objects;

public final class GameplayMusicController {
    private final AreaMusicResolver resolver;
    private final MusicCatalog catalog;
    private final MusicTrackPlayer player;

    private int currentTrackId = MusicTrackIds.MUSIC_NONE;
    private boolean continueMusicAfterWarp;

    public GameplayMusicController(AreaMusicResolver resolver, MusicCatalog catalog, MusicTrackPlayer player) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.player = Objects.requireNonNull(player, "player");
    }

    public void selectAfterTransition(RoomMusicContext context) {
        Objects.requireNonNull(context, "context");
        if (continueMusicAfterWarp) {
            continueMusicAfterWarp = false;
            return;
        }

        int nextTrackId = resolver.resolve(context);
        if (nextTrackId == currentTrackId) {
            return;
        }

        currentTrackId = nextTrackId;
        if (nextTrackId == MusicTrackIds.MUSIC_NONE || nextTrackId == MusicTrackIds.MUSIC_SILENCE) {
            player.stop();
            return;
        }
        player.play(catalog.requireTrack(nextTrackId));
    }

    public void playDirect(int trackId) {
        continueMusicAfterWarp = false;
        int nextTrackId = trackId & 0xFF;
        if (nextTrackId == currentTrackId) {
            return;
        }

        currentTrackId = nextTrackId;
        if (nextTrackId == MusicTrackIds.MUSIC_NONE || nextTrackId == MusicTrackIds.MUSIC_SILENCE) {
            player.stop();
            return;
        }
        player.play(catalog.requireTrack(nextTrackId));
    }

    public void continueMusicAfterNextWarp() {
        continueMusicAfterWarp = true;
    }

    public int currentTrackId() {
        return currentTrackId;
    }
}
