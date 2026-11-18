package linksawakening.world;

import linksawakening.entity.Link;
import linksawakening.entity.EntitySpriteHandlerCatalog;

public final class RoomTransitionCoordinator {
    private final RoomSession roomSession;
    private final RoomBoundaryController boundaryController;
    private final TransitionController transitionController;
    private final ScrollController scrollController;

    private ActiveRoom portalRoom;
    private boolean portalArmed;
    private int portalCountdown;
    private int suppressedWarpTile = -1;

    public RoomTransitionCoordinator(RoomSession roomSession,
                                     RoomBoundaryController boundaryController,
                                     TransitionController transitionController,
                                     ScrollController scrollController) {
        this.roomSession = roomSession;
        this.boundaryController = boundaryController;
        this.transitionController = transitionController;
        this.scrollController = scrollController;
    }

    /**
     * Warp-transition check runs before overworld edge scroll/clamp. Indoor
     * rooms need this ordering because their south-edge front-door trigger must
     * see Link past the bottom boundary before any fallback clamp can run.
     */
    public void handleWarpAndIndoorBoundaries(Link link) {
        if (!roomSession.hasActiveRoom()
            || transitionController.isActive()) {
            return;
        }
        ActiveRoom room = roomSession.activeRoom();

        if (link.warpHoleReady()) {
            Warp target = roomSession.nextVisitedWarpHole();
            link.setPixelPosition(target.javaPixelX(), target.javaPixelY());
            transitionController.startFadeOut(() -> {
                applyWarp(target, link);
                link.startWarpHoleLanding();
            });
            return;
        }
        if (link.warpHoleControlsLink()) return;
        if (room.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            RoomBoundaryDecision boundary = boundaryController.decide(
                roomSession.boundaryState(link.pixelX(), link.pixelY()));
            if (boundary.type() == RoomBoundaryDecision.Type.INDOOR_FRONT_DOOR_WARP) {
                Warp target = room.firstWarp();
                System.out.println("Indoor front-door exit → cat=" + target.category()
                    + " map=" + String.format("%02X", target.destMap())
                    + " room=" + String.format("%02X", target.destRoom())
                    + " at (" + target.destX() + "," + target.destY() + ")");
                startDoorWarp(target, link, true);
                return;
            }
            if (boundary.type() == RoomBoundaryDecision.Type.SIDE_SCROLL_VERTICAL_WARP
                && room.hasWarps()) {
                Warp target = room.firstWarp();
                startDoorWarp(target, link, true);
                return;
            }
            if (boundary.type() == RoomBoundaryDecision.Type.INDOOR_SCROLL) {
                int previousX = link.pixelX();
                int previousY = link.pixelY();
                link.setRoomEntryPixelPosition(boundary.linkTargetX(), boundary.linkTargetY());
                roomSession.startAdjacentIndoorScroll(scrollController, boundary.direction(), previousX, previousY);
                suppressedWarpTile = Warp.packTileLocation(boundary.linkTargetX(), boundary.linkTargetY());
                return;
            }
        }

        if (!room.hasWarps()) {
            return;
        }

        Warp staircaseWarp = roomSession.pollStaircaseWarp(
            link.romEntityX(), link.romEntityY(), link.romEntityZ(),
            link.isCarryingLiftedObject());
        if (staircaseWarp != null) {
            startDoorWarp(staircaseWarp, link, fadesMusicOnStairs(room));
            return;
        }

        int linkTile = Warp.packTileLocation(link.pixelX(), link.pixelY());
        if (linkTile != suppressedWarpTile) {
            suppressedWarpTile = -1;
        }
        if (linkTile == suppressedWarpTile) {
            return;
        }
        for (Warp warp : room.warps()) {
            if (warp.tileLocation() == linkTile && roomSession.canEnterWarpTile(link, linkTile)) {
                final Warp target = warp;
                System.out.println("Warp trigger: tile=" + String.format("%02X", linkTile)
                    + " → cat=" + target.category()
                    + " map=" + String.format("%02X", target.destMap())
                    + " room=" + String.format("%02X", target.destRoom())
                    + " at (" + target.destX() + "," + target.destY() + ")");
                startDoorWarp(target, link, fadesMusicOnStairs(room));
                return;
            }
        }
    }

