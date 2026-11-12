package linksawakening.save;

import linksawakening.save.SaveSlotComponents.*;

/** Builds a slot from explicit cohesive component groups, retaining raw SRAM bytes. */
public final class SaveSlotStateBuilder {
    private final int slot;
    private final int[] nameBytes;
    private final byte[] rawSlot;
    private Resources resources;
    private Inventory inventory;
    private Ammunition ammunition;
    private Progression progression;
    private Spawn spawn;

    SaveSlotStateBuilder(int slot, int[] nameBytes, byte[] rawSlot) {
        this.slot = slot;
        this.nameBytes = nameBytes;
        this.rawSlot = rawSlot;
    }

    public SaveSlotStateBuilder resources(Resources value) {
        resources = value;
        return this;
    }

    public SaveSlotStateBuilder inventory(Inventory value) {
        inventory = value;
        return this;
    }

    public SaveSlotStateBuilder ammunition(Ammunition value) {
        ammunition = value;
        return this;
    }

    public SaveSlotStateBuilder progression(Progression value) {
        progression = value;
        return this;
    }

    public SaveSlotStateBuilder spawn(Spawn value) {
        spawn = value;
        return this;
    }

    public SaveSlotState build() {
        return new SaveSlotState(slot, nameBytes, resources, inventory, ammunition, progression, spawn, rawSlot);
    }
}
