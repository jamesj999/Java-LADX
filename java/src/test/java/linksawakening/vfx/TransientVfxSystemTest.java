package linksawakening.vfx;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import linksawakening.gpu.Tile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void swordPokeUsesTheRomTransientTypeAndCountdown() {
        TransientVfxSystem system = new TransientVfxSystem(1);

        int slotIndex = system.spawn(TransientVfxType.SWORD_POKE, 0x40, 0x50);

        assertEquals(0, slotIndex);
        assertEquals(
            List.of(new TransientVfxSystem.Slot(0, TransientVfxType.SWORD_POKE,
                0x0F, 0x40, 0x50)),
            system.activeSlots()
        );

        system.tick();

        assertEquals(0x0E, system.activeSlots().getFirst().countdown());
    }

    @Test
    void laserBeamUsesTheRomTransientTypeAndSixteenFrameCountdown() {
        TransientVfxSystem system = new TransientVfxSystem(1);

        system.spawn(TransientVfxType.LASER_BEAM, 0x44, 0x60);

        assertEquals(
            List.of(new TransientVfxSystem.Slot(0, TransientVfxType.LASER_BEAM,
                0x10, 0x44, 0x60)),
            system.activeSlots());
    }

    @Test
    void swordBeamVfxCarriesTheEntitySpriteVariant() {
        TransientVfxSystem system = new TransientVfxSystem(1);

        system.spawn(TransientVfxType.SWORD_BEAM, 0x44, 0x60, 3);

        assertEquals(TransientVfxType.SWORD_BEAM, system.activeSlots().getFirst().type());
        assertEquals(0x08, system.activeSlots().getFirst().countdown());
        assertEquals(3, system.activeSlots().getFirst().variant());
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

    @Test
    void laserBeamRendererUsesTheRomTileAndAlternatingAttributeBit() throws IOException {
        TransientVfxSpriteSheet spriteSheet = TransientVfxSpriteSheet.loadFromRom(loadRom());
        CutLeavesEffectRenderer renderer = new CutLeavesEffectRenderer(spriteSheet);

        List<CutLeavesEffectRenderer.SpritePlacement> firstFrame = renderer.renderLaserBeam(
            0x44, 0x60, 0x10, 0, 0);
        List<CutLeavesEffectRenderer.SpritePlacement> alternateFrame = renderer.renderLaserBeam(
            0x44, 0x60, 0x10, 1, 0);
        List<CutLeavesEffectRenderer.SpritePlacement> alternateSlot = renderer.renderLaserBeam(
            0x44, 0x60, 0x10, 0, 1);

        assertEquals(1, firstFrame.size());
        assertEquals(0x44 - 0x08, firstFrame.get(0).x());
        assertEquals(0x60 - 0x10, firstFrame.get(0).y());
        assertEquals(0x24, firstFrame.get(0).tileId());
        assertEquals(0x00, firstFrame.get(0).attributes());
        assertEquals(0x10, alternateFrame.get(0).attributes());
        assertEquals(0x10, alternateSlot.get(0).attributes());
        assertNotNull(firstFrame.get(0).tile());
    }

    @Test
    void swordBeamRendererUsesTheRomTwoSpriteVariantAndFlickerTables() throws IOException {
        CutLeavesEffectRenderer renderer = new CutLeavesEffectRenderer(
            TransientVfxSpriteSheet.loadFromRom(loadRom()));

        assertTrue(renderer.renderSwordBeam(0x44, 0x60, 0x08, 0, 0, 0).isEmpty());
        var firstFrame = renderer.renderSwordBeam(0x44, 0x60, 0x08, 1, 0, 0);
        var alternateTable = renderer.renderSwordBeam(0x44, 0x60, 0x08, 3, 0, 0);
        var rotated = renderer.renderSwordBeam(0x44, 0x60, 0x08, 1, 0, 2);

        assertEquals(2, firstFrame.size());
        assertEquals(0x44 - 8, firstFrame.get(0).x());
        assertEquals(0x60 - 16, firstFrame.get(0).y());
        assertEquals(0x08, firstFrame.get(0).tileId());
        assertEquals(0x20, firstFrame.get(0).attributes());
        assertEquals(0x30, alternateTable.get(0).attributes());
        assertEquals(0x04, rotated.get(0).tileId());
    }

    @Test
    void waterSplashRendererUsesRomTwoSpritePhases() throws IOException {
        CutLeavesEffectRenderer renderer = new CutLeavesEffectRenderer(
            TransientVfxSpriteSheet.loadFromRom(loadRom()));

        var first = renderer.renderWaterSplash(0x40, 0x60, 0x07);
        var second = renderer.renderWaterSplash(0x40, 0x60, 0x0F);

        assertEquals(2, first.size());
        assertEquals(0x40 - 0x08 - 0x02, first.get(0).x());
        assertEquals(0x60 - 0x10 - 0x0A, first.get(0).y());
        assertEquals(0x18, first.get(0).tileId());
        assertEquals(0x20, first.get(1).attributes());
        assertNotEquals(first.get(0).x(), second.get(0).x());
        assertNotNull(first.get(0).tile());
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
