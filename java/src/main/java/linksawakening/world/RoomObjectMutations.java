package linksawakening.world;

import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpDestination;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.RoomSession.*;

/** RoomObjectMutations operations for the live room session. */
final class RoomObjectMutations {
    private final RoomSession session;
    RoomObjectMutations(RoomSession session) { this.session = session; }

    void applyBombObjectInteractions(List<BombExplosionEvent> events) {
        if (session.activeRoom == null || session.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
            || events == null || events.isEmpty()) {
            return;
        }

        for (BombExplosionEvent event : events) {
            if (!event.targetsRoomObjects()) {
                continue;
            }
            RoomEntity bomb = event.bombSlot() < session.activeRoom.entities().slots().size()
                ? session.activeRoom.entities().slots().get(event.bombSlot()) : null;
            if (session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                BombObjectInteraction.Candidate candidate = BombObjectInteraction.basicCandidate(
                    event.bombX(), event.bombVisualY(), event.countdown());
                session.applyBasicBombObjectCandidate(candidate);
                if (bomb != null && bomb.loaded()) {
                    session.applyPuzzleBombObjectCandidate(BombObjectInteraction.puzzleCandidate(
                        event.bombX(), bomb.y(), event.countdown()), event.bombSlot(), bomb.z());
                }
            } else if (bomb != null && bomb.loaded()) {
                BombObjectInteraction.Candidate candidate = BombObjectInteraction.puzzleCandidate(
                    event.bombX(), bomb.y(), event.countdown());
                if (!session.applyIndoorBombableBlockCandidate(candidate)) {
                    session.applyIndoorBombableWallCandidate(candidate, event.bombSlot());
                }
            }
        }
    }

    void applyBasicBombObjectCandidate(BombObjectInteraction.Candidate candidate) {
        if (candidate == null) {
            return;
        }
        int location = candidate.location();
        OverworldBushInteraction.CutResult result =
            session.overworldBushInteraction.revealObjectAtLocation(
                location,
                session.activeRoom.roomId(),
                true,
                session.activeRoom.roomObjectsArea(),
                session.activeRoom.renderValues(),
                session.activeRoom.gbcOverlay(),
                session.activeRoom.tileIds(),
                session.activeRoom.tileAttrs());
        if (!result.changed()) {
            return;
        }

        if (result.bushLeavesVisible() && session.entityBridge.entityRuntime != null) {
            int sourceSpriteVariant = result.originalObjectId() == 0x0A ? 0xFF : 0x01;
            session.entityBridge.entityRuntime.spawnLiftableRockSmash(
                session.overworldBushInteraction.effectOriginXForLocation(location),
                session.overworldBushInteraction.effectOriginYForLocation(location),
                sourceSpriteVariant);
        }
        session.refreshOverworldCollisionAfterObjectMutation();
    }

