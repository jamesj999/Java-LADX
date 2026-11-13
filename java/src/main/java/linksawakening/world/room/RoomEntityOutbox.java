package linksawakening.world.room;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;

/** Bridges ExecuteEntities requests and room rewards; each consumer owns its drain timing. */
final class RoomEntityOutbox {
    private final RoomRequestDrain<linksawakening.world.entity.event.EntityRequest> rewardDrain =
            new RoomRequestDrain<>();
    final RoomRequestDrain<EntityCombatEvent> combatDrain = new RoomRequestDrain<>();

    /** Empty List.copyOf/addAll implementations can still allocate an array; avoid that boundary. */
    static <T> void append(List<T> target, List<? extends T> source) {
        if (!source.isEmpty()) target.addAll(source);
    }

    /** Copy before clearing: callers retain an immutable owned list. */
    private static <T> List<T> take(List<T> pending) {
        if (pending.isEmpty()) return List.of();
        List<T> result = List.copyOf(pending);
        pending.clear();
        return result;
    }

    final List<BombExplosionEvent> pendingBombExplosionEvents = new ArrayList<>();
    final List<EntityCombatEvent> pendingRoomEntityEvents = new ArrayList<>();
    final List<ChestRewardEvent> pendingChestRewardEvents = new ArrayList<>();
    final List<KeyRewardEvent> pendingKeyRewardEvents = new ArrayList<>();
    final List<SlimeKeyRewardEvent> pendingSlimeKeyRewardEvents = new ArrayList<>();
    final List<HeartContainerRewardEvent> pendingHeartContainerRewards = new ArrayList<>();
    final List<MadamMeowMeowFullHealRequest> pendingMadamMeowMeowFullHealRequests = new ArrayList<>();
    final List<RichardProgressEvent> pendingRichardProgressEvents = new ArrayList<>();
    final List<SwordPickupRewardEvent> pendingSwordPickupRewards = new ArrayList<>();
    final List<ToadstoolRewardEvent> pendingToadstoolRewards = new ArrayList<>();
    final List<WitchExchangeEvent> pendingWitchExchangeEvents = new ArrayList<>();
    final List<WitchRewardEvent> pendingWitchRewardEvents = new ArrayList<>();
    final List<DialogRequest> pendingRoomDialogRequests = new ArrayList<>();
    int pendingRoomMusicTrack = -1;

    List<ChestRewardEvent> consumeChestRewardEvents() {
        return take(pendingChestRewardEvents);
    }

    List<KeyRewardEvent> consumeKeyRewardEvents() {
        return take(pendingKeyRewardEvents);
    }

    List<SlimeKeyRewardEvent> consumeSlimeKeyRewardEvents() {
        return take(pendingSlimeKeyRewardEvents);
    }

    List<HeartContainerRewardEvent> consumeHeartContainerRewards() {
        return take(pendingHeartContainerRewards);
    }

    List<MadamMeowMeowFullHealRequest> consumeMadamMeowMeowFullHealRequests() {
        return take(pendingMadamMeowMeowFullHealRequests);
    }

    List<RichardProgressEvent> consumeRichardProgressEvents() {
        return take(pendingRichardProgressEvents);
    }

    List<SwordPickupRewardEvent> consumeSwordPickupRewards() {
        return take(pendingSwordPickupRewards);
    }

    List<ToadstoolRewardEvent> consumeToadstoolRewards() {
        return take(pendingToadstoolRewards);
    }

    List<WitchExchangeEvent> consumeWitchExchangeEvents() {
        return take(pendingWitchExchangeEvents);
    }

    List<WitchRewardEvent> consumeWitchRewardEvents() {
        return take(pendingWitchRewardEvents);
    }

    List<BombExplosionEvent> consumeBombExplosionEvents() {
        return take(pendingBombExplosionEvents);
    }

    void drainChestRewardEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        rewardDrain.drain(pendingChestRewardEvents, target);
    }

    void drainKeyRewardEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        rewardDrain.drain(pendingKeyRewardEvents, target);
    }

    void drainSlimeKeyRewardEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        rewardDrain.drain(pendingSlimeKeyRewardEvents, target);
    }

    void drainHeartContainerRewards(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        rewardDrain.drain(pendingHeartContainerRewards, target);
    }

    void drainMadamMeowMeowFullHealRequests(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        rewardDrain.drain(pendingMadamMeowMeowFullHealRequests, target);
    }

    void drainRichardProgressEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        rewardDrain.drain(pendingRichardProgressEvents, target);
    }

    void drainSwordPickupRewards(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        rewardDrain.drain(pendingSwordPickupRewards, target);
    }

    void drainToadstoolRewards(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        rewardDrain.drain(pendingToadstoolRewards, target);
    }

    void drainWitchExchangeEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        rewardDrain.drain(pendingWitchExchangeEvents, target);
    }

    void drainWitchRewardEvents(RoomFrameDrain<linksawakening.world.entity.event.EntityRequest> target) {
        rewardDrain.drain(pendingWitchRewardEvents, target);
    }
}
