package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.ENTITY_DROPPABLE_SECRET_SEASHELL;

import java.util.List;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.Warp;

final class RoomEntityCombatBridge {
    private final Dependencies dependencies;

    RoomEntityCombatBridge(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    List<EntityCombatEvent> resolveEntityCombat(RoomRuntime loaded, EntityCombatInputs inputs) {
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        boolean linkAirborne = inputs.linkAirborne();
        int linkVerticalVelocity = inputs.linkVerticalVelocity();
        boolean linkInteractive = inputs.linkInteractive();
        boolean swordCollisionActive = inputs.sword().active();
        int swordX = inputs.sword().x();
        int swordWidth = inputs.sword().width();
        int swordY = inputs.sword().y();
        int swordHeight = inputs.sword().height();
        EnemyAttackContext attackContext = inputs.attackContext();

        List<EntityCombatEvent> events = loaded.entities.resolveCombat(inputs);
        loaded.room.replaceEntities(loaded.entities.inspection().view());
        return events;
    }

    boolean deferContactRecoil(RoomRuntime loaded, int slot, int linkX, int linkY) {
        return loaded.entities.deferContactRecoil(slot, linkX, linkY);
    }

    int clearEntity(RoomRuntime loaded, int slot) {

        int mask = loaded.entities.clearEntity(slot);
        if (mask != 0) {
            dependencies.entityFrame().recentRoomEntityClears.addMask(loaded.room.roomId(), mask);
        }
        loaded.room.replaceEntities(loaded.entities.inspection().view());
        return mask;
    }

    EntityPickupEvent collectEntityIfNeeded(RoomRuntime loaded, EntityPickupInputs inputs) {
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.link().x();
        int linkEntityY = inputs.link().y();
        boolean linkAirborne = inputs.link().airborne();
        boolean linkInteractive = inputs.link().interactive();
        int linkDirection = inputs.link().rawDirection();
        int linkZ = inputs.link().z();
        boolean swordActive = inputs.sword().active();
        int swordX = inputs.sword().x();
        int swordWidth = inputs.sword().width();
        int swordY = inputs.sword().y();
        int swordHeight = inputs.sword().height();

        dependencies.followers().followingLinkZ = linkZ & 0xFF;
        dependencies.followers().followingLinkDirection = linkDirection & 0xFF;
        EntityPickupEvent event = loaded.entities.collectIfNeeded(inputs);
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
            RoomLocationState location,
            RoomEntitySessionState entityBridge,
            RoomEntityRewards roomEntityRewards,
            RoomWorldState roomState) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(entityBridge, "entityBridge");
            java.util.Objects.requireNonNull(roomEntityRewards, "roomEntityRewards");
            java.util.Objects.requireNonNull(roomState, "roomState");
        }
    }
}
