package linksawakening.app.gameplay.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.equipment.Sword;
import linksawakening.world.entity.event.EntityProjectileEvent;
import org.junit.jupiter.api.Test;

class EntityFrameOrderTest {
    static final class Recording implements EntityFrameStages {
        final List<String> calls = new ArrayList<>();
        final List<EntityProjectileEvent> events = List.of(EntityProjectileEvent.hookshotPull(1, 16, 0));
        final Sword.CollisionBox box = new Sword.CollisionBox(true, 1, 2, 3, 4);

        @Override
        public List<EntityProjectileEvent> tickEntities(GameplaySimulation c, Sword.CollisionBox b) {
            assertSame(box, b);
            calls.add("tick");
            return events;
        }

        @Override
        public void positions(GameplaySimulation c) {
            calls.add("positions");
        }

        @Override
        public void motion(GameplaySimulation c) {
            calls.add("motion");
        }

        @Override
        public void attacks(GameplaySimulation c) {
            calls.add("attacks");
        }

        @Override
        public void sword(GameplaySimulation c) {
            calls.add("sword");
        }

        @Override
        public void chests(GameplaySimulation c) {
            calls.add("chests");
        }

        @Override
        public void progress(GameplaySimulation c) {
            calls.add("progress");
        }

        @Override
        public void witch(GameplaySimulation c) {
            calls.add("witch");
        }

        @Override
        public void dialogs(GameplaySimulation c) {
            calls.add("dialogs");
        }

        @Override
        public void finalEffects(GameplaySimulation c) {
            calls.add("finalEffects");
        }

        @Override
        public void combat(GameplaySimulation c, List<EntityProjectileEvent> events) {
            assertSame(this.events, events);
            calls.add("combat");
        }

        @Override
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
