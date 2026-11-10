package linksawakening.world.room;

import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@RequiresRom
final class LibraryTreeCollisionTest {
    @Test
    void offscreenTreeStripBlocksLinksLeadingEdgesFromEveryDirection() throws Exception {
        byte[] rom;
        try (var input = TestRom.open()) { rom = input.readAllBytes(); }
        var room = new RoomLoader(rom).loadOverworld(0xB0);
        assertTrue(room.warps().stream().anyMatch(w -> w.destMap() == 0x1D), "library exterior");
        var tables = RomTables.loadFromRom(rom);
        var collision = new OverworldCollision(tables);
        collision.setRoom(room.roomObjectsArea());
        // Room B0's C4 F9 F5 strip begins above the screen. TreeMacroHandler
        // ($24:759D) wraps F9+11 to 0A and expands down the eastern border.
        for (int row = 0; row < 8; row++) {
            int tileX = 9 * 16, tileY = row * 16;
            int object = room.roomObjectsArea()[0x11 + row * 16 + 9];
            assertTrue(object >= 0x25 && object <= 0x2A, "tree row " + row);
            assertEquals(1, tables.objectPhysicsFlag(RomTables.PHYSICS_TABLE_OVERWORLD, object));
            for (int direction = 0; direction < 4; direction++) {
                // Positions put both ROM LinkCollisionPointsX/Y probes
                // ($02:6E15/$6E1D) one pixel outside the chosen tree edge.
                int x = switch (direction) {
                    case Link.DIRECTION_LEFT -> tileX + 12;
                    case Link.DIRECTION_RIGHT -> tileX - 12;
                    default -> tileX + 1;
                };
                int y = switch (direction) {
                    case Link.DIRECTION_UP -> tileY + 10;
                    case Link.DIRECTION_DOWN -> tileY - 16;
                    default -> tileY + 1;
                };
                var config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
                var input = new InputState();
                int key = switch (direction) {
                    case Link.DIRECTION_LEFT -> config.leftKey();
                    case Link.DIRECTION_RIGHT -> config.rightKey();
                    case Link.DIRECTION_UP -> config.upKey();
                    default -> config.downKey();
                };
                input.onKeyEvent(key, 1);
                var link = new Link(input, config, tables, collision, null, new PlayerState(), new ItemRegistry());
                link.setPixelPosition(x, y);
                for (int frame = 0; frame < 4; frame++) link.update();
                String context = "row " + row + " direction " + direction;
                assertEquals(x, link.pixelX(), context + " X");
                assertEquals(y, link.pixelY(), context + " Y");
                assertNotEquals(0, link.romCollisionType(), context + " background collision");
            }
        }
    }
}
