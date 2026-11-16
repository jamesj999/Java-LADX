package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_EVENT_2;
import static linksawakening.world.room.RoomSessionRules.W_TILESET_NO_UPDATE;

import java.util.Arrays;
import java.util.Map;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;

/** LoadRoomEntities story flags and restoration; follower and NPC presentation have distinct owners. */
final class RoomStoryState implements RoomStory {
    private final LoadedRoomHandle rooms;
    private final RoomTransientState transients;

    @Override
    public boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null && applyMarinWakeUpPresentation(loaded, x, y, spriteVariant);
    }

    @Override
    public void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded != null) updateHouseTarinPresentation(loaded, frameCounter, linkX, linkY);
    }

    @Override
    public void setFollowerSpriteOverrides(
            java.util.Map<Integer, linksawakening.entity.EntitySpriteDefinition> overrides) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded != null) setFollowerSpriteOverrides(loaded, overrides);
    }

    @Override
    public void setTailKeyOwned(boolean owned) {
        RoomRuntime loaded = rooms.currentOrNull();
        rememberTailKeyOwned(owned);
        if (loaded != null) applyTailKeyOwned(loaded, owned);
    }

    @Override
    public void restoreRoomStatuses(
            byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) {
        RoomRuntime loaded = rooms.currentOrNull();
        rememberRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
        if (loaded != null)
            applyRestoredRoomStatuses(loaded, overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
    }

    @Override
    public void setFollowingNpcState(
            FollowingNpcState state, int linkX, int linkY, int linkZ, int entityYOffset, int linkDirection) {
        RoomRuntime loaded = rooms.currentOrNull();
        rememberFollowingNpcState(state, linkX, linkY, linkZ, entityYOffset, linkDirection);
        if (loaded != null) synchronizeFollowingNpcEntitiesIfNeeded(loaded);
    }

    @Override
    public void setBowWowState(int state) {
        RoomRuntime loaded = rooms.currentOrNull();
        rememberBowWowState(state);
        if (loaded != null) applyBowWowState(loaded, state);
    }

    @Override
    public void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        RoomRuntime loaded = rooms.currentOrNull();
        rememberChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
        if (loaded != null) applyChestPlayerLevels(loaded, shieldLevel, swordLevel, powerBraceletLevel);
    }

    @Override
    public void setHouseNpcTradeItem(int tradeItem) {
        RoomRuntime loaded = rooms.currentOrNull();
        rememberHouseNpcTradeItem(tradeItem);
        if (loaded != null) configureHouseNpcStoryState(loaded);
    }

    @Override
    public void setEntityDefaultMusicTrack(int trackId) {
        RoomRuntime loaded = rooms.currentOrNull();
        rememberEntityDefaultMusicTrack(trackId);
        if (loaded != null) updateWitchEnvironment(loaded);
    }

    @Override
    public void initializeNewGameWorldState() {
        RoomRuntime loaded = rooms.currentOrNull();
        resetWorldStoryState();
        if (loaded != null) transients.clearTransientRoomState(loaded.contents);
        else transients.clearTransientRoomState();
    }

    private final Dependencies dependencies;
    private final RoomNpcPresentation roomNpcPresentation;
    private final RoomFollowerSynchronizer roomFollowerSynchronizer;

    RoomStoryState(LoadedRoomHandle rooms, RoomTransientState transients, Dependencies dependencies) {
        this.rooms = java.util.Objects.requireNonNull(rooms);
        this.transients = java.util.Objects.requireNonNull(transients);
        this.dependencies = dependencies;
        roomNpcPresentation = new RoomNpcPresentation(new RoomNpcPresentation.Dependencies(dependencies.followers()));
        roomFollowerSynchronizer = new RoomFollowerSynchronizer(new RoomFollowerSynchronizer.Dependencies(
                dependencies.chestContentsTable(),
                dependencies.enemyCombatTables(),
                dependencies.entityInputs(),
                dependencies.entityRandomByteSource(),
                dependencies.entitySpriteHandlerCatalog(),
                dependencies.followers(),
                dependencies.followingNpcEntitySpawner(),
                dependencies.owlEventDialogResolver(),
                dependencies.romData(),
                dependencies.romTables(),
                dependencies.entityFrame(),
                dependencies.roomSessionViews(),
                dependencies.roomState(),
                this,
                dependencies.dropResolver(),
                dependencies.installRoom(),
                dependencies.roomRuntimeServices()));
    }

    boolean applyMarinWakeUpPresentation(RoomRuntime loaded, int x, int y, int spriteVariant) {
        return roomNpcPresentation.applyMarinWakeUpPresentation(loaded, x, y, spriteVariant);
    }

    void updateHouseTarinPresentation(RoomRuntime loaded, int frameCounter, int linkX, int linkY) {
        roomNpcPresentation.updateHouseTarinPresentation(loaded, frameCounter, linkX, linkY);
    }

    @Override
    public void setBirdKeyOwned(boolean owned) {
        dependencies.followers().hasBirdKey = owned;
    }

    void rememberTailKeyOwned(boolean owned) {
        dependencies.followers().hasTailKey = owned;
    }

    void resetWorldStoryState() {
        Arrays.fill(dependencies.roomState().overworldRoomStatus, (byte) 0);
        Arrays.fill(dependencies.roomState().indoorARoomStatus, (byte) 0);
        Arrays.fill(dependencies.roomState().indoorBRoomStatus, (byte) 0);
        Arrays.fill(dependencies.roomState().colorDungeonRoomStatus, (byte) 0);
        dependencies.location().shouldGetLostInMysteriousWoods = false;
        dependencies.entityFrame().recentRoomEntityClears.reset();
        Arrays.fill(dependencies.roomState().dungeonProgressFlags, (byte) 0);
        dependencies.roomState().dungeonItemState.reset();
        dependencies.followers().followingNpcState = FollowingNpcState.none();
        dependencies.followers().followingNpcRoomNeedsSync = true;
        dependencies.followers().followingLinkPositionHistory.fill(0x08, 0x10, 0, 0);
        dependencies.followers().hasBirdKey = false;
        dependencies.followers().hasTailKey = false;
        dependencies.followers().bowWowState = 0;
        dependencies.followers().richardSpokenFlag = 0;
        dependencies.followers().tarinFlag = 0;
        dependencies.followers().playerShieldLevel = 0;
        dependencies.followers().playerSwordLevel = 0;
        dependencies.followers().playerBraceletLevel = 0;
        dependencies.followers().houseNpcTradeItem = 0;
        dependencies.entityInputs().entityGoldenLeavesCount = 0;
        dependencies.entityFrame().enemyDropCounters = new EnemyDropResolver.CounterState(0, 0);
        dependencies.entityFrame().boomerangSoundCounter = 0;
        dependencies.entityFrame().boomerangSoundClockProvided = false;
        dependencies.puzzles().dungeonTitleMessageCountdown = 0;
        dependencies.terrain().switchBlocksState = 0;
        dependencies.terrain().switchableObjectAnimationStage = 0;
        dependencies.puzzles().switchButtonPressed = 0;
        dependencies.location().currentOverworldTilesetId = W_TILESET_NO_UPDATE;
    }

    void rememberRoomStatuses(
            byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) {
        dependencies.roomState().restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
    }

    void setFollowerSpriteOverrides(RoomRuntime loaded, Map<Integer, EntitySpriteDefinition> overrides) {
        roomNpcPresentation.setFollowerSpriteOverrides(loaded, overrides);
    }

    void rememberFollowingNpcState(
            FollowingNpcState state, int linkX, int linkY, int linkZ, int entityYOffset, int linkDirection) {
        if (state == null) {
            throw new IllegalArgumentException("Follower state cannot be null");
        }
        dependencies.followers().followingLinkX = linkX & 0xFF;
        dependencies.followers().followingLinkY = linkY & 0xFF;
        dependencies.followers().followingLinkZ = linkZ & 0xFF;
        dependencies.followers().followingEntityYOffset = entityYOffset & 0xFF;
        dependencies.followers().followingLinkDirection = linkDirection & 0xFF;
        if (!state.equals(dependencies.followers().followingNpcState)) {
            dependencies.followers().followingNpcRoomNeedsSync = true;
        }
        dependencies.followers().followingNpcState = state;
    }

    @Override
    public FollowingNpcState followingNpcState() {
        return dependencies.followers().followingNpcState;
    }

    @Override
    public int bowWowState() {
        return dependencies.followers().bowWowState & 0xFF;
    }

    void rememberBowWowState(int state) {
        if ((state & ~0xFF) != 0) {
            throw new IllegalArgumentException("BowWow state must be an unsigned byte");
        }
        dependencies.followers().bowWowState = state;
    }

    @Override
    public int richardSpokenFlag() {
        return dependencies.followers().richardSpokenFlag & 0xFF;
    }

    @Override
    public void setRichardSpokenFlag(int flag) {
        if ((flag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Richard spoken flag must be an unsigned byte");
        }
        dependencies.followers().richardSpokenFlag = flag;
    }

    @Override
    public int tarinFlag() {
        return dependencies.followers().tarinFlag & 0xFF;
    }

    @Override
    public void setTarinFlag(int flag) {
        if ((flag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Tarin flag must be an unsigned byte");
        }
        dependencies.followers().tarinFlag = flag;
    }

    void rememberChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        if ((shieldLevel & ~0xFF) != 0 || (swordLevel & ~0xFF) != 0 || (powerBraceletLevel & ~0xFF) != 0) {
            throw new IllegalArgumentException("Player upgrade levels must be unsigned bytes");
        }
        dependencies.followers().playerShieldLevel = shieldLevel;
        dependencies.followers().playerSwordLevel = swordLevel;
        dependencies.followers().playerBraceletLevel = powerBraceletLevel;
    }

    void rememberHouseNpcTradeItem(int tradeItem) {
        if ((tradeItem & ~0xFF) != 0) throw new IllegalArgumentException("Trade item must be a byte");
        dependencies.followers().houseNpcTradeItem = tradeItem;
    }

    void configureHouseNpcStoryState(RoomRuntime loaded) {
        loaded.entities
                .configuration()
                .setHouseNpcStoryState(
                        dependencies.followers().tarinFlag,
                        dependencies.followers().houseNpcTradeItem,
                        Byte.toUnsignedInt(dependencies.roomState().dungeonProgressFlags[2]),
                        Byte.toUnsignedInt(dependencies.roomState().overworldRoomStatus[0xFD]));
    }

    void rememberEntityDefaultMusicTrack(int trackId) {
        if ((trackId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Default music track must be an unsigned byte");
        }
        dependencies.followers().entityDefaultMusicTrack = trackId;
    }

    void updateWitchEnvironment(RoomRuntime loaded) {
        loaded.entities
                .configuration()
                .setWitchEnvironment(
                        dependencies.indoorTorchPaletteEffect().effectAddress(),
                        dependencies.indoorTorchPaletteEffect().paletteDataFlags(),
                        dependencies.followers().entityDefaultMusicTrack);
    }

    @Override
    public boolean shouldGetLostInMysteriousWoods() {
        return dependencies.location().shouldGetLostInMysteriousWoods;
    }

    RoomRuntime synchronizeFollowingNpcEntitiesIfNeeded(RoomRuntime loaded) {
        return roomFollowerSynchronizer.synchronizeFollowingNpcEntitiesIfNeeded(loaded);
    }

    void markHeartContainerCollected(RoomRuntime loaded) {

        byte[] activeStatus = dependencies.roomSessionViews().indoorStatusTableForMap(loaded.room.mapId());
        activeStatus[loaded.room.roomId()] |= (byte) ROOM_STATUS_EVENT_2;
        if (loaded.room.mapId() == 0x06) {
            dependencies.roomState().indoorBRoomStatus[0x2E] |= (byte) ROOM_STATUS_EVENT_2;
        } else if (loaded.room.mapId() == 0x03) {
            dependencies.roomState().indoorARoomStatus[0x66] |= (byte) ROOM_STATUS_EVENT_2;
        }
    }

    record Dependencies(
            RoomEntitySessionState entityFrame,
            RoomFollowerState followers,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            RoomTerrainState terrain,
            ChestContentsTable chestContentsTable,
            RomEnemyCombatTables enemyCombatTables,
            RoomEntityInputs entityInputs,
            RomRandomByteSource entityRandomByteSource,
            EntitySpriteHandlerCatalog entitySpriteHandlerCatalog,
            FollowingNpcEntitySpawner followingNpcEntitySpawner,
            IndoorTorchPaletteEffect indoorTorchPaletteEffect,
            OwlEventDialogResolver owlEventDialogResolver,
            Rom romData,
            RomTables romTables,
            linksawakening.world.entity.EnemyDropResolver dropResolver,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Consumer<RoomRuntime> installRoom,
            RoomRuntime.Services roomRuntimeServices) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(puzzles, "puzzles");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(chestContentsTable, "chestContentsTable");
            java.util.Objects.requireNonNull(enemyCombatTables, "enemyCombatTables");
            java.util.Objects.requireNonNull(entityInputs, "entityInputs");
            java.util.Objects.requireNonNull(entityRandomByteSource, "entityRandomByteSource");
            java.util.Objects.requireNonNull(entitySpriteHandlerCatalog, "entitySpriteHandlerCatalog");
            java.util.Objects.requireNonNull(followingNpcEntitySpawner, "followingNpcEntitySpawner");
            java.util.Objects.requireNonNull(indoorTorchPaletteEffect, "indoorTorchPaletteEffect");
            java.util.Objects.requireNonNull(owlEventDialogResolver, "owlEventDialogResolver");
            java.util.Objects.requireNonNull(romData, "romData");
            java.util.Objects.requireNonNull(romTables, "romTables");
            java.util.Objects.requireNonNull(dropResolver, "dropResolver");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
        }
    }

    void applyTailKeyOwned(RoomRuntime loaded, boolean owned) {
        loaded.entities.configuration().setTailKeyOwned(owned);
    }

    void applyRestoredRoomStatuses(
            RoomRuntime loaded,
            byte[] overworldStatus,
            byte[] indoorAStatus,
            byte[] indoorBStatus,
            byte[] colorDungeonStatus) {
        loaded.entities
                .configuration()
                .setEntityRoomStatus(dependencies.roomSessionViews().activeRoomStatusFlags(loaded.room));
        loaded.entities.configuration().setTailKeyOwned(dependencies.followers().hasTailKey);
    }

    void applyBowWowState(RoomRuntime loaded, int state) {
        loaded.entities.configuration().setBowWowState(state);
    }

    void applyChestPlayerLevels(RoomRuntime loaded, int shieldLevel, int swordLevel, int powerBraceletLevel) {
        configureHouseNpcStoryState(loaded);

        loaded.entities.configuration().setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);

        loaded.room.replaceEntities(loaded.entities.inspection().view());
    }
}
