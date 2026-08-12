package linksawakening.world;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DungeonRoomEventTableTest {

    @Test
    void readsTheTailCaveOpeningEventsFromTheRomTable() throws IOException {
        DungeonRoomEventTable table = new DungeonRoomEventTable(loadRom());

        assertEquals(0x63, table.eventFor(0x00, 0x13));
        assertEquals(0x21, table.eventFor(0x00, 0x12));
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = DungeonRoomEventTableTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
