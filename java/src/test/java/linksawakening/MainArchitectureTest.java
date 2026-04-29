package linksawakening;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class MainArchitectureTest {

    @Test
    void mainDoesNotOwnRenderingBackendOrLayerAssembly() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));

        assertFalse(source.contains("GL11."));
        assertFalse(source.contains("GL12."));
        assertFalse(source.contains("GL15."));
        assertFalse(source.contains("GL20."));
        assertFalse(source.contains("GL30."));
        assertFalse(source.contains("addBackgroundSceneLayers"));
        assertFalse(source.contains("addOverworldLayers"));
        assertFalse(source.contains("uploadTexture"));
    }
}
