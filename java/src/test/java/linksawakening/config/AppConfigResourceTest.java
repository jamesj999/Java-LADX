package linksawakening.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

final class AppConfigResourceTest {
    @Test
    void missingResourceKeepsDocumentedDefaults() {
        ClassLoader missing = new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return null;
            }
        };
        var config = AppConfig.loadFromResources(missing);
        assertEquals(AppConfig.defaults().startMode(), config.startMode());
        assertTrue(config.showTitleScreen());
    }

    @Test
    void unreadableResourceKeepsDocumentedDefaultsAndClosesTheStream() {
        boolean[] closed = {false};
        ClassLoader broken = new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return new InputStream() {
                    public int read() throws IOException {
                        throw new IOException("fake read failure");
                    }

                    public void close() {
                        closed[0] = true;
                    }
                };
            }
        };
        assertEquals(
                AppConfig.defaults().itemProfile(),
                AppConfig.loadFromResources(broken).itemProfile());
        assertTrue(closed[0]);
    }
}
