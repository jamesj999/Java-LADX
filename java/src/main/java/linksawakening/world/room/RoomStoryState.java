package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_EVENT_2;
import static linksawakening.world.room.RoomSessionRules.W_TILESET_NO_UPDATE;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.state.Direction;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EntityRomConstants;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.transition.Warp;

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
    public void setFollowerSpriteOverrides(Map<Integer, linksawakening.entity.EntitySpriteDefinition> overrides) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded != null) setFollowerSpriteOverrides(loaded, overrides);
    }

    @Override
    public void setTailKeyOwned(boolean owned) {
        rememberTailKeyOwned(owned);
        rooms.update(
                this,
                (room, story) -> story.applyTailKeyOwned(
                        room, story.dependencies.followers().hasTailKey()));
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
        rememberFollowingNpcState(state, linkX, linkY, linkZ, entityYOffset, linkDirection);
        rooms.update(this, (room, story) -> story.synchronizeFollowingNpcEntitiesIfNeeded(room));
    }

    @Override
    public void setBowWowState(int state) {
        rememberBowWowState(state);
        rooms.update(
                this,
                (room, story) -> story.applyBowWowState(
                        room, story.dependencies.followers().bowWowState()));
    }

    @Override
    public void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        rememberChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
        rooms.update(
                this,
                (room, story) -> story.applyChestPlayerLevels(
                        room,
                        story.dependencies.followers().playerShieldLevel(),
                        story.dependencies.followers().playerSwordLevel(),
                        story.dependencies.followers().playerBraceletLevel()));
    }

    @Override
    public void setHouseNpcTradeItem(int tradeItem) {
        rememberHouseNpcTradeItem(tradeItem);
        rooms.update(this, (room, story) -> story.configureHouseNpcStoryState(room));
    }

    @Override
    public void setEntityDefaultMusicTrack(int trackId) {
        rememberEntityDefaultMusicTrack(trackId);
        rooms.update(this, (room, story) -> story.updateWitchEnvironment(room));
    }

    @Override
    public void initializeNewGameWorldState() {
        resetWorldStoryState();
        rooms.updateOr(
                transients,
                RoomTransientState::clearTransientRoomState,
                (room, state) -> state.clearTransientRoomState(room.contents()));
    }

    private final Dependencies dependencies;
    private final RoomNpcPresentation roomNpcPresentation;
    private final RoomFollowerSynchronizer roomFollowerSynchronizer;

    RoomStoryState(
            LoadedRoomHandle rooms,
            RoomTransientState transients,
            Dependencies dependencies,
            RoomNpcPresentation roomNpcPresentation,
            RoomFollowerSynchronizer roomFollowerSynchronizer) {
        this.rooms = Objects.requireNonNull(rooms);
        this.transients = Objects.requireNonNull(transients);
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies");
        this.roomNpcPresentation = Objects.requireNonNull(roomNpcPresentation, "roomNpcPresentation");
        this.roomFollowerSynchronizer = Objects.requireNonNull(roomFollowerSynchronizer, "roomFollowerSynchronizer");
    }

    boolean applyMarinWakeUpPresentation(RoomRuntime loaded, int x, int y, int spriteVariant) {
        return roomNpcPresentation.applyMarinWakeUpPresentation(loaded, x, y, spriteVariant);
    }

    void updateHouseTarinPresentation(RoomRuntime loaded, int frameCounter, int linkX, int linkY) {
        roomNpcPresentation.updateHouseTarinPresentation(loaded, frameCounter, linkX, linkY);
    }

    @Override
    public void setBirdKeyOwned(boolean owned) {
        dependencies.followers().hasBirdKey(owned);
    }

    void rememberTailKeyOwned(boolean owned) {
        dependencies.followers().hasTailKey(owned);
    }

    void resetWorldStoryState() {
        Arrays.fill(dependencies.roomState().overworldRoomStatus(), (byte) 0);
        Arrays.fill(dependencies.roomState().indoorARoomStatus(), (byte) 0);
        Arrays.fill(dependencies.roomState().indoorBRoomStatus(), (byte) 0);
        Arrays.fill(dependencies.roomState().colorDungeonRoomStatus(), (byte) 0);
        dependencies.location().shouldGetLostInMysteriousWoods(false);
        dependencies.entityFrame().recentRoomEntityClears().reset();
        Arrays.fill(dependencies.roomState().dungeonProgressFlags(), (byte) 0);
        dependencies.roomState().dungeonItemState().reset();
        dependencies.followers().followingNpcState(FollowingNpcState.none());
        dependencies.followers().followingNpcRoomNeedsSync(true);
        dependencies.followers().followingLinkPositionHistory().fill(0x08, 0x10, 0, 0);
        dependencies.followers().hasBirdKey(false);
        dependencies.followers().hasTailKey(false);
        dependencies.followers().bowWowState(0);
        dependencies.followers().richardSpokenFlag(0);
        dependencies.followers().tarinFlag(0);
        dependencies.followers().playerShieldLevel(0);
        dependencies.followers().playerSwordLevel(0);
        dependencies.followers().playerBraceletLevel(0);
        dependencies.followers().houseNpcTradeItem(0);
        dependencies.entityInputs().entityGoldenLeavesCount(0);
        dependencies.entityFrame().enemyDropCounters(new EnemyDropResolver.CounterState(0, 0));
        dependencies.entityFrame().boomerangSoundCounter(0);
        dependencies.entityFrame().boomerangSoundClockProvided(false);
        dependencies.puzzles().dungeonTitleMessageCountdown(0);
        dependencies.terrain().switchBlocksState(0);
        dependencies.terrain().switchableObjectAnimationStage(0);
        dependencies.puzzles().switchButtonPressed(0);
        dependencies.location().currentOverworldTilesetId(W_TILESET_NO_UPDATE);
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
        dependencies.followers().followingLinkX(linkX & 0xFF);
        dependencies.followers().followingLinkY(linkY & 0xFF);
        dependencies.followers().followingLinkZ(linkZ & 0xFF);
        dependencies.followers().followingEntityYOffset(entityYOffset & 0xFF);
        dependencies.followers().followingLinkDirection(linkDirection & 0xFF);
        if (!state.equals(dependencies.followers().followingNpcState())) {
            dependencies.followers().followingNpcRoomNeedsSync(true);
        }
        dependencies.followers().followingNpcState(state);
    }

    @Override
    public linksawakening.state.FollowerFlags followerFlags() {
        var state = followingNpcState();
        return new linksawakening.state.FollowerFlags(
                state.roosterFollowing(), state.ghostFollowingState(), state.marinFollowing(), state.db10());
    }

    @Override
    public void restoreFollowers(linksawakening.state.FollowerFlags flags, int braceletLevel, int spawnX, int spawnY) {
        var state = new FollowingNpcState(
                flags.roosterFollowing(),
                flags.ghostFollowingState(),
                flags.marinFollowing(),
                bowWowState() == 1,
                dependencies.roomState().dungeonProgressFlags()[3] & 255,
                braceletLevel,
                flags.db10());
        rememberFollowingNpcState(state, spawnX, spawnY, 0, 0, 0);
        dependencies.followers().followingLinkPositionHistory().fill(spawnX, spawnY, 0, 0);
    }

    @Override
    public FollowingNpcState followingNpcState() {
        return dependencies.followers().followingNpcState();
    }

    @Override
    public int bowWowState() {
        return dependencies.followers().bowWowState() & 0xFF;
    }

    @Override
    public SignpostDialogRef useTelephone(int x, int y, Direction facing, PlayerState player) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null || loaded.room().mapCategory() == Warp.CATEGORY_OVERWORLD) return null;
        for (var entity : loaded.room().entityView().loadedEntities()) {
            if (entity.type() != EntityRomConstants.ENTITY_TELEPHONE || entity.status() != EntityStatus.ACTIVE)
                continue;
            int dx = (byte) (x - entity.x());
            int dy = (byte) (y - entity.y());
            if (dx < -16 || dx >= 16 || dy < -20 || dy >= 20) continue;
            int direction = Math.abs(dx) > Math.abs(dy) ? (dx < 0 ? 1 : 0) : (dy < 0 ? 2 : 3);
            if ((facing.romIndex() ^ 1) != direction) continue;
            int id = telephoneDialog(loaded, player);
            dependencies.roomState().indoorBRoomStatus()[0xA9] |= 0x20;
            return new SignpostDialogRef(id >>> 8, id & 255);
        }
        return null;
    }

    // Bank 06:6A8F checks these flags in order, including the shared first-call flag.
    private int telephoneDialog(RoomRuntime loaded, PlayerState player) {
        if (loaded.room().roomId() == 0xA9) return 0x1FD;
        if (player.progression().tradeSequenceItem() == 0x0A && hasInstrument(5)) return 0x16E;
        if ((dependencies.roomState().indoorBRoomStatus()[0xA9] & 0x20) == 0) return 0x141;
        if (!hasInstrument(1)) return 0x146;
        if (bowWowState() == 0x80) return 0x142;
        if (!hasInstrument(2)) return 0x143;
        if (bowWowState() == 1) return 0x244;
        if (richardSpokenFlag() != 2) return 0x144;
        if (player.progression().tradeSequenceItem() < 0x05) return 0x145;
        int leaves = player.progression().goldenLeavesCount();
        if (leaves < 5) {
            return 0x228 + loaded.contents().puzzles().nextTelephoneLeafHint();
        }
        if (leaves != 6) return 0x148;
        if (!hasInstrument(3)) return 0x149;
        if (player.progression().anglerKeyCount() == 0) return 0x14A;
        if (!hasInstrument(4)) return 0x240;
        if (followingNpcState().ghostFollowingState() != 0
                && (dependencies.roomState().indoorARoomStatus()[0xE3] & 0x40) == 0) return 0x14B;
        if (!hasInstrument(5)) return 0x14C;
        if ((player.progression().ocarinaSongFlags() & 1) == 0) return 0x245;
        if (!hasInstrument(6)) return 0x14D;
        if (followingNpcState().roosterFollowing() && player.progression().birdKeyCount() == 0) return 0x246;
        if (!hasInstrument(7)) return followingNpcState().roosterFollowing() ? 0x241 : 0x14E;
        if ((dependencies.roomState().overworldRoomStatus()[0x10] & 0x30) == 0) return 0x14F;
        if (!hasInstrument(8)) return 0x248;
        if ((dependencies.roomState().overworldRoomStatus()[0x06] & 0x30) == 0) return 0x242;
        if ((dependencies.roomState().indoorBRoomStatus()[0x74] & 0x40) == 0) return 0x243;
        return player.equipmentLevels().swordLevel() < 2 ? 0x247 : 0x248;
    }

    private boolean hasInstrument(int number) {
        return (dependencies.roomState().dungeonProgressFlags()[number - 1] & 2) != 0;
    }

    void rememberBowWowState(int state) {
        if ((state & ~0xFF) != 0) {
            throw new IllegalArgumentException("BowWow state must be an unsigned byte");
        }
        dependencies.followers().bowWowState(state);
    }

    @Override
    public int richardSpokenFlag() {
        return dependencies.followers().richardSpokenFlag() & 0xFF;
    }

    @Override
    public void setRichardSpokenFlag(int flag) {
        if ((flag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Richard spoken flag must be an unsigned byte");
        }
        dependencies.followers().richardSpokenFlag(flag);
    }

    @Override
    public int tarinFlag() {
        return dependencies.followers().tarinFlag() & 0xFF;
    }

    @Override
    public void setTarinFlag(int flag) {
        if ((flag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Tarin flag must be an unsigned byte");
        }
        dependencies.followers().tarinFlag(flag);
    }

    void rememberChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        if ((shieldLevel & ~0xFF) != 0 || (swordLevel & ~0xFF) != 0 || (powerBraceletLevel & ~0xFF) != 0) {
            throw new IllegalArgumentException("Player upgrade levels must be unsigned bytes");
        }
        dependencies.followers().playerShieldLevel(shieldLevel);
        dependencies.followers().playerSwordLevel(swordLevel);
        dependencies.followers().playerBraceletLevel(powerBraceletLevel);
    }

    void rememberHouseNpcTradeItem(int tradeItem) {
        if ((tradeItem & ~0xFF) != 0) throw new IllegalArgumentException("Trade item must be a byte");
        dependencies.followers().houseNpcTradeItem(tradeItem);
    }

    void configureHouseNpcStoryState(RoomRuntime loaded) {
        roomFollowerSynchronizer.configureHouseNpcStoryState(loaded);
    }

    void rememberEntityDefaultMusicTrack(int trackId) {
        if ((trackId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Default music track must be an unsigned byte");
        }
        dependencies.followers().entityDefaultMusicTrack(trackId);
    }

    void updateWitchEnvironment(RoomRuntime loaded) {
        loaded.entities()
                .configuration()
                .setWitchEnvironment(
                        dependencies.indoorTorchPaletteEffect().effectAddress(),
                        dependencies.indoorTorchPaletteEffect().paletteDataFlags(),
                        dependencies.followers().entityDefaultMusicTrack());
    }

    @Override
    public boolean shouldGetLostInMysteriousWoods() {
        return dependencies.location().shouldGetLostInMysteriousWoods();
    }

    RoomRuntime synchronizeFollowingNpcEntitiesIfNeeded(RoomRuntime loaded) {
        return roomFollowerSynchronizer.synchronizeFollowingNpcEntitiesIfNeeded(loaded);
    }

    void markHeartContainerCollected(RoomRuntime loaded) {

        byte[] activeStatus = dependencies
                .roomSessionViews()
                .indoorStatusTableForMap(loaded.room().mapId());
        activeStatus[loaded.room().roomId()] |= (byte) ROOM_STATUS_EVENT_2;
        if (loaded.room().mapId() == 0x06) {
            dependencies.roomState().indoorBRoomStatus()[0x2E] |= (byte) ROOM_STATUS_EVENT_2;
        } else if (loaded.room().mapId() == 0x03) {
            dependencies.roomState().indoorARoomStatus()[0x66] |= (byte) ROOM_STATUS_EVENT_2;
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
            Consumer<RoomRuntime> installRoom,
            RoomRuntime.Services roomRuntimeServices) {
        Dependencies {
            Objects.requireNonNull(entityFrame, "entityFrame");
            Objects.requireNonNull(followers, "followers");
            Objects.requireNonNull(location, "location");
            Objects.requireNonNull(puzzles, "puzzles");
            Objects.requireNonNull(terrain, "terrain");
            Objects.requireNonNull(chestContentsTable, "chestContentsTable");
            Objects.requireNonNull(enemyCombatTables, "enemyCombatTables");
            Objects.requireNonNull(entityInputs, "entityInputs");
            Objects.requireNonNull(entityRandomByteSource, "entityRandomByteSource");
            Objects.requireNonNull(entitySpriteHandlerCatalog, "entitySpriteHandlerCatalog");
            Objects.requireNonNull(followingNpcEntitySpawner, "followingNpcEntitySpawner");
            Objects.requireNonNull(indoorTorchPaletteEffect, "indoorTorchPaletteEffect");
            Objects.requireNonNull(owlEventDialogResolver, "owlEventDialogResolver");
            Objects.requireNonNull(romData, "romData");
            Objects.requireNonNull(romTables, "romTables");
            Objects.requireNonNull(dropResolver, "dropResolver");
            Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            Objects.requireNonNull(roomState, "roomState");
        }
    }

    void applyTailKeyOwned(RoomRuntime loaded, boolean owned) {
        loaded.entities().configuration().setTailKeyOwned(owned);
    }

    void applyRestoredRoomStatuses(
            RoomRuntime loaded,
            byte[] overworldStatus,
            byte[] indoorAStatus,
            byte[] indoorBStatus,
            byte[] colorDungeonStatus) {
        loaded.entities()
                .configuration()
                .setEntityRoomStatus(dependencies.roomSessionViews().activeRoomStatusFlags(loaded.room()));
        loaded.entities()
                .configuration()
                .setTailKeyOwned(dependencies.followers().hasTailKey());
    }

    void applyBowWowState(RoomRuntime loaded, int state) {
        loaded.entities().configuration().setBowWowState(state);
    }

    void applyChestPlayerLevels(RoomRuntime loaded, int shieldLevel, int swordLevel, int powerBraceletLevel) {
        configureHouseNpcStoryState(loaded);

        loaded.entities().configuration().setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);

        loaded.room().replaceEntities(loaded.entities().inspection().view());
    }
}
