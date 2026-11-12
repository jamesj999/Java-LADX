package linksawakening.render;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.entity.Link;
import linksawakening.entity.LinkSpriteSheet;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import linksawakening.testing.TestRoms;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;

@RequiresRom
final class LinkScrollVisibilityTest {
    @Test
    void scrollInterpolatedLinkBodyUsesDrawLinkSpriteYGuard() throws Exception {
        byte[] rom;
        try (var input = TestRom.open()) {
            rom = input.readAllBytes();
        }
        var sheet = LinkSpriteSheet.loadFromRom(TestRoms.partial(rom));
        var link = new Link(
                new InputState(),
                new InputConfig(1, 2, 3, 4, 5, 6, 7),
                RomTables.loadFromRom(rom),
                null,
                sheet,
                new PlayerState(),
                null);
        link.position().setPixelPosition(40, 112);
        var scroll = new ScrollController();
        scroll.start(ScrollController.UP, 40, 128, null, 128);
        // Source OAM raw Y=144 at the start is suppressed as a whole body.
        assertEquals(0, visiblePixels(link, scroll));
        scroll.tick(64); // screen top=120, raw Y=$88 is still suppressed for Link.
        assertEquals(0, visiblePixels(link, scroll));
        scroll.tick(8); // screen top=119, raw Y=$87 is allowed.
        assertTrue(visiblePixels(link, scroll) > 0);
        scroll.tick(56);
        assertTrue(visiblePixels(link, scroll) > 0, "Link remains visible after the scroll");
    }

    private static long visiblePixels(Link link, ScrollController scroll) {
        var frame = new HardwareFrame(true, new HardwareFrameDiagnostics());
        frame.withLayerSource("LinkRenderLayer", () -> new LinkRenderLayer(link, scroll).submit(frame, new GPU()));
        byte[] buffer = new byte[160 * 144 * 4];
        frame.compose(buffer);
        long count = 0;
        for (int pixel = 0; pixel < 160 * 144; pixel++) {
            if (frame.diagnostics().fidelitySource(pixel).startsWith("OBJ=LinkRenderLayer ")) count++;
        }
        return count;
    }
}
