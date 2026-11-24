package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;

@RequiresRom
final class RoomScrollPreparationTest {
    @Test
    void beachScrollDoesNotIntegrateUntilAfterItsThreePreparationPhases() throws Exception {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0xF0);
        ScrollController scroll = new ScrollController();
        session.transitions().startAdjacentOverworldScroll(scroll, ScrollController.RIGHT, 161, 40);

        assertFirstIntegration(scroll, 4);
    }

    @Test
    void villageScrollWaitsForTwoQueuedEntitySheets() throws Exception {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0xA2);
        ScrollController scroll = new ScrollController();
        session.transitions().startAdjacentOverworldScroll(scroll, ScrollController.LEFT, 4, 120);

        assertFirstIntegration(scroll, 11);
    }

    @Test
    void overworldBackgroundChangeWaitsForEightVBlankCopies() throws Exception {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0xD0);
        ScrollController scroll = new ScrollController();
        session.transitions().startAdjacentOverworldScroll(scroll, ScrollController.DOWN, 136, 140);

        assertEquals(0xE0, session.rooms().currentRoomId());
        assertFirstIntegration(scroll, 11);
    }

    @Test
    void indoorBackgroundChangeWaitsForFourVBlankCopies() throws Exception {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x13);
        ScrollController scroll = new ScrollController();
        session.transitions().startAdjacentIndoorScroll(scroll, ScrollController.RIGHT, 155, 58);

        assertEquals(0x14, session.rooms().currentRoomId());
        assertFirstIntegration(scroll, 7);
    }

    @Test
    void incomingSparksInitializeOnceAndDrawFromTheDispatchedClockWithoutMoving() throws Exception {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x0F);
        ScrollController scroll = new ScrollController();
        session.transitions().startAdjacentIndoorScroll(scroll, ScrollController.LEFT, 5, 80);
        assertEquals(0x0E, session.rooms().currentRoomId());
        assertFalse(scroll.incomingRoomPrepared());
        assertSpark(find(session, 0x16), EntityStatus.INIT, 120, 112);
        assertSpark(find(session, 0x17), EntityStatus.INIT, 72, 32);

        int activeDispatches = 0;
        int displayedVariant = 0;
        for (int frame = 186; scroll.isActive(); frame++) {
            scroll.tick(4);
            if (scroll.incomingRoomPrepared() && !scroll.tileTransferOnlyFrame()) {
                tickIncomingScrollEntities(session, frame);
            }
            RoomEntity counterClockwise = find(session, 0x16);
            RoomEntity clockwise = find(session, 0x17);
            assertSpark(counterClockwise, EntityStatus.ACTIVE, 120, 109);
            assertSpark(clockwise, EntityStatus.ACTIVE, 72, 35);
            if (frame > 186 && scroll.incomingRoomPrepared() && !scroll.tileTransferOnlyFrame()) {
                activeDispatches++;
                displayedVariant = (frame >>> 1) & 1;
            }
            assertEquals(displayedVariant, counterClockwise.spriteVariant());
            assertEquals(displayedVariant, clockwise.spriteVariant());
        }
        assertEquals(41, activeDispatches);
    }

    @Test
    void incomingLeeverSetsItsHiddenVariantOnThePrepareFrame() throws Exception {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0xF0);
        ScrollController scroll = new ScrollController();
        session.transitions().startAdjacentOverworldScroll(scroll, ScrollController.RIGHT, 161, 40);
        assertEquals(EntityStatus.INIT, find(session, 0x09).status());
        assertEquals(EntityStatus.INIT, find(session, 0x0E).status());

        scroll.tick(4);
        tickIncomingScrollEntities(session, 191);

        assertEquals(EntityStatus.ACTIVE, find(session, 0x09).status());
        RoomEntity leever = find(session, 0x0E);
        assertEquals(EntityStatus.ACTIVE, leever.status());
        assertEquals(-1, leever.spriteVariant());
        assertEquals(0, scroll.offset());
        assertEquals(
                EntityStatus.INIT,
                scroll.previousRoom().entities().loadedEntities().getFirst().status());
    }

    private static void tickIncomingScrollEntities(RoomSession session, int frame) {
        session.tickEntities(new EntityTickInputs(
                frame,
                new LinkPosition(5, 80, 0, 0, 0),
                new LinkMotion(false, 0, false, 0, 0, 0),
                0,
                new SwordBox(false, 0, 0, 0, 0),
                true));
    }

    private static RoomEntity find(RoomSession session, int type) {
        return session.rooms().requireActiveRoom().entityView().loadedEntities().stream()
                .filter(entity -> entity.type() == type)
                .findFirst()
                .orElseThrow();
    }

    private static void assertSpark(RoomEntity spark, EntityStatus status, int x, int y) {
        assertEquals(status, spark.status());
        assertEquals(x, spark.x());
        assertEquals(y, spark.y());
    }

    private static RoomSession newSession() throws Exception {
        byte[] rom = RoomNavigationFixture.loadRom();
        var tables = RomTables.load(TestRoms.partial(rom));
        return RoomNavigationFixture.newSession(rom, tables, new OverworldCollision(tables));
    }

    private static void assertFirstIntegration(ScrollController scroll, int firstIntegration) {
        for (int frame = 1; frame < firstIntegration; frame++) {
            assertTrue(scroll.isActive());
            assertFalse(scroll.tick(4));
            assertEquals(0, scroll.offset());
        }
        assertFalse(scroll.tick(4));
        assertEquals(4, scroll.offset());
    }
}
