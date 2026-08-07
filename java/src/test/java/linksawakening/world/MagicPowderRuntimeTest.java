package linksawakening.world;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
