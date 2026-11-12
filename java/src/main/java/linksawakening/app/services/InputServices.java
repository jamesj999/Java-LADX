package linksawakening.app.services;

import java.util.Objects;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;

/** Long-lived input services constructed before the first frame. */
public record InputServices(InputConfig inputConfig, InputState inputState) {
    public InputServices {
        Objects.requireNonNull(inputConfig);
        Objects.requireNonNull(inputState);
    }
}
