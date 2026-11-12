package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntityRuntimeZolGel2Test extends EntityRuntimeTestSupport {

    @Test
    void hidingZolRequestsTheRomJumpJingleForEachLaunchedLeap() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()))
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9B)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));
        for (int frame = 2; frame <= 33; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 80, 64, sequence(0x00)));
        }
        assertEquals(2, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));
        assertEquals(0x24, runtime.consumePendingEntityEvents().getFirst().soundId());

        int frame = 34;
        while (new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0) != 4 && frame < 200) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
        }
        assertEquals(4, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));

        boolean foundSecondJumpJingle = false;
        for (int count = 0; count < 40; count++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
            List<EntityCombatEvent> events = runtime.consumePendingEntityEvents();
            if (!events.isEmpty()) {
                assertEquals(1, events.size());
                assertEquals(
                        EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
                assertEquals(0x24, events.getFirst().soundId());
                foundSecondJumpJingle = true;
                break;
            }
        }
        assertTrue(foundSecondJumpJingle);
        assertEquals(5, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));
    }

    @Test
    void hidingZolRestoresHarmlessPhysicsWhenItsFinalLeapHides() {
        EntitySpriteDefinition definition = new EntitySpriteHandlerCatalog(TestRoms.partial(syntheticRom()))
                .forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .sourceLoadOrder(0)
                        .type(0x9B)
                        .at(64, 64)
                        .status(EntityStatus.INIT)
                        .spriteDefinition(definition)
                        .build()))
                .indoorRoom(false)
                .randomByteSupplier(sequence(0x00))
                .build();

        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        runtime.tick(EntityTestFrames.frame(runtime, 1, 80, 64, sequence(0x00)));

        int frame = 2;
        while (new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0) != 6 && frame < 1000) {
            runtime.tick(EntityTestFrames.frame(runtime, frame++, 80, 64, sequence(0x00)));
        }

        assertEquals(6, new EntityEnemyInspectionQueries(runtime.enemies()).hidingZolState(0));
        assertEquals(0xD2, runtime.enemies().physicsFlags(0));
    }

    @Test
    void combatUsesTheEntityVisualYWhenHidingZolIsAirborne() {
        EntitySpriteDefinition definition = pairDefinition(0x9B, 4);
        RoomEntity airborne = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x9B)
                .at(64, 64)
                .spriteDefinition(definition)
                .z(0x10)
                .build();

        assertTrue(RoomEntityCombatRules.overlapsLink(airborne, 64, 48));
        assertFalse(RoomEntityCombatRules.overlapsLink(airborne, 64, 64));
        assertTrue(EntityCombatTestAccess.overlapsSword(airborne, 72, 1, 56, 1));
        assertFalse(EntityCombatTestAccess.overlapsSword(airborne, 72, 1, 72, 1));
    }
}
