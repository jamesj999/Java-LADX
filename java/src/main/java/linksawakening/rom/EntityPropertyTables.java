package linksawakening.rom;
/** Immutable copies of EntityPropertyTables ROM lookup data. */
public final class EntityPropertyTables {
    private static final int ENTITY_OPTIONS_TABLE_SIZE = 0x100;
    // Options1ForEntity: original loader length.
    private static final int ENTITY_OPTIONS1_LENGTH = 256;
    // PhysicsFlagsForEntity ends at HitboxFlagsForEntity ($03:40FB).
    private static final int ENTITY_PHYSICS_FLAGS_LENGTH = 0xFB;
    // HitboxFlagsForEntity: original loader length.
    private static final int ENTITY_HITBOX_FLAGS_LENGTH = 251;
    private final int[] entityOptions1;
    private final int[] entityHitboxFlags;
    private final int[] entityPhysicsFlags;

    private EntityPropertyTables(Rom rom) {
        entityOptions1 = rom.u8Table(RomSymbols.Options1ForEntity, ENTITY_OPTIONS1_LENGTH);
        entityPhysicsFlags = rom.u8Table(RomSymbols.PhysicsFlagsForEntity, ENTITY_PHYSICS_FLAGS_LENGTH);
        entityHitboxFlags = rom.u8Table(RomSymbols.HitboxFlagsForEntity, ENTITY_HITBOX_FLAGS_LENGTH);
    }

    public static EntityPropertyTables load(Rom rom) {
        return new EntityPropertyTables(rom);
    }

    /**
     * ROM {@code Options1ForEntity[entityType]} from bank $03. Bit $10 in
     * this byte is {@code ENTITY_OPT1_B_NO_GROUND_INTERACTION}.
     */
    public int entityOptions1(int entityType) {
        if (entityType < 0 || entityType >= ENTITY_OPTIONS_TABLE_SIZE) {
            return 0;
        }
        return entityOptions1[entityType];
    }

    /** Initial ROM {@code PhysicsFlagsForEntity[entityType]} before the init handler runs. */
    public int entityPhysicsFlags(int entityType) {
        if (entityType < 0 || entityType >= entityPhysicsFlags.length) {
            return 0;
        }
        return entityPhysicsFlags[entityType];
    }

    /** ROM {@code HitboxFlagsForEntity[entityType]}. */
    public int entityHitboxFlags(int entityType) {
        if (entityType < 0 || entityType >= entityHitboxFlags.length) {
            return 0;
        }
        return entityHitboxFlags[entityType];
    }

    /** The low two bits used by ApplyEntityInteractionWithBackground. */
    public int entityCollisionBoxType(int entityType) {
        return entityHitboxFlags(entityType) & 0x03;
    }
}
