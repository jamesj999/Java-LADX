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
        List<LinkWrite> writes = new ArrayList<>();
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
                case 0 -> writes.add(new LinkWrite.PlatformWrite(platforms.get(platform++)));
                case 1 -> writes.add(new LinkWrite.FinalPositionWrite(positions.get(position++)));
                default -> writes.add(new LinkWrite.PotWrite(pots.get(pot++)));
            }
        }
        return List.copyOf(writes);
    }

    static List<LinkWrite> attacks(List<LinkAttackClearRequest> clears, List<LinkAttackStepRequest> steps) {
        List<LinkWrite> writes = new ArrayList<>();
        int clear = 0, step = 0;
        while (clear < clears.size() || step < steps.size()) {
            boolean applyClear = step >= steps.size()
                    || (clear < clears.size()
                            && clears.get(clear).sourceSlot() > steps.get(step).sourceSlot());
            if (applyClear) writes.add(new LinkWrite.AttackClearWrite(clears.get(clear++)));
            else writes.add(new LinkWrite.AttackStepWrite(steps.get(step++)));
        }
        return List.copyOf(writes);
    }
}
