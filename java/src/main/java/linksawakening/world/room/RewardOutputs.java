package linksawakening.world.room;

import java.util.List;
import java.util.Objects;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HeartPiecePickupUpdate;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;

public final class RewardOutputs {
    private final Dependencies dependencies;
    private final RoomEntityOutputSource source;

    RewardOutputs(Dependencies dependencies, RoomEntityOutputSource source) {
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies");
        this.source = Objects.requireNonNull(source);
    }

    public List<ChestRewardEvent> consumeChestRewardEvents() {
        return dependencies.entityRuntimeState().outbox.consumeChestRewardEvents();
    }

    public List<KeyRewardEvent> consumeKeyRewardEvents() {
        return dependencies.entityRuntimeState().outbox.consumeKeyRewardEvents();
    }

    public List<SlimeKeyRewardEvent> consumeSlimeKeyRewardEvents() {
        return dependencies.entityRuntimeState().outbox.consumeSlimeKeyRewardEvents();
    }

    public List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        return source.consumeHeartPieceUpdates();
    }

    public List<HeartContainerRewardEvent> consumeHeartContainerRewards() {
        return dependencies.entityRuntimeState().outbox.consumeHeartContainerRewards();
    }

    public List<MadamMeowMeowFullHealRequest> consumeMadamMeowMeowFullHealRequests() {
        return dependencies.entityRuntimeState().outbox.consumeMadamMeowMeowFullHealRequests();
    }

    public List<RichardProgressEvent> consumeRichardProgressEvents() {
        return dependencies.entityRuntimeState().outbox.consumeRichardProgressEvents();
    }

    public List<ToadstoolRewardEvent> consumeToadstoolRewards() {
        return dependencies.entityRuntimeState().outbox.consumeToadstoolRewards();
    }

    public List<WitchExchangeEvent> consumeWitchExchangeEvents() {
        return dependencies.entityRuntimeState().outbox.consumeWitchExchangeEvents();
    }

    public List<WitchRewardEvent> consumeWitchRewardEvents() {
        return dependencies.entityRuntimeState().outbox.consumeWitchRewardEvents();
    }

    public void drainChestRewardEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityRuntimeState().outbox.drainChestRewardEvents(target);
    }

    public void drainKeyRewardEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityRuntimeState().outbox.drainKeyRewardEvents(target);
    }

    public void drainSlimeKeyRewardEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityRuntimeState().outbox.drainSlimeKeyRewardEvents(target);
    }

    public void drainHeartContainerRewards(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityRuntimeState().outbox.drainHeartContainerRewards(target);
    }

    public void drainMadamMeowMeowFullHealRequests(
            RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityRuntimeState().outbox.drainMadamMeowMeowFullHealRequests(target);
    }

    public void drainRichardProgressEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityRuntimeState().outbox.drainRichardProgressEvents(target);
    }

    public void drainSwordPickupRewards(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityRuntimeState().outbox.drainSwordPickupRewards(target);
    }

    public void drainToadstoolRewards(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityRuntimeState().outbox.drainToadstoolRewards(target);
    }

    public void drainWitchExchangeEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityRuntimeState().outbox.drainWitchExchangeEvents(target);
    }

    public void drainWitchRewardEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityRuntimeState().outbox.drainWitchRewardEvents(target);
    }

    record Dependencies(RoomEntitySessionState entityRuntimeState) {
        Dependencies {
            Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
        }
    }
}
