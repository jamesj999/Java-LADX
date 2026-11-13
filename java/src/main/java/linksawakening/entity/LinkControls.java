package linksawakening.entity;

import static linksawakening.entity.LinkRomConstants.JOY_DOWN;
import static linksawakening.entity.LinkRomConstants.JOY_LEFT;
import static linksawakening.entity.LinkRomConstants.JOY_RIGHT;
import static linksawakening.entity.LinkRomConstants.JOY_UP;

import java.util.Objects;

/** Link controls state and operations used by ROM {@code ApplyLinkMotionState}. */
final class LinkControls {
    private final LinkComponentRef<LinkServices> linkServices;

    LinkControls(Dependencies dependencies) {
        linkServices = Objects.requireNonNull(dependencies.linkServices, "linkServices");
    }

    static final class Dependencies {
        LinkComponentRef<LinkServices> linkServices;
    }

    boolean accessButtonWasPressed(int key) {
        return key >= 0 && linkServices.get().inputState.wasPressed(key);
    }

    boolean accessPlayerHasFlippers() {
        return linkServices.get().playerState.progression().hasFlippers();
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

        return accessBuildJoypadMask();
    }
}
