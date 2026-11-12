package linksawakening.world.room;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.entity.event.*;

/** Stable head merges: preserve each stream's order and the source tie precedence. */
final class RoomLinkWrites {
    private RoomLinkWrites() {}

    static List<LinkWrite> positions(
            List<SideViewPlatformLinkRequest> platforms,
            List<SideViewPotLinkRequest> pots,
            List<LinkFinalPositionRequest> positions) {
        if (platforms.isEmpty() && pots.isEmpty() && positions.isEmpty()) return List.of();
        var collector = new Collector();
        visitPositions(platforms, pots, positions, collector);
        return List.copyOf(collector.writes);
    }

    static List<LinkWrite> attacks(List<LinkAttackClearRequest> clears, List<LinkAttackStepRequest> steps) {
        if (clears.isEmpty() && steps.isEmpty()) return List.of();
        var collector = new Collector();
        visitAttacks(clears, steps, collector);
        return List.copyOf(collector.writes);
    }

    private static final class Collector implements LinkWriteSink.Positions, LinkWriteSink.Attacks {
        final List<LinkWrite> writes = new ArrayList<>();

        public void platform(SideViewPlatformLinkRequest r) {
            writes.add(new LinkWrite.PlatformWrite(r));
        }

        public void position(LinkFinalPositionRequest r) {
            writes.add(new LinkWrite.FinalPositionWrite(r));
        }

        public void pot(SideViewPotLinkRequest r) {
            writes.add(new LinkWrite.PotWrite(r));
        }

        public void clear(LinkAttackClearRequest r) {
            writes.add(new LinkWrite.AttackClearWrite(r));
        }

        public void step(LinkAttackStepRequest r) {
            writes.add(new LinkWrite.AttackStepWrite(r));
        }
    }

    static void visitPositions(
            List<SideViewPlatformLinkRequest> platforms,
            List<SideViewPotLinkRequest> pots,
            List<LinkFinalPositionRequest> positions,
            LinkWriteSink.Positions target) {
        int platform = 0, pot = 0, position = 0;
        while (platform < platforms.size() || pot < pots.size() || position < positions.size()) {
            int stream = -1, slot = -1;
            if (platform < platforms.size() && platforms.get(platform).sourceSlot() > slot) {
                stream = 0;
                slot = platforms.get(platform).sourceSlot();
            }
            if (position < positions.size() && positions.get(position).sourceSlot() > slot) {
                stream = 1;
                slot = positions.get(position).sourceSlot();
            }
            // D6 explicit writes win ties; platforms precede final-position writes on ties.
            if (pot < pots.size() && pots.get(pot).sourceSlot() >= slot) stream = 2;
            switch (stream) {
                case 0 -> target.platform(platforms.get(platform++));
                case 1 -> target.position(positions.get(position++));
                default -> target.pot(pots.get(pot++));
            }
        }
    }

    static void visitAttacks(
            List<LinkAttackClearRequest> clears, List<LinkAttackStepRequest> steps, LinkWriteSink.Attacks target) {
        int clear = 0, step = 0;
        while (clear < clears.size() || step < steps.size()) {
            boolean applyClear = step >= steps.size()
                    || (clear < clears.size()
                            && clears.get(clear).sourceSlot() > steps.get(step).sourceSlot());
            if (applyClear) target.clear(clears.get(clear++));
            else target.step(steps.get(step++));
        }
    }
}
