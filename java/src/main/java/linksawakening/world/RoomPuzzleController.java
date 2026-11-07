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
        if (session.context.entityBridge.entityRuntime == null) {
            return;
        }
        for (ThrownBackgroundCollisionEvent collision
                : session.context.entityBridge.entityRuntime.consumePendingThrownBackgroundCollisions()) {
            int objectId = collision.objectId();
            if ((session.context.activeRoomEvent & session.EVENT_TRIGGER_MASK) == session.EVENT_TRIGGER_THROW_AT_DOOR
                && objectId >= 0x35 && objectId < 0x3D) {
                session.context.thrownAtDoorTriggerResolved = true;
            }
        }
    }

    void tickRoomEvent(int linkEntityX, int linkEntityY) {
        session.tickIndoorKeyDoorAnimation();
        if (session.tickShutterClosing()) return;
        session.tickSwitchButton(linkEntityX, linkEntityY);
        session.tickRoomEventChestReveal(linkEntityX, linkEntityY);
        session.tickRoomEventStairReveal();
        if (session.context.pushedBlocksTriggerEffectPending) {
            session.context.pushedBlocksTriggerEffectPending = false;
            return;
        }
        if (session.context.activeRoom == null || session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            || session.context.activeRoomEvent == 0) {
            return;
        }
        int effect = session.context.activeRoomEvent & session.EVENT_EFFECT_MASK;
        // OpenLockedDoorsEffectHandler does not call EventEffectGuard:
        // opening a chest sets bit $10 too, but must not cancel the shutters.
        if (effect != session.EVENT_EFFECT_OPEN_LOCKED_DOORS
            && effect != session.EVENT_EFFECT_CLEAR_MIDBOSS
            && (session.activeRoomStatusFlags() & session.ROOM_STATUS_EVENT_1) != 0) {
            return;
        }
        int trigger = session.context.activeRoomEvent & session.EVENT_TRIGGER_MASK;
        if (!session.context.roomEventEffectExecuted
            && ((trigger == session.EVENT_TRIGGER_KILL_ALL_ENEMIES
                    && session.killAllEnemiesTriggerResolved())
                || (trigger == session.EVENT_TRIGGER_STEP_ON_BUTTON
                    && session.context.switchButtonPressed != 0)
                || (trigger == session.EVENT_TRIGGER_LIGHT_TORCHES
                    && session.context.roomTriggerCount == 0x02)
                || (trigger == session.EVENT_TRIGGER_THROW_AT_DOOR
                    && session.context.thrownAtDoorTriggerResolved)
                || (trigger == session.EVENT_TRIGGER_KILL_IN_ORDER
                    && session.context.entityBridge.entityRuntime != null
                    && session.context.entityBridge.entityRuntime.killOrderAt(0) == 0
                    && session.context.entityBridge.entityRuntime.killOrderAt(1) == 1
                    && session.context.entityBridge.entityRuntime.killOrderAt(2) == 2))) {
            session.context.roomEventEffectExecuted = true;
            session.context.colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        }
        if (!session.context.roomEventEffectExecuted) {
            if ((effect == session.EVENT_EFFECT_OPEN_LOCKED_DOORS || effect == session.EVENT_EFFECT_CLEAR_MIDBOSS)
                && !session.context.shutterCloseStarted && session.context.pendingShutterCloseMask != 0
                && ((linkEntityX - 0x11) & 255) < 0x7E
                && ((linkEntityY - 0x16) & 255) < 0x5E) {
                session.context.shutterCloseStarted = true;
                session.context.colorShellSoundSink.play(GameplaySoundEvent.DOOR_CLOSED);
                session.tickShutterClosing();
            }
            return;
        }
        if (effect == session.EVENT_EFFECT_REVEAL_CHEST) {
            session.context.activeRoomEvent = 0;
            session.context.roomEventChestRevealCountdown = session.ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;
            if (session.context.transientVfxSystem != null) {
                session.context.transientVfxSystem.spawn(TransientVfxType.CHEST_APPEARS, 0x88,
                    session.roomEventChestTop(linkEntityX, linkEntityY) + 0x10);
            }
        } else if (effect == session.EVENT_EFFECT_OPEN_LOCKED_DOORS) {
            session.openActiveRoomShutterDoors(true);
            session.context.activeRoomEvent = 0;
            session.context.colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        } else if (effect == session.EVENT_EFFECT_DROP_KEY) {
            if (session.context.entityBridge.entityRuntime != null && session.context.entityBridge.entityRuntime.spawnRoomEventKeyDrop() >= 0) {
                session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.snapshot());
            }
            session.context.activeRoomEvent = 0;
        } else if (effect == session.EVENT_EFFECT_REVEAL_STAIRWAY) {
            session.markActiveRoomCompleted();
            session.context.activeRoomEvent = 0;
            session.context.roomEventStairRevealCountdown = session.ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;
            if (session.context.transientVfxSystem != null) {
                session.context.transientVfxSystem.spawn(TransientVfxType.STAIRS_APPEARS, 0x88, 0x20);
            }
        } else if (effect == session.EVENT_EFFECT_CLEAR_MIDBOSS) {
            session.openActiveRoomShutterDoors(true);
            if (session.context.activeRoomEvent == session.EVENT_CLEAR_MIDBOSS
                && session.context.activeRoom.mapId() >= 0
                && session.context.activeRoom.mapId() < session.context.roomState.dungeonProgressFlags.length) {
                session.context.roomState.dungeonProgressFlags[session.context.activeRoom.mapId()] |= 0x01;
                session.indoorStatusTableForMap(session.context.activeRoom.mapId())[session.context.activeRoom.roomId()] |= 0x20;
                session.context.entityBridge.entityRuntime.setDungeonMinibossReady(true);
                session.context.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
                    0, 0x61, 0, false,
                    EntityCombatEvent.SoundChannel.JINGLE, 0x1B));
            }
            session.context.activeRoomEvent = 0;
        }
    }

    void tickPushedBlockMotion(int linkEntityX, int linkEntityY) {
        if (session.context.pushedBlockMotionFrames == 0 || session.context.activeRoom == null) {
            return;
        }
        if (session.context.entityInputs.entityDialogActive || session.context.entityInputs.entityInventoryAppearing || session.context.entityInputs.entityDialogCooldown != 0) {
            return;
        }
        session.context.pushedBlockMotionFrames--;
        int elapsedFrames = session.PUSHED_BLOCK_MOTION_FRAMES - session.context.pushedBlockMotionFrames;
        int distance = elapsedFrames >>> 1;
        int sourceX = ((session.context.pushedBlockSourceLocation & 0x0F) << 4) + 0x08;
        int sourceY = (session.context.pushedBlockSourceLocation & 0xF0) + 0x10;
        int x = sourceX;
        int y = sourceY;
        switch (session.context.pushedBlockDirection) {
            case 0 -> y -= distance;
            case 1 -> y += distance;
            case 2 -> x -= distance;
            case 3 -> x += distance;
            default -> {
                session.context.pushedBlockMotionFrames = 0;
                return;
            }
        }
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.entityRuntime.movePushedBlock(session.context.pushedBlockEntitySlot, x, y);
            if (session.context.currentLinkMotionState < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE) {
                session.context.entityBridge.entityRuntime.resolvePushedBlockLinkContact(session.context.pushedBlockEntitySlot, linkEntityX, linkEntityY);
            }
            session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.snapshot());
        }
        if (session.context.pushedBlockMotionFrames != 0) {
            return;
        }
        int destinationIndex = RoomConstants.ROOM_OBJECTS_BASE
            + session.context.pushedBlockDestinationLocation;
        if (destinationIndex >= 0
            && destinationIndex < session.context.activeRoom.roomObjectsArea().length) {
            session.context.activeRoom.roomObjectsArea()[destinationIndex] = session.OBJECT_SETTLED_PUSHED_BLOCK;
        }
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.entityRuntime.removePushedBlock(session.context.pushedBlockEntitySlot);
            session.context.activeRoom.replaceEntities(session.context.entityBridge.entityRuntime.snapshot());
        }
        session.context.pushedBlockEntitySlot = -1;
        session.refreshActiveRoomTilemap();
        session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
        session.context.overworldCollision.setGbcOverlay(null);
        if ((session.context.activeRoomEvent & session.EVENT_TRIGGER_MASK) == session.EVENT_TRIGGER_PUSH_SINGLE_BLOCK) {
            session.context.roomEventEffectExecuted = true;
            session.context.colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        } else if ((session.context.activeRoomEvent & session.EVENT_TRIGGER_MASK) == session.EVENT_TRIGGER_PUSH_BLOCKS
            && session.pushedBlocksTriggerResolved()) {
            session.context.roomEventEffectExecuted = true;
            session.context.pushedBlocksTriggerEffectPending = true;
            session.context.colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        }
    }

    boolean pushedBlocksTriggerResolved() {
        if (session.context.activeRoom == null || session.context.pushedBlockDestinationLocation < 0) {
            return false;
        }
        int neighborLocation = switch (session.context.pushedBlockDirection) {
            // The source checks the object collided with on the block's
            // movement-facing side; only horizontal pushes can satisfy $07.
            case 2 -> session.context.pushedBlockDestinationLocation - 1;
            case 3 -> session.context.pushedBlockDestinationLocation + 1;
            default -> -1;
        };
        if (neighborLocation < 0) {
            return false;
        }
        int neighbor = session.objectAtRoomLocation(neighborLocation);
        return neighbor == session.OBJECT_SETTLED_PUSHED_BLOCK || neighbor == session.OBJECT_PUSHABLE_BLOCK;
    }

    void tickIndoorKeyDoorAnimation() {
        if (session.context.indoorKeyDoorAnimationCountdown == 0 || session.context.indoorKeyDoorDirection < 0
            || session.context.activeRoom == null) {
            return;
        }
        session.context.worldLinkMotionBlockPending = true;
        session.context.indoorKeyDoorAnimationCountdown--;
        if (session.context.indoorKeyDoorAnimationCountdown != 0) {
            return;
        }

        int direction = session.context.indoorKeyDoorDirection;
        session.context.indoorKeyDoorDirection = -1;
        if (session.context.indoorBossDoorOpening) {
            session.replaceBossDoorObjects(session.context.indoorKeyDoorLocation);
            session.context.indoorBossDoorOpening = false;
        } else {
            session.replaceKeyDoorObjects(direction, session.context.indoorKeyDoorLocation);
        }
        session.context.indoorKeyDoorLocation = -1;
        byte[] status = session.indoorStatusTableForMap(session.context.activeRoom.mapId());
        status[session.context.activeRoom.roomId()] |= (byte) session.KEY_DOOR_CURRENT_STATUS[direction];
        int adjacentRoom = session.adjacentIndoorRoomIdForBombWall(direction);
        status[adjacentRoom] |= (byte) session.KEY_DOOR_ADJACENT_STATUS[direction];
        session.refreshActiveRoomTilemap();
        session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
        session.context.overworldCollision.setGbcOverlay(null);
    }

    void replaceKeyDoorObjects(int direction, int location) {
        int firstClosedObject = session.KEY_DOOR_CLOSED_OBJECTS[direction];
        int[] openObjects = session.KEY_DOOR_OPEN_OBJECTS[direction];
        int[] objects = session.context.activeRoom.roomObjectsArea();
        int secondLocation = location + (direction < 2 ? 1 : 0x10);
        int[] locations = {location, secondLocation};
        for (int objectOffset = 0; objectOffset < locations.length; objectOffset++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + locations[objectOffset];
            if (areaIndex >= 0 && areaIndex < objects.length
                && (objects[areaIndex] & 0xFF) == firstClosedObject + objectOffset) {
                objects[areaIndex] = openObjects[objectOffset];
                if (session.context.activeRoom.renderValues() != null) {
                    session.context.activeRoom.renderValues()[areaIndex] = openObjects[objectOffset];
                }
            }
        }
    }

    void replaceBossDoorObjects(int location) {
        int[] objects = session.context.activeRoom.roomObjectsArea();
        int[] openObjects = session.KEY_DOOR_OPEN_OBJECTS[0];
        for (int offset = 0; offset < 2; offset++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location + offset;
            if (areaIndex >= 0 && areaIndex < objects.length
                && objects[areaIndex] == 0xA4 + offset) {
                objects[areaIndex] = openObjects[offset];
                if (session.context.activeRoom.renderValues() != null) {
                    session.context.activeRoom.renderValues()[areaIndex] = openObjects[offset];
                }
            }
        }
    }

    boolean killAllEnemiesTriggerResolved() {
        for (RoomEntity entity : session.context.activeRoom.entities().slots()) {
            if (entity.status() != EntityStatus.DISABLED
                && (session.context.romTables.entityOptions1(entity.type())
                    & session.ENTITY_OPTION_EXCLUDED_FROM_KILL_ALL) == 0) {
                return false;
            }
        }
        return true;
    }

    void tickSwitchButton(int linkEntityX, int linkEntityY) {
        if (session.context.activeRoom == null || session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            || session.context.switchButtonPressed != 0) {
            return;
        }
        int objectUnderLink = session.context.overworldCollision.groundInteractionSample(
            linkEntityX, linkEntityY).objectId();
        if (objectUnderLink != session.OBJECT_SWITCH_BUTTON) {
            session.context.switchButtonPressCounter = 0;
            return;
        }
        session.context.switchButtonPressCounter = (session.context.switchButtonPressCounter + 1) & 0xFF;
        if (session.context.switchButtonPressCounter == session.SWITCH_BUTTON_PRESS_FRAMES) {
            session.context.switchButtonPressed = session.SWITCH_BUTTON_PRESSED;
            session.context.gpu.copyPressedFloorSwitchTiles(session.context.romData);
            session.context.colorShellSoundSink.play(GameplaySoundEvent.FLOOR_SWITCH);
        }
    }

    void tickRoomEventChestReveal(int linkEntityX, int linkEntityY) {
        if (session.context.roomEventChestRevealCountdown == 0) {
            return;
        }
        session.context.roomEventChestRevealCountdown--;
        if (session.context.roomEventChestRevealCountdown != session.ROOM_EVENT_CHEST_WRITE_COUNTDOWN) {
            return;
        }
        int location = session.roomEventChestTop(linkEntityX, linkEntityY) | 0x08;
        session.writeBombPuzzleObject(location, 0xA0);
        session.refreshActiveRoomTilemap();
        session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
    }

    void tickRoomEventStairReveal() {
        if (session.context.roomEventStairRevealCountdown == 0) {
            return;
        }
        session.context.roomEventStairRevealCountdown--;
        if (session.context.roomEventStairRevealCountdown != session.ROOM_EVENT_CHEST_WRITE_COUNTDOWN) {
            return;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + session.EVENT_STAIRWAY_LOCATION;
        session.context.activeRoom.roomObjectsArea()[areaIndex] = session.OBJECT_STAIRS_DOWN;
        session.configureStaircase(session.EVENT_STAIRWAY_LOCATION);
        session.refreshActiveRoomTilemap();
        session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
        session.context.overworldCollision.setGbcOverlay(null);
    }

    boolean tickShutterClosing() {
        if (!session.context.shutterCloseStarted || session.context.activeRoom == null) return false;
        if (session.context.shutterCloseDirection < 0) {
            if (session.context.pendingShutterCloseMask == 0) return false;
            session.context.shutterCloseDirection = Integer.numberOfTrailingZeros(session.context.pendingShutterCloseMask);
            session.context.pendingShutterCloseMask &= ~(1 << session.context.shutterCloseDirection);
            session.context.shutterCloseLocation = new RoomObjectParser(session.context.romData).indoorShutterLocation(
                session.context.activeRoom.roomObjectsArea(), session.context.shutterCloseDirection);
            session.context.shutterCloseTicks = 0;
            return true;
        }
        session.context.worldLinkMotionBlockPending = true;
        if (session.context.shutterCloseLocation >= 0) {
            int direction = session.context.shutterCloseDirection;
            int left = (session.context.shutterCloseLocation & 15) * 16
                + Byte.toUnsignedInt(session.context.romData[RomBank.romOffset(2, 0x5BE0 + direction + 4)]);
            int top = (session.context.shutterCloseLocation >>> 4) * 16;
            int source = RomBank.romOffset(2, 0x5BC4 + direction * 4);
            int rowOffset = RomBank.romOffset(2, 0x5BE8 + direction + 4);
            for (int row = 0; row < 2; row++) {
                int y = top + Byte.toUnsignedInt(session.context.romData[rowOffset + row * 4]);
                int index = (y / 8) * RoomConstants.ROOM_TILE_WIDTH + left / 8;
                for (int col = 0; col < 2; col++) {
                    int tile = Byte.toUnsignedInt(session.context.romData[source + row * 2 + col]);
                    if (index + col < session.context.activeRoom.tileIds().length) {
                        session.context.activeRoom.tileIds()[index + col] = tile;
                        session.context.shutterTileOverrides.put(index + col, tile);
                    }
                }
            }
        }
        if (++session.context.shutterCloseTicks < 8) return true;
        int direction = session.context.shutterCloseDirection;
        session.context.shutterTileOverrides.clear();
        new RoomObjectParser(session.context.romData).applyIndoorShutterClosedState(
            session.context.activeRoom.roomObjectsArea(), 1 << direction);
        session.context.closedShutterMask |= 1 << direction;
        // $02:5D21 uses indoor A even for indoor B (the original's bank selection).
        byte[] status = session.context.activeRoom.mapId() == 0xFF ? session.context.roomState.colorDungeonRoomStatus : session.context.roomState.indoorARoomStatus;
        status[session.context.activeRoom.roomId()] &= (byte) ~session.KEY_DOOR_CURRENT_STATUS[direction];
        session.context.shutterCloseDirection = session.context.shutterCloseLocation = -1;
        session.refreshActiveRoomTilemap();
        session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
        return true;
    }

    void openActiveRoomShutterDoors() {
        session.openActiveRoomShutterDoors(false);
    }

    void openActiveRoomShutterDoors(boolean persistDirectionalStatus) {
        session.context.pendingShutterCloseMask = session.context.closedShutterMask = 0;
        session.context.shutterCloseDirection = session.context.shutterCloseLocation = -1;
        session.context.shutterTileOverrides.clear();
        if (session.context.activeRoom == null) {
            return;
        }
        int shutterMask = session.context.activeRoom.shutterDoorMask();
        if (shutterMask != 0 && session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            RoomObjectParser parser = new RoomObjectParser(session.context.romData);
            int[] openDoorTypes = {0xF4, 0xF5, 0xF6, 0xF7};
            for (int direction = 0; direction < openDoorTypes.length; direction++) {
                if ((shutterMask & (1 << direction)) != 0) {
                    int location = parser.indoorShutterLocation(
                        session.context.activeRoom.roomObjectsArea(), direction);
                    if (location < 0) {
                        continue;
                    }
                    parser.applyIndoorDoorMacro(
                        session.context.activeRoom.roomObjectsArea(), openDoorTypes[direction],
                        location);
                    if (persistDirectionalStatus) {
                        byte[] status = session.indoorStatusTableForMap(session.context.activeRoom.mapId());
                        status[session.context.activeRoom.roomId()]
                            |= (byte) session.KEY_DOOR_CURRENT_STATUS[direction];
                        int adjacentRoom = session.adjacentIndoorRoomIdForBombWall(direction);
                        status[adjacentRoom]
                            |= (byte) session.KEY_DOOR_ADJACENT_STATUS[direction];
                    }
                }
            }
            session.refreshActiveRoomTilemap();
            session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
            session.context.overworldCollision.setGbcOverlay(null);
        }
        session.context.activeRoom.openShutterDoors();
    }
}
