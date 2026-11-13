package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RoomRuntimeTest {
    @Test
    void aLoadedGenerationOwnsItsRoomRuntimeAndFreshCollisionScratch() {
        var session = RoomRenderAllocationTest.room();
        var room = session.rooms().activeRoom();
        var entities = session.loadedRoomOrNull().entities;
        var first = new RoomRuntime(
                session.loadedRoomOrNull().contents, entities, session.components.roomRuntimeServices());
        var replacement = new RoomRuntime(
                session.loadedRoomOrNull().contents, entities, session.components.roomRuntimeServices());
        assertSame(room, first.room);
        assertSame(entities, first.entities);
        assertNotSame(first.collisions, replacement.collisions);
        assertNotSame(first.collisions.objectSamples, replacement.collisions.objectSamples);
        assertNotSame(first.collisions.groundResults, replacement.collisions.groundResults);
    }

    @Test
    void absenceCannotBePackagedAsALoadedGeneration() {
        var session = RoomRenderAllocationTest.room();
        assertThrows(
                NullPointerException.class,
                () -> new RoomRuntime(
                        null, session.loadedRoomOrNull().entities, session.components.roomRuntimeServices()));
        assertThrows(
                NullPointerException.class,
                () -> new RoomRuntime(
                        session.loadedRoomOrNull().contents, null, session.components.roomRuntimeServices()));
    }

    @Test
    void facadeKeepsNoRoomDefaultsDistinct() {
        var rom = linksawakening.testing.TestRoms.partial(new byte[0x100000]);
        var session = new RoomSession(new RoomSessionConfig(
                rom,
                new linksawakening.gpu.GPU(),
                new RoomLoader(rom),
                new OverworldTilesetTable(rom),
                new linksawakening.physics.OverworldCollision(linksawakening.rom.RomTables.load(rom)),
                new RoomSessionConfig.Presentation(null, null, null),
                new linksawakening.world.entity.RomRandomByteSource()));
        org.junit.jupiter.api.Assertions.assertFalse(session.hasRoom());
        org.junit.jupiter.api.Assertions.assertNull(session.rooms().activeRoomOrNull());
        org.junit.jupiter.api.Assertions.assertNull(session.rooms().palettesOrNull());
        assertThrows(NullPointerException.class, () -> session.rooms().requireActiveRoom());
        assertThrows(NullPointerException.class, () -> session.rooms().currentRoomId());
        assertThrows(NullPointerException.class, () -> session.rooms().mapId());
        assertThrows(NullPointerException.class, () -> session.rooms().mapCategory());
        assertThrows(NullPointerException.class, () -> session.entityState().entityHealth(0));
        assertEquals(-1, session.rooms().indoorRoomPositionForSave());
        org.junit.jupiter.api.Assertions.assertFalse(session.transitions().startOcarina(1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> session.transitions().startOcarina(-1, 0, 0));
        assertEquals(-1, session.transitions().tickDungeonTitleMessage(false, true, 0, false));
        org.junit.jupiter.api.Assertions.assertNull(session.transitions().consumeManboPondTransitionRequest());
        org.junit.jupiter.api.Assertions.assertNull(session.transitions().consumeInstrumentTransitionRequest());
        org.junit.jupiter.api.Assertions.assertNull(session.transitions().pollStaircaseWarp(0, 0, 0, false));
        org.junit.jupiter.api.Assertions.assertNull(session.rooms().renderViewOrNull());
        org.junit.jupiter.api.Assertions.assertNull(session.rooms().renderSnapshotOrNull());
        org.junit.jupiter.api.Assertions.assertNull(session.rooms().renderView());
        org.junit.jupiter.api.Assertions.assertNull(session.rooms().renderSnapshot());
        for (int i = 0; i < session.dungeon().view().minimapSize(); i++)
            assertEquals(0, session.dungeon().view().minimapStatus(i));
        org.junit.jupiter.api.Assertions.assertFalse(session.entities().inBossBattle());
        org.junit.jupiter.api.Assertions.assertTrue(session.entities()
                .resolveEntityCombat(new EntityCombatInputs(
                        0,
                        0,
                        0,
                        false,
                        0,
                        true,
                        new SwordBox(false, 0, 0, 0, 0),
                        linksawakening.world.entity.EnemyAttackContext.standard()))
                .isEmpty());
        org.junit.jupiter.api.Assertions.assertNull(session.entities()
                .collectEntityIfNeeded(new EntityPickupInputs(
                        0, new EntityPickupInputs.Link(0, 0, false, true, 0, 0), new SwordBox(false, 0, 0, 0, 0))));
        assertEquals(0, session.entityState().clearEntity(-1));
        org.junit.jupiter.api.Assertions.assertFalse(session.playerItems().throwLiftedEntity(null));
        org.junit.jupiter.api.Assertions.assertFalse(session.playerItems().fireBoomerang(0, 0, 0, -1, 0));
        assertEquals(new ArrowShotResult(false, false), session.playerItems().shootArrowResult(0, 0, 0, -1));
        assertEquals(
                new ShovelStartResult(false, true, -1), session.playerItems().startShovel(0, 0, 0, false));
        assertThrows(IllegalArgumentException.class, () -> session.playerItems().startShovel(0, 0, -1, false));
        org.junit.jupiter.api.Assertions.assertFalse(session.entityState().deferContactRecoil(-1, 0, 0));
    }

    @Test
    void installingAReplacementKeepsRetainedRoleObjectsWorking() {
        var session = RoomRenderAllocationTest.room();
        var role = session.entities();
        var first = session.loadedRoomOrNull();
        var replacement = new RoomRuntime(first.contents, first.entities, session.components.roomRuntimeServices());
        session.installRoom(replacement);
        assertSame(replacement, session.loadedRoomOrNull());
        assertSame(role, session.entities());
        org.junit.jupiter.api.Assertions.assertTrue(session.hasRoom());
    }

    @Test
    void cachedInputsStillValidateAndPersistBeforeTheFirstLoad() {
        var rom = linksawakening.testing.TestRoms.partial(new byte[0x100000]);
        var session = new RoomSession(new RoomSessionConfig(
                rom,
                new linksawakening.gpu.GPU(),
                new RoomLoader(rom),
                new OverworldTilesetTable(rom),
                new linksawakening.physics.OverworldCollision(linksawakening.rom.RomTables.load(rom)),
                new RoomSessionConfig.Presentation(null, null, null),
                new linksawakening.world.entity.RomRandomByteSource()));
        session.entityState().setEntityDialogActive(true);
        session.entityState().setEntityTalkState(true, 7, 0x80);
        org.junit.jupiter.api.Assertions.assertTrue(session.components.entityInputs().entityDialogActive);
        assertEquals(7, session.components.entityInputs().entityDialogCooldown);
        assertThrows(IllegalArgumentException.class, () -> session.entityState().setEntityTalkState(false, 256, 0));
        assertEquals(7, session.components.entityInputs().entityDialogCooldown);
        org.junit.jupiter.api.Assertions.assertTrue(session.components.entityInputs().entityInventoryAppearing);
        org.junit.jupiter.api.Assertions.assertFalse(session.hasRoom());
    }

    @Test
    void noRoomTickStillCapturesLinkRegistersBeforeReturningNoEvents() {
        var rom = linksawakening.testing.TestRoms.partial(new byte[0x100000]);
        var session = new RoomSession(new RoomSessionConfig(
                rom,
                new linksawakening.gpu.GPU(),
                new RoomLoader(rom),
                new OverworldTilesetTable(rom),
                new linksawakening.physics.OverworldCollision(linksawakening.rom.RomTables.load(rom)),
                new RoomSessionConfig.Presentation(null, null, null),
                new linksawakening.world.entity.RomRandomByteSource()));
        session.components.location().shouldGetLostInMysteriousWoods = true;
        var result = session.tickEntities(new EntityTickInputs(
                3,
                new LinkPosition(260, -1, 257, 5, 0),
                new LinkMotion(false, 0xE1, false, 0, 0, 0),
                0,
                new SwordBox(false, 0, 0, 0, 0)));
        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
        assertEquals(4, session.components.followers().followingLinkX);
        assertEquals(255, session.components.followers().followingLinkY);
        assertEquals(1, session.components.followers().followingLinkZ);
        assertEquals(5, session.components.followers().followingLinkDirection);
        assertEquals(0xE1, session.components.entityFrame().currentLinkMotionState);
        org.junit.jupiter.api.Assertions.assertFalse(session.components.location().shouldGetLostInMysteriousWoods);
    }

    @Test
    void disabledPresentationHasNoEffectsAndKeepsTheEntityRequestGateOff() {
        var session = RoomRenderAllocationTest.room();
        var effects = session.components.transientVfxSystem();
        org.junit.jupiter.api.Assertions.assertNotNull(session.components.droppableRupeeSystem());
        assertEquals(-1, effects.spawn(linksawakening.vfx.TransientVfxType.POOF, 16, 32));
        assertEquals(0, effects.activeCount());
        org.junit.jupiter.api.Assertions.assertFalse(
                session.components.entityEffects().transientVfxEnabled());
    }
}
