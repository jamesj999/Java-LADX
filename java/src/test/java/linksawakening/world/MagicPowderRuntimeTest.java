package linksawakening.world;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MagicPowderRuntimeTest {

    @Test
    void spawnUsesTheRomSprinkleDisplayAndInitialTransitionTimer() throws IOException {
        RoomEntityRuntime runtime = runtime(false);

        int slot = runtime.spawnMagicPowderSprinkle(0x40, 0x40, 0, 0);

        assertEquals(0x0F, slot);
        RoomEntity sprinkle = runtime.snapshot().slots().get(slot);
        assertEquals(0x08, sprinkle.type());
        assertEquals(0x4E, sprinkle.x());
        assertEquals(0x40, sprinkle.y());
        assertEquals(0x18, sprinkle.spriteDefinition().bank());
        assertEquals(0x7ABA, sprinkle.spriteDefinition().address());
        assertEquals(EntitySpriteDefinition.Shape.RECTANGLE,
            sprinkle.spriteDefinition().shape());
        assertEquals(0xC3, runtime.physicsFlags(slot));
        assertEquals(0x17, runtime.magicPowderTransitionCountdown(slot));

        tick(runtime, 0, 0x20, 0x20, 0);

        assertEquals(0x16, runtime.magicPowderTransitionCountdown(slot));
        assertEquals(0x05, runtime.snapshot().slots().get(slot).spriteVariant());
    }

    @Test
    void outdoorBushRevealUsesTheSourceCellFormulaAndPoofSideEffects() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicPowderSprinkle(0x38, 0x40, 0, 3);
        runtime.setMagicPowderTransitionCountdownForTest(slot, 0x08);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            0x5C, 0x01, 0x30, 0x40));

        tick(runtime, 0, 0x20, 0x20, 0);

        assertFalse(runtime.snapshot().slots().get(slot).loaded());
        assertEquals(1, runtime.magicPowderObjectRequests().size());
        RoomEntityRuntime.MagicPowderObjectRequest request =
            runtime.magicPowderObjectRequests().getFirst();
        assertEquals(RoomEntityRuntime.MagicPowderObjectAction.REVEAL,
            request.action());
        assertEquals(0x43, request.location());
        assertEquals(0x30, request.objectLeft());
        assertEquals(0x40, request.objectTop());
        assertEquals(TransientVfxType.POOF, runtime.transientVfxRequests().getFirst().type());
        assertEquals(0x2F, runtime.consumePendingEntityEvents().getFirst().soundId());
    }

    @Test
    void indoorTorchMovesToStateOneAndQueuesExpirationAfterTheSlowTransition() throws IOException {
        RoomEntityRuntime runtime = runtime(true);
        int slot = runtime.spawnMagicPowderSprinkle(0x48, 0x4C, 0, 3);
        runtime.setMagicPowderTransitionCountdownForTest(slot, 0x08);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            0xAB, 0x01, 0x40, 0x40));

        tick(runtime, 0, 0x20, 0x20, 0);

        assertEquals(1, runtime.magicPowderState(slot));
        assertEquals(0x80, runtime.magicPowderSlowTransitionCountdown(slot));
        assertEquals(0x40, runtime.snapshot().slots().get(slot).x());
        assertEquals(0x40, runtime.snapshot().slots().get(slot).y());
        assertEquals(RoomEntityRuntime.MagicPowderObjectAction.IGNITE_TORCH,
            runtime.magicPowderObjectRequests().getFirst().action());

        runtime.setMagicPowderSlowTransitionCountdownForTest(slot, 1);
        tick(runtime, 4, 0x20, 0x20, 0);

        assertFalse(runtime.snapshot().slots().get(slot).loaded());
        assertEquals(RoomEntityRuntime.MagicPowderObjectAction.EXTINGUISH_TORCH,
            runtime.magicPowderObjectRequests().getFirst().action());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void pausedSprinkleRendersButDoesNotSampleObjects(String gate) throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicPowderSprinkle(0x40, 0x50, 0, 0);
        runtime.setMagicPowderTransitionCountdownForTest(slot, 8);
        setGate(runtime, gate, true);
        runtime.setObjectIntersectionQuery(entity -> {
            throw new AssertionError("Paused powder sampled a room object");
        });
        tick(runtime, 0, 0, 0, 0);
        int expected = gate.equals("got_item") ? 7 : 8;
        assertEquals(expected, runtime.magicPowderTransitionCountdown(slot));
        assertEquals(expected >> 2, runtime.snapshot().slots().get(slot).spriteVariant());
        assertTrue(runtime.magicPowderObjectRequests().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void torchExpiryWaitsForItsHandlerAndUsesTheGlobalSlowTimerGate(String gate)
            throws IOException {
        RoomEntityRuntime runtime = runtime(true);
        int slot = runtime.spawnMagicPowderSprinkle(0x48, 0x4C, 0, 3);
        runtime.setMagicPowderTransitionCountdownForTest(slot, 8);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            0xAB, 1, 0x40, 0x40));
        tick(runtime, 0, 0, 0, 0);
        runtime.setMagicPowderSlowTransitionCountdownForTest(slot, 1);
        setGate(runtime, gate, true);
        tick(runtime, 4, 0, 0, 0);
        assertTrue(runtime.snapshot().slots().get(slot).loaded());
        assertEquals(gate.equals("got_item") ? 0 : 1,
            runtime.magicPowderSlowTransitionCountdown(slot));
        assertTrue(runtime.magicPowderObjectRequests().isEmpty());
        setGate(runtime, gate, false);
        tick(runtime, 8, 0, 0, 6);
        assertFalse(runtime.snapshot().slots().get(slot).loaded());
        assertEquals(RoomEntityRuntime.MagicPowderObjectAction.EXTINGUISH_TORCH,
            runtime.magicPowderObjectRequests().getFirst().action());
    }

    @Test
    void zeroCountdownIsHiddenButNotUnloadedBeforeTheHandlerResumes() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicPowderSprinkle(0x40, 0x50, 0, 0);
        runtime.setMagicPowderTransitionCountdownForTest(slot, 1);
        runtime.setWitchGotItemPresentationActiveForTest(true);
        tick(runtime, 0, 0, 0, 0);
        assertEquals(0, runtime.magicPowderTransitionCountdown(slot));
        assertTrue(runtime.snapshot().slots().get(slot).loaded());
        assertEquals(-1, runtime.snapshot().slots().get(slot).spriteVariant());
        runtime.setWitchGotItemPresentationActiveForTest(false);
        tick(runtime, 1, 0, 0, 0);
        assertFalse(runtime.snapshot().slots().get(slot).loaded());
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 5, 6})
    void linksMotionStateDoesNotPreventOutdoorPowderEffects(int motionState)
            throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicPowderSprinkle(0x40, 0x50, 0, 0);
        runtime.setMagicPowderTransitionCountdownForTest(slot, 8);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            0x5C, 1, 0x40, 0x40));
        tick(runtime, 0, 0, 0, motionState);
        assertFalse(runtime.snapshot().slots().get(slot).loaded());
        assertEquals(RoomEntityRuntime.MagicPowderObjectAction.REVEAL,
            runtime.magicPowderObjectRequests().getFirst().action());
    }

    @ParameterizedTest
    @CsvSource({"2, false, true", "5, false, false", "6, false, true",
        "0, true, false"})
    void gbcTorchIgnitionChecksOnlyRevolvingDoorMotionAndThePaletteTransition(
        int motionState, boolean paletteTransition, boolean ignite) throws IOException {
        RoomEntityRuntime runtime = runtime(true);
        int slot = runtime.spawnMagicPowderSprinkle(0x48, 0x4C, 0, 3);
        runtime.setMagicPowderTransitionCountdownForTest(slot, 8);
        runtime.setTorchPaletteTransitionState(4, paletteTransition);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            0xAB, 1, 0x40, 0x40));
        tick(runtime, 0, 0, 0, motionState);
        assertEquals(ignite ? 1 : 0, runtime.magicPowderState(slot));
        assertEquals(ignite ? 1 : 0, runtime.magicPowderObjectRequests().size());
    }

    @Test
    void bankEighteenDoesNotAllowTheCreditsHandlerException() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicPowderSprinkle(0x40, 0x50, 0, 0);
        runtime.setMagicPowderTransitionCountdownForTest(slot, 8);
        runtime.setObjectIntersectionQuery(entity -> {
            throw new AssertionError("Credits reached the powder object handler");
        });
        runtime.tick(0, 0, 0, () -> 0, true);
        assertEquals(7, runtime.magicPowderTransitionCountdown(slot));
        assertEquals(1, runtime.snapshot().slots().get(slot).spriteVariant());
    }

    @ParameterizedTest
    @CsvSource({"12, 0, true", "-12, 0, false", "0, -12, true", "0, 12, false"})
    void enemyCollisionUsesTheRomsAsymmetricByteWindows(int dx, int dy, boolean hits)
            throws IOException {
        RoomEntityRuntime runtime = runtimeWithAntiFairy(false, 0x40 + dx, 0x50 + dy);
        int slot = runtime.spawnMagicPowderSprinkle(0x32, 0x50, 0, 0);
        runtime.setMagicPowderTransitionCountdownForTest(slot, 0x10);
        tick(runtime, 0, 0, 0, 0);
        assertEquals(hits, runtime.consumePendingEntityEvents().stream()
            .anyMatch(event -> event.slot() == 0 && event.type() == 0x15));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void objectSamplingTickSkipsEnemyCollisionForNonTorchIndoorObjects(boolean indoor)
            throws IOException {
        RoomEntityRuntime runtime = runtimeWithAntiFairy(indoor, 0x40, 0x50);
        int slot = runtime.spawnMagicPowderSprinkle(0x32, 0x50, 0, 0);
        runtime.setMagicPowderTransitionCountdownForTest(slot, 8);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            0, 0, 0x30, 0x40));
        tick(runtime, 0, 0, 0, 0);
        assertEquals(!indoor, runtime.consumePendingEntityEvents().stream()
            .anyMatch(event -> event.slot() == 0 && event.type() == 0x15));
    }

    @Test
    void torchExpirationBlocksIgnitionLaterInTheSameEntityPass() throws IOException {
        RoomEntityRuntime runtime = runtime(true);
        int expiring = runtime.spawnMagicPowderSprinkle(0x48, 0x4C, 0, 3);
        runtime.setMagicPowderTransitionCountdownForTest(expiring, 8);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            0xAB, 1, entity.x() & 0xF0, (entity.y() - 8) & 0xF0));
        tick(runtime, 0, 0, 0, 0);
        runtime.setMagicPowderSlowTransitionCountdownForTest(expiring, 0);
        runtime.setTorchPaletteTransitionState(0, false);
        int igniting = runtime.spawnMagicPowderSprinkle(0x68, 0x4C, 0, 3);
        runtime.setMagicPowderTransitionCountdownForTest(igniting, 8);

        tick(runtime, 1, 0, 0, 0);

        assertFalse(runtime.snapshot().slots().get(expiring).loaded());
        assertEquals(0, runtime.magicPowderState(igniting));
        assertEquals(1, runtime.magicPowderObjectRequests().size());
        assertEquals(RoomEntityRuntime.MagicPowderObjectAction.EXTINGUISH_TORCH,
            runtime.magicPowderObjectRequests().getFirst().action());
    }

    @Test
    void firstTorchIgnitionBlocksAnotherPaletteTransitionInTheSameEntityPass()
            throws IOException {
        RoomEntityRuntime runtime = runtime(true);
        int firstSlot = runtime.spawnMagicPowderSprinkle(0x48, 0x4C, 0, 3);
        int secondSlot = runtime.spawnMagicPowderSprinkle(0x68, 0x4C, 0, 3);
        runtime.setMagicPowderTransitionCountdownForTest(firstSlot, 8);
        runtime.setMagicPowderTransitionCountdownForTest(secondSlot, 8);
        runtime.setTorchPaletteTransitionState(8, false);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            0xAB, 1, entity.x() & 0xF0, (entity.y() - 8) & 0xF0));
        tick(runtime, 0, 0, 0, 0);
        assertEquals(1, runtime.magicPowderState(firstSlot));
        assertEquals(0, runtime.magicPowderState(secondSlot));
        assertEquals(1, runtime.magicPowderObjectRequests().size());
    }

    @Test
    void ocarinaFreezesPowderCountdownWithoutDisablingItsHandler() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicPowderSprinkle(0x40, 0x50, 0, 0);
        runtime.setMagicPowderTransitionCountdownForTest(slot, 7);
        runtime.setOcarinaPlaybackForTest(1, 0, 0);
        runtime.setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(
            0x5C, 1, 0x40, 0x40));
        tick(runtime, 0, 0, 0, 0);
        assertFalse(runtime.snapshot().slots().get(slot).loaded());
        assertEquals(RoomEntityRuntime.MagicPowderObjectAction.REVEAL,
            runtime.magicPowderObjectRequests().getFirst().action());
    }

    private static void setGate(RoomEntityRuntime runtime, String gate, boolean active) {
        switch (gate) {
            case "dialog" -> runtime.setDialogActive(active);
            case "inventory" -> runtime.setTalkState(active, 0, 0x80);
            case "transition" -> runtime.setRoomTransitionStateForTest(active);
            case "gameplay" -> runtime.setGameplayWorldForTest(!active);
            case "sequence" -> runtime.setTransitionSequenceCounterForTest(active ? 3 : 4);
            case "got_item" -> runtime.setWitchGotItemPresentationActiveForTest(active);
            default -> throw new IllegalArgumentException(gate);
        }
    }

    private static RoomEntityRuntime runtimeWithAntiFairy(boolean indoor, int x, int y)
            throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        List<RoomEntity> slots = new ArrayList<>(emptySnapshot().slots());
        slots.set(0, new RoomEntity(0, -1, 0x15, x, y, EntityStatus.ACTIVE,
            catalog.forEntityType(0x15, EntityRoomLoader.RoomTable.OVERWORLD, -1), 0));
        return RoomEntityRuntime.from(new RoomEntitySnapshot(slots), indoor, () -> 0,
            catalog, new RomEnemyCombatTables(rom), new ChestContentsTable(rom));
    }

    private static RoomEntityRuntime runtime(boolean indoor) throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        return RoomEntityRuntime.from(emptySnapshot(), indoor, () -> 0, catalog,
            new RomEnemyCombatTables(rom), new ChestContentsTable(rom));
    }

    private static void tick(RoomEntityRuntime runtime, int frame, int linkX, int linkY,
                             int linkMotionState) {
        runtime.tickWithProjectileEvents(frame, linkX, linkY, () -> 0, null,
            new EnemyProjectileCollision.LinkState(linkX, linkY, 0, linkMotionState,
                0, false));
    }

    private static RoomEntitySnapshot emptySnapshot() {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots);
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = MagicPowderRuntimeTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
