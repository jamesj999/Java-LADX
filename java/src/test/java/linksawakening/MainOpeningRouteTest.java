package linksawakening;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real game-loop integration, with buttons only after the new-game bootstrap. */
final class MainOpeningRouteTest {
    @Test
    void newGameCanCollectTailCavesFirstSmallKeyThroughRealInputWithoutStateInjection() throws Exception {
        Main.initializeHeadlessNewGame();
        var checkpoints = new ArrayList<Main.HeadlessState>();
        for (String line : Files.readAllLines(Path.of("../tools/opening/tail-cave-dungeon-route.tsv"))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] fields = line.split("\\s+");
            int idle = Integer.parseInt(fields[2]);
            if (fields[1].contains("=")) {
                Main.headlessButton(fields[0].replace("*", ""), true);
                int ticks = 0;
                while (!OpeningRouteCapture.reached(fields[0], fields[1], Main.headlessState())) {
                    assertTrue(++ticks < 2000, "Route blocked: " + line + " " + Main.headlessState());
                    OpeningRouteCapture.pulseButtons(fields[0], ticks - 1);
                    advanceAlive();
                }
                Main.headlessButton(fields[0].replace("*", ""), false);
                do {
                    assertTrue(++ticks < 2500, "Transition stuck: " + line);
                    advanceAlive();
                } while (Main.headlessState().loading());
            }
            int held = fields[1].contains("=") ? 0 : Integer.parseInt(fields[1]);
            if (held > 0) Main.headlessButton(fields[0].replace("*", ""), true);
            for (int tick = 0; tick < held + idle; tick++) {
                if (tick == held && held > 0) Main.headlessButton(fields[0].replace("*", ""), false);
                if (tick < held) OpeningRouteCapture.pulseButtons(fields[0], tick);
                advanceAlive();
            }
            checkpoints.add(Main.headlessState());
        }
        assertTrue(checkpoints.get(0).dialog());
        assertTrue(checkpoints.get(0).text().startsWith("What a relief!"));
        assertFalse(checkpoints.get(6).dialog());
        assertEquals(70, checkpoints.get(7).x(), "Bed hop completed");
        assertEquals(105, checkpoints.get(9).x());
        assertEquals(72, checkpoints.get(9).y());
        assertTrue(checkpoints.get(10).text().contains("Tarin")
            || checkpoints.get(10).text().contains("finally snapped"));
        assertEquals(0, checkpoints.get(15).shield());
        assertEquals(1, checkpoints.get(16).shield());
        assertFalse(checkpoints.get(19).dialog());
        var exit = checkpoints.get(21);
        assertEquals(0, exit.map());
        assertEquals(0xA2, exit.room());
        assertEquals(88, exit.x());
        assertEquals(82, exit.y());
        assertEquals(1, exit.shield());
        var beach = checkpoints.get(44);
        assertEquals(0xF1, beach.room());
        assertEquals(0, beach.map());
        assertEquals(1, beach.shield());
        assertTrue(beach.health() > 0, "Route must reach the beach alive");
        assertFalse(beach.loading());
        var owlDialog = checkpoints.get(53);
        assertEquals(74, owlDialog.y(), "Landing countdown must allow five walking frames before dialogue");
        assertTrue(owlDialog.dialog());
        assertTrue(owlDialog.text().contains("Hoot!"));
        var pickup = checkpoints.get(65);
        assertTrue(pickup.dialog());
        assertTrue(pickup.text().contains("Sword!"));
        assertTrue(pickup.y() >= 80 && pickup.y() <= 82,
            "Source pickup hitbox/cadence; sprite-top-left coordinates collect a full tile too low");
        var sword = checkpoints.get(68);
        assertEquals(0xF2, sword.room());
        assertEquals(1, sword.sword());
        assertEquals(0, sword.urchins(), "The acquisition spin must hit the nearby urchin before sword ownership");
        assertEquals(1, sword.shield());
        assertFalse(sword.dialog());
        assertFalse(sword.loading());
        assertTrue(sword.health() > 0);
        var forest = checkpoints.get(99);
        assertEquals(0, forest.map());
        assertEquals(0x80, forest.room());
        assertEquals(1, forest.sword());
        assertEquals(1, forest.shield());
        assertTrue(forest.health() > 0, "Return route must reach the forest alive");
        assertTrue(forest.dialog());
        assertTrue(forest.text().startsWith("Hoot!"));
        assertFalse(forest.loading());
        var cave = checkpoints.get(110);
        assertEquals(0x0A, cave.map());
        assertEquals(0xBD, cave.room());
        assertEquals(80, cave.x());
        assertEquals(124, cave.y());
        assertEquals(1, cave.sword());
        assertEquals(1, cave.shield());
        assertTrue(cave.health() > 0);
        assertFalse(cave.dialog());
        assertFalse(cave.loading());
        assertTrue(checkpoints.get(127).dialog());
        assertFalse(checkpoints.get(127).toadstool(), "Reward follows the pickup dialogue");
        var mushroom = checkpoints.get(128);
        assertEquals(0, mushroom.map());
        assertEquals(0x50, mushroom.room());
        assertEquals(50, mushroom.x());
        assertEquals(64, mushroom.y());
        assertTrue(mushroom.toadstool());
        assertTrue(mushroom.health() > 0);
        assertFalse(mushroom.dialog());
        assertFalse(mushroom.loading());
        var witch = checkpoints.get(173);
        assertEquals(0x0E, witch.map());
        assertEquals(0xA2, witch.room());
        assertTrue(witch.toadstool());
        assertEquals(0, witch.powder());
        for (int index : new int[]{178, 179}) {
            assertTrue(checkpoints.get(index).dialog());
            assertFalse(checkpoints.get(index).toadstool());
            assertEquals(0, checkpoints.get(index).powder(), "Reward must follow the ready dialogue");
        }
        assertTrue(checkpoints.get(180).dialog());
        assertEquals(20, checkpoints.get(180).powder());
        var reward = checkpoints.get(181);
        assertFalse(reward.dialog());
        assertFalse(reward.toadstool());
        assertEquals(20, reward.powder());
        assertFalse(reward.loading());
        var tutorial = checkpoints.get(185);
        assertTrue(tutorial.dialog());
        assertTrue(tutorial.text().startsWith("Good job!"));
        assertEquals(19, tutorial.powder(), "Lighting the brazier consumes exactly one use");
        var complete = checkpoints.get(186);
        assertFalse(complete.dialog());
        assertEquals(19, complete.powder());
        assertEquals(104, complete.x());
        assertEquals(106, complete.y());
        var transformed = checkpoints.get(231);
        assertFalse(transformed.dialog());
        assertTrue(transformed.forestCleared());
        assertEquals(18, transformed.powder());
        assertEquals(0x51, transformed.room());
        assertTrue(transformed.tarinX() >= 0 && transformed.tarinX() < 160);
        assertTrue(transformed.tarinY() >= 0 && transformed.tarinY() < 144);
        assertTrue(transformed.health() > 0);
        var key = checkpoints.get(239);
        assertEquals(1, key.tailKey());
        assertTrue(key.dialog());
        assertTrue(key.text().contains("Tail Key"));
        assertTrue(checkpoints.get(241).text().startsWith("Hoot!"));
        var end = checkpoints.get(242);
        assertEquals(0x41, end.room());
        assertEquals(1, end.tailKey());
        assertFalse(end.dialog());
        var entrance = checkpoints.get(288);
        assertEquals(0, entrance.map());
        assertEquals(0x17, entrance.room());
        assertTrue(entrance.dialog());
        assertTrue(entrance.text().contains("Level 1"));
        var dungeon = checkpoints.get(290);
        assertFalse(dungeon.dialog());
        assertEquals(0x17, dungeon.room());
        assertEquals(1, dungeon.tailKey());
        assertEquals(1, dungeon.sword());
        assertTrue(dungeon.health() > 0);
        var firstSmallKey = checkpoints.getLast();
        assertEquals(0x16, firstSmallKey.room());
        assertEquals(1, firstSmallKey.smallKeys());
        assertEquals(dungeon.health(), firstSmallKey.health(), "Clear beetles without damage");
        assertFalse(Main.headlessEntityPositions().contains("20:ACTIVE"));
    }

    private static void advanceAlive() {
        Main.headlessFrame();
        assertTrue(Main.headlessState().health() > 0,
            () -> "Route must remain alive throughout: " + Main.headlessState());
    }
}
