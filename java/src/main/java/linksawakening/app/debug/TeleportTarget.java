package linksawakening.app.debug;

import linksawakening.world.transition.Warp;

/** World-entry request made by the debug menu. */
interface TeleportTarget {
    void teleport(Warp destination);
}
