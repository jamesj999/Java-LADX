package linksawakening.audio.music;

import java.util.Objects;

public final class GameplayMusicController {
    private final AreaMusicResolver resolver;
    private final MusicCatalog catalog;
    private final MusicTrackPlayer player;

    private int currentTrackId = MusicTrackIds.MUSIC_NONE;
    private boolean continueMusicAfterWarp;
    private int areaTrackId = MusicTrackIds.MUSIC_NONE;

    public GameplayMusicController(AreaMusicResolver resolver, MusicCatalog catalog, MusicTrackPlayer player) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.player = Objects.requireNonNull(player, "player");
    }

    public void selectAfterTransition(RoomMusicContext context) {
        selectAfterTransition(context, false);
    }

    public void selectAfterTransition(RoomMusicContext context, boolean adjacentScroll) {
        Objects.requireNonNull(context, "context");
        if (continueMusicAfterWarp) {
            continueMusicAfterWarp = false;
            return;
        }

        int previousAreaTrack = areaTrackId;
        areaTrackId = resolver.areaTrack(context);
        if (adjacentScroll && context.activePowerUp() != 0 && playingPowerUpMusic()
            && (context.indoor() || areaTrackId == previousAreaTrack)) {
            return;
        }
        int selected = resolver.trackWithPowerUp(areaTrackId, context.activePowerUp());
        // The four channel streams of $27 already fall through into the $49 loop.
        if (selected == MusicTrackIds.MUSIC_ACTIVE_POWER_UP && playingPowerUpMusic()) return;
        playDirect(selected);
    }

    public void playResidentTrack(int trackId) {
        areaTrackId = trackId;
        playDirect(trackId);
    }

    public void finishPowerUp() {
        if (playingPowerUpMusic()) playDirect(areaTrackId);
    }

    private boolean playingPowerUpMusic() {
        return currentTrackId == MusicTrackIds.MUSIC_OBTAIN_POWERUP
            || currentTrackId == MusicTrackIds.MUSIC_ACTIVE_POWER_UP;
    }

    public int areaTrackId() {
        return areaTrackId;
    }

    public void playDirect(int trackId) {
        continueMusicAfterWarp = false;
        int nextTrackId = trackId & 0xFF;
        if (nextTrackId == currentTrackId) {
            return;
        }

        playRequestedTrack(nextTrackId);
    }

    /** A nonzero ROM request restarts even a score that is already playing. */
    public void playRequestedTrack(int trackId) {
        int nextTrackId = trackId & 0xFF;
        currentTrackId = nextTrackId;
        if (nextTrackId == MusicTrackIds.MUSIC_NONE || nextTrackId == MusicTrackIds.MUSIC_SILENCE) {
            player.stop();
            return;
        }
        player.play(catalog.requireTrack(nextTrackId));
    }

    /** The ROM effect handler has already stopped playback. */
    public void effectSilencedMusic() {
        currentTrackId = MusicTrackIds.MUSIC_NONE;
    }

    public void continueMusicAfterNextWarp() {
        continueMusicAfterWarp = true;
    }

    public int currentTrackId() {
        return currentTrackId;
    }
}
