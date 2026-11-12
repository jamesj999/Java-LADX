package linksawakening.render;

import linksawakening.gpu.GPU;

/** Optional completed-frame inspection, supplied explicitly by tests or capture tools. */
@FunctionalInterface
public interface FrameDrawObserver {
    void drawn(FrameScene scene, byte[] rgba, GPU gpu);
}
