package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeSideViewTest extends EntityRuntimeTestSupport {

    @Test
    void helperEntitiesUseTheirRomPillarAndSideViewHitboxes() {
        RoomEntity pillar = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x9D)
                .at(0x50, 0x60)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x9D))
                .build();
        RoomEntity thwomp = new RoomEntityBuilder()
                .slot(1)
                .sourceLoadOrder(0)
                .type(0xD8)
                .at(0x50, 0x60)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0xD8))
                .build();

        assertTrue(RoomEntityCombatRules.overlapsLink(pillar, 0x5A, 0x60));
        assertTrue(RoomEntityCombatRules.overlapsLink(thwomp, 0x68, 0x60));
    }

    @Test
    void enemyProjectileReboundUsesTheSideViewRoomGravity() {
        RoomEntity projectile = new RoomEntityBuilder()
                .type(0x0C)
                .at(0x40, 0x50)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x0C))
                .build();
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                        snapshot(projectile).withSideScrolling(true))
                .build();
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0,
                0,
                () -> 0,
                (entity, direction, nextX, nextY) -> direction == 0,
                EnemyProjectileCollision.LinkState.nonInteractive()));
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyProjectileTransitionCountdown(0));

        for (int frame = 1; frame <= 9; frame++) {
            runtime.tick(EntityTestFrames.projectileFrame(
                    runtime, frame, 0, 0, () -> 0, null, EnemyProjectileCollision.LinkState.nonInteractive()));
        }

        RoomEntity arrow = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.ACTIVE, arrow.status());
        assertEquals(0, arrow.z());
        assertEquals(0x54, arrow.y());
        assertEquals(18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyProjectileSpeedY(0));
    }
}
