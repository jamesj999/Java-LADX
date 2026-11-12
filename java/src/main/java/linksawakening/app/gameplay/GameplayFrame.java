package linksawakening.app.gameplay;

import java.util.List;
import linksawakening.app.screen.InputCompletion;

/**
 * Ordered gameplay frame. Simulation includes its dialog/story gates; completion still runs when a gate ends
 * simulation early. Exceptions do not advance input. One instance is retained for the owning session.
 */
public final class GameplayFrame {
    /** Transition advancement performed before simulation. */
    public interface Transitions {
        void run();
    }
    /** Ordered simulation step. */
    public interface Simulation {
        void tick();
    }

    /** Observable order of the retained gameplay frame phases. */
    public enum Phase {
        TRANSITIONS,
        SIMULATION,
        INPUT_COMPLETION
    }

    public static List<Phase> phaseOrder() {
        return List.of(Phase.TRANSITIONS, Phase.SIMULATION, Phase.INPUT_COMPLETION);
    }

    private final Transitions transitions;
    private final Simulation simulation;
    private final InputCompletion inputCompletion;

    public GameplayFrame(Transitions transitions, Simulation simulation, InputCompletion inputCompletion) {
        this.transitions = transitions;
        this.simulation = simulation;
        this.inputCompletion = inputCompletion;
    }

    public void tick() {
        transitions.run();
        simulation.tick();
        inputCompletion.complete();
    }
}
