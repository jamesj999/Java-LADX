package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.entity.event.LiftedEntityState;

public interface RoomInspectionCompatibility {
    RoomSession facade();
    /** @deprecated Use {@code entityState().entityHealth()}. */
    @Deprecated(forRemoval = true)
    default int entityHealth(int slot) {
        return facade().entityState().entityHealth(slot);
    }

    /** @deprecated Use {@code rooms().activeRoom()}. */
    @Deprecated(forRemoval = true)
    default ActiveRoom activeRoom() {
        return facade().rooms().activeRoom();
    }

    /** @deprecated Use {@code rooms().hasActiveRoom()}. */
    @Deprecated(forRemoval = true)
    default boolean hasActiveRoom() {
        return facade().rooms().hasActiveRoom();
    }

    /** @deprecated Use {@code rooms().currentRoomId()}. */
    @Deprecated(forRemoval = true)
    default int currentRoomId() {
        return facade().rooms().currentRoomId();
    }

    /** @deprecated Use {@code rooms().mapCategory()}. */
    @Deprecated(forRemoval = true)
    default int mapCategory() {
        return facade().rooms().mapCategory();
    }

    /** @deprecated Use {@code rooms().mapId()}. */
    @Deprecated(forRemoval = true)
    default int mapId() {
        return facade().rooms().mapId();
    }
    /** @deprecated Use {@code rooms().indoorRoomPositionForSave()}. */
    @Deprecated(forRemoval = true)
    default int indoorRoomPositionForSave() {
        return facade().rooms().indoorRoomPositionForSave();
    }
    /** @deprecated Use {@code dungeon().dungeonMinimapRoomStatusSnapshot()}. */
    @Deprecated(forRemoval = true)
    default byte[] dungeonMinimapRoomStatusSnapshot() {
        return facade().dungeon().dungeonMinimapRoomStatusSnapshot();
    }

    /** @deprecated Use {@code dungeon().overworldRoomStatusSnapshot()}. */
    @Deprecated(forRemoval = true)
    default byte[] overworldRoomStatusSnapshot() {
        return facade().dungeon().overworldRoomStatusSnapshot();
    }

    /** @deprecated Use {@code dungeon().indoorARoomStatusSnapshot()}. */
    @Deprecated(forRemoval = true)
    default byte[] indoorARoomStatusSnapshot() {
        return facade().dungeon().indoorARoomStatusSnapshot();
    }

    /** @deprecated Use {@code dungeon().indoorBRoomStatusSnapshot()}. */
    @Deprecated(forRemoval = true)
    default byte[] indoorBRoomStatusSnapshot() {
        return facade().dungeon().indoorBRoomStatusSnapshot();
    }

    /** @deprecated Use {@code dungeon().colorDungeonRoomStatusSnapshot()}. */
    @Deprecated(forRemoval = true)
    default byte[] colorDungeonRoomStatusSnapshot() {
        return facade().dungeon().colorDungeonRoomStatusSnapshot();
    }

    /** @deprecated Use {@code dungeon().dungeonItemFlagsSnapshot()}. */
    @Deprecated(forRemoval = true)
    default byte[] dungeonItemFlagsSnapshot() {
        return facade().dungeon().dungeonItemFlagsSnapshot();
    }

    /** @deprecated Use {@code dungeon().colorDungeonItemFlagsSnapshot()}. */
    @Deprecated(forRemoval = true)
    default byte[] colorDungeonItemFlagsSnapshot() {
        return facade().dungeon().colorDungeonItemFlagsSnapshot();
    }

    /** @deprecated Use {@code dungeon().currentDungeonItemFlagsSnapshot()}. */
    @Deprecated(forRemoval = true)
    default byte[] currentDungeonItemFlagsSnapshot() {
        return facade().dungeon().currentDungeonItemFlagsSnapshot();
    }

    /** @deprecated Use {@code dungeon().restoreDungeonItemFlags()}. */
    @Deprecated(forRemoval = true)
    default void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags) {
        facade().dungeon().restoreDungeonItemFlags(dungeonFlags, colorFlags);
    }
    /** @deprecated Use {@code dungeon().dungeonProgressFlagsSnapshot()}. */
    @Deprecated(forRemoval = true)
    default byte[] dungeonProgressFlagsSnapshot() {
        return facade().dungeon().dungeonProgressFlagsSnapshot();
    }

    /** @deprecated Use {@code dungeon().restoreDungeonProgressFlags()}. */
    @Deprecated(forRemoval = true)
    default void restoreDungeonProgressFlags(byte[] flags) {
        facade().dungeon().restoreDungeonProgressFlags(flags);
    }

    /** @deprecated Use {@code rooms().palettes()}. */
    @Deprecated(forRemoval = true)
    default int[][] palettes() {
        return facade().rooms().palettes();
    }

    /** @deprecated Use {@code rooms().heartPiecePickupActive()}. */
    @Deprecated(forRemoval = true)
    default boolean heartPiecePickupActive() {
        return facade().rooms().heartPiecePickupActive();
    }

    /** @deprecated Use {@code rooms().instrumentTrace()}. */
    @Deprecated(forRemoval = true)
    default String instrumentTrace() {
        return facade().rooms().instrumentTrace();
    }

    /** @deprecated Use {@code rooms().moblinKingTrace()}. */
    @Deprecated(forRemoval = true)
    default String moblinKingTrace() {
        return facade().rooms().moblinKingTrace();
    }

    /** @deprecated Use {@code rooms().renderSnapshot()}. */
    @Deprecated(forRemoval = true)
    default RoomRenderSnapshot renderSnapshot() {
        return facade().rooms().renderSnapshot();
    }

    /** @deprecated Use {@code rooms().liftedEntityState()}. */
    @Deprecated(forRemoval = true)
    default LiftedEntityState liftedEntityState() {
        return facade().rooms().liftedEntityState();
    }

    /** @deprecated Use {@code entityState().setColorShellSoundSink()}. */
    @Deprecated(forRemoval = true)
    default void setColorShellSoundSink(GameplaySoundSink soundSink) {
        facade().entityState().setColorShellSoundSink(soundSink);
    }
}
