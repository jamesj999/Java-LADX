package linksawakening.app.gameplay;

import java.util.List;

/**
 * Ordered gameplay frame. Simulation includes its dialog/story gates; completion
 * still runs when a gate ends simulation early. Exceptions do not advance input.
 */
public final class GameplayFrame {
    public enum Phase { TRANSITIONS, SIMULATION, INPUT_COMPLETION }

    public static List<Phase> phaseOrder() {
        return List.of(Phase.TRANSITIONS, Phase.SIMULATION, Phase.INPUT_COMPLETION);
    }

    private final Runnable transitions;
    private final Runnable simulation;
    private final Runnable inputCompletion;

    public GameplayFrame(Runnable transitions, Runnable simulation, Runnable inputCompletion) {
        this.transitions = transitions;
        this.simulation = simulation;
        this.inputCompletion = inputCompletion;
    }

    public void tick() {
        for (Phase phase : phaseOrder()) {
            switch (phase) {
                case TRANSITIONS -> transitions.run();
                case SIMULATION -> simulation.run();
                case INPUT_COMPLETION -> inputCompletion.run();
            }
        }
    }
}
