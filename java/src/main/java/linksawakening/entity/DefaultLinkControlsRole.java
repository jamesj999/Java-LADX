package linksawakening.entity;

/** Delegates controls operations to state owners used by ROM {@code ApplyLinkMotionState}. */
final class DefaultLinkControlsRole implements LinkControlsRole {
    private final LinkControls linkControls;

    DefaultLinkControlsRole(LinkAssembly assembly) {
        linkControls = assembly.linkControls.get();
    }

    @Override
    public boolean buttonWasPressed(int key) {
        return linkControls.accessButtonWasPressed(key);
    }

    @Override
    public boolean playerHasFlippers() {
        return linkControls.accessPlayerHasFlippers();
    }

    @Override
    public int buildJoypadMask() {
        return linkControls.accessBuildJoypadMask();
    }

    @Override
    public int romPressedButtonsMask() {
        return linkControls.accessRomPressedButtonsMask();
    }
}
