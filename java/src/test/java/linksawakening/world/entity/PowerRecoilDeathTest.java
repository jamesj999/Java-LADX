package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.keeseDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.loadRom;
import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class PowerRecoilDeathTest {

    @Test
    void rejectsPowerRecoilDeathForNonDyingEntities() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        assertThrows(IllegalArgumentException.class, () -> new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(body)
                .spriteVariant(1)
                .powerRecoilDeath(true)
                .build());
    }

    @Test
    void hydratesPowerRecoilDeathStateFromAnInitialDyingEntity() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        RoomEntity initialEntity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .status(EntityStatus.DYING)
                .spriteDefinition(body)
                .spriteVariant(1)
                .deathSpriteVariant(2)
                .powerRecoilDeath(true)
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(initialEntity)).build();

        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).powerRecoilDeath(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).deathSpriteVariant());
    }

    @Test
    void disablingARecoilDeathClearsItsInternalPowerState() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x19)
                        .at(64, 64)
                        .spriteDefinition(keeseDefinition())
                        .build()))
                .build();

        runtime.combat()
                .resolveCombat(new EntityVerticalContactFrame(
                        0,
                        120,
                        120,
                        false,
                        true,
                        true,
                        72,
                        1,
                        72,
                        1,
                        new EnemyAttackContext(1, false, true, false, false)));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).powerRecoilDeath(0));

        for (int frame = 0; frame < 0x40; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }

        assertFalse(runtime.inspection().snapshot().slots().get(0).loaded());
        assertFalse(new EntityEnemyInspectionQueries(runtime.enemies()).powerRecoilDeath(0));
    }

    @Test
    void aPowerRecoilSwordHitMarksTheRomPowerDeathPresentation() {
        EntitySpriteDefinition definition = keeseDefinition();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x19)
                        .at(64, 64)
                        .spriteDefinition(definition)
                        .build()))
                .build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntityVerticalContactFrame(
                        0,
                        120,
                        120,
                        false,
                        true,
                        true,
                        72,
                        1,
                        72,
                        1,
                        new EnemyAttackContext(1, false, true, false, false)));

        assertEquals(1, events.size());
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
        assertEquals(0x20, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(0x40, new EntityEnemyInspectionQueries(runtime.enemies()).dyingCountdown(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).deathSpriteVariant());
        assertTrue(runtime.inspection().snapshot().slots().get(0).powerRecoilDeath());
        for (int frame = 1; frame <= 11; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(0x40 - 11, new EntityEnemyInspectionQueries(runtime.enemies()).dyingCountdown(0));
        assertEquals(-1, runtime.inspection().snapshot().slots().get(0).deathSpriteVariant());
        assertTrue(runtime.enemies().enemyIgnoreHitsCountdown(0) > 0);
    }

    @Test
    void deathPresentationFieldsAreIndependentFromNormalSpriteVariant() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        RoomEntity entity = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .status(EntityStatus.DYING)
                .spriteDefinition(body)
                .spriteVariant(1)
                .deathSpriteVariant(3)
                .powerRecoilDeath(true)
                .build();
        assertEquals(1, entity.spriteVariant());
        assertEquals(3, entity.deathSpriteVariant());
        assertTrue(entity.powerRecoilDeath());
    }

    @Test
    void rejectsDeathSpriteVariantsOutsideTheFourFrameRange() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        assertThrows(IllegalArgumentException.class, () -> new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .status(EntityStatus.DYING)
                .spriteDefinition(body)
                .spriteVariant(1)
                .deathSpriteVariant(-2)
                .build());
        assertThrows(IllegalArgumentException.class, () -> new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .status(EntityStatus.DYING)
                .spriteDefinition(body)
                .spriteVariant(1)
                .deathSpriteVariant(4)
                .build());
    }

    @Test
    void rejectsDeathSpriteVariantsForNonDyingEntities() {
        EntitySpriteDefinition body = pairDefinition(0x09, 2);
        assertThrows(IllegalArgumentException.class, () -> new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(body)
                .spriteVariant(1)
                .deathSpriteVariant(0)
                .build());
    }

    @Test
    void clearingThenReusingAProjectileSlotDoesNotRetainPowerDeathState() throws IOException {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(loadRom()));
        EntitySpriteDefinition octorok = catalog.forEntityType(0x09, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        EntitySpriteDefinition keese = catalog.forEntityType(0x19, EntityRoomLoader.RoomTable.OVERWORLD, -1);
        List<RoomEntity> slots = new ArrayList<>();
        slots.add(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(64, 64)
                .spriteDefinition(octorok)
                .build());
        for (int slot = 1; slot < 15; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        slots.add(new RoomEntityBuilder()
                .slot(15)
                .sourceLoadOrder(1)
                .type(0x19)
                .at(120, 120)
                .spriteDefinition(keese)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(new RoomEntitySnapshot(slots))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .build();

        runtime.combat()
                .resolveCombat(new EntityVerticalContactFrame(
                        0,
                        80,
                        64,
                        false,
                        true,
                        true,
                        112,
                        16,
                        112,
                        16,
                        new EnemyAttackContext(1, false, true, false, false)));
        assertTrue(new EntityEnemyInspectionQueries(runtime.enemies()).powerRecoilDeath(15));
        runtime.lifecycle().clearEntity(15);
        assertFalse(new EntityEnemyInspectionQueries(runtime.enemies()).powerRecoilDeath(15));

        runtime.tick(EntityTestFrames.frame(runtime, 0, 80, 64, () -> 0, false));
        for (int frame = 1; frame <= 6; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, () -> 0, false));
        }

        RoomEntity projectile = runtime.inspection().snapshot().slots().get(15);
        assertEquals(EntityStatus.ACTIVE, projectile.status());
        assertEquals(0x0A, projectile.type());
        assertEquals(-1, projectile.deathSpriteVariant());
        assertFalse(projectile.powerRecoilDeath());
        assertFalse(new EntityEnemyInspectionQueries(runtime.enemies()).powerRecoilDeath(15));
    }
}
