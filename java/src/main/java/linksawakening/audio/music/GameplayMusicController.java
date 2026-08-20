package linksawakening.audio.music;

import java.util.Objects;

public final class GameplayMusicController {
    private final AreaMusicResolver resolver;
    private final MusicCatalog catalog;
    private final MusicTrackPlayer player;

    private int currentTrackId = MusicTrackIds.MUSIC_NONE;
    private boolean continueMusicAfterWarp;
    private int fadeOutCountdown;

    public GameplayMusicController(AreaMusicResolver resolver, MusicCatalog catalog, MusicTrackPlayer player) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.player = Objects.requireNonNull(player, "player");
    }

    public void selectAfterTransition(RoomMusicContext context) {
        Objects.requireNonNull(context, "context");
        fadeOutCountdown = 0;
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
        fadeOutCountdown = 0;
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

    public void requestFadeOut(int countdown) {
        if (countdown < 0 || countdown > 0xFF) {
            throw new IllegalArgumentException("Music fade countdown must be an unsigned byte");
        }
        fadeOutCountdown = countdown;
    }

    public void tickFadeOut() {
        if (fadeOutCountdown == 0) {
            return;
        }
        fadeOutCountdown--;
        if (fadeOutCountdown == 0) {
            player.stop();
            currentTrackId = MusicTrackIds.MUSIC_NONE;
        }
    }

    public int fadeOutCountdown() {
        return fadeOutCountdown;
    }
}
