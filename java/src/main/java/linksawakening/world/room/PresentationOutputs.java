package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.List;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.ScreenShakeRequest;

public final class PresentationOutputs {
    private final Dependencies dependencies;

    PresentationOutputs(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    public List<ScreenShakeRequest> consumeScreenShakeRequests() {
        return dependencies.entityBridge().get().outbox.consumeScreenShakeRequests();
    }

    public List<EntityCombatEvent> consumeEntityEvents() {
        return dependencies.entityBridge().get().outbox.consumeEntityEvents();
    }

    public int consumePendingMusicFadeOutCountdown() {
        return dependencies.entityBridge().get().outbox.consumePendingMusicFadeOutCountdown();
    }

    public int consumePendingMusicTrack() {
        return dependencies.entityBridge().get().outbox.consumePendingMusicTrack();
    }

    public List<LikeLikeEvent> consumeLikeLikeEvents() {
        return dependencies.entityBridge().get().outbox.consumeLikeLikeEvents();
    }

    record Dependencies(java.util.function.Supplier<RoomEntityBridge> entityBridge) {}
}
