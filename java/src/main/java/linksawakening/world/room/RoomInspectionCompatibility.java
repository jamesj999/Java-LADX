package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.entity.event.LiftedEntityState;

interface RoomInspectionCompatibility {
    RoomSession facade();
    /** Current ROM health byte for diagnostic captures. */
    default int entityHealth(int slot) {
        return facade().context.roomSessionViews.entityHealth(slot);
    }

    default ActiveRoom activeRoom() {
        return facade().context.roomSessionViews.activeRoom();
    }

    default boolean hasActiveRoom() {
        return facade().context.roomSessionViews.hasActiveRoom();
    }

    default int currentRoomId() {
        return facade().context.roomSessionViews.currentRoomId();
    }

    default int mapCategory() {
        return facade().context.roomSessionViews.mapCategory();
    }

    default int mapId() {
        return facade().context.roomSessionViews.mapId();
    }
    /**
     * Returns the source {@code wIndoorRoom} value for the active indoor map.
     * A negative result means this map has no ROM layout table, so the caller
     * should preserve the existing save byte rather than inventing one.
     */
    default int indoorRoomPositionForSave() {
        return facade().context.roomSessionViews.indoorRoomPositionForSave();
    }
    /** Room-status bytes in the active dungeon's 8x8 ROM map order. */
    default byte[] dungeonMinimapRoomStatusSnapshot() {
        return facade().context.roomSessionViews.dungeonMinimapRoomStatusSnapshot();
    }

    default byte[] overworldRoomStatusSnapshot() {
        return facade().context.roomSessionViews.overworldRoomStatusSnapshot();
    }

    default byte[] indoorARoomStatusSnapshot() {
        return facade().context.roomSessionViews.indoorARoomStatusSnapshot();
    }

    default byte[] indoorBRoomStatusSnapshot() {
        return facade().context.roomSessionViews.indoorBRoomStatusSnapshot();
    }

    default byte[] colorDungeonRoomStatusSnapshot() {
        return facade().context.roomSessionViews.colorDungeonRoomStatusSnapshot();
    }

    default byte[] dungeonItemFlagsSnapshot() {
        return facade().context.roomSessionViews.dungeonItemFlagsSnapshot();
    }

    default byte[] colorDungeonItemFlagsSnapshot() {
        return facade().context.roomSessionViews.colorDungeonItemFlagsSnapshot();
    }

    default byte[] currentDungeonItemFlagsSnapshot() {
        return facade().context.roomSessionViews.currentDungeonItemFlagsSnapshot();
    }

    default void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags) {
        facade().context.roomSessionViews.restoreDungeonItemFlags(dungeonFlags, colorFlags);
    }
    /** Snapshots wHasInstrument1..wHasInstrument8 (DB65..DB6C). */
    default byte[] dungeonProgressFlagsSnapshot() {
        return facade().context.roomSessionViews.dungeonProgressFlagsSnapshot();
    }

    default void restoreDungeonProgressFlags(byte[] flags) {
        facade().context.roomSessionViews.restoreDungeonProgressFlags(flags);
    }

    default int[][] palettes() {
        return facade().context.roomSessionViews.palettes();
    }

    default boolean heartPiecePickupActive() {
        return facade().context.roomSessionViews.heartPiecePickupActive();
    }

    default String instrumentTrace() {
        return facade().context.roomSessionViews.instrumentTrace();
    }

    default String moblinKingTrace() {
        return facade().context.roomSessionViews.moblinKingTrace();
    }

    default RoomRenderSnapshot renderSnapshot() {
        return facade().context.roomSessionViews.renderSnapshot();
    }

    default LiftedEntityState liftedEntityState() {
        return facade().context.roomSessionViews.liftedEntityState();
    }

    default void setColorShellSoundSink(GameplaySoundSink soundSink) {
        facade().context.roomSessionViews.setColorShellSoundSink(soundSink);
    }
}
