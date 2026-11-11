package linksawakening.world.room;

import linksawakening.world.object.BombObjectInteraction;
import linksawakening.world.object.OverworldBushInteraction;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.BombExplosionEvent;

import linksawakening.world.entity.RoomEntity;

import linksawakening.world.entity.event.MagicPowderObjectAction;

import linksawakening.world.transition.Warp;

import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxType;
import java.util.List;
import java.util.Map;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.room.RoomSession.*;

/** RoomObjectMutations operations for the live room session. */
final class RoomObjectMutations {
    private final RoomSession session;
    RoomObjectMutations(RoomSession session) { this.session = session; }

    /**
     * ApplyLinkMotionState ($00:1794) calls func_002_753A after XY movement.
     * $02:7587-7589 copies the last GetObjectUnderLink result, not this
     * frame's pre-movement coordinates. Entity pushes, airborne frames and
     * skipped probes can make that older than one frame.
     */
    boolean tickCrackedFloor(RoomSession.LinkGroundFrame frame, boolean dialogGotItemActive) {
        var context = session.context;
        if (context.activeRoom == null) return false;
        if (session.hookshotActive()) {
            // $02:7549-754D takes func_002_755B: sample, but no $7589 copy
            // and no ApplyLinkGroundPhysics call.
            sampleLinkGround(frame.pixelX(), frame.pixelY());
            return false;
        }
        context.linkGroundFinalRoomPosition = context.linkGroundRoomPosition;
        // $02:7590-7593 and $75B6-75BB bypass ground physics.
        if (frame.airborne() || frame.motionState() == 0x02) return false;
        return applyCrackedFloorGroundPhysics(frame, dialogGotItemActive);
    }

