package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.entity.event.LiftedEntityState;

interface RoomInspectionCompatibility {
    RoomSession facade();
    /** Current ROM health byte for diagnostic captures. */
    default int entityHealth(int slot) {
        return facade().components.roomSessionViews().entityHealth(slot);
    }

    default ActiveRoom activeRoom() {
        return facade().components.roomSessionViews().activeRoom();
    }

    default boolean hasActiveRoom() {
        return facade().components.roomSessionViews().hasActiveRoom();
    }

    default int currentRoomId() {
        return facade().components.roomSessionViews().currentRoomId();
    }

    default int mapCategory() {
        return facade().components.roomSessionViews().mapCategory();
    }

    default int mapId() {
        return facade().components.roomSessionViews().mapId();
    }
    /**
     * Returns the source {@code wIndoorRoom} value for the active indoor map.
     * A negative result means this map has no ROM layout table, so the caller
     * should preserve the existing save byte rather than inventing one.
     */
    default int indoorRoomPositionForSave() {
        return facade().components.roomSessionViews().indoorRoomPositionForSave();
    }
    /** Room-status bytes in the active dungeon's 8x8 ROM map order. */
    default byte[] dungeonMinimapRoomStatusSnapshot() {
        return facade().components.roomSessionViews().dungeonMinimapRoomStatusSnapshot();
    }

    default byte[] overworldRoomStatusSnapshot() {
        return facade().components.roomSessionViews().overworldRoomStatusSnapshot();
    }

    default byte[] indoorARoomStatusSnapshot() {
        return facade().components.roomSessionViews().indoorARoomStatusSnapshot();
    }

    default byte[] indoorBRoomStatusSnapshot() {
        return facade().components.roomSessionViews().indoorBRoomStatusSnapshot();
    }

    default byte[] colorDungeonRoomStatusSnapshot() {
        return facade().components.roomSessionViews().colorDungeonRoomStatusSnapshot();
    }

    default byte[] dungeonItemFlagsSnapshot() {
        return facade().components.roomSessionViews().dungeonItemFlagsSnapshot();
    }

    default byte[] colorDungeonItemFlagsSnapshot() {
        return facade().components.roomSessionViews().colorDungeonItemFlagsSnapshot();
    }

    default byte[] currentDungeonItemFlagsSnapshot() {
        return facade().components.roomSessionViews().currentDungeonItemFlagsSnapshot();
    }

    default void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags) {
        facade().components.roomSessionViews().restoreDungeonItemFlags(dungeonFlags, colorFlags);
    }
    /** Snapshots wHasInstrument1..wHasInstrument8 (DB65..DB6C). */
    default byte[] dungeonProgressFlagsSnapshot() {
        return facade().components.roomSessionViews().dungeonProgressFlagsSnapshot();
    }

    default void restoreDungeonProgressFlags(byte[] flags) {
        facade().components.roomSessionViews().restoreDungeonProgressFlags(flags);
    }

    default int[][] palettes() {
        return facade().components.roomSessionViews().palettes();
    }

    default boolean heartPiecePickupActive() {
        return facade().components.roomSessionViews().heartPiecePickupActive();
    }

    default String instrumentTrace() {
        return facade().components.roomSessionViews().instrumentTrace();
    }

    default String moblinKingTrace() {
        return facade().components.roomSessionViews().moblinKingTrace();
    }

    default RoomRenderSnapshot renderSnapshot() {
        return facade().components.roomSessionViews().renderSnapshot();
    }

    default LiftedEntityState liftedEntityState() {
        return facade().components.roomSessionViews().liftedEntityState();
    }

    default void setColorShellSoundSink(GameplaySoundSink soundSink) {
        facade().components.roomSessionViews().setColorShellSoundSink(soundSink);
    }
}
