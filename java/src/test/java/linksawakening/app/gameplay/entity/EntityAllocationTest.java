package linksawakening.app.gameplay.entity;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.app.AllocationSessions;
import linksawakening.equipment.Sword;
import linksawakening.testing.AllocationProbe;
import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;

/** Isolates the immutable entity frame inputs from the world entity tick. */
@RequiresRom
class EntityAllocationTest {
    private static volatile Object observed;

    @Test
    void frameInputsAllocation() throws Exception {
        try (var session = AllocationSessions.active()) {
            var c = session.flows().simulation();
            var sword = Sword.CollisionBox.inactive();
            var inputs = new EntityFrameInputsFactory();
            double bytes = AllocationProbe.bytesPerIteration(600, 120, () -> {
                session.frameClock().advance();
                observed = inputs.create(c, sword);
            });
            System.out.println("Entity inputs bytes/frame: " + bytes);
            assertTrue(bytes <= 50, "Entity frame input bytes: " + bytes);
        }
    }

    @Test
    void resultApplicationWithoutWorldTickAllocation() throws Exception {
        try (var session = AllocationSessions.active()) {
            var c = session.flows().simulation();
            var phase = new EntityPhase(
                    c.roomSession(), c.link(), session.gameplaySounds(), c.transientVfxSystem(), c.musicDirector());
            var events = java.util.List.<linksawakening.world.entity.event.EntityProjectileEvent>of();
            double bytes = AllocationProbe.bytesPerIteration(600, 120, () -> phase.applyResults(c, events));
            System.out.println("Entity results without world tick bytes/frame: " + bytes);
            assertTrue(bytes <= 350, "Entity result application bytes: " + bytes);
        }
    }
}
