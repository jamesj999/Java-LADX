package linksawakening.world.entity.event;

public enum FloatingItemPickupEffect {
    TEN_RUPEES,
    MAGIC_POWDER,
    TEN_BOMBS,
    HEALTH_18,
    TEN_ARROWS;

public static FloatingItemPickupEffect pickupEffect(int entityType, int sourceVariant) {
    validateEntityType(entityType);
    if (sourceVariant < 0 || sourceVariant > 0xFF) {
        throw new IllegalArgumentException("Floating item source variant must be a byte: "
            + sourceVariant);
    }
    if (entityType == 0x86) {
        return switch (sourceVariant) {
            case 0, 3 -> FloatingItemPickupEffect.TEN_RUPEES;
            case 1 -> FloatingItemPickupEffect.MAGIC_POWDER;
            case 2 -> FloatingItemPickupEffect.TEN_BOMBS;
            default -> throw invalidVariant(entityType, sourceVariant);
        };
    }
    return switch (sourceVariant) {
        case 4 -> FloatingItemPickupEffect.HEALTH_18;
        case 5 -> FloatingItemPickupEffect.TEN_ARROWS;
        default -> throw invalidVariant(entityType, sourceVariant);
    };
}

private static IllegalArgumentException invalidVariant(int entityType, int sourceVariant) {
    return new IllegalArgumentException("Invalid floating item variant 0x"
        + Integer.toHexString(sourceVariant) + " for entity 0x"
        + Integer.toHexString(entityType));
}

public static void validateEntityType(int entityType) {
    if (entityType != 0x86 && entityType != 0xE5) {
        throw new IllegalArgumentException("Not a floating item entity type: 0x"
            + Integer.toHexString(entityType));
    }
}

}
