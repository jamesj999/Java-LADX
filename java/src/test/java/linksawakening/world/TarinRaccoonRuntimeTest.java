package linksawakening.world;

import linksawakening.dialog.DialogController;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TarinRaccoonRuntimeTest {
    private static final int ROOM_MYSTERIOUS_WOODS = 0x51;
    private static final int ENTITY_TARIN = 0x3F;

    @Test
    void room51LoadsTheOutdoorRaccoonWithItsRomSprite() {
        RoomSession session = newSession();

        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);

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
        completed.restoreRoomStatuses(completedOverworld, new byte[0x100],
            new byte[0x100], new byte[0x20]);

        completed.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);

        assertTrue(hasRaccoon(completed));
        completed.tickEntities(0, 0x50, 0x60, 0, 0);
        assertFalse(hasRaccoon(completed));

        RoomSession live = newSession();
        live.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        live.tickEntities(0, 0x50, 0x60, 0, 0);
        assertTrue(hasRaccoon(live));

        live.restoreRoomStatuses(completedOverworld, new byte[0x100],
            new byte[0x100], new byte[0x20]);
        live.tickEntities(1, 0x50, 0x60, 0, 0);

        assertFalse(hasRaccoon(live));
    }

    @Test
    void normalEntityPassWithoutQualifyingTarinClearsTheLostState() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.tickEntities(0, 0x78, 0x60, 0, 1);
        session.tickEntities(1, 0x78, 0x1F, 0, 1);
        assertTrue(session.shouldGetLostInMysteriousWoods());

        session.loadOverworld(0x52);
        session.tickEntities(2, 0x78, 0x1F, 0, 1);

        assertFalse(session.shouldGetLostInMysteriousWoods());
    }

    @Test
    void crossingTheNorthThresholdSetsLostStateAndQueuesDialog021Once() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.setChestPlayerLevels(1, 1, 0);
        session.tickEntities(0, 0x50, 0x60, 0, 0);

        session.tickEntities(8, 0x78, 0x1F, 0, 0);

        assertTrue(session.shouldGetLostInMysteriousWoods());
        assertEquals(3, raccoon(session).spriteVariant());
        assertEquals(List.of(new RoomEntityRuntime.DialogRequest(0, 0x21)),
            session.consumeEntityDialogRequests());

        session.tickEntities(9, 0x78, 0x1E, 0, 0);

        assertTrue(session.shouldGetLostInMysteriousWoods());
        assertTrue(session.consumeEntityDialogRequests().isEmpty());

        session.tickEntities(0x10, 0x78, 0x30, 0, 0);

        assertFalse(session.shouldGetLostInMysteriousWoods());
        assertEquals(1, raccoon(session).spriteVariant());
    }

    @Test
    void nearbyFacingActionQueuesDialog00dOnlyWhenDialogAndAttackGatesAreClear() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.tickEntities(0, 0x78, 0x50, 0, 1);
        session.setEntityActionButtonsHeld(true, false);
        session.setEntityAttackStepAnimationCountdown(1);

        session.tickEntities(1, 0x78, 0x50, 0, 1);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());

        session.setEntityAttackStepAnimationCountdown(0);
        session.setEntityDialogActive(true);
        session.tickEntities(2, 0x78, 0x50, 0, 1);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());

        session.setEntityDialogActive(false);
        session.tickEntities(3, 0x78, 0x50, 0, 1);

        assertEquals(List.of(new RoomEntityRuntime.DialogRequest(0, 0x0D)),
            session.consumeEntityDialogRequests());
    }

    @Test
    void bOnlyDoesNotQueueDialog00d() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.tickEntities(0, 0x78, 0x50, 0, 1);
        session.setEntityActionButtonsHeld(false, true);

        session.tickEntities(1, 0x78, 0x50, 0, 1);

        assertTrue(session.consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void explicitAirborneLinkDoesNotQueueDialog00dWhenZIsZero() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.tickEntities(0, 0x78, 0x50, 0, 1);
        session.setEntityActionButtonsHeld(true, false);

        session.tickEntities(1, 0x78, 0x50, 0, true, 1);

        assertTrue(session.consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void groundedLinkCanTalkWhenZIsNonzero() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.tickEntities(0, 0x78, 0x50, 0, 1);
        session.setEntityActionButtonsHeld(true, false);

        session.tickEntities(1, 0x78, 0x50, 1, false, 1);

        assertEquals(List.of(new RoomEntityRuntime.DialogRequest(0, 0x0D)),
            session.consumeEntityDialogRequests());
    }

    @Test
    void appearingInventoryDoesNotQueueDialog00d() {
        RoomSession session = actionReadySession();
        session.setEntityTalkState(true, 0, 0x80);

        session.tickEntities(1, 0x78, 0x50, 0, 1);

        assertTrue(session.consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void appearingInventoryDefersDialog021WithoutSuppressingTheLostFlag() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.tickEntities(0, 0x78, 0x60, 0, 1);
        session.setEntityTalkState(true, 0, 0x78);

        session.tickEntities(1, 0x78, 0x1F, 0, 1);

        assertTrue(session.shouldGetLostInMysteriousWoods());
        assertEquals(2, raccoon(session).spriteVariant());
        assertTrue(session.consumeEntityDialogRequests().isEmpty());

        session.setEntityTalkState(false, 0, 0x80);
        session.tickEntities(2, 0x78, 0x1E, 0, 1);

        assertEquals(List.of(new RoomEntityRuntime.DialogRequest(0, 0x21)),
            session.consumeEntityDialogRequests());
    }

    @Test
    void dialogCooldownDoesNotQueueDialog00d() {
        RoomSession session = actionReadySession();
        session.setEntityTalkState(false, 1, 0x80);

        session.tickEntities(1, 0x78, 0x50, 0, 1);

        assertTrue(session.consumeEntityDialogRequests().isEmpty());
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
            session.setEntityTalkState(false, dialog.dialogCooldown(), 0x80);
            session.tickEntities(frame, 0x78, 0x50, 0, 1);
            assertTrue(session.consumeEntityDialogRequests().isEmpty(),
                "held A must remain blocked on entity pass " + frame);
            dialog.tickGameplayFrameCooldown();
        }

        assertEquals(0, dialog.dialogCooldown());
        dialog.tick();
        session.setEntityTalkState(false, dialog.dialogCooldown(), 0x80);
        session.tickEntities(0x19, 0x78, 0x50, 0, 1);

        assertEquals(List.of(new RoomEntityRuntime.DialogRequest(0, 0x0D)),
            session.consumeEntityDialogRequests());
    }

    @Test
    void nonGameplayWindowPositionDoesNotQueueDialog00d() {
        RoomSession session = actionReadySession();
        session.setEntityTalkState(false, 0, 0x70);

        session.tickEntities(1, 0x78, 0x50, 0, 1);

        assertTrue(session.consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void realMagicPowderSprinkleStartsOnlyTheCollisionSliceOnItsRomCadence() {
        RoomEntityRuntime runtime = raccoonRuntime(false);
        runtime.setEnemyHealthForTest(0, 3);
        int powderSlot = runtime.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);

        for (int frame = 0; frame < 8; frame++) {
            tick(runtime, frame, 0x50, 0x60);
        }

        assertEquals(0x0F, powderSlot);
        assertTrue(runtime.consumePendingLinkMotionBlockRequests().isEmpty());
        assertEquals(0, runtime.slowTransitionCountdownForTest(0));
        assertEquals(0, runtime.enemyFlashCountdown(0));

        tick(runtime, 8, 0x50, 0x60);

        assertEquals(List.of(new RoomEntityRuntime.LinkMotionBlockRequest(0)),
            runtime.consumePendingLinkMotionBlockRequests());
        // Powder writes $7F/$10 before Tarin's lower slot receives the
        // runtime's one shared countdown decrement on frame $08.
        assertEquals(0x7E, runtime.slowTransitionCountdownForTest(0));
        assertEquals(0x0F, runtime.enemyFlashCountdown(0));
        assertEquals(3, runtime.enemyHealth(0));

        tick(runtime, 9, 0x50, 0x1F);

        assertFalse(runtime.shouldGetLostInMysteriousWoods(),
            "the collision must have changed Tarin from state zero to state one");
        assertEquals(3, runtime.enemyHealth(0));
    }

    @Test
    void powderWritesExactSourceCountdownsWhenTarinAlreadyRanThisFrame() {
        RoomEntityRuntime runtime = raccoonRuntime(false, 0x0F);
        int powderSlot = runtime.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);

        for (int frame = 0; frame < 7; frame++) {
            tick(runtime, frame, 0x50, 0x60);
        }

        assertEquals(0x0E, powderSlot);
        assertEquals(0, runtime.slowTransitionCountdownForTest(0x0F));
        assertEquals(0, runtime.enemyFlashCountdown(0x0F));

        tick(runtime, 7, 0x50, 0x60);

        assertEquals(0x7F, runtime.slowTransitionCountdownForTest(0x0F));
        assertEquals(0x10, runtime.enemyFlashCountdown(0x0F));
        assertTrue(runtime.consumePendingLinkMotionBlockRequests().isEmpty());

        tick(runtime, 8, 0x50, 0x60);

        assertEquals(0x7E, runtime.slowTransitionCountdownForTest(0x0F));
        assertEquals(0x0F, runtime.enemyFlashCountdown(0x0F));
        assertEquals(List.of(new RoomEntityRuntime.LinkMotionBlockRequest(0x0F)),
            runtime.consumePendingLinkMotionBlockRequests());
    }

    @Test
    void powderCollisionWindowsAreStrictlyLessThanTwelveUnsignedPixels() {
        RoomEntityRuntime xInside = raccoonRuntime(false);
        xInside.spawnMagicPowderSprinkle(0x75, 0x40, 0, 0);
        tickThroughFirstPowderCollision(xInside);
        assertEquals(0x7E, xInside.slowTransitionCountdownForTest(0));

        RoomEntityRuntime xBoundary = raccoonRuntime(false);
        xBoundary.spawnMagicPowderSprinkle(0x76, 0x40, 0, 0);
        tickThroughFirstPowderCollision(xBoundary);
        assertEquals(0, xBoundary.slowTransitionCountdownForTest(0));

        RoomEntityRuntime yInside = raccoonRuntime(false);
        yInside.spawnMagicPowderSprinkle(0x6A, 0x4B, 0, 0);
        tickThroughFirstPowderCollision(yInside);
        assertEquals(0x7E, yInside.slowTransitionCountdownForTest(0));

        RoomEntityRuntime yBoundary = raccoonRuntime(false);
        yBoundary.spawnMagicPowderSprinkle(0x6A, 0x4C, 0, 0);
        tickThroughFirstPowderCollision(yBoundary);
        assertEquals(0, yBoundary.slowTransitionCountdownForTest(0));
    }

    @Test
    void indoorTarinNeverTransformsFromMagicPowder() {
        RoomEntityRuntime runtime = raccoonRuntime(true);
        runtime.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);

        for (int frame = 0; frame <= 8; frame++) {
            tick(runtime, frame, 0x50, 0x60);
        }

        assertTrue(runtime.consumePendingLinkMotionBlockRequests().isEmpty());
        assertEquals(0, runtime.slowTransitionCountdownForTest(0));
        assertEquals(0, runtime.enemyFlashCountdown(0));
    }

    @Test
    void powderAppliesTheRomEligibilityGuardsBeforeTheTarinSpecialCase() {
        RoomEntityRuntime projectileNoclip = raccoonRuntime(false);
        projectileNoclip.setPhysicsFlagsForTest(0, 0x40);
        projectileNoclip.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);
        tickThroughFirstPowderCollision(projectileNoclip);
        assertEquals(0, projectileNoclip.slowTransitionCountdownForTest(0));

        RoomEntityRuntime grabbable = raccoonRuntime(false);
        grabbable.setPhysicsFlagsForTest(0, 0x20);
        grabbable.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);
        tickThroughFirstPowderCollision(grabbable);
        assertEquals(0, grabbable.slowTransitionCountdownForTest(0));

        RoomEntityRuntime hidden = raccoonRuntime(false);
        hidden.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);
        for (int frame = 0; frame < 8; frame++) {
            tick(hidden, frame, 0x50, 0x60);
        }
        hidden.setSpriteVariantForTest(0, -1);
        tick(hidden, 8, 0x50, 0x60);
        assertEquals(0, hidden.slowTransitionCountdownForTest(0));

        RoomEntityRuntime aboveActive = raccoonRuntime(
            false, 0, EntityStatus.STUNNED, 0);
        aboveActive.spawnMagicPowderSprinkle(0x6A, 0x40, 0, 0);
        tickThroughFirstPowderCollision(aboveActive);
        assertEquals(0x7E, aboveActive.slowTransitionCountdownForTest(0));
    }

    private static RoomSession actionReadySession() {
        RoomSession session = newSession();
        session.loadInitialOverworld(ROOM_MYSTERIOUS_WOODS);
        session.tickEntities(0, 0x78, 0x50, 0, 1);
        session.setEntityActionButtonsHeld(true, false);
        return session;
    }

    private static RoomEntity raccoon(RoomSession session) {
        return session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == ENTITY_TARIN)
            .findFirst()
            .orElseThrow();
    }

    private static boolean hasRaccoon(RoomSession session) {
        return session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == ENTITY_TARIN);
    }

    private static RoomSession newSession() {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        return new RoomSession(rom, new GPU(), new RoomLoader(rom),
            new OverworldTilesetTable(rom), new OverworldCollision(romTables),
            new TransientVfxSystem(16), null);
    }

    private static RoomEntityRuntime raccoonRuntime(boolean indoor) {
        return raccoonRuntime(indoor, 0);
    }

    private static RoomEntityRuntime raccoonRuntime(boolean indoor, int tarinSlot) {
        return raccoonRuntime(indoor, tarinSlot, EntityStatus.ACTIVE, 0);
    }

    private static RoomEntityRuntime raccoonRuntime(boolean indoor, int tarinSlot,
                                                     EntityStatus status, int variant) {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        EntitySpriteDefinition definition = catalog.forEntityType(ENTITY_TARIN,
            indoor ? EntityRoomLoader.RoomTable.INDOORS_B
                : EntityRoomLoader.RoomTable.OVERWORLD);
        List<RoomEntity> slots = new java.util.ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        slots.set(tarinSlot, new RoomEntity(tarinSlot, 0, ENTITY_TARIN, 0x78, 0x40,
            status, definition, variant));
        return RoomEntityRuntime.from(new RoomEntitySnapshot(slots), indoor,
            () -> 0, catalog, new RomEnemyCombatTables(rom),
            new ChestContentsTable(rom));
    }

    private static void tickThroughFirstPowderCollision(RoomEntityRuntime runtime) {
        for (int frame = 0; frame <= 8; frame++) {
            tick(runtime, frame, 0x50, 0x60);
        }
    }

    private static void tick(RoomEntityRuntime runtime, int frame, int linkX, int linkY) {
        runtime.tickWithProjectileEvents(frame, linkX, linkY, () -> 0, null,
            new EnemyProjectileCollision.LinkState(linkX, linkY, 0, 0,
                0, false));
    }

    private static byte[] loadRom() {
        try (var stream = TarinRaccoonRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load ROM", exception);
        }
    }
}
