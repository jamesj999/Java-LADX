package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.object.BombObjectInteraction;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.transition.Warp;

final class RoomBombObjectInteractions {
    private final Dependencies dependencies;

    RoomBombObjectInteractions(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void applyBombObjectInteractions(List<BombExplosionEvent> events) {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
                || events == null
                || events.isEmpty()) {
            return;
        }

        for (BombExplosionEvent event : events) {
            if (!event.targetsRoomObjects()) {
                continue;
            }
            RoomEntity bomb = event.bombSlot()
                            < dependencies
                                    .location()
                                    .activeRoom
                                    .entities()
                                    .slots()
                                    .size()
                    ? dependencies.location().activeRoom.entities().slots().get(event.bombSlot())
                    : null;
            if (dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                BombObjectInteraction.Candidate candidate =
                        BombObjectInteraction.basicCandidate(event.bombX(), event.bombVisualY(), event.countdown());
                dependencies.roomObjectMutations().get().applyBasicBombObjectCandidate(candidate);
                if (bomb != null && bomb.loaded()) {
                    dependencies
                            .roomObjectMutations()
                            .get()
                            .applyPuzzleBombObjectCandidate(
                                    BombObjectInteraction.puzzleCandidate(event.bombX(), bomb.y(), event.countdown()),
                                    event.bombSlot(),
                                    bomb.z());
                }
            } else if (bomb != null && bomb.loaded()) {
                BombObjectInteraction.Candidate candidate =
                        BombObjectInteraction.puzzleCandidate(event.bombX(), bomb.y(), event.countdown());
                if (!dependencies.roomObjectMutations().get().applyIndoorBombableBlockCandidate(candidate)) {
                    dependencies
                            .roomObjectMutations()
                            .get()
                            .applyIndoorBombableWallCandidate(candidate, event.bombSlot());
                }
            }
        }
    }

    void applyBasicBombObjectCandidate(BombObjectInteraction.Candidate candidate) {
        if (candidate == null) {
            return;
        }
        int location = candidate.location();
        OverworldBushInteraction.CutResult result = dependencies
                .overworldBushInteraction()
                .revealObjectAtLocation(
                        location,
                        dependencies.location().activeRoom.roomId(),
                        true,
                        dependencies.location().activeRoom.roomObjectsArea(),
                        dependencies.location().activeRoom.renderValues(),
                        dependencies.location().activeRoom.gbcOverlay(),
                        dependencies.location().activeRoom.tileIds(),
                        dependencies.location().activeRoom.tileAttrs());
        if (!result.changed()) {
            return;
        }

        if (result.bushLeavesVisible() && dependencies.entityBridge().get().entityRuntime != null) {
            int sourceSpriteVariant = result.originalObjectId() == 0x0A ? 0xFF : 0x01;
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .spawnLiftableRockSmash(
                            dependencies.overworldBushInteraction().effectOriginXForLocation(location),
                            dependencies.overworldBushInteraction().effectOriginYForLocation(location),
                            sourceSpriteVariant);
        }
        dependencies.roomObjectMutations().get().refreshOverworldCollisionAfterObjectMutation();
    }

    void applyPuzzleBombObjectCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot, int sourceZ) {
        if (candidate == null) {
            return;
        }
        int objectId = dependencies.roomSessionViews().get().objectAtRoomLocation(candidate.location());
        if (objectId < OBJECT_GIANT_SKULL_TOP_LEFT || objectId > OBJECT_GIANT_SKULL_BOTTOM_RIGHT) {
            dependencies.roomObjectMutations().get().applyOutdoorBombableCaveDoorCandidate(candidate, sourceSlot);
            return;
        }

        int blockTop = candidate.objectTop() & 0xE0;
        int blockLeft = candidate.objectLeft() & 0xE0;
        int[] rubbleXOffsets = {0x18, 0x08, 0x18, 0x08};
        int[] rubbleYOffsets = {0x20, 0x20, 0x10, 0x10};
        if (dependencies.entityBridge().get().entityRuntime != null) {
            for (int index = 0; index < rubbleXOffsets.length; index++) {
                dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .spawnLiftableRockRubble(
                                blockLeft + rubbleXOffsets[index], blockTop + rubbleYOffsets[index] - sourceZ);
            }
        }
        dependencies.roomObjectMutations().get().queuePuzzleSolvedJingle(sourceSlot);

        int blockLocation = blockTop | (blockLeft >>> 4);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                dependencies
                        .roomObjectMutations()
                        .get()
                        .writeBombPuzzleObject(
                                blockLocation + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column,
                                OBJECT_ROCKY_GROUND);
            }
        }
        dependencies.roomState()
                .overworldRoomStatus[dependencies.location().activeRoom.roomId()] |= (byte) OW_ROOM_STATUS_OPENED;
        dependencies.roomObjectMutations().get().refreshOverworldCollisionAfterObjectMutation();
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomLocationState location,
            OverworldBushInteraction overworldBushInteraction,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState) {}
}
