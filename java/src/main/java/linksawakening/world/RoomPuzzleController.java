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

/** RoomPuzzleController operations for the live room session. */
final class RoomPuzzleController {
    private final RoomSession session;
    RoomPuzzleController(RoomSession session) { this.session = session; }

    void harvestThrownRoomEventCollisions() {
        if (session.entityBridge.entityRuntime == null) {
            return;
        }
        for (ThrownBackgroundCollisionEvent collision
                : session.entityBridge.entityRuntime.consumePendingThrownBackgroundCollisions()) {
            int objectId = collision.objectId();
            if ((session.activeRoomEvent & session.EVENT_TRIGGER_MASK) == session.EVENT_TRIGGER_THROW_AT_DOOR
                && objectId >= 0x35 && objectId < 0x3D) {
                session.thrownAtDoorTriggerResolved = true;
            }
        }
    }

    void tickRoomEvent(int linkEntityX, int linkEntityY) {
        session.tickIndoorKeyDoorAnimation();
        if (session.tickShutterClosing()) return;
        session.tickSwitchButton(linkEntityX, linkEntityY);
        session.tickRoomEventChestReveal(linkEntityX, linkEntityY);
        session.tickRoomEventStairReveal();
        if (session.pushedBlocksTriggerEffectPending) {
            session.pushedBlocksTriggerEffectPending = false;
            return;
        }
        if (session.activeRoom == null || session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            || session.activeRoomEvent == 0) {
            return;
        }
        int effect = session.activeRoomEvent & session.EVENT_EFFECT_MASK;
        // OpenLockedDoorsEffectHandler does not call EventEffectGuard:
        // opening a chest sets bit $10 too, but must not cancel the shutters.
        if (effect != session.EVENT_EFFECT_OPEN_LOCKED_DOORS
            && effect != session.EVENT_EFFECT_CLEAR_MIDBOSS
            && (session.activeRoomStatusFlags() & session.ROOM_STATUS_EVENT_1) != 0) {
            return;
        }
        int trigger = session.activeRoomEvent & session.EVENT_TRIGGER_MASK;
        if (!session.roomEventEffectExecuted
            && ((trigger == session.EVENT_TRIGGER_KILL_ALL_ENEMIES
                    && session.killAllEnemiesTriggerResolved())
                || (trigger == session.EVENT_TRIGGER_STEP_ON_BUTTON
                    && session.switchButtonPressed != 0)
                || (trigger == session.EVENT_TRIGGER_LIGHT_TORCHES
                    && session.roomTriggerCount == 0x02)
                || (trigger == session.EVENT_TRIGGER_THROW_AT_DOOR
                    && session.thrownAtDoorTriggerResolved)
                || (trigger == session.EVENT_TRIGGER_KILL_IN_ORDER
                    && session.entityBridge.entityRuntime != null
                    && session.entityBridge.entityRuntime.killOrderAt(0) == 0
                    && session.entityBridge.entityRuntime.killOrderAt(1) == 1
                    && session.entityBridge.entityRuntime.killOrderAt(2) == 2))) {
            session.roomEventEffectExecuted = true;
            session.colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        }
        if (!session.roomEventEffectExecuted) {
            if ((effect == session.EVENT_EFFECT_OPEN_LOCKED_DOORS || effect == session.EVENT_EFFECT_CLEAR_MIDBOSS)
                && !session.shutterCloseStarted && session.pendingShutterCloseMask != 0
                && ((linkEntityX - 0x11) & 255) < 0x7E
                && ((linkEntityY - 0x16) & 255) < 0x5E) {
                session.shutterCloseStarted = true;
                session.colorShellSoundSink.play(GameplaySoundEvent.DOOR_CLOSED);
                session.tickShutterClosing();
            }
            return;
        }
        if (effect == session.EVENT_EFFECT_REVEAL_CHEST) {
            session.activeRoomEvent = 0;
            session.roomEventChestRevealCountdown = session.ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;
            if (session.transientVfxSystem != null) {
                session.transientVfxSystem.spawn(TransientVfxType.CHEST_APPEARS, 0x88,
                    session.roomEventChestTop(linkEntityX, linkEntityY) + 0x10);
            }
        } else if (effect == session.EVENT_EFFECT_OPEN_LOCKED_DOORS) {
            session.openActiveRoomShutterDoors(true);
            session.activeRoomEvent = 0;
            session.colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        } else if (effect == session.EVENT_EFFECT_DROP_KEY) {
            if (session.entityBridge.entityRuntime != null && session.entityBridge.entityRuntime.spawnRoomEventKeyDrop() >= 0) {
                session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
            }
            session.activeRoomEvent = 0;
        } else if (effect == session.EVENT_EFFECT_REVEAL_STAIRWAY) {
            session.markActiveRoomCompleted();
            session.activeRoomEvent = 0;
            session.roomEventStairRevealCountdown = session.ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;
            if (session.transientVfxSystem != null) {
                session.transientVfxSystem.spawn(TransientVfxType.STAIRS_APPEARS, 0x88, 0x20);
            }
        } else if (effect == session.EVENT_EFFECT_CLEAR_MIDBOSS) {
            session.openActiveRoomShutterDoors(true);
            if (session.activeRoomEvent == session.EVENT_CLEAR_MIDBOSS
                && session.activeRoom.mapId() >= 0
                && session.activeRoom.mapId() < session.roomState.dungeonProgressFlags.length) {
                session.roomState.dungeonProgressFlags[session.activeRoom.mapId()] |= 0x01;
                session.indoorStatusTableForMap(session.activeRoom.mapId())[session.activeRoom.roomId()] |= 0x20;
                session.entityBridge.entityRuntime.setDungeonMinibossReady(true);
                session.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
                    0, 0x61, 0, false,
                    EntityCombatEvent.SoundChannel.JINGLE, 0x1B));
            }
            session.activeRoomEvent = 0;
        }
    }

    void tickPushedBlockMotion(int linkEntityX, int linkEntityY) {
        if (session.pushedBlockMotionFrames == 0 || session.activeRoom == null) {
            return;
        }
        if (session.entityInputs.entityDialogActive || session.entityInputs.entityInventoryAppearing || session.entityInputs.entityDialogCooldown != 0) {
            return;
        }
        session.pushedBlockMotionFrames--;
        int elapsedFrames = session.PUSHED_BLOCK_MOTION_FRAMES - session.pushedBlockMotionFrames;
        int distance = elapsedFrames >>> 1;
        int sourceX = ((session.pushedBlockSourceLocation & 0x0F) << 4) + 0x08;
        int sourceY = (session.pushedBlockSourceLocation & 0xF0) + 0x10;
        int x = sourceX;
        int y = sourceY;
        switch (session.pushedBlockDirection) {
            case 0 -> y -= distance;
            case 1 -> y += distance;
            case 2 -> x -= distance;
            case 3 -> x += distance;
            default -> {
                session.pushedBlockMotionFrames = 0;
                return;
            }
        }
        if (session.entityBridge.entityRuntime != null) {
            session.entityBridge.entityRuntime.movePushedBlock(session.pushedBlockEntitySlot, x, y);
            if (session.currentLinkMotionState < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE) {
                session.entityBridge.entityRuntime.resolvePushedBlockLinkContact(session.pushedBlockEntitySlot, linkEntityX, linkEntityY);
            }
            session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        }
        if (session.pushedBlockMotionFrames != 0) {
            return;
        }
        int destinationIndex = RoomConstants.ROOM_OBJECTS_BASE
            + session.pushedBlockDestinationLocation;
        if (destinationIndex >= 0
            && destinationIndex < session.activeRoom.roomObjectsArea().length) {
            session.activeRoom.roomObjectsArea()[destinationIndex] = session.OBJECT_SETTLED_PUSHED_BLOCK;
        }
        if (session.entityBridge.entityRuntime != null) {
            session.entityBridge.entityRuntime.removePushedBlock(session.pushedBlockEntitySlot);
            session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        }
        session.pushedBlockEntitySlot = -1;
        session.refreshActiveRoomTilemap();
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        session.overworldCollision.setGbcOverlay(null);
        if ((session.activeRoomEvent & session.EVENT_TRIGGER_MASK) == session.EVENT_TRIGGER_PUSH_SINGLE_BLOCK) {
            session.roomEventEffectExecuted = true;
            session.colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        } else if ((session.activeRoomEvent & session.EVENT_TRIGGER_MASK) == session.EVENT_TRIGGER_PUSH_BLOCKS
            && session.pushedBlocksTriggerResolved()) {
            session.roomEventEffectExecuted = true;
            session.pushedBlocksTriggerEffectPending = true;
            session.colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        }
    }

    boolean pushedBlocksTriggerResolved() {
        if (session.activeRoom == null || session.pushedBlockDestinationLocation < 0) {
            return false;
        }
        int neighborLocation = switch (session.pushedBlockDirection) {
            // The source checks the object collided with on the block's
            // movement-facing side; only horizontal pushes can satisfy $07.
            case 2 -> session.pushedBlockDestinationLocation - 1;
            case 3 -> session.pushedBlockDestinationLocation + 1;
            default -> -1;
        };
        if (neighborLocation < 0) {
            return false;
        }
        int neighbor = session.objectAtRoomLocation(neighborLocation);
        return neighbor == session.OBJECT_SETTLED_PUSHED_BLOCK || neighbor == session.OBJECT_PUSHABLE_BLOCK;
    }

    void tickIndoorKeyDoorAnimation() {
        if (session.indoorKeyDoorAnimationCountdown == 0 || session.indoorKeyDoorDirection < 0
            || session.activeRoom == null) {
            return;
        }
        session.worldLinkMotionBlockPending = true;
        session.indoorKeyDoorAnimationCountdown--;
        if (session.indoorKeyDoorAnimationCountdown != 0) {
            return;
        }

        int direction = session.indoorKeyDoorDirection;
        session.indoorKeyDoorDirection = -1;
        if (session.indoorBossDoorOpening) {
            session.replaceBossDoorObjects(session.indoorKeyDoorLocation);
            session.indoorBossDoorOpening = false;
        } else {
            session.replaceKeyDoorObjects(direction, session.indoorKeyDoorLocation);
        }
        session.indoorKeyDoorLocation = -1;
        byte[] status = session.indoorStatusTableForMap(session.activeRoom.mapId());
        status[session.activeRoom.roomId()] |= (byte) session.KEY_DOOR_CURRENT_STATUS[direction];
        int adjacentRoom = session.adjacentIndoorRoomIdForBombWall(direction);
        status[adjacentRoom] |= (byte) session.KEY_DOOR_ADJACENT_STATUS[direction];
        session.refreshActiveRoomTilemap();
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        session.overworldCollision.setGbcOverlay(null);
    }

    void replaceKeyDoorObjects(int direction, int location) {
        int firstClosedObject = session.KEY_DOOR_CLOSED_OBJECTS[direction];
        int[] openObjects = session.KEY_DOOR_OPEN_OBJECTS[direction];
        int[] objects = session.activeRoom.roomObjectsArea();
        int secondLocation = location + (direction < 2 ? 1 : 0x10);
        int[] locations = {location, secondLocation};
        for (int objectOffset = 0; objectOffset < locations.length; objectOffset++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + locations[objectOffset];
            if (areaIndex >= 0 && areaIndex < objects.length
                && (objects[areaIndex] & 0xFF) == firstClosedObject + objectOffset) {
                objects[areaIndex] = openObjects[objectOffset];
                if (session.activeRoom.renderValues() != null) {
                    session.activeRoom.renderValues()[areaIndex] = openObjects[objectOffset];
                }
            }
        }
    }

    void replaceBossDoorObjects(int location) {
        int[] objects = session.activeRoom.roomObjectsArea();
        int[] openObjects = session.KEY_DOOR_OPEN_OBJECTS[0];
        for (int offset = 0; offset < 2; offset++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location + offset;
            if (areaIndex >= 0 && areaIndex < objects.length
                && objects[areaIndex] == 0xA4 + offset) {
                objects[areaIndex] = openObjects[offset];
                if (session.activeRoom.renderValues() != null) {
                    session.activeRoom.renderValues()[areaIndex] = openObjects[offset];
                }
            }
        }
    }

    boolean killAllEnemiesTriggerResolved() {
        for (RoomEntity entity : session.activeRoom.entities().slots()) {
            if (entity.status() != EntityStatus.DISABLED
                && (session.romTables.entityOptions1(entity.type())
                    & session.ENTITY_OPTION_EXCLUDED_FROM_KILL_ALL) == 0) {
                return false;
            }
        }
        return true;
    }

    void tickSwitchButton(int linkEntityX, int linkEntityY) {
        if (session.activeRoom == null || session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            || session.switchButtonPressed != 0) {
            return;
        }
        int objectUnderLink = session.overworldCollision.groundInteractionSample(
            linkEntityX, linkEntityY).objectId();
        if (objectUnderLink != session.OBJECT_SWITCH_BUTTON) {
            session.switchButtonPressCounter = 0;
            return;
        }
        session.switchButtonPressCounter = (session.switchButtonPressCounter + 1) & 0xFF;
        if (session.switchButtonPressCounter == session.SWITCH_BUTTON_PRESS_FRAMES) {
            session.switchButtonPressed = session.SWITCH_BUTTON_PRESSED;
            session.gpu.copyPressedFloorSwitchTiles(session.romData);
            session.colorShellSoundSink.play(GameplaySoundEvent.FLOOR_SWITCH);
        }
    }

    void tickRoomEventChestReveal(int linkEntityX, int linkEntityY) {
        if (session.roomEventChestRevealCountdown == 0) {
            return;
        }
        session.roomEventChestRevealCountdown--;
        if (session.roomEventChestRevealCountdown != session.ROOM_EVENT_CHEST_WRITE_COUNTDOWN) {
            return;
        }
        int location = session.roomEventChestTop(linkEntityX, linkEntityY) | 0x08;
        session.writeBombPuzzleObject(location, 0xA0);
        session.refreshActiveRoomTilemap();
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
    }

    void tickRoomEventStairReveal() {
        if (session.roomEventStairRevealCountdown == 0) {
            return;
        }
        session.roomEventStairRevealCountdown--;
        if (session.roomEventStairRevealCountdown != session.ROOM_EVENT_CHEST_WRITE_COUNTDOWN) {
            return;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + session.EVENT_STAIRWAY_LOCATION;
        session.activeRoom.roomObjectsArea()[areaIndex] = session.OBJECT_STAIRS_DOWN;
        session.configureStaircase(session.EVENT_STAIRWAY_LOCATION);
        session.refreshActiveRoomTilemap();
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        session.overworldCollision.setGbcOverlay(null);
    }

    boolean tickShutterClosing() {
        if (!session.shutterCloseStarted || session.activeRoom == null) return false;
        if (session.shutterCloseDirection < 0) {
            if (session.pendingShutterCloseMask == 0) return false;
            session.shutterCloseDirection = Integer.numberOfTrailingZeros(session.pendingShutterCloseMask);
            session.pendingShutterCloseMask &= ~(1 << session.shutterCloseDirection);
            session.shutterCloseLocation = new RoomObjectParser(session.romData).indoorShutterLocation(
                session.activeRoom.roomObjectsArea(), session.shutterCloseDirection);
            session.shutterCloseTicks = 0;
            return true;
        }
        session.worldLinkMotionBlockPending = true;
        if (session.shutterCloseLocation >= 0) {
            int direction = session.shutterCloseDirection;
            int left = (session.shutterCloseLocation & 15) * 16
                + Byte.toUnsignedInt(session.romData[RomBank.romOffset(2, 0x5BE0 + direction + 4)]);
            int top = (session.shutterCloseLocation >>> 4) * 16;
            int source = RomBank.romOffset(2, 0x5BC4 + direction * 4);
            int rowOffset = RomBank.romOffset(2, 0x5BE8 + direction + 4);
            for (int row = 0; row < 2; row++) {
                int y = top + Byte.toUnsignedInt(session.romData[rowOffset + row * 4]);
                int index = (y / 8) * RoomConstants.ROOM_TILE_WIDTH + left / 8;
                for (int col = 0; col < 2; col++) {
                    int tile = Byte.toUnsignedInt(session.romData[source + row * 2 + col]);
                    if (index + col < session.activeRoom.tileIds().length) {
                        session.activeRoom.tileIds()[index + col] = tile;
                        session.shutterTileOverrides.put(index + col, tile);
                    }
                }
            }
        }
        if (++session.shutterCloseTicks < 8) return true;
        int direction = session.shutterCloseDirection;
        session.shutterTileOverrides.clear();
        new RoomObjectParser(session.romData).applyIndoorShutterClosedState(
            session.activeRoom.roomObjectsArea(), 1 << direction);
        session.closedShutterMask |= 1 << direction;
        // $02:5D21 uses indoor A even for indoor B (the original's bank selection).
        byte[] status = session.activeRoom.mapId() == 0xFF ? session.roomState.colorDungeonRoomStatus : session.roomState.indoorARoomStatus;
        status[session.activeRoom.roomId()] &= (byte) ~session.KEY_DOOR_CURRENT_STATUS[direction];
        session.shutterCloseDirection = session.shutterCloseLocation = -1;
        session.refreshActiveRoomTilemap();
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        return true;
    }

    void openActiveRoomShutterDoors() {
        session.openActiveRoomShutterDoors(false);
    }

    void openActiveRoomShutterDoors(boolean persistDirectionalStatus) {
        session.pendingShutterCloseMask = session.closedShutterMask = 0;
        session.shutterCloseDirection = session.shutterCloseLocation = -1;
        session.shutterTileOverrides.clear();
        if (session.activeRoom == null) {
            return;
        }
        int shutterMask = session.activeRoom.shutterDoorMask();
        if (shutterMask != 0 && session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            RoomObjectParser parser = new RoomObjectParser(session.romData);
            int[] openDoorTypes = {0xF4, 0xF5, 0xF6, 0xF7};
            for (int direction = 0; direction < openDoorTypes.length; direction++) {
                if ((shutterMask & (1 << direction)) != 0) {
                    int location = parser.indoorShutterLocation(
                        session.activeRoom.roomObjectsArea(), direction);
                    if (location < 0) {
                        continue;
                    }
                    parser.applyIndoorDoorMacro(
                        session.activeRoom.roomObjectsArea(), openDoorTypes[direction],
                        location);
                    if (persistDirectionalStatus) {
                        byte[] status = session.indoorStatusTableForMap(session.activeRoom.mapId());
                        status[session.activeRoom.roomId()]
                            |= (byte) session.KEY_DOOR_CURRENT_STATUS[direction];
                        int adjacentRoom = session.adjacentIndoorRoomIdForBombWall(direction);
                        status[adjacentRoom]
                            |= (byte) session.KEY_DOOR_ADJACENT_STATUS[direction];
                    }
                }
            }
            session.refreshActiveRoomTilemap();
            session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
            session.overworldCollision.setGbcOverlay(null);
        }
        session.activeRoom.openShutterDoors();
    }
}
