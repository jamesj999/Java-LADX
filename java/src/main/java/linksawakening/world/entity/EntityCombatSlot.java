package linksawakening.world.entity;
/** Scratch values for exactly one slot's ordered combat pass. */
final class EntityCombatSlot {
    final EntityCombatResolverServices runtime;
    EnemyCombatFrame inputs;
    java.util.List<linksawakening.world.entity.event.EntityCombatEvent> events;
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

    EntityCombatSlot(EntityCombatResolverServices runtime) {
        this.runtime = runtime;
    }

    void reset(
            EnemyCombatFrame inputs,
            java.util.List<linksawakening.world.entity.event.EntityCombatEvent> events,
            RoomEntity entity) {
        this.inputs = inputs;
        this.events = events;
        this.entity = entity;
        this.booBuddyAllowsLinkCollision = false;
        this.booBuddyAllowsSwordCollision = false;
        this.peaHatGrounded = false;
        this.hidingZolSwordCollision = false;
        this.hidingZolLinkCollision = false;
        this.collisionFlashCountdown = 0;
        this.goombaStomp = false;
        this.linkCollision = false;
        this.swordHit = false;
        this.moldormTailSwordHit = false;
        this.soundChannel = null;
        this.soundId = 0;
        this.secondarySoundChannel = null;
        this.secondarySoundId = 0;
        this.enemyDamage = 0;
        this.enemySpecialAction = 0;
        this.swordPokeVfx = null;
        this.peaHatSwordClink = false;
        this.spikedBeetleSwordClink = false;
        this.maskedMimicSwordClink = false;
        this.rollingBonesBarSwordClink = false;
        this.moblinKingSwordClink = false;
        this.linkDamage = 0;
        this.linkCollisionResponse = null;
        this.bodyRecoilBeforeSword = false;
        this.swordResult = null;
        this.swordDamage = 0;
        this.swordResultApplied = false;
    }
}
