package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.stream.IntStream;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

@RequiresRom
class EntityHealthGroupTest {
    private RoomEntityRuntime runtime() throws IOException {
        byte[] rom;
        try (var input = TestRom.open()) {
            rom = input.readAllBytes();
        }
        var slots = IntStream.range(0, 16)
                .mapToObj(slot -> slot == 0
                        ? new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x09)
                                .at(64, 80)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x09))
                                .build()
                        : RoomEntity.disabled(slot))
                .toList();
        var runtime = RoomEntityRuntime.builder(new RoomEntitySnapshot(slots))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(null)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .build();
        runtime.execution()
                .bossRewards()
                .state
                .writeEnemyDropResolver(new EnemyDropResolver(linksawakening.world.entity.EntityRomTestData.view(rom)));
        return runtime;
    }

    @Test
    void initializesResetsAndReinitializesSlotGroup() throws IOException {
        var runtime = runtime();
        assertEquals(
                runtime.inspection().enemyCombatTables.healthGroup(0x09),
                runtime.execution().slot().healthGroup(0));
        runtime.execution().slot().setHealthGroup(0, 12);
        runtime.lifecycle().disableEntityWithoutPersistence(0);
        assertEquals(0, runtime.execution().slot().healthGroup(0));
        runtime.inspection()
                .slots
                .replace(
                        0,
                        new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x18)
                                .at(64, 80)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x18))
                                .build());
        runtime.lifecycle().initializeHealthGroup(0);
        assertEquals(12, runtime.execution().slot().healthGroup(0));
    }

    @Test
    void gibdoTransformationReinitializesGroupFromTheNewType() throws IOException {
        var runtime = runtime();
        var old = runtime.inspection().slots.get(0);
        runtime.inspection()
                .slots
                .replace(
                        0,
                        new RoomEntityBuilder()
                                .sourceLoadOrder(0)
                                .type(0x1F)
                                .at(old.x(), old.y())
                                .status(EntityStatus.BURNING)
                                .spriteDefinition(EntitySpriteDefinition.unsupported(0x1F))
                                .build());
        runtime.execution().slot().setHealthGroup(0, 0);
        runtime.lifecycle().finishBurning(runtime.inspection().slots.get(0));
        assertEquals(
                runtime.inspection().enemyCombatTables.healthGroup(0x1E),
                runtime.execution().slot().healthGroup(0));
    }

    @Test
    void mutatedGroupChangesContactAndSwordDamageWithoutChangingType() throws IOException {
        var runtime = runtime();
        assertEquals(4, runtime.execution().slotProperties().contactDamage(0));
        runtime.execution().slot().setHealthGroup(0, 12);
        assertEquals(8, runtime.execution().slotProperties().contactDamage(0));
        // Find a ROM group with different sword damage, then exercise combat itself.
        var attack = new EnemyAttackContext(1, false, false, false, false);
        int original = runtime.inspection()
                .enemyCombatTables
                .resolveSwordDamage(9, attack)
                .numericDamage();
        int group = IntStream.range(0, 0x35)
                .filter(g -> runtime.inspection()
                                .enemyCombatTables
                                .resolveSwordDamage(9, g, attack)
                                .numericDamage()
                        != original)
                .findFirst()
                .orElseThrow();
        runtime.execution().slot().setHealthGroup(0, group);
        runtime.inspection().combatTables.writeEnemyHealth(0, 100);
        var events = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 100, 100, false, true, true, 48, 32, 64, 32));
        assertEquals(1, events.size());
        assertEquals(
                runtime.inspection()
                        .enemyCombatTables
                        .resolveSwordDamage(9, group, attack)
                        .numericDamage(),
                events.get(0).enemyDamage());
        assertEquals(9, runtime.inspection().slots.get(0).type());
    }

    @Test
    void mutatedGroupSuppressesDropForTheSameEntityType() throws IOException {
        var normal = runtime();
        normal.execution()
                .bossRewards()
                .handleTerminalEnemyDeath(normal.inspection().slots.get(0), () -> 0);
        assertTrue(normal.inspection().snapshot().loadedEntities().stream().anyMatch(e -> e.type() == 0x2E));
        var changed = runtime();
        changed.execution().slot().setHealthGroup(0, 12); // Destroyed group offset zero: no drop.
        changed.execution()
                .bossRewards()
                .handleTerminalEnemyDeath(changed.inspection().slots.get(0), () -> 0);
        assertTrue(changed.inspection().snapshot().loadedEntities().isEmpty());
    }
}
