package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.ENTITY_DROPPABLE_SECRET_SEASHELL;

import java.util.List;
import linksawakening.world.entity.EnemyCombatInputs;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.entity.EntityPickupFrameInputs;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.Warp;

final class RoomEntityCombatBridge {
    private final Dependencies dependencies;

    RoomEntityCombatBridge(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    List<EntityCombatEvent> resolveEntityCombat(RoomRuntime loaded, EnemyCombatInputs inputs) {

        List<EntityCombatEvent> events = loaded.entities.combat().resolveCombat(inputs);
        loaded.room.replaceEntities(loaded.entities.inspection().view());
        return events;
    }

    boolean deferContactRecoil(RoomRuntime loaded, int slot, int linkX, int linkY) {
        return loaded.entities.combat().deferContactRecoil(slot, linkX, linkY);
    }

    int clearEntity(RoomRuntime loaded, int slot) {

        int mask = loaded.entities.lifecycle().clearEntity(slot);
        if (mask != 0) {
            dependencies.entityFrame().recentRoomEntityClears.addMask(loaded.room.roomId(), mask);
        }
        loaded.room.replaceEntities(loaded.entities.inspection().view());
        return mask;
    }

    EntityPickupEvent collectEntityIfNeeded(RoomRuntime loaded, EntityPickupFrameInputs inputs) {

        int linkDirection = inputs.linkDirection();
        int linkZ = inputs.linkZ();

        dependencies.followers().followingLinkZ = linkZ & 0xFF;
        dependencies.followers().followingLinkDirection = linkDirection & 0xFF;
        EntityPickupEvent event = loaded.entities.pickups().collectIfNeeded(inputs);
        if (event == null) {
            return null;
        }
        if (event.persistentClearMask() != 0) {
            dependencies
                    .entityFrame()
                    .recentRoomEntityClears
                    .addMask(loaded.room.roomId(), event.persistentClearMask());
        }
        if (event.type() == 0x30
                && loaded.room.roomId() == 0x7C
                && loaded.room.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            // PickDroppableKey sets the Angler's Tunnel source room flag before
            // it starts the held-item transition.
            dependencies.roomState().indoorARoomStatus[0x69] |= 0x10;
        }
        if (event.type() == ENTITY_DROPPABLE_SECRET_SEASHELL) {
            // PickSecretSeashell opens Dialog0EF and completes the room after
            // the shared pickup collision has cleared the source entity.
            dependencies.roomState().markCompleted(loaded.room);
            dependencies.entityBridge().outbox.pendingRoomDialogRequests.add(new DialogRequest(0, 0xEF));
            dependencies
                    .entityBridge()
                    .outbox
                    .pendingRoomEntityEvents
                    .add(new EntityCombatEvent(
                            event.slot(), event.type(), 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x01));
        }
        dependencies.roomEntityRewards().harvestKeyRewardEvents(loaded);
        loaded.room.replaceEntities(loaded.entities.inspection().view());
        return event;
    }

    record Dependencies(
            RoomEntitySessionState entityFrame,
            RoomFollowerState followers,
            RoomEntitySessionState entityBridge,
            RoomEntityRewards roomEntityRewards,
            RoomWorldState roomState) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(entityBridge, "entityBridge");
            java.util.Objects.requireNonNull(roomEntityRewards, "roomEntityRewards");
            java.util.Objects.requireNonNull(roomState, "roomState");
        }
    }
}
