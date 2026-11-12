package linksawakening.render;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import linksawakening.cutscene.IntroFrameSnapshot;
import linksawakening.cutscene.IntroSprite;
import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

/** ROM-free guards for presentation outside the ordinary overworld frame. */
final class SpecialSceneAllocationTest {
    static GameFrameState intro() {
        int[] tiles = new int[1024], attrs = new int[1024], x = new int[144], y = new int[144];
        for (int i = 0; i < x.length; i++) {
            x[i] = i & 7;
            y[i] = i / 8;
        }
        int[][] palettes = {{0, 0x303030, 0x909090, 0xffffff}};
        var snapshot = new IntroFrameSnapshot.Builder()
                .sceneId("synthetic")
                .substate("waves")
                .frameCounter(7)
                .scrollX(3)
                .scrollY(4)
                .lineScrollX(x)
                .verticalWaveOffset(0)
                .tilemap(tiles)
                .attrmap(attrs)
                .bgPalettes(palettes)
                .objPalettes(palettes)
                .sprites(
                        List.of(new IntroSprite(0, 30, 20, 0, false, false), new IntroSprite(4, 70, 50, 0, true, true)))
                .titleRevealRows(0)
                .lineScrollY(y)
                .animatedTiles(new byte[32])
                .animatedTilesDestination(0x100)
                .build();
        return GameFrameState.empty().withScreen(RenderScreen.CUTSCENE).withIntroFrameSnapshot(snapshot);
    }

    static GameFrameState openInventory(SceneConstructionFixture fixture) {
        var inventory = fixture.state.inventoryController();
        inventory.transition().open(false);
        for (int tick = 0; tick < 200 && !inventory.transition().interactive(); tick++) inventory.tick();
        assertTrue(inventory.transition().interactive());
        return fixture.state.withDialogController(null);
    }

    @Test
    void introBuildAndDrawAllocation() {
        measure(intro());
    }

    @Test
    void openInventoryBuildAndDrawAllocation() {
        var fixture = new SceneConstructionFixture();
        measure(openInventory(fixture), fixture);
    }

    private void measure(GameFrameState input) {
        measure(input, new SceneConstructionFixture());
    }

    private void measure(GameFrameState input, SceneConstructionFixture fixture) {
        var state = new ReusableGameFrameState().copyFrom(input);
        var builder = new GameFrameSceneBuilder();
        fixture.configure(builder);
        double bytes = AllocationProbe.bytesPerIteration(1000, 200, () -> {
            state.copyFrom(input);
            builder.build(state).drawTo(fixture.output, fixture.gpu);
        });
        System.out.println(input.screen() + " special build+draw bytes/frame=" + bytes);
        // Measured zero with EA off; use the 16-byte probe floor for zero baselines.
        assertTrue(bytes < 16, "Special build+draw bytes/frame=" + bytes);
    }
}
