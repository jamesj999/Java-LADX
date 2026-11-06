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
            int idle = Integer.parseInt(fields[2]);
            if (fields[1].contains("=")) {
                Main.headlessButton(fields[0], true);
                int ticks = 0;
                while (!reached(fields[0], fields[1], Main.headlessState())) {
                    if (++ticks > 2000) throw new IllegalStateException("Route blocked: " + line + " " + Main.headlessState());
                    rgba = Main.headlessFrame();
                    appendTrace(trace, frame++);
                }
                Main.headlessButton(fields[0], false);
                do {
                    if (++ticks > 2500) throw new IllegalStateException("Transition did not finish: " + line);
                    rgba = Main.headlessFrame();
                    appendTrace(trace, frame++);
                } while (Main.headlessState().loading());
            }
            int held = fields[1].contains("=") ? 0 : Integer.parseInt(fields[1]);
            if (held > 0) Main.headlessButton(fields[0], true);
            for (int tick = 0; tick < held + idle; tick++) {
                if (tick == held && held > 0) Main.headlessButton(fields[0], false);
                rgba = Main.headlessFrame();
                appendTrace(trace, frame++);
            }
            BufferedImage image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_RGB);
            for (int p = 0; p < 160 * 144; p++) image.setRGB(p % 160, p / 160,
                ((rgba[p*4]&255)<<16)|((rgba[p*4+1]&255)<<8)|(rgba[p*4+2]&255));
            ImageIO.write(image, "png", out.resolve("ours/%02d.png".formatted(segment++)).toFile());
            System.out.println(segment + " " + Main.headlessState());
        }
        Files.writeString(out.resolve("java-trace.tsv"), trace);
    }
    static boolean reached(String button, String condition, Main.HeadlessState s) {
        String[] parts = condition.split("=");
        int target = Integer.decode(parts[1]);
        int value = switch (parts[0]) {
            case "room" -> s.room();
            case "x" -> s.x();
            case "y" -> s.y();
            default -> throw new IllegalArgumentException(condition);
        };
        if (parts[0].equals("room")) return value == target;
        return button.startsWith("left") || button.startsWith("up") ? value <= target : value >= target;
    }

    private static void appendTrace(StringBuilder trace, int frame) {
        var s = Main.headlessState();
        trace.append(frame).append('\t').append(s.map()).append('\t').append(s.room())
            .append('\t').append(s.x()).append('\t').append(s.y()).append('\t')
            .append(s.dialog()).append('\t').append(s.shield()).append('\n');
    }

}
