package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.MotionTestAccess;
import linksawakening.world.entity.motion.RoosterMotion;
import org.junit.jupiter.api.Test;

final class RoosterRuntimeTest {

    @Test
    void activeFollowingRoosterUsesItsDedicatedPowerBraceletLiftBranch() {
        RoomEntityRuntime runtime = RoomEntityRuntime.builder(snapshot(new RoomEntityBuilder()
                        .type(MotionTestAccess.RoosterMotion_ENTITY_TYPE)
                        .at(0x40, 0x50)
                        .spriteDefinition(definition())
                        .build()))
                .build();
        runtime.configuration().setFollowingNpcState(new FollowingNpcState(true, 0, false, false, 0, 0, false));
        runtime.configuration().setActionButtonsHeld(false, true);
        runtime.configuration().setLikeLikeLinkInventory(0x00, 0x03);
        runtime.configuration().setPowerBraceletButtonHeld(true);
        runtime.configuration().setLinkAttackStepAnimationCountdown(0);

        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x40,
                0x50,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0, 3, false)));

        RoomEntity rooster = runtime.inspection().snapshot().slots().get(0);
        assertEquals(EntityStatus.LIFTED, rooster.status());
        assertEquals(0xD2, runtime.enemies().physicsFlags(0));
        assertTrue(runtime.inspection().liftedEntityState().active());
        assertEquals(
                MotionTestAccess.RoosterMotion_ENTITY_TYPE,
                runtime.inspection().liftedEntityState().type());
        assertTrue(runtime.consumePendingEntityEvents()
                .contains(new EntityCombatEvent(
                        0,
                        MotionTestAccess.RoosterMotion_ENTITY_TYPE,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.WAVE,
                        RoosterMotion.LIFT_WAVE_SFX)));
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
        EntitySpriteDefinition.OamAttribute first = new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        EntitySpriteDefinition.OamAttribute second = new EntitySpriteDefinition.OamAttribute(0xFF, 0);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            variants.add(new EntitySpriteDefinition.Variant(first, second));
        }
        return new EntitySpriteDefinition(
                MotionTestAccess.RoosterMotion_ENTITY_TYPE,
                0x19,
                0x59BC,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                variants);
    }
}
