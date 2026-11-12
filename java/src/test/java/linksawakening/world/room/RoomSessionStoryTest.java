package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.state.Direction;
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
            session.dungeon().restoreDungeonItemFlags(flags, new byte[5]);
            session.rooms().loadIndoor(2, 0x54);
            RoomEntity statue = session.rooms().activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x42)
                    .findFirst()
                    .orElseThrow();
            session.entities().tickEntities(0, statue.x(), statue.y() + 0x13, 0, Direction.UP.javaIndex());
            session.entityState().setEntityActionButtonsHeld(true, false);
            session.entities().tickEntities(1, statue.x(), statue.y() + 0x13, 0, Direction.UP.javaIndex());
            assertEquals(
                    List.of(new DialogRequest(hasBeak ? 2 : 1, hasBeak ? 0x82 : 0x10)),
                    session.entityState().consumeEntityDialogRequests());
            RoomEntity updated = session.rooms().activeRoom().entities().loadedEntities().stream()
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
        session.rooms().loadIndoor(2, 0x54);
        int x = 0x38;
        int y = 0x40;
        session.entities().tickEntities(0, x, y + 0x13, 0, Direction.UP.javaIndex());
        session.entityState().setEntityActionButtonsHeld(false, true);
        session.entities().tickEntities(1, x, y + 0x13, 0, Direction.UP.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
        session.entityState().setEntityActionButtonsHeld(true, false);
        session.entities().tickEntities(2, x, y + 0x13, 0, Direction.DOWN.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
        session.entities().tickEntities(3, x, y + 0x14, 0, Direction.UP.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
        session.entityState().setEntityTalkState(false, 1, 0x80);
        session.entities().tickEntities(4, x, y + 0x13, 0, Direction.UP.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
        session.entityState().setEntityTalkState(true, 0, 0x80);
        session.entities().tickEntities(5, x, y + 0x13, 0, Direction.UP.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
        session.entityState().setEntityTalkState(false, 0, 0x70);
        session.entities().tickEntities(6, x, y + 0x13, 0, Direction.UP.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
        session.entityState().setEntityTalkState(false, 0, 0x80);
        session.entityState().setEntityDialogActive(true);
        session.entities().tickEntities(7, x, y + 0x13, 0, Direction.UP.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
        session.entityState().setEntityDialogActive(false);
        session.entities().tickEntities(8, x, y + 0x13, 0, true, Direction.UP.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
        session.entities().tickEntities(9, x + 0x10, y, 0, Direction.LEFT.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
        session.entities().tickEntities(10, x, y + 0x13, 0, Direction.UP.javaIndex());
        assertEquals(
                0x110,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());
    }

    @Test
    void postSwordForestOwlUsesItsRomDialogAndPersistsCompletion() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x80);
        session.story().setChestPlayerLevels(0, 1, 0);

        tickInteractive(session, 0, 0x50, 0x50);
        tickInteractive(session, 1, 0x50, 0x50);
        assertEquals(0x22, session.components.entityBridge().outbox.consumePendingMusicTrack());
        int frame = 2;
        boolean dialogOpened = false;
        while (!dialogOpened && frame < 0x300) {
            tickInteractive(session, frame++, 0x50, 0x50);
            dialogOpened |= session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0C0);
        }
        assertTrue(dialogOpened);
        while ((RoomDebugFixture.of(session).overworldRoomStatusForTest(0x80) & 0x20) == 0 && frame < 0x340) {
            tickInteractive(session, frame++, 0x50, 0x50);
        }
        assertEquals(0x20, RoomDebugFixture.of(session).overworldRoomStatusForTest(0x80) & 0x20);

        boolean restoredMusic = false;
        while (session.rooms().activeRoom().entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x41)
                && frame < 0x500) {
            tickInteractive(session, frame++, 0x50, 0x50);
            restoredMusic |= session.components.entityBridge().outbox.consumePendingMusicTrack() == 0x09;
        }
        assertTrue(restoredMusic);

        session.rooms().loadInitialOverworld(0x80);
        assertFalse(session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x41));
    }

    @Test
    void forestOwlRestoresActivePowerUpMusicWhenDepartureFinishes() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x80);
        session.story().setChestPlayerLevels(0, 1, 0);
        session.components.entityBridge().setEnemyDropPlayerState(3, 3, true);

        int frame = 0;
        boolean restoredPowerMusic = false;
        while (session.rooms().activeRoom().entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x41)
                && frame < 0x500) {
            tickInteractive(session, frame++, 0x50, 0x50);
            restoredPowerMusic |= session.components.entityBridge().outbox.consumePendingMusicTrack() == 0x49;
            session.entityState().consumeEntityDialogRequests();
        }

        assertTrue(restoredPowerMusic);
    }

    @Test
    void fifthKanaletLeafPublishesFinalLeafDialogAndCompletesItsSourceRoom() {
        RoomSession session = newSession();
        session.rooms().loadOverworld(0x58);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(new RoomEntitySnapshot(List.of(
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
                        RoomEntity.disabled(15))))
                .build();
        linksawakening.world.entity.EntityRuntimeTestAccess.setEntityRoomIdForTest(runtime, 0x58);
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0xCC, 0, 0x40, 0x50));
        RoomDebugFixture.of(session).replaceEntityRuntimeForTest(runtime);
        session.components.entityInputs().setEntityGoldenLeavesCount(4);

        session.entities().tickEntities(0, 0x20, 0x30);
        session.entities().tickEntities(1, 0x40, 0x50);
        linksawakening.world.entity.EntityRuntimeTestAccess.setHidingSlimeKeyTransitionCountdownForTest(
                runtime, 0, 0x11);
        session.entities().tickEntities(2, 0x40, 0x50);

        assertEquals(
                List.of(new SlimeKeyRewardEvent(0, 5, 0xE9)),
                session.components.entityBridge().outbox.consumeSlimeKeyRewardEvents());
        assertEquals(0x10, session.dungeon().overworldRoomStatusSnapshot()[0x58] & 0x10);
        assertEquals(List.of(new DialogRequest(0, 0xE9)), session.entityState().consumeEntityDialogRequests());
    }

    @Test
    void noSongOcarinaCompletionRequestsTheRomNarratorDialog() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x25);

        assertTrue(session.transitions().startOcarina(0x01, 0x00, 0x00));
        session.entities().tickEntities(0);

        assertEquals(List.of(new DialogRequest(0, 0x8E)), session.entityState().consumeEntityDialogRequests());
        assertFalse(session.transitions().ocarinaPlaying());
    }

    @Test
    void learnedMamboRequestsTheRomManboTransitionInsteadOfACompletionDialog() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x25);

        assertTrue(session.transitions().startOcarina(0x01, 0x02, 0x01));
        session.entities().tickEntities(0);

        assertEquals(
                new Warp(1, 0x00, 0x17, 0x50, 0x7C, -1), session.transitions().consumeManboPondTransitionRequest());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void richardStartsGoldenLeafQuestAndPersistsHisHouseEvent() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x10, 0xC7);
        RoomEntity richard = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x95)
                .findFirst()
                .orElseThrow();
        int linkX = richard.x() + 0x0F;

        session.entities().tickEntities(0, linkX, richard.y(), 0, Direction.LEFT.javaIndex());
        session.entities().tickEntities(1, linkX, richard.y(), 0, Direction.LEFT.javaIndex());
        assertEquals(0x40, session.components.entityBridge().outbox.consumePendingMusicTrack());
        session.entityState().setEntityActionButtonsHeld(true, false);
        session.entities().tickEntities(2, linkX, richard.y(), 0, Direction.LEFT.javaIndex());

        assertEquals(
                0x13A,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());
        assertEquals(
                List.of(new RichardProgressEvent(richard.slot(), RichardProgressEvent.Kind.START_QUEST, 0)),
                session.components.entityBridge().outbox.consumeRichardProgressEvents());
        assertEquals(0x10, session.dungeon().indoorBRoomStatusSnapshot()[0xC7] & 0x10);
        assertEquals(2, session.story().richardSpokenFlag());
    }

    @Test
    void richardInteractionRecomputesDirectionInsteadOfUsingHisPeriodicFacing() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x10, 0xC7);
        RoomEntity richard = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x95)
                .findFirst()
                .orElseThrow();

        session.entities().tickEntities(0, richard.x() + 0x0F, richard.y(), 0, Direction.LEFT.javaIndex());
        session.entities().tickEntities(1, richard.x() + 0x0F, richard.y(), 0, Direction.LEFT.javaIndex());
        session.entityState().setEntityActionButtonsHeld(true, false);
        session.entities().tickEntities(2, richard.x() - 0x10, richard.y(), 0, Direction.RIGHT.javaIndex());

        assertEquals(
                0x13A,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());
    }

    @Test
    void richardUsesTheDialogChoiceIndexForHisQuestAnswer() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x10, 0xC7);
        RoomEntity richard = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x95)
                .findFirst()
                .orElseThrow();
        int linkX = richard.x() + 0x0F;

        session.entities().tickEntities(0, linkX, richard.y(), 0, Direction.LEFT.javaIndex());
        session.entities().tickEntities(1, linkX, richard.y(), 0, Direction.LEFT.javaIndex());
        session.entityState().setEntityActionButtonsHeld(true, false);
        session.entities().tickEntities(2, linkX, richard.y(), 0, Direction.LEFT.javaIndex());
        session.entityState().consumeEntityDialogRequests();

        session.entityState().setEntityActionButtonsHeld(false, false);
        session.entityState().setEntityDialogActive(true);
        session.entities().tickEntities(3, linkX, richard.y(), 0, Direction.LEFT.javaIndex());
        session.components.entityInputs().setEntityDialogAskSelectionIndex(1);
        session.entityState().setEntityDialogActive(false);
        session.entities().tickEntities(4, linkX, richard.y(), 0, Direction.LEFT.javaIndex());

        assertEquals(
                0x13C,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());
    }

    @Test
    void onlyLevelTwoBraceletCanLiftStatues() {
        for (int level = 1; level <= 2; level++) {
            RoomSession session = newSession();
            session.rooms().loadInitialOverworld(0x00);
            session.rooms().loadIndoor(0x05, 0xB2);
            session.entities().tickEntities(0, 0x80, 0x70);
            var statue = session.rooms().activeRoom().entities().loadedEntities().stream()
                    .filter(e -> e.type() == 0x9D)
                    .findFirst()
                    .orElseThrow();
            for (int frame = 0; frame < 8; frame++) {
                assertTrue(session.doors()
                        .tryLiftObject(
                                statue.x() - 8,
                                statue.y() - 8,
                                Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                                true,
                                8,
                                false,
                                level));
            }
            assertEquals(level == 2, session.rooms().liftedEntityState().active());
        }
    }
}
