package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.testing.AllocationProbe;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.*;
import org.junit.jupiter.api.Test;

/** Read-only production paths over a populated synthetic room; no user ROM needed. */
class RoomRenderAllocationTest {
    private static volatile Object retained;
    private static volatile int checksum;

    static RoomSession room() {
        var rom = TestRoms.partial(new byte[0x100000]);
        var initial = TestEntities.room(
                TestEntities.active(9).slot(0).at(64, 64).build(),
                TestEntities.active(0x3E).slot(1).at(72, 64).build());
        var selection =
                new EntitySpriteSelection(EntityRoomLoader.RoomTable.INDOORS_A, 1, 0, new int[4], true, new int[6][4]);
        var runtime = RoomEntityRuntime.builder(initial.withSpriteSelection(selection))
                .build();
        runtime.configuration().setChestPlayerLevels(1, 0, 1);
        var session = new RoomSession(new RoomSessionConfig(
                rom,
                new GPU(),
                new RoomLoader(rom),
                new OverworldTilesetTable(rom),
                new OverworldCollision(RomTables.load(rom)),
                new RoomSessionConfig.Presentation(null, null, room -> {}),
                new RomRandomByteSource()));
        session.components.location().activeRoom = ActiveRoom.from(new LoadedRoom(new LoadedRoomSpec(
                new LoadedRoomSpec.Identity(1, 1, 0, 0),
                new LoadedRoomSpec.Objects(new int[256], null, null),
                new LoadedRoomSpec.Graphics(
                        new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT],
                        new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT],
                        new int[8][4]),
                new LoadedRoomSpec.Exits(List.of(), false, 0, -1),
                runtime.inspection().view())));
        session.components.entityBridge().entityRuntime = runtime;
        return session;
    }

    @Test
    void populatedRenderAndInventoryViewsAllocateLessThan256Bytes() {
        var session = room();
        double owned = AllocationProbe.bytesPerIteration(4000, 1000, () -> {
            retained = session.rooms().renderSnapshot();
            retained = session.dungeon().currentDungeonItemFlagsSnapshot();
            retained = session.dungeon().dungeonProgressFlagsSnapshot();
            retained = session.dungeon().dungeonMinimapRoomStatusSnapshot();
        });
        byte[] progress = new byte[8], items = new byte[5], minimap = new byte[64];
        double live = AllocationProbe.bytesPerIteration(4000, 1000, () -> {
            var view = session.rooms().renderView();
            retained = view;
            var entities = view.entities().loadedEntities();
            int value = entities.get(0).x() + entities.get(1).spriteVariant();
            for (int tile = 0; tile < view.tileCount(); tile++) value += view.tileId(tile) + view.tileAttribute(tile);
            for (int p = 0; p < view.paletteCount(); p++)
                for (int c = 0; c < view.colorCount(p); c++) value += view.paletteColor(p, c);
            for (int p = 0; p < view.spritePaletteCount(); p++)
                for (int c = 0; c < view.spriteColorCount(p); c++) value += view.spritePaletteColor(p, c);
            var dungeon = session.dungeon().view();
            dungeon.copyTo(progress, items, minimap);
            for (int i = 0; i < dungeon.minimapSize(); i++) value += dungeon.minimapStatus(i);
            for (int i = 0; i < dungeon.currentItemFlagCount(); i++) value += dungeon.currentItemFlag(i);
            for (int i = 0; i < dungeon.progressFlagCount(); i++) value += dungeon.progressFlag(i);
            checksum = value;
        });
        System.out.println("Owned room/inventory / live views bytes/frame: " + owned + " / " + live);
        assertTrue(live < 256, "Render view bytes/frame: " + live);
        assertNotEquals(0, checksum);
    }

    @Test
    void liveViewsFollowChangesWhileOwnedCopiesStayHistorical() {
        var session = room();
        var view = session.rooms().renderView();
        var snapshot = session.rooms().renderSnapshot();
        var flags = session.dungeon().currentDungeonItemFlagsSnapshot();
        session.rooms().activeRoom().tileIds()[0] = 7;
        session.rooms().activeRoom().palettes()[0][0] = 11;
        session.dungeon().setDungeonItemForDebug(0, 0, 1);
        session.components.roomState().dungeonItemState.loadForMap(0, true);
        assertSame(view, session.rooms().renderView());
        assertEquals(7, view.tileId(0));
        assertEquals(11, view.paletteColor(0, 0));
        assertEquals(0, snapshot.tileIds()[0]);
        assertEquals(0, snapshot.palettes()[0][0]);
        assertEquals(1, session.dungeon().view().currentItemFlag(0));
        assertEquals(0, flags[0]);
        assertThrows(
                UnsupportedOperationException.class,
                () -> view.entities().slots().clear());
    }
}
