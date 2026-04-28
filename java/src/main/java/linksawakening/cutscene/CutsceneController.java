package linksawakening.cutscene;

import java.util.List;
import java.util.Objects;

public final class CutsceneController {

    private static final int MAX_IMMEDIATE_STEPS_PER_TICK = 16;

    private final CutsceneContext context;
    private List<CutsceneStep> steps = List.of();
    private int stepIndex;
    private boolean active;

    public CutsceneController(CutsceneContext context) {
        this.context = Objects.requireNonNull(context);
    }

    public void start(CutsceneScript script) {
        steps = Objects.requireNonNull(script).instantiateSteps();
        stepIndex = 0;
        active = !steps.isEmpty();
    }

    public void tick() {
        if (!active) {
            return;
        }

        int immediateSteps = 0;
        while (active && immediateSteps < MAX_IMMEDIATE_STEPS_PER_TICK) {
            CutsceneStep step = steps.get(stepIndex);
            if (!step.tick(context)) {
                return;
            }
            stepIndex++;
            immediateSteps++;
            if (stepIndex >= steps.size()) {
                active = false;
            }
        }
    }

    public boolean isActive() {
        return active;
    }
}
