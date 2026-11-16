package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class FriendlyNpcContactTest {

    @Test
    void friendlyNpcHandlersRestoreLinksPreEntityPositionOnContact() {
        RoomEntity tarin = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x3F)
                .at(0x50, 0x60)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x3F))
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(tarin))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x00,
                0x00,
                () -> 0,
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(0, 0, 0, 0, 3, false)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                0x50,
                0x60,
                () -> 0,
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(0x50, 0x60, 0, 0, 3, false)));

        assertEquals(
                List.of(new LinkFinalPositionRequest(0, true, true, false)),
                runtime.outbox().consumePendingLinkFinalPositionRequests());
    }

    @Test
    void friendlyNpcPushUsesTheRomAirPermittedCollisionEntry() {
        RoomEntity tarin = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x3F)
                .at(0x50, 0x60)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x3F))
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(tarin))
                .indoorRoom(true)
                .randomByteSupplier(() -> 0)
                .build();

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x00,
                0x00,
                () -> 0,
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(0, 0, 0, 0, 3, false)));
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                1,
                0x50,
                0x60,
                () -> 0,
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(0x50, 0x60, 1, 0, 3, false)));

        assertEquals(
                List.of(new LinkFinalPositionRequest(0, true, true, false)),
                runtime.outbox().consumePendingLinkFinalPositionRequests());
    }

    @Test
    void friendlyNpcContactUsesTheRomNpcHitbox() {
        RoomEntity marin = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x3E)
                .at(0x50, 0x60)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x3E))
                .build();

        // HITBOX_NPC is ($08,$06,$06,$08), so this edge contact still
        // collides; the ordinary enemy box ($08,$05,$08,$05) would miss it.
        assertTrue(RoomEntityCombatRules.overlapsLink(marin, 0x59, 0x60));
    }

    @Test
    void largeFriendlyNpcsUseTheRomBigNpcHitbox() {
        RoomEntity bear = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0xB5)
                .at(0x40, 0x50)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0xB5))
                .build();

        assertTrue(RoomEntityCombatRules.overlapsLink(bear, 0x4D, 0x50));
    }

    @Test
    void ballPlayingChildrenPushLinkAtGroundPositionEvenWhileHopping() {
        for (int type : new int[] {0x71, 0x72}) {
            RoomEntity kid = new RoomEntityBuilder()
                    .sourceLoadOrder(0)
                    .type(type)
                    .at(0x50, 0x60)
                    .spriteDefinition(pairDefinition(type, 4))
                    .z(8)
                    .build();
            RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(kid))
                    .indoorRoom(false)
                    .randomByteSupplier(() -> 0)
                    .build();
            runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x60, () -> 0));
            assertEquals(
                    List.of(new LinkFinalPositionRequest(0, true, true, false)),
                    runtime.outbox().consumePendingLinkFinalPositionRequests());
        }
    }
}
