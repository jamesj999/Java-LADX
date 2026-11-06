package linksawakening;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Exercises Main, without scene injection, using the shared button route. */
public final class OpeningRouteCapture {
    public static void main(String[] args) throws Exception {
        Path route = Path.of(args[0]);
        Path out = Path.of(args[1]);
        Files.createDirectories(out.resolve("ours"));
        Main.initializeHeadlessNewGame();
        StringBuilder trace = new StringBuilder();
        int frame = 0, segment = 0;
        byte[] rgba = null;
        for (String line : Files.readAllLines(route)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] fields = line.split("\\s+");
            int held = Integer.parseInt(fields[1]), idle = Integer.parseInt(fields[2]);
            if (held > 0) Main.headlessButton(fields[0], true);
            for (int tick = 0; tick < held + idle; tick++) {
                if (tick == held && held > 0) Main.headlessButton(fields[0], false);
                rgba = Main.headlessFrame();
                var s = Main.headlessState();
                trace.append(frame++).append('\t').append(s.map()).append('\t').append(s.room())
                    .append('\t').append(s.x()).append('\t').append(s.y()).append('\t')
                    .append(s.dialog()).append('\t').append(s.shield()).append('\n');
            }
            BufferedImage image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_RGB);
            for (int p = 0; p < 160 * 144; p++) image.setRGB(p % 160, p / 160,
                ((rgba[p*4]&255)<<16)|((rgba[p*4+1]&255)<<8)|(rgba[p*4+2]&255));
            ImageIO.write(image, "png", out.resolve("ours/%02d.png".formatted(segment++)).toFile());
            System.out.println(segment + " " + Main.headlessState());
        }
        Files.writeString(out.resolve("java-trace.tsv"), trace);
    }
}
