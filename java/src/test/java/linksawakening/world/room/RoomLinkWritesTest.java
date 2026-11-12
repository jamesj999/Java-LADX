package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import linksawakening.world.entity.event.*;
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

    @Test
    void directVisitorsPreserveTiePrecedenceAndStreamOrderWithoutWrapperLists() {
        var calls = new java.util.ArrayList<String>();
        var target = new LinkWriteSink.Positions() {
            public void platform(SideViewPlatformLinkRequest r) {
                calls.add("platform" + r.sourceSlot());
            }

            public void pot(SideViewPotLinkRequest r) {
                calls.add("pot" + r.sourceSlot());
            }

            public void position(LinkFinalPositionRequest r) {
                calls.add("position" + r.sourceSlot());
            }
        };
        RoomLinkWrites.visitPositions(
                List.of(platform(5), platform(2)),
                List.of(pot(5), pot(3)),
                List.of(new LinkFinalPositionRequest(5), new LinkFinalPositionRequest(1)),
                target);
        assertEquals(List.of("pot5", "platform5", "position5", "pot3", "platform2", "position1"), calls);
        calls.clear();
        RoomLinkWrites.visitAttacks(
                List.of(new LinkAttackClearRequest(5), new LinkAttackClearRequest(1)),
                List.of(new LinkAttackStepRequest(5)),
                new LinkWriteSink.Attacks() {
                    public void clear(LinkAttackClearRequest r) {
                        calls.add("clear" + r.sourceSlot());
                    }

                    public void step(LinkAttackStepRequest r) {
                        calls.add("step" + r.sourceSlot());
                    }
                });
        assertEquals(List.of("step5", "clear5", "clear1"), calls);
    }
}
