package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.FollowingNpcRoomContext;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.transition.Warp;

final class RoomFollowerSynchronizer {
    private final Dependencies dependencies;

    RoomFollowerSynchronizer(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void synchronizeFollowingNpcEntitiesIfNeeded() {
        if (!dependencies.followers().followingNpcRoomNeedsSync
                || dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null
                || dependencies.location().activeRoom.entities() == null) {
            return;
        }
        FollowingNpcRoomContext room = new FollowingNpcRoomContext(
                dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
                dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL,
                dependencies.location().activeRoom.mapId(),
                dependencies.location().activeRoom.roomId());
        FollowingNpcEntitySpawner.Result result = dependencies
                .followingNpcEntitySpawner()
                .synchronize(new linksawakening.world.entity.FollowingNpcSpawn(
                        dependencies.location().activeRoom.entities(),
                        room,
                        dependencies.followers().followingNpcState,
                        dependencies.followers().followingLinkX,
                        dependencies.followers().followingLinkY,
                        dependencies.followers().followingLinkZ,
                        dependencies.followers().followingEntityYOffset,
                        dependencies.followers().followingLinkDirection,
                        dependencies.followers().followingLinkPositionHistory));
        dependencies.followers().followingNpcState = result.state();
        dependencies.followers().followingNpcRoomNeedsSync = false;
        dependencies.location().activeRoom.replaceEntities(result.snapshot());
        dependencies.entityBridge().get().entityRuntime = RoomEntityRuntime.builder(result.snapshot())
                .indoorRoom(dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD)
                .randomByteSupplier(dependencies.entityRandomByteSource())
                .spriteHandlers(dependencies.entitySpriteHandlerCatalog())
                .enemyCombatTables(dependencies.enemyCombatTables())
                .chestContentsTable(dependencies.chestContentsTable())
                .romTables(dependencies.romTables())
                .rom(dependencies.romData())
                .build();
        configureFollowerWorld();
        configureFollowerCollision();
        configureFollowerInputs();
        configureFollowerProgress();
        dependencies.owner().get().configureHouseNpcStoryState();
        if (dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                && dependencies.location().activeRoom.mapId() == 0x10
                && dependencies.location().activeRoom.roomId() == 0xA3) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setChestPlayerLevels(
                            dependencies.followers().playerShieldLevel,
                            dependencies.followers().playerSwordLevel,
                            dependencies.followers().playerBraceletLevel);
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
        dependencies.roomLoadCoordinator().get().configureEnemyDropRuntime();
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
            FollowingNpcEntitySpawner followingNpcEntitySpawner,
            RoomLocationState location,
            OwlEventDialogResolver owlEventDialogResolver,
            Rom romData,
            RomTables romTables,
            java.util.function.Supplier<RoomCollisionQueries> roomCollisionQueries,
            java.util.function.Supplier<RoomLoadCoordinator> roomLoadCoordinator,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> owner) {}

    private void configureFollowerWorld() {
        var configuration = dependencies.entityBridge().get().entityRuntime.configuration();
        configuration.setColorShellWorld(dependencies.colorShellWorld());
        configuration.setFollowingNpcState(dependencies.followers().followingNpcState);
        configuration.setEntityMapId(dependencies.location().activeRoom.mapId());
        configuration.setEntityRoomId(dependencies.location().activeRoom.roomId());
        configuration.setDungeonMinibossReady(dependencies.location().activeRoom.mapId() >= 0
                && dependencies.location().activeRoom.mapId() < dependencies.roomState().dungeonProgressFlags.length
                && (dependencies.roomState()
                                        .dungeonProgressFlags[
                                        dependencies.location().activeRoom.mapId()]
                                & 0x01)
                        != 0);
        configuration.setOwlDialogResolver(dependencies.owlEventDialogResolver()::globalDialogId);
    }

    private void configureFollowerCollision() {
        var configuration = dependencies.entityBridge().get().entityRuntime.configuration();

        configuration.setOwlDefaultMusicResolver(dependencies.owlEventDialogResolver()::defaultMusicTrack);
        configuration.setGroundInteraction(dependencies.roomCollisionQueries().get()::entityGroundInteraction);
        configuration.setBackgroundInteraction(dependencies.entityBackgroundInteraction());
        configuration.setObjectQuery(dependencies.roomCollisionQueries().get()::entityObjectSample);
        configuration.setObjectIntersectionQuery(
                dependencies.roomCollisionQueries().get()::entityObjectIntersectionSample);
        configuration.setGroundInteractionSideScrolling(
                dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
    }

    private void configureFollowerInputs() {
        var configuration = dependencies.entityBridge().get().entityRuntime.configuration();

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

    private void configureFollowerProgress() {
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
        configuration.setSecretSeashellPegasusCollisionState(
                dependencies.entityInputs().get().secretSeashellScreenShakeActive,
                        dependencies.entityInputs().get().secretSeashellPegasusCollisionActive,
                dependencies.entityInputs().get().secretSeashellPegasusCollisionX,
                        dependencies.entityInputs().get().secretSeashellPegasusCollisionY);
    }
}
