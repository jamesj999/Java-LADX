package linksawakening;

import linksawakening.world.transition.Warp;

import linksawakening.app.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;

import linksawakening.app.screen.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real game-loop integration, with buttons only after the new-game bootstrap. */
@linksawakening.testing.RequiresRom
final class MainOpeningRouteTest {
    private static final HeadlessGameDriver game = new HeadlessGameDriver();
    @Test
    void newGameCanDefeatRollingBonesAndWarpBackToTheEntrance() throws Exception {
        game.initializeHeadlessNewGame();
        var checkpoints = new ArrayList<HeadlessGameDriver.State>();
        var entityCheckpoints = new ArrayList<String>();
        for (String line : Files.readAllLines(Path.of("../tools/opening/tail-cave-warp-route.tsv"))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] fields = line.split("\\s+");
            int idle = Integer.parseInt(fields[2]);
            if (fields[1].contains("=")) {
                game.headlessButton(fields[0].replace("*", ""), true);
                int ticks = 0;
                while (!OpeningRouteCapture.reached(game, fields[0], fields[1], game.headlessState())) {
                    assertTrue(++ticks < 2000, "Route blocked: " + line + " " + game.headlessState());
                    OpeningRouteCapture.pulseButtons(game, fields[0], ticks - 1);
                    advanceAlive();
                }
                game.headlessButton(fields[0].replace("*", ""), false);
                do {
                    assertTrue(++ticks < 2500, "Transition stuck: " + line);
                    advanceAlive();
                } while (game.headlessState().loading());
            }
            int held = fields[1].contains("=") ? 0 : Integer.parseInt(fields[1]);
            if (held > 0) game.headlessButton(fields[0].replace("*", ""), true);
            for (int tick = 0; tick < held + idle; tick++) {
                if (tick == held && held > 0) game.headlessButton(fields[0].replace("*", ""), false);
                if (tick < held) OpeningRouteCapture.pulseButtons(game, fields[0], tick);
                advanceAlive();
            }
            if (held > 0 && idle == 0) game.headlessButton(fields[0].replace("*", ""), false);
            checkpoints.add(game.headlessState());
            entityCheckpoints.add(game.headlessEntityPositions());
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
        assertEquals(24, checkpoints.get(99).health(),
            "The fixed well hearts refill health before the forest");
        var forest = checkpoints.get(102);
        assertEquals(0, forest.map());
        assertEquals(0x80, forest.room());
        assertEquals(1, forest.sword());
        assertEquals(1, forest.shield());
        assertTrue(forest.health() > 0, "Return route must reach the forest alive");
        assertTrue(forest.dialog());
        assertTrue(forest.text().startsWith("Hoot!"));
        assertFalse(forest.loading());
        var cave = checkpoints.get(115);
        assertEquals(0x0A, cave.map());
        assertEquals(0xBD, cave.room());
        assertEquals(80, cave.x());
        assertEquals(124, cave.y());
        assertEquals(1, cave.sword());
        assertEquals(1, cave.shield());
        assertTrue(cave.health() > 0);
        assertFalse(cave.dialog());
        assertFalse(cave.loading());
        assertTrue(checkpoints.get(132).dialog());
        assertFalse(checkpoints.get(132).toadstool(), "Reward follows the pickup dialogue");
        var mushroom = checkpoints.get(133);
        assertEquals(0, mushroom.map());
        assertEquals(0x50, mushroom.room());
        assertEquals(49, mushroom.x(), "Re-derived pickup approach includes the sword launch frame");
        assertEquals(64, mushroom.y());
        assertTrue(mushroom.toadstool());
        assertTrue(mushroom.health() > 0);
        assertFalse(mushroom.dialog());
        assertFalse(mushroom.loading());
        var witch = checkpoints.get(181);
        assertEquals(0x0E, witch.map());
        assertEquals(0xA2, witch.room());
        assertTrue(witch.toadstool());
        assertEquals(0, witch.powder());
        for (int index : new int[]{186, 187}) {
            assertTrue(checkpoints.get(index).dialog());
            assertFalse(checkpoints.get(index).toadstool());
            assertEquals(0, checkpoints.get(index).powder(), "Reward must follow the ready dialogue");
        }
        assertTrue(checkpoints.get(188).dialog());
        assertEquals(20, checkpoints.get(188).powder());
        var reward = checkpoints.get(189);
        assertFalse(reward.dialog());
        assertFalse(reward.toadstool());
        assertEquals(20, reward.powder());
        assertFalse(reward.loading());
        var tutorial = checkpoints.get(193);
        assertTrue(tutorial.dialog());
        assertTrue(tutorial.text().startsWith("Good job!"));
        assertEquals(19, tutorial.powder(), "Lighting the brazier consumes exactly one use");
        var complete = checkpoints.get(194);
        assertFalse(complete.dialog());
        assertEquals(19, complete.powder());
        assertEquals(104, complete.x());
        assertEquals(106, complete.y());
        var transformed = checkpoints.get(240);
        assertFalse(transformed.dialog());
        assertTrue(transformed.forestCleared());
        assertEquals(18, transformed.powder());
        assertEquals(0x51, transformed.room());
        assertTrue(transformed.tarinX() >= 0 && transformed.tarinX() < 160);
        assertTrue(transformed.tarinY() >= 0 && transformed.tarinY() < 144);
        assertTrue(transformed.health() > 0);
        var key = checkpoints.get(248);
        assertEquals(1, key.tailKey());
        assertTrue(key.dialog());
        assertTrue(key.text().contains("Tail Key"));
        assertTrue(checkpoints.get(250).text().startsWith("Hoot!"));
        var end = checkpoints.get(251);
        assertEquals(0x41, end.room());
        assertEquals(1, end.tailKey());
        assertFalse(end.dialog());
        assertEquals(24, checkpoints.get(271).health(),
            "Refill from fixed well hearts before entering Tail Cave");
        var entrance = checkpoints.get(300);
        assertEquals(0, entrance.map());
        assertEquals(0x17, entrance.room());
        assertTrue(entrance.dialog());
        assertTrue(entrance.text().contains("Level 1"));
        var dungeon = checkpoints.get(302);
        assertFalse(dungeon.dialog());
        assertEquals(0x17, dungeon.room());
        assertEquals(1, dungeon.tailKey());
        assertEquals(1, dungeon.sword());
        assertTrue(dungeon.health() > 0);
        var firstSmallKey = checkpoints.get(312);
        assertEquals(0x16, firstSmallKey.room());
        assertEquals(1, firstSmallKey.smallKeys());
        assertEquals(dungeon.health(), firstSmallKey.health(), "Clear beetles without damage");
        var compassDialogue = checkpoints.get(323);
        assertTrue(compassDialogue.dialog());
        assertTrue(compassDialogue.text().contains("Compass"));
        assertEquals(1, compassDialogue.compass());
        var afterCompass = checkpoints.get(337);
        assertEquals(0x13, afterCompass.room(), "Exit the compass room and return through the entrance");
        assertEquals(1, afterCompass.compass());
        assertEquals(1, afterCompass.smallKeys());
        assertFalse(afterCompass.dialog());
        assertTrue(afterCompass.health() > 0);
        // Warp respawns change the kill/drop sequence, so this waypoint no
        // longer guarantees a Piece of Power. It must remain interactive.
        assertFalse(checkpoints.get(342).dialog());
        var switchKey = checkpoints.get(356);
        assertEquals(627, checkpoints.size());
        assertEquals(0x13, switchKey.room());
        assertEquals(2, switchKey.smallKeys());
        assertEquals(1, switchKey.compass());
        assertTrue(switchKey.dialog());
        assertTrue(switchKey.text().contains("Small"));
        assertTrue(switchKey.health() > 0);
        var mapReward = checkpoints.get(413);
        assertEquals(0x14, mapReward.room());
        assertTrue(mapReward.dialog());
        assertTrue(mapReward.text().contains("a Map!"));
        assertEquals(1, mapReward.dungeonMap());
        assertEquals(1, mapReward.compass());
        assertEquals(2, mapReward.smallKeys());
        assertTrue(mapReward.health() > 0);
        var afterDoor = checkpoints.get(428);
        assertEquals(0x0F, afterDoor.room());
        assertEquals(80, afterDoor.y());
        assertEquals(2, afterDoor.smallKeys());
        assertEquals(1, afterDoor.dungeonMap());
        assertFalse(afterDoor.dialog());
        assertTrue(afterDoor.health() > 0);
        var thirdKey = checkpoints.get(441);
        assertEquals(0x0E, thirdKey.room());
        assertEquals(3, thirdKey.smallKeys());
        assertEquals(1, thirdKey.dungeonMap());
        assertEquals(1, thirdKey.compass());
        assertTrue(thirdKey.dialog());
        assertTrue(thirdKey.text().contains("Small"));
        assertTrue(thirdKey.health() > 0);
        var pushedBlock = checkpoints.get(469);
        assertEquals(0x04, pushedBlock.room());
        assertTrue(pushedBlock.x() <= 44, "Link must stay behind the moving block");
        var beetleRoom = checkpoints.get(470);
        assertEquals(0x03, beetleRoom.room());
        assertEquals(2, beetleRoom.smallKeys());
        assertEquals(1, beetleRoom.dungeonMap());
        assertEquals(1, beetleRoom.compass());
        assertTrue(beetleRoom.health() > 0);
        var clearedBeetles = checkpoints.get(485);
        assertEquals(0x03, clearedBeetles.room());
        assertTrue(clearedBeetles.health() > 0);
        assertFalse(entityCheckpoints.get(485).contains("2C:"),
            "Both Spiked Beetles must be defeated before entering the passage");
        var passage = checkpoints.get(487);
        assertEquals(0x19, passage.room());
        assertEquals(0, passage.map());
        assertEquals(2, passage.smallKeys());
        assertTrue(passage.health() > 0);
        assertFalse(passage.loading());

        assertEquals(0x18, checkpoints.get(491).room());
        assertEquals(112, checkpoints.get(492).y(),
            "Crossing the side-view platform gap must fall to the lower floor");
        assertEquals(0x01, checkpoints.get(494).room());
        var feather = checkpoints.get(502);
        assertEquals(0x1D, feather.room());
        assertTrue(feather.dialog());
        assertTrue(feather.text().contains("Roc's Feather"));
        assertTrue(game.headlessHasInventoryItem(
            linksawakening.state.PlayerState.INVENTORY_ROCS_FEATHER));
        assertEquals(0x18, checkpoints.get(520).room());
        assertEquals(47, checkpoints.get(520).y());
        assertEquals(110, checkpoints.get(521).x());
        assertEquals(64, checkpoints.get(521).y());
        assertEquals(2, entityCheckpoints.get(522).split("9F:", -1).length - 1,
            "Goombas respawn after the Feather staircase trip");
        assertEquals(2, entityCheckpoints.get(527).split("2C:", -1).length - 1,
            "Spiked Beetles respawn after returning upstairs");
        assertEquals(0x03, checkpoints.get(527).room());
        assertEquals(136, checkpoints.get(527).x());
        assertEquals(32, checkpoints.get(527).y());
        assertFalse(checkpoints.get(527).dialog());
        assertTrue(checkpoints.get(527).health() > 0);
        assertEquals(0x09, checkpoints.get(563).room());
        assertEquals(1, checkpoints.get(563).smallKeys(), "The keyhole block spends one key");
        assertEquals(0x08, checkpoints.get(565).room());
        var nightmareReward = checkpoints.get(569);
        assertTrue(nightmareReward.dialog());
        assertTrue(nightmareReward.text().contains("Nightmare"));
        assertEquals(1, nightmareReward.nightmareKey());
        assertEquals(0x08, checkpoints.get(573).room());
        assertEquals(1, checkpoints.get(573).nightmareKey());
        assertEquals(1, checkpoints.get(573).smallKeys());
        assertFalse(checkpoints.get(573).dialog());
        assertTrue(checkpoints.get(573).health() > 0);
        var misalignedDoor = checkpoints.get(591);
        assertEquals(0x0F, misalignedDoor.room());
        assertEquals(1, misalignedDoor.smallKeys(), "Off-center contact must not spend a key");
        assertEquals(0x10, checkpoints.get(593).room());
        assertEquals(0, checkpoints.get(593).smallKeys());
        var entry = checkpoints.get(601);
        assertEquals(0x11, entry.room());
        assertEquals(17, entry.x(), "The boss handler pushes Link clear of the doorway");
        assertEquals(72, entry.y());
        assertEquals(1, entry.nightmareKey());
        assertTrue(entityCheckpoints.get(601).contains("81:ACTIVE"));
        assertTrue(entityCheckpoints.get(601).contains("82:ACTIVE"));
        var approach = checkpoints.get(602);
        assertEquals(41, approach.x(), "Sword swings must not halt Feather approaches");
        assertEquals(72, approach.y());
        assertEquals(entry.health(), approach.health());
        assertEquals(627, checkpoints.size());
        assertEquals(0x11, checkpoints.get(615).room());
        assertEquals(0x17, checkpoints.getLast().room(), "The miniboss warp returns to the entrance");
        assertEquals(24, checkpoints.getLast().health(), "Collecting the miniboss fairy heals Link");
        assertEquals(80, checkpoints.getLast().x());
        assertEquals(72, checkpoints.getLast().y());
        assertTrue(checkpoints.getLast().health() > 0);
        assertFalse(entityCheckpoints.getLast().contains("81:"), "Rolling Bones must finish dying");
        assertFalse(entityCheckpoints.getLast().contains("82:"), "The defeated boss removes its bar");
        assertTrue(entityCheckpoints.getLast().contains("61:ACTIVE"), "The miniboss warp remains");

    }

    private static void advanceAlive() {
        game.headlessFrame();
        assertTrue(game.headlessState().health() > 0,
            () -> "Route must remain alive throughout: " + game.headlessState());
    }
}
