package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.romWithSwordResult;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class GibdoRuntimeTest {

    @Test
    void romBurningGibdoBecomesAnActiveStalfosAfterItsTimerExpires() throws IOException {
        byte[] rom = romWithSwordResult(0x1F, 0xFE);
        RomEnemyCombatTables tables = new RomEnemyCombatTables(linksawakening.world.entity.EntityRomTestData.view(rom));
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(rom));
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x1F)
                        .at(64, 64)
                        .spriteDefinition(pairDefinition(0x1F, 2))
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(null)
                .spriteHandlers(catalog)
                .enemyCombatTables(tables)
                .build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(0xFE, events.get(0).enemySpecialAction());

        for (int frame = 0; frame < 0x60; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(0x1E, runtime.inspection().snapshot().slots().get(0).type());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, runtime.enemies().transitionCountdown(0));
        assertEquals(0x12, runtime.enemies().physicsFlags(0));
        assertEquals(
                0x4E7D,
                runtime.inspection()
                        .snapshot()
                        .slots()
                        .get(0)
                        .spriteDefinition()
                        .address());
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }

    @Test
    void gibdoRunsTheRomInitDirectionChoiceAndFixedPointWalk() {
        EntitySpriteDefinition definition = pairDefinition(0x1F, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1F)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        // EntityInitHandlersTable._1F only increments the state. The first
        // handler pass therefore takes the forced-direction path.
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0xFF)));
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoState(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoSpeedY(0));

        // LikeLikeGibdoSpeeds[2] is $F8 (-8 pixels per 16 frames).
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x02)));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoState(0));
        assertEquals(0xF8, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoSpeedX(0));
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());

        // A nonzero random low six bits keeps the selected direction and the
        // ROM fixed-point update moves the entity left by one pixel here.
        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x01)));
        assertEquals(63, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0xF8, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoSpeedX(0));
    }

    @Test
    void gibdoFallsBackToTheRomVerticalSpeedTableWhenHorizontalSpeedIsZero() {
        EntitySpriteDefinition definition = pairDefinition(0x1F, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1F)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x00, 0x01)));

        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoSpeedX(0));
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoSpeedY(0));
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(64, runtime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void gibdoReversesTheMovingAxisWhenTheBackgroundQueryBlocksIt() {
        EntitySpriteDefinition definition = pairDefinition(0x1F, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1F)
                .at(64, 64)
                .status(EntityStatus.INIT)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0xFF)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 120, 120, sequence(0x02)));

        RoomEntityBackgroundCollision leftWall = (entity, direction, nextX, nextY) -> direction == 1;
        runtime.tick(EntityTestFrames.frame(runtime, 2, 120, 120, sequence(0x01), leftWall));

        assertEquals(64, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x08, new EntityEnemyInspectionQueries(runtime.enemies()).gibdoSpeedX(0));
    }

    @Test
    void gibdoUsesHealthGroupTwoFCombatValues() {
        EntitySpriteDefinition definition = pairDefinition(0x1F, 2);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x1F)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(0x08, contact.get(0).linkDamage());
        assertEquals(0x06, runtime.enemies().enemyHealth(0));

        List<EntityCombatEvent> sword =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 72, 72, false, true, true, 72, 1, 72, 1));
        assertEquals(1, sword.size());
        assertTrue(sword.get(0).swordHit());
        assertEquals(0x05, runtime.enemies().enemyHealth(0));
    }
}
