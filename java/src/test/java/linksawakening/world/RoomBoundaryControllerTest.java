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
}
