package linksawakening.render;

import linksawakening.gpu.GPU;

/** Optional completed-frame inspection, supplied explicitly or by a capture-tool service. */
@FunctionalInterface
public interface FrameDrawObserver {
    void drawn(FrameScene scene, byte[] rgba, GPU gpu);

    /** Supplies an optional observer for hosts that cannot pass one on individual draws. */
    interface Provider {
        FrameDrawObserver current();
    }
}
