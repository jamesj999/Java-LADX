package linksawakening.app.gameplay;

import linksawakening.gameplay.OverworldDialogBlockers;

/** Live input and simulation gates used by dialog interactions. */
public interface DialogBlockers {
    OverworldDialogBlockers current();
}
