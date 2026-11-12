package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.List;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.ScreenShakeRequest;

public final class PresentationOutputs {
    private final RoomSession session;

    PresentationOutputs(RoomSession session) {
        this.session = session;
    }

    public List<ScreenShakeRequest> consumeScreenShakeRequests() {
        return session.context.entityBridge.outbox.consumeScreenShakeRequests();
    }

    public List<EntityCombatEvent> consumeEntityEvents() {
        return session.context.entityBridge.outbox.consumeEntityEvents();
    }

    public int consumePendingMusicFadeOutCountdown() {
        return session.context.entityBridge.outbox.consumePendingMusicFadeOutCountdown();
    }

    public int consumePendingMusicTrack() {
        return session.context.entityBridge.outbox.consumePendingMusicTrack();
    }

    public List<LikeLikeEvent> consumeLikeLikeEvents() {
        return session.context.entityBridge.outbox.consumeLikeLikeEvents();
    }
}
