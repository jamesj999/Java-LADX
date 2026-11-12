package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.Map;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** RoomStoryState operations for the live room session. */
final class RoomStoryState {
    private final Dependencies dependencies;
    private final RoomNpcPresentation roomNpcPresentation;
    private final RoomFollowerSynchronizer roomFollowerSynchronizer;
    private final RoomPlayerStoryState roomPlayerStoryState;
    private final RoomStoryRestoration roomStoryRestoration;

    RoomStoryState(Dependencies dependencies) {
        this.dependencies = dependencies;
        roomNpcPresentation = new RoomNpcPresentation(new RoomNpcPresentation.Dependencies(
                dependencies.entityBridge(), dependencies.followers(), dependencies.location()));
        roomFollowerSynchronizer = new RoomFollowerSynchronizer(new RoomFollowerSynchronizer.Dependencies(
                dependencies.chestContentsTable(),
                dependencies.colorShellWorld(),
                dependencies.enemyCombatTables(),
                dependencies.entityBackgroundInteraction(),
                dependencies.entityBridge(),
                dependencies.entityInputs(),
                dependencies.entityRandomByteSource(),
                dependencies.entitySpriteHandlerCatalog(),
                dependencies.followers(),
                dependencies.followingNpcEntitySpawner(),
                dependencies.location(),
                dependencies.owlEventDialogResolver(),
                dependencies.romData(),
                dependencies.romTables(),
                dependencies.roomCollisionQueries(),
                dependencies.roomLoadCoordinator(),
                dependencies.roomSessionViews(),
                dependencies.roomState(),
                () -> this));
        roomPlayerStoryState = new RoomPlayerStoryState(new RoomPlayerStoryState.Dependencies(
                dependencies.entityBridge(),
                dependencies.followers(),
                dependencies.indoorTorchPaletteEffect(),
                dependencies.location(),
                dependencies.roomState(),
                dependencies.roomStoryState(),
                () -> this));
        roomStoryRestoration = new RoomStoryRestoration(new RoomStoryRestoration.Dependencies(
                dependencies.entityBridge(),
                dependencies.entityFrame(),
                dependencies.entityInputs(),
                dependencies.followers(),
                dependencies.location(),
                dependencies.puzzles(),
                dependencies.roomSessionViews(),
                dependencies.roomState(),
                dependencies.roomStoryState(),
                dependencies.roomTransientState(),
                dependencies.terrain()));
    }

    boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        return roomNpcPresentation.applyMarinWakeUpPresentation(x, y, spriteVariant);
    }

    void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        roomNpcPresentation.updateHouseTarinPresentation(frameCounter, linkX, linkY);
    }

    void setBirdKeyOwned(boolean owned) {
        roomStoryRestoration.setBirdKeyOwned(owned);
    }

    void setTailKeyOwned(boolean owned) {
        roomStoryRestoration.setTailKeyOwned(owned);
    }

    void initializeNewGameWorldState() {
        roomStoryRestoration.initializeNewGameWorldState();
    }

    void restoreRoomStatuses(
            byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) {
        roomStoryRestoration.restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
    }

    void setFollowerSpriteOverrides(Map<Integer, EntitySpriteDefinition> overrides) {
        roomNpcPresentation.setFollowerSpriteOverrides(overrides);
    }

    void setFollowingNpcState(
            FollowingNpcState state, int linkX, int linkY, int linkZ, int entityYOffset, int linkDirection) {
        roomStoryRestoration.setFollowingNpcState(state, linkX, linkY, linkZ, entityYOffset, linkDirection);
    }

    FollowingNpcState followingNpcState() {
        return roomStoryRestoration.followingNpcState();
    }

    int bowWowState() {
        return roomStoryRestoration.bowWowState();
    }

    void setBowWowState(int state) {
        roomStoryRestoration.setBowWowState(state);
    }

    int richardSpokenFlag() {
        return roomStoryRestoration.richardSpokenFlag();
    }

    void setRichardSpokenFlag(int flag) {
        roomStoryRestoration.setRichardSpokenFlag(flag);
    }

    int tarinFlag() {
        return roomStoryRestoration.tarinFlag();
    }

    void setTarinFlag(int flag) {
        roomStoryRestoration.setTarinFlag(flag);
    }

    void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        roomPlayerStoryState.setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
    }

    void setHouseNpcTradeItem(int tradeItem) {
        roomPlayerStoryState.setHouseNpcTradeItem(tradeItem);
    }

    void configureHouseNpcStoryState() {
        roomPlayerStoryState.configureHouseNpcStoryState();
    }

    void setEntityDefaultMusicTrack(int trackId) {
        roomPlayerStoryState.setEntityDefaultMusicTrack(trackId);
    }

    void updateWitchEnvironment() {
        roomPlayerStoryState.updateWitchEnvironment();
    }

    boolean shouldGetLostInMysteriousWoods() {
        return roomStoryRestoration.shouldGetLostInMysteriousWoods();
    }

    void synchronizeFollowingNpcEntitiesIfNeeded() {
        roomFollowerSynchronizer.synchronizeFollowingNpcEntitiesIfNeeded();
    }

    void markHeartContainerCollected() {
        roomStoryRestoration.markHeartContainerCollected();
    }

    void markActiveRoomCompleted() {
        roomStoryRestoration.markActiveRoomCompleted();
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomFollowerState followers,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            RoomTerrainState terrain,
            ChestContentsTable chestContentsTable,
            ColorShellWorld colorShellWorld,
            RomEnemyCombatTables enemyCombatTables,
            RoomEntityBackgroundInteraction entityBackgroundInteraction,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            java.util.function.Supplier<RoomEntityInputs> entityInputs,
            RomRandomByteSource entityRandomByteSource,
            EntitySpriteHandlerCatalog entitySpriteHandlerCatalog,
            FollowingNpcEntitySpawner followingNpcEntitySpawner,
            IndoorTorchPaletteEffect indoorTorchPaletteEffect,
            OwlEventDialogResolver owlEventDialogResolver,
            byte[] romData,
            RomTables romTables,
            java.util.function.Supplier<RoomCollisionQueries> roomCollisionQueries,
            java.util.function.Supplier<RoomLoadCoordinator> roomLoadCoordinator,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState,
            java.util.function.Supplier<RoomTransientState> roomTransientState) {}
}
