package linksawakening.world.room;

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
                || dependencies.entityRuntimeState().runtime == null
                || dependencies.location().activeRoom.entityView() == null) {
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
                        dependencies.location().activeRoom.entityView().ownedCopy(),
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
        dependencies.entityRuntimeState().runtime = RoomEntityRuntime.builder(result.snapshot())
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
        dependencies.owner().configureHouseNpcStoryState();
        if (dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                && dependencies.location().activeRoom.mapId() == 0x10
                && dependencies.location().activeRoom.roomId() == 0xA3) {
            dependencies
                    .entityRuntimeState()
                    .runtime
                    .configuration()
                    .setChestPlayerLevels(
                            dependencies.followers().playerShieldLevel,
                            dependencies.followers().playerSwordLevel,
                            dependencies.followers().playerBraceletLevel);
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityRuntimeState()
                            .runtime
                            .inspection()
                            .view());
        }
        dependencies.dropState().configureEnemyDropRuntime(dependencies.dropResolver(), dependencies.entityInputs());
    }

    record Dependencies(
            ChestContentsTable chestContentsTable,
            ColorShellWorld colorShellWorld,
            RomEnemyCombatTables enemyCombatTables,
            RoomEntityBackgroundInteraction entityBackgroundInteraction,
            RoomEntitySessionState entityRuntimeState,
            RoomEntityInputs entityInputs,
            RomRandomByteSource entityRandomByteSource,
            EntitySpriteHandlerCatalog entitySpriteHandlerCatalog,
            RoomFollowerState followers,
            FollowingNpcEntitySpawner followingNpcEntitySpawner,
            RoomLocationState location,
            OwlEventDialogResolver owlEventDialogResolver,
            Rom romData,
            RomTables romTables,
            RoomCollisionQueries roomCollisionQueries,
            RoomEntitySessionState dropState,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            RoomStoryState owner,
            linksawakening.world.entity.EnemyDropResolver dropResolver) {
        Dependencies {
            java.util.Objects.requireNonNull(chestContentsTable, "chestContentsTable");
            java.util.Objects.requireNonNull(colorShellWorld, "colorShellWorld");
            java.util.Objects.requireNonNull(enemyCombatTables, "enemyCombatTables");
            java.util.Objects.requireNonNull(entityBackgroundInteraction, "entityBackgroundInteraction");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(entityInputs, "entityInputs");
            java.util.Objects.requireNonNull(entityRandomByteSource, "entityRandomByteSource");
            java.util.Objects.requireNonNull(entitySpriteHandlerCatalog, "entitySpriteHandlerCatalog");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(followingNpcEntitySpawner, "followingNpcEntitySpawner");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(owlEventDialogResolver, "owlEventDialogResolver");
            java.util.Objects.requireNonNull(romData, "romData");
            java.util.Objects.requireNonNull(romTables, "romTables");
            java.util.Objects.requireNonNull(roomCollisionQueries, "roomCollisionQueries");
            java.util.Objects.requireNonNull(dropState, "dropState");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(owner, "owner");
        }
    }

    private void configureFollowerWorld() {
        var configuration = dependencies.entityRuntimeState().runtime.configuration();
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
        var configuration = dependencies.entityRuntimeState().runtime.configuration();

        configuration.setOwlDefaultMusicResolver(dependencies.owlEventDialogResolver()::defaultMusicTrack);
        configuration.setGroundInteraction(dependencies.roomCollisionQueries()::entityGroundInteraction);
        configuration.setBackgroundInteraction(dependencies.entityBackgroundInteraction());
        configuration.setObjectQuery(dependencies.roomCollisionQueries()::entityObjectSample);
        configuration.setObjectIntersectionQuery(dependencies.roomCollisionQueries()::entityObjectIntersectionSample);
        configuration.setGroundInteractionSideScrolling(
                dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
    }

    private void configureFollowerInputs() {
        var configuration = dependencies.entityRuntimeState().runtime.configuration();

        configuration.setActionButtonsHeld(
                dependencies.entityInputs().actionButtonAHeld, dependencies.entityInputs().actionButtonBHeld);
        configuration.setTalkState(
                dependencies.entityInputs().entityInventoryAppearing,
                dependencies.entityInputs().entityDialogCooldown,
                dependencies.entityInputs().entityWindowY);
        configuration.setJoypadHeld(dependencies.entityInputs().joypadHeld);
        configuration.setPressedButtonsMask(dependencies.entityInputs().entityPressedButtonsMask);
        configuration.setPowerBraceletButtonHeld(dependencies.entityInputs().powerBraceletButtonHeld);
        configuration.setBombButtonHeld(dependencies.entityInputs().bombButtonHeld);
    }

    private void configureFollowerProgress() {
        var configuration = dependencies.entityRuntimeState().runtime.configuration();

        configuration.setLiftedLinkC13B(dependencies.followers().followingEntityYOffset);
        configuration.setEntityRoomStatus(dependencies.roomSessionViews().activeRoomStatusFlags());
        configuration.setDungeonMinibossReady(dependencies.location().activeRoom.mapId() >= 0
                && dependencies.location().activeRoom.mapId() < dependencies.roomState().dungeonProgressFlags.length
                && (dependencies.roomState()
                                        .dungeonProgressFlags[
                                        dependencies.location().activeRoom.mapId()]
                                & 0x01)
                        != 0);
        configuration.setTailKeyOwned(dependencies.followers().hasTailKey);
        configuration.setSecretSeashellPegasusCollisionState(
                dependencies.entityInputs().secretSeashellScreenShakeActive,
                        dependencies.entityInputs().secretSeashellPegasusCollisionActive,
                dependencies.entityInputs().secretSeashellPegasusCollisionX,
                        dependencies.entityInputs().secretSeashellPegasusCollisionY);
    }
}
