package linksawakening.world;

/** Outbound entity effects, retaining their ROM handler payloads. */
public sealed interface EntityEvent
    permits EntityRequestBridge,
        EntityCombatEvent,
        BombExplosionEvent,
        EntityProjectileEvent,
        RoamingEnemyMotion.LaunchRequest,
        HeartPiecePickupMotion.Update {}
