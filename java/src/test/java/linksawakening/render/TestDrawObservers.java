package linksawakening.render;

/** Test-only scoped service that attaches parity inspection to existing session replay tests. */
public final class TestDrawObservers implements FrameDrawObserver.Provider {
    static final ThreadLocal<FrameDrawObserver> CURRENT = new ThreadLocal<>();

    @Override
    public FrameDrawObserver current() {
        return CURRENT.get();
    }
}
