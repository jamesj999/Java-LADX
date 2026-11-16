package linksawakening.world.room;

import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.transition.RoomBoundaryController;
import linksawakening.world.transition.RoomBoundaryDecision;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;

/** Orders ApplyCollisionWithDoor and LinkMotionMapFadeOutHandler before fallback edge clamping. */
public final class RoomTransitionCoordinator {
    private final Dependencies dependencies;
    private final RoomBoundaryOperations boundaryOperations;
    private final RoomBoundaryController boundaryController;
    private final TransitionController transitionController;
    private final ScrollController scrollController;
    private final GameplaySoundSink soundSink;

    RoomTransitionCoordinator(
            Dependencies dependencyValues,
            RoomBoundaryOperations boundaryOperations,
            RoomBoundaryController boundaryController,
            TransitionController transitionController,
            ScrollController scrollController,
            GameplaySoundSink soundSink) {
        this.soundSink = java.util.Objects.requireNonNull(soundSink);
        this.dependencies = java.util.Objects.requireNonNull(dependencyValues);
        this.boundaryOperations = java.util.Objects.requireNonNull(boundaryOperations);
        this.boundaryController = java.util.Objects.requireNonNull(boundaryController);
        this.transitionController = java.util.Objects.requireNonNull(transitionController);
        this.scrollController = java.util.Objects.requireNonNull(scrollController);
    }

    /**
     * Warp-transition check runs before overworld edge scroll/clamp. Indoor
     * rooms need this ordering because their south-edge front-door trigger must
     * see Link past the bottom boundary before any fallback clamp can run.
     */
    void handleWarpAndIndoorBoundaries(RoomRuntime loaded, Link link) {
        if (transitionController.isActive()) {
            return;
        }
        ActiveRoom room = loaded.room;

        // $02:5136-5167 uses wWarp0, independent of the door warp positions.
        if (link.status().pitWarpPending() && room.hasWarps()) {
            link.status().consumePitWarp();
            Warp source = room.firstWarp();
            boolean sideView = source.category() == Warp.CATEGORY_SIDESCROLL;
            Warp target = new Warp(
                    source.category(),
                    source.destMap(),
                    source.destRoom(),
                    sideView ? link.position().romEntityY() : (link.position().romEntityX() & 0xF0) + 8,
                    sideView ? source.destY() : link.position().romEntityY() & 0xF0,
                    source.tileLocation());
            transitionController.startFadeOut(() -> {
                applyWarp(target, link);
                link.airborne().setPitWarpArrival(sideView ? 0 : 0x70);
            });
            return;
        }
        if (link.airborne().isFallingIntoPit()) return;

        if (room.mapCategory() != Warp.CATEGORY_OVERWORLD && handleIndoorBoundary(loaded, link, room)) return;

        if (!room.hasWarps()) {
            return;
        }

        Warp staircaseWarp = dependencies
                .roomTransitionController()
                .pollStaircaseWarp(
                        loaded,
                        link.position().romEntityX(),
                        link.position().romEntityY(),
                        link.animation().romEntityZ(),
                        link.actions().isCarryingLiftedObject());
        if (staircaseWarp != null) {
            startEntranceWarp(staircaseWarp, link);
            return;
        }

        // LinkMotionMapFadeOutHandler selects a record only after
        // ApplyCollisionWithDoor has requested the fade ($02:71BB).
        if (!link.position().consumeDoorWarp()) return;
        if (room.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            startEntranceWarp(room.firstWarp(), link);
            return;
        }
        int linkTile =
                Warp.packTileLocation(link.position().pixelX(), link.position().pixelY());
        for (Warp warp : room.warps()) {
            if (warp.tileLocation() == linkTile) {
                final Warp target = warp;
                System.out.println("Warp trigger: tile=" + String.format("%02X", linkTile)
                        + " → cat=" + target.category()
                        + " map=" + String.format("%02X", target.destMap())
                        + " room=" + String.format("%02X", target.destRoom())
                        + " at (" + target.destX() + "," + target.destY() + ")");
                startEntranceWarp(target, link);
                return;
            }
        }
    }

    void handleOverworldBoundary(RoomRuntime loaded, Link link) {

        int previousX = link.position().pixelX();
        int previousY = link.position().pixelY();
        RoomBoundaryDecision boundary = boundaryController.decide(dependencies
                .roomCollisionQueries()
                .boundaryView(loaded, previousX, previousY, link.airborne().isAirborne()));
        if (boundary.type() == RoomBoundaryDecision.Type.OVERWORLD_SCROLL) {
            link.position().setRoomScrollEntryPixelPosition(boundary.linkTargetX(), boundary.linkTargetY());
            if (boundary.direction() == ScrollController.UP
                    && dependencies.roomStoryState().shouldGetLostInMysteriousWoods()) {
                dependencies
                        .roomTransitionController()
                        .startMysteriousWoodsLostScroll(loaded, scrollController, previousX, previousY);
            } else {
                dependencies
                        .roomTransitionController()
                        .startAdjacentOverworldScroll(
                                loaded, scrollController, boundary.direction(), previousX, previousY);
            }
            scrollController.onCompletion(
                    () -> link.motion().finishRoomScroll(boundary.direction() == ScrollController.DOWN));
        } else if (boundary.type() == RoomBoundaryDecision.Type.CLAMP_LINK) {
            link.position().setPixelPosition(boundary.linkTargetX(), boundary.linkTargetY());
        }
    }

