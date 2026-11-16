package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.ENTITY_OWL_EVENT;
import static linksawakening.world.room.RoomSessionRules.ENTITY_TARIN;
import static linksawakening.world.room.RoomSessionRules.OCARINA_MARIN_DIALOG_ID;
import static linksawakening.world.room.RoomSessionRules.OCARINA_MARIN_DIALOG_TABLE;
import static linksawakening.world.room.RoomSessionRules.OCARINA_NO_SONG_DIALOG_ID;
import static linksawakening.world.room.RoomSessionRules.OCARINA_NO_SONG_DIALOG_TABLE;
import static linksawakening.world.room.RoomSessionRules.ROOM_OW_MYSTERIOUS_WOODS_LOST;
import static linksawakening.world.room.RoomSessionRules.ROOM_OW_TAIL_KEY_CHEST;
import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_DISCOVERED;
import static linksawakening.world.room.RoomSessionRules.STAIRCASE_ACTIVE;
import static linksawakening.world.room.RoomSessionRules.STAIRCASE_INACTIVE;
import static linksawakening.world.room.RoomSessionRules.STAIRCASE_NONE;

import linksawakening.entity.Link;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

/** Room replacement and scroll setup for GameplayWorldLoadRoomHandler ($01:442B). */
final class RoomTransitionController implements RoomTransitions, RoomBoundaryOperations {
    private final LoadedRoomHandle rooms;

    @Override
    public void applyWarp(Warp warp, Link link) {
        var destination = dependencies.roomLoadCoordinator().acquireWarp(warp).room;
        link.motion()
                .setRoomPhysicsForRoom(
                        dependencies.roomSessionViews().roomLoadToken(),
                        destination.mapId(),
                        destination.roomId(),
                        destination.mapCategory());
        link.position().setRoomEntryPixelPosition(warp.javaPixelX(), warp.javaPixelY());
    }

    @Override
    public boolean handlePendingInstrumentTransition(RoomTransitionCoordinator coordinator, Link link) {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null && coordinator.handlePendingInstrumentTransition(loaded, link);
    }

    @Override
    public boolean handlePendingManboTransition(RoomTransitionCoordinator coordinator, Link link) {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null && coordinator.handlePendingManboTransition(loaded, link);
    }

