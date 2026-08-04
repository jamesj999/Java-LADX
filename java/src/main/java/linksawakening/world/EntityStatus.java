package linksawakening.world;

/** Runtime status values used by the disassembly's entity status table. */
public enum EntityStatus {
    DISABLED(0),
    DYING(1),
    FALLING(2),
    BURNING(3),
    INIT(4),
    ACTIVE(5),
    STUNNED(6),
    LIFTED(7),
    THROWN(8);

    private final int value;

    EntityStatus(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
