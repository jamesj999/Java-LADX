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
        var expectedSources = new java.util.TreeMap<String, Group>();
        context.getStore(KEY).put("expectedSources", expectedSources);
        boolean probe = Boolean.getBoolean("sceneParity.probe");
        FrameScene.DRAW_OBSERVER.set((scene, rgba, gpu, pipeline) -> {
            String id = Integer.toString(rows.size());
            var blocked = scene.unmigratedLayers();
            if (!blocked.isEmpty()) {
                rows.add(id + "\tBLOCKED\t" + String.join(",", blocked));
                return;
            }
            byte[] legacy = new byte[rgba.length];
            byte[] hardware = rgba;
            LegacySceneReference.render(scene, legacy, gpu);
            HardwareFrame model = scene.hardwareFrame(gpu);
            byte[] rebuilt = new byte[rgba.length];
            model.compose(rebuilt);
            if (!Arrays.equals(hardware, rebuilt))
                throw new AssertionError("Compatibility reference mutated hardware output: " + context.getDisplayName());
            int expectedPixels = 0, mismatchPixels = 0;
            var sceneGroups = new java.util.HashSet<String>();
            var sceneExpectedSources = new java.util.HashSet<String>();
            var evidence = new java.util.LinkedHashSet<String>();
            for (int offset = 0; offset < rgba.length; offset += 4) {
                if (Arrays.equals(legacy, offset, offset + 4, hardware, offset, offset + 4)) continue;
                int pixel = offset / 4;
                if (model.explainsHudPriority(legacy, hardware, pixel) || model.explainsInventoryHiding(legacy, hardware, pixel)) {
                    expectedPixels++; evidence.add(model.hudEvidence(pixel));
                    if (model.explainsInventoryHiding(legacy, hardware, pixel))
                        evidence.add("whole-entry inventory hiding: bank1.asm:$5F51-$5F67 rawY>=WY+8");
                    String source = model.fidelitySource(pixel);
                    Group expected = expectedSources.computeIfAbsent(source, key -> new Group());
                    expected.pixels++;
                    if (sceneExpectedSources.add(source)) expected.scenes++;
                    if (expected.example == null) expected.example = "scene=" + id + " pixel=" + pixel % 160 + "," + pixel / 160;
                    continue;
                }
                String detail = "pixel=" + pixel % 160 + "," + pixel / 160 + " " + model.hudEvidence(pixel);
                if (!probe) {
                    rows.add(id + "\tMISMATCH\t" + detail);
                    throw new AssertionError(context.getDisplayName() + " scene " + id + ": " + detail);
                }
                mismatchPixels++;
                String groupKey = model.mismatchGroup(pixel, legacy, hardware);
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
                + "; CGB priority: OBJ wins when BG index=0 or both priority flags clear; PyBoy beach-route 56 overlap frames LCDC=$E7 WY=128 WX=7; HUD " + evidence + "; reference HUD tile=$7F attr=$01 index=0; animated_tiles.asm:$1D57-$1D59; dialog attr=$01 verified in 804 original-ROM prompt-arrow overlap frames (not Link/NPC geometry), LCDC=$E7; dialog_letters.asm:$4A84; Link excluded from hide loop bank1.asm:$5F7F-$5F81");
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
        var expectedSources = (java.util.Map<String, Group>) context.getStore(KEY).remove("expectedSources");
        if (expectedSources != null && !expectedSources.isEmpty()) {
            var report = new ArrayList<String>();
            report.add("source\tpixels\tscenes\texample");
            expectedSources.forEach((source, group) -> report.add(source + "\t" + group.pixels + "\t" + group.scenes + "\t" + group.example));
            Files.write(directory.resolve(name + ".expected.tsv"), report);
        }
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
