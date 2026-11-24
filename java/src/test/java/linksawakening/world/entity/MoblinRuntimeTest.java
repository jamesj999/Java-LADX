package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.sequence;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class MoblinRuntimeTest {

    @Test
    void raisedShieldReproducesRoom52BumpBeforeTheNextEnemyRecoilStep() {
        RoomEntity moblin = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(63, 80)
                .spriteDefinition(pairDefinition(0x0B, 8))
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(moblin)).build();
        var shield = new EnemyAttackContext(1, false, false, false, false, true, 2, 0);
        var contact = new EnemyCombatFrame(171, 72, 94, false, 0, true, false, 0, 0, 0, 0, shield);

        var events = runtime.combat().resolveCombat(contact);
        assertEquals(1, events.size());
        var bump = events.getFirst();
        assertEquals(0, bump.linkDamage());
        assertEquals(0, bump.enemyDamage());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, bump.soundChannel());
        assertEquals(0x09, bump.soundId());
        assertEquals(new EntityCombatEvent.LinkCollisionResponse(11, 18, 12), bump.linkCollisionResponse());

        runtime.tick(EntityTestFrames.frame(runtime, 171, 72, 94, () -> 0));
        assertEquals(63, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(80, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0x0A, runtime.enemies().enemyIgnoreHitsCountdown(0));

        runtime.tick(EntityTestFrames.frame(runtime, 172, 72, 94, () -> 0));
        assertEquals(61, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(78, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0x09, runtime.enemies().enemyIgnoreHitsCountdown(0));
        assertEquals(2, runtime.enemies().enemyHealth(0));
    }

    @Test
    void bodyContactPrecedesTheShieldAndAnActiveSwordUsesTheWeaponRectangle() {
        RoomEntity moblin = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(63, 80)
                .spriteDefinition(pairDefinition(0x0B, 8))
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(moblin)).build();
        var shield = new EnemyAttackContext(1, false, false, false, false, true, 2, 0);

        var shieldFrame = new EnemyCombatFrame(171, 70, 88, false, 0, true, false, 0, 0, 0, 0, shield);
        var events = runtime.combat().resolveCombat(shieldFrame);
        assertEquals(2, events.size());
        assertEquals(4, events.get(0).linkDamage());
        assertEquals(16, events.get(0).linkCollisionResponse().ignoreCollisionCountdown());
        assertEquals(0, events.get(1).linkDamage());
        assertEquals(12, events.get(1).linkCollisionResponse().ignoreCollisionCountdown());
        assertEquals(2, runtime.enemies().enemyHealth(0));

        var swordFrame = new EnemyCombatFrame(171, 70, 88, false, 0, true, true, 78, 8, 88, 3, shield);
        events = runtime.combat().resolveCombat(swordFrame);
        assertEquals(1, events.size());
        assertEquals(4, events.getFirst().linkDamage());
        assertTrue(events.getFirst().swordHit());
        assertEquals(16, events.getFirst().linkCollisionResponse().ignoreCollisionCountdown());
        assertEquals(1, runtime.enemies().enemyHealth(0));
    }

    @Test
    void moblinUsesTheSameRomRoamingStateMachineAndDirectionalDisplayPairs() {
        EntitySpriteDefinition definition = pairDefinition(0x0B, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();
        IntSupplier randomBytes = sequence(0x02, 0x03);

        runtime.tick(EntityTestFrames.frame(runtime, 0, 200, 32, randomBytes));
        for (int frame = 1; frame <= 0x12; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 200, 32, randomBytes));
        }

        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).octorokState(0));
        assertEquals(3, new EntityEnemyInspectionQueries(runtime.enemies()).octorokDirection(0));
        assertEquals(8, new EntityEnemyInspectionQueries(runtime.enemies()).octorokSpeedY(0));
        runtime.tick(EntityTestFrames.frame(runtime, 0x13, 200, 32, randomBytes));
        runtime.tick(EntityTestFrames.frame(runtime, 0x14, 200, 32, randomBytes));
        assertEquals(65, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0, runtime.inspection().snapshot().slots().get(0).spriteVariant());
    }

    @Test
    void moblinUsesTwoHealthPointsAndTheRomDamageCooldown() {
        EntitySpriteDefinition definition = pairDefinition(0x0B, 8);
        RoomEntitySnapshot initial = snapshot(new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x0B)
                .at(64, 64)
                .spriteDefinition(definition)
                .build());
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(initial).build();

        List<EntityCombatEvent> contact =
                runtime.combat().resolveCombat(new EntitySwordContactFrame(1, 64, 64, false, true, false, 0, 0, 0, 0));
        assertEquals(1, contact.size());
        assertEquals(4, contact.get(0).linkDamage());

        List<EntityCombatEvent> firstHit = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 120, 120, false, true, true, 72, 1, 72, 1));
        assertEquals(1, firstHit.size());
        assertTrue(firstHit.get(0).swordHit());
        assertEquals(
                EntityStatus.ACTIVE,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(1, runtime.enemies().enemyHealth(0));
        runtime.tick(EntityTestFrames.frame(runtime, 0, 120, 120, sequence(0x00)));
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));
        assertTrue(runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 120, 120, false, true, true, 72, 1, 72, 1))
                .isEmpty());

        for (int frame = 1; frame <= 0x18; frame++) {
            runtime.tick(EntityTestFrames.frame(runtime, frame, 120, 120, sequence(0x00)));
        }
        assertEquals(0, new EntityEnemyInspectionQueries(runtime.enemies()).enemyFlashCountdown(0));

        RoomEntity movedMoblin = runtime.inspection().snapshot().slots().get(0);
        List<EntityCombatEvent> secondHit = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(
                        0x19, 120, 120, false, true, true, movedMoblin.x() + 8, 1, movedMoblin.y() + 8, 1));
        assertEquals(1, secondHit.size());
        assertEquals(
                EntityStatus.DYING,
                runtime.inspection().snapshot().slots().get(0).status());
        assertEquals(0, runtime.enemies().enemyHealth(0));
    }
}
