package linksawakening.rom;
/** Immutable copies of EntityPropertyTables ROM lookup data. */
public final class EntityPropertyTables {
    private static final int ENTITY_OPTIONS_TABLE_SIZE = 0x100;
    // Options1ForEntity: original loader length.
    private static final int ENTITY_OPTIONS1_LENGTH = 256;
    private final int[] entityOptions1;
    // HitboxFlagsForEntity: original loader length.
    private static final int ENTITY_HITBOX_FLAGS_LENGTH = 251;
    private final int[] entityHitboxFlags;

    private EntityPropertyTables(Rom rom) {
        entityOptions1 = rom.u8Table(RomSymbols.Options1ForEntity, ENTITY_OPTIONS1_LENGTH);
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
