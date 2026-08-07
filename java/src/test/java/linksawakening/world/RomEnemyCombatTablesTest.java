package linksawakening.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RomEnemyCombatTablesTest {

    @Test
    void decodesHealthAndSwordTablesFromTheShippedRom() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());

        assertEquals(0x00, tables.healthGroup(0x09));
        assertEquals(0x01, tables.healthGroup(0x0B));
        assertEquals(0x01, tables.initialHealth(0x09));
        assertEquals(0x02, tables.initialHealth(0x0B));
        assertEquals(0x04, tables.contactDamage(0x09));
        assertEquals(0x04, tables.contactDamage(0x0B));
        assertEquals(0x0C, tables.healthGroup(0x18));
        assertEquals(0x04, tables.initialHealth(0x18));
        assertEquals(0x08, tables.contactDamage(0x18));

        assertEquals(0x01, tables.damageTypeEntry(0x00, 0x00));
        assertEquals(0x02, tables.damageValue(0x01, 0x01));
        assertEquals(0xFF, tables.damageValue(0x00, 0x06));
    }

    @Test
    void resolvesSwordLevelAndEveryRomAttackModifier() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());

        RomEnemyCombatTables.SwordDamageResult levelOne = tables.resolveSwordDamage(
            0x09, new EnemyAttackContext(1, false, false, false, false));
        assertEquals(0, levelOne.effectiveDamageType());
        assertEquals(1, levelOne.damageTableEntry());
        assertEquals(1, levelOne.rawValue());
        assertEquals(1, levelOne.numericDamage());

        RomEnemyCombatTables.SwordDamageResult levelTwo = tables.resolveSwordDamage(
            0x09, new EnemyAttackContext(2, false, false, false, false));
        assertEquals(1, levelTwo.effectiveDamageType());
        assertEquals(2, levelTwo.rawValue());

        RomEnemyCombatTables.SwordDamageResult levelThree = tables.resolveSwordDamage(
            0x09, new EnemyAttackContext(3, false, false, false, false));
        assertEquals(2, levelThree.effectiveDamageType());
        assertEquals(4, levelThree.rawValue());

        assertEquals(1, tables.resolveSwordDamage(0x09,
            new EnemyAttackContext(1, true, false, false, false)).effectiveDamageType());
        assertEquals(1, tables.resolveSwordDamage(0x09,
            new EnemyAttackContext(1, false, true, false, false)).effectiveDamageType());
        assertEquals(1, tables.resolveSwordDamage(0x09,
            new EnemyAttackContext(1, false, false, true, false)).effectiveDamageType());
        assertEquals(1, tables.resolveSwordDamage(0x09,
            new EnemyAttackContext(1, false, false, false, true)).effectiveDamageType());
    }

    @Test
    void resolvesFixedProjectileAttackTypesThroughTheSameRomMatrix() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());

        RomEnemyCombatTables.SwordDamageResult arrow = tables.resolveAttackDamage(0x09, 0x05);
        assertEquals(0, arrow.healthGroup());
        assertEquals(1, arrow.damageTableEntry());
        assertEquals(1, arrow.rawValue());

        RomEnemyCombatTables.SwordDamageResult moblinArrow =
            tables.resolveAttackDamage(0x0B, 0x05);
        assertEquals(1, moblinArrow.healthGroup());
        assertEquals(2, moblinArrow.damageTableEntry());
        assertEquals(4, moblinArrow.rawValue());
    }

    @Test
    void distinguishesIgnoredAndSpecialRawSwordResults() throws IOException {
        RomEnemyCombatTables tables = new RomEnemyCombatTables(loadRom());

        RomEnemyCombatTables.SwordDamageResult ignored = tables.resolveSwordDamage(
            0x15, new EnemyAttackContext(1, false, false, false, false));
        assertEquals(0, ignored.rawValue());
        assertTrue(ignored.ignored());
        assertEquals(0, ignored.numericDamage());

        RomEnemyCombatTables.SwordDamageResult special = tables.resolveSwordDamage(
            0xE9, new EnemyAttackContext(1, false, false, false, false));
        assertEquals(0xFF, special.rawValue());
        assertTrue(special.special());
        assertEquals(0, special.numericDamage());
        assertEquals(0xFF, special.specialAction());
    }

    @Test
    void swordlessContextProducesAnIgnoredResult() throws IOException {
        RomEnemyCombatTables.SwordDamageResult result =
            new RomEnemyCombatTables(loadRom()).resolveSwordDamage(
                0x09, new EnemyAttackContext(0, false, false, false, false));

        assertEquals(-1, result.effectiveDamageType());
        assertTrue(result.ignored());
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = RomEnemyCombatTablesTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
