package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.BOMBABLE_WALL_PHYSICS_BASE;
import static linksawakening.world.room.RoomSessionRules.INDOOR_ROOM_STATUS_EVENT_3;
import static linksawakening.world.room.RoomSessionRules.OBJECT_BOMBABLE_BLOCK;
import static linksawakening.world.room.RoomSessionRules.OBJECT_BOMBABLE_CAVE_DOOR;
import static linksawakening.world.room.RoomSessionRules.OBJECT_BOMBED_PASSAGE_HORIZONTAL;
import static linksawakening.world.room.RoomSessionRules.OBJECT_BOMBED_PASSAGE_VERTICAL;
import static linksawakening.world.room.RoomSessionRules.OBJECT_FLOOR_OD;
import static linksawakening.world.room.RoomSessionRules.OBJECT_GIANT_SKULL_BOTTOM_RIGHT;
import static linksawakening.world.room.RoomSessionRules.OBJECT_GIANT_SKULL_TOP_LEFT;
import static linksawakening.world.room.RoomSessionRules.OBJECT_ROCKY_CAVE_DOOR;
import static linksawakening.world.room.RoomSessionRules.OBJECT_ROCKY_GROUND;
import static linksawakening.world.room.RoomSessionRules.OW_ROOM_STATUS_OPENED;

import java.util.List;
import java.util.Map;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.MagicPowderObjectAction;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.motion.HookshotBridgeMotion;
import linksawakening.world.object.BombObjectInteraction;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.transition.Warp;

/** ApplyLinkMotionState and bomb/projectile object writes, including their immediate tile redraws. */
final class RoomObjectMutations {
    private final Dependencies dependencies;

    RoomObjectMutations(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * ApplyLinkMotionState ($00:1794) calls func_002_753A after XY movement.
     * $02:7587-7589 copies the last GetObjectUnderLink result, not this
     * frame's pre-movement coordinates. Entity pushes, airborne frames and
     * skipped probes can make that older than one frame.
     */
    boolean tickCrackedFloor(LinkGroundFrame frame, boolean dialogGotItemActive) {

        if (dependencies.location().activeRoom == null) return false;
        if (dependencies.entityRuntimeState().hookshotActive()) {
            // $02:7549-754D takes func_002_755B: sample, but no $7589 copy
            // and no ApplyLinkGroundPhysics call.
            sampleLinkGround(frame.pixelX(), frame.pixelY());
            return false;
        }
        dependencies.terrain().linkGroundFinalRoomPosition = dependencies.terrain().linkGroundRoomPosition;
        // $02:7590-7593 and $75B6-75BB bypass ground physics.
        if (frame.airborne() || frame.motionState() == linksawakening.state.LinkMotionState.UNSTUCKING) return false;
        return applyCrackedFloorGroundPhysics(frame, dialogGotItemActive);
    }

    /** ApplyLinkGroundMotion's landing call ($02:4588), before XY and the $7589 copy. */
    boolean applyCrackedFloorGroundPhysics(LinkGroundFrame frame, boolean dialogGotItemActive) {

        if (dependencies.location().activeRoom == null) return false;
        boolean bypassEntry = frame.dialogActive() || frame.roomTransitionActive();
        if (!bypassEntry) {
            sampleLinkGround(frame.pixelX(), frame.pixelY()); // GetObjectUnderLink $02:75C7
            dependencies.terrain().linkGroundObjectPhysics = dependencies
                    .overworldCollision()
                    .objectPhysicsFlagAtPoint((frame.pixelX() + 8) & 0xFF, (frame.pixelY() + 12) & 0xFF);
            // $02:75D2-75D4/$75DA-75E5 bypass Default for these hazards.
            if ((!dependencies.overworldCollision().usesIndoorPhysicsTable()
                            && dependencies.terrain().linkGroundObjectUnderEntity == 0x61)
                    || (dependencies.overworldCollision().usesIndoorPhysicsTable()
                            && dependencies.terrain().linkGroundObjectUnderEntity == 0x4C
                            && (((frame.pixelY() + 15) & 15) >= 12))) return false;
        }
        if (!RoomObjectMutations.reachesDefaultGroundPhysics(
                dependencies.terrain().linkGroundObjectPhysics,
                bypassEntry,
                frame.slowWalkingSpeed(),
                frame.itemUsageContext())) return false;
        // Default can return at $02:77E8 while standing on a raised switch block.
        if (linksawakening.world.object.SwitchBlockLinkInteraction.marksStanding(
                dependencies.terrain().linkGroundObjectUnderEntity,
                dependencies.terrain().linkGroundObjectPhysics,
                dependencies.terrain().switchBlocksState)) return false;
        // $02:77F7-77FB gates on wIsIndoor, NOT hIsSideScrolling. Overworld
        // $DF has solid physics ($01), but still exits here without resetting.
        if (!dependencies.overworldCollision().usesIndoorPhysicsTable()) return false;
        // $02:75C4 -> 76C0 -> 7757 can re-enter Default using cached physics.
        // RoomTransitions retain wC1C9 at $02:7802; dialogs reaching $7858 reset it.
        if (frame.roomTransitionActive()) return false;
        // $02:7804-783B returns while an unpressed floor button is sinking.
        if (dependencies.terrain().linkGroundObjectUnderEntity == 0xAA
                && dependencies.puzzles().switchButtonPressed == 0) return false;
        if (dependencies.terrain().linkGroundRoomPosition != dependencies.terrain().linkGroundFinalRoomPosition
                || dependencies.terrain().linkGroundObjectUnderEntity != 0xDF
                || frame.interactionBlocked()
                || dialogGotItemActive
                || frame.dialogActive()) {
            dependencies.terrain().crackedFloorCounter = 0; // $02:7851-785D/$786C
            return false;
        }
        dependencies.terrain().crackedFloorCounter = (dependencies.terrain().crackedFloorCounter + 1) & 0xFF;
        if (dependencies.terrain().crackedFloorCounter < 0x28) return false;
        // label_002_4D97 writes $AE and redraws its tiles and GBC attributes.
        dependencies.location()
                .activeRoom
                .roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + dependencies.terrain().linkGroundRoomPosition] = 0xAE;
        this.refreshActiveRoomTilemap();
        dependencies
                .entityFrame()
                .colorShellSoundSink
                .play(linksawakening.gameplay.GameplaySoundEvent.CRACKED_FLOOR_COLLAPSE);
        return true;
    }

