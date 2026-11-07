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

    private static final class Group {
        long pixels, scenes;
        String example;
    }

    @Override public void beforeEach(ExtensionContext context) {
        var rows = new ArrayList<String>();
        rows.add("scene\tstatus\tdetail");
        context.getStore(KEY).put("rows", rows);
        var groups = new java.util.TreeMap<String, Group>();
        context.getStore(KEY).put("groups", groups);
        boolean probe = Boolean.getBoolean("sceneParity.probe");
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
            int expectedPixels = 0, mismatchPixels = 0;
            var sceneGroups = new java.util.HashSet<String>();
            var evidence = new java.util.LinkedHashSet<String>();
            for (int offset = 0; offset < rgba.length; offset += 4) {
                if (Arrays.equals(rgba, offset, offset + 4, hardware, offset, offset + 4)) continue;
                int pixel = offset / 4;
                if (model.explainsHudPriority(rgba, hardware, pixel)) {
                    expectedPixels++; evidence.add(model.hudEvidence(pixel)); continue;
                }
                String detail = "pixel=" + pixel % 160 + "," + pixel / 160 + " " + model.hudEvidence(pixel);
                if (!probe) {
                    rows.add(id + "\tMISMATCH\t" + detail);
                    throw new AssertionError(context.getDisplayName() + " scene " + id + ": " + detail);
                }
                mismatchPixels++;
                String groupKey = model.mismatchGroup(pixel, rgba, hardware);
                Group group = groups.computeIfAbsent(groupKey, key -> new Group());
                group.pixels++;
                if (sceneGroups.add(groupKey)) group.scenes++;
                if (group.example == null) group.example = "scene=" + id + " " + detail;
            }
            if (mismatchPixels > 0) {
                rows.add(id + "\tMISMATCH\tpixels=" + mismatchPixels + ";expectedPixels=" + expectedPixels);
                return;
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
        @SuppressWarnings("unchecked")
        var groups = (java.util.Map<String, Group>) context.getStore(KEY).remove("groups");
        if (groups != null && !groups.isEmpty()) {
            var report = new ArrayList<String>();
            report.add("pattern\tpixels\tscenes\texample");
            groups.entrySet().stream().sorted((a,b) -> Long.compare(b.getValue().pixels, a.getValue().pixels))
                .forEach(entry -> report.add(entry.getKey() + "\t" + entry.getValue().pixels + "\t"
                    + entry.getValue().scenes + "\t" + entry.getValue().example));
            Files.write(directory.resolve(name + ".groups.tsv"), report);
        }
    }
}
