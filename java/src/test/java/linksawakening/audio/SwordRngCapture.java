package linksawakening.audio;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import linksawakening.app.GameSession;
import linksawakening.entity.Link;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.RomRandomByteSource;

/** Bounded fixed-input counterpart of the original-ROM isolated D3 setup. */
@linksawakening.testing.RequiresRom
public final class SwordRngCapture {
    private static Object get(Object object, String name) throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var f = type.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(object);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void set(Object object, String name, Object value) throws Exception {
        var f = object.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(object, value);
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        Files.createDirectories(out);
        var rng = new RomRandomByteSource(0xA2);
        var game = new GameSession(rng);
        int[] frame = {-1};
        try (var trace = new PrintWriter(out.resolve("engine-rng.jsonl").toFile());
                var states = new PrintWriter(out.resolve("engine-state.tsv").toFile())) {
            game.initializeHeadlessNewGame();
            set(game, "newGameWakeUpMotion", null);
            set(game, "newGameTarinShieldMotion", null);
            set(game, "frameCounter", 1);
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
            rng.reset();
            rng.setObserver(draw -> {
                var caller = Arrays.stream(Thread.currentThread().getStackTrace())
                        .filter(s -> !s.getClassName().equals(Thread.class.getName())
                                && !s.getClassName().equals(RomRandomByteSource.class.getName())
                                && !s.getClassName().equals(SwordRngCapture.class.getName()))
                        .findFirst()
                        .orElseThrow();
                trace.printf(
                        "{\"frame\":%d,\"counter\":%d,\"ly\":%d,\"previousSeed\":%d,\"value\":%d,\"caller\":\"%s.%s:%d\"}%n",
                        frame[0],
                        draw.frameCounter(),
                        draw.scanline(),
                        draw.previousSeed(),
                        draw.value(),
                        caller.getClassName(),
                        caller.getMethodName(),
                        caller.getLineNumber());
            });
            var sword = d.equipmentController().activeSword();
            var runtime = get(get(get(room, "context"), "entityBridge"), "entityRuntime");
            var boss = get(runtime, "slimeEye");
            var state = boss.getClass().getDeclaredMethod("state", int.class);
            state.setAccessible(true);
            states.println(
                    "frame\tcounter\tbuttons\tdialog\tsword\tlinkX\tlinkY\tbossState\tslowCountdown\tblockBefore");
            Set<String> held = new HashSet<>();
            var input = Files.readAllLines(Path.of(args[1]));
            // Setup cutoff: observation and input only below here.
            for (int i = 1; i < input.size(); i++) {
                var parts = input.get(i).split("\t", -1);
                frame[0] = Integer.parseInt(parts[0]);
                Set<String> wanted = parts[1].isEmpty() ? Set.of() : Set.of(parts[1].split("\\+"));
                for (String key : held) if (!wanted.contains(key)) game.headlessButton(key, false);
                for (String key : wanted) if (!held.contains(key)) game.headlessButton(key, true);
                int blockBefore = link.romInteractiveMotionBlockValue();
                held = new HashSet<>(wanted);
                game.headlessFrame();
                states.printf(
                        "%d\t%d\t%s\t%b\t%d\t%d\t%d\t%d\t%d\t%d%n",
                        frame[0],
                        (int) get(game, "frameCounter"),
                        parts[1],
                        d.dialogController().isActive(),
                        sword.state(),
                        link.romEntityX(),
                        link.romEntityY(),
                        (int) state.invoke(boss, 0),
                        ((int[]) get(runtime, "slowTransitionCountdown"))[0],
                        blockBefore);
            }
        } finally {
            var cleanup = GameSession.class.getDeclaredMethod("cleanup");
            cleanup.setAccessible(true);
            cleanup.invoke(game);
        }
    }
}
