package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import java.util.Map;
import linksawakening.physics.OverworldCollision;
import linksawakening.world.entity.event.MagicPowderObjectAction;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.transition.Warp;

final class RoomTorchMutations {
    private final Dependencies dependencies;

    RoomTorchMutations(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void applyMagicPowderObjectInteractions(List<MagicPowderObjectRequest> requests) {
        if (dependencies.location().activeRoom == null || requests == null || requests.isEmpty()) {
            return;
        }

        for (MagicPowderObjectRequest request : requests) {
            boolean changed = applyPowderRequest(request);

            if (!changed) {
                continue;
            }
            if (request.action() == MagicPowderObjectAction.IGNITE_TORCH) {
                dependencies.puzzles().roomTriggerCount = (dependencies.puzzles().roomTriggerCount + 1) & 0xFF;
                dependencies.indoorTorchPaletteEffect().igniteTorch();
            } else if (request.action() == MagicPowderObjectAction.EXTINGUISH_TORCH
                    && dependencies.location().activeRoom.roomId() != 0x74) {
                dependencies.puzzles().roomTriggerCount = (dependencies.puzzles().roomTriggerCount - 1) & 0xFF;
                dependencies
                        .indoorTorchPaletteEffect()
                        .extinguishTorch(dependencies.entityFrame().currentLinkMotionState != 0x05);
            }
            if (dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                dependencies.roomObjectMutations().get().refreshOverworldCollisionAfterObjectMutation();
            } else {
                dependencies
                        .overworldCollision()
                        .setRoom(dependencies.location().activeRoom.roomObjectsArea());
                dependencies.overworldCollision().setGbcOverlay(null);
            }
        }
    }

    boolean writeMagicPowderTorchObject(int location, int expectedObject, int replacementObject, boolean redraw) {
        // Expiration writes $AB through the saved object pointer unconditionally.
        if (expectedObject >= 0
                && dependencies.roomSessionViews().get().objectAtRoomLocation(location) != expectedObject) {
            return false;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return false;
        }
        if (redraw) {
            dependencies.terrain().retainedTorchTiles.remove(areaIndex);
        } else {
            dependencies.roomObjectMutations().get().retainTorchTiles(areaIndex);
        }
        objects[areaIndex] = replacementObject & 0xFF;
        if (redraw
                && dependencies.location().activeRoom.renderValues() != null
                && areaIndex < dependencies.location().activeRoom.renderValues().length) {
            dependencies.location().activeRoom.renderValues()[areaIndex] = replacementObject & 0xFF;
        }
        if (redraw) {
            dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        }
        return true;
    }

    void retainTorchTiles(int areaIndex) {
        int location = areaIndex - RoomConstants.ROOM_OBJECTS_BASE;
        int topLeft = (location >>> 4) * 2 * RoomConstants.ROOM_TILE_WIDTH + (location & 0x0F) * 2;
        int[] saved = new int[8];
        for (int tile = 0; tile < 4; tile++) {
            int index = topLeft + (tile / 2) * RoomConstants.ROOM_TILE_WIDTH + tile % 2;
            saved[tile] = dependencies.location().activeRoom.tileIds()[index];
            saved[tile + 4] = dependencies.location().activeRoom.tileAttrs()[index];
        }
        dependencies.terrain().retainedTorchTiles.putIfAbsent(areaIndex, saved);
    }

    void applyRetainedTorchTiles() {
        dependencies
                .terrain()
                .retainedTorchTiles
                .keySet()
                .removeIf(areaIndex -> dependencies.location().activeRoom.roomObjectsArea()[areaIndex] != 0xAB);
        for (Map.Entry<Integer, int[]> entry :
                dependencies.terrain().retainedTorchTiles.entrySet()) {
            int location = entry.getKey() - RoomConstants.ROOM_OBJECTS_BASE;
            int topLeft = (location >>> 4) * 2 * RoomConstants.ROOM_TILE_WIDTH + (location & 0x0F) * 2;
            int[] saved = entry.getValue();
            for (int tile = 0; tile < 4; tile++) {
                int index = topLeft + (tile / 2) * RoomConstants.ROOM_TILE_WIDTH + tile % 2;
                dependencies.location().activeRoom.tileIds()[index] = saved[tile];
                dependencies.location().activeRoom.tileAttrs()[index] = saved[tile + 4];
            }
        }
    }

    int countUnlitTorches() {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                || dependencies.location().activeRoom.roomId() == 0xC4) {
            return 0;
        }
        int count = 0;
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int index = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (index < objects.length && objects[index] == 0xAB) {
                    count++;
                }
            }
        }
        return count;
    }

    void copyIndoorTorchPalettesToActiveRoom() {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return;
        }
        int[][] source = dependencies.indoorTorchPaletteEffect().palettes();
        int[][] target = dependencies.location().activeRoom.palettes();
        if (target == null) {
            return;
        }
        for (int palette = 0; palette < Math.min(source.length, target.length); palette++) {
            if (target[palette] == null) {
                continue;
            }
            System.arraycopy(
                    source[palette], 0, target[palette], 0, Math.min(source[palette].length, target[palette].length));
        }
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            IndoorTorchPaletteEffect indoorTorchPaletteEffect,
            RoomLocationState location,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            RoomPuzzleState puzzles,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw,
            RoomTerrainState terrain) {}

    private boolean applyPowderRequest(MagicPowderObjectRequest request) {
        return switch (request.action()) {
            case REVEAL -> dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                    && dependencies
                            .overworldBushInteraction()
                            .revealObjectAtLocation(
                                    request.location(),
                                    dependencies.location().activeRoom.roomId(),
                                    true,
                                    dependencies.location().activeRoom.roomObjectsArea(),
                                    dependencies.location().activeRoom.renderValues(),
                                    dependencies.location().activeRoom.gbcOverlay(),
                                    dependencies.location().activeRoom.tileIds(),
                                    dependencies.location().activeRoom.tileAttrs())
                            .changed();
            case IGNITE_TORCH -> dependencies
                    .roomObjectMutations()
                    .get()
                    .writeMagicPowderTorchObject(request.location(), 0xAB, 0xAC, true);
            case EXTINGUISH_TORCH -> dependencies
                    .roomObjectMutations()
                    .get()
                    .writeMagicPowderTorchObject(
                            request.location(),
                            -1,
                            0xAB,
                            dependencies.location().activeRoom.roomId() != 0x74
                                    && (dependencies.indoorTorchPaletteEffect().targetAddress() >= 0x0C
                                            || dependencies.entityFrame().currentLinkMotionState != 0x05));
        };
    }
}
