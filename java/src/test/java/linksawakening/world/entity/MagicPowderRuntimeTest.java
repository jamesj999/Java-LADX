package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.MagicPowderObjectAction;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.room.ChestContentsTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@linksawakening.testing.RequiresRom
final class MagicPowderRuntimeTest {

    @Test
    void spawnUsesTheRomSprinkleDisplayAndInitialTransitionTimer() throws IOException {
        RoomEntityRuntime runtime = runtime(false);

        int slot = runtime.spawnMagicPowderSprinkle(0x40, 0x40, 0, 0);

        assertEquals(0x0F, slot);
        RoomEntity sprinkle = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(0x08, sprinkle.type());
        assertEquals(0x4E, sprinkle.x());
        assertEquals(0x40, sprinkle.y());
        assertEquals(0x18, sprinkle.spriteDefinition().bank());
        assertEquals(0x7ABA, sprinkle.spriteDefinition().address());
        assertEquals(
                EntitySpriteDefinition.Shape.RECTANGLE,
                sprinkle.spriteDefinition().shape());
        assertEquals(0xC3, runtime.enemies().physicsFlags(slot));
        assertEquals(0x17, new EntityRoomInspectionQueries(runtime.inspection()).magicPowderTransitionCountdown(slot));

        tick(runtime, 0, 0x20, 0x20, 0);

        assertEquals(0x16, new EntityRoomInspectionQueries(runtime.inspection()).magicPowderTransitionCountdown(slot));
        assertEquals(0x05, runtime.inspection().snapshot().slots().get(slot).spriteVariant());
    }

