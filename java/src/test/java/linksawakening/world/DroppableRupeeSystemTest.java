package linksawakening.world;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.IntSupplier;

import linksawakening.gpu.GPU;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DroppableRupeeSystemTest {

    private static final int OBJECT_BUSH = 0x5C;
    private static final int OBJECT_BUSH_GROUND_STAIRS = 0xD3;
    private static final int DROP_WORLD_X = 0x48;
    private static final int DROP_WORLD_Y = 0x60;

    private static final byte[] ROM = loadRom();

    @Test
    void bushDropRequiresRomOneInEightGateAndEvenRupeeSelectionBit() {
        DroppableRupeeSystem blockedByGate = new DroppableRupeeSystem(4, queuedBytes(0x01));
        assertFalse(blockedByGate.maybeSpawnFromBush(OBJECT_BUSH, DROP_WORLD_X, DROP_WORLD_Y));

        DroppableRupeeSystem selectedHeart = new DroppableRupeeSystem(4, queuedBytes(0x00, 0x01));
        assertFalse(selectedHeart.maybeSpawnFromBush(OBJECT_BUSH, DROP_WORLD_X, DROP_WORLD_Y));

        DroppableRupeeSystem selectedRupee = new DroppableRupeeSystem(4, queuedBytes(0x00, 0x00));
        assertTrue(selectedRupee.maybeSpawnFromBush(OBJECT_BUSH, DROP_WORLD_X, DROP_WORLD_Y));
        assertEquals(1, selectedRupee.activeCount());
    }

    @Test
    void stairsBushNeverSpawnsRupeeEvenWhenDropGatePasses() {
        DroppableRupeeSystem system = new DroppableRupeeSystem(4, queuedBytes(0x00, 0x00));

        assertFalse(system.maybeSpawnFromBush(OBJECT_BUSH_GROUND_STAIRS, DROP_WORLD_X, DROP_WORLD_Y));
        assertEquals(0, system.activeCount());
    }

    @Test
    void spawnedRupeeUsesRomAnchorAndTimers() {
        DroppableRupeeSystem system = new DroppableRupeeSystem(4, queuedBytes(0x00, 0x00));

        assertTrue(system.maybeSpawnFromBush(OBJECT_BUSH, DROP_WORLD_X, DROP_WORLD_Y));

        List<DroppableRupeeSystem.Slot> slots = system.activeSlots();
        assertEquals(1, slots.size());
        DroppableRupeeSystem.Slot slot = slots.get(0);
        assertEquals(DROP_WORLD_X, slot.worldX());
        assertEquals(DROP_WORLD_Y, slot.worldY());
        assertEquals(0, slot.posZSubpixels());
        assertEquals(0x80, slot.slowCountdown());
        assertEquals(0x18, slot.pickupDelay());
        assertTrue(slot.visible());
    }

    @Test
    void rupeeCannotBeCollectedUntilPickupDelayExpiresAndThenAddsOne() {
        DroppableRupeeSystem system = new DroppableRupeeSystem(4, queuedBytes(0x00, 0x00));
        PlayerState player = new PlayerState();
        player.setRupees(17);

        assertTrue(system.maybeSpawnFromBush(OBJECT_BUSH, DROP_WORLD_X, DROP_WORLD_Y));

        for (int i = 0; i < 0x17; i++) {
            system.tick(0, 0, player);
        }
        assertEquals(17, player.rupees());
        assertEquals(1, system.activeCount());

        system.tick(DROP_WORLD_X - 8, DROP_WORLD_Y - 16, player);
        assertEquals(18, player.rupees());
        assertEquals(0, system.activeCount());
    }

    @Test
    void rupeeDespawnMatchesSlowCountdownLifetime() {
        DroppableRupeeSystem system = new DroppableRupeeSystem(4, queuedBytes(0x00, 0x00));
        PlayerState player = new PlayerState();

        assertTrue(system.maybeSpawnFromBush(OBJECT_BUSH, DROP_WORLD_X, DROP_WORLD_Y));

        for (int i = 0; i < 0x80; i++) {
            system.tick(0, 0, player);
        }

        assertEquals(0, system.activeCount());
    }

    @Test
    void rupeeRenderUsesGameplaySpriteTilesLoadedFromRom() {
        DroppableRupeeSystem system = new DroppableRupeeSystem(4, queuedBytes(0x00, 0x00));
        GPU gpu = new GPU();
        gpu.loadBaseOverworldTiles(ROM);
        byte[] buffer = new byte[160 * 144 * 4];

        assertTrue(system.maybeSpawnFromBush(OBJECT_BUSH, DROP_WORLD_X, DROP_WORLD_Y));
        system.render(buffer, gpu);

        int nonTransparentPixels = 0;
        for (int y = DROP_WORLD_Y - 16; y < DROP_WORLD_Y; y++) {
            for (int x = DROP_WORLD_X - 4; x < DROP_WORLD_X + 4; x++) {
                int offset = (y * 160 + x) * 4;
                if ((buffer[offset] & 0xFF) != 0
                    || (buffer[offset + 1] & 0xFF) != 0
                    || (buffer[offset + 2] & 0xFF) != 0) {
                    nonTransparentPixels++;
                }
            }
        }

        assertTrue(nonTransparentPixels > 0);
    }

    @Test
    void rupeeRenderUsesBlueGameplayPickupPalette() {
        DroppableRupeeSystem system = new DroppableRupeeSystem(4, queuedBytes(0x00, 0x00));
        GPU gpu = new GPU();
        gpu.loadBaseOverworldTiles(ROM);
        byte[] buffer = new byte[160 * 144 * 4];

        assertTrue(system.maybeSpawnFromBush(OBJECT_BUSH, DROP_WORLD_X, DROP_WORLD_Y));
        system.render(buffer, gpu);

        boolean sawBlue = false;
        boolean sawRed = false;
        for (int y = DROP_WORLD_Y - 16; y < DROP_WORLD_Y; y++) {
            for (int x = DROP_WORLD_X - 4; x < DROP_WORLD_X + 4; x++) {
                int offset = (y * 160 + x) * 4;
                int r = buffer[offset] & 0xFF;
                int g = buffer[offset + 1] & 0xFF;
                int b = buffer[offset + 2] & 0xFF;
                if (r == 0x70 && g == 0xA8 && b == 0xF8) {
                    sawBlue = true;
                }
                if (r == 0xF8 && g == 0xB0 && b == 0x30) {
                    sawRed = true;
                }
            }
        }

        assertTrue(sawBlue);
        assertFalse(sawRed);
    }

    private static IntSupplier queuedBytes(int... values) {
        return new IntSupplier() {
            private int index;

            @Override
            public int getAsInt() {
                if (index >= values.length) {
                    throw new AssertionError("Random byte queue exhausted");
                }
                return values[index++] & 0xFF;
            }
        };
    }

    private static byte[] loadRom() {
        try (InputStream in = DroppableRupeeSystemTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (in == null) {
                throw new IllegalStateException("Missing ROM resource: rom/azle.gbc");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read ROM resource", e);
        }
    }
}
