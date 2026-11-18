package linksawakening.world.room;

import java.util.List;
import java.util.Objects;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.ScreenShakeRequest;

public final class PresentationOutputs {
    private final RoomEntityOutputSource source;

    PresentationOutputs(RoomEntityOutputSource source) {
        this.source = Objects.requireNonNull(source);
    }

    public List<ScreenShakeRequest> consumeScreenShakeRequests() {
        return source.consumeScreenShakeRequests();
    }

    public List<EntityCombatEvent> consumeEntityEvents() {
        return source.consumeEntityEvents();
    }

    public int consumePendingMusicFadeOutCountdown() {
        return source.consumePendingMusicFadeOutCountdown();
    }

    public int consumePendingMusicTrack() {
        return source.consumePendingMusicTrack();
    }

    public List<LikeLikeEvent> consumeLikeLikeEvents() {
        return source.consumeLikeLikeEvents();
    }

    public void drainEntityEvents(RoomFrameDrain<EntityCombatEvent> target) {
        source.drainEntityEvents(target);
    }

    public linksawakening.world.entity.event.DialogRequest consumeFirstEntityDialogRequest() {
        return source.consumeFirstEntityDialogRequest();
    }
}
