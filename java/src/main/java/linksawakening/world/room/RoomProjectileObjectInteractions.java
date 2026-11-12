package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.physics.OverworldCollision;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.transition.Warp;

final class RoomProjectileObjectInteractions {
    private final Dependencies dependencies;

    RoomProjectileObjectInteractions(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void applyBoomerangObjectInteractions(List<BoomerangObjectRequest> requests) {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                || requests == null
                || requests.isEmpty()) {
            return;
        }

        for (int requestIndex = 0; requestIndex < requests.size(); requestIndex++) {
            BoomerangObjectRequest request = requests.get(requestIndex);

            OverworldBushInteraction.CutResult result = dependencies
                    .overworldBushInteraction()
                    .revealObjectAtLocation(
                            request.location(),
                            dependencies.location().activeRoom.roomId(),
                            true,
                            dependencies.location().activeRoom.roomObjectsArea(),
                            dependencies.location().activeRoom.renderValues(),
                            dependencies.location().activeRoom.gbcOverlay(),
                            dependencies.location().activeRoom.tileIds(),
                            dependencies.location().activeRoom.tileAttrs());
            if (!result.changed()) {
                continue;
            }
            if (dependencies.transientVfxSystem() != null) {
                dependencies
                        .transientVfxSystem()
                        .spawn(TransientVfxType.SMOKE, request.objectLeft() + 0x08, request.objectTop() + 0x10);
            }
            dependencies
                    .entityBridge()
                    .get()
                    .outbox
                    .pendingRoomEntityEvents
                    .add(new EntityCombatEvent(
                            request.sourceSlot(), 0x01, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
            dependencies.roomObjectMutations().get().refreshOverworldCollisionAfterObjectMutation();
        }
    }

    void applyMagicRodObjectInteractions(List<MagicRodObjectRequest> requests) {
        if (dependencies.location().activeRoom == null || requests == null || requests.isEmpty()) {
            return;
        }

        for (int requestIndex = 0; requestIndex < requests.size(); requestIndex++) {
            MagicRodObjectRequest request = requests.get(requestIndex);

            boolean changed = false;
            if (dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                OverworldBushInteraction.CutResult result = dependencies
                        .overworldBushInteraction()
                        .revealObjectAtLocation(
                                request.location(),
                                dependencies.location().activeRoom.roomId(),
                                true,
                                dependencies.location().activeRoom.roomObjectsArea(),
                                dependencies.location().activeRoom.renderValues(),
                                dependencies.location().activeRoom.gbcOverlay(),
                                dependencies.location().activeRoom.tileIds(),
                                dependencies.location().activeRoom.tileAttrs());
                changed = result.changed();
            } else if (dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
                    && dependencies.roomSessionViews().get().objectAtRoomLocation(request.location()) == 0x8A) {
                int areaIndex =
                        RoomConstants.ROOM_OBJECTS_BASE + (request.location() & 0xF0) + (request.location() & 0x0F);
                int[] objects = dependencies.location().activeRoom.roomObjectsArea();
                if (areaIndex >= 0 && areaIndex < objects.length) {
                    objects[areaIndex] = 0x04; // OBJECT_SIDE_VIEW_EMPTY
                    if (dependencies.location().activeRoom.renderValues() != null
                            && areaIndex < dependencies.location().activeRoom.renderValues().length) {
                        dependencies.location().activeRoom.renderValues()[areaIndex] = 0x04;
                    }
                    dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
                    dependencies
                            .overworldCollision()
                            .setRoom(dependencies.location().activeRoom.roomObjectsArea());
                    dependencies.overworldCollision().setGbcOverlay(null);
                    changed = true;
                }
            }

            if (!changed) {
                continue;
            }
            if (dependencies.transientVfxSystem() != null) {
                dependencies
                        .transientVfxSystem()
                        .spawn(TransientVfxType.SMOKE, request.objectLeft() + 0x08, request.objectTop() + 0x10);
            }
            dependencies
                    .entityBridge()
                    .get()
                    .outbox
                    .pendingRoomEntityEvents
                    .add(new EntityCombatEvent(
                            request.sourceSlot(), 0x04, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
            if (dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                dependencies.roomObjectMutations().get().refreshOverworldCollisionAfterObjectMutation();
            }
        }
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomLocationState location,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw,
            TransientVfxSystem transientVfxSystem) {}
}
