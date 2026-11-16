package linksawakening.render;

import linksawakening.gpu.GPU;

/** Optional completed-frame inspection, supplied explicitly by tests or capture tools. */
@FunctionalInterface
interface FrameDrawObserver {
    FrameDrawObserver NONE = (scene, rgba, gpu) -> {};

    void drawn(FrameScene scene, byte[] rgba, GPU gpu);
}
