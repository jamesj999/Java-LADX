package linksawakening.world.entity.motion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.event.MoldormSegmentPosition;

/** Immutable projections of Moldorm's four history taps ($0C, $18, $24, $2E). */
final class MoldormTailSnapshots {
    private final List<List<MoldormSegmentPosition>> snapshots =
            new ArrayList<>(Collections.nCopies(EntityRoomLoader.MAX_ENTITIES, null));

    List<MoldormSegmentPosition> read(int slot, int inertia, int[] historyX, int[] historyY, int mask) {
        List<MoldormSegmentPosition> previous = snapshots.get(slot);
        var first = segment(previous, 0, (inertia - 0x0C) & mask, historyX, historyY);
        var second = segment(previous, 1, (inertia - 0x18) & mask, historyX, historyY);
        var third = segment(previous, 2, (inertia - 0x24) & mask, historyX, historyY);
        var fourth = segment(previous, 3, (inertia - 0x2E) & mask, historyX, historyY);
        if (previous != null
                && first == previous.get(0)
                && second == previous.get(1)
                && third == previous.get(2)
                && fourth == previous.get(3)) return previous;
        List<MoldormSegmentPosition> result = List.of(first, second, third, fourth);
        snapshots.set(slot, result);
        return result;
    }

    private static MoldormSegmentPosition segment(
            List<MoldormSegmentPosition> previous, int segment, int index, int[] historyX, int[] historyY) {
        int x = historyX[index], y = historyY[index];
        if (previous != null) {
            MoldormSegmentPosition value = previous.get(segment);
            if (value.x() == x && value.y() == y) return value;
        }
        return new MoldormSegmentPosition(x, y);
    }
}
