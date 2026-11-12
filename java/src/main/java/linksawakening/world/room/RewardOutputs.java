package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
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

    RewardOutputs(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    public List<ChestRewardEvent> consumeChestRewardEvents() {
        return dependencies.entityBridge().get().outbox.consumeChestRewardEvents();
    }

    public List<KeyRewardEvent> consumeKeyRewardEvents() {
        return dependencies.entityBridge().get().outbox.consumeKeyRewardEvents();
    }

    public List<SlimeKeyRewardEvent> consumeSlimeKeyRewardEvents() {
        return dependencies.entityBridge().get().outbox.consumeSlimeKeyRewardEvents();
    }

    public List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        return dependencies.roomSessionViews().get().consumeHeartPieceUpdates();
    }

    public List<HeartContainerRewardEvent> consumeHeartContainerRewards() {
        return dependencies.entityBridge().get().outbox.consumeHeartContainerRewards();
    }

    public List<MadamMeowMeowFullHealRequest> consumeMadamMeowMeowFullHealRequests() {
        return dependencies.entityBridge().get().outbox.consumeMadamMeowMeowFullHealRequests();
    }

    public List<RichardProgressEvent> consumeRichardProgressEvents() {
        return dependencies.entityBridge().get().outbox.consumeRichardProgressEvents();
    }

    public List<ToadstoolRewardEvent> consumeToadstoolRewards() {
        return dependencies.entityBridge().get().outbox.consumeToadstoolRewards();
    }

    public List<WitchExchangeEvent> consumeWitchExchangeEvents() {
        return dependencies.entityBridge().get().outbox.consumeWitchExchangeEvents();
    }

    public List<WitchRewardEvent> consumeWitchRewardEvents() {
        return dependencies.entityBridge().get().outbox.consumeWitchRewardEvents();
    }

    public void drainChestRewardEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityBridge().get().outbox.drainChestRewardEvents(target);
    }

    public void drainKeyRewardEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityBridge().get().outbox.drainKeyRewardEvents(target);
    }

    public void drainSlimeKeyRewardEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityBridge().get().outbox.drainSlimeKeyRewardEvents(target);
    }

    public void drainHeartContainerRewards(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityBridge().get().outbox.drainHeartContainerRewards(target);
    }

    public void drainMadamMeowMeowFullHealRequests(
            RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityBridge().get().outbox.drainMadamMeowMeowFullHealRequests(target);
    }

    public void drainRichardProgressEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityBridge().get().outbox.drainRichardProgressEvents(target);
    }

    public void drainSwordPickupRewards(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityBridge().get().outbox.drainSwordPickupRewards(target);
    }

    public void drainToadstoolRewards(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityBridge().get().outbox.drainToadstoolRewards(target);
    }

    public void drainWitchExchangeEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityBridge().get().outbox.drainWitchExchangeEvents(target);
    }

    public void drainWitchRewardEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        dependencies.entityBridge().get().outbox.drainWitchRewardEvents(target);
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews) {}
}
