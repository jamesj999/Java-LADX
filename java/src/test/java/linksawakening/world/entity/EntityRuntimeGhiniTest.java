package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeGhiniTest extends EntityRuntimeTestSupport {

    @Test
    void ghiniUsesItsRomTargetTimersAccelerationAndZCorrection() {
        EntitySpriteDefinition definition = pairDefinition(0x12, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x12)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 96, 96, sequence(0x00, 0x00)));
        assertEquals(0x20, runtime.enemies().ghiniTransitionCountdown(0));
        assertEquals(0x18, runtime.enemies().ghiniPrivateCountdown1(0));
        assertEquals(0, runtime.enemies().ghiniTargetXDirection(0));
        assertEquals(0, runtime.enemies().ghiniTargetYDirection(0));
        assertEquals(1, runtime.enemies().ghiniSpeedX(0));
        assertEquals(1, runtime.enemies().ghiniSpeedY(0));
        assertEquals(1, runtime.inspection().snapshot().slots().get(0).z());

        runtime.tick(EntityTestFrames.frame(runtime, 4, 96, 96, sequence(0x00, 0x00)));
        assertEquals(2, runtime.enemies().ghiniSpeedX(0));
        assertEquals(2, runtime.enemies().ghiniSpeedY(0));
        assertEquals(2, runtime.inspection().snapshot().slots().get(0).z());
    }

    @Test
    void ghiniUsesTheRomNormalEnemyContactAndHealthValues() {
        EntitySpriteDefinition definition = pairDefinition(0x12, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x12)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        List<EntityCombatEvent> contact =
                runtime.resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit =
                runtime.resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, firstHit.size());
        assertEquals(0x07, runtime.enemies().enemyHealth(0));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
    }

    @Test
    void ghiniTakesEachTargetDirectionFromTheSameRandomByteAsItsTimer() {
        EntitySpriteDefinition definition = pairDefinition(0x12, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x12)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 96, 96, sequence(0x1F, 0x0F)));

        assertEquals(0x3F, runtime.enemies().ghiniTransitionCountdown(0));
        assertEquals(1, runtime.enemies().ghiniTargetXDirection(0));
        assertEquals(0x1F, runtime.enemies().ghiniPrivateCountdown1(0));
        assertEquals(1, runtime.enemies().ghiniTargetYDirection(0));
        assertEquals(0xFF, runtime.enemies().ghiniSpeedX(0));
        assertEquals(0xFF, runtime.enemies().ghiniSpeedY(0));
    }

    @Test
    void ghiniFamilyCombatRulesUseRomHealthDamageAndHitboxEntries() throws IOException {
        byte[] rom = loadRom();
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);

        for (int type : new int[] {0x10, 0x11, 0x12}) {
            assertTrue(RoomEntityCombatRules.supportsEnemyCollision(type));
            assertEquals(0x08, RoomEntityCombatRules.initialHealth(type));
            assertEquals(tables.initialHealth(type), RoomEntityCombatRules.initialHealth(type));
            assertEquals(0x08, RoomEntityCombatRules.contactDamage(type));
            assertEquals(tables.contactDamage(type), RoomEntityCombatRules.contactDamage(type));
        }

        RoomEntity normal = ghiniEntity(catalog, 0, 0x10, EntityStatus.ACTIVE);
        RoomEntity giant = ghiniEntity(catalog, 1, 0x11, EntityStatus.ACTIVE);
        assertFalse(RoomEntityCombatRules.overlapsLink(normal, 0x5D, 0x50));
        assertTrue(RoomEntityCombatRules.overlapsLink(giant, 0x5D, 0x50));
        assertFalse(EntityCombatTestAccess.overlapsSword(normal, 0x62, 1, 0x58, 1));
        assertTrue(EntityCombatTestAccess.overlapsSword(giant, 0x62, 1, 0x58, 1));
    }

    @Test
    void hidingAndGiantGhinisStartHiddenAndSkipGroundAndCombat() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);

        for (int type : new int[] {0x10, 0x11}) {
            RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                            snapshot(ghiniEntity(catalog, 0, type, EntityStatus.INIT)))
                    .indoorRoom(false)
                    .randomByteSupplier(sequence(0x00))
                    .spriteHandlers(catalog)
                    .enemyCombatTables(tables)
                    .build();
            List<Integer> groundCalls = new ArrayList<>();
            runtime.configuration().setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) -> {
                groundCalls.add(entity.slot());
                return RoomEntityGroundInteraction.Result.unchanged(entity, previousStatus);
            });

            runtime.tick(EntityTestFrames.frame(runtime, 0, 0x90, 0x90, 0, sequence(0x00)));

            RoomEntity afterInit = runtime.inspection().snapshot().slots().get(0);
            assertEquals(EntityStatus.ACTIVE, afterInit.status());
            assertEquals(-1, afterInit.spriteVariant(), "type=0x" + Integer.toHexString(type));
            assertTrue(groundCalls.isEmpty());
            assertTrue(runtime.resolveCombat(
                            new EntitySwordContactFrame(1, 0x50, 0x50, false, true, true, 0x58, 1, 0x58, 1))
                    .isEmpty());
        }
    }

    @Test
    void typedGhiniPresentationReachesNormalFlipAndGiantRectangleOutput() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);

        RoomEntityRuntime normalRuntime = RoomEntityRuntime.builder(
                        snapshot(ghiniEntity(catalog, 0, 0x12, EntityStatus.ACTIVE)))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        normalRuntime.tick(EntityTestFrames.frame(normalRuntime, 0, 0x90, 0x90, 0, sequence(0x00)));
        assertNotEquals(0, normalRuntime.inspection().snapshot().slots().get(0).entityFlipAttribute() & 0x20);

        RoomEntityRuntime giantRuntime = RoomEntityRuntime.builder(
                        snapshot(ghiniEntity(catalog, 0, 0x11, EntityStatus.ACTIVE)))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        giantRuntime.tick(EntityTestFrames.frame(giantRuntime, 0, 0x50, 0x50, 0x01, sequence(0x00)));
        giantRuntime.tick(EntityTestFrames.frame(giantRuntime, 1, 0x90, 0x90, 0, sequence(0x00)));
        RoomEntity giant = giantRuntime.inspection().snapshot().slots().get(0);
        assertEquals(2, giant.spriteVariant());
        assertEquals(0, giant.entityFlipAttribute() & 0x20);
    }

    @Test
    void allGhiniTypesUseTheSharedRecoilPath() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        EnemyAttackContext nonDamaging = new EnemyAttackContext(0, false, false, false, false);

        for (int type : new int[] {0x10, 0x11, 0x12}) {
            RoomEntityRuntime runtime = RoomEntityRuntime.builder(
                            snapshot(ghiniEntity(catalog, 0, type, EntityStatus.ACTIVE)))
                    .indoorRoom(false)
                    .randomByteSupplier(sequence(0x00))
                    .spriteHandlers(catalog)
                    .enemyCombatTables(tables)
                    .build();
            if (type != 0x12) {
                runtime.tick(EntityTestFrames.frame(runtime, 0, 0x50, 0x50, 0x01, sequence(0x00)));
            }
            runtime.tick(EntityTestFrames.frame(runtime, 1, 0x90, 0x90, 0, sequence(0x00)));

            RoomEntity current = runtime.inspection().snapshot().slots().get(0);
            int linkX = (current.x() + 0x08) & 0xFF;
            int linkY = (current.y() - current.z() + 0x08) & 0xFF;
            List<EntityCombatEvent> events = runtime.resolveCombat(new EntityVerticalContactFrame(
                    0, linkX, linkY, false, false, true, linkX, 1, linkY, 1, nonDamaging));
            assertEquals(1, events.size(), "type=0x" + Integer.toHexString(type));
            assertTrue(runtime.enemies().enemyRecoilActive(0), "type=0x" + Integer.toHexString(type));
            assertEquals(0xD0, runtime.enemies().enemyRecoilSpeedX(0), "type=0x" + Integer.toHexString(type));
            assertEquals(0xD0, runtime.enemies().enemyRecoilSpeedY(0), "type=0x" + Integer.toHexString(type));
        }
    }

    @Test
    void GhiniOptionsDisableGroundInteractionAndWallCollisionForEveryVariant() throws IOException {
        byte[] rom = loadRom();
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(rom);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(
                        ghiniEntity(catalog, 0, 0x10, EntityStatus.ACTIVE),
                        ghiniEntity(catalog, 1, 0x11, EntityStatus.ACTIVE),
                        ghiniEntity(catalog, 2, 0x12, EntityStatus.ACTIVE)))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();
        List<Integer> groundCalls = new ArrayList<>();
        runtime.configuration().setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) -> {
            groundCalls.add(entity.slot());
            return RoomEntityGroundInteraction.Result.unchanged(entity, previousStatus);
        });

        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x90, 0x90, 0, sequence(0x00)));

        assertTrue(groundCalls.isEmpty());
        for (int slot = 0; slot < 3; slot++) {
            assertEquals(0x11, runtime.options1(slot));
        }
    }
}
