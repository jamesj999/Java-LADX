package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.dialog.DialogController;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.testing.TestRoms;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.room.ChestContentsTable;
import linksawakening.world.room.OverworldTilesetTable;
import linksawakening.world.room.RoomLoader;
import linksawakening.world.room.RoomPaletteLoader;
import linksawakening.world.room.RoomSession;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class TarinRaccoonRuntimeTest {
    private static final int ROOM_MYSTERIOUS_WOODS = 0x51;
    private static final int ENTITY_TARIN = 0x3F;

    @Test
    void room51LoadsTheOutdoorRaccoonWithItsRomSprite() {
        RoomSession session = newSession();

        session.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);

        RoomEntity raccoon = raccoon(session);
        assertEquals(0x78, raccoon.x());
        assertEquals(0x40, raccoon.y());
        assertTrue(raccoon.spriteDefinition().supported());
        assertEquals(0x05, raccoon.spriteDefinition().bank());
        assertEquals(0x4912, raccoon.spriteDefinition().address());
    }

    @Test
    void roomStatusEventOneUnloadsTheOutdoorRaccoonOnItsFirstAndLaterLiveTicks() {
        RoomSession completed = newSession();
        byte[] completedOverworld = new byte[0x100];
        completedOverworld[ROOM_MYSTERIOUS_WOODS] = 0x10;
        completed.story().restoreRoomStatuses(completedOverworld, new byte[0x100], new byte[0x100], new byte[0x20]);

        completed.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);

        assertTrue(hasRaccoon(completed));
        completed.entities().tickEntities(0, 0x50, 0x60, 0, 0);
        assertFalse(hasRaccoon(completed));

        RoomSession live = newSession();
        live.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        live.entities().tickEntities(0, 0x50, 0x60, 0, 0);
        assertTrue(hasRaccoon(live));

        live.story().restoreRoomStatuses(completedOverworld, new byte[0x100], new byte[0x100], new byte[0x20]);
        live.entities().tickEntities(1, 0x50, 0x60, 0, 0);

        assertFalse(hasRaccoon(live));
    }

    @Test
    void normalEntityPassWithoutQualifyingTarinClearsTheLostState() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.entities().tickEntities(0, 0x78, 0x60, 0, 1);
        session.entities().tickEntities(1, 0x78, 0x1F, 0, 1);
        assertTrue(session.story().shouldGetLostInMysteriousWoods());

        session.rooms().loadOverworld(0x52);
        session.entities().tickEntities(2, 0x78, 0x1F, 0, 1);

        assertFalse(session.story().shouldGetLostInMysteriousWoods());
    }

    @Test
    void crossingTheNorthThresholdSetsLostStateAndQueuesDialog021Once() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.story().setChestPlayerLevels(1, 1, 0);
        session.entities().tickEntities(0, 0x50, 0x60, 0, 0);

        session.entities().tickEntities(8, 0x78, 0x1F, 0, 0);

        assertTrue(session.story().shouldGetLostInMysteriousWoods());
        assertEquals(3, raccoon(session).spriteVariant());
        assertEquals(List.of(new DialogRequest(0, 0x21)), session.entityState().consumeEntityDialogRequests());

        session.entities().tickEntities(9, 0x78, 0x1E, 0, 0);

        assertTrue(session.story().shouldGetLostInMysteriousWoods());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());

        session.entities().tickEntities(0x10, 0x78, 0x30, 0, 0);

        assertFalse(session.story().shouldGetLostInMysteriousWoods());
        assertEquals(1, raccoon(session).spriteVariant());
    }

    @Test
    void nearbyFacingActionQueuesDialog00dOnlyWhenDialogAndAttackGatesAreClear() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.entities().tickEntities(0, 0x78, 0x50, 0, 1);
        session.entityState().setEntityActionButtonsHeld(true, false);
        session.entityState().setEntityAttackStepAnimationCountdown(1);

        session.entities().tickEntities(1, 0x78, 0x50, 0, 1);
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());

        session.entityState().setEntityAttackStepAnimationCountdown(0);
        session.entityState().setEntityDialogActive(true);
        session.entities().tickEntities(2, 0x78, 0x50, 0, 1);
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());

        session.entityState().setEntityDialogActive(false);
        session.entities().tickEntities(3, 0x78, 0x50, 0, 1);

        assertEquals(List.of(new DialogRequest(0, 0x0D)), session.entityState().consumeEntityDialogRequests());
    }

    @Test
    void bOnlyDoesNotQueueDialog00d() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.entities().tickEntities(0, 0x78, 0x50, 0, 1);
        session.entityState().setEntityActionButtonsHeld(false, true);

        session.entities().tickEntities(1, 0x78, 0x50, 0, 1);

        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void explicitAirborneLinkDoesNotQueueDialog00dWhenZIsZero() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.entities().tickEntities(0, 0x78, 0x50, 0, 1);
        session.entityState().setEntityActionButtonsHeld(true, false);

        session.entities().tickEntities(1, 0x78, 0x50, 0, true, 1);

        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void groundedLinkCanTalkWhenZIsNonzero() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.entities().tickEntities(0, 0x78, 0x50, 0, 1);
        session.entityState().setEntityActionButtonsHeld(true, false);

        session.entities().tickEntities(1, 0x78, 0x50, 1, false, 1);

        assertEquals(List.of(new DialogRequest(0, 0x0D)), session.entityState().consumeEntityDialogRequests());
    }

    @Test
    void appearingInventoryDoesNotQueueDialog00d() {
        RoomSession session = actionReadySession();
        session.entityState().setEntityTalkState(true, 0, 0x80);

        session.entities().tickEntities(1, 0x78, 0x50, 0, 1);

        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void appearingInventoryDefersDialog021WithoutSuppressingTheLostFlag() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.entities().tickEntities(0, 0x78, 0x60, 0, 1);
        session.entityState().setEntityTalkState(true, 0, 0x78);

        session.entities().tickEntities(1, 0x78, 0x1F, 0, 1);

        assertTrue(session.story().shouldGetLostInMysteriousWoods());
        assertEquals(2, raccoon(session).spriteVariant());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());

        session.entityState().setEntityTalkState(false, 0, 0x80);
        session.entities().tickEntities(2, 0x78, 0x1E, 0, 1);

        assertEquals(List.of(new DialogRequest(0, 0x21)), session.entityState().consumeEntityDialogRequests());
    }

    @Test
    void dialogCooldownDoesNotQueueDialog00d() {
        RoomSession session = actionReadySession();
        session.entityState().setEntityTalkState(false, 1, 0x80);

        session.entities().tickEntities(1, 0x78, 0x50, 0, 1);

        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void heldActionIsBlockedForExactlyTwentyFourPostCloseEntityPasses() {
        RoomSession session = actionReadySession();
        DialogController dialog = new DialogController(16);
        dialog.open("A");
        dialog.advance();
        dialog.advance();

        for (int frame = 1; frame <= 0x18; frame++) {
            dialog.tick();
            if (frame == 1) {
                assertEquals(0x18, dialog.dialogCooldown());
            }
            session.entityState().setEntityTalkState(false, dialog.dialogCooldown(), 0x80);
            session.entities().tickEntities(frame, 0x78, 0x50, 0, 1);
            assertTrue(
                    session.entityState().consumeEntityDialogRequests().isEmpty(),
                    "held A must remain blocked on entity pass " + frame);
            dialog.tickGameplayFrameCooldown();
        }

        assertEquals(0, dialog.dialogCooldown());
        dialog.tick();
        session.entityState().setEntityTalkState(false, dialog.dialogCooldown(), 0x80);
        session.entities().tickEntities(0x19, 0x78, 0x50, 0, 1);

        assertEquals(List.of(new DialogRequest(0, 0x0D)), session.entityState().consumeEntityDialogRequests());
    }

    @Test
    void nonGameplayWindowPositionDoesNotQueueDialog00d() {
        RoomSession session = actionReadySession();
        session.entityState().setEntityTalkState(false, 0, 0x70);

        session.entities().tickEntities(1, 0x78, 0x50, 0, 1);

        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void realMagicPowderSprinkleStartsOnlyTheCollisionSliceOnItsRomCadence() {
        RoomEntityRuntime runtime = raccoonRuntime(false);
        EntityRuntimeFixture.debug(runtime).setEnemyHealthForTest(0, 3);
        int powderSlot = runtime.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);

        for (int frame = 0; frame < 8; frame++) {
            tick(runtime, frame, 0x50, 0x60);
        }

        assertEquals(0x0F, powderSlot);
        assertTrue(runtime.consumePendingLinkMotionBlockRequests().isEmpty());
        assertEquals(0, EntityRuntimeFixture.inspection(runtime).slowTransitionCountdownForTest(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));

        tick(runtime, 8, 0x50, 0x60);

        assertEquals(List.of(new LinkMotionBlockRequest(0)), runtime.consumePendingLinkMotionBlockRequests());
        // Powder writes $7F/$10 before Tarin's lower slot receives the
        // runtime's one shared countdown decrement on frame $08.
        assertEquals(0x7E, EntityRuntimeFixture.inspection(runtime).slowTransitionCountdownForTest(0));
        assertEquals(0x0F, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
        assertEquals(3, runtime.enemies().enemyHealth(0));

        tick(runtime, 9, 0x50, 0x1F);

        assertFalse(
                runtime.inspection().shouldGetLostInMysteriousWoods(),
                "the collision must have changed Tarin from state zero to state one");
        assertEquals(3, runtime.enemies().enemyHealth(0));
    }

    @Test
    void powderWritesExactSourceCountdownsWhenTarinAlreadyRanThisFrame() {
        RoomEntityRuntime runtime = raccoonRuntime(false, 0x0F);
        int powderSlot = runtime.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);

        for (int frame = 0; frame < 7; frame++) {
            tick(runtime, frame, 0x50, 0x60);
        }

        assertEquals(0x0E, powderSlot);
        assertEquals(0, EntityRuntimeFixture.inspection(runtime).slowTransitionCountdownForTest(0x0F));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0x0F));

        tick(runtime, 7, 0x50, 0x60);

        assertEquals(0x7F, EntityRuntimeFixture.inspection(runtime).slowTransitionCountdownForTest(0x0F));
        assertEquals(0x10, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0x0F));
        assertTrue(runtime.consumePendingLinkMotionBlockRequests().isEmpty());

        tick(runtime, 8, 0x50, 0x60);

        assertEquals(0x7E, EntityRuntimeFixture.inspection(runtime).slowTransitionCountdownForTest(0x0F));
        assertEquals(0x0F, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0x0F));
        assertEquals(List.of(new LinkMotionBlockRequest(0x0F)), runtime.consumePendingLinkMotionBlockRequests());
    }

    @Test
    void powderCollisionWindowsUseTheRomsAsymmetricByteComparisons() {
        RoomEntityRuntime xInside = raccoonRuntime(false);
        xInside.spawnMagicPowderSprinkle(0x75, 0x40, 0, 0);
        tickThroughFirstPowderCollision(xInside);
        assertEquals(0x7E, EntityRuntimeFixture.inspection(xInside).slowTransitionCountdownForTest(0));

        RoomEntityRuntime xBoundary = raccoonRuntime(false);
        xBoundary.spawnMagicPowderSprinkle(0x76, 0x40, 0, 0);
        tickThroughFirstPowderCollision(xBoundary);
        assertEquals(0, EntityRuntimeFixture.inspection(xBoundary).slowTransitionCountdownForTest(0));

        RoomEntityRuntime xNegativeBoundary = raccoonRuntime(false);
        xNegativeBoundary.spawnMagicPowderSprinkle(0x5E, 0x40, 0, 0);
        tickThroughFirstPowderCollision(xNegativeBoundary);
        assertEquals(0x7E, EntityRuntimeFixture.inspection(xNegativeBoundary).slowTransitionCountdownForTest(0));

        RoomEntityRuntime yInside = raccoonRuntime(false);
        yInside.spawnMagicPowderSprinkle(0x6A, 0x4B, 0, 0);
        tickThroughFirstPowderCollision(yInside);
        assertEquals(0x7E, EntityRuntimeFixture.inspection(yInside).slowTransitionCountdownForTest(0));

        RoomEntityRuntime yBoundary = raccoonRuntime(false);
        yBoundary.spawnMagicPowderSprinkle(0x6A, 0x4C, 0, 0);
        tickThroughFirstPowderCollision(yBoundary);
        // Y uses targetVisualY - powderVisualY + $0C, reversing the
        // subtraction used for X. The positive twelve-pixel edge is inside.
        assertEquals(0x7E, EntityRuntimeFixture.inspection(yBoundary).slowTransitionCountdownForTest(0));

        RoomEntityRuntime yOutside = raccoonRuntime(false);
        yOutside.spawnMagicPowderSprinkle(0x6A, 0x4D, 0, 0);
        tickThroughFirstPowderCollision(yOutside);
        assertEquals(0, EntityRuntimeFixture.inspection(yOutside).slowTransitionCountdownForTest(0));
    }

    @Test
    void indoorTarinNeverTransformsFromMagicPowder() {
        RoomEntityRuntime runtime = raccoonRuntime(true);
        runtime.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);

        for (int frame = 0; frame <= 8; frame++) {
            tick(runtime, frame, 0x50, 0x60);
        }

        assertTrue(runtime.consumePendingLinkMotionBlockRequests().isEmpty());
        assertEquals(0, EntityRuntimeFixture.inspection(runtime).slowTransitionCountdownForTest(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
    }

    @Test
    void powderAppliesTheRomEligibilityGuardsBeforeTheTarinSpecialCase() {
        RoomEntityRuntime projectileNoclip = raccoonRuntime(false);
        EntityRuntimeFixture.debug(projectileNoclip).setPhysicsFlagsForTest(0, 0x40);
        projectileNoclip.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);
        tickThroughFirstPowderCollision(projectileNoclip);
        assertEquals(0, EntityRuntimeFixture.inspection(projectileNoclip).slowTransitionCountdownForTest(0));

        RoomEntityRuntime grabbable = raccoonRuntime(false);
        EntityRuntimeFixture.debug(grabbable).setPhysicsFlagsForTest(0, 0x20);
        grabbable.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);
        tickThroughFirstPowderCollision(grabbable);
        assertEquals(0, EntityRuntimeFixture.inspection(grabbable).slowTransitionCountdownForTest(0));

        RoomEntityRuntime hidden = raccoonRuntime(false);
        hidden.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);
        for (int frame = 0; frame < 8; frame++) {
            tick(hidden, frame, 0x50, 0x60);
        }
        EntityRuntimeFixture.debug(hidden).setSpriteVariantForTest(0, -1);
        tick(hidden, 8, 0x50, 0x60);
        assertEquals(0, EntityRuntimeFixture.inspection(hidden).slowTransitionCountdownForTest(0));

        RoomEntityRuntime aboveActive = raccoonRuntime(false, 0, EntityStatus.STUNNED, 0);
        aboveActive.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);
        tickThroughFirstPowderCollision(aboveActive);
        assertEquals(0x7E, EntityRuntimeFixture.inspection(aboveActive).slowTransitionCountdownForTest(0));
    }

    @Test
    void stateOneExpirySpawnsTheExactTarinBombAndQueuesDeferredPersistence() {
        RoomEntityRuntime runtime = raccoonRuntime(false);
        EntityRuntimeFixture.debug(runtime).setTarinRaccoonStateForTest(0, 1, 0, 0, 0x12, 0, 0, false);
        EntityRuntimeFixture.debug(runtime).setSlowTransitionCountdownForTest(0, 1);
        runtime.configuration().setLinkAttackStepAnimationCountdown(7);

        tick(runtime, 4, 0x50, 0x40);

        RoomEntity tarin = runtime.inspection().snapshot().slots().get(0);
        RoomEntity bomb = runtime.inspection().snapshot().slots().stream()
                .filter(entity -> entity.loaded() && entity.type() == 0x02)
                .findFirst()
                .orElseThrow();
        assertEquals(2, EntityRuntimeFixture.inspection(runtime).tarinRaccoonStateForTest(0));
        assertEquals(9, tarin.spriteVariant());
        assertEquals(tarin.x(), bomb.x());
        assertEquals(tarin.y(), bomb.y());
        assertEquals(tarin.z(), bomb.z());
        assertEquals(0x4C, EntityRuntimeFixture.inspection(runtime).bombPrivateState4ForTest(bomb.slot()));
        assertEquals(0x20, runtime.enemies().transitionCountdown(bomb.slot()));
        assertEquals(List.of(new LinkAttackClearRequest(0)), runtime.consumePendingLinkAttackClearRequests());
        assertEquals(
                List.of(new RoomStatusPersistenceRequest(0, 0x10, 1)),
                runtime.consumePendingRoomStatusPersistenceRequests());
        assertEquals(
                0,
                EntityRuntimeFixture.inspection(runtime).entityRoomStatusForTest() & 0x10,
                "persistence must not truncate live states two and three");
    }

    @Test
    void roomSessionPersistsTransformationWithoutTruncatingLiveTarinThenUnloadsOnReload() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        RoomEntityRuntime runtime = raccoonRuntime(false);
        EntityRuntimeFixture.debug(runtime).setTarinRaccoonStateForTest(0, 1, 0, 0, 0x12, 0, 0, false);
        EntityRuntimeFixture.debug(runtime).setSlowTransitionCountdownForTest(0, 1);
        linksawakening.world.room.RoomSessionTestAccess.replaceEntityRuntime(session, runtime);

        session.entities().tickEntities(4, 0x50, 0x40);

        assertEquals(
                0x10,
                linksawakening.world.room.RoomSessionTestAccess.overworldStatus(session, ROOM_MYSTERIOUS_WOODS) & 0x10);
        assertEquals(1, session.story().tarinFlag());
        assertTrue(hasRaccoon(session), "persisted room status must not be exposed to the live runtime");

        session.entities().tickEntities(5, 0x50, 0x40);
        assertTrue(hasRaccoon(session), "live Tarin must continue through states two and three");

        session.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.entities().tickEntities(6, 0x50, 0x40);

        assertFalse(hasRaccoon(session));
    }

    @Test
    void powderTransformationStaysInsideTheLoadedRoomAndLeavesTarinVisible() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.entities().tickEntities(0, 120, 86, 0, 1);
        assertTrue(session.playerItems().sprinkleMagicPowder(120, 86, 0, 2));
        boolean finished = false;
        for (int frame = 1; frame < 1000 && !finished; frame++) {
            session.entities().tickEntities(frame, 120, 86, 0, 1);
            RoomEntity tarin = raccoon(session);
            assertTrue(
                    tarin.x() < 160 && tarin.y() < 144,
                    "Tarin escaped the room at frame " + frame + ": " + tarin.x() + "," + tarin.y());
            finished = session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(dialog -> dialog.globalDialogId() == 0x00A);
        }
        assertTrue(finished, "Transformation must reach Tarin's recovery dialogue");
        assertEquals(
                0x10,
                linksawakening.world.room.RoomSessionTestAccess.overworldStatus(session, ROOM_MYSTERIOUS_WOODS) & 0x10);
        assertTrue(hasRaccoon(session), "Live Tarin remains until the room reloads");
    }

    @Test
    void newGameWorldInitializationResetsTarinFlag() {
        RoomSession session = newSession();
        session.story().setTarinFlag(2);

        session.story().initializeNewGameWorldState();

        assertEquals(0, session.story().tarinFlag());
    }

    @Test
    void roomSessionThreadsTarinFlagIntoBothMarinHousePalettePaths() {
        byte[] rom = loadRom();
        RoomSession session = newSession();
        session.story().setTarinFlag(2);

        session.rooms().loadIndoor(0x10, 0xA3);

        RoomPaletteLoader palettes = new RoomPaletteLoader((rom == null ? (Rom) null : TestRoms.partial(rom)));
        assertArrayEquals(
                palettes.loadIndoor(0x10, 0xA3, null, 2), session.rooms().palettes());
        assertArrayEquals(
                palettes.loadIndoorObjectPalettes(0x10, 0xA3, false, 2),
                session.rooms().activeRoom().entities().spriteSelection().objectPalettes());
    }

    @Test
    void roomSessionRetainsShieldLevelForBothMarinHousePalettePaths() {
        byte[] rom = loadRom();
        RoomSession session = newSession();
        session.story().setChestPlayerLevels(1, 0, 0);

        session.rooms().loadIndoor(0x10, 0xA3);

        RoomPaletteLoader palettes = new RoomPaletteLoader((rom == null ? (Rom) null : TestRoms.partial(rom)));
        assertArrayEquals(
                palettes.loadIndoor(0x10, 0xA3, null, 0, 1), session.rooms().palettes());
        assertArrayEquals(
                palettes.loadIndoorObjectPalettes(0x10, 0xA3, false, 0, 1),
                session.rooms().activeRoom().entities().spriteSelection().objectPalettes());
    }

    @Test
    void newGameWorldInitializationRestoresTheShieldlessHousePalette() {
        byte[] rom = loadRom();
        RoomSession session = newSession();
        session.story().setChestPlayerLevels(1, 0, 0);

        session.story().initializeNewGameWorldState();
        session.rooms().loadIndoor(0x10, 0xA3);

        RoomPaletteLoader palettes = new RoomPaletteLoader((rom == null ? (Rom) null : TestRoms.partial(rom)));
        assertArrayEquals(
                palettes.loadIndoor(0x10, 0xA3, null, 0, 0), session.rooms().palettes());
        assertArrayEquals(
                palettes.loadIndoorObjectPalettes(0x10, 0xA3, false, 0, 0),
                session.rooms().activeRoom().entities().spriteSelection().objectPalettes());
    }

    @Test
    void stateTwoLandingAndStateThreeDialogUseRuntimeTimerOrdering() {
        RoomEntityRuntime runtime = raccoonRuntime(false);
        runtime.configuration().setWitchEnvironment(0, 0, 0x0C);
        EntityRuntimeFixture.debug(runtime).setTarinRaccoonStateForTest(0, 2, 0, 0, 0xF0, 0, 0, false);

        tick(runtime, 0, 0x79, 0x40);

        assertEquals(3, EntityRuntimeFixture.inspection(runtime).tarinRaccoonStateForTest(0));
        assertEquals(0x40, runtime.enemies().transitionCountdown(0));
        assertEquals(8, runtime.inspection().snapshot().slots().get(0).spriteVariant());
        assertEquals(0x23, runtime.consumePendingEntityEvents().get(0).soundId());
        assertEquals(0x0C, runtime.consumePendingMusicTrack());

        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(0, 2);
        tick(runtime, 1, 0x70, 0x60);

        assertEquals(List.of(new DialogRequest(0, 0x0A)), runtime.consumePendingDialogRequests());
        assertTrue(runtime.consumePendingLinkMotionBlockRequests().isEmpty());
    }

    @Test
    void ocarinaFreezesTransformationTimersButNotTheTarinHandler() {
        RoomEntityRuntime runtime = raccoonRuntime(false);
        EntityRuntimeFixture.debug(runtime).setTarinRaccoonStateForTest(0, 1, 0, 0, 0, 0, 0, false);
        EntityRuntimeFixture.debug(runtime).setSlowTransitionCountdownForTest(0, 4);
        EntityRuntimeFixture.debug(runtime).setOcarinaPlaybackForTest(0x20, 1, 0);

        tick(runtime, 4, 0x50, 0x40);

        assertEquals(4, EntityRuntimeFixture.inspection(runtime).slowTransitionCountdownForTest(0));
        assertEquals(List.of(new LinkMotionBlockRequest(0)), runtime.consumePendingLinkMotionBlockRequests());
        assertEquals(List.of(new LinkAttackClearRequest(0)), runtime.consumePendingLinkAttackClearRequests());
        assertEquals(List.of(new LinkFacingRequest(0, 0, true)), runtime.consumePendingLinkFacingRequests());
    }

    @Test
    void roomSessionExposesTheLiveTarinAttackClearRequest() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        RoomEntityRuntime runtime = raccoonRuntime(false);
        EntityRuntimeFixture.debug(runtime).setTarinRaccoonStateForTest(0, 1, 0, 0, 0, 0, 0, false);
        linksawakening.world.room.RoomSessionTestAccess.replaceEntityRuntime(session, runtime);

        session.entities().tickEntities(0, 0x50, 0x60, 0, 0);

        assertFalse(session.entityState().consumeLinkAttackClearRequests().isEmpty());
    }

    @Test
    void tarinTransformationBombCannotEnterTheEquippedBombLiftPath() {
        RoomEntityRuntime runtime = raccoonRuntime(false);
        EntityRuntimeFixture.debug(runtime).setTarinRaccoonStateForTest(0, 1, 0, 0, 0x12, 0, 0, false);
        EntityRuntimeFixture.debug(runtime).setSlowTransitionCountdownForTest(0, 1);
        tick(runtime, 4, 0x50, 0x40);
        RoomEntity bomb = runtime.inspection().snapshot().slots().stream()
                .filter(entity -> entity.loaded() && entity.type() == 0x02)
                .findFirst()
                .orElseThrow();
        EntityRuntimeFixture.debug(runtime).setBombTransitionCountdownForTest(bomb.slot(), 0xA0);
        runtime.configuration().setBombButtonHeld(true);

        tick(runtime, 5, bomb.x(), bomb.y());

        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(bomb.slot()).status());
        assertEquals(0x4C, EntityRuntimeFixture.inspection(runtime).bombPrivateState4ForTest(bomb.slot()));
    }

    @Test
    void noninteractivePassFreezesTarinTimersAndTransformationHandler() {
        RoomEntityRuntime runtime = raccoonRuntime(false);
        EntityRuntimeFixture.debug(runtime).setTarinRaccoonStateForTest(0, 1, 0, 0, 0, 0, 0, false);
        EntityRuntimeFixture.debug(runtime).setSlowTransitionCountdownForTest(0, 4);
        runtime.configuration().setDialogActive(true);

        tick(runtime, 4, 0x50, 0x40);

        assertEquals(4, EntityRuntimeFixture.inspection(runtime).slowTransitionCountdownForTest(0));
        assertEquals(1, EntityRuntimeFixture.inspection(runtime).tarinRaccoonStateForTest(0));
        assertTrue(runtime.consumePendingLinkMotionBlockRequests().isEmpty());

        runtime.configuration().setDialogActive(false);
        EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(3);
        tick(runtime, 8, 0x50, 0x40);

        assertEquals(4, EntityRuntimeFixture.inspection(runtime).slowTransitionCountdownForTest(0));
        assertEquals(1, EntityRuntimeFixture.inspection(runtime).tarinRaccoonStateForTest(0));
    }

    private static RoomSession actionReadySession() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.entities().tickEntities(0, 0x78, 0x50, 0, 1);
        session.entityState().setEntityActionButtonsHeld(true, false);
        return session;
    }

    private static RoomEntity raccoon(RoomSession session) {
        return session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == ENTITY_TARIN)
                .findFirst()
                .orElseThrow();
    }

    private static boolean hasRaccoon(RoomSession session) {
        return session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == ENTITY_TARIN);
    }

    private static RoomSession newSession() {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.world.entity.EntityRomTestData.view(rom));
        return new RoomSession(new linksawakening.world.room.RoomSessionConfig(
                (rom == null ? (Rom) null : TestRoms.partial(rom)),
                new GPU(),
                new RoomLoader((rom == null ? (Rom) null : TestRoms.partial(rom))),
                new OverworldTilesetTable((rom == null ? (Rom) null : TestRoms.partial(rom))),
                new OverworldCollision(romTables),
                new linksawakening.world.room.RoomSessionConfig.Presentation(new TransientVfxSystem(16), null, null),
                new linksawakening.world.entity.RomRandomByteSource()));
    }

    private static RoomEntityRuntime raccoonRuntime(boolean indoor) {
        return raccoonRuntime(indoor, 0);
    }

    private static RoomEntityRuntime raccoonRuntime(boolean indoor, int tarinSlot) {
        return raccoonRuntime(indoor, tarinSlot, EntityStatus.ACTIVE, 0);
    }

    private static RoomEntityRuntime raccoonRuntime(boolean indoor, int tarinSlot, EntityStatus status, int variant) {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        EntitySpriteDefinition definition = catalog.forEntityType(
                ENTITY_TARIN, indoor ? EntityRoomLoader.RoomTable.INDOORS_B : EntityRoomLoader.RoomTable.OVERWORLD);
        List<RoomEntity> slots = new java.util.ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        slots.set(
                tarinSlot,
                new RoomEntityBuilder()
                        .slot(tarinSlot)
                        .sourceLoadOrder(0)
                        .type(ENTITY_TARIN)
                        .at(0x78, 0x40)
                        .status(status)
                        .spriteDefinition(definition)
                        .spriteVariant(variant)
                        .build());
        return RoomEntityRuntime.builder(new RoomEntitySnapshot(slots))
                .indoorRoom(indoor)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom)))
                .chestContentsTable(new ChestContentsTable((rom == null ? (Rom) null : TestRoms.partial(rom))))
                .build();
    }

    private static void tickThroughFirstPowderCollision(RoomEntityRuntime runtime) {
        for (int frame = 0; frame <= 8; frame++) {
            tick(runtime, frame, 0x50, 0x60);
        }
    }

    private static void tick(RoomEntityRuntime runtime, int frame, int linkX, int linkY) {
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                frame,
                linkX,
                linkY,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(linkX, linkY, 0, 0, 0, false)));
    }

    private static byte[] loadRom() {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load ROM", exception);
        }
    }
}
