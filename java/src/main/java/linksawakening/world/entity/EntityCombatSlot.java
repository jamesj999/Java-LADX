package linksawakening.world.entity;
/** Scratch values for exactly one slot's ordered combat pass. */
final class EntityCombatSlot {
    final EntityCombatResolverServices runtime;
    final EnemyCombatFrame inputs;
    final java.util.List<linksawakening.world.entity.event.EntityCombatEvent> events;
    linksawakening.world.entity.RoomEntity entity;
    boolean booBuddyAllowsLinkCollision;
    boolean booBuddyAllowsSwordCollision;
    boolean peaHatGrounded;
    boolean hidingZolSwordCollision;
    boolean hidingZolLinkCollision;
    int collisionFlashCountdown;
    boolean goombaStomp;
    boolean linkCollision;
    boolean swordHit;
    boolean moldormTailSwordHit;
    linksawakening.world.entity.event.EntityCombatEvent.SoundChannel soundChannel;
    int soundId;
    linksawakening.world.entity.event.EntityCombatEvent.SoundChannel secondarySoundChannel;
    int secondarySoundId;
    int enemyDamage;
    int enemySpecialAction;
    linksawakening.world.entity.event.EntityCombatEvent.SwordPokeVfx swordPokeVfx;
    boolean peaHatSwordClink;
    boolean spikedBeetleSwordClink;
    boolean maskedMimicSwordClink;
    boolean rollingBonesBarSwordClink;
    boolean moblinKingSwordClink;
    int linkDamage;
    linksawakening.world.entity.event.EntityCombatEvent.LinkCollisionResponse linkCollisionResponse;
    boolean bodyRecoilBeforeSword;
    linksawakening.world.entity.RomEnemyCombatTables.SwordDamageResult swordResult;
    int swordDamage;
    boolean swordResultApplied;

    EntityCombatSlot(
            EntityCombatResolverServices runtime,
            EnemyCombatFrame inputs,
            java.util.List<linksawakening.world.entity.event.EntityCombatEvent> events,
            RoomEntity entity) {
        this.runtime = runtime;
        this.inputs = inputs;
        this.events = events;
        this.entity = entity;
    }
}
