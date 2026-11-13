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
    void combatBoundaryKeepsNoRoomDefaultsDistinct() {
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
}
