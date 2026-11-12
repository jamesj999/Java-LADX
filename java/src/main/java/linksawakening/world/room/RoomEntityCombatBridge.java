package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

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

    List<EntityCombatEvent> resolveEntityCombat(EntityCombatInputs inputs) {
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

        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null) {
            return List.of();
        }
        List<EntityCombatEvent> events =
                dependencies.entityBridge().get().entityRuntime.resolveCombat(inputs);
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .view());
        return events;
    }

    boolean deferContactRecoil(int slot, int linkX, int linkY) {
        return dependencies.entityBridge().get().entityRuntime != null
                && dependencies.entityBridge().get().entityRuntime.deferContactRecoil(slot, linkX, linkY);
    }

    int clearEntity(int slot) {
        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null) {
            return 0;
        }
        int mask = dependencies.entityBridge().get().entityRuntime.clearEntity(slot);
        if (mask != 0) {
            dependencies
                    .entityFrame()
                    .recentRoomEntityClears
                    .addMask(dependencies.location().activeRoom.roomId(), mask);
        }
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .view());
        return mask;
    }

    EntityPickupEvent collectEntityIfNeeded(
            int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) {
        return dependencies
                .entityBridge()
                .get()
                .combat
                .collectEntityIfNeeded(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkAirborne,
                        linkInteractive,
                        dependencies.followers().followingLinkDirection,
                        dependencies.followers().followingLinkZ);
    }

    EntityPickupEvent collectEntityIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection) {
        return dependencies
                .entityBridge()
                .get()
                .combat
                .collectEntityIfNeeded(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkAirborne,
                        linkInteractive,
                        linkDirection,
                        dependencies.followers().followingLinkZ);
    }

    EntityPickupEvent collectEntityIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection,
            int linkZ) {
        return collectEntityIfNeeded(new EntityPickupInputs(
                frameCounter,
                new EntityPickupInputs.Link(
                        linkEntityX,
                        linkEntityY,
                        linkAirborne,
                        linkInteractive,
                        linksawakening.state.Direction.fromJavaOrUnknown(linkDirection),
                        linkZ,
                        linkDirection),
                new SwordBox(false, 0, 0, 0, 0)));
    }

    EntityPickupEvent collectEntityIfNeeded(EntityPickupInputs inputs) {
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

        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null) {
            return null;
        }
        dependencies.followers().followingLinkZ = linkZ & 0xFF;
        dependencies.followers().followingLinkDirection = linkDirection & 0xFF;
        EntityPickupEvent event =
                dependencies.entityBridge().get().entityRuntime.collectIfNeeded(inputs);
        if (event == null) {
            return null;
        }
        if (event.persistentClearMask() != 0) {
            dependencies
                    .entityFrame()
                    .recentRoomEntityClears
                    .addMask(dependencies.location().activeRoom.roomId(), event.persistentClearMask());
        }
        if (event.type() == 0x30
                && dependencies.location().activeRoom.roomId() == 0x7C
                && dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            // PickDroppableKey sets the Angler's Tunnel source room flag before
            // it starts the held-item transition.
            dependencies.roomState().indoorARoomStatus[0x69] |= 0x10;
        }
        if (event.type() == ENTITY_DROPPABLE_SECRET_SEASHELL) {
            // PickSecretSeashell opens Dialog0EF and completes the room after
            // the shared pickup collision has cleared the source entity.
            dependencies.roomStoryState().get().markActiveRoomCompleted();
            dependencies.entityBridge().get().outbox.pendingRoomDialogRequests.add(new DialogRequest(0, 0xEF));
            dependencies
                    .entityBridge()
                    .get()
                    .outbox
                    .pendingRoomEntityEvents
                    .add(new EntityCombatEvent(
                            event.slot(), event.type(), 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x01));
        }
        dependencies.roomEntityRewards().get().harvestKeyRewardEvents();
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .view());
        return event;
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomFollowerState followers,
            RoomLocationState location,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            java.util.function.Supplier<RoomEntityRewards> roomEntityRewards,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState) {}
}
