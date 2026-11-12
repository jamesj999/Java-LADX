package linksawakening.app.gameplay;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import linksawakening.equipment.Sword;
import org.junit.jupiter.api.Test;

class SimulationOrderTest {
    static final class Recording implements SimulationStages {
        final List<String> calls = new ArrayList<>();
        boolean proceed = true, passingOut;
        boolean blocked = true;
        final Sword.CollisionBox box = new Sword.CollisionBox(true, 1, 2, 3, 4);

        public boolean dialogStory() {
            calls.add("story");
            return proceed;
        }

        public boolean dialogBlocks() {
            calls.add("blockers");
            return blocked;
        }

        public void linkEquipment(boolean dialog, boolean inventory) {
            assertEquals(blocked, dialog);
            assertTrue(inventory);
            calls.add("link");
        }

        public boolean passingOut() {
            calls.add("passout");
            return passingOut;
        }

        public Sword.CollisionBox combat(boolean dialog) {
            assertEquals(blocked, dialog);
            calls.add("combat");
            return box;
        }

        public void entities(Sword.CollisionBox sword) {
            assertSame(box, sword);
            calls.add("entities");
        }

        public void completeInput() {
            calls.add("cooldown");
        }

        public void animatedTiles(boolean dialog) {
            assertEquals(blocked, dialog);
            calls.add("tiles");
        }
    }

    @Test
    void simulationStagesRunInSourceOrderAndCarrySwordBox() {
        var r = new Recording();
        r.run(true);
        assertEquals(
                List.of("story", "blockers", "link", "passout", "combat", "entities", "cooldown", "tiles"), r.calls);
    }

    @Test
    void openingStoryStopsAllLaterSimulation() {
        var r = new Recording();
        r.proceed = false;
        r.run(true);
        assertEquals(List.of("story"), r.calls);
    }

    @Test
    void passingOutStopsBeforeCombatAndEntities() {
        var r = new Recording();
        r.passingOut = true;
        r.run(true);
        assertEquals(List.of("story", "blockers", "link", "passout"), r.calls);
    }
}
