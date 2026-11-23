package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRoms;
import linksawakening.world.transition.RoomBoundaryController;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import org.junit.jupiter.api.Test;

@RequiresRom
final class OverworldWarpHoleTest {
    @Test
    void fallsChargesSkipsUnvisitedRoomAndLandsWithInputHeld() throws Exception {
        transit(0x2C);
    }

    @Test
    void returnsToSourceWhenNoOtherWarpHoleHasBeenVisited() throws Exception {
        transit(0x95);
    }

    private void transit(int destination) throws Exception {
        byte[] rom = RoomNavigationFixture.loadRom();
        RomTables tables = RomTables.load(TestRoms.partial(rom));
        OverworldCollision collision = new OverworldCollision(tables);
        RoomSession session = RoomNavigationFixture.newSession(rom, tables, collision);
        if (destination != 0x95) session.rooms().loadInitialOverworld(destination);
        session.rooms().loadInitialOverworld(0x95);
        var transition = new TransitionController();
        var coordinator =
                session.transitions().coordinator(new RoomBoundaryController(), transition, new ScrollController());
        var input = new InputState();
        var config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        var sounds = new ArrayList<GameplaySoundEvent>();
        var player = new PlayerState();
        int initialHealth = player.vitals().health();
        var link = new Link(input, config, tables, collision, null, player, new ItemRegistry(), sounds::add);

        link.airborne().setOverworldWarpHoleRoom(true);
        link.position().setRoomEntryPixelPosition(96, 96);
        link.position().setPixelPosition(96, 64);
        RoomNavigationFixture.runUntilPitFallStarts(link);
        RoomNavigationFixture.runUntilPitFallFinishes(link);
        assertTrue(link.airborne().warpHoleControlsLink());
        int sourceX = link.position().pixelX();
        int sourceY = link.position().pixelY();
        input.onKeyEvent(config.leftKey(), GLFW_PRESS);
        for (int frame = 0; frame < 95; frame++) {
            link.motion().update();
            coordinator.handleWarpAndIndoorBoundaries(link);
            if (frame < 94) {
                assertFalse(transition.isActive());
                assertEquals(sourceX, link.position().pixelX());
                assertEquals(sourceY, link.position().pixelY());
            }
        }
        assertTrue(transition.isActive());
        assertEquals(List.of(GameplaySoundEvent.PIT_FALL, GameplaySoundEvent.OVERWORLD_WARP_HOLE), sounds);
        for (int i = 0; i < 200 && transition.isActive(); i++) transition.tick();
        assertFalse(transition.isActive());
        assertEquals(destination, session.rooms().currentRoomId());
        assertEquals(0x78, link.airborne().zPixels());

        for (int i = 0; i < 80 && link.airborne().warpHoleControlsLink(); i++) {
            link.motion().update();
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertEquals(96, link.position().pixelX());
            assertEquals(96, link.position().pixelY());
        }
        assertFalse(link.airborne().warpHoleControlsLink());
        assertEquals(0, link.airborne().zPixels());
        assertEquals(initialHealth, player.vitals().health());
        assertEquals(
                List.of(
                        GameplaySoundEvent.PIT_FALL,
                        GameplaySoundEvent.OVERWORLD_WARP_HOLE,
                        GameplaySoundEvent.LINK_FOOTSTEP),
                sounds);
    }
}
