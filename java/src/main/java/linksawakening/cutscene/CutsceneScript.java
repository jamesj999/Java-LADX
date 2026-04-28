package linksawakening.cutscene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CutsceneScript {

    private final List<CutsceneStep> steps;

    private CutsceneScript(List<CutsceneStep> steps) {
        this.steps = List.copyOf(steps);
    }

    public static CutsceneScript of(CutsceneStep... steps) {
        return new CutsceneScript(Arrays.asList(steps));
    }

    List<CutsceneStep> instantiateSteps() {
        List<CutsceneStep> copy = new ArrayList<>(steps.size());
        for (CutsceneStep step : steps) {
            copy.add(step.fresh());
        }
        return copy;
    }
}
