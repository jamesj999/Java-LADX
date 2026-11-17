package linksawakening.app.gameplay.entity;

import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.gameplay.interaction.DialogInteractions;
import linksawakening.app.state.GameplaySounds;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.world.room.RoomTransitionCoordinator;

/**
 * Side-effect ports for the ROM music/dialog/warp sequence. Retained implementations support recording fakes
 * for ordering tests.
 */
public interface EntityDialogRouting {
    static EntityDialogRouting direct(
            MusicDirector music,
            DialogInteractions dialogs,
            RoomTransitionCoordinator transitions,
            Link link,
            GameplaySounds sounds) {
        Objects.requireNonNull(music);
        Objects.requireNonNull(dialogs);
        Objects.requireNonNull(transitions);
        Objects.requireNonNull(link);
        Objects.requireNonNull(sounds);
        return new EntityDialogRouting() {
            @Override
            public void music(int track) {
                music.playDirectMusic(track);
            }

            @Override
            public void dialogs() {
                dialogs.openEntityDialogRequests();
            }

            @Override
            public void manbo() {
                if (transitions.handlePendingManboTransition(link))
                    sounds.sink().play(GameplaySoundEvent.MANBO_WARP);
            }

            @Override
            public void instrument() {
                transitions.handlePendingInstrumentTransition(link);
            }
        };
    }

    void music(int track);

    void dialogs();

    void manbo();

    void instrument();
}
