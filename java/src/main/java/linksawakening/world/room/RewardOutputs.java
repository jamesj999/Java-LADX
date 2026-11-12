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

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews) {}
}
