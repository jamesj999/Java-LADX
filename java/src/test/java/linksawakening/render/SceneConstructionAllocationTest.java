package linksawakening.render;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

final class SceneConstructionAllocationTest {
    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void representativeBuildAndDrawAllocation(boolean highEntityTypes, boolean borrowedPlanes) {
        var fixture = new SceneConstructionFixture(highEntityTypes);
        var builder = new GameFrameSceneBuilder();
        fixture.configure(builder);
        var state = new ReusableGameFrameState().copyFrom(fixture.state);
        assertTrue(fixture.state.dialogController().isActive());
        assertEquals(10, fixture.state.room().entities().loadedEntities().size());
        assertEquals(3, fixture.state.transientVfxSystem().activeCount());
        assertNotNull(fixture.state.link());
        assertNotNull(fixture.state.inventoryController());
        int[] frame = {0};
        double bytes = AllocationProbe.bytesPerIteration(1000, 200, () -> {
            if (borrowedPlanes) fixture.refill(state, frame[0]++);
            else state.copyFrom(fixture.state).withFrameCounter(frame[0]++);
            state.link().position().setPixelPosition(40 + (frame[0] & 7), 40);
            builder.build(state).drawTo(fixture.output, fixture.gpu);
        });
        System.out.println("Representative build+draw bytes/frame=" + bytes);
        // Measured 328 with EA disabled; the budget adds 25% headroom.
        assertTrue(bytes < 410, "Build+draw bytes/frame=" + bytes);
    }

    @Test
    void constructionAloneReusesTheSceneAndLayerStorage() {
        var fixture = new SceneConstructionFixture();
        var state = new ReusableGameFrameState().copyFrom(fixture.state);
        var builder = new GameFrameSceneBuilder();
        fixture.configure(builder);
        FrameScene scene = builder.build(state);
        double bytes = AllocationProbe.bytesPerIteration(1000, 200, () -> builder.build(state));
        assertSame(scene, builder.build(state));
        System.out.println("Reusable construction bytes/frame=" + bytes);
        assertTrue(bytes < 16);
    }
}
