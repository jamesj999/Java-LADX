package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeLiftThrowTest extends EntityRuntimeTestSupport {

    @Test
    void villageChildrenThrowABallAndRetireItsSlot() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntity kid = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x72)
                .at(0x70, 0x40)
                .spriteDefinition(catalog.forEntityType(0x72, EntityRoomLoader.RoomTable.OVERWORLD))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(kid))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(new RomEnemyCombatTables(rom))
                .build();
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x20, 0x70, () -> 0));
        assertTrue(runtime.inspection().snapshot().slots().stream()
                .anyMatch(e -> e.loaded()
                        && e.sourceLoadOrder() == -1
                        && e.spriteDefinition().address() == 0x607D));
        for (int frame = 1; frame <= 40; frame++)
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x20, 0x70, () -> 0));
        assertFalse(runtime.inspection().snapshot().slots().stream()
                .anyMatch(e -> e.loaded()
                        && e.sourceLoadOrder() == -1
                        && e.spriteDefinition().address() == 0x607D));
    }

    @Test
    void powerUpPickupUsesAFinitePresentationRatherThanTheCarryState() {
        for (int type : new int[] {0x33, 0x34}) {
            EntitySpriteDefinition definition = pairDefinition(type, 2);
            RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                    .sourceLoadOrder(0)
                    .type(type)
                    .at(24, 32)
                    .spriteDefinition(definition)
                    .build());
            RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

            EntityPickupEvent pickup = runtime.collectIfNeeded(1, 24, 34, false, true);

            assertNotNull(pickup);
            assertEquals(1, pickup.persistentClearMask());
            assertEquals(
                    EntityStatus.ACTIVE,
                    runtime.inspection().snapshot().slots().get(0).status());
            assertFalse(runtime.inspection().liftedEntityState().active());
            assertNull(runtime.collectIfNeeded(1, 24, 34, false, true));
            for (int frame = 0; frame < 46; frame++) runtime.tick(EntityTestFrames.frame(runtime, frame));
            assertEquals(
                    List.of(new DialogRequest(0, type == 0x33 ? 0x08 : 0xEC)), runtime.consumePendingDialogRequests());
            runtime.configuration().setDialogActive(true);
            for (int frame = 46; frame < 246; frame++) runtime.tick(EntityTestFrames.frame(runtime, frame));
            assertTrue(runtime.inspection().powerUpPickupSequenceActive());
            assertFalse(runtime.inspection().liftedEntityState().active());
            assertEquals(
                    List.of(new LinkHeldItemPoseRequest(0, true)), runtime.consumePendingLinkHeldItemPoseRequests());
            runtime.configuration().setDialogActive(false);
            runtime.tick(EntityTestFrames.frame(runtime, 246));
            assertFalse(runtime.inspection().powerUpPickupSequenceActive());
            assertEquals(
                    EntityStatus.DISABLED,
                    runtime.inspection().snapshot().slots().get(0).status());
        }
    }

    @Test
    void liftableRockSmashUsesTheRomCountdownDisplayFramesAndNoise() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        int slot = runtime.spawnLiftableRockSmash(0x58, 0x60, 0x01);

        RoomEntity initial = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(EntityStatus.ACTIVE, initial.status());
        assertEquals(0x05, initial.type());
        assertEquals(0x58, initial.x());
        assertEquals(0x60, initial.y());
        assertEquals(0x06, initial.spriteVariant());
        assertEquals(
                EntitySpriteDefinition.Shape.DYNAMIC, initial.spriteDefinition().shape());
        assertEquals(0xD4, runtime.enemies().physicsFlags(slot));
        assertEquals(0x0A, runtime.options1(slot));
        assertEquals(
                0x05,
                runtime.consumePendingEntityEvents().stream()
                        .filter(event -> event.type() == 0x05)
                        .findFirst()
                        .orElseThrow()
                        .soundId());

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertEquals(0x06, runtime.inspection().snapshot().slots().get(slot).spriteVariant());
        runtime.tick(EntityTestFrames.frame(runtime, 1, 0, 0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0, 0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 3, 0, 0, () -> 0));
        assertEquals(0x07, runtime.inspection().snapshot().slots().get(slot).spriteVariant());

        for (int frame = 4; frame <= 29; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0, 0, () -> 0));
        }
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(slot).status());
    }

    @Test
    void liftableRockRubbleUsesTheRomSilentPhysicsAndRockFrame() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        new RoomEntitySnapshot(new ArrayList<>(snapshot().slots())))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        int slot = runtime.spawnLiftableRockRubble(0x48, 0x4F);

        RoomEntity rubble = runtime.inspection().snapshot().slots().get(slot);
        assertEquals(EntityStatus.ACTIVE, rubble.status());
        assertEquals(0x05, rubble.type());
        assertEquals(0x05, rubble.spriteVariant());
        assertEquals(0xC4, runtime.enemies().physicsFlags(slot));
        assertEquals(0x0A, runtime.options1(slot));
        assertEquals(List.of(), runtime.consumePendingEntityEvents());

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0, 0, () -> 0));
        assertEquals(0x05, runtime.inspection().snapshot().slots().get(slot).spriteVariant());
    }
}
