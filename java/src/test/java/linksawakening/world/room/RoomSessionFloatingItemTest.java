package linksawakening.world.room;

import linksawakening.world.*;

import linksawakening.world.room.OverworldTilesetTable;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomLoader;

import linksawakening.world.transition.Warp;

import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@linksawakening.testing.RequiresRom
final class RoomSessionFloatingItemTest {

    @Test
    void activeSideScrollRoomMarksEntityRenderSnapshotForMixedSpriteYRules() {
        byte[] rom = loadRom();
        RomTables tables = RomTables.loadFromRom(rom);
        RoomSession session = new RoomSession(
            rom, new GPU(), new RoomLoader(rom), new OverworldTilesetTable(rom),
            new OverworldCollision(tables), new TransientVfxSystem(16), null);

        session.loadInitialOverworld(0x92);
        session.loadIndoor(0x00, 0x00, Warp.CATEGORY_SIDESCROLL);

        assertTrue(session.activeRoom().entities().sideScrolling());
        assertTrue(session.renderSnapshot().entities().sideScrolling());
    }

    private static byte[] loadRom() {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }
}
