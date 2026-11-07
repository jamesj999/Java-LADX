package linksawakening.render;

import linksawakening.gpu.GPU;

/** Historical RGBA layer-composition oracle; intentionally excluded from production sources. */
final class LegacySceneReference {
    static void render(FrameScene scene, byte[] buffer, GPU gpu) {
        IndexedRenderer.clear(buffer);
        RenderContext context = new RenderContext(buffer, gpu);
        for (RenderLayer layer : scene.layersForComparison()) layer.render(context);
    }
}
