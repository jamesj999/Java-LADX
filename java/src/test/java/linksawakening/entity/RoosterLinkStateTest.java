package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

final class RoosterLinkStateTest {

    @Test
    void appliesAndClearsTheHandlerOwnedAirborneCarryState() {
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new linksawakening.equipment.ItemRegistry())
                .build();

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
