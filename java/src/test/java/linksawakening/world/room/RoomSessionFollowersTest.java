package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.Link;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityGroundInteraction;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomSessionFollowersTest {

    @Test
    void MarinHearsTheRomOcarinaDialogOnlyWhenFollowingOutdoors() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);
        session.setFollowingNpcState(new FollowingNpcState(false, 0, true, false, 0, 0, false), 0x40, 0x50, 0, 0, 0);

        assertTrue(session.startOcarina(0x01, 0x01, 0x00));
        session.tickEntities(0, 0x40, 0x50);

        assertEquals(List.of(new DialogRequest(2, 0x77)), session.consumeEntityDialogRequests());
    }

    @Test
    void MarinDoesNotHearThatOcarinaDialogInside() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x25);
        session.setFollowingNpcState(new FollowingNpcState(false, 0, true, false, 0, 0, false), 0x40, 0x50, 0, 0, 0);

        assertTrue(session.startOcarina(0x01, 0x01, 0x00));
        session.tickEntities(0, 0x40, 0x50);

        assertTrue(session.consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void shovelRequestsMarinsSourceDialogAfterACompletedDig() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);
        session.setFollowingNpcState(new FollowingNpcState(false, 0, true, false, 0, 0, false), 0x44, 0x56, 0, 0, 0);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0x04;
        session.activeRoom().renderValues()[areaIndex] = 0x04;

        assertTrue(session.startShovel(0x44, 0x56, 0, false).started());
        session.advanceShovel(0x44, 0x56, 0, 0x10);
        session.advanceShovel(0x44, 0x56, 0, 0x18);

        assertEquals(List.of(new DialogRequest(0, 0x79)), session.consumeEntityDialogRequests());
    }

    @Test
    void pitTransitionKeepsRomFollowerAndPickupExceptions() {
        OverworldCollision.GroundInteractionSample pit =
                new OverworldCollision.GroundInteractionSample(0x01, PhysicsFlags.NORMAL_PIT, 0x20, 0x30);
        for (int type : new int[] {0x6D, 0xD5, 0x36}) {
            RoomEntity entity = syntheticEntity(type);
            assertNull(
                    RoomSession.pitTransitionFor(entity, pit, EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE),
                    "type=" + Integer.toHexString(type));
        }

        RoomEntity marin = syntheticEntity(0xC1);
        assertNull(
                RoomSession.pitTransitionFor(marin, pit, RoomSession.LINK_MOTION_FALLING_DOWN),
                "Marin only falls through a well");

        OverworldCollision.GroundInteractionSample well =
                new OverworldCollision.GroundInteractionSample(0x61, PhysicsFlags.SOLID, 0x40, 0x50);
        assertNull(RoomSession.pitTransitionFor(marin, well, EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE));
        assertEquals(
                new RoomEntityGroundInteraction.PitTransition(0x48, 0x60),
                RoomSession.pitTransitionFor(marin, well, RoomSession.LINK_MOTION_FALLING_DOWN));
    }

    @Test
    void synchronizesDynamicFollowerSpawningAndFollowerDisplaySelection() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        session.setFollowingNpcState(
                new FollowingNpcState(true, 0, false, false, 0, 0, false), 0x50, 0x60, 0x00, 0x00, 0x00);

        RoomEntity rooster = session.activeRoom().entities().slots().get(15);
        assertEquals(0xD5, rooster.type());
        assertEquals(EntityStatus.ACTIVE, rooster.status());
        assertEquals(0x50, rooster.x());
        assertEquals(0x60, rooster.y());
        assertEquals(0x19, rooster.spriteDefinition().bank());
        assertEquals(0x59BC, rooster.spriteDefinition().address());
        assertEquals(
                0x59BC,
                session.activeRoom()
                        .entities()
                        .spriteSelection()
                        .spriteOverrideFor(0xD5)
                        .address());
        assertEquals(rooster, session.activeRoom().entities().slots().get(15));
    }

    @Test
    void newGameHouseMarinAndTarinUseTheLiveFriendlyPushPath() {
        RoomSession session = newSession();
        session.loadIndoor(0x10, 0xA3);

        session.tickEntitiesWithProjectileEvents(0, 0, 0, 0, 0, 0, false, 1, 0);
        session.consumeLinkFinalPositionRequests();

        int frame = 1;
        for (int type : new int[] {0x3E, 0x3F}) {
            RoomEntity npc = session.activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == type)
                    .findFirst()
                    .orElseThrow();
            session.tickEntitiesWithProjectileEvents(frame++, npc.x(), npc.y(), npc.z(), 0, 0, false, 1, 0);

            assertTrue(
                    session.consumeLinkFinalPositionRequests().stream()
                            .anyMatch(request -> request.sourceSlot() == npc.slot()),
                    "type=" + Integer.toHexString(type));
        }
    }

    @Test
    void houseMarinUnloadsOnceLinkOwnsTheSword() {
        RoomSession session = newSession();
        session.loadIndoor(0x10, 0xA3);
        assertTrue(session.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x3E));

        session.setChestPlayerLevels(1, 1, 0);
        session.tickEntities(0);

        assertFalse(
                session.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x3E));
    }

    @Test
    void indoorMarinSwordRuleDoesNotUnloadOverworldEntityType3e() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);
        assertTrue(session.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x3E));

        session.setChestPlayerLevels(1, 1, 0);
        session.tickEntities(0);

        assertTrue(session.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x3E));
    }

    @Test
    void kidnappedBowWowMakesTheMabeKidsRunTheirSourceWarningEvent() {
        RoomSession session = newSession();
        session.setBowWowState(0x80);
        session.loadInitialOverworld(0xB0);
        RoomEntity kid71 = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x71)
                .findFirst()
                .orElseThrow();
        RoomEntity kid72 = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x72)
                .findFirst()
                .orElseThrow();

        assertEquals(0x06, kid71.spriteDefinition().bank());
        assertEquals(0x604D, kid71.spriteDefinition().address());
        assertEquals(0x605D, kid72.spriteDefinition().address());

        session.tickEntities(0, 0x10, 0x10);
        session.tickEntities(1, 0x10, 0x10);
        assertEquals(0x0E, session.consumePendingMusicTrack());
        session.tickEntities(2, kid71.x(), kid71.y());

        assertEquals(0x220, session.consumeEntityDialogRequests().getFirst().globalDialogId());

        session.setEntityActionButtonsHeld(true);
        session.tickEntities(3, kid71.x(), kid71.y());
        assertEquals(0x220, session.consumeEntityDialogRequests().getFirst().globalDialogId());
    }

    @Test
    void rescuingBowWowPreservesHisEntitySlotAndPositionDuringDialog() {
        RoomSession session = newSession();
        session.setBowWowState(0x80);
        session.loadIndoor(0x15, 0xE2);
        session.tickEntities(0, 0x20, 0x40, 0, Link.DIRECTION_RIGHT);
        RoomEntity before = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x6D)
                .findFirst()
                .orElseThrow();
        session.tickEntitiesWithProjectileEvents(
                1, 0x80, 0x40, 0, 0, Link.DIRECTION_RIGHT, 1, false, 1, 0, false, 0, 0, 0, 0, 0, 0);
        assertEquals(1, session.bowWowState());
        RoomEntity rescued = session.activeRoom().entities().slots().get(before.slot());
        session.setEntityDialogActive(true);
        session.tickEntities(2, 0x80, 0x40, 0, Link.DIRECTION_RIGHT);
        RoomEntity after = session.activeRoom().entities().slots().get(before.slot());
        assertEquals(0x6D, after.type());
        assertEquals(before.sourceLoadOrder(), after.sourceLoadOrder());
        assertEquals(rescued.x(), after.x());
        assertEquals(rescued.y(), after.y());
    }

    @Test
    void kidnappedBowWowSelectsMadamMeowMeowsSourceQuestDialog() {
        RoomSession session = newSession();
        session.setBowWowState(0x80);
        session.loadIndoor(0x10, 0xA7);
        RoomEntity madam = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x79)
                .findFirst()
                .orElseThrow();

        assertEquals(0x06, madam.spriteDefinition().bank());
        assertEquals(0x5B56, madam.spriteDefinition().address());
        assertEquals(8, madam.spriteDefinition().variantCount());

        int linkY = madam.y() + 0x10;
        session.tickEntities(0, madam.x(), linkY, 0, Link.DIRECTION_UP);
        session.setEntityActionButtonsHeld(true);
        session.tickEntities(1, madam.x(), linkY, 0, Link.DIRECTION_UP);

        assertEquals(0x131, session.consumeEntityDialogRequests().getFirst().globalDialogId());
    }

    @Test
    void conchHornOwnerReturnsFollowingBowWowToMadamAndReceivesDelayedFullHeal() {
        RoomSession session = newSession();
        byte[] progress = new byte[0x08];
        progress[1] = 0x02;
        session.restoreDungeonProgressFlags(progress);
        session.setBowWowState(0x01);
        session.setFollowingNpcState(
                new FollowingNpcState(false, 0, false, true, 0, 0, false), 0x50, 0x50, 0, 0, Link.DIRECTION_DOWN);
        session.loadIndoor(0x10, 0xA7);
        RoomEntity madam = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x79)
                .findFirst()
                .orElseThrow();

        int linkY = madam.y() + 0x10;
        session.tickEntities(0, madam.x(), linkY, 0, Link.DIRECTION_UP);
        session.setEntityActionButtonsHeld(true);
        session.tickEntities(1, madam.x(), linkY, 0, Link.DIRECTION_UP);

        assertEquals(0x12F, session.consumeEntityDialogRequests().getFirst().globalDialogId());
        assertEquals(0, session.bowWowState());
        assertFalse(session.followingNpcState().bowWowFollowing());
        assertEquals(0x10, session.entityTransitionCountdownForTest(madam.slot()));
        assertTrue(session.consumeMadamMeowMeowFullHealRequests().isEmpty());

        session.setEntityActionButtonsHeld(false);
        for (int frame = 2; frame <= 16; frame++) {
            session.tickEntities(frame, madam.x(), linkY, 0, Link.DIRECTION_UP);
        }

        assertEquals(
                List.of(new MadamMeowMeowFullHealRequest(madam.slot())),
                session.consumeMadamMeowMeowFullHealRequests());
        assertEquals(0, session.entityTransitionCountdownForTest(madam.slot()));
    }

    @Test
    void followingBowWowStaysWithLinkUntilTheConchHornIsOwned() {
        RoomSession session = newSession();
        session.setBowWowState(0x01);
        session.setFollowingNpcState(
                new FollowingNpcState(false, 0, false, true, 0, 0, false), 0x50, 0x50, 0, 0, Link.DIRECTION_DOWN);
        session.loadIndoor(0x10, 0xA7);
        RoomEntity madam = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x79)
                .findFirst()
                .orElseThrow();

        int linkY = madam.y() + 0x10;
        session.tickEntities(0, madam.x(), linkY, 0, Link.DIRECTION_UP);
        session.setEntityActionButtonsHeld(true);
        session.tickEntities(1, madam.x(), linkY, 0, Link.DIRECTION_UP);

        assertEquals(0x132, session.consumeEntityDialogRequests().getFirst().globalDialogId());
        assertEquals(1, session.bowWowState());
        assertTrue(session.followingNpcState().bowWowFollowing());
        assertEquals(0, session.entityTransitionCountdownForTest(madam.slot()));
        assertTrue(session.consumeMadamMeowMeowFullHealRequests().isEmpty());
    }

    @Test
    void richardRejectsBowWowAndDoesNotStartTheGoldenLeafQuest() {
        RoomSession session = newSession();
        session.setBowWowState(1);
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

        assertEquals(0x12D, session.consumeEntityDialogRequests().getFirst().globalDialogId());
        assertTrue(session.consumeRichardProgressEvents().isEmpty());
        assertEquals(0, session.indoorBRoomStatusSnapshot()[0xC7] & 0x10);
    }
}
