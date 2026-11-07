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
            scene.drawTo(hardware, gpu, RenderPipeline.HARDWARE);
            int difference = Arrays.mismatch(rgba, hardware);
            if (difference >= 0) {
                int pixel = difference / 4;
                String detail = "pixel=" + pixel % 160 + "," + pixel / 160
                    + " channel=" + difference % 4 + " legacy=" + (rgba[difference] & 255)
                    + " hardware=" + (hardware[difference] & 255);
                rows.add(id + "\tMISMATCH\t" + detail);
                throw new AssertionError(context.getDisplayName() + " scene " + id + ": " + detail);
            }
            rows.add(id + "\tIDENTICAL\t" + scene.layerCount() + " layers");
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
