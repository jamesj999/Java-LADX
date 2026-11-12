package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import linksawakening.app.GameSession;
import linksawakening.entity.Link;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

/** Direct initial D3 setup, then normal buttons only for the entire encounter. */
@linksawakening.testing.RequiresRom
final class SlimeEyeControlsTest {
    private static final String[] KEYS = {"right", "left", "up", "down"};

    @Test
    void controlsOnlyDefeatsBothHalvesAndCollectsReward() throws Exception {
        var game = new GameSession();
        var out = Path.of("build/slime-eye/controls");
        Files.createDirectories(out);
        StringBuilder trace = new StringBuilder("frame\tbuttons\tlinkX\tlinkY\tlinkZ\thealth\trunning\teyes\n");
        try {
            game.initializeHeadlessNewGame();
            game.openingState().wakeUp(null);
            game.openingState().shield(null);
            var d = game.diagnostics();
            var room = d.roomSession();
            var link = d.link();
            var player = d.playerState();
            player.setSwordLevel(1);
            player.setItemA(PlayerState.INVENTORY_SWORD);
            player.setItemB(PlayerState.INVENTORY_PEGASUS_BOOTS);
            player.setMaxHearts(6);
            player.setHealth(48);
            room.loadIndoor(2, 0x5A);
            link.setRoomEntryRomPosition(72, 80);
            link.setDirection(Link.DIRECTION_UP);
            d.scrollController().startScreenShake(7, 0);
            var runtime = (RoomEntityRuntime) get(get(get(room, "context"), "entityBridge"), "entityRuntime");
            // All setup writes end here. Below this point only headlessButton/headlessFrame mutate gameplay.
            Set<String> pressed = new HashSet<>();
            boolean split = false, firstDeath = false, dead = false, reward = false, body = false;
            int splitFrame = -1, bodyHits = 0;
            int previousBodyStretch = 0;
            for (int frame = 0; frame < 9000; frame++) {
                var eyes = room.activeRoom().entities().loadedEntities().stream()
                        .filter(e -> e.type() == 0x5B)
                        .toList();
                var active = eyes.stream()
                        .filter(e -> e.status() == EntityStatus.ACTIVE)
                        .toList();
                Set<String> desired = new HashSet<>();
                if (d.dialogController().isActive()) {
                    if (frame % 24 < 8) desired.add("a");
                } else if (!split && eyes.size() == 1) {
                    var eye = eyes.getFirst();
                    int stretch = runtime.slimeEye.stretch(eye.slot());
                    if (stretch > previousBodyStretch) bodyHits++;
                    previousBodyStretch = stretch;
                    if (runtime.slimeEye.state(eye.slot()) >= 2) {
                        approach(desired, link, eye.x(), eye.y(), frame, true);
                        if (stretch == 4) {
                            desired.add("a");
                            desired.add("b");
                        } else if (frame % 24 < 8) desired.add("a");
                    }
                } else if (!active.isEmpty()) {
                    if (!split) {
                        split = true;
                        splitFrame = frame;
                    }
                    if (player.runningWithPegasusBoots()) {
                        // Keep the blade out: the first half contact must end the run
                        // through the ROM Slime Eye sword response, not a test reset.
                        desired.add("a");
                    } else {
                        RoomEntity target = active.stream()
                                .filter(e -> e.z() < 0x70)
                                .min(java.util.Comparator.comparingInt(e -> distance(link, e.x(), e.y())))
                                .orElse(active.getFirst());
                        if (target.z() < 32) approach(desired, link, target.x(), target.y(), frame, false);
                        else move(desired, link, 80, 100, -1);
                        if (frame % 24 < 8) desired.add("a");
                    }
                } else if (eyes.isEmpty()) {
                    dead = true;
                    var heart = room.activeRoom().entities().loadedEntities().stream()
                            .filter(e -> e.type() == 0x36)
                            .findFirst()
                            .orElse(null);
                    if (heart != null) move(desired, link, heart.x(), heart.y(), -1);
                    else if (player.maxHearts() >= 7) {
                        reward = true;
                    }
                }
                for (String key : pressed) if (!desired.contains(key)) game.headlessButton(key, false);
                for (String key : desired) if (!pressed.contains(key)) game.headlessButton(key, true);
                pressed = desired;
                byte[] rgba = game.headlessFrame();
                var after = room.activeRoom().entities().loadedEntities().stream()
                        .filter(e -> e.type() == 0x5B)
                        .toList();
                trace.append(frame)
                        .append('\t')
                        .append(String.join("+", desired.stream().sorted().toList()))
                        .append('\t')
                        .append(link.romEntityX())
                        .append('\t')
                        .append(link.romEntityY())
                        .append('\t')
                        .append(link.romEntityZ())
                        .append('\t')
                        .append(player.health())
                        .append('\t')
                        .append(player.runningWithPegasusBoots())
                        .append('\t')
                        .append(after.stream()
                                .map(e -> "%d,%d,%d,%d,%d,%d,%d,%d"
                                        .formatted(
                                                e.slot(),
                                                e.status().value(),
                                                runtime.slimeEye.state(e.slot()),
                                                e.x(),
                                                e.y(),
                                                e.z(),
                                                room.entityHealth(e.slot()),
                                                runtime.entityState.physicsTables.readEnemyTransitionCountdown(
                                                        e.slot())))
                                .collect(java.util.stream.Collectors.joining(";")))
                        .append('\n');
                if (!body
                        && after.size() == 1
                        && runtime.slimeEye.state(after.getFirst().slot()) == 3) {
                    save(rgba, out.resolve("body.png"));
                    body = true;
                }
                if (split && frame == splitFrame) save(rgba, out.resolve("split.png"));
                if (split && !firstDeath && after.stream().anyMatch(e -> e.status() == EntityStatus.DYING)) {
                    save(rgba, out.resolve("first-death.png"));
                    firstDeath = true;
                }
                if (dead && !reward && frame % 8 == 0) save(rgba, out.resolve("defeated.png"));
                if (reward) {
                    save(rgba, out.resolve("reward.png"));
                    break;
                }
                if (player.health() == 0) break;
            }
            Files.writeString(out.resolve("engine.tsv"), trace);
            assertTrue(split, "Body must split via normal controls; hits=" + bodyHits);
            assertTrue(dead, "Both halves must die via normal controls");
            assertTrue(reward, "Reward must be collected via normal controls");
        } finally {
            Files.writeString(out.resolve("engine.tsv"), trace);
            game.close();
        }
    }

