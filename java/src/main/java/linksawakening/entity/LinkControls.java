package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

/** Link controls state and operations used by ROM {@code ApplyLinkMotionState}. */
final class LinkControls {
    private final LinkComponentRef<LinkServices> linkServices;

    LinkControls(Dependencies dependencies) {
        linkServices = dependencies.linkServices;
    }

    static final class Dependencies {
        LinkComponentRef<LinkServices> linkServices;
    }

    boolean accessButtonWasPressed(int key) {
        return linkServices.get().inputState != null
                && key >= 0
                && linkServices.get().inputState.wasPressed(key);
    }

    boolean accessPlayerHasFlippers() {
        return linkServices.get().playerState != null
                && linkServices.get().playerState.progression().hasFlippers();
    }

    int accessBuildJoypadMask() {
        int mask = 0;
        if (linkServices.get().inputState.isDown(linkServices.get().inputConfig.downKey())) mask |= JOY_DOWN;
        if (linkServices.get().inputState.isDown(linkServices.get().inputConfig.upKey())) mask |= JOY_UP;
        if (linkServices.get().inputState.isDown(linkServices.get().inputConfig.leftKey())) mask |= JOY_LEFT;
        if (linkServices.get().inputState.isDown(linkServices.get().inputConfig.rightKey())) mask |= JOY_RIGHT;
        return mask;
    }

    int accessRomPressedButtonsMask() {
        if (linkServices.get().inputState == null || linkServices.get().inputConfig == null) {
            return 0;
        }
        return accessBuildJoypadMask();
    }
}
