package linksawakening.entity;
/** Immutable display-list layout or named specialized composition recipe; contains no ROM reader or owner callback. */
record EntitySpriteRecipe(
        Kind kind,
        int entityType,
        int bank,
        int address,
        int variantCount,
        int spriteCount,
        int initialVariant,
        EntitySpriteDefinition.DynamicSprite.TileSource tileSource) {
    enum Kind {
        PAIR,
        SINGLE,
        RECTANGLE,
        UNSUPPORTED,
        FLOATING_ITEM,
        COLOR_SHELL,
        ROCK,
        OWL,
        URCHIN,
        FISH,
        HIDING_ZOL,
        MOBLIN_SWORD,
        IRON_MASK,
        POKEY,
        DUNGEON_WARP,
        SLIME_EYE,
        MOLDORM,
        ROLLING_BAR,
        HOOKSHOT_CHAIN,
        OWL_STATUE
    }

    static EntitySpriteRecipe pair(int entity, int bank, int address, int variants, int initial) {
        return new EntitySpriteRecipe(
                Kind.PAIR,
                entity,
                bank,
                address,
                variants,
                2,
                initial,
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS);
    }

    static EntitySpriteRecipe single(int entity, int bank, int address, int variants, int initial) {
        return new EntitySpriteRecipe(
                Kind.SINGLE,
                entity,
                bank,
                address,
                variants,
                1,
                initial,
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS);
    }

    static EntitySpriteRecipe rectangle(int entity, int bank, int address, int variants, int sprites, int initial) {
        return new EntitySpriteRecipe(
                Kind.RECTANGLE,
                entity,
                bank,
                address,
                variants,
                sprites,
                initial,
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS);
    }

    static EntitySpriteRecipe special(Kind kind, int entity) {
        return new EntitySpriteRecipe(
                kind, entity, 0, 0, 0, 0, 0, EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS);
    }

    static EntitySpriteRecipe pair(
            int entity,
            int bank,
            int address,
            int variants,
            int initial,
            EntitySpriteDefinition.DynamicSprite.TileSource source) {
        return new EntitySpriteRecipe(Kind.PAIR, entity, bank, address, variants, 2, initial, source);
    }

    static EntitySpriteRecipe owlStatue(int entity, int bank, int address) {
        return new EntitySpriteRecipe(
                Kind.OWL_STATUE,
                entity,
                bank,
                address,
                1,
                2,
                0,
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS);
    }
}
