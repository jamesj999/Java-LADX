package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import linksawakening.vfx.TransientVfxSystem;
import org.junit.jupiter.api.Test;

class RoomRuntimeTest {
    @Test
    void handleDefaultsDoNotEvaluateLoadedOperations() {
        var handle = new LoadedRoomHandle();
        assertEquals(List.of(), handle.drain(room -> {
            throw new AssertionError("unloaded drain");
        }));
        org.junit.jupiter.api.Assertions.assertFalse(handle.test(room -> {
            throw new AssertionError("unloaded query");
        }));
        assertEquals(-1, handle.intOr(-1, room -> {
            throw new AssertionError("unloaded integer query");
        }));
        var absent = new Object();
        assertSame(absent, handle.readOr(absent, room -> {
            throw new AssertionError("unloaded value query");
        }));
        handle.updateInt(255, (room, value) -> {
            throw new AssertionError("unloaded update");
        });
        handle.updateInts(255, 254, (room, first, second) -> {
            throw new AssertionError("unloaded pair update");
        });
        handle.updateFlagInt(true, 255, (room, flag, value) -> {
            throw new AssertionError("unloaded item update");
        });
    }

    @Test
    void loadedOperationBorrowsOneGenerationAndDoesNotConstructAnAbsentResult() {
        var session = RoomRenderAllocationTest.room();
        var room = RoomSessionInspection.loadedRoom(session);
        var handle = RoomSessionInspection.component(session, LoadedRoomHandle.class);
        var result = handle.readOrGet(
                () -> {
                    throw new AssertionError("loaded fallback");
                },
                loaded -> {
                    assertSame(room, loaded);
                    handle.clear();
                    return loaded;
                });
        assertSame(room, result);
        org.junit.jupiter.api.Assertions.assertFalse(handle.hasRoom());
        var first = handle.readOrGet(Object::new, loaded -> loaded);
        var second = handle.readOrGet(Object::new, loaded -> loaded);
        assertNotSame(first, second);
    }

    @Test
    void aLoadedGenerationOwnsItsRoomRuntimeAndFreshCollisionScratch() {
        var session = RoomRenderAllocationTest.room();
        var room = session.rooms().activeRoomOrNull();
        var entities = RoomSessionInspection.loadedRoom(session).entities;
        var first = new RoomRuntime(
                RoomSessionInspection.loadedRoom(session).contents,
                entities,
                RoomSessionInspection.component(session, RoomRuntime.Services.class));
        var replacement = new RoomRuntime(
                RoomSessionInspection.loadedRoom(session).contents,
                entities,
                RoomSessionInspection.component(session, RoomRuntime.Services.class));
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
                        null,
                        RoomSessionInspection.loadedRoom(session).entities,
                        RoomSessionInspection.component(session, RoomRuntime.Services.class)));
        assertThrows(
                NullPointerException.class,
                () -> new RoomRuntime(
                        RoomSessionInspection.loadedRoom(session).contents,
                        null,
                        RoomSessionInspection.component(session, RoomRuntime.Services.class)));
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
        org.junit.jupiter.api.Assertions.assertNull(session.rooms().renderViewOrNull());
        org.junit.jupiter.api.Assertions.assertNull(session.rooms().renderSnapshotOrNull());
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
        var first = RoomSessionInspection.loadedRoom(session);
        var replacement = new RoomRuntime(
                first.contents, first.entities, RoomSessionInspection.component(session, RoomRuntime.Services.class));
        session.installRoom(replacement);
        assertSame(replacement, RoomSessionInspection.loadedRoom(session));
        assertSame(role, session.entities());
        org.junit.jupiter.api.Assertions.assertTrue(session.hasRoom());
    }

    @Test
    void replacementCombatPolicyUsesOnlyTheNewGenerationsEntityOwners() {
        var session = RoomRenderAllocationTest.room();
        var original = RoomSessionInspection.loadedRoom(session);
        var replacementEntities = new linksawakening.world.entity.RoomEntityRuntimeBuilder(
                        original.entities.inspection().snapshot())
                .build();
        var replacement = new RoomRuntime(
                original.contents,
                replacementEntities,
                RoomSessionInspection.component(session, RoomRuntime.Services.class));
        session.installRoom(replacement);
        session.entityState().clearEntity(0);
        org.junit.jupiter.api.Assertions.assertTrue(
                original.entities.inspection().view().slots().get(0).loaded());
        org.junit.jupiter.api.Assertions.assertFalse(
                replacementEntities.inspection().view().slots().get(0).loaded());
        org.junit.jupiter.api.Assertions.assertFalse(
                session.rooms().requireActiveRoom().entityView().slots().get(0).loaded());
        assertNotSame(original.interactions, replacement.interactions);
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
        org.junit.jupiter.api.Assertions.assertTrue(
                RoomSessionInspection.component(session, RoomEntityInputs.class).entityDialogActive);
        assertEquals(7, RoomSessionInspection.component(session, RoomEntityInputs.class).entityDialogCooldown);
        assertThrows(IllegalArgumentException.class, () -> session.entityState().setEntityTalkState(false, 256, 0));
        assertEquals(7, RoomSessionInspection.component(session, RoomEntityInputs.class).entityDialogCooldown);
        org.junit.jupiter.api.Assertions.assertTrue(
                RoomSessionInspection.component(session, RoomEntityInputs.class).entityInventoryAppearing);
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
        RoomSessionInspection.component(session, RoomLocationState.class).shouldGetLostInMysteriousWoods = true;
        var result = session.tickEntities(new EntityTickInputs(
                3,
                new LinkPosition(260, -1, 257, 5, 0),
                new LinkMotion(false, 0xE1, false, 0, 0, 0),
                0,
                new SwordBox(false, 0, 0, 0, 0)));
        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
        assertEquals(4, RoomSessionInspection.component(session, RoomFollowerState.class).followingLinkX);
        assertEquals(255, RoomSessionInspection.component(session, RoomFollowerState.class).followingLinkY);
        assertEquals(1, RoomSessionInspection.component(session, RoomFollowerState.class).followingLinkZ);
        assertEquals(5, RoomSessionInspection.component(session, RoomFollowerState.class).followingLinkDirection);
        assertEquals(
                0xE1, RoomSessionInspection.component(session, RoomEntitySessionState.class).currentLinkMotionState);
        org.junit.jupiter.api.Assertions.assertFalse(
                RoomSessionInspection.component(session, RoomLocationState.class).shouldGetLostInMysteriousWoods);
    }

    @Test
    void disabledPresentationHasNoEffectsAndKeepsTheEntityRequestGateOff() {
        var session = RoomRenderAllocationTest.room();
        var effects = RoomSessionInspection.component(session, TransientVfxSystem.class);
        org.junit.jupiter.api.Assertions.assertNotNull(
                RoomSessionInspection.component(session, DroppableRupeeSystem.class));
        assertEquals(-1, effects.spawn(linksawakening.vfx.TransientVfxType.POOF, 16, 32));
        assertEquals(0, effects.activeCount());
        org.junit.jupiter.api.Assertions.assertFalse(
                RoomSessionInspection.component(session, RoomEntitySimulation.EffectsDependencies.class)
                        .transientVfxEnabled());
    }
}
