package linksawakening;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Exercises Main, without scene injection, using the shared button route. */
public final class OpeningRouteCapture {
    private static int lastHealth = 24;
    public static void main(String[] args) throws Exception {
        Path route = Path.of(args[0]);
        Path out = Path.of(args[1]);
        Files.createDirectories(out.resolve("ours"));
        Files.deleteIfExists(out.resolve("java.json"));
        Main.initializeHeadlessNewGame();
        StringBuilder trace = new StringBuilder();
        int frame = 0, segment = 0;
        byte[] rgba = null;
        for (String line : Files.readAllLines(route)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] fields = line.split("\\s+");
            int idle = Integer.parseInt(fields[2]);
            if (fields[1].contains("=")) {
                Main.headlessButton(fields[0].replace("*", ""), true);
                int ticks = 0;
                while (!reached(fields[0], fields[1], Main.headlessState())) {
                    if (++ticks > 2000) throw new IllegalStateException("Route blocked: " + line + " " + Main.headlessState() + " entities " + Main.headlessEntityPositions());
                    pulseButtons(fields[0], ticks - 1);
                    rgba = Main.headlessFrame();
                    appendTrace(trace, frame++);
                }
                Main.headlessButton(fields[0].replace("*", ""), false);
                do {
                    if (++ticks > 2500) throw new IllegalStateException("Transition did not finish: " + line);
                    rgba = Main.headlessFrame();
                    appendTrace(trace, frame++);
                } while (Main.headlessState().loading());
            }
            int held = fields[1].contains("=") ? 0 : Integer.parseInt(fields[1]);
            if (held > 0) Main.headlessButton(fields[0].replace("*", ""), true);
            for (int tick = 0; tick < held + idle; tick++) {
                if (tick == held && held > 0) Main.headlessButton(fields[0].replace("*", ""), false);
                if (tick < held) pulseButtons(fields[0], tick);
                rgba = Main.headlessFrame();
                appendTrace(trace, frame++);
            }
            if (held > 0 && idle == 0) Main.headlessButton(fields[0].replace("*", ""), false);
            BufferedImage image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_RGB);
            for (int p = 0; p < 160 * 144; p++) image.setRGB(p % 160, p / 160,
                ((rgba[p*4]&255)<<16)|((rgba[p*4+1]&255)<<8)|(rgba[p*4+2]&255));
            ImageIO.write(image, "png", out.resolve("ours/%02d.png".formatted(segment++)).toFile());
            System.out.println(segment + " " + Main.headlessState()
                + " entities " + Main.headlessEntityPositions());
        }
        Files.writeString(out.resolve("java-trace.tsv"), trace);
        String routeHash = java.util.HexFormat.of().formatHex(
            java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(route)));
        Files.writeString(out.resolve("java.json"),
            "{\"route_sha256\":\"" + routeHash + "\",\"checkpoints\":" + segment + "}");
    }
    static void pulseButtons(String buttons, int tick) {
        for (String button : buttons.split("\\+")) {
            if (!button.endsWith("*")) continue;
            String key = button.substring(0, button.length() - 1);
            if (tick % 24 == 8) Main.headlessButton(key, false);
            else if (tick > 0 && tick % 24 == 0) Main.headlessButton(key, true);
        }
    }

    static boolean reached(String button, String condition, Main.HeadlessState s) {
        String[] parts = condition.split("=");
        int target = Integer.decode(parts[1]);
        int value = switch (parts[0]) {
            case "room" -> s.room();
            case "dialog" -> s.dialog() ? 1 : 0;
            case "sword" -> s.sword();
            case "inventorytick" -> s.inventoryTick();
            case "bgtick" -> s.bgTick();
            case "smallkeys" -> s.smallKeys();
            case "nightmarekey" -> s.nightmareKey();
            case "x" -> s.x();
            case "y" -> s.y();
            default -> throw new IllegalArgumentException(condition);
        };
        if (!parts[0].equals("x") && !parts[0].equals("y")) return value == target;
        return button.startsWith("left") || button.startsWith("up") ? value <= target : value >= target;
    }

    private static void appendTrace(StringBuilder trace, int frame) {
        var s = Main.headlessState();
        if (s.health() < lastHealth) System.out.println("Damage at " + frame + " " + s + " entities " + Main.headlessEntityPositions());
        lastHealth = s.health();
        if (s.health() <= 0) throw new IllegalStateException("Link died at frame " + frame + ": " + s);
        trace.append(frame).append('\t').append(s.map()).append('\t').append(s.room())
            .append('\t').append(s.x()).append('\t').append(s.y()).append('\t')
            .append(s.dialog()).append('\t').append(s.shield())
            .append('\t').append(s.smallKeys()).append('\t').append(s.compass()).append('\t')
            .append(s.dungeonMap()).append('\t').append(s.nightmareKey())
            .append('\t').append(s.swordAnimation())
            .append('\t').append(s.z()).append('\t').append(s.velocityZ())
            .append('\t').append(s.speedX()).append('\t').append(s.speedY()).append('\n');
    }

}
