package linksawakening.app.gameplay.recovery;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LinkPassOutAnimationTest {
    @Test
    void triggerHonorsEveryGate() {
        assertTrue(LinkPassOutAnimation.shouldStart(0, 0, 0, false, false, false));
        assertFalse(LinkPassOutAnimation.shouldStart(1, 0, 0, false, false, false));
        assertFalse(LinkPassOutAnimation.shouldStart(0, 1, 0, false, false, false));
        assertFalse(LinkPassOutAnimation.shouldStart(0, 0, 1, false, false, false));
        assertFalse(LinkPassOutAnimation.shouldStart(0, 0, 0, true, false, false));
        assertFalse(LinkPassOutAnimation.shouldStart(0, 0, 0, false, true, false));
        assertFalse(LinkPassOutAnimation.shouldStart(0, 0, 0, false, false, true));
    }

    @Test
    void countdownDoesNotWrapAndHidesAtCompletion() {
        var animation = new LinkPassOutAnimation(linksawakening.testing.TestRoms.partial(new byte[0x8000]));
        assertEquals(191, animation.countdown());
        for (int i = 0; i < 190; i++) animation.advanceFrame();
        assertFalse(animation.finished());
        animation.advanceFrame();
        assertTrue(animation.finished());
        assertEquals(255, animation.animationState());
        animation.advanceFrame();
        assertEquals(0, animation.countdown());
    }
}
