package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

final class RoamingSimultaneousContactTest {
    @Test
    void bodyContactRetainsRecoilWhenTheSameFrameAlsoDamagesTheEnemy() {
        for (int type : new int[] {0x09, 0x0B}) {
            var runtime = enemy(type);
            var event = runtime.resolveCombat(135, 51, 88, false, true, true, 66, 5, 90, 5)
                    .getFirst();

            assertTrue(event.swordHit());
            assertEquals(1, event.enemyDamage());
            assertEquals(7, runtime.enemies().enemyHealth(2));
            assertEquals(4, event.linkDamage());
            assertEquals(new EntityCombatEvent.LinkCollisionResponse(0xEC, 0x11, 0x10), event.linkCollisionResponse());
        }
    }

    @Test
    void swordHitOnTheOtherCollisionParityDoesNotInventBodyRecoil() {
        for (int type : new int[] {0x09, 0x0B}) {
            var runtime = enemy(type);
            var event = runtime.resolveCombat(134, 51, 88, false, true, true, 66, 5, 90, 5)
                    .getFirst();

            assertTrue(event.swordHit());
            assertEquals(7, runtime.enemies().enemyHealth(2));
            assertEquals(0, event.linkDamage());
            assertEquals(EntityCombatEvent.LinkCollisionResponse.NONE, event.linkCollisionResponse());
        }
    }

    private static RoomEntityRuntime enemy(int type) {
        var slots = new ArrayList<RoomEntity>();
        for (int slot = 0; slot < 16; slot++) slots.add(RoomEntity.disabled(slot));
        var definition = new EntitySpriteDefinition(
                type,
                3,
                0x57FB,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                Collections.nCopies(
                        8,
                        new EntitySpriteDefinition.Variant(
                                new EntitySpriteDefinition.OamAttribute(0, 0),
                                new EntitySpriteDefinition.OamAttribute(0, 0))));
        slots.set(2, new RoomEntity(2, 0, type, 58, 82, EntityStatus.ACTIVE, definition, 0, 0, 0, 0, -1, false));
        var runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(slots));
        runtime.enemyHealth[2] = 8; // Synthetic nonlethal target, matching the CPU-hook probe.
        return runtime;
    }
}
