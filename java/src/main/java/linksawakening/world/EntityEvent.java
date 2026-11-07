package linksawakening.world;

import linksawakening.world.entity.motion.HeartPiecePickupMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;

/** Outbound entity effects, retaining their ROM handler payloads. */
public sealed interface EntityEvent
    permits EntityRequestBridge,
        EntityCombatEvent,
        BombExplosionEvent,
        EntityProjectileEvent {}
