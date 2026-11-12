package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

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
    private final RoomSession session;

    RewardOutputs(RoomSession session) {
        this.session = session;
    }

    public List<ChestRewardEvent> consumeChestRewardEvents() {
        return session.context.entityBridge.outbox.consumeChestRewardEvents();
    }

    public List<KeyRewardEvent> consumeKeyRewardEvents() {
        return session.context.entityBridge.outbox.consumeKeyRewardEvents();
    }

    public List<SlimeKeyRewardEvent> consumeSlimeKeyRewardEvents() {
        return session.context.entityBridge.outbox.consumeSlimeKeyRewardEvents();
    }

    public List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        return session.context.roomSessionViews.consumeHeartPieceUpdates();
    }

    public List<HeartContainerRewardEvent> consumeHeartContainerRewards() {
        return session.context.entityBridge.outbox.consumeHeartContainerRewards();
    }

    public List<MadamMeowMeowFullHealRequest> consumeMadamMeowMeowFullHealRequests() {
        return session.context.entityBridge.outbox.consumeMadamMeowMeowFullHealRequests();
    }

    public List<RichardProgressEvent> consumeRichardProgressEvents() {
        return session.context.entityBridge.outbox.consumeRichardProgressEvents();
    }

    public List<ToadstoolRewardEvent> consumeToadstoolRewards() {
        return session.context.entityBridge.outbox.consumeToadstoolRewards();
    }

    public List<WitchExchangeEvent> consumeWitchExchangeEvents() {
        return session.context.entityBridge.outbox.consumeWitchExchangeEvents();
    }

    public List<WitchRewardEvent> consumeWitchRewardEvents() {
        return session.context.entityBridge.outbox.consumeWitchRewardEvents();
    }
}
