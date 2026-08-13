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
    void mainSuppliesLiveInventoryAndDialogTalkGatesBeforeEntityTick() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));

        int talkState = source.indexOf("roomSession.setEntityTalkState(");
        int entityTick = source.indexOf("roomSession.tickEntitiesWithProjectileEvents(");
        assertTrue(talkState >= 0, "Main must supply the live NPC talk gates");
        assertTrue(entityTick >= 0, "Main must tick the room entities");
        assertTrue(talkState < entityTick, "Talk gates must be supplied before entity handlers");
        assertTrue(source.contains("inventoryMenu.isTransitioning()"));
        assertTrue(source.contains("inventoryMenu.windowY()"));
        assertTrue(source.contains("dialogController.dialogCooldown()"));
    }

    @Test
    void mainCapturesLinkFinalPositionBeforeLinkMotionForEntityPushes() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));

        int capture = source.indexOf("link.captureRomFinalPosition();");
        int update = source.indexOf("link.update();");
        assertTrue(capture >= 0, "Link final position must be captured for entity pushes");
        assertTrue(update >= 0, "Main must run Link motion");
        assertTrue(capture < update,
            "The ROM final-position shadow must be captured before Link moves");
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
        String normalizedSource = source.replaceAll("\\s+", " ");

        assertTrue(source.contains("PlayerState.INVENTORY_BOMBS"));
        assertTrue(source.contains("new Bomb("));
        assertTrue(source.contains("roomSession.placeBomb("));
        assertTrue(source.contains("link.romEntityX()"));
        assertTrue(source.contains("link.romEntityY()"));
        assertTrue(source.contains("link.romEntityZ()"));
        assertTrue(source.contains("romDirectionForLink(link.direction())"));
        assertTrue(source.contains("link::canUseItems"));
        assertTrue(normalizedSource.contains(
            "if (placed) { link.startRomItemAttackStep(); } return placed;"));
        assertFalse(normalizedSource.contains(
            "roomSession.placeBomb( link.romEntityX(), link.romEntityY(), link.romEntityZ(), "
                + "romDirectionForLink(link.direction()), link.isAirborne(), false"));
    }

    @Test
    void mainStartsLinkItemAttackStepAfterASuccessfulBombThrow() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/Main.java"));
        String normalizedSource = source.replaceAll("\\s+", " ");

        assertTrue(normalizedSource.contains(
            "boolean bombBeingThrown = liftedState.type() == 0x02;"));
        assertTrue(normalizedSource.contains(
            "boolean thrown = roomSession.throwLiftedEntity(link.direction());"));
        assertTrue(normalizedSource.contains(
            "if (thrown && bombBeingThrown) { link.startRomItemAttackStep(); }"));
    }

    @Test
    void roomSessionExposesOnlyTheCanonicalBombPlacementBridge() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/world/RoomSession.java"));
        String normalizedSource = source.replaceAll("\\s+", " ");

        assertTrue(normalizedSource.contains(
            "public boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection)"));
        assertFalse(normalizedSource.contains(
            "public boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, "
                + "int romDirection, boolean"));
    }
}
