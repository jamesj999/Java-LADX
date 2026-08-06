package linksawakening;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void mainConsumesProjectileEventsAtTheEntityFrameBoundary() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));

        assertTrue(source.contains("tickEntitiesWithProjectileEvents"));
        assertTrue(source.contains("link.romCollisionType()"));
        assertTrue(source.contains("EnemyProjectileEventConsumer.consume"));
    }

    @Test
    void mainAppliesHookshotPullEventsWithoutProjectileIgnoreSideEffects() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        String normalizedSource = source.replaceAll("\\s+", " ");

        assertTrue(source.contains("EntityProjectileEvent.Kind.HOOKSHOT_PULL"));
        assertTrue(normalizedSource.contains(
            "if (hookshotPull) { link.applyRomFinalPosition(event.linkSpeedX(), event.linkSpeedY()); "
                + "continue; } link.applyRomSpeed(event.linkSpeedX(), event.linkSpeedY());"));
        assertTrue(source.contains("link.applyRomFinalPosition(event.linkSpeedX(), event.linkSpeedY())"));
        assertTrue(source.contains("link.applyRomSpeed(event.linkSpeedX(), event.linkSpeedY())"));
    }

    @Test
    void mainRegistersBombWithLinkRomPlacementState() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));

        assertTrue(source.contains("PlayerState.INVENTORY_BOMBS"));
        assertTrue(source.contains("new Bomb("));
        assertTrue(source.contains("roomSession.placeBomb("));
        assertTrue(source.contains("link.romEntityX()"));
        assertTrue(source.contains("link.romEntityY()"));
        assertTrue(source.contains("link.romEntityZ()"));
        assertTrue(source.contains("romDirectionForLink(link.direction())"));
        assertTrue(source.contains("link.isAirborne()"));
    }
}
