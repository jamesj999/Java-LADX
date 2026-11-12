package linksawakening.app.host;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class HostResourcesTest {
    @Test
    void startupFailureAtEveryStageReleasesAllEarlierResourcesInReverseOrder() {
        for (int failAt = 0; failAt < 5; failAt++) {
            var resources = new HostResources();
            var closed = new ArrayList<Integer>();
            var failure = new IllegalStateException("startup");
            int stage = failAt;
            assertSame(
                    failure,
                    assertThrows(
                            IllegalStateException.class,
                            () -> resources.initialize(() -> {
                                for (int i = 0; i < 5; i++) {
                                    if (i == stage) throw failure;
                                    int id = i;
                                    resources.add(() -> closed.add(id));
                                }
                            })));
            var expected = new ArrayList<Integer>();
            for (int i = failAt - 1; i >= 0; i--) expected.add(i);
            assertEquals(expected, closed);
            resources.close();
            assertEquals(expected, closed);
        }
    }

    @Test
    void cleanupErrorsDoNotHideStartupFailureOrSkipLaterReleases() {
        var resources = new HostResources();
        var events = new ArrayList<String>();
        var primary = new IllegalStateException("startup");
        resources.add(() -> events.add("device"));
        resources.add(() -> {
            events.add("window");
            throw new UnsatisfiedLinkError("cleanup");
        });
        assertSame(
                primary,
                assertThrows(
                        IllegalStateException.class,
                        () -> resources.initialize(() -> {
                            throw primary;
                        })));
        assertEquals(List.of("window", "device"), events);
        assertEquals(1, primary.getSuppressed().length);
    }

    @Test
    void successfulOwnershipIsClosedOnceAndRejectsNewAcquisitions() {
        var resources = new HostResources();
        var closed = new ArrayList<Integer>();
        resources.initialize(() -> resources.add(() -> closed.add(1)));
        resources.close();
        resources.close();
        assertEquals(List.of(1), closed);
        assertThrows(IllegalStateException.class, () -> resources.add(() -> {}));
    }

    @Test
    void repeatedCleanupExceptionStillReleasesRemainingResources() {
        var resources = new HostResources();
        var events = new ArrayList<Integer>();
        var shared = new IllegalStateException("shared failure");
        resources.add(() -> events.add(1));
        resources.add(() -> {
            throw shared;
        });
        resources.add(() -> {
            throw shared;
        });
        assertSame(shared, assertThrows(IllegalStateException.class, resources::close));
        assertEquals(List.of(1), events);
    }
}
