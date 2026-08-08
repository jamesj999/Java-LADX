package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoosterRuntimeTest {

    @Test
    void activeFollowingRoosterUsesItsDedicatedPowerBraceletLiftBranch() {
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, -1, RoosterMotion.ENTITY_TYPE, 0x40, 0x50,
                EntityStatus.ACTIVE, definition(), 0)));
        runtime.setFollowingNpcState(new FollowingNpcState(true, 0, false, false,
            0, 0, false));
        runtime.setActionButtonsHeld(false, true);
        runtime.setLikeLikeLinkInventory(0x00, 0x03);
        runtime.setPowerBraceletButtonHeld(true);
        runtime.setLinkAttackStepAnimationCountdown(0);

        runtime.tickWithProjectileEvents(0, 0x40, 0x50, () -> 0, null,
            new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0, 3, false));

        RoomEntity rooster = runtime.snapshot().slots().get(0);
        assertEquals(EntityStatus.LIFTED, rooster.status());
        assertEquals(0xD2, runtime.physicsFlags(0));
        assertTrue(runtime.liftedEntityState().active());
        assertEquals(RoosterMotion.ENTITY_TYPE, runtime.liftedEntityState().type());
        assertTrue(runtime.consumePendingEntityEvents().contains(new EntityCombatEvent(
            0, RoosterMotion.ENTITY_TYPE, 0, false,
            EntityCombatEvent.SoundChannel.WAVE, RoosterMotion.LIFT_WAVE_SFX)));
    }

    private static RoomEntitySnapshot snapshot(RoomEntity entity) {
        List<RoomEntity> slots = new ArrayList<>();
        while (slots.size() < EntityRoomLoader.MAX_ENTITIES) {
            slots.add(RoomEntity.disabled(slots.size()));
        }
        slots.set(entity.slot(), entity);
        return new RoomEntitySnapshot(slots);
    }

    private static EntitySpriteDefinition definition() {
        EntitySpriteDefinition.OamAttribute first =
            new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        EntitySpriteDefinition.OamAttribute second =
            new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            variants.add(new EntitySpriteDefinition.Variant(first, second));
        }
        return new EntitySpriteDefinition(RoosterMotion.ENTITY_TYPE, 0x19, 0x59BC,
            EntitySpriteDefinition.Shape.PAIR, 0, variants);
    }
}
