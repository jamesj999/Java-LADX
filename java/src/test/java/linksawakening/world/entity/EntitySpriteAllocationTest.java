package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.testing.AllocationProbe;
import linksawakening.testing.TestRoms;
import org.junit.jupiter.api.Test;

class EntitySpriteAllocationTest {
    private static volatile Object retained;

    @Test
    void recurringDisplayListsAvoidDefinitionAndDynamicVariantCopies() {
        var catalog = new EntitySpriteHandlerCatalog(TestRoms.partial(new byte[0x100000]));
        var cache = new EntitySpriteViews(catalog);
        double owned = AllocationProbe.bytesPerIteration(4000, 1000, () -> retained = catalog.forFallingEntity());
        double cached = AllocationProbe.bytesPerIteration(4000, 1000, () -> retained = cache.forFallingEntity());
        System.out.println("Falling definition decode / cached bytes: " + owned + " / " + cached);
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
