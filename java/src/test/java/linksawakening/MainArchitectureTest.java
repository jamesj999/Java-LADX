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

    @Test
    void mainDoesNotOwnRoomLoadingOrEdgeScrolling() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));

        assertFalse(source.contains("private static void loadOverworldRoom"));
        assertFalse(source.contains("private static void loadIndoorRoom"));
        assertFalse(source.contains("private static void startScroll"));
        assertFalse(source.contains("private static void startIndoorScroll"));
        assertFalse(source.contains("private static void maybeTriggerEdgeScroll"));
        assertFalse(source.contains("private static void beginScrollWithLink"));
        assertFalse(source.contains("private static void maybeTriggerWarpTransition"));
        assertFalse(source.contains("private static void applyWarp"));
        assertFalse(source.contains("private static void handleRoomBoundaryAfterWarpChecks"));
        assertFalse(source.contains("private static int currentRoomId"));
        assertFalse(source.contains("private static int[] roomObjectsArea"));
    }
}
