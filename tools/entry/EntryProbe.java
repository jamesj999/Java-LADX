package linksawakening.render;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import linksawakening.app.GameSession;
import linksawakening.entity.LinkPositionRole;

/** Controlled scroll after the normal house route. Run from the repo root with the test runtime classpath. */
public final class EntryProbe {
    private static final Path OUTPUT = Path.of("/tmp/ladx-entry-controlled");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT);
        for (String button : List.of("right", "left", "up", "down")) {
            try (var game = new GameSession()) {
                game.initializeHeadlessNewGame();
                for (String row : Files.readAllLines(Path.of("tools/opening/house-route.tsv"))) {
                    if (row.isBlank() || row.startsWith("#")) continue;
                    String[] fields = row.split("\\s+");
                    int held = Integer.parseInt(fields[1]);
                    if (held > 0) game.headlessButton(fields[0], true);
                    for (int i = 0; i < held; i++) game.headlessFrame();
                    if (held > 0) game.headlessButton(fields[0], false);
                    for (int i = 0; i < Integer.parseInt(fields[2]); i++) game.headlessFrame();
                }
                capture(game, button, args);
            }
        }
    }

    private static void capture(GameSession game, String button, String[] args) throws Exception {
        var diagnostics = game.diagnostics();
        var scroll = diagnostics.scrollController();
        var rooms = diagnostics.roomSession().rooms();
        var link = diagnostics.link();
        var position = link.position();
        int x = button.equals("right") ? 148 : button.equals("left") ? -5 : 72;
        int y = button.equals("up") ? -5 : button.equals("down") ? 116 : 80;
        position.setPixelPosition(x, y);
        position.applyRomFinalPosition(10, 5);
        position.captureRomFinalPosition();
        game.headlessButton(button, true);
        game.services().world().roomTransitionCoordinator().handleOverworldBoundary(link);
        System.out.printf("%s entry (%d,%d) room %02X%n", button,
                position.romEntityX(), position.romEntityY(), rooms.currentRoomId());
        if (args.length > 1) {
            int steps = button.equals("left") || button.equals("right") ? 40 : 32;
            game.state().clock().seed((Integer.parseInt(args[1]) - steps) & 255);
        }
        if (!scroll.isActive()) throw new IllegalStateException("No scroll for " + button);
        byte[] rgba;
        int count = 0;
        do {
            if (++count > 100) throw new IllegalStateException("Scroll timed out for " + button);
            rgba = game.headlessFrame();
        } while (scroll.isActive());
        System.out.printf("%s complete (%d,%d) fractions (%d,%d) speed (%d,%d) frames %d clock %d%n",
                button, position.romEntityX(), position.romEntityY(), fraction(position, true), fraction(position, false),
                position.romSpeedX(), position.romSpeedY(), count, game.state().clock().value());
        String label = args.length == 0 ? "current" : args[0];
        png(rgba, "java-" + label + "-" + button + "-complete.png");
        if (!button.equals("right") || args.length < 2) return;

        boolean visual = args.length > 2;
        if (visual) {
            byte[] solo = new byte[rgba.length], background = new byte[rgba.length];
            FrameScene.of(List.of(new LinkRenderLayer(link, scroll))).drawTo(solo, diagnostics.gpu());
            FrameScene.of(List.of(new RoomRenderLayer(rooms.renderViewOrNull(), scroll, diagnostics.transitionController())))
                    .drawTo(background, diagnostics.gpu());
            png(solo, "link-only-right.png");
            png(background, "background-only-right.png");
        }
        for (int post = 1; post <= 4; post++) {
            byte[] next = game.headlessFrame();
            if (visual) png(next, "java-right-post" + post + ".png");
            System.out.printf("right post %d clock %d position (%d,%d) pose %02X%n",
                    post, game.state().clock().value(), position.romEntityX(), position.romEntityY(),
                    link.animation().romAnimationState());
        }
    }

    // Observe when the next integer pixel is reached, then undo the fractional displacement.
    private static int fraction(LinkPositionRole position, boolean horizontal) {
        int start = horizontal ? position.pixelX() : position.pixelY();
        int steps = 0;
        do {
            position.applyRomFinalPosition(horizontal ? 1 : 0, horizontal ? 0 : 1);
            steps++;
        } while ((horizontal ? position.pixelX() : position.pixelY()) == start);
        position.applyRomFinalPosition(horizontal ? (-steps & 255) : 0, horizontal ? 0 : (-steps & 255));
        return 16 - steps;
    }

    private static void png(byte[] rgba, String filename) throws Exception {
        var image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_RGB);
        for (int pixel = 0; pixel < 160 * 144; pixel++) {
            int i = pixel * 4;
            image.setRGB(pixel % 160, pixel / 160,
                    ((rgba[i] & 255) << 16) | ((rgba[i + 1] & 255) << 8) | (rgba[i + 2] & 255));
        }
        ImageIO.write(image, "png", OUTPUT.resolve(filename).toFile());
    }
}