    /** Starts a pending Ocarina Mambo transition after entity playback ends. */
    boolean handlePendingManboTransition(RoomRuntime loaded, Link link) {
        if (link == null || transitionController.isActive()) {
            return false;
        }
        Warp target = dependencies.roomTransitionController().consumeManboPondTransitionRequest(loaded);
        if (target == null) {
            return false;
        }
        transitionController.startManboIn(() -> applyWarp(target, link));
        return true;
    }

    /** Starts disableMovementInTransition after the Sirens Instrument effect. */
    boolean handlePendingInstrumentTransition(RoomRuntime loaded, Link link) {
        if (link == null || transitionController.isActive()) {
            return false;
        }
        Warp target = dependencies.roomTransitionController().consumeInstrumentTransitionRequest(loaded);
        if (target == null) {
            return false;
        }
        transitionController.startFadeOut(() -> applyWarp(target, link));
        return true;
    }

    private void startEntranceWarp(Warp target, Link link) {
        // ApplyMapFadeOutTransitionWithNoise/WithSound both reach
        // playNoiseStairs ($00:0C9A): hNoiseSfx = NOISE_SFX_STAIRS.
        soundSink.play(GameplaySoundEvent.STAIRS);
        transitionController.startFadeOut(() -> applyWarp(target, link));
    }

    private void applyWarp(Warp warp, Link link) {
        boundaryOperations.applyWarp(warp, link);
    }

    record Dependencies(
            RoomCollisionQueries roomCollisionQueries,
            RoomSessionViews roomSessionViews,
            RoomStoryState roomStoryState,
            RoomTransitionController roomTransitionController) {
        Dependencies {
            java.util.Objects.requireNonNull(roomCollisionQueries, "roomCollisionQueries");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomStoryState, "roomStoryState");
            java.util.Objects.requireNonNull(roomTransitionController, "roomTransitionController");
        }
    }

    private boolean handleIndoorBoundary(RoomRuntime loaded, Link link, ActiveRoom room) {

        link.motion()
                .initializeRoomPhysicsForRoom(
                        dependencies.roomSessionViews().roomLoadToken(),
                        room.mapId(),
                        room.roomId(),
                        room.mapCategory());
        Warp dungeonWarp = dependencies.roomTransitionController().consumeClearedDungeonWarp();
        if (dungeonWarp != null) {
            transitionController.startFadeOut(() -> applyWarp(dungeonWarp, link));
            return true;
        }
        if (link.airborne().romMotionState() == 0x05) return true;
        if (link.motion().consumeRevolvingDoorTransition()) {
            int previousX = link.position().pixelX();
            int previousY = link.position().pixelY();
            // $02:790B: 32 scroll steps * $3A / 16 = 116 pixels.
            link.position().setRoomEntryPixelPosition(previousX, previousY + 116);
            dependencies
                    .roomTransitionController()
                    .startAdjacentIndoorScroll(loaded, scrollController, ScrollController.UP, previousX, previousY);
            scrollController.onCompletion(() -> link.motion().finishRoomScroll(false));
            return true;
        }
        RoomBoundaryDecision boundary = boundaryController.decide(dependencies
                .roomCollisionQueries()
                .boundaryView(
                        loaded,
                        link.position().pixelX(),
                        link.position().pixelY(),
                        link.airborne().isAirborne(),
                        link.motion().romPhysicsModifier()));
        if (boundary.type() == RoomBoundaryDecision.Type.INDOOR_FRONT_DOOR_WARP) {
            Warp target = room.firstWarp();
            System.out.println("Indoor front-door exit → cat=" + target.category()
                    + " map=" + String.format("%02X", target.destMap())
                    + " room=" + String.format("%02X", target.destRoom())
                    + " at (" + target.destX() + "," + target.destY() + ")");
            startEntranceWarp(target, link);
            return true;
        }
        if (boundary.type() == RoomBoundaryDecision.Type.SIDE_SCROLL_VERTICAL_WARP && room.hasWarps()) {
            Warp target = room.firstWarp();
            transitionController.startFadeOut(() -> applyWarp(target, link));
            return true;
        }
        if (boundary.type() == RoomBoundaryDecision.Type.INDOOR_SCROLL) {
            int previousX = link.position().pixelX();
            int previousY = link.position().pixelY();
            link.position().setRoomEntryPixelPosition(boundary.linkTargetX(), boundary.linkTargetY());
            dependencies
                    .roomTransitionController()
                    .startAdjacentIndoorScroll(loaded, scrollController, boundary.direction(), previousX, previousY);
            scrollController.onCompletion(
                    () -> link.motion().finishRoomScroll(boundary.direction() == ScrollController.DOWN));
            return true;
        }
        return false;
    }

    public void handleWarpAndIndoorBoundaries(Link link) {
        boundaryOperations.handleWarpAndIndoorBoundaries(this, link);
    }

    public void handleOverworldBoundary(Link link) {
        boundaryOperations.handleOverworldBoundary(this, link);
    }

    public boolean handlePendingManboTransition(Link link) {
        return boundaryOperations.handlePendingManboTransition(this, link);
    }

    public boolean handlePendingInstrumentTransition(Link link) {
        return boundaryOperations.handlePendingInstrumentTransition(this, link);
    }
}
