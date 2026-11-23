package linksawakening.rom;
/** Compatibility aggregate of immutable, themed ROM tables. */
public final class RomTables {
    public static final int PHYSICS_TABLE_OVERWORLD = 0;
    public static final int PHYSICS_TABLE_INDOORS1 = 1;
    public static final int PHYSICS_TABLE_INDOORS2 = 2;
    private final PhysicsTables physicsTables;
    private final EntityPropertyTables entityPropertyTables;
    private final CollisionTables collisionTables;
    private final LinkMotionTables linkMotionTables;
    private final SwordTables swordTables;
    private final EquipmentTables equipmentTables;

    private RomTables(Rom rom) {
        physicsTables = PhysicsTables.load(rom);
        entityPropertyTables = EntityPropertyTables.load(rom);
        collisionTables = CollisionTables.load(rom);
        linkMotionTables = LinkMotionTables.load(rom);
        swordTables = SwordTables.load(rom);
        equipmentTables = EquipmentTables.load(rom);
    }

    public static RomTables load(Rom rom) {
        return new RomTables(rom);
    }

    public PhysicsTables physicsTables() {
        return physicsTables;
    }

    public EntityPropertyTables entityPropertyTables() {
        return entityPropertyTables;
    }

    public CollisionTables collisionTables() {
        return collisionTables;
    }

    public LinkMotionTables linkMotionTables() {
        return linkMotionTables;
    }

    public SwordTables swordTables() {
        return swordTables;
    }

    public EquipmentTables equipmentTables() {
        return equipmentTables;
    }
}