    @Test
    void outdoorBushRevealUsesTheSourceCellFormulaAndPoofSideEffects() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicPowderSprinkle(0x38, 0x40, 0, 3);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(slot, 0x08);
        runtime.configuration()
                .setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0x5C, 0x01, 0x30, 0x40));

        tick(runtime, 0, 0x20, 0x20, 0);

        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(1, runtime.enemies().magicPowderObjectRequests().size());
        MagicPowderObjectRequest request =
                runtime.enemies().magicPowderObjectRequests().getFirst();
        assertEquals(MagicPowderObjectAction.REVEAL, request.action());
        assertEquals(0x43, request.location());
        assertEquals(0x30, request.objectLeft());
        assertEquals(0x40, request.objectTop());
        assertEquals(
                TransientVfxType.POOF,
                runtime.enemies().transientVfxRequests().getFirst().type());
        assertEquals(0x2F, runtime.consumePendingEntityEvents().getFirst().soundId());
    }

    @Test
    void indoorTorchMovesToStateOneAndQueuesExpirationAfterTheSlowTransition() throws IOException {
        RoomEntityRuntime runtime = runtime(true);
        int slot = runtime.spawnMagicPowderSprinkle(0x48, 0x4C, 0, 3);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(slot, 0x08);
        runtime.configuration()
                .setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0xAB, 0x01, 0x40, 0x40));

        tick(runtime, 0, 0x20, 0x20, 0);

        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).magicPowderState(slot));
        assertEquals(
                0x80, new EntityRoomInspectionQueries(runtime.inspection()).magicPowderSlowTransitionCountdown(slot));
        assertEquals(0x40, runtime.inspection().snapshot().slots().get(slot).x());
        assertEquals(0x40, runtime.inspection().snapshot().slots().get(slot).y());
        assertEquals(
                MagicPowderObjectAction.IGNITE_TORCH,
                runtime.enemies().magicPowderObjectRequests().getFirst().action());

        EntityRuntimeFixture.debug(runtime).setMagicPowderSlowTransitionCountdownForTest(slot, 1);
        tick(runtime, 4, 0x20, 0x20, 0);

        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(
                MagicPowderObjectAction.EXTINGUISH_TORCH,
                runtime.enemies().magicPowderObjectRequests().getFirst().action());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void pausedSprinkleRendersButDoesNotSampleObjects(String gate) throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicPowderSprinkle(0x40, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(slot, 8);
        setGate(runtime, gate, true);
        runtime.configuration().setObjectIntersectionQuery(entity -> {
            throw new AssertionError("Paused powder sampled a room object");
        });
        tick(runtime, 0, 0, 0, 0);
        int expected = gate.equals("got_item") ? 7 : 8;
        assertEquals(
                expected, new EntityRoomInspectionQueries(runtime.inspection()).magicPowderTransitionCountdown(slot));
        assertEquals(
                expected >> 2, runtime.inspection().snapshot().slots().get(slot).spriteVariant());
        assertTrue(runtime.enemies().magicPowderObjectRequests().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dialog", "inventory", "transition", "gameplay", "sequence", "got_item"})
    void torchExpiryWaitsForItsHandlerAndUsesTheGlobalSlowTimerGate(String gate) throws IOException {
        RoomEntityRuntime runtime = runtime(true);
        int slot = runtime.spawnMagicPowderSprinkle(0x48, 0x4C, 0, 3);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(slot, 8);
        runtime.configuration().setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0xAB, 1, 0x40, 0x40));
        tick(runtime, 0, 0, 0, 0);
        EntityRuntimeFixture.debug(runtime).setMagicPowderSlowTransitionCountdownForTest(slot, 1);
        setGate(runtime, gate, true);
        tick(runtime, 4, 0, 0, 0);
        assertTrue(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(
                gate.equals("got_item") ? 0 : 1,
                new EntityRoomInspectionQueries(runtime.inspection()).magicPowderSlowTransitionCountdown(slot));
        assertTrue(runtime.enemies().magicPowderObjectRequests().isEmpty());
        setGate(runtime, gate, false);
        tick(runtime, 8, 0, 0, 6);
        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(
                MagicPowderObjectAction.EXTINGUISH_TORCH,
                runtime.enemies().magicPowderObjectRequests().getFirst().action());
    }

    @Test
    void zeroCountdownIsHiddenButNotUnloadedBeforeTheHandlerResumes() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicPowderSprinkle(0x40, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(slot, 1);
        EntityRuntimeFixture.debug(runtime).setWitchGotItemPresentationActiveForTest(true);
        tick(runtime, 0, 0, 0, 0);
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).magicPowderTransitionCountdown(slot));
        assertTrue(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(-1, runtime.inspection().snapshot().slots().get(slot).spriteVariant());
        EntityRuntimeFixture.debug(runtime).setWitchGotItemPresentationActiveForTest(false);
        tick(runtime, 1, 0, 0, 0);
        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 5, 6})
    void linksMotionStateDoesNotPreventOutdoorPowderEffects(int motionState) throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicPowderSprinkle(0x40, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(slot, 8);
        runtime.configuration().setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0x5C, 1, 0x40, 0x40));
        tick(runtime, 0, 0, 0, motionState);
        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(
                MagicPowderObjectAction.REVEAL,
                runtime.enemies().magicPowderObjectRequests().getFirst().action());
    }

    @ParameterizedTest
    @CsvSource({"2, false, true", "5, false, false", "6, false, true", "0, true, false"})
    void gbcTorchIgnitionChecksOnlyRevolvingDoorMotionAndThePaletteTransition(
            int motionState, boolean paletteTransition, boolean ignite) throws IOException {
        RoomEntityRuntime runtime = runtime(true);
        int slot = runtime.spawnMagicPowderSprinkle(0x48, 0x4C, 0, 3);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(slot, 8);
        runtime.configuration().setTorchPaletteTransitionState(4, paletteTransition);
        runtime.configuration().setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0xAB, 1, 0x40, 0x40));
        tick(runtime, 0, 0, 0, motionState);
        assertEquals(ignite ? 1 : 0, new EntityRoomInspectionQueries(runtime.inspection()).magicPowderState(slot));
        assertEquals(
                ignite ? 1 : 0, runtime.enemies().magicPowderObjectRequests().size());
    }

    @Test
    void bankEighteenDoesNotAllowTheCreditsHandlerException() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicPowderSprinkle(0x40, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(slot, 8);
        runtime.configuration().setObjectIntersectionQuery(entity -> {
            throw new AssertionError("Credits reached the powder object handler");
        });
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0, true));
        assertEquals(7, new EntityRoomInspectionQueries(runtime.inspection()).magicPowderTransitionCountdown(slot));
        assertEquals(1, runtime.inspection().snapshot().slots().get(slot).spriteVariant());
    }

    @ParameterizedTest
    @CsvSource({"12, 0, true", "-12, 0, false", "0, -12, true", "0, 12, false"})
    void enemyCollisionUsesTheRomsAsymmetricByteWindows(int dx, int dy, boolean hits) throws IOException {
        RoomEntityRuntime runtime = runtimeWithAntiFairy(false, 0x40 + dx, 0x50 + dy);
        int slot = runtime.spawnMagicPowderSprinkle(0x32, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(slot, 0x10);
        tick(runtime, 0, 0, 0, 0);
        assertEquals(
                hits,
                runtime.consumePendingEntityEvents().stream()
                        .anyMatch(event -> event.slot() == 0 && event.type() == 0x15));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void objectSamplingTickSkipsEnemyCollisionForNonTorchIndoorObjects(boolean indoor) throws IOException {
        RoomEntityRuntime runtime = runtimeWithAntiFairy(indoor, 0x40, 0x50);
        int slot = runtime.spawnMagicPowderSprinkle(0x32, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(slot, 8);
        runtime.configuration().setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0, 0, 0x30, 0x40));
        tick(runtime, 0, 0, 0, 0);
        assertEquals(
                !indoor,
                runtime.consumePendingEntityEvents().stream()
                        .anyMatch(event -> event.slot() == 0 && event.type() == 0x15));
    }

    @Test
    void torchExpirationBlocksIgnitionLaterInTheSameEntityPass() throws IOException {
        RoomEntityRuntime runtime = runtime(true);
        int expiring = runtime.spawnMagicPowderSprinkle(0x48, 0x4C, 0, 3);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(expiring, 8);
        runtime.configuration()
                .setObjectIntersectionQuery(
                        entity -> new RoomEntityObjectSample(0xAB, 1, entity.x() & 0xF0, (entity.y() - 8) & 0xF0));
        tick(runtime, 0, 0, 0, 0);
        EntityRuntimeFixture.debug(runtime).setMagicPowderSlowTransitionCountdownForTest(expiring, 0);
        runtime.configuration().setTorchPaletteTransitionState(0, false);
        int igniting = runtime.spawnMagicPowderSprinkle(0x68, 0x4C, 0, 3);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(igniting, 8);

        tick(runtime, 1, 0, 0, 0);

        assertFalse(runtime.inspection().snapshot().slots().get(expiring).loaded());
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).magicPowderState(igniting));
        assertEquals(1, runtime.enemies().magicPowderObjectRequests().size());
        assertEquals(
                MagicPowderObjectAction.EXTINGUISH_TORCH,
                runtime.enemies().magicPowderObjectRequests().getFirst().action());
    }

    @Test
    void firstTorchIgnitionBlocksAnotherPaletteTransitionInTheSameEntityPass() throws IOException {
        RoomEntityRuntime runtime = runtime(true);
        int firstSlot = runtime.spawnMagicPowderSprinkle(0x48, 0x4C, 0, 3);
        int secondSlot = runtime.spawnMagicPowderSprinkle(0x68, 0x4C, 0, 3);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(firstSlot, 8);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(secondSlot, 8);
        runtime.configuration().setTorchPaletteTransitionState(8, false);
        runtime.configuration()
                .setObjectIntersectionQuery(
                        entity -> new RoomEntityObjectSample(0xAB, 1, entity.x() & 0xF0, (entity.y() - 8) & 0xF0));
        tick(runtime, 0, 0, 0, 0);
        assertEquals(1, new EntityRoomInspectionQueries(runtime.inspection()).magicPowderState(firstSlot));
        assertEquals(0, new EntityRoomInspectionQueries(runtime.inspection()).magicPowderState(secondSlot));
        assertEquals(1, runtime.enemies().magicPowderObjectRequests().size());
    }

    @Test
    void ocarinaFreezesPowderCountdownWithoutDisablingItsHandler() throws IOException {
        RoomEntityRuntime runtime = runtime(false);
        int slot = runtime.spawnMagicPowderSprinkle(0x40, 0x50, 0, 0);
        EntityRuntimeFixture.debug(runtime).setMagicPowderTransitionCountdownForTest(slot, 7);
        EntityRuntimeFixture.debug(runtime).setOcarinaPlaybackForTest(1, 0, 0);
        runtime.configuration().setObjectIntersectionQuery(entity -> new RoomEntityObjectSample(0x5C, 1, 0x40, 0x40));
        tick(runtime, 0, 0, 0, 0);
        assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
        assertEquals(
                MagicPowderObjectAction.REVEAL,
                runtime.enemies().magicPowderObjectRequests().getFirst().action());
    }

    private static void setGate(RoomEntityRuntime runtime, String gate, boolean active) {
        switch (gate) {
            case "dialog" -> runtime.configuration().setDialogActive(active);
            case "inventory" -> runtime.configuration().setTalkState(active, 0, 0x80);
            case "transition" -> EntityRuntimeFixture.debug(runtime).setRoomTransitionStateForTest(active);
            case "gameplay" -> EntityRuntimeFixture.debug(runtime).setGameplayWorldForTest(!active);
            case "sequence" -> EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(active ? 3 : 4);
            case "got_item" -> EntityRuntimeFixture.debug(runtime).setWitchGotItemPresentationActiveForTest(active);
            default -> throw new IllegalArgumentException(gate);
        }
    }

    private static RoomEntityRuntime runtimeWithAntiFairy(boolean indoor, int x, int y) throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        List<RoomEntity> slots = new ArrayList<>(emptySnapshot().slots());
        slots.set(
                0,
                new RoomEntityBuilder()
                        .type(0x15)
                        .at(x, y)
                        .spriteDefinition(catalog.forEntityType(0x15, EntityRoomLoader.RoomTable.OVERWORLD, -1))
                        .build());
        return RoomEntityRuntime.builder(new RoomEntitySnapshot(slots))
                .indoorRoom(indoor)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .chestContentsTable(new ChestContentsTable(rom))
                .build();
    }

    private static RoomEntityRuntime runtime(boolean indoor) throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        return RoomEntityRuntime.builder(emptySnapshot())
                .indoorRoom(indoor)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .chestContentsTable(new ChestContentsTable(rom))
                .build();
    }

    private static void tick(RoomEntityRuntime runtime, int frame, int linkX, int linkY, int linkMotionState) {
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                frame,
                linkX,
                linkY,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(linkX, linkY, 0, linkMotionState, 0, false)));
    }

    private static RoomEntitySnapshot emptySnapshot() {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        return new RoomEntitySnapshot(slots);
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }
}
