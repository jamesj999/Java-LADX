package linksawakening.world;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomEntityRuntimeChestTest {

    @Test
    void chestInitializesWithTheRomOpenAnimationAndRewardEvent() {
        byte[] rom = loadRom();
        ChestContentsTable chestTable = new ChestContentsTable(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot(), false, () -> 0,
            new EntitySpriteHandlerCatalog(rom), new RomEnemyCombatTables(rom), chestTable);

        int slot = runtime.spawnChestWithItem(0x28, 0x50,
            ChestContentsTable.CHEST_POWER_BRACELET);
        assertEquals(EntityRoomLoader.MAX_ENTITIES - 1, slot);
        assertEquals(EntityStatus.INIT, runtime.snapshot().slots().get(slot).status());
        assertEquals(0x60, runtime.snapshot().slots().get(slot).y());

        runtime.tick(0, 0, 0, () -> 0);

        RoomEntity initialized = runtime.snapshot().slots().get(slot);
        assertEquals(EntityStatus.ACTIVE, initialized.status());
        assertEquals(0x58, initialized.y());
        assertEquals(ChestContentsTable.CHEST_POWER_BRACELET, initialized.spriteVariant());
        assertEquals(0xC2, runtime.physicsFlags(slot));
        assertTrue(runtime.consumePendingEntityEvents().stream()
            .anyMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                && event.soundId() == 0x04));
        assertEquals(1, runtime.consumePendingChestRewardEvents().size());
    }

    @Test
    void chestEmitsTheRomPresentationSoundAtInertiaEightAndDialogAtThirtyEight() {
        byte[] rom = loadRom();
        ChestContentsTable chestTable = new ChestContentsTable(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot(), false, () -> 0,
            new EntitySpriteHandlerCatalog(rom), new RomEnemyCombatTables(rom), chestTable);
        int slot = runtime.spawnChestWithItem(0x28, 0x50,
            ChestContentsTable.CHEST_RUPEES_50);

        runtime.tick(0, 0, 0, () -> 0);
        runtime.consumePendingEntityEvents();
        runtime.consumePendingChestRewardEvents();
        for (int frame = 1; frame <= 8; frame++) {
            runtime.tick(frame, 0, 0, () -> 0);
        }

        assertTrue(runtime.consumePendingEntityEvents().stream()
            .anyMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE
                && event.soundId() == 0x01));

        for (int frame = 9; frame <= 0x26; frame++) {
            runtime.tick(frame, 0, 0, () -> 0);
        }
        assertEquals(List.of(new RoomEntityRuntime.DialogRequest(0, 0xAC)),
            runtime.consumePendingDialogRequests());
        assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(slot).status());

        runtime.tick(0x27, 0, 0, () -> 0);
        assertTrue(runtime.consumePendingDialogRequests().isEmpty());
        runtime.tick(0x28, 0, 0, () -> 0);
        assertFalse(runtime.snapshot().slots().get(slot).loaded());
    }

    @Test
    void zolChestUsesTheRomSpawnStateBeforeTheZolHandlerAdvancesIt() {
        byte[] rom = loadRom();
        RoomEntityRuntime runtime = RoomEntityRuntime.from(emptySnapshot(), false, () -> 0,
            new EntitySpriteHandlerCatalog(rom), new RomEnemyCombatTables(rom),
            new ChestContentsTable(rom));
        int chestSlot = runtime.spawnChestWithItem(0x28, 0x50, ChestContentsTable.CHEST_ZOL);

        runtime.tick(0, 0, 0, () -> 0);
        runtime.tick(1, 0, 0, () -> 0);

        int zolSlot = chestSlot - 1;
        RoomEntity zol = runtime.snapshot().slots().get(zolSlot);
        assertEquals(0x1B, zol.type());
        assertEquals(0x06, zol.z());
        assertEquals(3, runtime.zolState(zolSlot));
        assertEquals(0x08, runtime.zolSpeedX(zolSlot));
        assertEquals(0x18, runtime.zolSpeedZ(zolSlot));
        assertEquals(0x50, runtime.zolPrivateCountdown1(zolSlot));
        assertTrue(runtime.consumePendingEntityEvents().stream()
            .anyMatch(event -> event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE
                && event.soundId() == 0x1D));
    }

    private static RoomEntitySnapshot emptySnapshot() {
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        return new RoomEntitySnapshot(slots);
    }

    private static byte[] loadRom() {
        try (var stream = RoomEntityRuntimeChestTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load ROM", exception);
        }
    }
}
