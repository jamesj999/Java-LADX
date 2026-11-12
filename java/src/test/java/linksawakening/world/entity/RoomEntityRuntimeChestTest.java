package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.room.ChestContentsTable;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomEntityRuntimeChestTest {

    @Test
    void chestInitializesWithTheRomOpenAnimationAndRewardEvent() {
        byte[] rom = loadRom();
        ChestContentsTable chestTable = new ChestContentsTable(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot())
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(new EntitySpriteHandlerCatalog(rom))
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .chestContentsTable(chestTable)
                .build();

        int slot = runtime.spawnChestWithItem(0x28, 0x50, ChestContentsTable.CHEST_POWER_BRACELET);
        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, slot);
        assertEquals(
                EntityStatus.INIT,
                runtime.inspection().snapshot().slots().get(slot).status());
        assertEquals(0x60, runtime.inspection().snapshot().slots().get(slot).y());

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));

        RoomEntity initialized = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(EntityStatus.ACTIVE, initialized.status());
        assertEquals(0x58, initialized.y());
        assertEquals(ChestContentsTable.CHEST_POWER_BRACELET, initialized.spriteVariant());
        assertEquals(0xC2, runtime.enemies().physicsFlags(slot));
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x04));
        assertEquals(1, runtime.consumePendingChestRewardEvents().size());
    }

    @Test
    void chestEmitsTheRomPresentationSoundAtInertiaEightAndDialogAtThirtyEight() {
        byte[] rom = loadRom();
        ChestContentsTable chestTable = new ChestContentsTable(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot())
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(new EntitySpriteHandlerCatalog(rom))
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .chestContentsTable(chestTable)
                .build();
        int slot = runtime.spawnChestWithItem(0x28, 0x50, ChestContentsTable.CHEST_RUPEES_50);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        runtime.consumePendingEntityEvents();
        runtime.consumePendingChestRewardEvents();
        for (int frame = 1; frame <= 8; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }

        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x01));

        for (int frame = 9; frame <= 0x26; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }
        assertEquals(List.of(new DialogRequest(0, 0xAC)), runtime.consumePendingDialogRequests());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(slot).status());

        RoomEntity displayedItem = runtime.inspection().snapshot().slots().get(slot);
        runtime.configuration().setDialogActive(true);
        for (int frame = 0; frame < 240; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
            assertEquals(displayedItem, runtime.inspection().snapshot().slots().get(slot));
        }
        assertTrue(runtime.consumePendingDialogRequests().isEmpty());
        runtime.configuration().setDialogActive(false);
        runtime.tick(EntityTestFrames.frame(runtime, 0x27, 0, 0, () -> 0));
        assertTrue(runtime.consumePendingDialogRequests().isEmpty());
        runtime.tick(EntityTestFrames.frame(runtime, 0x28, 0, 0, () -> 0));
        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
    }

    @Test
    void zolChestUsesTheRomSpawnStateBeforeTheZolHandlerAdvancesIt() {
        byte[] rom = loadRom();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(emptySnapshot())
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(new EntitySpriteHandlerCatalog(rom))
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .chestContentsTable(new ChestContentsTable(rom))
                .build();
        int chestSlot = runtime.spawnChestWithItem(0x28, 0x50, ChestContentsTable.CHEST_ZOL);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));

        int zolSlot = chestSlot - 1;
        RoomEntity zol = runtime.inspection().snapshot().slots().get(zolSlot);
        assertEquals(0x1B, zol.type());
        assertEquals(0x06, zol.z());
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).zolState(zolSlot));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).zolSpeedX(zolSlot));
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).zolSpeedZ(zolSlot));
        assertEquals(0x50, new EntityEnemyInspectionQueries(runtime.enemies()).zolPrivateCountdown1(zolSlot));
        assertTrue(runtime.consumePendingEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x1D));
    }

    private static RoomEntitySnapshot emptySnapshot() {
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return new RoomEntitySnapshot(slots);
    }

    private static byte[] loadRom() {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load ROM", exception);
        }
    }
}
