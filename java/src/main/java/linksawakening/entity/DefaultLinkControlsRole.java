package linksawakening.entity;

import static linksawakening.entity.Link.*;

/** Delegates controls operations to their state-owning ROM implementations. */
final class DefaultLinkControlsRole implements LinkControlsRole {
    private final LinkControls linkControls;

    DefaultLinkControlsRole(LinkAssembly assembly) {
        linkControls = assembly.linkControls.get();
    }

    public boolean buttonWasPressed(int key) {
        return linkControls.accessButtonWasPressed(key);
    }

    public boolean playerHasFlippers() {
        return linkControls.accessPlayerHasFlippers();
    }

    public int buildJoypadMask() {
        return linkControls.accessBuildJoypadMask();
    }

    public int romPressedButtonsMask() {
        return linkControls.accessRomPressedButtonsMask();
    }
}
