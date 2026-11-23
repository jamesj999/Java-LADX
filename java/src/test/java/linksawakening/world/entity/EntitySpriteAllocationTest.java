package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.AllocationProbe;
import linksawakening.testing.TestRoms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

class EntitySpriteAllocationTest {
    private static volatile Object retained;

    @Test
    void recurringDisplayListsAvoidDefinitionAndDynamicVariantCopies(TestReporter reporter) {
        var catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(new byte[0x100000]));
        var cache = new EntitySpriteViews(catalog);
        double owned = AllocationProbe.bytesPerIteration(4000, 1000, () -> retained = catalog.forFallingEntity());
        double cached = AllocationProbe.bytesPerIteration(4000, 1000, () -> retained = cache.forFallingEntity());
        reporter.publishEntry("fallingDefinitionDecodeBytes", Double.toString(owned));
        reporter.publishEntry("cachedFallingDefinitionBytes", Double.toString(cached));
        assertEquals(0.0, cached);
        var original = catalog.forFallingEntity();
        var reused = cache.forFallingEntity();
        assertEquals(original.dynamicVariants(), reused.dynamicVariants());
        assertEquals(original.variantCount(), reused.variantCount());
        assertSame(reused, cache.forFallingEntity());
    }

    @Test
    void displayKeysKeepEveryColorShellStateAndStatusDistinct() {
        var catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(new byte[0x100000]));
        var cache = new EntitySpriteViews(catalog);
        for (EntityStatus status : EntityStatus.values()) {
            for (int state = 0; state <= 13; state++) {
                EntitySpriteDefinition owned = catalog.forColorShellState(0xE9, state, status);
                EntitySpriteDefinition view = cache.forColorShellState(0xE9, state, status);
                assertEquals(owned.shape(), view.shape());
                assertEquals(owned.rectangleVariants(), view.rectangleVariants());
                assertSame(view, cache.forColorShellState(0xE9, state, status));
            }
        }
    }
}
