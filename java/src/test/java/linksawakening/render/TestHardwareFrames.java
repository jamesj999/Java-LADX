package linksawakening.render;

import linksawakening.gpu.GPU;

/** Test-only attachment and access for pixel provenance. */
final class TestHardwareFrames {
    private TestHardwareFrames() {}

    static HardwareFrameDiagnostics diagnostics(HardwareFrame frame) {
        return (HardwareFrameDiagnostics) frame.observer();
    }

    static HardwareFrame hardwareFrame(FrameScene scene, GPU gpu, HardwareFrameDiagnostics data) {
        return scene.hardwareFrame(gpu, new HardwareFrame(true, data));
    }
}
