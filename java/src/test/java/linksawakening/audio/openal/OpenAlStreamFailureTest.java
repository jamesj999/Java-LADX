package linksawakening.audio.openal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class OpenAlStreamFailureTest {
    @Test
    void unavailableDeviceHasClearStatusAndNoOwnedResources() {
        var stream = new OpenAlStream();
        stream.initializeOpenAl(() -> 0);
        assertFalse(stream.available);
        assertTrue(stream.statusMessage.contains("OpenAL unavailable: could not open device"));
        assertEquals(0, stream.device);
        assertEquals(0, stream.context);
        assertEquals(0, stream.source);
        assertNull(stream.releaseNativeResources());
    }

    @Test
    void missingNativeLibraryAlsoFallsBackSilently() {
        var stream = new OpenAlStream();
        stream.initializeOpenAl(() -> {
            throw new UnsatisfiedLinkError("missing fake library");
        });
        assertFalse(stream.available);
        assertTrue(stream.statusMessage.contains("missing fake library"));
        assertNull(stream.releaseNativeResources());
    }

    @Test
    void linkageFailureDuringCleanupIsRememberedAndLaterCleanupCanRun() {
        var first = OpenAlStream.rememberFailure(null, () -> {
            throw new UnsatisfiedLinkError("fake cleanup");
        });
        assertInstanceOf(UnsatisfiedLinkError.class, first.getCause());
        var second = OpenAlStream.rememberFailure(first, () -> {
            throw new IllegalStateException("next cleanup");
        });
        assertSame(first, second);
        assertEquals(1, first.getSuppressed().length);
    }
}
