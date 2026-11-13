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
        var entities = session.components.entityFrame().runtime;
        var first = new RoomRuntime(room, entities);
        var replacement = new RoomRuntime(room, entities);
        assertSame(room, first.room);
        assertSame(entities, first.entities);
        assertNotSame(first.collisions, replacement.collisions);
        assertNotSame(first.collisions.objectSamples, replacement.collisions.objectSamples);
        assertNotSame(first.collisions.groundResults, replacement.collisions.groundResults);
    }

    @Test
    void absenceCannotBePackagedAsALoadedGeneration() {
        var session = RoomRenderAllocationTest.room();
        assertThrows(NullPointerException.class, () -> new RoomRuntime(null, session.components.entityFrame().runtime));
        assertThrows(
                NullPointerException.class,
                () -> new RoomRuntime(session.rooms().activeRoom(), null));
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
        var replacement = new RoomRuntime(first.room, first.entities);
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
}
