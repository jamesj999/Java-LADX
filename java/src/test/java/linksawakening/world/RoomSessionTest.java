package linksawakening.world;

import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomSessionTest {

    @Test
    void loadsInitialOverworldRoomIntoActiveRoomState() {
        RoomSession session = newSession();

        session.loadInitialOverworld(0x92);

        assertTrue(session.hasActiveRoom());
        assertEquals(0x92, session.currentRoomId());
        assertEquals(Warp.CATEGORY_OVERWORLD, session.mapCategory());
        assertNotNull(session.activeRoom().roomObjectsArea());
        assertEquals(0x92, session.activeRoom().entities().spriteSelection().roomId());
        assertNotNull(session.activeRoom().entities().spriteTiles());
        assertNotNull(session.renderSnapshot());
    }

    @Test
    void adjacentOverworldScrollLoadsNextRoomAndStartsScroll() {
        RoomSession session = newSession();
        ScrollController scrollController = new ScrollController();
        session.loadInitialOverworld(0x92);
        var previousEntities = session.activeRoom().entities();

        session.startAdjacentOverworldScroll(scrollController, ScrollController.RIGHT, 161, 40);

        assertEquals(0x93, session.currentRoomId());
        assertNotSame(previousEntities, session.activeRoom().entities());
        assertNotSame(previousEntities.spriteTiles(), session.activeRoom().entities().spriteTiles());
        assertEquals(0x93, session.activeRoom().entities().spriteSelection().roomId());
        assertEquals(previousEntities, scrollController.previousRoom().entities());
        assertNotNull(scrollController.previousRoom().entities().spriteTiles());
        assertTrue(scrollController.isActive());
        assertEquals(ScrollController.RIGHT, scrollController.direction());
        assertNotNull(scrollController.previousRoom());
    }

    @Test
    void notifiesListenerAfterRoomLoads() {
        List<Integer> loadedRoomIds = new ArrayList<>();
        RoomSession session = newSession(room -> loadedRoomIds.add(room.roomId()));
        ScrollController scrollController = new ScrollController();

        session.loadInitialOverworld(0x92);
        session.startAdjacentOverworldScroll(scrollController, ScrollController.RIGHT, 161, 40);

        assertEquals(List.of(0x92, 0x93), loadedRoomIds);
    }

    @Test
    void ticksTheLoadedEntityRuntimeAndRefreshesTheRenderSnapshot() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        session.tickEntities(0);

        assertEquals(EntityStatus.ACTIVE, session.activeRoom().entities().slots().get(0).status());
        assertEquals(0x6E, session.activeRoom().entities().slots().get(3).type());
        assertEquals(1, session.activeRoom().entities().slots().get(3).spriteVariant());
        assertEquals(session.activeRoom().entities(), session.renderSnapshot().entities());
    }

    @Test
    void returnsProjectileEventsFromTheEntityPassWhenLinkStateIsProvided() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        List<EntityProjectileEvent> events = session.tickEntitiesWithProjectileEvents(
            0, 0x08, 0x10, 0x00, 0x02, 0x00, false);

        assertTrue(events.isEmpty());
        assertEquals(session.activeRoom().entities(), session.renderSnapshot().entities());
    }

    @Test
    void publishesARealRomProjectileIntoTheSessionRenderSnapshot() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x2F);
        RoomEntity parent = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x09)
            .findFirst()
            .orElseThrow();

        RoomEntity projectile = null;
        for (int frame = 0; frame < 0x40 && projectile == null; frame++) {
            RoomEntity currentParent = session.activeRoom().entities().slots()
                .get(parent.slot());
            session.tickEntitiesWithProjectileEvents(
                frame, (currentParent.x() + 0x20) & 0xFF, currentParent.y(),
                0x00, 0x00, 0x00, false);
            projectile = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x0A)
                .findFirst()
                .orElse(null);
        }

        assertNotNull(projectile);
        assertEquals(-1, projectile.sourceLoadOrder());
        assertEquals(0x03, projectile.spriteDefinition().bank());
        assertEquals(0x6A1E, projectile.spriteDefinition().address());
        assertTrue(projectile.spriteDefinition().supported());
        assertEquals(session.activeRoom().entities(), session.renderSnapshot().entities());
        assertNotNull(session.renderSnapshot().entities().spriteTiles());
    }

    @Test
    void synchronizesDynamicFollowerSpawningAndFollowerDisplaySelection() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        session.setFollowingNpcState(
            new FollowingNpcState(true, 0, false, false, 0, 0, false),
            0x50, 0x60, 0x00, 0x00, 0x00);

        RoomEntity rooster = session.activeRoom().entities().slots().get(15);
        assertEquals(0xD5, rooster.type());
        assertEquals(EntityStatus.ACTIVE, rooster.status());
        assertEquals(0x50, rooster.x());
        assertEquals(0x60, rooster.y());
        assertEquals(0x19, rooster.spriteDefinition().bank());
        assertEquals(0x59BC, rooster.spriteDefinition().address());
        assertEquals(0x59BC, session.activeRoom().entities().spriteSelection()
            .spriteOverrideFor(0xD5).address());
        assertEquals(rooster, session.activeRoom().entities().slots().get(15));
    }

    @Test
    void loadsColorDungeonEntityRowsThroughTheSpecialRoomPath() {
        byte[] rom = loadRom();
        RoomSession session = newSession();

        session.loadIndoor(0xFF, 0x00);

        int sourceOffset = RomBank.romOffset(0x35, 0x5100);
        int expectedPixel = ((Byte.toUnsignedInt(rom[sourceOffset]) >>> 7) & 0x01)
            | (((Byte.toUnsignedInt(rom[sourceOffset + 1]) >>> 7) & 0x01) << 1);
        assertEquals(EntityRoomLoader.RoomTable.COLOR_DUNGEON,
            session.activeRoom().entities().spriteSelection().roomTable());
        assertNotNull(session.activeRoom().entities().spriteTiles());
        assertEquals(expectedPixel,
            session.activeRoom().entities().spriteTiles().tile(0x40).getPixel(0, 0));
    }

    @Test
    void persistsClearedFirstEightEntitySlotsWhenTheRoomReloads() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        session.clearEntity(0);
        session.loadOverworld(0x92);

        assertEquals(0x3E, session.activeRoom().entities().slots().get(0).type());
        assertEquals(1, session.activeRoom().entities().slots().get(0).sourceLoadOrder());
    }

    @Test
    void collectsAStaticPickupThroughTheActiveRoomSessionAndPersistsIt() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0xA4);

        RoomEntity pickup = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x3D)
            .findFirst()
            .orElseThrow();
        session.tickEntities(0);
        int collectionFrame = (pickup.slot() & 1) == 0 ? 1 : 0;

        EntityPickupEvent event = session.collectEntityIfNeeded(
            collectionFrame, pickup.x(), pickup.y(), false, true);

        assertNotNull(event);
        assertEquals(pickup.slot(), event.slot());
        assertEquals(EntityStatus.DISABLED,
            session.activeRoom().entities().slots().get(pickup.slot()).status());

        session.loadOverworld(0xA4);

        assertEquals(0xAE, session.activeRoom().entities().slots().get(0).type());
    }

    private static RoomSession newSession() {
        return newSession(room -> {
        });
    }

    private static RoomSession newSession(RoomLoadListener roomLoadListener) {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        return new RoomSession(
            rom,
            new GPU(),
            new RoomLoader(rom),
            new OverworldTilesetTable(rom),
            new OverworldCollision(romTables),
            new TransientVfxSystem(16),
            null,
            roomLoadListener
        );
    }

    private static byte[] loadRom() {
        try (var stream = RoomSessionTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }
}
