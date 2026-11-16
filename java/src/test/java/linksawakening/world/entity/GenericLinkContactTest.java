package linksawakening.world.entity;

import static linksawakening.world.entity.EntityRuntimeTestSupport.pairDefinition;
import static linksawakening.world.entity.EntityRuntimeTestSupport.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class GenericLinkContactTest {

    @Test
    void ordinaryEnemyContactCarriesTheRomLinkRecoilResponse() {
        RoomEntity octorok = new RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(0x09)
                .at(0x40, 0x50)
                .spriteDefinition(pairDefinition(0x09, 2))
                .build();
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(octorok)).build();

        List<EntityCombatEvent> events = runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(1, 0x40, 0x58, false, true, false, 0, 0, 0, 0));

        assertEquals(1, events.size());
        assertEquals(0x04, events.get(0).linkDamage());
        assertEquals(
                new EntityCombatEvent.LinkCollisionResponse(0, 0x14, 0x10),
                events.get(0).linkCollisionResponse());
    }

    @Test
    void specialEnemyHandlersDoNotReceiveTheGenericLinkRecoilResponse() {
        assertFalse(RoomEntityCombatRules.usesStandardLinkContactResponse(0x1C));
        // Beetles only use the special branch for shield contact ($03:6F2A).
        assertTrue(RoomEntityCombatRules.usesStandardLinkContactResponse(0x2C));
        assertFalse(RoomEntityCombatRules.usesStandardLinkContactResponse(0x55));
        assertFalse(RoomEntityCombatRules.usesStandardLinkContactResponse(0x9C));
    }

    @Test
    void rockHittingLinkAlsoSuppressesTheWallPokeJingle() {
        RoomEntityRuntime runtime = new RoomEntityRuntimeBuilder(snapshot(new RoomEntityBuilder()
                        .type(0x0A)
                        .at(0x40, 0x50)
                        .spriteDefinition(pairDefinition(0x0A, 1))
                        .build()))
                .build();
        List<EntityProjectileEvent> events = runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x40,
                0x50,
                () -> 0,
                linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN,
                new EnemyProjectileCollision.LinkState(0x40, 0x50, 0, 0, 1, false)));

        assertEquals(1, events.size());
        assertEquals(EntityProjectileEvent.Kind.LINK_DAMAGE, events.get(0).kind());
        assertEquals(0x18, new EntityEnemyInspectionQueries(runtime.enemies()).enemyProjectileTransitionCountdown(0));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
    }
}
