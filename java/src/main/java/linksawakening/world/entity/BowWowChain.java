package linksawakening.world.entity;

import linksawakening.world.entity.RoomEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Bank $05:4253/$43A8/$43FE: the chain's two directional constraint passes. */
public final class BowWowChain {
    private final int[] x = new int[7];
    private final int[] y = new int[7];

    public record Entry(int sourceSlot, int rawX, int rawY, int tileIndex) { }

    public void initialize(int entityX, int visualY) {
        Arrays.fill(x, entityX & 0xFF);
        Arrays.fill(y, visualY & 0xFF);
    }

    public RoomEntity advance(RoomEntity entity, int anchorX, int anchorY,
                       int questState, boolean linkMoving) {
        x[0] = entity.x() & 0xFF;
        y[0] = (entity.y() - entity.z()) & 0xFF;
        // First propagate BowWow's position towards the loose end.
        for (int i = 1; i <= 5; i++) {
            approach(x, i, i - 1, 2);
            approach(y, i, i - 1, 2);
        }
        if (questState == 0 || questState == 0x80 || !linkMoving) return entity;
        x[6] = anchorX & 0xFF;
        y[6] = anchorY & 0xFF;
        boolean pulledX = false;
        boolean pulledY = false;
        // Then propagate Link's end back towards BowWow, three pixels per link.
        for (int i = 5; i >= 0; i--) {
            boolean moved = approach(x, i, i + 1, 3);
            if (i == 0) pulledX = moved;
        }
        for (int i = 5; i >= 0; i--) {
            boolean moved = approach(y, i, i + 1, 3);
            if (i == 0) pulledY = moved;
        }
        // $4480/$449F pull the perpendicular axis one pixel towards the anchor.
        if (pulledX) align(y, anchorY);
        if (pulledY) align(x, anchorX);
        if (!pulledX && !pulledY) return entity;
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x[0], (y[0] + entity.z()) & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z(), entity.deathSpriteVariant(), entity.powerRecoilDeath());
    }

    private static boolean approach(int[] axis, int index, int towards, int speed) {
        int distance = (axis[towards] - axis[index] + 7) & 0xFF;
        if (distance < 14) return false;
        axis[index] = (axis[index] + ((distance & 0x80) == 0 ? speed : -speed)) & 0xFF;
        return true;
    }

    private static void align(int[] axis, int anchor) {
        for (int i = 0; i < 6; i++) {
            int difference = (anchor - axis[i]) & 0xFF;
            if (difference != 0) {
                axis[i] = (axis[i] + ((difference & 0x80) == 0 ? 1 : -1)) & 0xFF;
            }
        }
    }

    public List<Entry> entries(int slot, int questState, int frame) {
        List<Entry> entries = new ArrayList<>(3);
        for (int i = 5; i > 0; i--) {
            if (((frame ^ i) & 1) == 0) {
                entries.add(new Entry(slot, (x[i] + 4) & 0xFF, y[i],
                    questState == 0 ? 0x7E : 0x4E));
            }
        }
        return List.copyOf(entries);
    }
}