    /** ApplyLinkGroundMotion's landing call ($02:4588), before XY and the $7589 copy. */
    boolean applyCrackedFloorGroundPhysics(RoomSession.LinkGroundFrame frame, boolean dialogGotItemActive) {
        var context = session.context;
        if (context.activeRoom == null) return false;
        boolean bypassEntry = frame.dialogActive() || frame.roomTransitionActive();
        if (!bypassEntry) {
            sampleLinkGround(frame.pixelX(), frame.pixelY()); // GetObjectUnderLink $02:75C7
            context.linkGroundObjectPhysics = context.overworldCollision.objectPhysicsFlagAtPoint(
                (frame.pixelX() + 8) & 0xFF, (frame.pixelY() + 12) & 0xFF);
            // $02:75D2-75D4/$75DA-75E5 bypass Default for these hazards.
            if ((!context.overworldCollision.usesIndoorPhysicsTable()
                    && context.linkGroundObjectUnderEntity == 0x61)
                || (context.overworldCollision.usesIndoorPhysicsTable()
                    && context.linkGroundObjectUnderEntity == 0x4C
                    && (((frame.pixelY() + 15) & 15) >= 12))) return false;
        }
        if (!reachesDefaultGroundPhysics(context.linkGroundObjectPhysics, bypassEntry,
                frame.slowWalkingSpeed(), frame.itemUsageContext())) return false;
        // Default can return at $02:77E8 while standing on a raised switch block.
        if (linksawakening.world.object.SwitchBlockLinkInteraction.marksStanding(
                context.linkGroundObjectUnderEntity, context.linkGroundObjectPhysics,
                context.switchBlocksState)) return false;
        // $02:77F7-77FB gates on wIsIndoor, NOT hIsSideScrolling. Overworld
        // $DF has solid physics ($01), but still exits here without resetting.
        if (!context.overworldCollision.usesIndoorPhysicsTable()) return false;
        // $02:75C4 -> 76C0 -> 7757 can re-enter Default using cached physics.
        // Transitions retain wC1C9 at $02:7802; dialogs reaching $7858 reset it.
        if (frame.roomTransitionActive()) return false;
        // $02:7804-783B returns while an unpressed floor button is sinking.
        if (context.linkGroundObjectUnderEntity == 0xAA && context.switchButtonPressed == 0) return false;
        if (context.linkGroundRoomPosition != context.linkGroundFinalRoomPosition
            || context.linkGroundObjectUnderEntity != 0xDF || frame.interactionBlocked()
            || dialogGotItemActive || frame.dialogActive()) {
            context.crackedFloorCounter = 0; // $02:7851-785D/$786C
            return false;
        }
        context.crackedFloorCounter = (context.crackedFloorCounter + 1) & 0xFF;
        if (context.crackedFloorCounter < 0x28) return false;
        // label_002_4D97 writes $AE and redraws its tiles and GBC attributes.
        context.activeRoom.roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + context.linkGroundRoomPosition] = 0xAE;
        session.refreshActiveRoomTilemap();
        context.colorShellSoundSink.play(linksawakening.gameplay.GameplaySoundEvent.CRACKED_FLOOR_COLLAPSE);
        return true;
    }

    /** GetObjectUnderLink $02:7512-7536, translated from ROM center/bottom to Java top-left. */
    private void sampleLinkGround(int pixelX, int pixelY) {
        int location = linkGroundLocation(pixelX, pixelY);
        session.context.linkGroundRoomPosition = location;
        session.context.linkGroundObjectUnderEntity = session.objectAtRoomLocation(location);
    }

    static int linkGroundLocation(int pixelX, int pixelY) {
        // hLinkPositionX = Java X+8; hLinkPositionY = Java Y+16, then subtract 4.
        // AND $F0 after byte arithmetic also preserves the ROM's wrap behavior.
        return ((pixelY + 12) & 0xF0) | (((pixelX + 8) & 0xF0) >>> 4);
    }

    /** Only the control-flow decision; movement, damage and water effects stay with Link. */
    static boolean reachesDefaultGroundPhysics(int physics, boolean bypassEntry,
                                               int slowWalkingSpeed, int itemUsageContext) {
        if (!bypassEntry) {
            if (physics == 0 || physics == 0xFF) return true; // $75EE/$763A
            if (physics == 0xE0 || physics >= 0xF0 || physics == 0x50 || physics == 0x51) return false;
        }
        // label_002_76C0 is also the entry used by dialogs/transitions ($75C4).
        if (physics == 0x08 || physics == 0x09) return true; // $76D2/$76E1
        if (physics == 0x07 || physics == 0x0B) {
            if (slowWalkingSpeed != 0) return false; // $76F1 -> current handler $7C50
            if (itemUsageContext == 0x80) return true; // raft: $76F9 -> $7757
            return false; // swimming/recovery and entry into water return before Default
        }
        return physics != 0x06; // grass $7752 returns; shallow water falls through $779F
    }

    /** CheckStaticSwordCollision $00:161A-1679; RevealObjectUnderObject $14:5584-55E3. */
    boolean breakIndoorSwordCrystal(int location) {
        if (session.context.activeRoom == null
            || session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            || location < 0 || (location & 15) >= 10 || (location >> 4) >= 8
            || session.objectAtRoomLocation(location) != 0xDD) return false;
        session.context.activeRoom.roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + location] = 0x0D;
        session.refreshActiveRoomTilemap();
        if (session.context.entityBridge.entityRuntime != null) {
            // Variant zero uses LiftableRockStartSmashingAnimation ($03:53E4):
            // fifteen frames of rock debris and NOISE_SFX_POT_SMASHED ($09).
            session.context.entityBridge.entityRuntime.spawnLiftableRockSmash(
                ((location & 15) << 4) + 8, (location & 0xF0) + 16, 0);
            // Link runs before the entity tick, which clears its frame outbox.
            session.context.entityBridge.pendingRoomEntityEvents.addAll(
                session.context.entityBridge.entityRuntime.consumePendingEntityEvents());
        }
        return true;
    }

    void applyBombObjectInteractions(List<BombExplosionEvent> events) {
        if (session.context.activeRoom == null || session.context.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
            || events == null || events.isEmpty()) {
            return;
        }

        for (BombExplosionEvent event : events) {
            if (!event.targetsRoomObjects()) {
                continue;
            }
            RoomEntity bomb = event.bombSlot() < session.context.activeRoom.entities().slots().size()
                ? session.context.activeRoom.entities().slots().get(event.bombSlot()) : null;
            if (session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
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
            session.context.overworldBushInteraction.revealObjectAtLocation(
                location,
                session.context.activeRoom.roomId(),
                true,
                session.context.activeRoom.roomObjectsArea(),
                session.context.activeRoom.renderValues(),
                session.context.activeRoom.gbcOverlay(),
                session.context.activeRoom.tileIds(),
                session.context.activeRoom.tileAttrs());
        if (!result.changed()) {
            return;
        }

        if (result.bushLeavesVisible() && session.context.entityBridge.entityRuntime != null) {
            int sourceSpriteVariant = result.originalObjectId() == 0x0A ? 0xFF : 0x01;
            session.context.entityBridge.entityRuntime.spawnLiftableRockSmash(
                session.context.overworldBushInteraction.effectOriginXForLocation(location),
                session.context.overworldBushInteraction.effectOriginYForLocation(location),
                sourceSpriteVariant);
        }
        session.refreshOverworldCollisionAfterObjectMutation();
    }

    void applyBoomerangObjectInteractions( List<BoomerangObjectRequest> requests) {
        if (session.context.activeRoom == null || session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
            || requests == null || requests.isEmpty()) {
            return;
        }

        for (BoomerangObjectRequest request : requests) {
            OverworldBushInteraction.CutResult result =
                session.context.overworldBushInteraction.revealObjectAtLocation(
                    request.location(), session.context.activeRoom.roomId(), true,
                    session.context.activeRoom.roomObjectsArea(), session.context.activeRoom.renderValues(),
                    session.context.activeRoom.gbcOverlay(), session.context.activeRoom.tileIds(), session.context.activeRoom.tileAttrs());
            if (!result.changed()) {
                continue;
            }
            if (session.context.transientVfxSystem != null) {
                session.context.transientVfxSystem.spawn(TransientVfxType.SMOKE,
                    request.objectLeft() + 0x08, request.objectTop() + 0x10);
            }
            session.context.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
                request.sourceSlot(), 0x01, 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x13));
            session.refreshOverworldCollisionAfterObjectMutation();
        }
    }

    void applyMagicRodObjectInteractions( List<MagicRodObjectRequest> requests) {
        if (session.context.activeRoom == null || requests == null || requests.isEmpty()) {
            return;
        }

        for (MagicRodObjectRequest request : requests) {
            boolean changed = false;
            if (session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                OverworldBushInteraction.CutResult result =
                    session.context.overworldBushInteraction.revealObjectAtLocation(
                        request.location(), session.context.activeRoom.roomId(), true,
                        session.context.activeRoom.roomObjectsArea(), session.context.activeRoom.renderValues(),
                        session.context.activeRoom.gbcOverlay(), session.context.activeRoom.tileIds(), session.context.activeRoom.tileAttrs());
                changed = result.changed();
            } else if (session.context.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
                && session.objectAtRoomLocation(request.location()) == 0x8A) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
                    + (request.location() & 0xF0) + (request.location() & 0x0F);
                int[] objects = session.context.activeRoom.roomObjectsArea();
                if (areaIndex >= 0 && areaIndex < objects.length) {
                    objects[areaIndex] = 0x04; // OBJECT_SIDE_VIEW_EMPTY
                    if (session.context.activeRoom.renderValues() != null
                        && areaIndex < session.context.activeRoom.renderValues().length) {
                        session.context.activeRoom.renderValues()[areaIndex] = 0x04;
                    }
                    session.refreshActiveRoomTilemap();
                    session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
                    session.context.overworldCollision.setGbcOverlay(null);
                    changed = true;
                }
            }

            if (!changed) {
                continue;
            }
            if (session.context.transientVfxSystem != null) {
                session.context.transientVfxSystem.spawn(TransientVfxType.SMOKE,
                    request.objectLeft() + 0x08, request.objectTop() + 0x10);
            }
            session.context.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
                request.sourceSlot(), 0x04, 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x13));
            if (session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                session.refreshOverworldCollisionAfterObjectMutation();
            }
        }
    }

    void applyMagicPowderObjectInteractions( List<MagicPowderObjectRequest> requests) {
        if (session.context.activeRoom == null || requests == null || requests.isEmpty()) {
            return;
        }

        for (MagicPowderObjectRequest request : requests) {
            boolean changed = switch (request.action()) {
                case REVEAL -> session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                    && session.context.overworldBushInteraction.revealObjectAtLocation(
                        request.location(), session.context.activeRoom.roomId(), true,
                        session.context.activeRoom.roomObjectsArea(), session.context.activeRoom.renderValues(),
                        session.context.activeRoom.gbcOverlay(), session.context.activeRoom.tileIds(), session.context.activeRoom.tileAttrs())
                        .changed();
                case IGNITE_TORCH -> session.writeMagicPowderTorchObject(
                    request.location(), 0xAB, 0xAC, true);
                case EXTINGUISH_TORCH -> session.writeMagicPowderTorchObject(
                    request.location(), -1, 0xAB,
                    session.context.activeRoom.roomId() != 0x74
                        && (session.context.indoorTorchPaletteEffect.targetAddress() >= 0x0C
                            || session.context.currentLinkMotionState != 0x05));
            };

            if (!changed) {
                continue;
            }
            if (request.action() == MagicPowderObjectAction.IGNITE_TORCH) {
                session.context.roomTriggerCount = (session.context.roomTriggerCount + 1) & 0xFF;
                session.context.indoorTorchPaletteEffect.igniteTorch();
            } else if (request.action()
                == MagicPowderObjectAction.EXTINGUISH_TORCH
                && session.context.activeRoom.roomId() != 0x74) {
                session.context.roomTriggerCount = (session.context.roomTriggerCount - 1) & 0xFF;
                session.context.indoorTorchPaletteEffect.extinguishTorch(session.context.currentLinkMotionState != 0x05);
            }
            if (session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                session.refreshOverworldCollisionAfterObjectMutation();
            } else {
                session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
                session.context.overworldCollision.setGbcOverlay(null);
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
        int[] objects = session.context.activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return false;
        }
        if (redraw) {
            session.context.retainedTorchTiles.remove(areaIndex);
        } else {
            session.retainTorchTiles(areaIndex);
        }
        objects[areaIndex] = replacementObject & 0xFF;
        if (redraw && session.context.activeRoom.renderValues() != null
            && areaIndex < session.context.activeRoom.renderValues().length) {
            session.context.activeRoom.renderValues()[areaIndex] = replacementObject & 0xFF;
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
            saved[tile] = session.context.activeRoom.tileIds()[index];
            saved[tile + 4] = session.context.activeRoom.tileAttrs()[index];
        }
        session.context.retainedTorchTiles.putIfAbsent(areaIndex, saved);
    }

    void applyRetainedTorchTiles() {
        session.context.retainedTorchTiles.keySet().removeIf(
            areaIndex -> session.context.activeRoom.roomObjectsArea()[areaIndex] != 0xAB);
        for (Map.Entry<Integer, int[]> entry : session.context.retainedTorchTiles.entrySet()) {
            int location = entry.getKey() - RoomConstants.ROOM_OBJECTS_BASE;
            int topLeft = (location >>> 4) * 2 * RoomConstants.ROOM_TILE_WIDTH
                + (location & 0x0F) * 2;
            int[] saved = entry.getValue();
            for (int tile = 0; tile < 4; tile++) {
                int index = topLeft + (tile / 2) * RoomConstants.ROOM_TILE_WIDTH + tile % 2;
                session.context.activeRoom.tileIds()[index] = saved[tile];
                session.context.activeRoom.tileAttrs()[index] = saved[tile + 4];
            }
        }
    }

    int countUnlitTorches() {
        if (session.context.activeRoom == null || session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            || session.context.activeRoom.roomId() == 0xC4) {
            return 0;
        }
        int count = 0;
        int[] objects = session.context.activeRoom.roomObjectsArea();
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
        if (session.context.activeRoom == null || session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return;
        }
        int[][] source = session.context.indoorTorchPaletteEffect.palettes();
        int[][] target = session.context.activeRoom.palettes();
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
        if (session.context.entityBridge.entityRuntime != null) {
            for (int index = 0; index < rubbleXOffsets.length; index++) {
                session.context.entityBridge.entityRuntime.spawnLiftableRockRubble(
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
        session.context.roomState.overworldRoomStatus[session.context.activeRoom.roomId()] |= (byte) session.OW_ROOM_STATUS_OPENED;
        session.refreshOverworldCollisionAfterObjectMutation();
    }

    void applyOutdoorBombableCaveDoorCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) {
        int objectId = session.objectAtRoomLocation(candidate.location());
        int doorIndex = session.context.romTables.objectPhysicsFlag(RomTables.PHYSICS_TABLE_OVERWORLD, objectId)
            - session.BOMBABLE_WALL_PHYSICS_BASE;
        if (objectId != session.OBJECT_BOMBABLE_CAVE_DOOR || doorIndex < 0 || doorIndex >= 4) {
            return;
        }

        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (candidate.objectTop() & 0xF0)
            + ((candidate.objectLeft() & 0xF0) >>> 4);
        int[] objects = session.context.activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }

        session.writeBombPuzzleObject(candidate.location(), session.OBJECT_ROCKY_CAVE_DOOR);
        session.context.bombedCaveDoorTileOverrides.add(areaIndex);
        session.context.roomState.overworldRoomStatus[session.context.activeRoom.roomId()] |= (byte) session.OW_ROOM_STATUS_OPENED;
        session.queuePuzzleSolvedJingle(sourceSlot);
        session.refreshActiveRoomTilemap();
        session.refreshOverworldCollisionAfterObjectMutation();
    }

    void writeBombPuzzleObject(int location, int objectId) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = session.context.activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = objectId & 0xFF;
        if (session.context.activeRoom.renderValues() != null) {
            session.context.activeRoom.renderValues()[areaIndex] = objectId & 0xFF;
        }
        session.context.overworldBushInteraction.refreshRoomObjectCell(
            session.context.activeRoom.roomId(), location, objects, session.context.activeRoom.renderValues(),
            session.context.activeRoom.gbcOverlay(), session.context.activeRoom.tileIds(), session.context.activeRoom.tileAttrs());
    }

    void refreshOverworldCollisionAfterObjectMutation() {
        session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
        session.context.overworldCollision.setGbcOverlay(session.context.activeRoom.gbcOverlay());
    }

    void applyIndoorBombableWallCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) {
        if (candidate == null) {
            return;
        }
        int location = candidate.location();
        int objectId = session.objectAtRoomLocation(location);
        int physicsTable = session.context.activeRoom.mapId() == 0xFF
            ? RomTables.PHYSICS_TABLE_INDOORS2 : RomTables.PHYSICS_TABLE_INDOORS1;
        int wallIndex = session.context.romTables.objectPhysicsFlag(physicsTable, objectId)
            - session.BOMBABLE_WALL_PHYSICS_BASE;
        if (wallIndex < 0 || wallIndex >= 4) {
            return;
        }

        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = session.context.activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = wallIndex < 2
            ? session.OBJECT_BOMBED_PASSAGE_VERTICAL : session.OBJECT_BOMBED_PASSAGE_HORIZONTAL;

        byte[] status = session.indoorStatusTableForMap(session.context.activeRoom.mapId());
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
        status[session.context.activeRoom.roomId()] |= (byte) currentStatus;
        status[adjacentRoom] |= (byte) adjacentStatus;
        session.context.bombedWallTileOverrides.put(areaIndex, wallIndex < 2 ? 0 : 1);
        session.queuePuzzleSolvedJingle(sourceSlot);

        session.refreshActiveRoomTilemap();
        session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
        session.context.overworldCollision.setGbcOverlay(null);
    }

    void queuePuzzleSolvedJingle(int sourceSlot) {
        session.context.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
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
        int[] objects = session.context.activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return false;
        }

        objects[areaIndex] = session.OBJECT_FLOOR_OD;
        session.indoorStatusTableForMap(session.context.activeRoom.mapId())[session.context.activeRoom.roomId()]
            |= (byte) session.INDOOR_ROOM_STATUS_EVENT_3;
        session.context.bombedBlockTileOverrides.add(areaIndex);

        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.entityRuntime.spawnLiftableRockSmash(
                candidate.objectLeft() + 0x08, candidate.objectTop() + 0x10, 0x00);
        }

        session.refreshActiveRoomTilemap();
        session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
        session.context.overworldCollision.setGbcOverlay(null);
        return true;
    }

    int colorShellObjectAt(RoomEntity entity, int relativeOffset) {
        if (session.context.activeRoom == null || entity == null) {
            return 0xFF;
        }
        int index = session.colorShellObjectIndex(entity) + relativeOffset;
        int[] objects = session.context.activeRoom.roomObjectsArea();
        return index >= 0 && index < objects.length ? objects[index] : 0xFF;
    }

    void writeColorShellObject(RoomEntity entity, int objectId) {
        if (session.context.activeRoom == null || entity == null) {
            return;
        }
        int index = session.colorShellObjectIndex(entity);
        int[] objects = session.context.activeRoom.roomObjectsArea();
        if (index < 0 || index >= objects.length) {
            return;
        }
        objects[index] = objectId & 0xFF;
        session.refreshActiveRoomTilemap();
    }
}
