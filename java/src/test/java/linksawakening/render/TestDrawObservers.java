package linksawakening.render;

/** Test-only explicit observer scope for standalone scene draws. */
final class TestDrawObservers {
    static final ThreadLocal<FrameDrawObserver> CURRENT = ThreadLocal.withInitial(() -> FrameDrawObserver.NONE);

    private TestDrawObservers() {}
}
