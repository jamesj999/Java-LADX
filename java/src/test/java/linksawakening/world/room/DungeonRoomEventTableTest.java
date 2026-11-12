package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class DungeonRoomEventTableTest {

    @Test
    void readsTheTailCaveOpeningEventsFromTheRomTable() throws IOException {
        DungeonRoomEventTable table = new DungeonRoomEventTable(loadRom());

        assertEquals(0x63, table.eventFor(0x00, 0x13));
        assertEquals(0x21, table.eventFor(0x00, 0x12));
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IOException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
