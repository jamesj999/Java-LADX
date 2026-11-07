package linksawakening.render;

import linksawakening.gpu.GPU;

/** Applies the frame's ROM-derived animated tile upload before rasterization. */
final class TileUploadRenderLayer implements HardwareRenderLayer {
    private final byte[] data;
    private final int destination;
    TileUploadRenderLayer(byte[] data, int destination) {
        this.data = data;
        this.destination = destination;
    }
    private void upload(GPU gpu) {
        for (int i = 0; i < data.length; i++) gpu.writeVRAM(destination + i, data[i]);
    }

    @Override public void submit(HardwareFrame frame, GPU gpu) { upload(gpu); }
}