    private int distance(Link link, int x, int y) {
        return Math.abs(x - link.romEntityX()) + Math.abs(y - link.romEntityY());
    }

    private void approach(Set<String> desired, Link link, int x, int y, int frame, boolean body) {
        // Approach a reachable cardinal sword pose; preserve the chosen side while close.
        int[][] poses = {{x - 16, y, 0}, {x + 16, y, 1}, {x, y + 16, 2}, {x, y - 16, 3}};
        int[] best = null;
        int score = Integer.MAX_VALUE;
        for (int[] pose : poses) {
            if (pose[0] < 24 || pose[0] > 136 || pose[1] < 32 || pose[1] > 112) continue;
            int cost = distance(link, pose[0], pose[1]);
            if (pose[2] != romDirection(link.direction())) cost += 4;
            if (cost < score) {
                score = cost;
                best = pose;
            }
        }
        if (best == null) best = new int[] {80, 100, -1};
        move(desired, link, best[0], best[1], best[2]);
    }

    private void move(Set<String> desired, Link link, int x, int y, int facing) {
        int dx = x - link.romEntityX(), dy = y - link.romEntityY();
        if (Math.abs(dx) > 2 || Math.abs(dy) > 2)
            desired.add(KEYS[Math.abs(dx) >= Math.abs(dy) ? (dx >= 0 ? 0 : 1) : (dy >= 0 ? 3 : 2)]);
        else if (facing >= 0 && romDirection(link.direction()) != facing) desired.add(KEYS[facing]);
    }

    private int romDirection(int direction) {
        return switch (direction) {
            case Link.DIRECTION_RIGHT -> 0;
            case Link.DIRECTION_LEFT -> 1;
            case Link.DIRECTION_UP -> 2;
            default -> 3;
        };
    }

    private Object get(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private void set(Object object, String name, Object value) throws Exception {
        var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private void save(byte[] pixels, Path file) throws Exception {
        var image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 144; y++)
            for (int x = 0; x < 160; x++) {
                int i = (y * 160 + x) * 4;
                image.setRGB(
                        x,
                        y,
                        0xff000000 | ((pixels[i] & 255) << 16) | ((pixels[i + 1] & 255) << 8) | (pixels[i + 2] & 255));
            }
        ImageIO.write(image, "png", file.toFile());
    }
}
