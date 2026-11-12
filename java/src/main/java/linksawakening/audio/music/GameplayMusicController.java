package linksawakening.audio.music;

import java.util.Objects;

public final class GameplayMusicController {
    private final AreaMusicResolver resolver;
    private final MusicCatalog catalog;
    private final MusicTrackPlayer player;

    private int currentTrackId = MusicTrackIds.MUSIC_NONE;
    private int defaultTrackId = MusicTrackIds.MUSIC_NONE;
    private boolean continueMusicAfterWarp;
    private int fadeOutCountdown;
    private int nextDefaultMusicTrack = MusicTrackIds.MUSIC_NONE;

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

        // RoomTransitionPrepareHandler.C1CFIsZero, $02:7AE9-7B34: scrolling
        // indoors/swordless leaves music alone; overworld scrolling compares
        // hDefaultMusicTrack, not the currently playing event track.
        int areaTrackId = resolver.resolveDefault(context);
        if (context.scrollingTransition()
                && (context.indoor() || context.swordLevel() == 0 || areaTrackId == defaultTrackId)) {
            return;
        }
        defaultTrackId = areaTrackId;
        int nextTrackId = resolver.resolve(context);
        nextDefaultMusicTrack = nextTrackId;
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
        // SwordShieldPickable.playSwordFanfare ($03:5BCE-5BD7), also
        // MarinAsFollower ($18:5A0F-5A16), set the default separately.
        if (nextTrackId == MusicTrackIds.MUSIC_OVERWORLD_INTRO) {
            defaultTrackId = MusicTrackIds.MUSIC_OVERWORLD;
            nextDefaultMusicTrack = MusicTrackIds.MUSIC_OVERWORLD;
        }
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

    /** ApplyLinkCollisionWithEnemy, $03:6DCE-6DDD. */
    public void restoreAfterPowerUpEnds(boolean inBossBattle, int defaultTrack) {
        if (inBossBattle) return;
        int track = defaultTrack & 0xFF;
        if (track != 0x22) playDirect(track);
        nextDefaultMusicTrack = track;
    }

    public int nextDefaultMusicTrack() {
        return nextDefaultMusicTrack;
    }

    public int defaultTrackId() {
        return defaultTrackId;
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
