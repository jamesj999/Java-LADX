package linksawakening.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.rom.GbcBanks;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.state.PlayerState;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import linksawakening.testing.TestRoms;
import linksawakening.ui.InventoryController;
import linksawakening.ui.InventoryMenu;
import linksawakening.ui.InventoryTilemapLoader;
import org.junit.jupiter.api.Test;

@RequiresRom
final class RenderPipelineGpuUploadsTest {
    @Test
    void closedInventoryPatchesMushroomAndPowderWithoutReferenceRendering() throws Exception {
        byte[] bytes;
        try (var stream = TestRom.open()) {
            bytes = stream.readAllBytes();
        }
        var player = new PlayerState();
        var menu = new InventoryMenu(
                InventoryTilemapLoader.loadFromRom(TestRoms.partial(bytes)),
                player,
                linksawakening.gameplay.GameplaySoundSink.none(),
                TestRoms.partial(bytes));
        var controller = new InventoryController(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7), menu);
        GPU gpu = new GPU();
        gpu.loadBaseTiles(Rom.of(bytes));
        FrameScene scene = FrameScene.of(List.of(new InventoryRenderLayer(controller)));
        var observer = TestDrawObservers.CURRENT.get();
        TestDrawObservers.CURRENT.remove();
        try {
            player.ammunition().setHasToadstool(true);
            scene.drawTo(new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4], gpu);
            assertUpload(gpu, bytes, RomSymbols.MushroomTiles);
            player.ammunition().setHasToadstool(false);
            scene.drawTo(new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4], gpu);
            assertUpload(gpu, bytes, RomSymbols.InventoryEquipmentItemsTiles.plus(0xE0));
        } finally {
            if (observer != null) TestDrawObservers.CURRENT.set(observer);
        }
    }

    private static void assertUpload(GPU gpu, byte[] bytes, RomAddress source) {
        byte[] expected = TestRoms.partial(bytes).bytes(source.inBank(GbcBanks.adjust(source.bank())), 32);
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], gpu.readVRAM(0x8E0 + i));
    }
}
