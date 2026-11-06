package linksawakening;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real game-loop integration, with buttons only after the new-game bootstrap. */
final class MainOpeningRouteTest {
    @Test
    void newGameCanWakeTalkReceiveShieldAndLeaveHouseWithoutStateInjection() throws Exception {
        Main.initializeHeadlessNewGame();
        var checkpoints = new ArrayList<Main.HeadlessState>();
        for (String line : Files.readAllLines(Path.of("../tools/opening/house-route.tsv"))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] fields = line.split("\\s+");
            int held = Integer.parseInt(fields[1]), idle = Integer.parseInt(fields[2]);
            if (held > 0) Main.headlessButton(fields[0], true);
            for (int tick = 0; tick < held + idle; tick++) {
                if (tick == held && held > 0) Main.headlessButton(fields[0], false);
                Main.headlessFrame();
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
        var exit = checkpoints.getLast();
        assertEquals(0, exit.map());
        assertEquals(0xA2, exit.room());
        assertEquals(88, exit.x());
        assertEquals(82, exit.y());
        assertEquals(1, exit.shield());
    }
}
