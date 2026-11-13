package linksawakening.vfx;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

/**
 * Fixed-slot transient VFX runtime for short-lived overworld gameplay effects.
 */
public final class TransientVfxSystem {
    /** Read-only effect values; live implementations change with the simulation. */
    public interface SlotView {
        int slotIndex();

        TransientVfxType type();

        int countdown();

        int worldX();

        int worldY();

        int variant();
    }

    /** Immutable slot values. */
    public record Slot(int slotIndex, TransientVfxType type, int countdown, int worldX, int worldY, int variant)
            implements SlotView {
        public Slot(int slotIndex, TransientVfxType type, int countdown, int worldX, int worldY) {
            this(slotIndex, type, countdown, worldX, worldY, 0);
        }
    }

    private static final class MutableSlot implements SlotView {
        private final int slotIndex;
        // An inactive slot has no effect. Its retained values are ignored until activate().
        private boolean active;
        private TransientVfxType type = TransientVfxType.POOF;
        private int countdown;
        private int worldX;
        private int worldY;
        private int variant;

        private MutableSlot(int slotIndex) {
            this.slotIndex = slotIndex;
        }

        private void activate(TransientVfxType type, int countdown, int worldX, int worldY, int variant) {
            active = true;
            this.type = type;
            this.countdown = countdown;
            this.worldX = worldX;
            this.worldY = worldY;
            this.variant = variant & 0xFF;
        }

        public int slotIndex() {
            return slotIndex;
        }

        public TransientVfxType type() {
            return type;
        }

        public int countdown() {
            return countdown;
        }

        public int worldX() {
            return worldX;
        }

        public int worldY() {
            return worldY;
        }

        public int variant() {
            return variant;
        }

        private Slot snapshot() {
            return new Slot(slotIndex, type, countdown, worldX, worldY, variant);
        }
    }

    private final MutableSlot[] slots;
    private final List<SlotView> liveSlots;

    private static final class LiveSlots extends AbstractList<SlotView> implements RandomAccess {
        private final MutableSlot[] slots;

        private LiveSlots(MutableSlot[] slots) {
            this.slots = slots;
        }

        public SlotView get(int index) {
            if (index >= 0) {
                for (MutableSlot slot : slots) {
                    if (slot.active && index-- == 0) return slot;
                }
            }
            throw new IndexOutOfBoundsException();
        }

        public int size() {
            int size = 0;
            for (MutableSlot slot : slots) if (slot.active) size++;
            return size;
        }
    }

    public TransientVfxSystem(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be >= 0");
        }
        this.slots = new MutableSlot[capacity];
        for (int i = 0; i < capacity; i++) slots[i] = new MutableSlot(i);
        this.liveSlots = Collections.unmodifiableList(new LiveSlots(slots));
    }

    public int spawn(TransientVfxType type, int worldX, int worldY) {
        return spawn(type, worldX, worldY, 0);
    }

    /** Adds an effect while retaining the handler-selected ROM sprite variant. */
    public int spawn(TransientVfxType type, int worldX, int worldY, int variant) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        for (int i = 0; i < slots.length; i++) {
            if (!slots[i].active) {
                slots[i].activate(type, type.defaultCountdown(), worldX, worldY, variant);
                return i;
            }
        }
        return -1;
    }

    public void tick() {
        for (int i = 0; i < slots.length; i++) {
            MutableSlot slot = slots[i];
            if (!slot.active) {
                continue;
            }
            if (slot.countdown <= 1) {
                slot.active = false;
            } else {
                slot.countdown--;
            }
        }
    }

    public void clear() {
        for (MutableSlot slot : slots) slot.active = false;
    }

    public int activeCount() {
        int count = 0;
        for (MutableSlot slot : slots) {
            if (slot.active) {
                count++;
            }
        }
        return count;
    }

    /**
     * Borrowed read-only view in slot order, for immediate same-thread consumption.
     * Its contents change on spawn, tick and clear. Use {@link #activeSlots()} for retained history.
     */
    public List<SlotView> liveSlots() {
        return liveSlots;
    }

    /** Returns an owned immutable snapshot that can be retained across simulation updates. */
    public List<Slot> activeSlots() {
        List<Slot> active = new ArrayList<>(slots.length);
        for (MutableSlot slot : slots) {
            if (slot.active) active.add(slot.snapshot());
        }
        return List.copyOf(active);
    }
}
