package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.ScreenShakeRequest;

public final class PresentationOutputs {
    private final RoomEntityOutputSource session;
    private final Dependencies dependencies;

    PresentationOutputs(Dependencies dependencies, RoomEntityOutputSource session) {
        this.dependencies = dependencies;
        this.session = java.util.Objects.requireNonNull(session);
    }

    public List<ScreenShakeRequest> consumeScreenShakeRequests() {
        return session.consumeScreenShakeRequests();
    }

    public List<EntityCombatEvent> consumeEntityEvents() {
        return session.consumeEntityEvents();
    }

    public int consumePendingMusicFadeOutCountdown() {
        return session.consumePendingMusicFadeOutCountdown();
    }

    public int consumePendingMusicTrack() {
        return session.consumePendingMusicTrack();
    }

    public List<LikeLikeEvent> consumeLikeLikeEvents() {
        return session.consumeLikeLikeEvents();
    }

    public void drainEntityEvents(RoomFrameDrain<EntityCombatEvent> target) {
        session.drainEntityEvents(target);
    }

    public linksawakening.world.entity.event.DialogRequest consumeFirstEntityDialogRequest() {
        return session.consumeFirstEntityDialogRequest();
    }

    record Dependencies(RoomEntitySessionState entityRuntimeState) {
        Dependencies {
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
        }
    }
}
