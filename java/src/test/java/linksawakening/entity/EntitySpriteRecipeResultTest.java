package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EntitySpriteRecipeResultTest {
    @Test
    void explicitlyUnsupportedRecipeStopsLookupWhileNoMatchContinues() {
        assertFalse(EntitySpriteRecipe.NO_MATCH.matched());
        assertTrue(EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.UNSUPPORTED, 0xFF)
                .matched());
    }
}
