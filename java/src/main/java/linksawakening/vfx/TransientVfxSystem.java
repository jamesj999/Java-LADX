package linksawakening.vfx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fixed-slot transient VFX runtime for short-lived overworld gameplay effects.
 */
public final class TransientVfxSystem {

    public record Slot(int slotIndex, TransientVfxType type, int countdown, int worldX, int worldY) {}

    private static final class MutableSlot {
        private final int slotIndex;
        private TransientVfxType type;
        private int countdown;
        private int worldX;
        private int worldY;

        private MutableSlot(int slotIndex, TransientVfxType type, int countdown, int worldX, int worldY) {
            this.slotIndex = slotIndex;
            this.type = type;
            this.countdown = countdown;
            this.worldX = worldX;
            this.worldY = worldY;
        }

        private Slot snapshot() {
            return new Slot(slotIndex, type, countdown, worldX, worldY);
        }
    }

    private final MutableSlot[] slots;

    public TransientVfxSystem(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be >= 0");
        }
        this.slots = new MutableSlot[capacity];
    }

    public int spawn(TransientVfxType type, int worldX, int worldY) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                slots[i] = new MutableSlot(i, type, type.defaultCountdown(), worldX, worldY);
                return i;
            }
        }
        return -1;
    }

    public void tick() {
        for (int i = 0; i < slots.length; i++) {
            MutableSlot slot = slots[i];
            if (slot == null) {
                continue;
            }
            if (slot.countdown <= 1) {
                slots[i] = null;
            } else {
                slot.countdown--;
            }
        }
    }

    public void clear() {
        Arrays.fill(slots, null);
    }

    public int activeCount() {
        int count = 0;
        for (MutableSlot slot : slots) {
            if (slot != null) {
                count++;
            }
        }
        return count;
    }

    public List<Slot> activeSlots() {
        List<Slot> active = new ArrayList<>(slots.length);
        Arrays.stream(slots)
            .filter(slot -> slot != null)
            .forEach(slot -> active.add(slot.snapshot()));
        return List.copyOf(active);
    }
}