    public void handleOverworldBoundary(Link link) {
        if (!roomSession.hasActiveRoom()) {
            return;
        }

        int previousX = link.pixelX();
        int previousY = link.pixelY();
        RoomBoundaryDecision boundary = boundaryController.decide(
            roomSession.boundaryState(previousX, previousY));
        if (boundary.type() == RoomBoundaryDecision.Type.OVERWORLD_SCROLL) {
            link.setRoomEntryPixelPosition(boundary.linkTargetX(), boundary.linkTargetY());
            if (boundary.direction() == ScrollController.UP
                && roomSession.shouldGetLostInMysteriousWoods()) {
                roomSession.startMysteriousWoodsLostScroll(
                    scrollController, previousX, previousY);
            } else {
                roomSession.startAdjacentOverworldScroll(
                    scrollController, boundary.direction(), previousX, previousY);
            }
        } else if (boundary.type() == RoomBoundaryDecision.Type.CLAMP_LINK) {
            link.setPixelPosition(boundary.linkTargetX(), boundary.linkTargetY());
        }
    }

    public boolean dungeonPortalControlsLink() {
        return portalCountdown > 0;
    }

    public void tickDungeonPortal(Link link, int frame) {
        ActiveRoom room = roomSession.activeRoom();
        if (room != portalRoom) {
            portalRoom = room;
            portalArmed = false;
            portalCountdown = 0;
        }
        if (room == null || room.entities() == null || transitionController.isActive()) return;
        Warp target = roomSession.dungeonPortalDestination();
        if (target == null) return;
        for (RoomEntity entity : room.entities().loadedEntities()) {
            if (entity.type() != EntitySpriteHandlerCatalog.ENTITY_WARP) continue;
            int dx = link.romEntityX() - entity.x();
            int dy = link.romEntityY() - entity.y();
            if (portalCountdown > 0) {
                if (--portalCountdown > 0) {
                    link.spinOnDungeonPortal(entity.x(), entity.y(), frame);
                } else {
                    portalArmed = false;
                    transitionController.startFadeOut(() -> applyWarp(target, link));
                }
            } else if (!portalArmed) {
                portalArmed = dx < -4 || dx >= 4 || dy < -4 || dy >= 4;
            } else if (link.romEntityZ() == 0 && dx >= -3 && dx < 3 && dy >= -3 && dy < 3) {
                link.beginDungeonPortal();
                portalCountdown = 0x50;
            }
            return;
        }
    }

    public void handleRichardPassage(Link link) {
        if (transitionController.isActive()) return;
        Warp target = roomSession.takeRichardPassage();
        if (target != null) transitionController.startFadeOut(() -> applyWarp(target, link));
    }

    /** Starts a pending Ocarina Mambo transition after entity playback ends. */
    public boolean handlePendingManboTransition(Link link) {
        if (link == null || !roomSession.hasActiveRoom() || transitionController.isActive()) {
            return false;
        }
        Warp target = roomSession.consumeManboPondTransitionRequest();
        if (target == null) {
            return false;
        }
        transitionController.startManboIn(target.category() != Warp.CATEGORY_OVERWORLD, () -> applyWarp(target, link));
        return true;
    }

    /** Starts disableMovementInTransition after the Sirens Instrument effect. */
    public boolean handlePendingInstrumentTransition(Link link) {
        if (link == null || !roomSession.hasActiveRoom() || transitionController.isActive()) {
            return false;
        }
        Warp target = roomSession.consumeInstrumentTransitionRequest();
        if (target == null) {
            return false;
        }
        transitionController.startFadeOut(() -> applyWarp(target, link));
        return true;
    }

    private static boolean fadesMusicOnStairs(ActiveRoom room) {
        // ApplyMapFadeOutTransitionWithSound checks wWarp0MapCategory before wIsIndoor.
        return room.mapCategory() == Warp.CATEGORY_OVERWORLD || room.firstWarp().category() != Warp.CATEGORY_INDOOR;
    }

    private void startDoorWarp(Warp target, Link link, boolean fadeMusic) {
        roomSession.playDoorSound();
        transitionController.startDoorFade(fadeMusic, () -> applyWarp(target, link));
    }

    private void applyWarp(Warp warp, Link link) {
        // hFreeWarpDataAddress holds the source wIsIndoor at $18B0.
        boolean fromOverworld = roomSession.activeRoom().mapCategory() == Warp.CATEGORY_OVERWORLD;
        if (fromOverworld != (warp.category() == Warp.CATEGORY_OVERWORLD)) link.clearActivePowerUp();
        roomSession.loadWarpDestination(warp);
        link.enterWarpView(warp.category() == Warp.CATEGORY_SIDESCROLL);
        int landingX = warp.javaPixelX();
        int landingY = warp.javaPixelY();
        link.setRoomEntryPixelPosition(landingX, landingY);
        suppressedWarpTile = Warp.packTileLocation(landingX, landingY);
    }
}
