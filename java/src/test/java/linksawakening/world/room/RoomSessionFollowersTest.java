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
        session.rooms().loadInitialOverworld(0x92);
        session.story()
                .setFollowingNpcState(new FollowingNpcState(false, 0, true, false, 0, 0, false), 0x40, 0x50, 0, 0, 0);

        assertTrue(session.transitions().startOcarina(0x01, 0x01, 0x00));
        session.entities().tickEntities(0, 0x40, 0x50);

        assertEquals(List.of(new DialogRequest(2, 0x77)), session.entityState().consumeEntityDialogRequests());
    }

    @Test
    void MarinDoesNotHearThatOcarinaDialogInside() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x25);
        session.story()
                .setFollowingNpcState(new FollowingNpcState(false, 0, true, false, 0, 0, false), 0x40, 0x50, 0, 0, 0);

        assertTrue(session.transitions().startOcarina(0x01, 0x01, 0x00));
        session.entities().tickEntities(0, 0x40, 0x50);

        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void shovelRequestsMarinsSourceDialogAfterACompletedDig() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);
        session.story()
                .setFollowingNpcState(new FollowingNpcState(false, 0, true, false, 0, 0, false), 0x44, 0x56, 0, 0, 0);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.rooms().activeRoom().roomObjectsArea()[areaIndex] = 0x04;
        session.rooms().activeRoom().renderValues()[areaIndex] = 0x04;

        assertTrue(session.playerItems().startShovel(0x44, 0x56, 0, false).started());
        session.playerItems().advanceShovel(0x44, 0x56, 0, 0x10);
        session.playerItems().advanceShovel(0x44, 0x56, 0, 0x18);

        assertEquals(List.of(new DialogRequest(0, 0x79)), session.entityState().consumeEntityDialogRequests());
    }

    @Test
    void pitTransitionKeepsRomFollowerAndPickupExceptions() {
        OverworldCollision.GroundInteractionSample pit =
                new OverworldCollision.GroundInteractionSample(0x01, PhysicsFlags.NORMAL_PIT, 0x20, 0x30);
        for (int type : new int[] {0x6D, 0xD5, 0x36}) {
            RoomEntity entity = syntheticEntity(type);
            assertNull(
                    RoomInteractionGeometry.pitTransitionFor(
                            entity, pit, EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE),
                    "type=" + Integer.toHexString(type));
        }

        RoomEntity marin = syntheticEntity(0xC1);
        assertNull(
                RoomInteractionGeometry.pitTransitionFor(marin, pit, RoomSession.LINK_MOTION_FALLING_DOWN),
                "Marin only falls through a well");

        OverworldCollision.GroundInteractionSample well =
                new OverworldCollision.GroundInteractionSample(0x61, PhysicsFlags.SOLID, 0x40, 0x50);
        assertNull(RoomInteractionGeometry.pitTransitionFor(
                marin, well, EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE));
        assertEquals(
                new RoomEntityGroundInteraction.PitTransition(0x48, 0x60),
                RoomInteractionGeometry.pitTransitionFor(marin, well, RoomSession.LINK_MOTION_FALLING_DOWN));
    }

    @Test
    void synchronizesDynamicFollowerSpawningAndFollowerDisplaySelection() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);

        session.story()
                .setFollowingNpcState(
                        new FollowingNpcState(true, 0, false, false, 0, 0, false), 0x50, 0x60, 0x00, 0x00, 0x00);

        RoomEntity rooster = session.rooms().activeRoom().entities().slots().get(15);
        assertEquals(0xD5, rooster.type());
        assertEquals(EntityStatus.ACTIVE, rooster.status());
        assertEquals(0x50, rooster.x());
        assertEquals(0x60, rooster.y());
        assertEquals(0x19, rooster.spriteDefinition().bank());
        assertEquals(0x59BC, rooster.spriteDefinition().address());
        assertEquals(
                0x59BC,
                session.rooms()
                        .activeRoom()
                        .entities()
                        .spriteSelection()
                        .spriteOverrideFor(0xD5)
                        .address());
        assertEquals(rooster, session.rooms().activeRoom().entities().slots().get(15));
    }

    @Test
    void newGameHouseMarinAndTarinUseTheLiveFriendlyPushPath() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x10, 0xA3);

        session.tickEntities(new EntityTickInputs(
                0,
                new LinkPosition(0, 0, 0, 0, 0),
                new LinkMotion(false, 0, false, 0, 0, 0),
                1,
                new SwordBox(false, 0, 0, 0, 0)));
        session.components.entityBridge().outbox.consumeLinkFinalPositionRequests();

        int frame = 1;
        for (int type : new int[] {0x3E, 0x3F}) {
            RoomEntity npc = session.rooms().activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == type)
                    .findFirst()
                    .orElseThrow();
            session.tickEntities(new EntityTickInputs(
                    frame++,
                    new LinkPosition(npc.x(), npc.y(), npc.z(), 0, 0),
                    new LinkMotion(false, 0, false, 0, 0, 0),
                    1,
                    new SwordBox(false, 0, 0, 0, 0)));

            assertTrue(
                    session.components.entityBridge().outbox.consumeLinkFinalPositionRequests().stream()
                            .anyMatch(request -> request.sourceSlot() == npc.slot()),
                    "type=" + Integer.toHexString(type));
        }
    }

    @Test
    void houseMarinUnloadsOnceLinkOwnsTheSword() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x10, 0xA3);
        assertTrue(session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x3E));

        session.story().setChestPlayerLevels(1, 1, 0);
        session.entities().tickEntities(0);

        assertFalse(session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x3E));
    }

    @Test
    void indoorMarinSwordRuleDoesNotUnloadOverworldEntityType3e() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);
        assertTrue(session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x3E));

        session.story().setChestPlayerLevels(1, 1, 0);
        session.entities().tickEntities(0);

        assertTrue(session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x3E));
    }

    @Test
    void kidnappedBowWowMakesTheMabeKidsRunTheirSourceWarningEvent() {
        RoomSession session = newSession();
        session.story().setBowWowState(0x80);
        session.rooms().loadInitialOverworld(0xB0);
        RoomEntity kid71 = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x71)
                .findFirst()
                .orElseThrow();
        RoomEntity kid72 = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x72)
                .findFirst()
                .orElseThrow();

        assertEquals(0x06, kid71.spriteDefinition().bank());
        assertEquals(0x604D, kid71.spriteDefinition().address());
        assertEquals(0x605D, kid72.spriteDefinition().address());

        session.entities().tickEntities(0, 0x10, 0x10);
        session.entities().tickEntities(1, 0x10, 0x10);
        assertEquals(0x0E, session.components.entityBridge().outbox.consumePendingMusicTrack());
        session.entities().tickEntities(2, kid71.x(), kid71.y());

        assertEquals(
                0x220,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());

        session.entityState().setEntityActionButtonsHeld(true);
        session.entities().tickEntities(3, kid71.x(), kid71.y());
        assertEquals(
                0x220,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());
    }

    @Test
    void rescuingBowWowPreservesHisEntitySlotAndPositionDuringDialog() {
        RoomSession session = newSession();
        session.story().setBowWowState(0x80);
        session.rooms().loadIndoor(0x15, 0xE2);
        session.entities().tickEntities(0, 0x20, 0x40, 0, Link.DIRECTION_RIGHT);
        RoomEntity before = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x6D)
                .findFirst()
                .orElseThrow();
        session.tickEntities(new EntityTickInputs(
                1,
                new LinkPosition(0x80, 0x40, 0, Link.DIRECTION_RIGHT, 1),
                new LinkMotion(false, 0, false, 0, 0, 0),
                1,
                new SwordBox(false, 0, 0, 0, 0)));
        assertEquals(1, session.story().bowWowState());
        RoomEntity rescued = session.rooms().activeRoom().entities().slots().get(before.slot());
        session.entityState().setEntityDialogActive(true);
        session.entities().tickEntities(2, 0x80, 0x40, 0, Link.DIRECTION_RIGHT);
        RoomEntity after = session.rooms().activeRoom().entities().slots().get(before.slot());
        assertEquals(0x6D, after.type());
        assertEquals(before.sourceLoadOrder(), after.sourceLoadOrder());
        assertEquals(rescued.x(), after.x());
        assertEquals(rescued.y(), after.y());
    }

    @Test
    void kidnappedBowWowSelectsMadamMeowMeowsSourceQuestDialog() {
        RoomSession session = newSession();
        session.story().setBowWowState(0x80);
        session.rooms().loadIndoor(0x10, 0xA7);
        RoomEntity madam = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x79)
                .findFirst()
                .orElseThrow();

        assertEquals(0x06, madam.spriteDefinition().bank());
        assertEquals(0x5B56, madam.spriteDefinition().address());
        assertEquals(8, madam.spriteDefinition().variantCount());

        int linkY = madam.y() + 0x10;
        session.entities().tickEntities(0, madam.x(), linkY, 0, Link.DIRECTION_UP);
        session.entityState().setEntityActionButtonsHeld(true);
        session.entities().tickEntities(1, madam.x(), linkY, 0, Link.DIRECTION_UP);

        assertEquals(
                0x131,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());
    }

    @Test
    void conchHornOwnerReturnsFollowingBowWowToMadamAndReceivesDelayedFullHeal() {
        RoomSession session = newSession();
        byte[] progress = new byte[0x08];
        progress[1] = 0x02;
        session.dungeon().restoreDungeonProgressFlags(progress);
        session.story().setBowWowState(0x01);
        session.story()
                .setFollowingNpcState(
                        new FollowingNpcState(false, 0, false, true, 0, 0, false),
                        0x50,
                        0x50,
                        0,
                        0,
                        Link.DIRECTION_DOWN);
        session.rooms().loadIndoor(0x10, 0xA7);
        RoomEntity madam = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x79)
                .findFirst()
                .orElseThrow();

        int linkY = madam.y() + 0x10;
        session.entities().tickEntities(0, madam.x(), linkY, 0, Link.DIRECTION_UP);
        session.entityState().setEntityActionButtonsHeld(true);
        session.entities().tickEntities(1, madam.x(), linkY, 0, Link.DIRECTION_UP);

        assertEquals(
                0x12F,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());
        assertEquals(0, session.story().bowWowState());
        assertFalse(session.story().followingNpcState().bowWowFollowing());
        assertEquals(0x10, session.components.roomSessionViews().entityTransitionCountdownForTest(madam.slot()));
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeMadamMeowMeowFullHealRequests()
                .isEmpty());

        session.entityState().setEntityActionButtonsHeld(false);
        for (int frame = 2; frame <= 16; frame++) {
            session.entities().tickEntities(frame, madam.x(), linkY, 0, Link.DIRECTION_UP);
        }

        assertEquals(
                List.of(new MadamMeowMeowFullHealRequest(madam.slot())),
                session.components.entityBridge().outbox.consumeMadamMeowMeowFullHealRequests());
        assertEquals(0, session.components.roomSessionViews().entityTransitionCountdownForTest(madam.slot()));
    }

    @Test
    void followingBowWowStaysWithLinkUntilTheConchHornIsOwned() {
        RoomSession session = newSession();
        session.story().setBowWowState(0x01);
        session.story()
                .setFollowingNpcState(
                        new FollowingNpcState(false, 0, false, true, 0, 0, false),
                        0x50,
                        0x50,
                        0,
                        0,
                        Link.DIRECTION_DOWN);
        session.rooms().loadIndoor(0x10, 0xA7);
        RoomEntity madam = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x79)
                .findFirst()
                .orElseThrow();

        int linkY = madam.y() + 0x10;
        session.entities().tickEntities(0, madam.x(), linkY, 0, Link.DIRECTION_UP);
        session.entityState().setEntityActionButtonsHeld(true);
        session.entities().tickEntities(1, madam.x(), linkY, 0, Link.DIRECTION_UP);

        assertEquals(
                0x132,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());
        assertEquals(1, session.story().bowWowState());
        assertTrue(session.story().followingNpcState().bowWowFollowing());
        assertEquals(0, session.components.roomSessionViews().entityTransitionCountdownForTest(madam.slot()));
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeMadamMeowMeowFullHealRequests()
                .isEmpty());
    }

    @Test
    void richardRejectsBowWowAndDoesNotStartTheGoldenLeafQuest() {
        RoomSession session = newSession();
        session.story().setBowWowState(1);
        session.rooms().loadIndoor(0x10, 0xC7);
        RoomEntity richard = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x95)
                .findFirst()
                .orElseThrow();
        int linkX = richard.x() + 0x0F;

        session.entities().tickEntities(0, linkX, richard.y(), 0, Link.DIRECTION_LEFT);
        session.entities().tickEntities(1, linkX, richard.y(), 0, Link.DIRECTION_LEFT);
        session.entityState().setEntityActionButtonsHeld(true, false);
        session.entities().tickEntities(2, linkX, richard.y(), 0, Link.DIRECTION_LEFT);

        assertEquals(
                0x12D,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeRichardProgressEvents()
                .isEmpty());
        assertEquals(0, session.dungeon().indoorBRoomStatusSnapshot()[0xC7] & 0x10);
    }
}
