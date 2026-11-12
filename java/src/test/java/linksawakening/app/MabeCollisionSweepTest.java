package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.input.*;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.*;
import linksawakening.state.PlayerState;
import linksawakening.testing.*;
import linksawakening.world.room.*;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@RequiresRom
final class MabeCollisionSweepTest {
    private static final int[] ROOMS = {0xB1, 0xA1, 0xB0, 0xB2, 0xC1};
    // LinkCollisionPointsX/Y ($02:6E15/6E1D), in Java down/up/left/right order.
    private static final int[][] XS = {{6, 9}, {6, 9}, {4, 4}, {11, 11}};
    private static final int[][] YS = {{15, 15}, {6, 6}, {9, 12}, {9, 12}};
    private static final int[] DX = {0, 0, -1, 1}, DY = {1, -1, 0, 0};
    private static final String[] BUTTON = {"down", "up", "left", "right"};

    @Test
    void tileProbeSweepMatchesRomDispatchInBlueHouseAndNeighbours() throws Exception {
        byte[] bytes;
        try (var stream = TestRom.open()) {
            bytes = stream.readAllBytes();
        }
        Rom rom = new Rom(bytes);
        var tables = RomTables.loadFromRom(bytes);
        int cases = 0;
        List<String> mismatches = new ArrayList<>();
        for (int roomId : ROOMS) {
            var room = new RoomLoader(bytes).loadOverworld(roomId);
            var collision = new OverworldCollision(tables);
            collision.setRoom(room.roomObjectsArea());
            for (int y = 0; y <= 112; y += 8)
                for (int x = 0; x <= 144; x += 8) {
                    if (!walkable(rom, room, x, y)) continue;
                    for (int dir = 0; dir < 4; dir++) {
                        if (!passableProbes(rom, room, x + DX[dir], y + DY[dir], dir)) continue;
                        var input = new InputState();
                        var config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
                        var link =
                                new Link(input, config, tables, collision, null, new PlayerState(), new ItemRegistry());
                        link.setRoomEntryPixelPosition(x, y);
                        int key =
                                switch (dir) {
                                    case 0 -> config.downKey();
                                    case 1 -> config.upKey();
                                    case 2 -> config.leftKey();
                                    default -> config.rightKey();
                                };
                        input.onKeyEvent(key, 1);
                        link.update();
                        cases++;
                        if (link.romCollisionType() != 0)
                            mismatches.add(String.format(
                                    "%02X (%d,%d) %s collision=%02X",
                                    roomId, x, y, BUTTON[dir], link.romCollisionType()));
                    }
                }
        }
        System.out.println("ROM-passable tile movement cases=" + cases + "; mismatches=" + mismatches);
        assertTrue(mismatches.isEmpty(), mismatches.toString());
    }

