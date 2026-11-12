package linksawakening.render;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteSelection;

/** Immutable selection metadata indexed once by ROM byte id, avoiding per-entity boxed map lookups. */
final class EntitySelectionSprites {
    private EntitySpriteSelection selection;
    private final EntitySpriteDefinition[] overrides = new EntitySpriteDefinition[256],
            overlays = new EntitySpriteDefinition[256];

    void bind(EntitySpriteSelection next) {
        if (selection == next) return;
        selection = next;
        for (int i = 0; i < 256; i++) {
            overrides[i] = next == null ? null : next.spriteOverrideFor(i);
            overlays[i] = next == null ? null : next.spriteOverlayFor(i);
        }
    }

    EntitySpriteDefinition override(int type) {
        return type < 0 || type >= 256 ? null : overrides[type];
    }

    EntitySpriteDefinition overlay(int type) {
        return type < 0 || type >= 256 ? null : overlays[type];
    }
}
