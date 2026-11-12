package linksawakening.render;

/** Test-only explicit observer scope for standalone scene draws. */
final class TestDrawObservers {
    static final ThreadLocal<FrameDrawObserver> CURRENT = new ThreadLocal<>();

    private TestDrawObservers() {}
}
