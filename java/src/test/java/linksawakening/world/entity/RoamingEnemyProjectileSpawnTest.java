package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static linksawakening.world.entity.EntityRuntimeTestSupport.syntheticRom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoamingEnemyProjectileSpawnTest {

    @Test
    void moblinLaunchUsesTheHighestDisabledSlotAndDoesNotTickItTwice() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()));
        EntitySpriteDefinition definition = catalog.forEntityType(0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < 15; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        slots.add(new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .z(0x03)
                .build());
        RoomEntitySnapshot initial = new RoomEntitySnapshot(slots);
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial)
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();

        for (int frame = 0; frame <= 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x70, 0x50, () -> 0));
        }

        RoomEntity projectile = runtime.inspection().snapshot().slots().get(14);
        assertEquals(EntityStatus.ACTIVE, projectile.status());
        assertEquals(-1, projectile.sourceLoadOrder());
        assertEquals(0x0C, projectile.type());
        assertEquals(0x48, projectile.x());
        assertEquals(0x4C, projectile.y());
        assertEquals(0x03, projectile.z());
        assertEquals(0, projectile.spriteVariant());
        assertTrue(projectile.spriteDefinition().supported());
        assertEquals(1, runtime.enemies().enemyIgnoreHitsCountdown(14));
        assertEquals(0x20, new EntityEnemyInspectionQueries(runtime.enemies()).enemyProjectileSpeedX(14));
        assertEquals(
                1,
                new EntityRoomInspectionQueries(runtime.inspection())
                        .projectileLaunchRequests()
                        .size());
        assertEquals(
                15,
                new EntityRoomInspectionQueries(runtime.inspection())
                        .projectileLaunchRequests()
                        .get(0)
                        .sourceSlot());
        assertEquals(
                0x0C,
                new EntityRoomInspectionQueries(runtime.inspection())
                        .projectileLaunchRequests()
                        .get(0)
                        .projectileType());

        assertEquals(0, runtime.lifecycle().clearEntity(14));
        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(14).status());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyProjectileSpeedX(14));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyProjectileTransitionCountdown(14));
    }

    @Test
    void runtimeExposesTheRoamingEnemyLaunchRequestAndSpawnsTheProjectile() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()));
        EntitySpriteDefinition definition = catalog.forEntityType(0x09, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial)
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();
        AtomicInteger randomCalls = new AtomicInteger();
        IntSupplier randomBytes = () -> {
            randomCalls.incrementAndGet();
            return 0;
        };

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, randomBytes, false));
        for (int frame = 1; frame <= 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, randomBytes, false));
        }

        assertEquals(1, randomCalls.get());
        assertEquals(
                1,
                new EntityRoomInspectionQueries(runtime.inspection())
                        .projectileLaunchRequests()
                        .size());
        RoamingEnemyMotion.LaunchRequest request = new EntityRoomInspectionQueries(runtime.inspection())
                .projectileLaunchRequests()
                .get(0);
        assertEquals(0, request.sourceSlot());
        assertEquals(0x09, request.sourceType());
        assertEquals(0x0A, request.projectileType());
        assertEquals(2, runtime.inspection().snapshot().loadedEntities().size());
        RoomEntity projectile = runtime.inspection().snapshot().slots().get(15);
        assertEquals(EntityStatus.ACTIVE, projectile.status());
        assertEquals(-1, projectile.sourceLoadOrder());
        assertEquals(0x0A, projectile.type());
        assertEquals(0x48, projectile.x());
        assertEquals(0x40, projectile.y());
    }

    @Test
    void enemyProjectileWallTransitionUnloadsAndClearsTheReverseSlot() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()));
        EntitySpriteDefinition definition = catalog.forEntityType(0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < 15; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        slots.add(new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(0x40, 0x50)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(new RoomEntitySnapshot(slots))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();
        RoomEntityBackgroundCollision rightWall = (entity, direction, nextX, nextY) -> direction == 0;

        for (int frame = 0; frame <= 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x70, 0x50, () -> 0));
        }
        runtime.tick(EntityTestFrames.frame(runtime, 7, 0x70, 0x50, () -> 0, rightWall));
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyProjectileTransitionCountdown(14));

        for (int frame = 8; frame <= 30; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x70, 0x50, () -> 0, rightWall));
        }

        assertEquals(
                EntityStatus.DISABLED,
                runtime.inspection().snapshot().slots().get(14).status());
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyProjectileSpeedX(14));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyProjectileTransitionCountdown(14));
    }

    @Test
    void fullEntityTableLeavesTheLaunchSourceAndProjectileStateUnchanged() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()));
        EntitySpriteDefinition moblinDefinition = catalog.forEntityType(0x0B, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        List<RoomEntity> entities = new ArrayList<>();
        for (int slot = 0; slot < 15; slot++) {
            entities.add(new RoomEntityBuilder()
                    .slot(slot)
                    .type(0x01)
                    .at(0x20, 0x20)
                    .spriteDefinition(EntitySpriteDefinition.unsupported(0x01))
                    .spriteVariant(-1)
                    .build());
        }
        entities.add(new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(0x40, 0x50)
                .spriteDefinition(moblinDefinition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(new RoomEntitySnapshot(entities))
                .indoorRoom(false)
                .randomByteSupplier(() -> 0)
                .spriteHandlers(catalog)
                .build();

        for (int frame = 0; frame <= 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 0x70, 0x50, () -> 0));
        }

        assertEquals(0x0B, runtime.inspection().snapshot().slots().get(15).type());
        assertEquals(0x0A, new EntityEnemyInspectionQueries(runtime.enemies()).octorokTransitionCountdown(15));
        for (int slot = 0; slot < 15; slot++) {
            assertNotEquals(
                    0x0C, runtime.inspection().snapshot().slots().get(slot).type());
        }
    }
}
