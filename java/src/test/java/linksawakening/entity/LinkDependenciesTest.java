package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import linksawakening.testing.TestLinks;
import org.junit.jupiter.api.Test;

class LinkDependenciesTest {
    @Test
    void defaultActorHasRealServicesAndRemainsStillInAnOpenWorld() {
        Link link = TestLinks.link().build();
        assertNotNull(link.linkServices.playerState);
        assertNotNull(link.linkServices.itemRegistry);
        assertNotNull(link.linkServices.inputState);
        assertNotNull(link.linkServices.inputConfig);
        assertNotNull(link.linkServices.spriteSheet);
        assertEquals(0xFF, link.linkServices.collision.objectIdAtPoint(64, 64));
        link.position().setPixelPosition(64, 64);
        for (int frame = 0; frame < 60; frame++) link.update();
        assertEquals(64, link.position().pixelX());
        assertEquals(64, link.position().pixelY());
    }

    @Test
    void strictConstructionRejectsMissingDependenciesBeforePublishingTheActor() {
        var services = TestLinks.link().build().linkServices;
        assertThrows(
                NullPointerException.class,
                () -> new LinkDependencies(
                        services.inputState,
                        services.inputConfig,
                        services.linkMotionTables,
                        services.swordTables,
                        services.collision,
                        services.spriteSheet,
                        null,
                        services.itemRegistry,
                        services.soundSink,
                        services.tunicPalette));
        assertThrows(NullPointerException.class, () -> TestLinks.link().player(null));
    }
}
