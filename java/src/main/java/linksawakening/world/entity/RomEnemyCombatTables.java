package linksawakening.world.entity;

import java.util.Objects;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;

/** Immutable decoder for the ROM's shared entity combat and Bow-Wow tables. */
public final class RomEnemyCombatTables {
    private static final int BOW_WOW_EATABLE_COUNT = 0x100;
    private static final int HEALTH_GROUP_COUNT = 0xFB;
    private static final int HEALTH_GROUP_COUNT_FOR_DAMAGE = 0x35;
    private static final int ATTACK_TYPE_COUNT = 0x10;
    private static final int DAMAGE_VALUES_PER_TYPE = 0x08;

    private final int[] healthGroupByEntity;
    private final int[] damageTypeMatrix;
    private final int[] damageValues;
    private final int[] initialHealthByGroup;
    private final int[] contactDamageByGroup;
    private final int[] bowWowEatableByEntity;

    /** @deprecated External callers: world.room.RoomSessionContext and RoomSessionTestSupport; pass a Rom to the typed constructor. */
    @Deprecated(forRemoval = true)
    public RomEnemyCombatTables(byte[] romData) {
        this(romData == null ? null : new Rom(romData));
    }

    public RomEnemyCombatTables(Rom romData) {
        Objects.requireNonNull(romData, "ROM data cannot be null");
        healthGroupByEntity = loadUnsigned(romData, RomSymbols.HealthGroupForEntity, HEALTH_GROUP_COUNT);
        damageTypeMatrix =
                loadUnsigned(romData, RomSymbols.Data_003_43EC, HEALTH_GROUP_COUNT_FOR_DAMAGE * ATTACK_TYPE_COUNT);
        damageValues = loadUnsigned(romData, RomSymbols.Data_003_473C, ATTACK_TYPE_COUNT * DAMAGE_VALUES_PER_TYPE);
        initialHealthByGroup = loadUnsigned(romData, RomSymbols.InitialHealthForGroup, HEALTH_GROUP_COUNT_FOR_DAMAGE);
        contactDamageByGroup = loadUnsigned(romData, RomSymbols.EntityDamagesForGroup, HEALTH_GROUP_COUNT_FOR_DAMAGE);
        bowWowEatableByEntity = loadUnsigned(romData, RomSymbols.BowWowEatableEntitiesTable, BOW_WOW_EATABLE_COUNT);
    }

    public int healthGroup(int entityType) {
        int index = entityType & 0xFF;
        return index < healthGroupByEntity.length ? healthGroupByEntity[index] : 0;
    }

    public int initialHealth(int entityType) {
        return initialHealthByGroup[healthGroup(entityType)];
    }

    public int contactDamage(int entityType) {
        return contactDamageByGroup[healthGroup(entityType)];
    }

    public int contactDamageForGroup(int healthGroup) {
        checkHealthGroup(healthGroup);
        return contactDamageByGroup[healthGroup];
    }

    /** Mirrors bank-$14's CanBowWowEatEntity lookup. */
    public boolean canBowWowEatEntity(int entityType) {
        return bowWowEatableByEntity[entityType & 0xFF] != 0;
    }

    /** Entry selected by the health-group/attack-type matrix. */
    public int damageTypeEntry(int healthGroup, int attackType) {
        checkHealthGroup(healthGroup);
        checkAttackType(attackType);
        return damageTypeMatrix[healthGroup * ATTACK_TYPE_COUNT + attackType];
    }

    /** Raw value selected by the damage-type table. */
    public int damageValue(int attackType, int entry) {
        checkAttackType(attackType);
        if (entry < 0 || entry >= DAMAGE_VALUES_PER_TYPE) {
            throw new IllegalArgumentException("Damage table entry out of range: " + entry);
        }
        return damageValues[attackType * DAMAGE_VALUES_PER_TYPE + entry];
    }

    /** Mirrors the two table lookups in ApplySwordDamagesToEnemy. */
    public SwordDamageResult resolveSwordDamage(int entityType, EnemyAttackContext attackContext) {
        return resolveSwordDamage(entityType, healthGroup(entityType), attackContext);
    }

    public SwordDamageResult resolveSwordDamage(int entityType, int healthGroup, EnemyAttackContext attackContext) {
        Objects.requireNonNull(attackContext, "Attack context cannot be null");
        int attackType = attackContext.effectiveDamageType();
        if (attackType < 0) {
            return new SwordDamageResult(entityType & 0xFF, healthGroup, -1, 0, 0);
        }
        return resolveAttackDamage(entityType, healthGroup, attackType);
    }

    /** Mirrors the bank-$03 damage-table lookup for a fixed projectile type. */
    public SwordDamageResult resolveAttackDamage(int entityType, int attackType) {
        return resolveAttackDamage(entityType, healthGroup(entityType), attackType);
    }

    public SwordDamageResult resolveAttackDamage(int entityType, int healthGroup, int attackType) {
        int entry = damageTypeEntry(healthGroup, attackType);
        int rawValue = damageValue(attackType, entry);
        return new SwordDamageResult(entityType & 0xFF, healthGroup, attackType, entry, rawValue);
    }

    public record SwordDamageResult(
            int entityType, int healthGroup, int effectiveDamageType, int damageTableEntry, int rawValue) {
        public SwordDamageResult {
            if (entityType < 0 || entityType > 0xFF) {
                throw new IllegalArgumentException("Entity type must be an unsigned byte");
            }
            if (healthGroup < 0 || healthGroup >= HEALTH_GROUP_COUNT_FOR_DAMAGE) {
                throw new IllegalArgumentException("Health group out of range: " + healthGroup);
            }
            if (effectiveDamageType < -1 || effectiveDamageType >= ATTACK_TYPE_COUNT) {
                throw new IllegalArgumentException("Attack type out of range: " + effectiveDamageType);
            }
            if (damageTableEntry < 0 || damageTableEntry >= DAMAGE_VALUES_PER_TYPE) {
                throw new IllegalArgumentException("Damage table entry out of range: " + damageTableEntry);
            }
            if (rawValue < 0 || rawValue > 0xFF) {
                throw new IllegalArgumentException("Raw damage must be an unsigned byte");
            }
        }

        public boolean ignored() {
            return rawValue == 0;
        }

        public boolean special() {
            return rawValue >= 0xF0;
        }

        public int numericDamage() {
            return special() ? 0 : rawValue;
        }

        public int specialAction() {
            return special() ? rawValue : -1;
        }
    }

    private static int[] loadUnsigned(Rom romData, RomAddress address, int length) {
        int offset = address.offset();
        if (offset < 0 || length < 0 || offset > romData.size() - length) {
            throw new IllegalArgumentException("ROM table is truncated at bank $" + Integer.toHexString(address.bank())
                    + ":$" + Integer.toHexString(address.address()));
        }
        Rom rom = romData;
        int[] values = new int[length];
        for (int index = 0; index < length; index++) {
            values[index] = rom.u8(address.plus(index));
        }
        return values;
    }

    private static void checkHealthGroup(int healthGroup) {
        if (healthGroup < 0 || healthGroup >= HEALTH_GROUP_COUNT_FOR_DAMAGE) {
            throw new IllegalArgumentException("Health group out of range: " + healthGroup);
        }
    }

    private static void checkAttackType(int attackType) {
        if (attackType < 0 || attackType >= ATTACK_TYPE_COUNT) {
            throw new IllegalArgumentException("Attack type out of range: " + attackType);
        }
    }
}
