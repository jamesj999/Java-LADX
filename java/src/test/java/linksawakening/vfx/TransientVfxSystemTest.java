package linksawakening.vfx;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import linksawakening.gpu.Tile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TransientVfxSystemTest {

    @Test
    void spawnStoresTypeCountdownAndPositionInFirstFreeSlot() {
        TransientVfxSystem system = new TransientVfxSystem(2);

        int slotIndex = system.spawn(TransientVfxType.BUSH_LEAVES, 0x34, 0x78);

        assertEquals(0, slotIndex);
        assertEquals(1, system.activeCount());
        assertEquals(
            List.of(new TransientVfxSystem.Slot(0, TransientVfxType.BUSH_LEAVES, 0x1F, 0x34, 0x78)),
            system.activeSlots()
        );
    }

    @Test
    void tickDecrementsCountdownAndExpiresAtZero() {
        TransientVfxSystem system = new TransientVfxSystem(1);
        system.spawn(TransientVfxType.BUSH_LEAVES, 0x20, 0x40);

        for (int i = 0; i < 30; i++) {
            system.tick();
        }

        assertEquals(1, system.activeCount());
        assertEquals(0x01, system.activeSlots().get(0).countdown());

        system.tick();

        assertEquals(0, system.activeCount());
        assertTrue(system.activeSlots().isEmpty());
    }

    @Test
    void expiredSlotIsReusedForTheNextSpawn() {
        TransientVfxSystem system = new TransientVfxSystem(1);

        int firstSlot = system.spawn(TransientVfxType.BUSH_LEAVES, 0x10, 0x20);
        for (int i = 0; i < 31; i++) {
            system.tick();
        }

        int secondSlot = system.spawn(TransientVfxType.BUSH_LEAVES, 0x30, 0x40);

        assertEquals(0, firstSlot);
        assertEquals(0, secondSlot);
        assertEquals(
            List.of(new TransientVfxSystem.Slot(0, TransientVfxType.BUSH_LEAVES, 0x1F, 0x30, 0x40)),
            system.activeSlots()
        );
    }

    @Test
    void clearDropsAllActiveEffects() {
        TransientVfxSystem system = new TransientVfxSystem(2);
        system.spawn(TransientVfxType.BUSH_LEAVES, 0x10, 0x20);
        system.spawn(TransientVfxType.BUSH_LEAVES, 0x30, 0x40);

        system.clear();

        assertEquals(0, system.activeCount());
        assertTrue(system.activeSlots().isEmpty());
    }

    @Test
    void bushLeavesRendererUsesRomFrameSelectionAndRectMetadata() throws IOException {
        byte[] rom = loadRom();
        TransientVfxSpriteSheet spriteSheet = TransientVfxSpriteSheet.loadFromRom(rom);
        CutLeavesEffectRenderer renderer = new CutLeavesEffectRenderer(spriteSheet);

        assertEquals(0, renderer.frameIndexForCountdown(0x1F));
        assertEquals(1, renderer.frameIndexForCountdown(0x1B));
        assertEquals(7, renderer.frameIndexForCountdown(0x00));

        List<CutLeavesEffectRenderer.SpritePlacement> frame = renderer.renderBushLeaves(0x40, 0x60, 0x1F);
        assertEquals(4, frame.size());

        CutLeavesEffectRenderer.SpritePlacement first = frame.get(0);
        assertEquals(0x40 + 2 - 8, first.x());
        assertEquals(0x60 - 4 - 16, first.y());
        assertEquals(0x28, first.tileId());
        assertEquals(0x00, first.attributes());

        CutLeavesEffectRenderer.SpritePlacement second = frame.get(1);
        assertEquals(0x40 - 5 - 8, second.x());
        assertEquals(0x60 + 4 - 16, second.y());
        assertEquals(0x28, second.tileId());
        assertEquals(0x60, second.attributes());

        Tile leafTile = spriteSheet.tile(0x28);
        assertNotNull(leafTile);
        assertEquals(leafTile, first.tile());
        assertFalse(frame.isEmpty());
    }

    @Test
    void bushLeavesRendererConvertsEntityOamCoordinatesToScreenPixels() throws IOException {
        byte[] rom = loadRom();
        TransientVfxSpriteSheet spriteSheet = TransientVfxSpriteSheet.loadFromRom(rom);
        CutLeavesEffectRenderer renderer = new CutLeavesEffectRenderer(spriteSheet);

        List<CutLeavesEffectRenderer.SpritePlacement> frame = renderer.renderBushLeaves(0x48, 0x60, 0x1F);

        CutLeavesEffectRenderer.SpritePlacement first = frame.get(0);
        assertEquals(0x42, first.x());
        assertEquals(0x4C, first.y());
    }

    private static byte[] loadRom() throws IOException {
        try (InputStream input = TransientVfxSystemTest.class
            .getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (input == null) {
                throw new IOException("ROM resource not found: rom/azle.gbc");
            }
            return input.readAllBytes();
        }
    }
}
