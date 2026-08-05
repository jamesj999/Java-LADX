package linksawakening.world;

import linksawakening.rom.RomBank;

import java.util.Objects;

/** Immutable decoder for bank-$03's shared entity health and damage tables. */
public final class RomEnemyCombatTables {
    private static final int BANK = 0x03;
    private static final int HEALTH_GROUP_ADDRESS = 0x41F6;
    private static final int HEALTH_GROUP_COUNT = 0xFB;
    private static final int DAMAGE_TYPE_MATRIX_ADDRESS = 0x43EC;
    private static final int HEALTH_GROUP_COUNT_FOR_DAMAGE = 0x35;
    private static final int ATTACK_TYPE_COUNT = 0x10;
    private static final int DAMAGE_VALUE_ADDRESS = 0x473C;
    private static final int DAMAGE_VALUES_PER_TYPE = 0x08;
    private static final int INITIAL_HEALTH_ADDRESS = 0x47BC;
    private static final int CONTACT_DAMAGE_ADDRESS = 0x47F1;

    private final int[] healthGroupByEntity;
    private final int[] damageTypeMatrix;
    private final int[] damageValues;
    private final int[] initialHealthByGroup;
    private final int[] contactDamageByGroup;

    public RomEnemyCombatTables(byte[] romData) {
        Objects.requireNonNull(romData, "ROM data cannot be null");
        healthGroupByEntity = loadUnsigned(
            romData, HEALTH_GROUP_ADDRESS, HEALTH_GROUP_COUNT);
        damageTypeMatrix = loadUnsigned(
            romData, DAMAGE_TYPE_MATRIX_ADDRESS,
            HEALTH_GROUP_COUNT_FOR_DAMAGE * ATTACK_TYPE_COUNT);
        damageValues = loadUnsigned(
            romData, DAMAGE_VALUE_ADDRESS,
            ATTACK_TYPE_COUNT * DAMAGE_VALUES_PER_TYPE);
        initialHealthByGroup = loadUnsigned(
            romData, INITIAL_HEALTH_ADDRESS, HEALTH_GROUP_COUNT_FOR_DAMAGE);
        contactDamageByGroup = loadUnsigned(
            romData, CONTACT_DAMAGE_ADDRESS, HEALTH_GROUP_COUNT_FOR_DAMAGE);
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
    public SwordDamageResult resolveSwordDamage(int entityType,
                                                EnemyAttackContext attackContext) {
        Objects.requireNonNull(attackContext, "Attack context cannot be null");
        int healthGroup = healthGroup(entityType);
        int attackType = attackContext.effectiveDamageType();
        if (attackType < 0) {
            return new SwordDamageResult(entityType & 0xFF, healthGroup, -1, 0, 0);
        }
        int entry = damageTypeEntry(healthGroup, attackType);
        int rawValue = damageValue(attackType, entry);
        return new SwordDamageResult(entityType & 0xFF, healthGroup, attackType,
            entry, rawValue);
    }

    public record SwordDamageResult(int entityType, int healthGroup,
                                    int effectiveDamageType, int damageTableEntry,
                                    int rawValue) {
        public SwordDamageResult {
            if (entityType < 0 || entityType > 0xFF) {
                throw new IllegalArgumentException("Entity type must be an unsigned byte");
            }
            if (healthGroup < 0 || healthGroup >= HEALTH_GROUP_COUNT_FOR_DAMAGE) {
                throw new IllegalArgumentException("Health group out of range: " + healthGroup);
            }
            if (effectiveDamageType < -1 || effectiveDamageType >= ATTACK_TYPE_COUNT) {
                throw new IllegalArgumentException("Attack type out of range: "
                    + effectiveDamageType);
            }
            if (damageTableEntry < 0 || damageTableEntry >= DAMAGE_VALUES_PER_TYPE) {
                throw new IllegalArgumentException("Damage table entry out of range: "
                    + damageTableEntry);
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

    private static int[] loadUnsigned(byte[] romData, int address, int length) {
        int offset = RomBank.romOffset(BANK, address);
        if (offset < 0 || length < 0 || offset > romData.length - length) {
            throw new IllegalArgumentException("ROM table is truncated at bank $03:$"
                + Integer.toHexString(address));
        }
        int[] values = new int[length];
        for (int index = 0; index < length; index++) {
            values[index] = Byte.toUnsignedInt(romData[offset + index]);
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
