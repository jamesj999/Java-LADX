package linksawakening.app.gameplay.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.equipment.Sword;
import linksawakening.world.entity.event.EntityProjectileEvent;
import org.junit.jupiter.api.Test;

class EntityFrameOrderTest {
    static final class Recording implements EntityFrameStages {
        final List<String> calls = new ArrayList<>();
        final List<EntityProjectileEvent> events = List.of(EntityProjectileEvent.hookshotPull(1, 16, 0));
        final Sword.CollisionBox box = new Sword.CollisionBox(true, 1, 2, 3, 4);

        public List<EntityProjectileEvent> tickEntities(GameplaySimulation c, Sword.CollisionBox b) {
            assertSame(box, b);
            calls.add("tick");
            return events;
        }

        public void positions(GameplaySimulation c) {
            calls.add("positions");
        }

        public void motion(GameplaySimulation c) {
            calls.add("motion");
        }

        public void attacks(GameplaySimulation c) {
            calls.add("attacks");
        }

        public void sword(GameplaySimulation c) {
            calls.add("sword");
        }

        public void chests(GameplaySimulation c) {
            calls.add("chests");
        }

        public void progress(GameplaySimulation c) {
            calls.add("progress");
        }

        public void witch(GameplaySimulation c) {
            calls.add("witch");
        }

        public void dialogs(GameplaySimulation c) {
            calls.add("dialogs");
        }

        public void finalEffects(GameplaySimulation c) {
            calls.add("finalEffects");
        }

        public void combat(GameplaySimulation c, List<EntityProjectileEvent> events) {
            assertSame(this.events, events);
            calls.add("combat");
        }

        public void projectiles(GameplaySimulation c, List<EntityProjectileEvent> events) {
            assertSame(this.events, events);
            calls.add("projectiles");
        }
    }

    @Test
    void drainsWritesThenCombatRewardsDialogsAndProjectileResponses() {
        var r = new Recording();
        r.run(null, r.box);
        assertEquals(
                List.of(
                        "tick",
                        "positions",
                        "motion",
                        "attacks",
                        "sword",
                        "combat",
                        "chests",
                        "progress",
                        "witch",
                        "dialogs",
                        "projectiles",
                        "finalEffects"),
                r.calls);
    }

    @Test
    void projectileConsumersReceiveTheExactTickEvents() {
        var r = new Recording();
        r.run(null, r.box);
        assertTrue(r.calls.indexOf("tick") < r.calls.indexOf("combat"));
        assertTrue(r.calls.indexOf("dialogs") < r.calls.indexOf("projectiles"));
    }

    @Test
    void entityAttackRequestsApplyAfterTheTickThatCanThrowABomb() {
        var r = new Recording();
        r.run(null, r.box);
        assertEquals(List.of("tick", "positions", "motion", "attacks"), r.calls.subList(0, 4));
    }
}
