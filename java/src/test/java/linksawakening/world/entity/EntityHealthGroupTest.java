package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

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
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        runtime.bossRewards.writeEnemyDropResolver(new EnemyDropResolver(rom));
        return runtime;
    }

    @Test
    void initializesResetsAndReinitializesSlotGroup() throws IOException {
        var runtime = runtime();
        assertEquals(runtime.enemyCombatTables.healthGroup(0x09), runtime.entityState.slotView.healthGroup(0));
        runtime.entityState.slotView.setHealthGroup(0, 12);
        runtime.lifecycle.disableEntityWithoutPersistence(0);
        assertEquals(0, runtime.entityState.slotView.healthGroup(0));
        runtime.slots[0] = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x18)
                .at(64, 80)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x18))
                .build();
        runtime.initializeHealthGroup(0);
        assertEquals(12, runtime.entityState.slotView.healthGroup(0));
    }

    @Test
    void gibdoTransformationReinitializesGroupFromTheNewType() throws IOException {
        var runtime = runtime();
        var old = runtime.slots[0];
        runtime.slots[0] = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1F)
                .at(old.x(), old.y())
                .status(EntityStatus.BURNING)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x1F))
                .build();
        runtime.entityState.slotView.setHealthGroup(0, 0);
        runtime.lifecycle.finishBurning(runtime.slots[0]);
        assertEquals(runtime.enemyCombatTables.healthGroup(0x1E), runtime.entityState.slotView.healthGroup(0));
    }

    @Test
    void mutatedGroupChangesContactAndSwordDamageWithoutChangingType() throws IOException {
        var runtime = runtime();
        assertEquals(4, runtime.contactDamage(0));
        runtime.entityState.slotView.setHealthGroup(0, 12);
        assertEquals(8, runtime.contactDamage(0));
        // Find a ROM group with different sword damage, then exercise combat itself.
        var attack = new EnemyAttackContext(1, false, false, false, false);
        int original = runtime.enemyCombatTables.resolveSwordDamage(9, attack).numericDamage();
        int group = IntStream.range(0, 0x35)
                .filter(g -> runtime.enemyCombatTables
                                .resolveSwordDamage(9, g, attack)
                                .numericDamage()
                        != original)
                .findFirst()
                .orElseThrow();
        runtime.entityState.slotView.setHealthGroup(0, group);
        runtime.entityState.combatTables.writeEnemyHealth(0, 100);
        var events = runtime.resolveCombat(new EntitySwordContactFrame(1, 100, 100, false, true, true, 48, 32, 64, 32));
        assertEquals(1, events.size());
        assertEquals(
                runtime.enemyCombatTables.resolveSwordDamage(9, group, attack).numericDamage(),
                events.get(0).enemyDamage());
        assertEquals(9, runtime.slots[0].type());
    }

    @Test
    void mutatedGroupSuppressesDropForTheSameEntityType() throws IOException {
        var normal = runtime();
        normal.bossRewards.handleTerminalEnemyDeath(normal.slots[0], () -> 0);
        assertTrue(normal.inspection().snapshot().loadedEntities().stream().anyMatch(e -> e.type() == 0x2E));
        var changed = runtime();
        changed.entityState.slotView.setHealthGroup(0, 12); // Destroyed group offset zero: no drop.
        changed.bossRewards.handleTerminalEnemyDeath(changed.slots[0], () -> 0);
        assertTrue(changed.inspection().snapshot().loadedEntities().isEmpty());
    }
}
