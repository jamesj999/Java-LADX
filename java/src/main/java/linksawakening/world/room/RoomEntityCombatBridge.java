package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.List;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.Warp;

final class RoomEntityCombatBridge {
    private final RoomSession session;

    RoomEntityCombatBridge(RoomSession session) {
        this.session = session;
    }

    List<EntityCombatEvent> resolveEntityCombat(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight) {
        return session.resolveEntityCombat(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                0,
                linkInteractive,
                swordCollisionActive,
                swordX,
                swordWidth,
                swordY,
                swordHeight,
                EnemyAttackContext.standard());
    }

    List<EntityCombatEvent> resolveEntityCombat(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight,
            EnemyAttackContext attackContext) {
        return session.resolveEntityCombat(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                0,
                linkInteractive,
                swordCollisionActive,
                swordX,
                swordWidth,
                swordY,
                swordHeight,
                attackContext);
    }

    List<EntityCombatEvent> resolveEntityCombat(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            int linkVerticalVelocity,
            boolean linkInteractive,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight,
            EnemyAttackContext attackContext) {
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return List.of();
        }
        List<EntityCombatEvent> events = session.context.entityBridge.entityRuntime.resolveCombat(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                linkVerticalVelocity,
                linkInteractive,
                swordCollisionActive,
                swordX,
                swordWidth,
                swordY,
                swordHeight,
                attackContext);
        session.context.activeRoom.replaceEntities(
                session.context.entityBridge.entityRuntime.inspection().snapshot());
        return events;
    }

    boolean deferContactRecoil(int slot, int linkX, int linkY) {
        return session.context.entityBridge.entityRuntime != null
                && session.context.entityBridge.entityRuntime.deferContactRecoil(slot, linkX, linkY);
    }

    int clearEntity(int slot) {
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return 0;
        }
        int mask = session.context.entityBridge.entityRuntime.clearEntity(slot);
        if (mask != 0) {
            session.context.recentRoomEntityClears.addMask(session.context.activeRoom.roomId(), mask);
        }
        session.context.activeRoom.replaceEntities(
                session.context.entityBridge.entityRuntime.inspection().snapshot());
        return mask;
    }

    EntityPickupEvent collectEntityIfNeeded(
            int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) {
        return session.collectEntityIfNeeded(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                linkInteractive,
                session.context.followingLinkDirection,
                session.context.followingLinkZ);
    }

    EntityPickupEvent collectEntityIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection) {
        return session.collectEntityIfNeeded(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                linkInteractive,
                linkDirection,
                session.context.followingLinkZ);
    }

    EntityPickupEvent collectEntityIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection,
            int linkZ) {
        return collectEntityIfNeeded(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                linkInteractive,
                linkDirection,
                linkZ,
                false,
                0,
                0,
                0,
                0);
    }

    EntityPickupEvent collectEntityIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection,
            int linkZ,
            boolean swordActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight) {
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return null;
        }
        session.context.followingLinkZ = linkZ & 0xFF;
        session.context.followingLinkDirection = linkDirection & 0xFF;
        EntityPickupEvent event = session.context.entityBridge.entityRuntime.collectIfNeeded(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                linkInteractive,
                linkDirection,
                linkZ,
                swordActive,
                swordX,
                swordWidth,
                swordY,
                swordHeight);
        if (event == null) {
            return null;
        }
        if (event.persistentClearMask() != 0) {
            session.context.recentRoomEntityClears.addMask(
                    session.context.activeRoom.roomId(), event.persistentClearMask());
        }
        if (event.type() == 0x30
                && session.context.activeRoom.roomId() == 0x7C
                && session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            // PickDroppableKey sets the Angler's Tunnel source room flag before
            // it starts the held-item transition.
            session.context.roomState.indoorARoomStatus[0x69] |= 0x10;
        }
        if (event.type() == session.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            // PickSecretSeashell opens Dialog0EF and completes the room after
            // the shared pickup collision has cleared the source entity.
            session.markActiveRoomCompleted();
            session.context.entityBridge.outbox.pendingRoomDialogRequests.add(new DialogRequest(0, 0xEF));
            session.context.entityBridge.outbox.pendingRoomEntityEvents.add(new EntityCombatEvent(
                    event.slot(), event.type(), 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x01));
        }
        session.harvestKeyRewardEvents();
        session.context.activeRoom.replaceEntities(
                session.context.entityBridge.entityRuntime.inspection().snapshot());
        return event;
    }
}