    @Test
    void fullSessionSweepHasNoInvisibleSolidEntityOnWalkableTiles() throws Exception {
        byte[] bytes;
        try (var stream = TestRom.open()) {
            bytes = stream.readAllBytes();
        }
        var rom = new Rom(bytes);
        var game = new GameSession((pcm, rate) -> {});
        game.initializeHeadlessNewGame();
        List<String> invisible = new ArrayList<>();
        int cases = 0, entityBlocks = 0;
        try {
            var teleport = GameSession.class.getDeclaredMethod("debugTeleport", Warp.class);
            teleport.setAccessible(true);
            Link link = field(game, "link", Link.class);
            RoomSession session = field(game, "roomSession", RoomSession.class);
            for (int roomId : ROOMS) {
                teleport.invoke(game, new Warp(0, 0, roomId, 80, 96, -1));
                var loaded = new RoomLoader(bytes).loadOverworld(roomId);
                for (int y = 8; y <= 104; y += 8)
                    for (int x = 8; x <= 136; x += 8) {
                        if (!walkable(rom, loaded, x, y)) continue;
                        for (int dir = 0; dir < 4; dir++) {
                            if (!passableProbes(rom, loaded, x + DX[dir], y + DY[dir], dir)) continue;
                            if (session.currentRoomId() != roomId || session.mapCategory() != 0)
                                teleport.invoke(game, new Warp(0, 0, roomId, 80, 96, -1));
                            link.resetTransientStateForDebug();
                            link.setRoomEntryPixelPosition(x, y);
                            game.headlessButton(BUTTON[dir], true);
                            game.headlessFrame();
                            game.headlessButton(BUTTON[dir], false);
                            cases++;
                            if (link.pixelX() == x && link.pixelY() == y && link.romCollisionType() == 0) {
                                entityBlocks++;
                                for (var entity :
                                        session.activeRoom().entities().loadedEntities()) {
                                    if (Math.abs(entity.x() - link.romEntityX()) < 20
                                            && Math.abs(entity.y() - link.romEntityY()) < 20
                                            && !entity.spriteDefinition().supported()) {
                                        invisible.add(String.format(
                                                "room=%02X (%d,%d) %s slot=%d type=%02X entity=(%d,%d)",
                                                roomId,
                                                x,
                                                y,
                                                BUTTON[dir],
                                                entity.slot(),
                                                entity.type(),
                                                entity.x(),
                                                entity.y()));
                                    }
                                }
                            }
                        }
                    }
            }
            System.out.println("Full-session ROM-passable cases=" + cases + "; stationary samples=" + entityBlocks
                    + "; invisible blockers=" + invisible);
            assertTrue(invisible.isEmpty(), invisible.toString());
        } finally {
            var cleanup = GameSession.class.getDeclaredMethod("cleanup");
            cleanup.setAccessible(true);
            cleanup.invoke(game);
        }
    }

    private static <T> T field(GameSession game, String name, Class<T> type) throws Exception {
        var field = SessionTestAccess.field(game, name);
        field.setAccessible(true);
        return type.cast(field.get(SessionTestAccess.owner(game, name)));
    }

    private static boolean walkable(Rom rom, LoadedRoom room, int x, int y) {
        int flag = physics(rom, room, x + 8, y + 12);
        // Restrict initial footing to ordinary grass/floor. Other terrain has
        // intentional ledge, fall, doorway and slow-ground state transitions.
        return (flag == 0 || flag == 6)
                && passableProbes(rom, room, x, y, 0)
                && passableProbes(rom, room, x, y, 1)
                && passableProbes(rom, room, x, y, 2)
                && passableProbes(rom, room, x, y, 3);
    }

    private static boolean passableProbes(Rom rom, LoadedRoom room, int x, int y, int dir) {
        for (int i = 0; i < 2; i++) {
            int px = x + XS[dir][i], py = y + YS[dir][i];
            int flag = physics(rom, room, px, py);
            // ApplyCollisionWithObject: exact dispatch at $02:6F62-6F9A;
            // residual $7C-$8F shapes at $02:715F-7199.
            if (flag >= 0x7C && flag < 0x90) {
                int quadrant = ((px >> 3) & 1) | ((py >> 2) & 2);
                if (rom.u8(RomSymbols.Data_002_49CA.plus((flag - 0x7C) * 4 + quadrant)) != 0) return false;
            } else if (flag == 1
                    || flag == 4
                    || flag == 0x30
                    || flag == 0x60
                    || flag == 0xC0
                    || flag == 0x10
                    || (flag >= 0x90 && flag < 0x99)
                    || (flag >= 0xD0 && flag < 0xD4)) return false;
            else if (flag == 3 && (py & 15) >= 6 && ((px & 15) < 6 || (px & 15) >= 11)) return false;
        }
        return true;
    }

    private static int physics(Rom rom, LoadedRoom room, int x, int y) {
        if (x < 0 || x >= 160 || y < 0 || y >= 128) return 1;
        int object = room.roomObjectsArea()[0x11 + (y & 0xF0) + (x >> 4)];
        return rom.u8(RomSymbols.OverworldObjectPhysicFlags.plus(object));
    }
}
