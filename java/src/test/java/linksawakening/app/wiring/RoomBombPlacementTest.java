package linksawakening.app.wiring;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.*;
import linksawakening.entity.Link;
import linksawakening.world.room.RoomPlayerItems;
import org.junit.jupiter.api.Test;

class RoomBombPlacementTest {
    @Test
    void registersRomPositionAndDirectionBeforeStartingTheItemAttackStep() {
        var link = new Link(null, null, null, null, null, null, null);
        link.position().setPixelPosition(40, 50);
        link.animation().setRomDirection(1);
        var calls = new ArrayList<String>();
        var items = (RoomPlayerItems) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {RoomPlayerItems.class}, (proxy, method, args) -> {
                    calls.add(method.getName());
                    assertEquals(0, link.equipment().romAttackStepAnimationCountdown());
                    if (method.getName().equals("placeBomb")) {
                        assertArrayEquals(new Object[] {48, 66, 0, 1}, args);
                        return true;
                    }
                    if (method.getName().equals("lastBombPlacementPlayedBump")) return true;
                    throw new AssertionError(method);
                });
        var target = new RoomBombPlacement(link, items);
        assertTrue(target.placeBomb());
        assertTrue(target.playBumpForLastPlacement());
        assertTrue(link.equipment().romAttackStepAnimationCountdown() > 0);
        assertEquals(List.of("placeBomb", "lastBombPlacementPlayedBump"), calls);
    }

    @Test
    void failedPlacementDoesNotStartAnAttackOrReadBump() {
        var link = new Link(null, null, null, null, null, null, null);
        var items = (RoomPlayerItems) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {RoomPlayerItems.class}, (proxy, method, args) -> {
                    assertEquals("placeBomb", method.getName());
                    return false;
                });
        var target = new RoomBombPlacement(link, items);
        assertFalse(target.placeBomb());
        assertFalse(target.playBumpForLastPlacement());
        assertEquals(0, link.equipment().romAttackStepAnimationCountdown());
    }
}
