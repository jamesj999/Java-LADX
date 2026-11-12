package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import linksawakening.world.entity.event.*;
import linksawakening.world.room.RoomSession.LinkWrite;
import org.junit.jupiter.api.Test;

class RoomLinkWritesTest {
    private static SideViewPlatformLinkRequest platform(int slot) {
        return new SideViewPlatformLinkRequest(slot, 0, 0, 0, true, true);
    }

    private static SideViewPotLinkRequest pot(int slot) {
        return new SideViewPotLinkRequest(slot, false, false, 0, 0, false, 0, 0);
    }

    @Test
    void mergesAllPositionStreamsInDescendingSourceOrder() {
        var a = platform(14);
        var b = pot(12);
        var c = new LinkFinalPositionRequest(10);
        var d = platform(8);
        var e = new LinkFinalPositionRequest(6);
        var f = pot(4);
        assertEquals(
                List.of(
                        new LinkWrite.PlatformWrite(a),
                        new LinkWrite.PotWrite(b),
                        new LinkWrite.FinalPositionWrite(c),
                        new LinkWrite.PlatformWrite(d),
                        new LinkWrite.FinalPositionWrite(e),
                        new LinkWrite.PotWrite(f)),
                RoomLinkWrites.positions(List.of(a, d), List.of(b, f), List.of(c, e)));
    }

    @Test
    void equalSlotsKeepPotThenPlatformThenFinalPositionPrecedence() {
        var platform = platform(5);
        var pot = pot(5);
        var position = new LinkFinalPositionRequest(5);
        assertEquals(
                List.of(
                        new LinkWrite.PotWrite(pot),
                        new LinkWrite.PotWrite(pot),
                        new LinkWrite.PlatformWrite(platform),
                        new LinkWrite.PlatformWrite(platform),
                        new LinkWrite.FinalPositionWrite(position)),
                RoomLinkWrites.positions(List.of(platform, platform), List.of(pot, pot), List.of(position)));
    }

    @Test
    void mergesAttacksWithStepWinningTiesAndMaintainsStreamOrder() {
        var clear = new LinkAttackClearRequest(9);
        var step = new LinkAttackStepRequest(9);
        var last = new LinkAttackClearRequest(0);
        assertEquals(
                List.of(
                        new LinkWrite.AttackStepWrite(step),
                        new LinkWrite.AttackStepWrite(step),
                        new LinkWrite.AttackClearWrite(clear),
                        new LinkWrite.AttackClearWrite(last)),
                RoomLinkWrites.attacks(List.of(clear, last), List.of(step, step)));
    }

    @Test
    void handlesEmptyStreamsAndReturnsImmutableLists() {
        assertEquals(List.of(), RoomLinkWrites.positions(List.of(), List.of(), List.of()));
        assertEquals(List.of(), RoomLinkWrites.attacks(List.of(), List.of()));
        var clear = new LinkAttackClearRequest(0);
        var writes = RoomLinkWrites.attacks(List.of(clear), List.of());
        assertEquals(List.of(new LinkWrite.AttackClearWrite(clear)), writes);
        assertThrows(UnsupportedOperationException.class, writes::clear);
    }
}