    @Override
    public void handleOverworldBoundary(RoomTransitionCoordinator coordinator, Link link) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded != null) coordinator.handleOverworldBoundary(loaded, link);
    }

    @Override
    public void handleWarpAndIndoorBoundaries(RoomTransitionCoordinator coordinator, Link link) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded != null) coordinator.handleWarpAndIndoorBoundaries(loaded, link);
    }

    @Override
    public Warp pollStaircaseWarp(int x, int y, int z, boolean carrying) {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null ? pollStaircaseWarp(loaded, x, y, z, carrying) : null;
    }

    @Override
    public Warp consumeInstrumentTransitionRequest() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null ? consumeInstrumentTransitionRequest(loaded) : null;
    }

    @Override
    public Warp consumeManboPondTransitionRequest() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null ? consumeManboPondTransitionRequest(loaded) : null;
    }

    @Override
    public boolean startOcarina(int countdown, int flags, int song) {
        RoomRuntime loaded = rooms.currentOrNull();
        RoomTransitionController.validateOcarina(countdown, flags, song);
        return loaded != null && beginOcarina(countdown, flags, song);
    }

    @Override
    public void startAdjacentIndoorScroll(
            linksawakening.world.transition.ScrollController scroll, int direction, int x, int y) {
        RoomRuntime loaded = rooms.currentOrNull();
        startAdjacentIndoorScroll(java.util.Objects.requireNonNull(loaded), scroll, direction, x, y);
    }

    @Override
    public void startAdjacentOverworldScroll(
            linksawakening.world.transition.ScrollController scroll, int direction, int x, int y) {
        RoomRuntime loaded = rooms.currentOrNull();
        startAdjacentOverworldScroll(java.util.Objects.requireNonNull(loaded), scroll, direction, x, y);
    }

    @Override
    public void loadWarpDestination(Warp warp) {
        dependencies.roomLoadCoordinator().acquireWarp(warp);
    }

    @Override
    public int tickDungeonTitleMessage(
            boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (dependencies.puzzles().dungeonTitleMessageCountdown == 0 || dialogActive || !inventoryClosed) return -1;
        if (--dependencies.puzzles().dungeonTitleMessageCountdown != 0 || linkMotionState != 0 || freeMovement)
            return -1;
        return 0x56 + java.util.Objects.requireNonNull(loaded).room.mapId();
    }

    @Override
    public RoomTransitionCoordinator coordinator(
            linksawakening.world.transition.RoomBoundaryController boundary,
            linksawakening.world.transition.TransitionController transition,
            ScrollController scroll,
            linksawakening.gameplay.GameplaySoundSink sound) {
        return new RoomTransitionCoordinator(
                new RoomTransitionCoordinator.Dependencies(
                        dependencies.roomCollisionQueries(),
                        dependencies.roomSessionViews(),
                        dependencies.roomStoryState(),
                        this),
                this,
                boundary,
                transition,
                scroll,
                sound);
    }

    private final Dependencies dependencies;

    RoomTransitionController(LoadedRoomHandle rooms, Dependencies dependencies) {
        this.rooms = java.util.Objects.requireNonNull(rooms);
        this.dependencies = dependencies;
    }

    void startAdjacentOverworldScroll(
            RoomRuntime loaded, ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = loaded.renderSnapshot();
        int nextRoomId = this.adjacentOverworldRoomId(loaded, direction);
        boolean discoveredTailKeyRoom = nextRoomId == ROOM_OW_TAIL_KEY_CHEST
                && direction == ScrollController.UP
                && (dependencies.roomState().overworldRoomStatus[nextRoomId] & ROOM_STATUS_DISCOVERED) == 0;
        if (discoveredTailKeyRoom) {
            dependencies.roomState().overworldRoomStatus[nextRoomId] |= (byte) ROOM_STATUS_DISCOVERED;
        }
        dependencies.roomLoadCoordinator().loadRoomSpecificTilesIfNeeded(nextRoomId);
        dependencies.roomLoadCoordinator().acquireLoadOverworld(nextRoomId, true);
        if (discoveredTailKeyRoom) {
            dependencies
                    .entityRuntimeState()
                    .outbox
                    .pendingRoomEntityEvents
                    .add(new EntityCombatEvent(
                            0, ENTITY_OWL_EVENT, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x02));
        }
        scrollController.start(
                direction, linkScreenX, linkScreenY, previousRoom, RoomInteractionGeometry.scrollTarget(direction));
    }

    void startMysteriousWoodsLostScroll(
            RoomRuntime loaded, ScrollController scrollController, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = loaded.renderSnapshot();
        dependencies.roomLoadCoordinator().loadRoomSpecificTilesIfNeeded(ROOM_OW_MYSTERIOUS_WOODS_LOST);
        dependencies.roomLoadCoordinator().acquireLoadOverworld(ROOM_OW_MYSTERIOUS_WOODS_LOST, true);
        scrollController.start(
                ScrollController.UP,
                linkScreenX,
                linkScreenY,
                previousRoom,
                RoomInteractionGeometry.scrollTarget(ScrollController.UP));
        dependencies
                .entityRuntimeState()
                .outbox
                .pendingRoomEntityEvents
                .add(new EntityCombatEvent(0, ENTITY_TARIN, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x1E));
    }

    void startAdjacentIndoorScroll(
            RoomRuntime loaded, ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = loaded.renderSnapshot();
        RoomScrollNavigation.IndoorRoomDestination destination = this.adjacentIndoorRoom(loaded, direction);
        dependencies
                .roomLoadCoordinator()
                .loadIndoor(
                        RoomLoadCoordinator.PreviousPalettes.from(loaded),
                        loaded.room.mapId(),
                        destination.roomId(),
                        loaded.room.mapCategory(),
                        switch (direction) {
                            case ScrollController.RIGHT -> 0;
                            case ScrollController.LEFT -> 1;
                            case ScrollController.UP -> 2;
                            case ScrollController.DOWN -> 3;
                            default -> throw new IllegalArgumentException("Indoor scroll direction " + direction);
                        });
        dependencies.location().indoorMapPosition = destination.mapPosition();
        scrollController.start(
                direction, linkScreenX, linkScreenY, previousRoom, RoomInteractionGeometry.scrollTarget(direction));
    }

    boolean beginOcarina(int countdown, int songFlags, int selectedSong) {
        if (dependencies.items().ocarinaPlaybackCountdown != 0) {
            return false;
        }
        dependencies.items().ocarinaPlaybackCountdown = countdown;
        dependencies.items().ocarinaSongFlags = songFlags & 0xFF;
        dependencies.items().selectedSongIndex = selectedSong & 0xFF;
        dependencies.items().ocarinaAnimationCounter = 0;
        dependencies.items().ocarinaAnimationPhase = 0;
        dependencies.items().pendingManboTransition = false;
        dependencies.items().pendingInstrumentTransition = false;
        return true;
    }

    @Override
    public boolean ocarinaPlaying() {
        return dependencies.items().ocarinaPlaybackCountdown != 0;
    }

    @Override
    public int ocarinaAnimationState() {
        if (!this.ocarinaPlaying()) {
            return -1;
        }
        return dependencies.items().ocarinaAnimationPhase == 0 ? 0x76 : 0x75;
    }

    Warp consumeManboPondTransitionRequest(RoomRuntime loaded) {
        if (!dependencies.items().pendingManboTransition) {
            return null;
        }
        dependencies.items().pendingManboTransition = false;
        return dependencies.manboWarpResolver().resolve(loaded.room.mapCategory(), loaded.room.mapId());
    }

    Warp consumeInstrumentTransitionRequest(RoomRuntime loaded) {
        if (!dependencies.items().pendingInstrumentTransition || !loaded.room.hasWarps()) {
            return null;
        }
        dependencies.items().pendingInstrumentTransition = false;
        return loaded.room.firstWarp();
    }

    Warp consumeClearedDungeonWarp() {
        Warp pending = dependencies.location().pendingDungeonWarp;
        dependencies.location().pendingDungeonWarp = null;
        return pending;
    }

    void finishOcarinaPlayback(RoomRuntime loaded) {
        if (dependencies.followers().followingNpcState.marinFollowing()) {
            if (dependencies.items().selectedSong() == linksawakening.state.OcarinaSong.MANBO_MAMBO) {
                dependencies.items().pendingManboTransition = true;
            } else if (loaded.room.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                dependencies
                        .entityRuntimeState()
                        .outbox
                        .pendingRoomDialogRequests
                        .add(new DialogRequest(OCARINA_MARIN_DIALOG_TABLE, OCARINA_MARIN_DIALOG_ID));
            }
            return;
        }
        if (dependencies.items().ocarinaSongFlags == 0) {
            dependencies
                    .entityRuntimeState()
                    .outbox
                    .pendingRoomDialogRequests
                    .add(new DialogRequest(OCARINA_NO_SONG_DIALOG_TABLE, OCARINA_NO_SONG_DIALOG_ID));
        } else if (dependencies.items().selectedSong() == linksawakening.state.OcarinaSong.MANBO_MAMBO) {
            dependencies.items().pendingManboTransition = true;
        }
    }

    void tickOcarinaAnimationHandler() {
        if (!this.ocarinaPlaying()) {
            return;
        }
        dependencies.items().ocarinaAnimationCounter++;
        if (dependencies.items().ocarinaAnimationCounter >= 0x38) {
            dependencies.items().ocarinaAnimationCounter = 0;
            dependencies.items().ocarinaAnimationPhase ^= 0x01;
        }
    }

    Warp pollStaircaseWarp(RoomRuntime loaded, int linkEntityX, int linkEntityY, int linkZ, boolean carryingObject) {
        if (loaded.contents.puzzles.staircaseState == STAIRCASE_NONE) {
            return null;
        }
        if (loaded.contents.puzzles.staircaseState == STAIRCASE_INACTIVE) {
            if (!RoomInteractionGeometry.withinUnsignedByteRange(
                            linkEntityX, loaded.contents.puzzles.staircaseX, 0x06, 0x0C)
                    || !RoomInteractionGeometry.withinUnsignedByteRange(
                            linkEntityY, loaded.contents.puzzles.staircaseY, 0x06, 0x0C)) {
                loaded.contents.puzzles.staircaseState = STAIRCASE_ACTIVE;
            }
            return null;
        }
        if (linkZ != 0
                || carryingObject
                || !RoomInteractionGeometry.withinUnsignedByteRange(
                        linkEntityX, loaded.contents.puzzles.staircaseX, 0x05, 0x0A)
                || !RoomInteractionGeometry.withinUnsignedByteRange(
                        linkEntityY, loaded.contents.puzzles.staircaseY, 0x05, 0x0A)
                || !loaded.room.hasWarps()) {
            return null;
        }
        loaded.contents.puzzles.staircaseState = STAIRCASE_NONE;
        return loaded.room.firstWarp();
    }

    void configureStaircase(RoomRuntime loaded, int location) {
        loaded.contents.puzzles.configureStaircase(location);
    }

    int adjacentOverworldRoomId(RoomRuntime loaded, int direction) {
        return dependencies.scrollNavigation().adjacentOverworldRoomId(loaded.room, direction);
    }

    RoomScrollNavigation.IndoorRoomDestination adjacentIndoorRoom(RoomRuntime loaded, int direction) {
        return dependencies
                .scrollNavigation()
                .adjacentIndoorRoom(loaded.room, dependencies.location().indoorMapPosition, direction);
    }

    record Dependencies(
            RoomFollowerState followers,
            RoomPuzzleState puzzles,
            RoomCollisionQueries roomCollisionQueries,
            RoomSessionViews roomSessionViews,
            RoomStoryState roomStoryState,
            RoomItemUseState items,
            RoomLocationState location,
            RoomEntitySessionState entityRuntimeState,
            ManboWarpResolver manboWarpResolver,
            RoomLoadCoordinator roomLoadCoordinator,
            RoomWorldState roomState,
            RoomScrollNavigation scrollNavigation) {
        Dependencies {
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(puzzles, "puzzles");
            java.util.Objects.requireNonNull(roomCollisionQueries, "roomCollisionQueries");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomStoryState, "roomStoryState");
            java.util.Objects.requireNonNull(items, "items");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(manboWarpResolver, "manboWarpResolver");
            java.util.Objects.requireNonNull(roomLoadCoordinator, "roomLoadCoordinator");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(scrollNavigation, "scrollNavigation");
        }
    }

    static void validateOcarina(int countdown, int songFlags, int selectedSong) {
        if (countdown < 0 || countdown > 0xFF) {
            throw new IllegalArgumentException("Ocarina countdown must be an unsigned byte: " + countdown);
        }
        if (songFlags < 0 || songFlags > 0xFF) {
            throw new IllegalArgumentException("Ocarina song flags must be an unsigned byte: " + songFlags);
        }
        if (selectedSong < 0 || selectedSong > 0xFF) {
            throw new IllegalArgumentException("Selected Ocarina song must be an unsigned byte: " + selectedSong);
        }
    }
}
