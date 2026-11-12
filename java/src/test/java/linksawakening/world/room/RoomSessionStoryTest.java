package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.Link;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomSessionStoryTest {

    @Test
    void keyCavernOwlStatueUsesCurrentDungeonsStoneBeakAndKeepsWorldPosition() {
        for (boolean hasBeak : new boolean[] {false, true}) {
            RoomSession session = newSession();
            byte[] flags = new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE];
            flags[2 * DungeonItemState.ITEM_FLAG_SIZE + DungeonItemState.STONE_BEAK_INDEX] = (byte) (hasBeak ? 1 : 0);
            session.restoreDungeonItemFlags(flags, new byte[5]);
            session.loadIndoor(2, 0x54);
            RoomEntity statue = session.activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x42)
                    .findFirst()
                    .orElseThrow();
            session.tickEntities(0, statue.x(), statue.y() + 0x13, 0, Link.DIRECTION_UP);
            session.setEntityActionButtonsHeld(true, false);
            session.tickEntities(1, statue.x(), statue.y() + 0x13, 0, Link.DIRECTION_UP);
            assertEquals(
                    List.of(new DialogRequest(hasBeak ? 2 : 1, hasBeak ? 0x82 : 0x10)),
                    session.consumeEntityDialogRequests());
            RoomEntity updated = session.activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x42)
                    .findFirst()
                    .orElseThrow();
            assertEquals(statue.x(), updated.x());
            assertEquals(statue.y(), updated.y());
            assertEquals(
                    EntitySpriteDefinition.Shape.DYNAMIC,
                    updated.spriteDefinition().shape());
        }
    }

    @Test
    void owlStatueRequiresAButtonFacingRangeAndInteractiveTalkState() {
        RoomSession session = newSession();
        session.loadIndoor(2, 0x54);
        int x = 0x38;
        int y = 0x40;
        session.tickEntities(0, x, y + 0x13, 0, Link.DIRECTION_UP);
        session.setEntityActionButtonsHeld(false, true);
        session.tickEntities(1, x, y + 0x13, 0, Link.DIRECTION_UP);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
        session.setEntityActionButtonsHeld(true, false);
        session.tickEntities(2, x, y + 0x13, 0, Link.DIRECTION_DOWN);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
        session.tickEntities(3, x, y + 0x14, 0, Link.DIRECTION_UP);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
        session.setEntityTalkState(false, 1, 0x80);
        session.tickEntities(4, x, y + 0x13, 0, Link.DIRECTION_UP);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
        session.setEntityTalkState(true, 0, 0x80);
        session.tickEntities(5, x, y + 0x13, 0, Link.DIRECTION_UP);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
        session.setEntityTalkState(false, 0, 0x70);
        session.tickEntities(6, x, y + 0x13, 0, Link.DIRECTION_UP);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
        session.setEntityTalkState(false, 0, 0x80);
        session.setEntityDialogActive(true);
        session.tickEntities(7, x, y + 0x13, 0, Link.DIRECTION_UP);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
        session.setEntityDialogActive(false);
        session.tickEntities(8, x, y + 0x13, 0, true, Link.DIRECTION_UP);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
        session.tickEntities(9, x + 0x10, y, 0, Link.DIRECTION_LEFT);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
        session.tickEntities(10, x, y + 0x13, 0, Link.DIRECTION_UP);
        assertEquals(0x110, session.consumeEntityDialogRequests().getFirst().globalDialogId());
    }

    @Test
    void postSwordForestOwlUsesItsRomDialogAndPersistsCompletion() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x80);
        session.setChestPlayerLevels(0, 1, 0);

        tickInteractive(session, 0, 0x50, 0x50);
        tickInteractive(session, 1, 0x50, 0x50);
        assertEquals(0x22, session.consumePendingMusicTrack());
        int frame = 2;
        boolean dialogOpened = false;
        while (!dialogOpened && frame < 0x300) {
            tickInteractive(session, frame++, 0x50, 0x50);
            dialogOpened |= session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0C0);
        }
        assertTrue(dialogOpened);
        while ((session.overworldRoomStatusForTest(0x80) & 0x20) == 0 && frame < 0x340) {
            tickInteractive(session, frame++, 0x50, 0x50);
        }
        assertEquals(0x20, session.overworldRoomStatusForTest(0x80) & 0x20);

        boolean restoredMusic = false;
        while (session.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x41)
                && frame < 0x500) {
            tickInteractive(session, frame++, 0x50, 0x50);
            restoredMusic |= session.consumePendingMusicTrack() == 0x09;
        }
        assertTrue(restoredMusic);

        session.loadInitialOverworld(0x80);
        assertFalse(
                session.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x41));
    }

    @Test
    void forestOwlRestoresActivePowerUpMusicWhenDepartureFinishes() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x80);
        session.setChestPlayerLevels(0, 1, 0);
        session.setEnemyDropPlayerState(3, 3, true);

        int frame = 0;
        boolean restoredPowerMusic = false;
        while (session.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x41)
                && frame < 0x500) {
            tickInteractive(session, frame++, 0x50, 0x50);
            restoredPowerMusic |= session.consumePendingMusicTrack() == 0x49;
            session.consumeEntityDialogRequests();
        }

        assertTrue(restoredPowerMusic);
    }

    @Test
    void fifthKanaletLeafPublishesFinalLeafDialogAndCompletesItsSourceRoom() {
        RoomSession session = newSession();
        session.loadOverworld(0x58);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(List.of(
                new RoomEntity(
                        0,
                        0xFF,
                        0x3C,
                        0x40,
                        0x50,
                        EntityStatus.INIT,
                        EntitySpriteDefinition.unsupported(0x3C),
                        -1,
                        0,
                        0,
                        0,
                        -1,
                        false),
                RoomEntity.disabled(1),
                RoomEntity.disabled(2),
                RoomEntity.disabled(3),
                RoomEntity.disabled(4),
                RoomEntity.disabled(5),
                RoomEntity.disabled(6),
                RoomEntity.disabled(7),
                RoomEntity.disabled(8),
                RoomEntity.disabled(9),
                RoomEntity.disabled(10),
                RoomEntity.disabled(11),
                RoomEntity.disabled(12),
                RoomEntity.disabled(13),
                RoomEntity.disabled(14),
                RoomEntity.disabled(15))));
        linksawakening.world.entity.EntityRuntimeTestAccess.setEntityRoomIdForTest(runtime, 0x58);
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0xCC, 0, 0x40, 0x50));
        session.replaceEntityRuntimeForTest(runtime);
        session.setEntityGoldenLeavesCount(4);

        session.tickEntities(0, 0x20, 0x30);
        session.tickEntities(1, 0x40, 0x50);
        linksawakening.world.entity.EntityRuntimeTestAccess.setHidingSlimeKeyTransitionCountdownForTest(
                runtime, 0, 0x11);
        session.tickEntities(2, 0x40, 0x50);

        assertEquals(List.of(new SlimeKeyRewardEvent(0, 5, 0xE9)), session.consumeSlimeKeyRewardEvents());
        assertEquals(0x10, session.overworldRoomStatusSnapshot()[0x58] & 0x10);
        assertEquals(List.of(new DialogRequest(0, 0xE9)), session.consumeEntityDialogRequests());
    }

    @Test
    void noSongOcarinaCompletionRequestsTheRomNarratorDialog() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x25);

        assertTrue(session.startOcarina(0x01, 0x00, 0x00));
        session.tickEntities(0);

        assertEquals(List.of(new DialogRequest(0, 0x8E)), session.consumeEntityDialogRequests());
        assertFalse(session.ocarinaPlaying());
    }

    @Test
    void learnedMamboRequestsTheRomManboTransitionInsteadOfACompletionDialog() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x25);

        assertTrue(session.startOcarina(0x01, 0x02, 0x01));
        session.tickEntities(0);

        assertEquals(new Warp(1, 0x00, 0x17, 0x50, 0x7C, -1), session.consumeManboPondTransitionRequest());
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void richardStartsGoldenLeafQuestAndPersistsHisHouseEvent() {
        RoomSession session = newSession();
        session.loadIndoor(0x10, 0xC7);
        RoomEntity richard = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x95)
                .findFirst()
                .orElseThrow();
        int linkX = richard.x() + 0x0F;

        session.tickEntities(0, linkX, richard.y(), 0, Link.DIRECTION_LEFT);
        session.tickEntities(1, linkX, richard.y(), 0, Link.DIRECTION_LEFT);
        assertEquals(0x40, session.consumePendingMusicTrack());
        session.setEntityActionButtonsHeld(true, false);
        session.tickEntities(2, linkX, richard.y(), 0, Link.DIRECTION_LEFT);

        assertEquals(0x13A, session.consumeEntityDialogRequests().getFirst().globalDialogId());
        assertEquals(
                List.of(new RichardProgressEvent(richard.slot(), RichardProgressEvent.Kind.START_QUEST, 0)),
                session.consumeRichardProgressEvents());
        assertEquals(0x10, session.indoorBRoomStatusSnapshot()[0xC7] & 0x10);
        assertEquals(2, session.richardSpokenFlag());
    }

    @Test
    void richardInteractionRecomputesDirectionInsteadOfUsingHisPeriodicFacing() {
        RoomSession session = newSession();
        session.loadIndoor(0x10, 0xC7);
        RoomEntity richard = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x95)
                .findFirst()
                .orElseThrow();

        session.tickEntities(0, richard.x() + 0x0F, richard.y(), 0, Link.DIRECTION_LEFT);
        session.tickEntities(1, richard.x() + 0x0F, richard.y(), 0, Link.DIRECTION_LEFT);
        session.setEntityActionButtonsHeld(true, false);
        session.tickEntities(2, richard.x() - 0x10, richard.y(), 0, Link.DIRECTION_RIGHT);

        assertEquals(0x13A, session.consumeEntityDialogRequests().getFirst().globalDialogId());
    }

    @Test
    void richardUsesTheDialogChoiceIndexForHisQuestAnswer() {
        RoomSession session = newSession();
        session.loadIndoor(0x10, 0xC7);
        RoomEntity richard = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x95)
                .findFirst()
                .orElseThrow();
        int linkX = richard.x() + 0x0F;

        session.tickEntities(0, linkX, richard.y(), 0, Link.DIRECTION_LEFT);
        session.tickEntities(1, linkX, richard.y(), 0, Link.DIRECTION_LEFT);
        session.setEntityActionButtonsHeld(true, false);
        session.tickEntities(2, linkX, richard.y(), 0, Link.DIRECTION_LEFT);
        session.consumeEntityDialogRequests();

        session.setEntityActionButtonsHeld(false, false);
        session.setEntityDialogActive(true);
        session.tickEntities(3, linkX, richard.y(), 0, Link.DIRECTION_LEFT);
        session.setEntityDialogAskSelectionIndex(1);
        session.setEntityDialogActive(false);
        session.tickEntities(4, linkX, richard.y(), 0, Link.DIRECTION_LEFT);

        assertEquals(0x13C, session.consumeEntityDialogRequests().getFirst().globalDialogId());
    }

    @Test
    void onlyLevelTwoBraceletCanLiftStatues() {
        for (int level = 1; level <= 2; level++) {
            RoomSession session = newSession();
            session.loadInitialOverworld(0x00);
            session.loadIndoor(0x05, 0xB2);
            session.tickEntities(0, 0x80, 0x70);
            var statue = session.activeRoom().entities().loadedEntities().stream()
                    .filter(e -> e.type() == 0x9D)
                    .findFirst()
                    .orElseThrow();
            for (int frame = 0; frame < 8; frame++) {
                assertTrue(session.tryLiftObject(
                        statue.x() - 8, statue.y() - 8, Link.DIRECTION_UP, true, 8, false, level));
            }
            assertEquals(level == 2, session.liftedEntityState().active());
        }
    }
}
