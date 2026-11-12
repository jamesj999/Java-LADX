package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;
import linksawakening.world.transition.Warp;

final class RoomEventTriggers {
    private final Dependencies dependencies;

    RoomEventTriggers(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void harvestThrownRoomEventCollisions() {
        if (dependencies.entityBridge().get().entityRuntime == null) {
            return;
        }
        for (ThrownBackgroundCollisionEvent collision :
                dependencies.entityBridge().get().entityRuntime.consumePendingThrownBackgroundCollisions()) {
            int objectId = collision.objectId();
            if ((dependencies.puzzles().activeRoomEvent & EVENT_TRIGGER_MASK) == EVENT_TRIGGER_THROW_AT_DOOR
                    && objectId >= 0x35
                    && objectId < 0x3D) {
                dependencies.puzzles().thrownAtDoorTriggerResolved = true;
            }
        }
    }

    void tickRoomEvent(int linkEntityX, int linkEntityY) {
        dependencies.roomPuzzleController().get().tickIndoorKeyDoorAnimation();
        if (dependencies.roomPuzzleController().get().tickShutterClosing()) return;
        dependencies.roomPuzzleController().get().tickSwitchButton(linkEntityX, linkEntityY);
        dependencies.roomPuzzleController().get().tickRoomEventChestReveal(linkEntityX, linkEntityY);
        dependencies.roomPuzzleController().get().tickRoomEventStairReveal();
        if (dependencies.puzzles().pushedBlocksTriggerEffectPending) {
            dependencies.puzzles().pushedBlocksTriggerEffectPending = false;
            return;
        }
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                || dependencies.puzzles().activeRoomEvent == 0) {
            return;
        }
        int effect = dependencies.puzzles().activeRoomEvent & EVENT_EFFECT_MASK;
        // OpenLockedDoorsEffectHandler does not call EventEffectGuard:
        // opening a chest sets bit $10 too, but must not cancel the shutters.
        if (effect != EVENT_EFFECT_OPEN_LOCKED_DOORS
                && effect != EVENT_EFFECT_CLEAR_MIDBOSS
                && (dependencies.roomSessionViews().get().activeRoomStatusFlags() & ROOM_STATUS_EVENT_1) != 0) {
            return;
        }
        int trigger = dependencies.puzzles().activeRoomEvent & EVENT_TRIGGER_MASK;
        if (!dependencies.puzzles().roomEventEffectExecuted
                && ((trigger == EVENT_TRIGGER_KILL_ALL_ENEMIES
                                && dependencies.roomPuzzleController().get().killAllEnemiesTriggerResolved())
                        || (trigger == EVENT_TRIGGER_STEP_ON_BUTTON && dependencies.puzzles().switchButtonPressed != 0)
                        || (trigger == EVENT_TRIGGER_LIGHT_TORCHES && dependencies.puzzles().roomTriggerCount == 0x02)
                        || (trigger == EVENT_TRIGGER_THROW_AT_DOOR
                                && dependencies.puzzles().thrownAtDoorTriggerResolved)
                        || (trigger == EVENT_TRIGGER_KILL_IN_ORDER
                                && dependencies.entityBridge().get().entityRuntime != null
                                && dependencies
                                                .entityBridge()
                                                .get()
                                                .entityRuntime
                                                .inspection()
                                                .killOrderAt(0)
                                        == 0
                                && dependencies
                                                .entityBridge()
                                                .get()
                                                .entityRuntime
                                                .inspection()
                                                .killOrderAt(1)
                                        == 1
                                && dependencies
                                                .entityBridge()
                                                .get()
                                                .entityRuntime
                                                .inspection()
                                                .killOrderAt(2)
                                        == 2))) {
            dependencies.puzzles().roomEventEffectExecuted = true;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        }
        if (!dependencies.puzzles().roomEventEffectExecuted) {
            if ((effect == EVENT_EFFECT_OPEN_LOCKED_DOORS || effect == EVENT_EFFECT_CLEAR_MIDBOSS)
                    && !dependencies.puzzles().shutterCloseStarted
                    && dependencies.puzzles().pendingShutterCloseMask != 0
                    && ((linkEntityX - 0x11) & 255) < 0x7E
                    && ((linkEntityY - 0x16) & 255) < 0x5E) {
                dependencies.puzzles().shutterCloseStarted = true;
                dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_CLOSED);
                dependencies.roomPuzzleController().get().tickShutterClosing();
            }
            return;
        }
        applyRoomEventEffect(effect, linkEntityX, linkEntityY);
    }

    boolean pushedBlocksTriggerResolved() {
        if (dependencies.location().activeRoom == null || dependencies.puzzles().pushedBlockDestinationLocation < 0) {
            return false;
        }
        int neighborLocation =
                switch (dependencies.puzzles().pushedBlockDirection) {
                        // The source checks the object collided with on the block's
                        // movement-facing side; only horizontal pushes can satisfy $07.
                    case 2 -> dependencies.puzzles().pushedBlockDestinationLocation - 1;
                    case 3 -> dependencies.puzzles().pushedBlockDestinationLocation + 1;
                    default -> -1;
                };
        if (neighborLocation < 0) {
            return false;
        }
        int neighbor = dependencies.roomSessionViews().get().objectAtRoomLocation(neighborLocation);
        return neighbor == OBJECT_SETTLED_PUSHED_BLOCK || neighbor == OBJECT_PUSHABLE_BLOCK;
    }

    boolean killAllEnemiesTriggerResolved() {
        for (RoomEntity entity : dependencies.location().activeRoom.entities().slots()) {
            if (entity.status() != EntityStatus.DISABLED
                    && (dependencies.romTables().entityPropertyTables().entityOptions1(entity.type())
                                    & ENTITY_OPTION_EXCLUDED_FROM_KILL_ALL)
                            == 0) {
                return false;
            }
        }
        return true;
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomEntityFrameState entityFrame,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            RomTables romTables,
            java.util.function.Supplier<RoomPuzzleController> roomPuzzleController,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState,
            TransientVfxSystem transientVfxSystem) {}

    private void applyRoomEventEffect(int effect, int linkEntityX, int linkEntityY) {
        if (effect == EVENT_EFFECT_REVEAL_CHEST) {
            dependencies.puzzles().activeRoomEvent = 0;
            dependencies.puzzles().roomEventChestRevealCountdown = ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;
            if (dependencies.transientVfxSystem() != null) {
                dependencies
                        .transientVfxSystem()
                        .spawn(
                                TransientVfxType.CHEST_APPEARS,
                                0x88,
                                RoomInteractionGeometry.roomEventChestTop(linkEntityX, linkEntityY) + 0x10);
            }
        } else if (effect == EVENT_EFFECT_OPEN_LOCKED_DOORS) {
            dependencies.roomPuzzleController().get().openActiveRoomShutterDoors(true);
            dependencies.puzzles().activeRoomEvent = 0;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        } else if (effect == EVENT_EFFECT_DROP_KEY) {
            if (dependencies.entityBridge().get().entityRuntime != null
                    && dependencies.entityBridge().get().entityRuntime.spawnRoomEventKeyDrop() >= 0) {
                dependencies
                        .location()
                        .activeRoom
                        .replaceEntities(dependencies
                                .entityBridge()
                                .get()
                                .entityRuntime
                                .inspection()
                                .snapshot());
            }
            dependencies.puzzles().activeRoomEvent = 0;
        } else if (effect == EVENT_EFFECT_REVEAL_STAIRWAY) {
            dependencies.roomStoryState().get().markActiveRoomCompleted();
            dependencies.puzzles().activeRoomEvent = 0;
            dependencies.puzzles().roomEventStairRevealCountdown = ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;
            if (dependencies.transientVfxSystem() != null) {
                dependencies.transientVfxSystem().spawn(TransientVfxType.STAIRS_APPEARS, 0x88, 0x20);
            }
        } else if (effect == EVENT_EFFECT_CLEAR_MIDBOSS) {
            dependencies.roomPuzzleController().get().openActiveRoomShutterDoors(true);
            if (dependencies.puzzles().activeRoomEvent == EVENT_CLEAR_MIDBOSS
                    && dependencies.location().activeRoom.mapId() >= 0
                    && dependencies.location().activeRoom.mapId()
                            < dependencies.roomState().dungeonProgressFlags.length) {
                dependencies.roomState()
                        .dungeonProgressFlags[dependencies.location().activeRoom.mapId()] |= 0x01;
                dependencies
                        .roomSessionViews()
                        .get()
                        .indoorStatusTableForMap(
                                dependencies.location().activeRoom.mapId())[
                        dependencies.location().activeRoom.roomId()] |= 0x20;
                dependencies.entityBridge().get().entityRuntime.configuration().setDungeonMinibossReady(true);
                dependencies
                        .entityBridge()
                        .get()
                        .outbox
                        .pendingRoomEntityEvents
                        .add(new EntityCombatEvent(0, 0x61, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x1B));
            }
            dependencies.puzzles().activeRoomEvent = 0;
        }
    }
}
