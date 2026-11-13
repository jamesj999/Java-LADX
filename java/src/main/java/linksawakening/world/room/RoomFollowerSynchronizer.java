package linksawakening.world.room;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.FollowingNpcRoomContext;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.transition.Warp;

final class RoomFollowerSynchronizer {
    private final Dependencies dependencies;

    RoomFollowerSynchronizer(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    RoomRuntime synchronizeFollowingNpcEntitiesIfNeeded(RoomRuntime loaded) {
        if (!dependencies.followers().followingNpcRoomNeedsSync || loaded.room.entityView() == null) {
            return loaded;
        }
        FollowingNpcRoomContext room = new FollowingNpcRoomContext(
                loaded.room.mapCategory() != Warp.CATEGORY_OVERWORLD,
                loaded.room.mapCategory() == Warp.CATEGORY_SIDESCROLL,
                loaded.room.mapId(),
                loaded.room.roomId());
        FollowingNpcEntitySpawner.Result result = dependencies
                .followingNpcEntitySpawner()
                .synchronize(new linksawakening.world.entity.FollowingNpcSpawn(
                        loaded.room.entityView().ownedCopy(),
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
        loaded.room.replaceEntities(result.snapshot());
        RoomEntityRuntime replacementRuntime = RoomEntityRuntime.builder(result.snapshot())
                .indoorRoom(loaded.room.mapCategory() != Warp.CATEGORY_OVERWORLD)
                .randomByteSupplier(dependencies.entityRandomByteSource())
                .spriteHandlers(dependencies.entitySpriteHandlerCatalog())
                .enemyCombatTables(dependencies.enemyCombatTables())
                .chestContentsTable(dependencies.chestContentsTable())
                .romTables(dependencies.romTables())
                .rom(dependencies.romData())
                .build();
        loaded = new RoomRuntime(loaded.room, replacementRuntime, dependencies.roomRuntimeServices());
        dependencies.installRoom().accept(loaded);
        configureFollowerWorld(loaded);
        configureFollowerCollision(loaded);
        configureFollowerInputs(loaded);
        configureFollowerProgress(loaded);
        dependencies.owner().configureHouseNpcStoryState(loaded);
        if (loaded.room.mapCategory() != Warp.CATEGORY_OVERWORLD
                && loaded.room.mapId() == 0x10
                && loaded.room.roomId() == 0xA3) {
            loaded.entities
                    .configuration()
                    .setChestPlayerLevels(
                            dependencies.followers().playerShieldLevel,
                            dependencies.followers().playerSwordLevel,
                            dependencies.followers().playerBraceletLevel);
            loaded.room.replaceEntities(loaded.entities.inspection().view());
        }
        dependencies
                .dropState()
                .configureEnemyDropRuntime(loaded, dependencies.dropResolver(), dependencies.entityInputs());
        return loaded;
    }

    record Dependencies(
            ChestContentsTable chestContentsTable,
            RomEnemyCombatTables enemyCombatTables,
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
            RoomEntitySessionState dropState,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            RoomStoryState owner,
            linksawakening.world.entity.EnemyDropResolver dropResolver,
            java.util.function.Consumer<RoomRuntime> installRoom,
            RoomRuntime.Services roomRuntimeServices) {
        Dependencies {
            java.util.Objects.requireNonNull(chestContentsTable, "chestContentsTable");
            java.util.Objects.requireNonNull(enemyCombatTables, "enemyCombatTables");
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
            java.util.Objects.requireNonNull(dropState, "dropState");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(owner, "owner");
        }
    }

    private void configureFollowerWorld(RoomRuntime loaded) {
        var configuration = loaded.entities.configuration();
        configuration.setColorShellWorld(loaded.colorShellWorld);
        configuration.setFollowingNpcState(dependencies.followers().followingNpcState);
        configuration.setEntityMapId(loaded.room.mapId());
        configuration.setEntityRoomId(loaded.room.roomId());
        configuration.setDungeonMinibossReady(loaded.room.mapId() >= 0
                && loaded.room.mapId() < dependencies.roomState().dungeonProgressFlags.length
                && (dependencies.roomState().dungeonProgressFlags[loaded.room.mapId()] & 0x01) != 0);
        configuration.setOwlDialogResolver(dependencies.owlEventDialogResolver()::globalDialogId);
    }

    private void configureFollowerCollision(RoomRuntime loaded) {
        var configuration = loaded.entities.configuration();

        configuration.setOwlDefaultMusicResolver(dependencies.owlEventDialogResolver()::defaultMusicTrack);
        configuration.setGroundInteraction(loaded.groundInteraction);
        configuration.setBackgroundInteraction(loaded.backgroundInteraction);
        configuration.setObjectQuery(loaded.objectQuery);
        configuration.setObjectIntersectionQuery(loaded.objectIntersectionQuery);
        configuration.setGroundInteractionSideScrolling(loaded.room.mapCategory() == Warp.CATEGORY_SIDESCROLL);
    }

    private void configureFollowerInputs(RoomRuntime loaded) {
        var configuration = loaded.entities.configuration();

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

    private void configureFollowerProgress(RoomRuntime loaded) {
        var configuration = loaded.entities.configuration();

        configuration.setLiftedLinkC13B(dependencies.followers().followingEntityYOffset);
        configuration.setEntityRoomStatus(dependencies.roomSessionViews().activeRoomStatusFlags());
        configuration.setDungeonMinibossReady(loaded.room.mapId() >= 0
                && loaded.room.mapId() < dependencies.roomState().dungeonProgressFlags.length
                && (dependencies.roomState().dungeonProgressFlags[loaded.room.mapId()] & 0x01) != 0);
        configuration.setTailKeyOwned(dependencies.followers().hasTailKey);
        configuration.setSecretSeashellPegasusCollisionState(
                dependencies.entityInputs().secretSeashellScreenShakeActive,
                        dependencies.entityInputs().secretSeashellPegasusCollisionActive,
                dependencies.entityInputs().secretSeashellPegasusCollisionX,
                        dependencies.entityInputs().secretSeashellPegasusCollisionY);
    }
}
