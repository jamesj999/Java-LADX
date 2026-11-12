package linksawakening.render;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Tile;
import linksawakening.ui.InventoryTransition;
import org.junit.jupiter.api.Test;

final class InventoryFadeRenderLayerTest {
    @Test
    void dxFadeAppliesAlternatingBgAndObjStepsWithoutFlatteningLayerParity() {
        var transition = new InventoryTransition();
        transition.open(false);
        for (int frame = 0; frame <= 10; frame++) transition.tick();
        Tile solid = new Tile();
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) solid.setPixel(x, y, 1);
        int[] black = {0, 0, 0, 0};
        HardwareRenderLayer objects = (frame, gpu) -> {
            frame.backgroundTile(solid, 0, 0, false, false, black, false);
            frame.append(solid, 0, 0, false, false, black, false);
        };
        HardwareRenderLayer background = (frame, gpu) -> frame.backgroundTile(solid, 0, 0, false, false, black, false);
        var scene = FrameScene.of(List.of(
                new InventoryFadeRenderLayer(objects, transition),
                new InventoryFadeRenderLayer(background, transition)));
        byte[] hardware = new byte[160 * 144 * 4], legacy = hardware.clone();
        scene.hardwareFrame(new GPU()).compose(hardware);
        LegacySceneReference.render(scene, legacy, new GPU());
        // $20:6C4F: BG gains 4 first, OBJ gains 4 on the following call.
        assertEquals(0, hardware[0] & 255);
        assertEquals(0x21, legacy[0] & 255);
        // This historical painter difference must remain observable to strict parity.
        assertFalse(java.util.Arrays.equals(hardware, legacy));
    }
}
