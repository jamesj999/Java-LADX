package linksawakening.app.state;

import java.util.Objects;
import linksawakening.save.SaveRamStore;

/**
 * The replaceable save store used by file operations and headless persistence setup. One instance is
 * retained for the owning session.
 */
public final class SaveState {
    public SaveRamStore saveRamStore;

    public SaveState(SaveRamStore store) {
        saveRamStore = Objects.requireNonNull(store);
    }
}
