package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

import linksawakening.config.AppConfig;
import linksawakening.testing.RequiresRom;
import linksawakening.world.room.RoomConstants;
import org.junit.jupiter.api.Test;

@RequiresRom
class ObjectInteractionSessionTest {
    private GameSession session() throws Exception {
        var game = new GameSession((pcm, rate) -> {});
        game.initializeHeadlessNewGame();
        game.state().options().replace(AppConfig.parse("{\"debugEnabled\":true}"));
        game.onKeyEvent(GLFW_KEY_F2, GLFW_PRESS);
        game.diagnostics().roomSession().rooms().loadIndoor(0, 0x17);
        game.diagnostics().link().position().setPixelPosition(40, 40);
        return game;
    }

    @Test
    void crackedFloorGivesWayThenLinkFallsThroughNormalPitPhysics() throws Exception {
        var game = session();
        var room = game.diagnostics().roomSession().rooms().activeRoom();
        int index = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        game.headlessFrame(); // establish the preceding GetObjectUnderLink sample
        room.roomObjectsArea()[index] = 0xDF;
        for (int i = 0; i < 39; i++) game.headlessFrame();
        assertEquals(0xDF, room.roomObjectsArea()[index]);
        game.headlessFrame();
        assertEquals(0xAE, room.roomObjectsArea()[index]);
        for (int i = 0; i < 64 && !game.diagnostics().link().airborne().isFallingIntoPit(); i++) game.headlessFrame();
        assertTrue(game.diagnostics().link().airborne().isFallingIntoPit());
    }

    @Test
    void dialogStillRunsTheCachedGroundPhysicsResetWhileLinkMotionIsPaused() throws Exception {
        var game = session();
        var roomSession = game.diagnostics().roomSession();
        game.headlessFrame();
        roomSession.rooms().activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33] = 0xDF;
        for (int i = 0; i < 17; i++) game.headlessFrame();
        assertEquals(17, linksawakening.world.room.RoomSessionTestAccess.crackedFloorCounter(roomSession));
        game.diagnostics().dialogController().open("Ground physics during dialog.");
        game.headlessFrame();
        assertTrue(game.diagnostics().dialogController().isActive());
        // $02:75C4 -> 76C0 -> 7757 -> $7858/$786C, despite the Java motion gate.
        assertEquals(0, linksawakening.world.room.RoomSessionTestAccess.crackedFloorCounter(roomSession));
        assertEquals(0xDF, roomSession.rooms().activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33]);
    }

    @Test
    void toadstoolGotItemPresentationResetsTheCrackedFloorCounter() throws Exception {
        var game = session();
        var roomSession = game.diagnostics().roomSession();
        var player = game.diagnostics().playerState();
        player.ammunition().setHasToadstool(true);
        player.ammunition().setMagicPowderCount(0);
        player.inventory().setItemA(linksawakening.state.InventoryItem.MAGIC_POWDER.romValue());
        player.inventory().setItemB(0);
        game.headlessFrame();
        roomSession.rooms().activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33] = 0xDF;
        for (int i = 0; i < 17; i++) game.headlessFrame();
        assertEquals(17, linksawakening.world.room.RoomSessionTestAccess.crackedFloorCounter(roomSession));
        // UseMagicPowder $00:149E sets wDialogGotItem; $02:7854 then resets wC1C9.
        game.headlessButton("a", true);
        game.headlessFrame();
        game.headlessButton("a", false);
        assertTrue(game.diagnostics().link().animation().dialogGotItemActive());
        game.headlessFrame();
        assertEquals(0, linksawakening.world.room.RoomSessionTestAccess.crackedFloorCounter(roomSession));
        assertEquals(0xDF, roomSession.rooms().activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33]);
    }

    @Test
    void externalPositionChangeIsComparedAgainstTheLastGroundProbe() throws Exception {
        var game = session();
        var roomSession = game.diagnostics().roomSession();
        game.headlessFrame();
        roomSession.rooms().activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33] = 0xDF;
        roomSession.rooms().activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x34] = 0xDF;
        for (int i = 0; i < 17; i++) game.headlessFrame();
        game.diagnostics().link().position().setPixelPosition(56, 40); // entity-style move between Link passes
        game.headlessFrame();
        assertEquals(0, linksawakening.world.room.RoomSessionTestAccess.crackedFloorCounter(roomSession));
        game.headlessFrame();
        assertEquals(1, linksawakening.world.room.RoomSessionTestAccess.crackedFloorCounter(roomSession));
    }

    @Test
    void levelOneSwordBreaksIndoorCrystalThroughGameplayPhase() throws Exception {
        var game = session();
        var player = game.diagnostics().playerState();
        player.equipmentLevels().setSwordLevel(1);
        player.inventory().setItemA(linksawakening.state.InventoryItem.SWORD.romValue());
        var room = game.diagnostics().roomSession().rooms().activeRoom();
        // Face right, then put a crystal at the ROM's sword probe (X+$16,Y+$08).
        game.headlessButton("right", true);
        game.headlessFrame();
        game.headlessButton("right", false);
        game.diagnostics().link().position().setPixelPosition(32, 40);
        int index = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        room.roomObjectsArea()[index] = 0xDD;
        game.headlessButton("a", true);
        for (int i = 0; i < 12; i++) game.headlessFrame();
        assertEquals(0x0D, room.roomObjectsArea()[index]);
    }
}
