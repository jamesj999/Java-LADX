package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.room.ChestContentsTable;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeHookshotTest extends EntityRuntimeTestSupport {

    @Test
    void hookshotUnmasksAnIronMaskAndSpawnsItsRomMaskEntity() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteDefinition ironMaskDefinition =
                catalog.forEntityType(0x24, EntityRoomLoader.RoomTable.INDOORS_A, -1);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x24)
                        .at(0x4D, 0x50)
                        .spriteDefinition(ironMaskDefinition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        int hookshotSlot = runtime.spawnHookshotChain(0x50, 0x50, 0, 1);
        assertEquals(15, hookshotSlot);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0xC0, 0xC0, () -> 0));

        assertEquals(1, runtime.enemies().ironMaskPrivateState2(0));
        RoomEntity mask = runtime.inspection().snapshot().slots().stream()
                .filter(entity -> entity.loaded() && entity.type() == 0x32)
                .findFirst()
                .orElseThrow();
        RoomEntity chain = runtime.inspection().snapshot().slots().get(hookshotSlot);
        assertEquals(chain.x(), mask.x());
        assertEquals(chain.y(), mask.y());
        assertEquals(0x03, mask.spriteDefinition().bank());
        assertEquals(0x5B80, mask.spriteDefinition().address());
        assertEquals(chain.spriteVariant() & 0x01, mask.spriteVariant());
        assertEquals(0xB2, runtime.enemies().physicsFlags(mask.slot()));

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0xC0, 0xC0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 2, 0xC0, 0xC0, () -> 0));
        runtime.tick(EntityTestFrames.frame(runtime, 3, 0xC0, 0xC0, () -> 0));

        RoomEntity unmasked = runtime.inspection().snapshot().slots().get(0);
        assertEquals(0x4F, unmasked.x());
        assertEquals(0x4FEB, unmasked.spriteDefinition().address());
        assertEquals(0, unmasked.spriteVariant());
    }

    @Test
    void keyDropPointMasterStalfosRoomUsesHookshotPresentationAndReward() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(syntheticRom());
        EntitySpriteDefinition definition = keyDropDefinition();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .type(0x30)
                        .at(24, 32)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(0x80);
        EntityRuntimeFixture.debug(runtime).setKeyDropTransitionCountdownForTest(0, 0x11);

        runtime.tick(EntityTestFrames.frame(runtime, 1, 0x40, 0x50, () -> 0));

        RoomEntity held = runtime.inspection().snapshot().slots().get(0);
        assertEquals(-1, held.spriteDefinition().bank());
        assertEquals(EntitySpriteDefinition.Shape.PAIR, held.spriteDefinition().shape());
        assertEquals(0x36, held.spriteDefinition().variant(0).first().tile());
        assertEquals(0x0F, runtime.pickupState().keyDropTransitionCountdown(0));
        assertEquals(List.of(new DialogRequest(0, 0x93)), runtime.consumePendingDialogRequests());
        assertEquals(
                List.of(new KeyRewardEvent(0, ChestContentsTable.CHEST_HOOKSHOT)),
                runtime.consumePendingKeyRewardEvents());
    }
}
