package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.GPU;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.transition.Warp;

final class RoomActiveRoomInitializer {
    private final Dependencies dependencies;

    RoomActiveRoomInitializer(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void setActiveRoom(LoadedRoom room) {
        dependencies.roomLoadCoordinator().get().setActiveRoom(room, null);
    }

    void setActiveRoom(LoadedRoom room, int[][] objectPalettes) {
        dependencies.entityInputs().get().secretSeashellScreenShakeActive = false;
        dependencies.entityInputs().get().secretSeashellPegasusCollisionActive = false;
        dependencies.entityInputs().get().secretSeashellPegasusCollisionX = 0;
        dependencies.entityInputs().get().secretSeashellPegasusCollisionY = 0;
        RoomEntitySnapshot entities = prepareEntities(room, objectPalettes);
        dependencies.location().activeRoom = ActiveRoom.from(room, entities);
        configureDoorAndEventState();
        dependencies.entityBridge().get().entityRuntime = entities == null
                ? null
                : RoomEntityRuntime.builder(entities)
                        .indoorRoom(dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD)
                        .randomByteSupplier(dependencies.entityRandomByteSource())
                        .spriteHandlers(dependencies.entitySpriteHandlerCatalog())
                        .enemyCombatTables(dependencies.enemyCombatTables())
                        .chestContentsTable(dependencies.chestContentsTable())
                        .romTables(dependencies.romTables())
                        .rom(dependencies.romData())
                        .build();
        if (dependencies.entityBridge().get().entityRuntime != null) {
            configureRuntimeWorld();
            configureRuntimeInputs();
            configureRuntimeProgress();
        }
        dependencies.followers().followingNpcRoomNeedsSync = true;
        dependencies.roomStoryState().get().synchronizeFollowingNpcEntitiesIfNeeded();
        if (dependencies.roomLoadListener() != null) {
            dependencies.roomLoadListener().roomLoaded(dependencies.location().activeRoom);
        }
    }

    record Dependencies(
            ChestContentsTable chestContentsTable,
            ColorShellWorld colorShellWorld,
            RomEnemyCombatTables enemyCombatTables,
            RoomEntityBackgroundInteraction entityBackgroundInteraction,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            java.util.function.Supplier<RoomEntityInputs> entityInputs,
            RomRandomByteSource entityRandomByteSource,
            EntitySpriteHandlerCatalog entitySpriteHandlerCatalog,
            RoomFollowerState followers,
            GPU gpu,
            RoomLocationState location,
            OwlEventDialogResolver owlEventDialogResolver,
            RoomPuzzleState puzzles,
            Rom romData,
            RomTables romTables,
            java.util.function.Supplier<RoomCollisionQueries> roomCollisionQueries,
            java.util.function.Supplier<RoomLoadCoordinator> roomLoadCoordinator,
            RoomLoadListener roomLoadListener,
            java.util.function.Supplier<RoomPuzzleController> roomPuzzleController,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState,
            java.util.function.Supplier<RoomTransitionController> roomTransitionController) {}

    private RoomEntitySnapshot prepareEntities(LoadedRoom room, int[][] objectPalettes) {
        RoomEntitySnapshot entities = room.entities();
        if (objectPalettes != null && entities != null && entities.spriteSelection() != null) {
            entities = entities.withSpriteSelection(entities.spriteSelection().withObjectPalettes(objectPalettes));
        }
        if (entities != null && entities.spriteSelection() != null) {
            var selection = entities.spriteSelection();
            if (selection.hasStandardSheets()) {
                dependencies.gpu().loadEntitySpriteSheets(dependencies.romData(), selection.sheetValues());
                entities = entities.withSpriteTiles(dependencies.gpu().snapshotEntityTiles());
            } else if (selection.roomTable() == EntityRoomLoader.RoomTable.COLOR_DUNGEON) {
                dependencies.gpu().loadColorDungeonEntitySheets(dependencies.romData(), selection.roomId());
                entities = entities.withSpriteTiles(dependencies.gpu().snapshotEntityTiles());
            }
        }
        if (entities != null) {
            entities = entities.withSideScrolling(room.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        }
        return entities;
    }

    private void configureDoorAndEventState() {
        if (dependencies.location().activeRoom.shutterDoorMask() != 0
                && dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            boolean ordinaryEventPending = dependencies.puzzles().activeRoomEvent != 0
                    && ((dependencies.puzzles().activeRoomEvent & EVENT_EFFECT_MASK) == EVENT_EFFECT_OPEN_LOCKED_DOORS
                            || (dependencies.roomSessionViews().get().activeRoomStatusFlags() & ROOM_STATUS_EVENT_1)
                                    == 0);
            if (ordinaryEventPending) {
                // $02:5E7B keeps entry doors open until Link is inside the room.
                dependencies.puzzles().pendingShutterCloseMask =
                        dependencies.location().activeRoom.shutterDoorMask();
            } else {
                dependencies.roomPuzzleController().get().openActiveRoomShutterDoors();
            }
        }
        dependencies
                .roomTransitionController()
                .get()
                .configureStaircase(dependencies.location().activeRoom.staircaseLocation());
        boolean minibossDefeated = dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                && dependencies.location().activeRoom.mapId() >= 0
                && dependencies.location().activeRoom.mapId() < dependencies.roomState().dungeonProgressFlags.length
                && ((dependencies.roomState()
                                                .dungeonProgressFlags[
                                                dependencies
                                                        .location()
                                                        .activeRoom
                                                        .mapId()]
                                        & 0x01)
                                != 0
                        || (dependencies.roomSessionViews().get().activeRoomStatusFlags() & ROOM_STATUS_EVENT_2) != 0);
        if (minibossDefeated && dependencies.puzzles().activeRoomEvent == EVENT_CLEAR_MIDBOSS) {
            dependencies.roomState()
                    .dungeonProgressFlags[dependencies.location().activeRoom.mapId()] |= 0x01;
            dependencies.roomPuzzleController().get().openActiveRoomShutterDoors();
            dependencies.puzzles().activeRoomEvent = 0;
        }
    }

    private void configureRuntimeWorld() {
        var configuration = dependencies.entityBridge().get().entityRuntime.configuration();

        configuration.setBooBuddyTriggerCount(dependencies.puzzles().roomTriggerCount);
        configuration.setColorShellWorld(dependencies.colorShellWorld());
        configuration.setFollowingNpcState(dependencies.followers().followingNpcState);
        configuration.setEntityMapId(dependencies.location().activeRoom.mapId());
        configuration.setEntityRoomId(dependencies.location().activeRoom.roomId());
        configuration.setOwlDialogResolver(dependencies.owlEventDialogResolver()::globalDialogId);
        configuration.setOwlDefaultMusicResolver(dependencies.owlEventDialogResolver()::defaultMusicTrack);
        configuration.setGroundInteraction(dependencies.roomCollisionQueries().get()::entityGroundInteraction);
        configuration.setBackgroundInteraction(dependencies.entityBackgroundInteraction());
    }

    private void configureRuntimeInputs() {
        var configuration = dependencies.entityBridge().get().entityRuntime.configuration();

        configuration.setObjectQuery(dependencies.roomCollisionQueries().get()::entityObjectSample);
        configuration.setObjectIntersectionQuery(
                dependencies.roomCollisionQueries().get()::entityObjectIntersectionSample);
        configuration.setGroundInteractionSideScrolling(
                dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        configuration.setActionButtonsHeld(
                dependencies.entityInputs().get().actionButtonAHeld,
                dependencies.entityInputs().get().actionButtonBHeld);
        configuration.setTalkState(
                dependencies.entityInputs().get().entityInventoryAppearing,
                dependencies.entityInputs().get().entityDialogCooldown,
                dependencies.entityInputs().get().entityWindowY);
        configuration.setJoypadHeld(dependencies.entityInputs().get().joypadHeld);
        configuration.setPressedButtonsMask(dependencies.entityInputs().get().entityPressedButtonsMask);
        configuration.setPowerBraceletButtonHeld(dependencies.entityInputs().get().powerBraceletButtonHeld);
        configuration.setBombButtonHeld(dependencies.entityInputs().get().bombButtonHeld);
    }

    private void configureRuntimeProgress() {
        var configuration = dependencies.entityBridge().get().entityRuntime.configuration();

        configuration.setLiftedLinkC13B(dependencies.followers().followingEntityYOffset);
        configuration.setEntityRoomStatus(dependencies.roomSessionViews().get().activeRoomStatusFlags());
        configuration.setDungeonMinibossReady(dependencies.location().activeRoom.mapId() >= 0
                && dependencies.location().activeRoom.mapId() < dependencies.roomState().dungeonProgressFlags.length
                && (dependencies.roomState()
                                        .dungeonProgressFlags[
                                        dependencies.location().activeRoom.mapId()]
                                & 0x01)
                        != 0);
        configuration.setTailKeyOwned(dependencies.followers().hasTailKey);
        configuration.setOwlInstrumentFlags(
                Byte.toUnsignedInt(dependencies.roomState().dungeonProgressFlags[0]),
                Byte.toUnsignedInt(dependencies.roomState().dungeonProgressFlags[1]));
        configuration.setSecretSeashellPegasusCollisionState(
                dependencies.entityInputs().get().secretSeashellScreenShakeActive,
                        dependencies.entityInputs().get().secretSeashellPegasusCollisionActive,
                dependencies.entityInputs().get().secretSeashellPegasusCollisionX,
                        dependencies.entityInputs().get().secretSeashellPegasusCollisionY);
        dependencies.roomLoadCoordinator().get().configureEnemyDropRuntime();
    }
}
