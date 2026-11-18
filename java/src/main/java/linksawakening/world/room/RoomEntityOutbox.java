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
    private final RoomRequestDrain<EntityCombatEvent> combatDrain = new RoomRequestDrain<>();

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

    private final List<BombExplosionEvent> pendingBombExplosionEvents = new ArrayList<>();
    private final List<EntityCombatEvent> pendingRoomEntityEvents = new ArrayList<>();
    private final List<ChestRewardEvent> pendingChestRewardEvents = new ArrayList<>();
    private final List<KeyRewardEvent> pendingKeyRewardEvents = new ArrayList<>();
    private final List<SlimeKeyRewardEvent> pendingSlimeKeyRewardEvents = new ArrayList<>();
    private final List<HeartContainerRewardEvent> pendingHeartContainerRewards = new ArrayList<>();
    private final List<MadamMeowMeowFullHealRequest> pendingMadamMeowMeowFullHealRequests = new ArrayList<>();
    private final List<RichardProgressEvent> pendingRichardProgressEvents = new ArrayList<>();
    private final List<SwordPickupRewardEvent> pendingSwordPickupRewards = new ArrayList<>();
    private final List<ToadstoolRewardEvent> pendingToadstoolRewards = new ArrayList<>();
    private final List<WitchExchangeEvent> pendingWitchExchangeEvents = new ArrayList<>();
    private final List<WitchRewardEvent> pendingWitchRewardEvents = new ArrayList<>();
    private final List<DialogRequest> pendingRoomDialogRequests = new ArrayList<>();
    private int pendingRoomMusicTrack = -1;

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

    List<DialogRequest> pendingRoomDialogRequests() {
        return this.pendingRoomDialogRequests;
    }

    List<EntityCombatEvent> pendingRoomEntityEvents() {
        return this.pendingRoomEntityEvents;
    }

    List<KeyRewardEvent> pendingKeyRewardEvents() {
        return this.pendingKeyRewardEvents;
    }

    List<BombExplosionEvent> pendingBombExplosionEvents() {
        return this.pendingBombExplosionEvents;
    }

    List<ChestRewardEvent> pendingChestRewardEvents() {
        return this.pendingChestRewardEvents;
    }

    List<SlimeKeyRewardEvent> pendingSlimeKeyRewardEvents() {
        return this.pendingSlimeKeyRewardEvents;
    }

    List<HeartContainerRewardEvent> pendingHeartContainerRewards() {
        return this.pendingHeartContainerRewards;
    }

    List<MadamMeowMeowFullHealRequest> pendingMadamMeowMeowFullHealRequests() {
        return this.pendingMadamMeowMeowFullHealRequests;
    }

    List<RichardProgressEvent> pendingRichardProgressEvents() {
        return this.pendingRichardProgressEvents;
    }

    List<SwordPickupRewardEvent> pendingSwordPickupRewards() {
        return this.pendingSwordPickupRewards;
    }

    List<ToadstoolRewardEvent> pendingToadstoolRewards() {
        return this.pendingToadstoolRewards;
    }

    List<WitchExchangeEvent> pendingWitchExchangeEvents() {
        return this.pendingWitchExchangeEvents;
    }

    List<WitchRewardEvent> pendingWitchRewardEvents() {
        return this.pendingWitchRewardEvents;
    }

    int pendingRoomMusicTrack() {
        return this.pendingRoomMusicTrack;
    }

    int pendingRoomMusicTrack(int value) {
        this.pendingRoomMusicTrack = value;
        return this.pendingRoomMusicTrack;
    }

    RoomRequestDrain<EntityCombatEvent> combatDrain() {
        return this.combatDrain;
    }
}
