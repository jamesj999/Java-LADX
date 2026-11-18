package linksawakening.app.gameplay.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.equipment.Sword;
import linksawakening.world.entity.event.EntityProjectileEvent;
import org.junit.jupiter.api.Test;

class EntityFrameOrderTest {
    static final class Recording implements EntityFrameStages {
        final List<String> calls = new ArrayList<>();
        final List<EntityProjectileEvent> events = List.of(EntityProjectileEvent.hookshotPull(1, 16, 0));
        final Sword.CollisionBox box = new Sword.CollisionBox(true, 1, 2, 3, 4);

        @Override
        public List<EntityProjectileEvent> tickEntities(Sword.CollisionBox b) {
            assertSame(box, b);
            calls.add("tick");
            return events;
        }

        @Override
        public void positions() {
            calls.add("positions");
        }

        @Override
        public void motion() {
            calls.add("motion");
        }

        @Override
        public void attacks() {
            calls.add("attacks");
        }

        @Override
        public void sword() {
            calls.add("sword");
        }

        @Override
        public void chests() {
            calls.add("chests");
        }

        @Override
        public void progress() {
            calls.add("progress");
        }

        @Override
        public void witch() {
            calls.add("witch");
        }

        @Override
        public void dialogs() {
            calls.add("dialogs");
        }

        @Override
        public void finalEffects() {
            calls.add("finalEffects");
        }

        @Override
        public void combat(List<EntityProjectileEvent> events) {
            assertSame(this.events, events);
            calls.add("combat");
        }

        @Override
        public void projectiles(List<EntityProjectileEvent> events) {
            assertSame(this.events, events);
            calls.add("projectiles");
        }
    }

    @Test
    void drainsWritesThenCombatRewardsDialogsAndProjectileResponses() {
        var r = new Recording();
        r.run(r.box);
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
        r.run(r.box);
        assertTrue(r.calls.indexOf("tick") < r.calls.indexOf("combat"));
        assertTrue(r.calls.indexOf("dialogs") < r.calls.indexOf("projectiles"));
    }

    @Test
    void entityAttackRequestsApplyAfterTheTickThatCanThrowABomb() {
        var r = new Recording();
        r.run(r.box);
        assertEquals(List.of("tick", "positions", "motion", "attacks"), r.calls.subList(0, 4));
    }
}
