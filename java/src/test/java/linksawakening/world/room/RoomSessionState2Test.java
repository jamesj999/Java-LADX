package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import linksawakening.entity.Link;
import linksawakening.rom.RomBank;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomSessionState2Test {

    @Test
    void noGroundInteractionEntitiesIgnoreTheRomConveyorNudge() {
        RoomSession passableSession = newSession();
        RoomSession conveyorSession = newSession();
        passableSession.loadIndoor(0x00, 0x05);
        conveyorSession.loadIndoor(0x00, 0x05);

        RoomEntity passableInitial = passableSession.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x17)
                .findFirst()
                .orElseThrow();
        RoomEntity conveyorInitial =
                conveyorSession.activeRoom().entities().slots().get(passableInitial.slot());
        fillActiveObjects(passableSession, 0x02);
        fillActiveObjects(conveyorSession, 0xCF);

        for (int frame = 0; frame <= 4; frame++) {
            passableSession.tickEntities(frame, 0, 0);
            conveyorSession.tickEntities(frame, 0, 0);
        }

        RoomEntity passable = passableSession.activeRoom().entities().slots().get(passableInitial.slot());
        RoomEntity conveyor = conveyorSession.activeRoom().entities().slots().get(conveyorInitial.slot());
        assertEquals(passable.x(), conveyor.x());
        assertEquals(passable.y(), conveyor.y());
    }

    @Test
    void ordinaryEntityKeepsTheRomShallowWaterGroundStatus() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x2F);
        RoomEntity initial = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x09)
                .findFirst()
                .orElseThrow();
        fillActiveObjects(session, 0x1B);

        session.tickEntities(0);
        session.tickEntities(1);

        RoomEntity entity = session.activeRoom().entities().slots().get(initial.slot());
        assertEquals(EntityStatus.ACTIVE, entity.status());
        assertEquals(0x02, session.entityGroundStatusForTest(initial.slot()));
    }

    @Test
    void gameplayVblankSynchronizesTheToggledStateIntoLinkCollision() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x0F);
        fillActiveObjects(session, 0xDC);
        session.setEntitySwitchBlocksStateForTest(0x00);

        assertTrue(session.linkCollisionPointBlockedForTest(0x20, 0x20));

        session.setSwitchableObjectAnimationStageForTest(0x02);
        session.tickGameplayVBlank();

        assertEquals(0x02, session.entitySwitchBlocksStateForTest());
        assertFalse(session.linkCollisionPointBlockedForTest(0x20, 0x20));
    }

    @Test
    void ordinaryEntityEnteringRomDeepWaterUnloadsAndCreatesSplashSideEffects() {
        TransientVfxSystem vfx = new TransientVfxSystem(16);
        RoomSession session = newSession(vfx);
        session.loadInitialOverworld(0x2F);
        RoomEntity initial = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x09)
                .findFirst()
                .orElseThrow();
        for (RoomEntity entity : session.activeRoom().entities().loadedEntities()) {
            if (entity.slot() != initial.slot()) {
                session.clearEntity(entity.slot());
            }
        }
        fillActiveObjects(session, 0x0E);

        session.tickEntities(0);
        session.tickEntities(1);

        assertEquals(
                EntityStatus.DISABLED,
                session.activeRoom().entities().slots().get(initial.slot()).status());
        assertEquals(0, session.entityGroundStatusForTest(initial.slot()));
        assertEquals(1, vfx.activeCount());
        assertEquals(TransientVfxType.WATER_SPLASH, vfx.activeSlots().getFirst().type());
        List<EntityCombatEvent> events = session.consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x0E, events.getFirst().soundId());
    }

    @Test
    void loadsColorDungeonEntityRowsThroughTheSpecialRoomPath() {
        byte[] rom = loadRom();
        RoomSession session = newSession();

        session.loadIndoor(0xFF, 0x00);

        int sourceOffset = RomBank.romOffset(0x35, 0x5100);
        int expectedPixel = ((Byte.toUnsignedInt(rom[sourceOffset]) >>> 7) & 0x01)
                | (((Byte.toUnsignedInt(rom[sourceOffset + 1]) >>> 7) & 0x01) << 1);
        assertEquals(
                EntityRoomLoader.RoomTable.COLOR_DUNGEON,
                session.activeRoom().entities().spriteSelection().roomTable());
        assertNotNull(session.activeRoom().entities().spriteTiles());
        assertEquals(
                expectedPixel,
                session.activeRoom().entities().spriteTiles().tile(0x40).getPixel(0, 0));

        int objectId = session.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE];
        int expectedTileOffset = RomBank.romOffset(0x08, 0x4760) + objectId * 4;
        int expectedAttrOffset = RomBank.romOffset(0x23, 0x6000) + objectId * 4;
        int expectedPaletteOffset = RomBank.romOffset(0x21, 0x67D0);
        int expectedPaletteColor = RomBank.decodeRgb555(Byte.toUnsignedInt(rom[expectedPaletteOffset])
                | (Byte.toUnsignedInt(rom[expectedPaletteOffset + 1]) << 8));
        assertEquals(expectedPaletteColor, session.activeRoom().palettes()[0][0]);
        assertEquals(
                Byte.toUnsignedInt(rom[expectedTileOffset]),
                session.activeRoom().tileIds()[0]);
        assertEquals(
                Byte.toUnsignedInt(rom[expectedAttrOffset]),
                session.activeRoom().tileAttrs()[0]);
    }

    @Test
    void colorShellPuzzleWritesBackIntoTheLiveRoomTilemap() {
        RoomSession session = newSession();
        session.loadIndoor(0xFF, 0x07);
        session.tickEntities(0);

        RoomEntity shell = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xE9)
                .findFirst()
                .orElseThrow();
        int objectIndex =
                RoomConstants.ROOM_OBJECTS_BASE + ((shell.y() - 0x07) & 0xF0) + (((shell.x() - 0x01) & 0xF0) >>> 4);
        session.activeRoom().roomObjectsArea()[objectIndex - 1] = 0x5E;
        int[] beforeTiles = session.activeRoom().tileIds().clone();
        session.setColorShellStateForTest(shell.slot(), 0x08, 1, 0, 0, 0);

        session.tickEntities(1);

        assertEquals(0x67, session.activeRoom().roomObjectsArea()[objectIndex]);
        assertNotEquals(
                Arrays.toString(beforeTiles),
                Arrays.toString(session.activeRoom().tileIds()));
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
    void exposesAndRestoresTheRoomStatusTablesUsedBySaveGameToFile() {
        RoomSession session = newSession();
        byte[] overworld = pattern(0x100, 0x10);
        byte[] indoorA = pattern(0x100, 0x40);
        byte[] indoorB = pattern(0x100, 0x70);
        byte[] colorDungeon = pattern(0x20, 0xA0);

        session.restoreRoomStatuses(overworld, indoorA, indoorB, colorDungeon);

        assertArrayEquals(overworld, session.overworldRoomStatusSnapshot());
        assertArrayEquals(indoorA, session.indoorARoomStatusSnapshot());
        assertArrayEquals(indoorB, session.indoorBRoomStatusSnapshot());
        assertArrayEquals(colorDungeon, session.colorDungeonRoomStatusSnapshot());
    }

    @Test
    void exposesAndRestoresDungeonItemFlagsUsedByTheWorldHandler() {
        RoomSession session = newSession();
        byte[] dungeonFlags = pattern(DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE, 0x10);
        byte[] colorFlags = pattern(DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE, 0xE0);

        session.restoreDungeonItemFlags(dungeonFlags, colorFlags);
        session.loadIndoor(2, 0x25);

        assertArrayEquals(dungeonFlags, session.dungeonItemFlagsSnapshot());
        assertArrayEquals(colorFlags, session.colorDungeonItemFlagsSnapshot());
        assertArrayEquals(
                new byte[] {
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE],
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 1],
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 2],
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 3],
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 4]
                },
                session.currentDungeonItemFlagsSnapshot());
    }

    @Test
    void exposesAndRestoresTheEightSourceDungeonProgressBytes() {
        RoomSession session = newSession();
        byte[] flags = pattern(0x08, 0x01);

        session.restoreDungeonProgressFlags(flags);

        assertArrayEquals(flags, session.dungeonProgressFlagsSnapshot());
        assertTrue(session.hasDungeonInstrumentForTest(1));
        assertTrue(session.hasDungeonInstrumentForTest(2));
    }

    @Test
    void madamMeowMeowRejectsWrongFacingWrongButtonAndTalkStateGates() {
        RoomSession session = newSession();
        session.setBowWowState(0x80);
        session.loadIndoor(0x10, 0xA7);
        RoomEntity madam = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x79)
                .findFirst()
                .orElseThrow();
        int linkY = madam.y() + 0x10;

        session.setEntityActionButtonsHeld(true, false);
        session.tickEntities(0, madam.x(), linkY, 0, Link.DIRECTION_DOWN);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());

        session.setEntityActionButtonsHeld(false, true);
        session.tickEntities(1, madam.x(), linkY, 0, Link.DIRECTION_UP);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());

        session.setEntityActionButtonsHeld(true, false);
        session.setEntityTalkState(false, 1, 0x80);
        session.tickEntities(2, madam.x(), linkY, 0, Link.DIRECTION_UP);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());

        session.setEntityTalkState(false, 0, 0x70);
        session.tickEntities(3, madam.x(), linkY, 0, Link.DIRECTION_UP);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());

        session.setEntityTalkState(false, 0, 0x80);
        session.tickEntities(4, madam.x(), linkY, 0, true, Link.DIRECTION_UP);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
    }
}
