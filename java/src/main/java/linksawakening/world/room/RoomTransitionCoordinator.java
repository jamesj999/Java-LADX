package linksawakening.world.room;

import java.util.Objects;
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
        this.soundSink = Objects.requireNonNull(soundSink);
        this.dependencies = Objects.requireNonNull(dependencyValues);
        this.boundaryOperations = Objects.requireNonNull(boundaryOperations);
        this.boundaryController = Objects.requireNonNull(boundaryController);
        this.transitionController = Objects.requireNonNull(transitionController);
        this.scrollController = Objects.requireNonNull(scrollController);
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
        ActiveRoom room = loaded.room();

        if (room.mapCategory() == Warp.CATEGORY_OVERWORLD
                && Warp.isOverworldWarpHoleRoom(room.roomId())
                && link.airborne().warpHoleReady()) {
            Warp target = dependencies.roomTransitionController().overworldWarpHoleDestination(room.roomId());
            link.position().setPixelPosition(target.javaPixelX(), target.javaPixelY());
            transitionController.startFadeOut(() -> {
                applyWarp(target, link);
                link.airborne().startWarpHoleLanding();
            });
            return;
        }
        if (link.airborne().warpHoleControlsLink()) return;

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
            startEntranceWarp(staircaseWarp, link, fadesMusicOnStairs(room));
            return;
        }

        // LinkMotionMapFadeOutHandler selects a record only after
        // ApplyCollisionWithDoor has requested the fade ($02:71BB).
        if (!link.position().consumeDoorWarp()) return;
        if (room.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            startEntranceWarp(room.firstWarp(), link, fadesMusicOnStairs(room));
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
                startEntranceWarp(target, link, true);
                return;
            }
        }
    }

    void handleOverworldBoundary(RoomRuntime loaded, Link link) {
        if (link.airborne().warpHoleControlsLink()) return;

        int previousX = link.position().pixelX();
        int previousY = link.position().pixelY();
        RoomBoundaryDecision boundary = boundaryController.decide(dependencies
                .roomCollisionQueries()
                .boundaryView(loaded, previousX, previousY, link.airborne().isAirborne()));
        if (boundary.type() == RoomBoundaryDecision.Type.OVERWORLD_SCROLL) {
            startAdjacentScroll(loaded, link, boundary);
        } else if (boundary.type() == RoomBoundaryDecision.Type.CLAMP_LINK) {
            link.position().setPixelPosition(boundary.linkTargetX(), boundary.linkTargetY());
        }
    }

    private void startAdjacentScroll(RoomRuntime loaded, Link link, RoomBoundaryDecision boundary) {
        var position = link.position();
        int deltaX = boundary.linkTargetX() - position.pixelX();
        int deltaY = boundary.linkTargetY() - position.pixelY();
        // $02:6D5C restores the saved integer bytes. $02:790B uses the current fractions.
        position.restoreRomFinalPositionPixels();
        int previousX = position.pixelX();
        int previousY = position.pixelY();
        position.setRoomScrollEntryPixelPosition(previousX + deltaX, previousY + deltaY);
        var transitions = dependencies.roomTransitionController();
        int direction = boundary.direction();
        if (boundary.type() == RoomBoundaryDecision.Type.INDOOR_SCROLL) {
            transitions.startAdjacentIndoorScroll(loaded, scrollController, direction, previousX, previousY);
        } else {
            // The source creates the follower after the scroll, from the destination coordinates.
            var story = dependencies.roomStoryState();
            story.rememberFollowingNpcPosition(position.romEntityX(), position.romEntityY());
            if (direction == ScrollController.UP && story.shouldGetLostInMysteriousWoods()) {
                transitions.startMysteriousWoodsLostScroll(loaded, scrollController, previousX, previousY);
            } else {
                transitions.startAdjacentOverworldScroll(loaded, scrollController, direction, previousX, previousY);
            }
        }
        scrollController.onCompletion(() -> link.motion().finishRoomScroll(direction == ScrollController.DOWN));
    }

    /** Starts a pending Ocarina Mambo transition after entity playback ends. */
    boolean handlePendingManboTransition(RoomRuntime loaded, Link link) {
        if (link == null || transitionController.isActive() || link.airborne().warpHoleControlsLink()) {
            return false;
        }
        Warp target = dependencies.roomTransitionController().consumeManboPondTransitionRequest(loaded);
        if (target == null) {
            return false;
        }
        transitionController.startManboIn(target.category() != Warp.CATEGORY_OVERWORLD, () -> applyWarp(target, link));
        return true;
    }

    /** Starts disableMovementInTransition after the Sirens Instrument effect. */
    boolean handlePendingInstrumentTransition(RoomRuntime loaded, Link link) {
        if (link == null || transitionController.isActive() || link.airborne().warpHoleControlsLink()) {
            return false;
        }
        Warp target = dependencies.roomTransitionController().consumeInstrumentTransitionRequest(loaded);
        if (target == null) {
            return false;
        }
        transitionController.startFadeOut(() -> applyWarp(target, link));
        return true;
    }

    private static boolean fadesMusicOnStairs(ActiveRoom room) {
        return room.mapCategory() == Warp.CATEGORY_OVERWORLD || room.firstWarp().category() != Warp.CATEGORY_INDOOR;
    }

    private void startEntranceWarp(Warp target, Link link, boolean fadeMusic) {
        // ApplyMapFadeOutTransitionWithNoise/WithSound both reach
        // playNoiseStairs ($00:0C9A): hNoiseSfx = NOISE_SFX_STAIRS.
        soundSink.play(GameplaySoundEvent.STAIRS);
        transitionController.startDoorFade(fadeMusic, () -> applyWarp(target, link));
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
            Objects.requireNonNull(roomCollisionQueries, "roomCollisionQueries");
            Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            Objects.requireNonNull(roomStoryState, "roomStoryState");
            Objects.requireNonNull(roomTransitionController, "roomTransitionController");
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
            startEntranceWarp(target, link, true);
            return true;
        }
        if (boundary.type() == RoomBoundaryDecision.Type.SIDE_SCROLL_VERTICAL_WARP && room.hasWarps()) {
            Warp target = room.firstWarp();
            startEntranceWarp(target, link, true);
            return true;
        }
        if (boundary.type() == RoomBoundaryDecision.Type.INDOOR_SCROLL) {
            startAdjacentScroll(loaded, link, boundary);
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
