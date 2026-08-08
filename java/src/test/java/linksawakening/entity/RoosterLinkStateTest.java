package linksawakening.entity;

import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoosterLinkStateTest {

    @Test
    void appliesAndClearsTheHandlerOwnedAirborneCarryState() {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new linksawakening.equipment.ItemRegistry());

        link.applyRoosterFlightState(0x14, 0x00, 0xF8, 0x00, 2);

        assertTrue(link.isRoosterCarryActive());
        assertTrue(link.isCarryingLiftedObject());
        assertTrue(link.isAirborne());
        assertEquals(0x14, link.romEntityZ());
        assertEquals(0x00, link.zVelocity());
        assertEquals(Link.DIRECTION_UP, link.direction());

        link.clearRoosterCarryState();

        assertFalse(link.isRoosterCarryActive());
        assertFalse(link.isCarryingLiftedObject());
        assertFalse(link.isAirborne());
        assertEquals(0x00, link.romEntityZ());
    }
}
