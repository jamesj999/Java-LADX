package linksawakening.app;

import linksawakening.app.GameSession;
import linksawakening.config.AppConfig;
import linksawakening.testing.RequiresRom;
import linksawakening.state.PlayerState;
import linksawakening.world.room.RoomConstants;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

@RequiresRom
class ObjectInteractionSessionTest {
    private GameSession session() throws Exception {
        var game = new GameSession((pcm, rate) -> {});
        game.initializeHeadlessNewGame();
        var config = GameSession.class.getDeclaredField("appConfig");
        config.setAccessible(true);
        config.set(game, AppConfig.parse("{\"debugEnabled\":true}"));
        game.onKeyEvent(GLFW_KEY_F2, GLFW_PRESS);
        game.diagnostics().roomSession().loadIndoor(0, 0x17);
        game.diagnostics().link().setPixelPosition(40, 40);
        return game;
    }

    @Test void crackedFloorGivesWayThenLinkFallsThroughNormalPitPhysics() throws Exception {
        var game = session();
        var room = game.diagnostics().roomSession().activeRoom();
        int index = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        game.headlessFrame(); // establish the preceding GetObjectUnderLink sample
        room.roomObjectsArea()[index] = 0xDF;
        for (int i = 0; i < 39; i++) game.headlessFrame();
        assertEquals(0xDF, room.roomObjectsArea()[index]);
        game.headlessFrame();
        assertEquals(0xAE, room.roomObjectsArea()[index]);
        for (int i = 0; i < 64 && !game.diagnostics().link().isFallingIntoPit(); i++) game.headlessFrame();
        assertTrue(game.diagnostics().link().isFallingIntoPit());
    }

    @Test void dialogStillRunsTheCachedGroundPhysicsResetWhileLinkMotionIsPaused() throws Exception {
        var game = session();
        var roomSession = game.diagnostics().roomSession();
        game.headlessFrame();
        roomSession.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33] = 0xDF;
        for (int i = 0; i < 17; i++) game.headlessFrame();
        assertEquals(17, linksawakening.world.room.RoomSessionTestAccess.crackedFloorCounter(roomSession));
        game.diagnostics().dialogController().open("Ground physics during dialog.");
        game.headlessFrame();
        assertTrue(game.diagnostics().dialogController().isActive());
        // $02:75C4 -> 76C0 -> 7757 -> $7858/$786C, despite the Java motion gate.
        assertEquals(0, linksawakening.world.room.RoomSessionTestAccess.crackedFloorCounter(roomSession));
        assertEquals(0xDF, roomSession.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33]);
    }

    @Test void externalPositionChangeIsComparedAgainstTheLastGroundProbe() throws Exception {
        var game = session();
        var roomSession = game.diagnostics().roomSession();
        game.headlessFrame();
        roomSession.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33] = 0xDF;
        roomSession.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x34] = 0xDF;
        for (int i = 0; i < 17; i++) game.headlessFrame();
        game.diagnostics().link().setPixelPosition(56, 40); // entity-style move between Link passes
        game.headlessFrame();
        assertEquals(0, linksawakening.world.room.RoomSessionTestAccess.crackedFloorCounter(roomSession));
        game.headlessFrame();
        assertEquals(1, linksawakening.world.room.RoomSessionTestAccess.crackedFloorCounter(roomSession));
    }

    @Test void levelOneSwordBreaksIndoorCrystalThroughGameplayPhase() throws Exception {
        var game = session();
        var player = game.diagnostics().playerState();
        player.setSwordLevel(1);
        player.setItemA(PlayerState.INVENTORY_SWORD);
        var room = game.diagnostics().roomSession().activeRoom();
        // Face right, then put a crystal at the ROM's sword probe (X+$16,Y+$08).
        game.headlessButton("right", true);
        game.headlessFrame();
        game.headlessButton("right", false);
        game.diagnostics().link().setPixelPosition(32, 40);
        int index = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        room.roomObjectsArea()[index] = 0xDD;
        game.headlessButton("a", true);
        for (int i = 0; i < 12; i++) game.headlessFrame();
        assertEquals(0x0D, room.roomObjectsArea()[index]);
    }
}
