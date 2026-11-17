package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.ENTITY_DROPPABLE_SECRET_SEASHELL;

import java.util.List;
import java.util.Objects;
import linksawakening.world.entity.EnemyCombatInputs;
import linksawakening.world.entity.EntityCombatResolver;
import linksawakening.world.entity.EntityEffectConsumption;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.entity.EntityPickupFrameInputs;
import linksawakening.world.entity.EntityRewardController;
import linksawakening.world.entity.EntityRoomInspection;
import linksawakening.world.entity.EntitySlotLifecycle;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.transition.Warp;

/** Loaded-room combat and pickup policy: persistence, room rewards and refreshed entity views. */
final class RoomEntityInteractions {
    private final ActiveRoom room;
    private final EntityCombatResolver combat;
    private final EntityRewardController pickups;
    private final EntitySlotLifecycle lifecycle;
    private final EntityRoomInspection inspection;
    private final EntityEffectConsumption effects;
    private final RoomEntitySessionState session;
    private final RoomFollowerState followers;
    private final RoomWorldState world;

    RoomEntityInteractions(
            ActiveRoom room,
            EntityCombatResolver combat,
            EntityRewardController pickups,
            EntitySlotLifecycle lifecycle,
            EntityRoomInspection inspection,
            EntityEffectConsumption effects,
            RoomEntitySessionState session,
            RoomFollowerState followers,
            RoomWorldState world) {
        this.room = Objects.requireNonNull(room, "room");
        this.combat = Objects.requireNonNull(combat, "combat");
        this.pickups = Objects.requireNonNull(pickups, "pickups");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.inspection = Objects.requireNonNull(inspection, "inspection");
        this.effects = Objects.requireNonNull(effects, "effects");
        this.session = Objects.requireNonNull(session, "session");
        this.followers = Objects.requireNonNull(followers, "followers");
        this.world = Objects.requireNonNull(world, "world");
    }

    List<EntityCombatEvent> resolveEntityCombat(EnemyCombatInputs inputs) {
        List<EntityCombatEvent> events = combat.resolveCombat(inputs);
        room.replaceEntities(inspection.view());
        return events;
    }

    int clearEntity(int slot) {
        int mask = lifecycle.clearEntity(slot);
        if (mask != 0) {
            session.recentRoomEntityClears().addMask(room.roomId(), mask);
        }
        room.replaceEntities(inspection.view());
        return mask;
    }

    EntityPickupEvent collectEntityIfNeeded(EntityPickupFrameInputs inputs) {
        int linkDirection = inputs.linkDirection();
        int linkZ = inputs.linkZ();

        followers.observePickup(linkZ, linkDirection);
        EntityPickupEvent event = pickups.collectIfNeeded(inputs);
        if (event == null) {
            return null;
        }
        if (event.persistentClearMask() != 0) {
            session.recentRoomEntityClears().addMask(room.roomId(), event.persistentClearMask());
        }
        if (event.type() == 0x30 && room.roomId() == 0x7C && room.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            // PickDroppableKey sets the Angler's Tunnel source room flag before
            // it starts the held-item transition.
            world.indoorARoomStatus()[0x69] |= 0x10;
        }
        if (event.type() == ENTITY_DROPPABLE_SECRET_SEASHELL) {
            // PickSecretSeashell opens Dialog0EF and completes the room after
            // the shared pickup collision has cleared the source entity.
            world.markCompleted(room);
            session.outbox().pendingRoomDialogRequests().add(new DialogRequest(0, 0xEF));
            session.outbox()
                    .pendingRoomEntityEvents()
                    .add(new EntityCombatEvent(
                            event.slot(), event.type(), 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x01));
        }
        harvestKeyRewardEvents();
        room.replaceEntities(inspection.view());
        return event;
    }

    void harvestKeyRewardEvents() {
        List<KeyRewardEvent> rewards = effects.consumePendingKeyRewardEvents();
        for (int rewardIndex = 0; rewardIndex < rewards.size(); rewardIndex++) {
            KeyRewardEvent reward = rewards.get(rewardIndex);

            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                    && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                world.dungeonItemState().incrementCurrentFlag(reward.itemType());
            }
            world.markCompleted(room);
        }
        RoomEntityOutbox.append(session.outbox().pendingKeyRewardEvents(), rewards);
    }
}
