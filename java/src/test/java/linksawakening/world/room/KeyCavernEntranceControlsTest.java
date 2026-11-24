package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import linksawakening.app.GameSession;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;

/** Controlled D3 entrance setup followed by the shared Key Cavern buttons. */
@RequiresRom
final class KeyCavernEntranceControlsTest {
    private static final Pattern ROUTE_FIELDS = Pattern.compile("\\s+");

    @Test
    void normalButtonsReachTheBasementAndCollectTwoKeys() throws Exception {
        try (var game = new GameSession((pcm, rate) -> {})) {
            game.initializeHeadlessNewGame();
            game.openingState().wakeUp(null);
            game.openingState().shield(null);
            var diagnostics = game.diagnostics();
            var player = diagnostics.playerState();
            player.equipmentLevels().setSwordLevel(1);
            player.equipmentLevels().setPowerBraceletLevel(1);
            player.inventory().setItemA(InventoryItem.SWORD.romValue());
            player.inventory().setItemB(InventoryItem.POWER_BRACELET.romValue());

            var session = diagnostics.roomSession();
            session.rooms().loadIndoor(0x02, 0x52);
            diagnostics.link().position().setRoomEntryRomPosition(72, 108);
            diagnostics.link().animation().setDirection(Direction.UP.javaIndex());

            List<String> checkpoints = new ArrayList<>();
            replay(game, session, "entrance.tsv", checkpoints);
            replay(game, session, "room-54-java.tsv", checkpoints);
            assertEquals(
                    List.of(
                            "pot-held",
                            "shutter-open",
                            "entrance-crossed",
                            "chest-revealed",
                            "first-key",
                            "four-zols",
                            "zol-shutter-open",
                            "north-room",
                            "stairs-room",
                            "basement-warp",
                            "key-door-crossed",
                            "stalfos-defeated",
                            "second-key-dropped",
                            "second-key",
                            "returned-to-basement",
                            "second-door-crossed",
                            "upper-stairs"),
                    checkpoints);
            assertTrue(player.vitals().health() > 0);
        }
    }

    // Route fields are variable-width ASCII whitespace with no empty tokens.
    @SuppressWarnings("StringSplitter")
    private static void replay(GameSession game, RoomSession session, String filename, List<String> checkpoints)
            throws Exception {
        for (String row : Files.readAllLines(Path.of("../tools/key-cavern", filename))) {
            if (row.isBlank() || row.startsWith("#")) continue;
            String[] fields = ROUTE_FIELDS.split(row);
            String buttons = fields[0];
            int frames = Integer.parseInt(fields[1]);
            if (!buttons.equals("none")) game.headlessButton(buttons, true);
            for (int frame = 0; frame < frames; frame++) game.headlessFrame();
            if (!buttons.equals("none")) game.headlessButton(buttons, false);
            if (fields.length == 3) {
                verifyCheckpoint(session, fields[2]);
                checkpoints.add(fields[2]);
            }
        }
    }

    private static void verifyCheckpoint(RoomSession session, String checkpoint) {
        var rooms = session.rooms();
        var room = rooms.requireActiveRoom();
        int keys = session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX];
        switch (checkpoint) {
            case "pot-held" -> {
                assertEquals(0x52, room.roomId());
                assertEquals(1, rooms.liftedEntityState().carryState());
                assertEquals(0x05, rooms.liftedEntityState().type());
            }
            case "shutter-open" -> {
                assertEquals(0x52, room.roomId());
                assertEquals(0, rooms.liftedEntityState().carryState());
                assertEquals(0, room.shutterDoorMask() & 1);
            }
            case "entrance-crossed" -> assertEquals(0x51, room.roomId());
            case "chest-revealed" -> {
                assertEquals(0x51, room.roomId());
                assertEquals(0xA0, RoomNavigationFixture.objectAt(session, 0x28));
                assertEquals(0, keys);
            }
            case "first-key" -> {
                assertEquals(0x51, room.roomId());
                assertEquals(0xA1, RoomNavigationFixture.objectAt(session, 0x28));
                assertEquals(1, keys);
            }
            case "four-zols" -> {
                assertEquals(0x4F, room.roomId());
                assertEquals(
                        4,
                        room.entities().loadedEntities().stream()
                                .filter(entity -> entity.type() == 0x9B)
                                .count());
                assertEquals(1, keys);
            }
            case "zol-shutter-open" -> {
                assertEquals(0x4F, room.roomId());
                assertEquals(0, room.shutterDoorMask() & 1);
                assertEquals(1, keys);
            }
            case "north-room" -> assertEquals(0x4E, room.roomId());
            case "stairs-room" -> assertEquals(0x4C, room.roomId());
            case "basement-warp" -> {
                assertEquals(0x56, room.roomId());
                assertEquals(1, keys);
            }
            case "key-door-crossed" -> {
                assertEquals(0x54, room.roomId());
                assertEquals(0, keys);
                assertEquals(0x04, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x02, 0x56) & 0x04);
                assertEquals(0x08, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x02, 0x54) & 0x08);
            }
            case "stalfos-defeated" -> {
                assertEquals(0x54, room.roomId());
                assertTrue(room.entities().loadedEntities().stream().noneMatch(entity -> entity.type() == 0x1A));
            }
            case "second-key-dropped" -> {
                assertEquals(0x54, room.roomId());
                assertEquals(0, keys);
                assertTrue(room.entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x30 && entity.x() == 40 && entity.y() == 60));
            }
            case "second-key" -> {
                assertEquals(0x54, room.roomId());
                assertEquals(1, keys);
            }
            case "returned-to-basement" -> {
                assertEquals(0x56, room.roomId());
                assertEquals(1, keys);
            }
            case "second-door-crossed" -> {
                assertEquals(0x57, room.roomId());
                assertEquals(0, keys);
                assertEquals(0x01, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x02, 0x56) & 0x01);
                assertEquals(0x02, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x02, 0x57) & 0x02);
            }
            case "upper-stairs" -> {
                assertEquals(0x4D, room.roomId());
                assertEquals(0, keys);
            }
            default -> throw new AssertionError(checkpoint);
        }
    }
}