    /** GetObjectUnderLink $02:7512-7536, translated from ROM center/bottom to Java top-left. */
    void sampleLinkGround(int pixelX, int pixelY) {
        int location = RoomObjectMutations.linkGroundLocation(pixelX, pixelY);
        dependencies.terrain().linkGroundRoomPosition = location;
        dependencies.terrain().linkGroundObjectUnderEntity =
                dependencies.roomSessionViews().objectAtRoomLocation(location);
    }

    static int linkGroundLocation(int pixelX, int pixelY) {
        // hLinkPositionX = Java X+8; hLinkPositionY = Java Y+16, then subtract 4.
        // AND $F0 after byte arithmetic also preserves the ROM's wrap behavior.
        return ((pixelY + 12) & 0xF0) | (((pixelX + 8) & 0xF0) >>> 4);
    }

    /** Only the control-flow decision; movement, damage and water effects stay with Link. */
    static boolean reachesDefaultGroundPhysics(
            int physics, boolean bypassEntry, int slowWalkingSpeed, int itemUsageContext) {
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
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                || location < 0
                || (location & 15) >= 10
                || (location >> 4) >= 8
                || dependencies.roomSessionViews().objectAtRoomLocation(location) != 0xDD) return false;
        dependencies.location().activeRoom.roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + location] = 0x0D;
        this.refreshActiveRoomTilemap();
        if (dependencies.entityRuntimeState().runtime != null) {
            // Variant zero uses LiftableRockStartSmashingAnimation ($03:53E4):
            // fifteen frames of rock debris and NOISE_SFX_POT_SMASHED ($09).
            dependencies
                    .entityRuntimeState()
                    .runtime
                    .spawnLiftableRockSmash(((location & 15) << 4) + 8, (location & 0xF0) + 16, 0);
            // Link runs before the entity tick, which clears its frame outbox.
            RoomEntityOutbox.append(
                    dependencies.entityRuntimeState().outbox.pendingRoomEntityEvents,
                    dependencies.entityRuntimeState().runtime.consumePendingEntityEvents());
        }
        return true;
    }

    void applyBombObjectInteractions(List<BombExplosionEvent> events) {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
                || events == null
                || events.isEmpty()) {
            return;
        }

        for (int eventIndex = 0; eventIndex < events.size(); eventIndex++) {
            BombExplosionEvent event = events.get(eventIndex);

            if (!event.targetsRoomObjects()) {
                continue;
            }
            RoomEntity bomb = event.bombSlot()
                            < dependencies
                                    .location()
                                    .activeRoom
                                    .entityView()
                                    .slots()
                                    .size()
                    ? dependencies.location().activeRoom.entityView().slots().get(event.bombSlot())
                    : null;
            if (dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                BombObjectInteraction.Candidate candidate =
                        BombObjectInteraction.basicCandidate(event.bombX(), event.bombVisualY(), event.countdown());
                this.applyBasicBombObjectCandidate(candidate);
                if (bomb != null && bomb.loaded()) {
                    this.applyPuzzleBombObjectCandidate(
                            BombObjectInteraction.puzzleCandidate(event.bombX(), bomb.y(), event.countdown()),
                            event.bombSlot(),
                            bomb.z());
                }
            } else if (bomb != null && bomb.loaded()) {
                BombObjectInteraction.Candidate candidate =
                        BombObjectInteraction.puzzleCandidate(event.bombX(), bomb.y(), event.countdown());
                if (!this.applyIndoorBombableBlockCandidate(candidate)) {
                    this.applyIndoorBombableWallCandidate(candidate, event.bombSlot());
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

        if (result.bushLeavesVisible() && dependencies.entityRuntimeState().runtime != null) {
            int sourceSpriteVariant = result.originalObjectId() == 0x0A ? 0xFF : 0x01;
            dependencies
                    .entityRuntimeState()
                    .runtime
                    .spawnLiftableRockSmash(
                            dependencies.overworldBushInteraction().effectOriginXForLocation(location),
                            dependencies.overworldBushInteraction().effectOriginYForLocation(location),
                            sourceSpriteVariant);
        }
        this.refreshOverworldCollisionAfterObjectMutation();
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
                    .entityRuntimeState()
                    .outbox
                    .pendingRoomEntityEvents
                    .add(new EntityCombatEvent(
                            request.sourceSlot(), 0x01, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
            this.refreshOverworldCollisionAfterObjectMutation();
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
                    && dependencies.roomSessionViews().objectAtRoomLocation(request.location()) == 0x8A) {
                int areaIndex =
                        RoomConstants.ROOM_OBJECTS_BASE + (request.location() & 0xF0) + (request.location() & 0x0F);
                int[] objects = dependencies.location().activeRoom.roomObjectsArea();
                if (areaIndex >= 0 && areaIndex < objects.length) {
                    objects[areaIndex] = 0x04; // OBJECT_SIDE_VIEW_EMPTY
                    if (dependencies.location().activeRoom.renderValues() != null
                            && areaIndex < dependencies.location().activeRoom.renderValues().length) {
                        dependencies.location().activeRoom.renderValues()[areaIndex] = 0x04;
                    }
                    this.refreshActiveRoomTilemap();
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
                    .entityRuntimeState()
                    .outbox
                    .pendingRoomEntityEvents
                    .add(new EntityCombatEvent(
                            request.sourceSlot(), 0x04, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
            if (dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                this.refreshOverworldCollisionAfterObjectMutation();
            }
        }
    }

    void applyMagicPowderObjectInteractions(List<MagicPowderObjectRequest> requests) {
        if (dependencies.location().activeRoom == null || requests == null || requests.isEmpty()) {
            return;
        }

        for (int requestIndex = 0; requestIndex < requests.size(); requestIndex++) {
            MagicPowderObjectRequest request = requests.get(requestIndex);

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
                this.refreshOverworldCollisionAfterObjectMutation();
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
        if (expectedObject >= 0 && dependencies.roomSessionViews().objectAtRoomLocation(location) != expectedObject) {
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
            this.retainTorchTiles(areaIndex);
        }
        objects[areaIndex] = replacementObject & 0xFF;
        if (redraw
                && dependencies.location().activeRoom.renderValues() != null
                && areaIndex < dependencies.location().activeRoom.renderValues().length) {
            dependencies.location().activeRoom.renderValues()[areaIndex] = replacementObject & 0xFF;
        }
        if (redraw) {
            this.refreshActiveRoomTilemap();
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

    void applyPuzzleBombObjectCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot, int sourceZ) {
        if (candidate == null) {
            return;
        }
        int objectId = dependencies.roomSessionViews().objectAtRoomLocation(candidate.location());
        if (objectId < OBJECT_GIANT_SKULL_TOP_LEFT || objectId > OBJECT_GIANT_SKULL_BOTTOM_RIGHT) {
            this.applyOutdoorBombableCaveDoorCandidate(candidate, sourceSlot);
            return;
        }

        int blockTop = candidate.objectTop() & 0xE0;
        int blockLeft = candidate.objectLeft() & 0xE0;
        int[] rubbleXOffsets = {0x18, 0x08, 0x18, 0x08};
        int[] rubbleYOffsets = {0x20, 0x20, 0x10, 0x10};
        if (dependencies.entityRuntimeState().runtime != null) {
            for (int index = 0; index < rubbleXOffsets.length; index++) {
                dependencies
                        .entityRuntimeState()
                        .runtime
                        .spawnLiftableRockRubble(
                                blockLeft + rubbleXOffsets[index], blockTop + rubbleYOffsets[index] - sourceZ);
            }
        }
        this.queuePuzzleSolvedJingle(sourceSlot);

        int blockLocation = blockTop | (blockLeft >>> 4);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                this.writeBombPuzzleObject(
                        blockLocation + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column, OBJECT_ROCKY_GROUND);
            }
        }
        dependencies.roomState()
                .overworldRoomStatus[dependencies.location().activeRoom.roomId()] |= (byte) OW_ROOM_STATUS_OPENED;
        this.refreshOverworldCollisionAfterObjectMutation();
    }

    void applyOutdoorBombableCaveDoorCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) {
        int objectId = dependencies.roomSessionViews().objectAtRoomLocation(candidate.location());
        int doorIndex =
                dependencies.romTables().physicsTables().objectPhysicsFlag(RomTables.PHYSICS_TABLE_OVERWORLD, objectId)
                        - BOMBABLE_WALL_PHYSICS_BASE;
        if (objectId != OBJECT_BOMBABLE_CAVE_DOOR || doorIndex < 0 || doorIndex >= 4) {
            return;
        }

        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
                + (candidate.objectTop() & 0xF0)
                + ((candidate.objectLeft() & 0xF0) >>> 4);
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }

        this.writeBombPuzzleObject(candidate.location(), OBJECT_ROCKY_CAVE_DOOR);
        dependencies.terrain().bombedCaveDoorTileOverrides.add(areaIndex);
        dependencies.roomState()
                .overworldRoomStatus[dependencies.location().activeRoom.roomId()] |= (byte) OW_ROOM_STATUS_OPENED;
        this.queuePuzzleSolvedJingle(sourceSlot);
        this.refreshActiveRoomTilemap();
        this.refreshOverworldCollisionAfterObjectMutation();
    }

    void writeBombPuzzleObject(int location, int objectId) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = objectId & 0xFF;
        if (dependencies.location().activeRoom.renderValues() != null) {
            dependencies.location().activeRoom.renderValues()[areaIndex] = objectId & 0xFF;
        }
        dependencies
                .overworldBushInteraction()
                .refreshRoomObjectCell(
                        dependencies.location().activeRoom.roomId(),
                        location,
                        objects,
                        dependencies.location().activeRoom.renderValues(),
                        dependencies.location().activeRoom.gbcOverlay(),
                        dependencies.location().activeRoom.tileIds(),
                        dependencies.location().activeRoom.tileAttrs());
    }

    void refreshOverworldCollisionAfterObjectMutation() {
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies
                .overworldCollision()
                .setGbcOverlay(dependencies.location().activeRoom.gbcOverlay());
    }

    void applyIndoorBombableWallCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) {
        if (candidate == null) {
            return;
        }
        int location = candidate.location();
        int objectId = dependencies.roomSessionViews().objectAtRoomLocation(location);
        int physicsTable = dependencies.location().activeRoom.mapId() == 0xFF
                ? RomTables.PHYSICS_TABLE_INDOORS2
                : RomTables.PHYSICS_TABLE_INDOORS1;
        int wallIndex = dependencies.romTables().physicsTables().objectPhysicsFlag(physicsTable, objectId)
                - BOMBABLE_WALL_PHYSICS_BASE;
        if (wallIndex < 0 || wallIndex >= 4) {
            return;
        }

        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = wallIndex < 2 ? OBJECT_BOMBED_PASSAGE_VERTICAL : OBJECT_BOMBED_PASSAGE_HORIZONTAL;

        byte[] status = dependencies
                .roomSessionViews()
                .indoorStatusTableForMap(dependencies.location().activeRoom.mapId());
        int currentStatus =
                switch (wallIndex) {
                    case 0 -> 0x04;
                    case 1 -> 0x08;
                    case 2 -> 0x02;
                    case 3 -> 0x01;
                    default -> throw new AssertionError(wallIndex);
                };
        int adjacentStatus =
                switch (wallIndex) {
                    case 0 -> 0x08;
                    case 1 -> 0x04;
                    case 2 -> 0x01;
                    case 3 -> 0x02;
                    default -> throw new AssertionError(wallIndex);
                };
        int adjacentRoom = dependencies.roomSessionViews().adjacentIndoorRoomIdForBombWall(wallIndex);
        status[dependencies.location().activeRoom.roomId()] |= (byte) currentStatus;
        status[adjacentRoom] |= (byte) adjacentStatus;
        dependencies.terrain().bombedWallTileOverrides.put(areaIndex, wallIndex < 2 ? 0 : 1);
        this.queuePuzzleSolvedJingle(sourceSlot);

        this.refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
    }

    void queuePuzzleSolvedJingle(int sourceSlot) {
        dependencies
                .entityRuntimeState()
                .outbox
                .pendingRoomEntityEvents
                .add(new EntityCombatEvent(sourceSlot, 0x02, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x02));
    }

    boolean applyIndoorBombableBlockCandidate(BombObjectInteraction.Candidate candidate) {
        if (candidate == null) {
            return false;
        }
        int location = candidate.location();
        if (dependencies.roomSessionViews().objectAtRoomLocation(location) != OBJECT_BOMBABLE_BLOCK) {
            return false;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return false;
        }

        objects[areaIndex] = OBJECT_FLOOR_OD;
        dependencies.roomSessionViews()
                        .indoorStatusTableForMap(
                                dependencies.location().activeRoom.mapId())[
                        dependencies.location().activeRoom.roomId()] |=
                (byte) INDOOR_ROOM_STATUS_EVENT_3;
        dependencies.terrain().bombedBlockTileOverrides.add(areaIndex);

        if (dependencies.entityRuntimeState().runtime != null) {
            dependencies
                    .entityRuntimeState()
                    .runtime
                    .spawnLiftableRockSmash(candidate.objectLeft() + 0x08, candidate.objectTop() + 0x10, 0x00);
        }

        this.refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
        return true;
    }

    int colorShellObjectAt(RoomEntity entity, int relativeOffset) {
        if (dependencies.location().activeRoom == null || entity == null) {
            return 0xFF;
        }
        int index = RoomInteractionGeometry.colorShellObjectIndex(entity) + relativeOffset;
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        return index >= 0 && index < objects.length ? objects[index] : 0xFF;
    }

    void writeColorShellObject(RoomEntity entity, int objectId) {
        if (dependencies.location().activeRoom == null || entity == null) {
            return;
        }
        int index = RoomInteractionGeometry.colorShellObjectIndex(entity);
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        if (index < 0 || index >= objects.length) {
            return;
        }
        objects[index] = objectId & 0xFF;
        this.refreshActiveRoomTilemap();
    }

    record Dependencies(
            RoomEntitySessionState entityFrame,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            RoomTerrainState terrain,
            RoomEntitySessionState entityRuntimeState,
            IndoorTorchPaletteEffect indoorTorchPaletteEffect,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            RomTables romTables,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            TransientVfxSystem transientVfxSystem,
            linksawakening.rom.Rom romData,
            RoomTilemapBuilder roomTilemapBuilder) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(puzzles, "puzzles");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(indoorTorchPaletteEffect, "indoorTorchPaletteEffect");
            java.util.Objects.requireNonNull(overworldBushInteraction, "overworldBushInteraction");
            java.util.Objects.requireNonNull(overworldCollision, "overworldCollision");
            java.util.Objects.requireNonNull(romTables, "romTables");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
        }
    }

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
            case IGNITE_TORCH -> this.writeMagicPowderTorchObject(request.location(), 0xAB, 0xAC, true);
            case EXTINGUISH_TORCH -> this.writeMagicPowderTorchObject(
                    request.location(),
                    -1,
                    0xAB,
                    dependencies.location().activeRoom.roomId() != 0x74
                            && (dependencies.indoorTorchPaletteEffect().targetAddress() >= 0x0C
                                    || dependencies.entityFrame().currentLinkMotionState != 0x05));
        };
    }

    void refreshActiveRoomTilemap() {
        RoomTilemap tilemap = dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                ? dependencies
                        .roomTilemapBuilder()
                        .buildOverworld(
                                dependencies.location().activeRoom.roomId(),
                                dependencies.location().activeRoom.roomObjectsArea())
                : dependencies
                        .roomTilemapBuilder()
                        .buildIndoor(
                                dependencies.location().activeRoom.mapId(),
                                dependencies.location().activeRoom.roomId(),
                                dependencies.location().activeRoom.roomObjectsArea());
        dependencies.location().activeRoom.replaceTilemap(tilemap.tileIds(), tilemap.tileAttrs());
        this.applyHookshotBridgeTileOverrides();
        this.applyBombedWallTileOverrides();
        this.applyBombedBlockTileOverrides();
        this.applyBombedCaveDoorTileOverrides();
        this.applyRetainedTorchTiles();
        dependencies
                .puzzles()
                .shutterTileOverrides
                .forEach((index, tile) -> dependencies.location().activeRoom.tileIds()[index] = tile);
    }

    void applyHookshotBridgeUpdates(List<HookshotBridgeUpdate> updates) {
        if (dependencies.location().activeRoom == null || updates == null || updates.isEmpty()) {
            return;
        }
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        boolean changed = false;
        for (HookshotBridgeUpdate update : updates) {
            int areaIndex = RoomInteractionGeometry.hookshotObjectAreaIndex(update.objectLeft(), update.objectTop());
            if (areaIndex < 0 || areaIndex >= objects.length) {
                continue;
            }
            HookshotBridgeUpdate previous =
                    dependencies.terrain().hookshotBridgeTileOverrides.put(areaIndex, update);
            if (objects[areaIndex] != 0x9D || !update.equals(previous)) {
                objects[areaIndex] = 0x9D;
                changed = true;
            }
        }
        if (changed) {
            this.refreshActiveRoomTilemap();
        }
    }

    void applyHookshotBridgeTileOverrides() {
        if (dependencies.location().activeRoom == null
                || dependencies.terrain().hookshotBridgeTileOverrides.isEmpty()) {
            return;
        }
        int[] tileIds = dependencies.location().activeRoom.tileIds();
        for (HookshotBridgeUpdate update :
                dependencies.terrain().hookshotBridgeTileOverrides.values()) {
            int tileX = (update.objectLeft() & 0xFF) >>> 3;
            int tileY = (update.objectTop() & 0xFF) >>> 3;
            if (tileX < 0
                    || tileX + 1 >= RoomConstants.ROOM_TILE_WIDTH
                    || tileY < 0
                    || tileY + 1 >= RoomConstants.ROOM_TILE_HEIGHT) {
                continue;
            }
            int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
            int bottomLeft = topLeft + RoomConstants.ROOM_TILE_WIDTH;
            if (update.direction() == HookshotBridgeMotion.PULL_DOWN_DIRECTION) {
                tileIds[topLeft] = 0x04;
                tileIds[topLeft + 1] = 0x05;
                tileIds[bottomLeft] = update.active() ? 0x08 : 0x04;
                tileIds[bottomLeft + 1] = update.active() ? 0x09 : 0x05;
            } else {
                tileIds[topLeft] = update.active() ? 0x0A : 0x04;
                tileIds[topLeft + 1] = update.active() ? 0x0B : 0x05;
                tileIds[bottomLeft] = 0x04;
                tileIds[bottomLeft + 1] = 0x05;
            }
        }
    }

    void applyBombedWallTileOverrides() {
        if (dependencies.location().activeRoom == null
                || dependencies.terrain().bombedWallTileOverrides.isEmpty()) {
            return;
        }
        int[] tileIds = dependencies.location().activeRoom.tileIds();
        for (Map.Entry<Integer, Integer> entry :
                dependencies.terrain().bombedWallTileOverrides.entrySet()) {
            int areaOffset = entry.getKey() - RoomConstants.ROOM_OBJECTS_BASE;
            int objectRow = (areaOffset >>> 4) & 0x0F;
            int objectColumn = areaOffset & 0x0F;
            int tileX = objectColumn * 2;
            int tileY = objectRow * 2;
            if (objectRow >= RoomConstants.OBJECTS_PER_COLUMN
                    || objectColumn >= RoomConstants.OBJECTS_PER_ROW
                    || tileX + 1 >= RoomConstants.ROOM_TILE_WIDTH
                    || tileY + 1 >= RoomConstants.ROOM_TILE_HEIGHT) {
                continue;
            }

            int[] replacement =
                    entry.getValue() == 0 ? new int[] {0x72, 0x72, 0x73, 0x73} : new int[] {0x69, 0x79, 0x69, 0x79};
            int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
            tileIds[topLeft] = replacement[0];
            tileIds[topLeft + 1] = replacement[1];
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH] = replacement[2];
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] = replacement[3];
        }
    }

    void applyBombedBlockTileOverrides() {
        if (dependencies.location().activeRoom == null
                || dependencies.terrain().bombedBlockTileOverrides.isEmpty()) {
            return;
        }
        int[] tileIds = dependencies.location().activeRoom.tileIds();
        for (int areaIndex : dependencies.terrain().bombedBlockTileOverrides) {
            int areaOffset = areaIndex - RoomConstants.ROOM_OBJECTS_BASE;
            int objectRow = (areaOffset >>> 4) & 0x0F;
            int objectColumn = areaOffset & 0x0F;
            int tileX = objectColumn * 2;
            int tileY = objectRow * 2;
            if (objectRow >= RoomConstants.OBJECTS_PER_COLUMN
                    || objectColumn >= RoomConstants.OBJECTS_PER_ROW
                    || tileX + 1 >= RoomConstants.ROOM_TILE_WIDTH
                    || tileY + 1 >= RoomConstants.ROOM_TILE_HEIGHT) {
                continue;
            }

            int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
            tileIds[topLeft] = 0x10;
            tileIds[topLeft + 1] = 0x12;
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH] = 0x11;
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] = 0x13;
        }
    }

    void applyBombedCaveDoorTileOverrides() {
        if (dependencies.location().activeRoom == null
                || dependencies.terrain().bombedCaveDoorTileOverrides.isEmpty()) {
            return;
        }
        int[] tileIds = dependencies.location().activeRoom.tileIds();
        for (int areaIndex : dependencies.terrain().bombedCaveDoorTileOverrides) {
            int areaOffset = areaIndex - RoomConstants.ROOM_OBJECTS_BASE;
            int objectRow = (areaOffset >>> 4) & 0x0F;
            int objectColumn = areaOffset & 0x0F;
            int tileX = objectColumn * 2;
            int tileY = objectRow * 2;
            if (objectRow >= RoomConstants.OBJECTS_PER_COLUMN
                    || objectColumn >= RoomConstants.OBJECTS_PER_ROW
                    || tileX + 1 >= RoomConstants.ROOM_TILE_WIDTH
                    || tileY + 1 >= RoomConstants.ROOM_TILE_HEIGHT) {
                continue;
            }

            RomAddress tileTable = RomSymbols.BombedCaveDoorTilesIndexesGBC;
            int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
            tileIds[topLeft] = dependencies.romData().u8(tileTable);
            tileIds[topLeft + 1] = dependencies.romData().u8(tileTable.plus(1));
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH] =
                    dependencies.romData().u8(tileTable.plus(2));
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] =
                    dependencies.romData().u8(tileTable.plus(3));
        }
    }
}
