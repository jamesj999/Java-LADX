package linksawakening.world;

import linksawakening.entity.Link;
import org.junit.jupiter.api.Test;

import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class RoomBoundaryControllerTest {

    private final RoomBoundaryController controller = new RoomBoundaryController();

    @Test
    void overworldLeftEdgeScrollsToPreviousRoomWhenRoomExists() {
        RoomBoundaryDecision decision = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_OVERWORLD, 0x92, false, false, -1, 40));

        assertEquals(RoomBoundaryDecision.Type.OVERWORLD_SCROLL, decision.type());
        assertEquals(ScrollController.LEFT, decision.direction());
        assertEquals(ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE, decision.linkTargetX());
        assertEquals(40, decision.linkTargetY());
    }

    @Test
    void overworldBoundaryClampsWhenNoAdjacentRoomExists() {
        RoomBoundaryDecision decision = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_OVERWORLD, 0x90, false, false, -1, 40));

        assertEquals(RoomBoundaryDecision.Type.CLAMP_LINK, decision.type());
        assertEquals(0, decision.linkTargetX());
        assertEquals(40, decision.linkTargetY());
    }

    @Test
    void indoorSouthEntranceRequestsFrontDoorWarp() {
        RoomBoundaryDecision decision = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_INDOOR, 0xA0, true, true,
                40, ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1));

        assertEquals(RoomBoundaryDecision.Type.INDOOR_FRONT_DOOR_WARP, decision.type());
    }

    @Test
    void indoorSideEdgeRequestsScrollWithinIndoorMap() {
        RoomBoundaryDecision decision = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_INDOOR, 0xA0, false, true, -1, 40));

        assertEquals(RoomBoundaryDecision.Type.INDOOR_SCROLL, decision.type());
        assertEquals(ScrollController.LEFT, decision.direction());
        assertEquals(ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE, decision.linkTargetX());
        assertEquals(40, decision.linkTargetY());
    }

    @Test
    void indoorShutterClampsTheMatchingRoomEdgeUntilItOpens() {
        RoomBoundaryDecision decision = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_INDOOR, 0x11, false, false,
                0x04, -1, 40));

        assertEquals(RoomBoundaryDecision.Type.CLAMP_LINK, decision.type());
        assertEquals(0, decision.linkTargetX());
        assertEquals(40, decision.linkTargetY());
    }

    @Test
    void airborneLinkDoesNotScrollBackUpAfterEnteringRoomDuringWallFlip() {
        RoomBoundaryDecision decision = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_INDOOR, 0xA0, false, true,
                0, 40, -1, 0x00, true));

        assertEquals(RoomBoundaryDecision.Type.NONE, decision.type());
    }

    @Test
    void airborneLinkDoesNotStartTheDownwardTransitionDuringTheFlip() {
        RoomBoundaryDecision decision = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_INDOOR, 0xA0, false, true,
                0, 40, ROOM_PIXEL_HEIGHT, 0x00, true));

        assertEquals(RoomBoundaryDecision.Type.NONE, decision.type());
    }

    @Test
    void airborneOverworldLinkDoesNotScrollBackUpAfterWallFlipEntry() {
        RoomBoundaryDecision decision = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_OVERWORLD, 0x92, false, false,
                0, 40, -1, 0x00, true));

        assertEquals(RoomBoundaryDecision.Type.NONE, decision.type());
    }

    @Test
    void airborneOverworldLinkDoesNotStartDownwardBoundaryScrollDuringTheFlip() {
        RoomBoundaryDecision decision = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_OVERWORLD, 0x82, false, false,
                0, 40, ROOM_PIXEL_HEIGHT, 0x00, true));

        assertEquals(RoomBoundaryDecision.Type.NONE, decision.type());
    }

    @Test
    void sideScrollingVerticalEdgesRequestWarpZeroInsteadOfIndoorScroll() {
        RoomBoundaryDecision top = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, 0x19, false, true,
                0, 0x70, -5, 0x00));
        RoomBoundaryDecision bottom = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, 0x19, false, true,
                0, 0x70, 0x74, 0x00));
        RoomBoundaryDecision topInside = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, 0x19, false, true,
                0, 0x70, -4, 0x00));
        RoomBoundaryDecision bottomInside = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, 0x19, false, true,
                0, 0x70, 0x73, 0x00));

        assertEquals(RoomBoundaryDecision.Type.SIDE_SCROLL_VERTICAL_WARP, top.type());
        assertEquals(RoomBoundaryDecision.Type.SIDE_SCROLL_VERTICAL_WARP, bottom.type());
        assertEquals(RoomBoundaryDecision.Type.NONE, topInside.type());
        assertEquals(RoomBoundaryDecision.Type.NONE, bottomInside.type());
    }

    @Test
    void sideScrollingWarpRoomsUseGenericVerticalWarpEdges() {
        RoomBoundaryDecision otherRoom = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, 0x1A, false, true,
                0, 0x70, -5, 0x00));
        RoomBoundaryDecision otherMap = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, 0x19, false, true,
                0, 0x70, -5, 0x01));
        RoomBoundaryDecision noWarp = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, 0x19, false, false,
                0, 0x70, -5, 0x00));
        RoomBoundaryDecision shuttered = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, 0x19, false, true,
                0x02, 0x70, 0x74, 0x00));

        assertEquals(RoomBoundaryDecision.Type.SIDE_SCROLL_VERTICAL_WARP, otherRoom.type());
        assertEquals(RoomBoundaryDecision.Type.SIDE_SCROLL_VERTICAL_WARP, otherMap.type());
        assertEquals(RoomBoundaryDecision.Type.INDOOR_SCROLL, noWarp.type());
        assertEquals(RoomBoundaryDecision.Type.CLAMP_LINK, shuttered.type());
    }

    @Test
    void tailCaveSideScrollingHorizontalEdgesUseRomEntityThresholds() {
        RoomBoundaryDecision left = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, 0x19, false, true,
                0, -5, 0x30, 0x00));
        RoomBoundaryDecision leftMargin = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, 0x19, false, true,
                0, -4, 0x30, 0x00));
        RoomBoundaryDecision rightMargin = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, 0x18, false, true,
                0, 0x93, 0x30, 0x00));
        RoomBoundaryDecision right = controller.decide(
            new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, 0x18, false, true,
                0, 0x94, 0x30, 0x00));

        assertEquals(RoomBoundaryDecision.Type.INDOOR_SCROLL, left.type());
        assertEquals(ScrollController.LEFT, left.direction());
        assertEquals(RoomBoundaryDecision.Type.NONE, leftMargin.type());
        assertEquals(RoomBoundaryDecision.Type.NONE, rightMargin.type());
        assertEquals(RoomBoundaryDecision.Type.INDOOR_SCROLL, right.type());
        assertEquals(ScrollController.RIGHT, right.direction());
    }

    @Test
    void sideScrollingVerticalMarginsDoNotFallThroughToIndoorScroll() {
        for (int y = -1; y >= -4; y--) {
            assertEquals(RoomBoundaryDecision.Type.NONE, controller.decide(
                sideViewState(0x3E, 0x01, y, 0, false)).type(), "top y=" + y);
        }
        for (int y = 0x71; y <= 0x73; y++) {
            assertEquals(RoomBoundaryDecision.Type.NONE, controller.decide(
                sideViewState(0x3E, 0x01, y, 0, false)).type(), "bottom y=" + y);
        }
    }

    @Test
    void sideScrollingModifierTwoSuppressesFadeButUsesAdjacentRoomBoundary() {
        RoomBoundaryDecision decision = controller.decide(
            sideViewState(0x3E, 0x01, -5, 0x02, false));

        assertEquals(RoomBoundaryDecision.Type.INDOOR_SCROLL, decision.type());
        assertEquals(ScrollController.UP, decision.direction());
    }

    @Test
    void bottleGrottoSideViewUsesGenericVerticalFadeWhenModifierAllows() {
        assertEquals(RoomBoundaryDecision.Type.SIDE_SCROLL_VERTICAL_WARP, controller.decide(
            sideViewState(0x3E, 0x01, -5, 0, false)).type());
    }

    @Test
    void sourceSideViewExceptionsSuppressRoomScrollOrWarpFallback() {
        for (int roomId : new int[] {0xE8, 0xF8, 0xFD}) {
            assertEquals(RoomBoundaryDecision.Type.NONE, controller.decide(
                sideViewState(roomId, 0x00, -5, 0, false)).type(),
                "room=" + Integer.toHexString(roomId));
        }
        for (int roomId : new int[] {0xA3, 0xC0, 0xC1}) {
            assertEquals(RoomBoundaryDecision.Type.NONE,
                controller.decide(sideViewState(roomId, 0x00, -5, 0x02, false)).type(),
                "direct map fade room must not fall through=" + Integer.toHexString(roomId));
        }
        assertEquals(RoomBoundaryDecision.Type.NONE,
            controller.decide(sideViewStateWithoutWarps(0xA3, -5, 0x02)).type());
    }

    @Test
    void finalSideViewExceptionDependsOnEntityActivityAndRomY() {
        assertEquals(RoomBoundaryDecision.Type.NONE, controller.decide(
            sideViewState(0xFF, 0x00, -5, 0, true)).type());
        assertEquals(RoomBoundaryDecision.Type.SIDE_SCROLL_VERTICAL_WARP, controller.decide(
            sideViewState(0xFF, 0x00, -5, 0, false)).type());
        assertEquals(RoomBoundaryDecision.Type.NONE, controller.decide(
            sideViewState(0xFF, 0x00, 0x74, 0, true)).type());
        assertEquals(RoomBoundaryDecision.Type.NONE, controller.decide(
            sideViewState(0xFF, 0x00, 0x74, 0, false)).type());
    }

    private static RoomBoundaryState sideViewState(int roomId, int mapId, int y,
                                                    int physicsModifier,
                                                    boolean entityActive) {
        return new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, roomId, false, true,
            0, 0x70, y, mapId, false, physicsModifier, entityActive);
    }

    private static RoomBoundaryState sideViewStateWithoutWarps(int roomId, int y,
                                                                 int physicsModifier) {
        return new RoomBoundaryState(Warp.CATEGORY_SIDESCROLL, roomId, false, false,
            0, 0x70, y, 0x00, false, physicsModifier, false);
    }
}
