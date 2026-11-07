package linksawakening.audio;

import linksawakening.app.GameSession;
import linksawakening.entity.Link;
import linksawakening.entity.LinkCommand;
import linksawakening.equipment.Sword;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.RomRandomByteSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@linksawakening.testing.RequiresRom
class SwordInteractionGateTest {
    @Test void exactTwoFreezesExistingSwordAndDropsBlockedPressWithoutBuffering() throws Exception {
        var rng=new RomRandomByteSource();int[] draws={0};
        var game=new GameSession(rng);
        try {
            game.initializeHeadlessNewGame();
            set(game,"newGameWakeUpMotion",null);set(game,"newGameTarinShieldMotion",null);
            var d=game.diagnostics();var player=d.playerState();var link=d.link();
            player.setSwordLevel(1);player.setItemA(PlayerState.INVENTORY_SWORD);player.setItemB(0);
            d.roomSession().loadIndoor(2,0x5A);
            for(int slot=0;slot<16;slot++)d.roomSession().clearEntity(slot);
            link.setRoomEntryRomPosition(72,80);
            rng.setObserver(draw -> draws[0]++);
            Sword sword=d.equipmentController().activeSword();
            game.headlessButton("a",true);game.headlessFrame();
            assertEquals(1,draws[0]);assertEquals(Sword.STATE_DRAW,sword.state());
            for(int i=0;i<8;i++) {
                link.writeRomInteractiveMotionBlock(2);game.headlessFrame();
                assertEquals(Sword.STATE_DRAW,sword.state());
                assertEquals(0,link.romInteractiveMotionBlockValue());
            }
            for(int i=0;i<3;i++)game.headlessFrame();
            assertEquals(Sword.STATE_SWING_START,sword.state(),"Resume the saved timer, not a fresh swing");
            game.headlessButton("a",false);
            for(int i=0;i<24;i++)game.headlessFrame();
            assertEquals(Sword.STATE_NONE,sword.state());
            link.writeRomInteractiveMotionBlock(2);game.headlessButton("a",true);game.headlessFrame();
            assertEquals(1,draws[0]);assertEquals(Sword.STATE_NONE,sword.state());
            game.headlessFrame();
            assertEquals(1,draws[0],"Blocked edge must not be replayed when held on resume");
            game.headlessButton("a",false);game.headlessFrame();
            link.writeRomInteractiveMotionBlock(3);game.headlessButton("a",true);game.headlessFrame();
            assertEquals(2,draws[0]);assertEquals(Sword.STATE_DRAW,sword.state(),"Raw 3 must not use the exact-2 item gate");
        } finally {
            var cleanup=GameSession.class.getDeclaredMethod("cleanup");cleanup.setAccessible(true);cleanup.invoke(game);
        }
    }
    @Test void rawWritesOverwriteAndSideViewKeepsItsOwnPolicy() {
        Link link=new Link(null,null,null,null,null,null,null);
        link.apply(new LinkCommand.MotionBlock(0,0,2));
        link.apply(new LinkCommand.MotionBlock(0,0,3));
        assertEquals(3,link.romInteractiveMotionBlockValue());assertFalse(link.romItemAnimationFrameBlocked());
        link.apply(new LinkCommand.MotionBlock(0,0,2));
        assertTrue(link.romItemAnimationFrameBlocked());
        link.setSideScrolling(true);assertFalse(link.romItemAnimationFrameBlocked());
        assertThrows(IllegalArgumentException.class,()->link.writeRomInteractiveMotionBlock(256));
    }
    private static void set(Object object,String name,Object value)throws Exception {
        var f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);
    }
}
