package linksawakening.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Writes local capture buffers for offline emulator comparisons. */
@linksawakening.testing.RequiresRom
final class CaptureImageWriter {
    private CaptureImageWriter() {}

    static byte[] readRom() throws IOException {
        try (var stream = linksawakening.testing.TestRom.open()) {
            return stream.readAllBytes();
        }
    }

    static void write(byte[] pixels, Path output) throws IOException {
        BufferedImage image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 144; y++)
            for (int x = 0; x < 160; x++) {
                int i = (y * 160 + x) * 4;
                image.setRGB(
                        x,
                        y,
                        0xff000000 | ((pixels[i] & 255) << 16) | ((pixels[i + 1] & 255) << 8) | (pixels[i + 2] & 255));
            }
        ImageIO.write(image, "png", output.toFile());
    }
}
