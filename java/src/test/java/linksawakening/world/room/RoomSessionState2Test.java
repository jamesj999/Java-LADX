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
        passableSession.rooms().loadIndoor(0x00, 0x05);
        conveyorSession.rooms().loadIndoor(0x00, 0x05);

        RoomEntity passableInitial = passableSession.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x17)
                .findFirst()
                .orElseThrow();
        RoomEntity conveyorInitial =
                conveyorSession.rooms().activeRoom().entities().slots().get(passableInitial.slot());
        fillActiveObjects(passableSession, 0x02);
        fillActiveObjects(conveyorSession, 0xCF);

        for (int frame = 0; frame <= 4; frame++) {
            passableSession.entities().tickEntities(frame, 0, 0);
            conveyorSession.entities().tickEntities(frame, 0, 0);
        }

        RoomEntity passable =
                passableSession.rooms().activeRoom().entities().slots().get(passableInitial.slot());
        RoomEntity conveyor =
                conveyorSession.rooms().activeRoom().entities().slots().get(conveyorInitial.slot());
        assertEquals(passable.x(), conveyor.x());
        assertEquals(passable.y(), conveyor.y());
    }

    @Test
    void ordinaryEntityKeepsTheRomShallowWaterGroundStatus() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x2F);
        RoomEntity initial = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x09)
                .findFirst()
                .orElseThrow();
        fillActiveObjects(session, 0x1B);

        session.entities().tickEntities(0);
        session.entities().tickEntities(1);

        RoomEntity entity = session.rooms().activeRoom().entities().slots().get(initial.slot());
        assertEquals(EntityStatus.ACTIVE, entity.status());
        assertEquals(0x02, session.components.roomSessionViews().entityGroundStatusForTest(initial.slot()));
    }

    @Test
    void gameplayVblankSynchronizesTheToggledStateIntoLinkCollision() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x0F);
        fillActiveObjects(session, 0xDC);
        session.components.roomSessionViews().setEntitySwitchBlocksStateForTest(0x00);

        assertTrue(session.components.roomSessionViews().linkCollisionPointBlockedForTest(0x20, 0x20));

        session.components.roomSessionViews().setSwitchableObjectAnimationStageForTest(0x02);
        session.entityState().tickGameplayVBlank();

        assertEquals(0x02, session.components.roomSessionViews().entitySwitchBlocksStateForTest());
        assertFalse(session.components.roomSessionViews().linkCollisionPointBlockedForTest(0x20, 0x20));
    }

    @Test
    void ordinaryEntityEnteringRomDeepWaterUnloadsAndCreatesSplashSideEffects() {
        TransientVfxSystem vfx = new TransientVfxSystem(16);
        RoomSession session = newSession(vfx);
        session.rooms().loadInitialOverworld(0x2F);
        RoomEntity initial = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x09)
                .findFirst()
                .orElseThrow();
        for (RoomEntity entity : session.rooms().activeRoom().entities().loadedEntities()) {
            if (entity.slot() != initial.slot()) {
                session.entityState().clearEntity(entity.slot());
            }
        }
        fillActiveObjects(session, 0x0E);

        session.entities().tickEntities(0);
        session.entities().tickEntities(1);

        assertEquals(
                EntityStatus.DISABLED,
                session.rooms()
                        .activeRoom()
                        .entities()
                        .slots()
                        .get(initial.slot())
                        .status());
        assertEquals(0, session.components.roomSessionViews().entityGroundStatusForTest(initial.slot()));
        assertEquals(1, vfx.activeCount());
        assertEquals(TransientVfxType.WATER_SPLASH, vfx.activeSlots().getFirst().type());
        List<EntityCombatEvent> events =
                session.components.entityBridge().outbox.consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x0E, events.getFirst().soundId());
    }

    @Test
    void loadsColorDungeonEntityRowsThroughTheSpecialRoomPath() {
        byte[] rom = loadRom();
        RoomSession session = newSession();

        session.rooms().loadIndoor(0xFF, 0x00);

        int sourceOffset = RomBank.romOffset(0x35, 0x5100);
        int expectedPixel = ((Byte.toUnsignedInt(rom[sourceOffset]) >>> 7) & 0x01)
                | (((Byte.toUnsignedInt(rom[sourceOffset + 1]) >>> 7) & 0x01) << 1);
        assertEquals(
                EntityRoomLoader.RoomTable.COLOR_DUNGEON,
                session.rooms().activeRoom().entities().spriteSelection().roomTable());
        assertNotNull(session.rooms().activeRoom().entities().spriteTiles());
        assertEquals(
                expectedPixel,
                session.rooms().activeRoom().entities().spriteTiles().tile(0x40).getPixel(0, 0));

        int objectId = session.rooms().activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE];
        int expectedTileOffset = RomBank.romOffset(0x08, 0x4760) + objectId * 4;
        int expectedAttrOffset = RomBank.romOffset(0x23, 0x6000) + objectId * 4;
        int expectedPaletteOffset = RomBank.romOffset(0x21, 0x67D0);
        int expectedPaletteColor = RomBank.decodeRgb555(Byte.toUnsignedInt(rom[expectedPaletteOffset])
                | (Byte.toUnsignedInt(rom[expectedPaletteOffset + 1]) << 8));
        assertEquals(expectedPaletteColor, session.rooms().activeRoom().palettes()[0][0]);
        assertEquals(
                Byte.toUnsignedInt(rom[expectedTileOffset]),
                session.rooms().activeRoom().tileIds()[0]);
        assertEquals(
                Byte.toUnsignedInt(rom[expectedAttrOffset]),
                session.rooms().activeRoom().tileAttrs()[0]);
    }

    @Test
    void colorShellPuzzleWritesBackIntoTheLiveRoomTilemap() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0xFF, 0x07);
        session.entities().tickEntities(0);

        RoomEntity shell = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xE9)
                .findFirst()
                .orElseThrow();
        int objectIndex =
                RoomConstants.ROOM_OBJECTS_BASE + ((shell.y() - 0x07) & 0xF0) + (((shell.x() - 0x01) & 0xF0) >>> 4);
        session.rooms().activeRoom().roomObjectsArea()[objectIndex - 1] = 0x5E;
        int[] beforeTiles = session.rooms().activeRoom().tileIds().clone();
        session.components.roomSessionViews().setColorShellStateForTest(shell.slot(), 0x08, 1, 0, 0, 0);

        session.entities().tickEntities(1);

        assertEquals(0x67, session.rooms().activeRoom().roomObjectsArea()[objectIndex]);
        assertNotEquals(
                Arrays.toString(beforeTiles),
                Arrays.toString(session.rooms().activeRoom().tileIds()));
    }

    @Test
    void persistsClearedFirstEightEntitySlotsWhenTheRoomReloads() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);

        session.entityState().clearEntity(0);
        session.rooms().loadOverworld(0x92);

        assertEquals(
                0x3E, session.rooms().activeRoom().entities().slots().get(0).type());
        assertEquals(1, session.rooms().activeRoom().entities().slots().get(0).sourceLoadOrder());
    }

    @Test
    void exposesAndRestoresTheRoomStatusTablesUsedBySaveGameToFile() {
        RoomSession session = newSession();
        byte[] overworld = pattern(0x100, 0x10);
        byte[] indoorA = pattern(0x100, 0x40);
        byte[] indoorB = pattern(0x100, 0x70);
        byte[] colorDungeon = pattern(0x20, 0xA0);

        session.story().restoreRoomStatuses(overworld, indoorA, indoorB, colorDungeon);

        assertArrayEquals(overworld, session.dungeon().overworldRoomStatusSnapshot());
        assertArrayEquals(indoorA, session.dungeon().indoorARoomStatusSnapshot());
        assertArrayEquals(indoorB, session.dungeon().indoorBRoomStatusSnapshot());
        assertArrayEquals(colorDungeon, session.dungeon().colorDungeonRoomStatusSnapshot());
    }

    @Test
    void exposesAndRestoresDungeonItemFlagsUsedByTheWorldHandler() {
        RoomSession session = newSession();
        byte[] dungeonFlags = pattern(DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE, 0x10);
        byte[] colorFlags = pattern(DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE, 0xE0);

        session.dungeon().restoreDungeonItemFlags(dungeonFlags, colorFlags);
        session.rooms().loadIndoor(2, 0x25);

        assertArrayEquals(dungeonFlags, session.dungeon().dungeonItemFlagsSnapshot());
        assertArrayEquals(colorFlags, session.dungeon().colorDungeonItemFlagsSnapshot());
        assertArrayEquals(
                new byte[] {
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE],
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 1],
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 2],
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 3],
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 4]
                },
                session.dungeon().currentDungeonItemFlagsSnapshot());
    }

    @Test
    void exposesAndRestoresTheEightSourceDungeonProgressBytes() {
        RoomSession session = newSession();
        byte[] flags = pattern(0x08, 0x01);

        session.dungeon().restoreDungeonProgressFlags(flags);

        assertArrayEquals(flags, session.dungeon().dungeonProgressFlagsSnapshot());
        assertTrue(session.components.roomSessionViews().hasDungeonInstrumentForTest(1));
        assertTrue(session.components.roomSessionViews().hasDungeonInstrumentForTest(2));
    }

    @Test
    void madamMeowMeowRejectsWrongFacingWrongButtonAndTalkStateGates() {
        RoomSession session = newSession();
        session.story().setBowWowState(0x80);
        session.rooms().loadIndoor(0x10, 0xA7);
        RoomEntity madam = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x79)
                .findFirst()
                .orElseThrow();
        int linkY = madam.y() + 0x10;

        session.entityState().setEntityActionButtonsHeld(true, false);
        session.entities().tickEntities(0, madam.x(), linkY, 0, Link.DIRECTION_DOWN);
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());

        session.entityState().setEntityActionButtonsHeld(false, true);
        session.entities().tickEntities(1, madam.x(), linkY, 0, Link.DIRECTION_UP);
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());

        session.entityState().setEntityActionButtonsHeld(true, false);
        session.entityState().setEntityTalkState(false, 1, 0x80);
        session.entities().tickEntities(2, madam.x(), linkY, 0, Link.DIRECTION_UP);
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());

        session.entityState().setEntityTalkState(false, 0, 0x70);
        session.entities().tickEntities(3, madam.x(), linkY, 0, Link.DIRECTION_UP);
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());

        session.entityState().setEntityTalkState(false, 0, 0x80);
        session.entities().tickEntities(4, madam.x(), linkY, 0, true, Link.DIRECTION_UP);
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
    }
}
