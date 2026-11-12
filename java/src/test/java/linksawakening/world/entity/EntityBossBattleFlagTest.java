package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.stream.IntStream;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

@RequiresRom
class EntityBossBattleFlagTest {
    private RoomEntityRuntime runtime() throws IOException {
        byte[] rom;
        try (var in = TestRom.open()) {
            rom = in.readAllBytes();
        }
        var slots = IntStream.range(0, 16).mapToObj(RoomEntity::disabled).toList();
        var runtime = RoomEntityRuntime.builder(new RoomEntitySnapshot(slots))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(null)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .chestContentsTable(new linksawakening.world.room.ChestContentsTable(
                        (rom == null ? (linksawakening.rom.Rom) null : new linksawakening.rom.Rom(rom))))
                .romTables(linksawakening.rom.RomTables.load(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .rom(linksawakening.world.entity.EntityRomTestData.view(rom))
                .build();
        runtime.bossRewards.writeEnemyDropResolver(
                new EnemyDropResolver(linksawakening.world.entity.EntityRomTestData.view(rom)));
        return runtime;
    }

    private RoomEntity entity(int slot, int type) {
        return new RoomEntityBuilder()
                .slot(slot)
                .sourceLoadOrder(slot)
                .type(type)
                .at(64, 80)
                .status(EntityStatus.INIT)
                .spriteDefinition(EntitySpriteDefinition.unsupported(type))
                .build();
    }

    @Test
    void laterBossInitSetsFlagAndDeathDoesNotClearItButRoomResetDoes() throws IOException {
        var runtime = runtime();
        assertFalse(runtime.inspection().inBossBattle());
        runtime.slots[15] = entity(15, 0x81);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertTrue(runtime.inspection().inBossBattle());
        runtime.lifecycle.disableEntityWithoutPersistence(15);
        assertTrue(runtime.inspection().inBossBattle());
        runtime.resetRoomVariables();
        assertFalse(runtime.inspection().inBossBattle());
        assertFalse(runtime().inspection().inBossBattle());
    }

    @Test
    void nonBossAndDefeatedBossDoNotSetFlag() throws IOException {
        var runtime = runtime();
        runtime.slots[15] = entity(15, 9);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertFalse(runtime.inspection().inBossBattle());
        runtime.slots[14] = entity(14, 0x81);
        runtime.entityState.roomVariables.writeEntityRoomStatus(0x20);
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));
        assertFalse(runtime.inspection().inBossBattle());
        assertEquals(EntityStatus.DISABLED, runtime.slots[14].status());
    }

    @Test
    void battleFlagSuppressesPowerUpsButStillAllowsOrdinaryDrops() throws IOException {
        var runtime = runtime();
        var source = entity(0, 9);
        runtime.slots[0] = source;
        runtime.slots[15] = entity(15, 0x81);
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        runtime.bossRewards.writeEnemyDropCounters(new EnemyDropResolver.CounterState(11, 0));
        runtime.bossRewards.handleTerminalEnemyDeath(runtime.slots[0], () -> 0);
        assertFalse(runtime.inspection().snapshot().loadedEntities().stream().anyMatch(e -> e.type() == 0x34));
        assertTrue(runtime.inspection().snapshot().loadedEntities().stream().anyMatch(e -> e.type() == 0x2E));
        var peaceful = runtime();
        peaceful.slots[0] = source;
        peaceful.initializeHealthGroup(0);
        peaceful.bossRewards.writeEnemyDropCounters(new EnemyDropResolver.CounterState(11, 0));
        peaceful.bossRewards.handleTerminalEnemyDeath(source, () -> 0);
        assertTrue(peaceful.inspection().snapshot().loadedEntities().stream().anyMatch(e -> e.type() == 0x34));
    }
}
