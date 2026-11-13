package linksawakening.world.room;

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
        return dependencies.entityRuntimeState().outbox.consumeScreenShakeRequests();
    }

    public List<EntityCombatEvent> consumeEntityEvents() {
        return dependencies.entityRuntimeState().outbox.consumeEntityEvents();
    }

    public int consumePendingMusicFadeOutCountdown() {
        return dependencies.entityRuntimeState().outbox.consumePendingMusicFadeOutCountdown();
    }

    public int consumePendingMusicTrack() {
        return dependencies.entityRuntimeState().outbox.consumePendingMusicTrack();
    }

    public List<LikeLikeEvent> consumeLikeLikeEvents() {
        return dependencies.entityRuntimeState().outbox.consumeLikeLikeEvents();
    }

    public void drainEntityEvents(RoomFrameDrain<EntityCombatEvent> target) {
        dependencies.entityRuntimeState().outbox.drainEntityEvents(target);
    }

    public linksawakening.world.entity.event.DialogRequest consumeFirstEntityDialogRequest() {
        return dependencies.entityRuntimeState().outbox.consumeFirstEntityDialogRequest();
    }

    record Dependencies(RoomEntitySessionState entityRuntimeState) {
        Dependencies {
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
        }
    }
}