    void applyBoomerangObjectInteractions( List<BoomerangObjectRequest> requests) {
        if (session.activeRoom == null || session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
            || requests == null || requests.isEmpty()) {
            return;
        }

        for (BoomerangObjectRequest request : requests) {
            OverworldBushInteraction.CutResult result =
                session.overworldBushInteraction.revealObjectAtLocation(
                    request.location(), session.activeRoom.roomId(), true,
                    session.activeRoom.roomObjectsArea(), session.activeRoom.renderValues(),
                    session.activeRoom.gbcOverlay(), session.activeRoom.tileIds(), session.activeRoom.tileAttrs());
            if (!result.changed()) {
                continue;
            }
            if (session.transientVfxSystem != null) {
                session.transientVfxSystem.spawn(TransientVfxType.SMOKE,
                    request.objectLeft() + 0x08, request.objectTop() + 0x10);
            }
            session.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
                request.sourceSlot(), 0x01, 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x13));
            session.refreshOverworldCollisionAfterObjectMutation();
        }
    }

    void applyMagicRodObjectInteractions( List<MagicRodObjectRequest> requests) {
        if (session.activeRoom == null || requests == null || requests.isEmpty()) {
            return;
        }

        for (MagicRodObjectRequest request : requests) {
            boolean changed = false;
            if (session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                OverworldBushInteraction.CutResult result =
                    session.overworldBushInteraction.revealObjectAtLocation(
                        request.location(), session.activeRoom.roomId(), true,
                        session.activeRoom.roomObjectsArea(), session.activeRoom.renderValues(),
                        session.activeRoom.gbcOverlay(), session.activeRoom.tileIds(), session.activeRoom.tileAttrs());
                changed = result.changed();
            } else if (session.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
                && session.objectAtRoomLocation(request.location()) == 0x8A) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
                    + (request.location() & 0xF0) + (request.location() & 0x0F);
                int[] objects = session.activeRoom.roomObjectsArea();
                if (areaIndex >= 0 && areaIndex < objects.length) {
                    objects[areaIndex] = 0x04; // OBJECT_SIDE_VIEW_EMPTY
                    if (session.activeRoom.renderValues() != null
                        && areaIndex < session.activeRoom.renderValues().length) {
                        session.activeRoom.renderValues()[areaIndex] = 0x04;
                    }
                    session.refreshActiveRoomTilemap();
                    session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
                    session.overworldCollision.setGbcOverlay(null);
                    changed = true;
                }
            }

            if (!changed) {
                continue;
            }
            if (session.transientVfxSystem != null) {
                session.transientVfxSystem.spawn(TransientVfxType.SMOKE,
                    request.objectLeft() + 0x08, request.objectTop() + 0x10);
            }
            session.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
                request.sourceSlot(), 0x04, 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x13));
            if (session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                session.refreshOverworldCollisionAfterObjectMutation();
            }
        }
    }

    void applyMagicPowderObjectInteractions( List<MagicPowderObjectRequest> requests) {
        if (session.activeRoom == null || requests == null || requests.isEmpty()) {
            return;
        }

        for (MagicPowderObjectRequest request : requests) {
            boolean changed = switch (request.action()) {
                case REVEAL -> session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                    && session.overworldBushInteraction.revealObjectAtLocation(
                        request.location(), session.activeRoom.roomId(), true,
                        session.activeRoom.roomObjectsArea(), session.activeRoom.renderValues(),
                        session.activeRoom.gbcOverlay(), session.activeRoom.tileIds(), session.activeRoom.tileAttrs())
                        .changed();
                case IGNITE_TORCH -> session.writeMagicPowderTorchObject(
                    request.location(), 0xAB, 0xAC, true);
                case EXTINGUISH_TORCH -> session.writeMagicPowderTorchObject(
                    request.location(), -1, 0xAB,
                    session.activeRoom.roomId() != 0x74
                        && (session.indoorTorchPaletteEffect.targetAddress() >= 0x0C
                            || session.currentLinkMotionState != 0x05));
            };

            if (!changed) {
                continue;
            }
            if (request.action() == RoomEntityRuntime.MagicPowderObjectAction.IGNITE_TORCH) {
                session.roomTriggerCount = (session.roomTriggerCount + 1) & 0xFF;
                session.indoorTorchPaletteEffect.igniteTorch();
            } else if (request.action()
                == RoomEntityRuntime.MagicPowderObjectAction.EXTINGUISH_TORCH
                && session.activeRoom.roomId() != 0x74) {
                session.roomTriggerCount = (session.roomTriggerCount - 1) & 0xFF;
                session.indoorTorchPaletteEffect.extinguishTorch(session.currentLinkMotionState != 0x05);
            }
            if (session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                session.refreshOverworldCollisionAfterObjectMutation();
            } else {
                session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
                session.overworldCollision.setGbcOverlay(null);
            }
        }
    }

    boolean writeMagicPowderTorchObject(int location, int expectedObject, int replacementObject, boolean redraw) {
        // Expiration writes $AB through the saved object pointer unconditionally.
        if (expectedObject >= 0 && session.objectAtRoomLocation(location) != expectedObject) {
            return false;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = session.activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return false;
        }
        if (redraw) {
            session.retainedTorchTiles.remove(areaIndex);
        } else {
            session.retainTorchTiles(areaIndex);
        }
        objects[areaIndex] = replacementObject & 0xFF;
        if (redraw && session.activeRoom.renderValues() != null
            && areaIndex < session.activeRoom.renderValues().length) {
            session.activeRoom.renderValues()[areaIndex] = replacementObject & 0xFF;
        }
        if (redraw) {
            session.refreshActiveRoomTilemap();
        }
        return true;
    }

    void retainTorchTiles(int areaIndex) {
        int location = areaIndex - RoomConstants.ROOM_OBJECTS_BASE;
        int topLeft = (location >>> 4) * 2 * RoomConstants.ROOM_TILE_WIDTH
            + (location & 0x0F) * 2;
        int[] saved = new int[8];
        for (int tile = 0; tile < 4; tile++) {
            int index = topLeft + (tile / 2) * RoomConstants.ROOM_TILE_WIDTH + tile % 2;
            saved[tile] = session.activeRoom.tileIds()[index];
            saved[tile + 4] = session.activeRoom.tileAttrs()[index];
        }
        session.retainedTorchTiles.putIfAbsent(areaIndex, saved);
    }

    void applyRetainedTorchTiles() {
        session.retainedTorchTiles.keySet().removeIf(
            areaIndex -> session.activeRoom.roomObjectsArea()[areaIndex] != 0xAB);
        for (Map.Entry<Integer, int[]> entry : session.retainedTorchTiles.entrySet()) {
            int location = entry.getKey() - RoomConstants.ROOM_OBJECTS_BASE;
            int topLeft = (location >>> 4) * 2 * RoomConstants.ROOM_TILE_WIDTH
                + (location & 0x0F) * 2;
            int[] saved = entry.getValue();
            for (int tile = 0; tile < 4; tile++) {
                int index = topLeft + (tile / 2) * RoomConstants.ROOM_TILE_WIDTH + tile % 2;
                session.activeRoom.tileIds()[index] = saved[tile];
                session.activeRoom.tileAttrs()[index] = saved[tile + 4];
            }
        }
    }

    int countUnlitTorches() {
        if (session.activeRoom == null || session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            || session.activeRoom.roomId() == 0xC4) {
            return 0;
        }
        int count = 0;
        int[] objects = session.activeRoom.roomObjectsArea();
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int index = RoomConstants.ROOM_OBJECTS_BASE
                    + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (index < objects.length && objects[index] == 0xAB) {
                    count++;
                }
            }
        }
        return count;
    }

    void copyIndoorTorchPalettesToActiveRoom() {
        if (session.activeRoom == null || session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return;
        }
        int[][] source = session.indoorTorchPaletteEffect.palettes();
        int[][] target = session.activeRoom.palettes();
        if (target == null) {
            return;
        }
        for (int palette = 0; palette < Math.min(source.length, target.length); palette++) {
            if (target[palette] == null) {
                continue;
            }
            System.arraycopy(source[palette], 0, target[palette], 0,
                Math.min(source[palette].length, target[palette].length));
        }
    }

    void applyPuzzleBombObjectCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot, int sourceZ) {
        if (candidate == null) {
            return;
        }
        int objectId = session.objectAtRoomLocation(candidate.location());
        if (objectId < session.OBJECT_GIANT_SKULL_TOP_LEFT
            || objectId > session.OBJECT_GIANT_SKULL_BOTTOM_RIGHT) {
            session.applyOutdoorBombableCaveDoorCandidate(candidate, sourceSlot);
            return;
        }

        int blockTop = candidate.objectTop() & 0xE0;
        int blockLeft = candidate.objectLeft() & 0xE0;
        int[] rubbleXOffsets = {0x18, 0x08, 0x18, 0x08};
        int[] rubbleYOffsets = {0x20, 0x20, 0x10, 0x10};
        if (session.entityBridge.entityRuntime != null) {
            for (int index = 0; index < rubbleXOffsets.length; index++) {
                session.entityBridge.entityRuntime.spawnLiftableRockRubble(
                    blockLeft + rubbleXOffsets[index],
                    blockTop + rubbleYOffsets[index] - sourceZ);
            }
        }
        session.queuePuzzleSolvedJingle(sourceSlot);

        int blockLocation = blockTop | (blockLeft >>> 4);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                session.writeBombPuzzleObject(blockLocation
                    + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column,
                    session.OBJECT_ROCKY_GROUND);
            }
        }
        session.roomState.overworldRoomStatus[session.activeRoom.roomId()] |= (byte) session.OW_ROOM_STATUS_OPENED;
        session.refreshOverworldCollisionAfterObjectMutation();
    }

    void applyOutdoorBombableCaveDoorCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) {
        int objectId = session.objectAtRoomLocation(candidate.location());
        int doorIndex = session.romTables.objectPhysicsFlag(RomTables.PHYSICS_TABLE_OVERWORLD, objectId)
            - session.BOMBABLE_WALL_PHYSICS_BASE;
        if (objectId != session.OBJECT_BOMBABLE_CAVE_DOOR || doorIndex < 0 || doorIndex >= 4) {
            return;
        }

        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (candidate.objectTop() & 0xF0)
            + ((candidate.objectLeft() & 0xF0) >>> 4);
        int[] objects = session.activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }

        session.writeBombPuzzleObject(candidate.location(), session.OBJECT_ROCKY_CAVE_DOOR);
        session.bombedCaveDoorTileOverrides.add(areaIndex);
        session.roomState.overworldRoomStatus[session.activeRoom.roomId()] |= (byte) session.OW_ROOM_STATUS_OPENED;
        session.queuePuzzleSolvedJingle(sourceSlot);
        session.refreshActiveRoomTilemap();
        session.refreshOverworldCollisionAfterObjectMutation();
    }

    void writeBombPuzzleObject(int location, int objectId) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = session.activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = objectId & 0xFF;
        if (session.activeRoom.renderValues() != null) {
            session.activeRoom.renderValues()[areaIndex] = objectId & 0xFF;
        }
        session.overworldBushInteraction.refreshRoomObjectCell(
            session.activeRoom.roomId(), location, objects, session.activeRoom.renderValues(),
            session.activeRoom.gbcOverlay(), session.activeRoom.tileIds(), session.activeRoom.tileAttrs());
    }

    void refreshOverworldCollisionAfterObjectMutation() {
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        session.overworldCollision.setGbcOverlay(session.activeRoom.gbcOverlay());
    }

    void applyIndoorBombableWallCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) {
        if (candidate == null) {
            return;
        }
        int location = candidate.location();
        int objectId = session.objectAtRoomLocation(location);
        int physicsTable = session.activeRoom.mapId() == 0xFF
            ? RomTables.PHYSICS_TABLE_INDOORS2 : RomTables.PHYSICS_TABLE_INDOORS1;
        int wallIndex = session.romTables.objectPhysicsFlag(physicsTable, objectId)
            - session.BOMBABLE_WALL_PHYSICS_BASE;
        if (wallIndex < 0 || wallIndex >= 4) {
            return;
        }

        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = session.activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = wallIndex < 2
            ? session.OBJECT_BOMBED_PASSAGE_VERTICAL : session.OBJECT_BOMBED_PASSAGE_HORIZONTAL;

        byte[] status = session.indoorStatusTableForMap(session.activeRoom.mapId());
        int currentStatus = switch (wallIndex) {
            case 0 -> 0x04;
            case 1 -> 0x08;
            case 2 -> 0x02;
            case 3 -> 0x01;
            default -> throw new AssertionError(wallIndex);
        };
        int adjacentStatus = switch (wallIndex) {
            case 0 -> 0x08;
            case 1 -> 0x04;
            case 2 -> 0x01;
            case 3 -> 0x02;
            default -> throw new AssertionError(wallIndex);
        };
        int adjacentRoom = session.adjacentIndoorRoomIdForBombWall(wallIndex);
        status[session.activeRoom.roomId()] |= (byte) currentStatus;
        status[adjacentRoom] |= (byte) adjacentStatus;
        session.bombedWallTileOverrides.put(areaIndex, wallIndex < 2 ? 0 : 1);
        session.queuePuzzleSolvedJingle(sourceSlot);

        session.refreshActiveRoomTilemap();
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        session.overworldCollision.setGbcOverlay(null);
    }

    void queuePuzzleSolvedJingle(int sourceSlot) {
        session.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
            sourceSlot, 0x02, 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, 0x02));
    }

    boolean applyIndoorBombableBlockCandidate(BombObjectInteraction.Candidate candidate) {
        if (candidate == null) {
            return false;
        }
        int location = candidate.location();
        if (session.objectAtRoomLocation(location) != session.OBJECT_BOMBABLE_BLOCK) {
            return false;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = session.activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return false;
        }

        objects[areaIndex] = session.OBJECT_FLOOR_OD;
        session.indoorStatusTableForMap(session.activeRoom.mapId())[session.activeRoom.roomId()]
            |= (byte) session.INDOOR_ROOM_STATUS_EVENT_3;
        session.bombedBlockTileOverrides.add(areaIndex);

        if (session.entityBridge.entityRuntime != null) {
            session.entityBridge.entityRuntime.spawnLiftableRockSmash(
                candidate.objectLeft() + 0x08, candidate.objectTop() + 0x10, 0x00);
        }

        session.refreshActiveRoomTilemap();
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        session.overworldCollision.setGbcOverlay(null);
        return true;
    }

    int colorShellObjectAt(RoomEntity entity, int relativeOffset) {
        if (session.activeRoom == null || entity == null) {
            return 0xFF;
        }
        int index = session.colorShellObjectIndex(entity) + relativeOffset;
        int[] objects = session.activeRoom.roomObjectsArea();
        return index >= 0 && index < objects.length ? objects[index] : 0xFF;
    }

    void writeColorShellObject(RoomEntity entity, int objectId) {
        if (session.activeRoom == null || entity == null) {
            return;
        }
        int index = session.colorShellObjectIndex(entity);
        int[] objects = session.activeRoom.roomObjectsArea();
        if (index < 0 || index >= objects.length) {
            return;
        }
        objects[index] = objectId & 0xFF;
        session.refreshActiveRoomTilemap();
    }
}
