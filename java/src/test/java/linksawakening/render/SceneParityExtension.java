package linksawakening.render;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Measures every FrameScene draw made by the existing tests, including replay checkpoints. */
public final class SceneParityExtension implements BeforeEachCallback, AfterEachCallback {
    private static final ExtensionContext.Namespace KEY = ExtensionContext.Namespace.create(SceneParityExtension.class);

    @Override public void beforeEach(ExtensionContext context) {
        var rows = new ArrayList<String>();
        rows.add("scene\tstatus\tdetail");
        context.getStore(KEY).put("rows", rows);
        FrameScene.DRAW_OBSERVER.set((scene, rgba, gpu) -> {
            String id = Integer.toString(rows.size());
            var blocked = scene.unmigratedLayers();
            if (!blocked.isEmpty()) {
                rows.add(id + "\tBLOCKED\t" + String.join(",", blocked));
                return;
            }
            byte[] hardware = new byte[rgba.length];
            HardwareFrame model = scene.hardwareFrame(gpu);
            model.compose(hardware);
            int expectedPixels = 0;
            var evidence = new java.util.LinkedHashSet<String>();
            for (int offset = 0; offset < rgba.length; offset += 4) {
                if (Arrays.equals(rgba, offset, offset + 4, hardware, offset, offset + 4)) continue;
                int pixel = offset / 4;
                if (model.explainsHudPriority(rgba, hardware, pixel)) {
                    expectedPixels++; evidence.add(model.hudEvidence(pixel)); continue;
                }
                String detail = "pixel=" + pixel % 160 + "," + pixel / 160 + " " + model.hudEvidence(pixel);
                rows.add(id + "\tMISMATCH\t" + detail);
                throw new AssertionError(context.getDisplayName() + " scene " + id + ": " + detail);
            }
            if (expectedPixels > 0) rows.add(id + "\tEXPECTED_FIDELITY_FIX\tpixels=" + expectedPixels
                + "; CGB priority: OBJ wins when BG index=0 or both priority flags clear; PyBoy beach-route 56 overlap frames LCDC=$E7 WY=128 WX=7; HUD " + evidence + "; reference tile=$7F attr=$01 index=0; animated_tiles.asm:$1D57-$1D59");
            else rows.add(id + "\tIDENTICAL\t" + scene.layerCount() + " layers");
        });
    }

    @Override public void afterEach(ExtensionContext context) throws Exception {
        FrameScene.DRAW_OBSERVER.remove();
        @SuppressWarnings("unchecked")
        var rows = (ArrayList<String>) context.getStore(KEY).remove("rows");
        if (rows == null || rows.size() == 1) return;
        String name = context.getRequiredTestClass().getName() + "." + context.getRequiredTestMethod().getName()
            + "-" + Integer.toHexString(context.getUniqueId().hashCode());
        Path directory = Path.of("build", "scene-parity", System.getProperty("sceneParity.task", "test"));
        Files.createDirectories(directory);
        Files.write(directory.resolve(name + ".tsv"), rows);
    }
}
