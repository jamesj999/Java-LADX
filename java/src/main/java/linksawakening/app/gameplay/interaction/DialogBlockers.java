package linksawakening.app.gameplay.interaction;

import linksawakening.gameplay.OverworldDialogBlockers;

/**
 * Live input and simulation gates used by dialog interactions. Implementations are connected when the owning
 * session is wired.
 */
public interface DialogBlockers {
    OverworldDialogBlockers current();
}
