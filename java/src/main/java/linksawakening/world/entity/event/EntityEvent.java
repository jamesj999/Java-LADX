package linksawakening.world.entity.event;

/** Outbound entity effects, retaining their ROM handler payloads. */
public sealed interface EntityEvent
    permits EntityRequestBridge,
        EntityCombatEvent,
        BombExplosionEvent,
        EntityProjectileEvent {}
