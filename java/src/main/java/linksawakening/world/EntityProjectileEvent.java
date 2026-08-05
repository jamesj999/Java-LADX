package linksawakening.world;

/**
 * An interaction emitted by one of the enemy projectile handlers.
 *
 * <p>The event retains the raw ROM collision value and sound request instead
 * of coupling the room runtime to the player's health or audio sinks.  The
 * gameplay boundary can therefore apply the existing Link state rules after
 * the entity pass has completed.</p>
 */
public record EntityProjectileEvent(
    int slot,
    int type,
    Kind kind,
    int collisionValue,
    int linkDamage,
    SoundChannel soundChannel,
    int soundId,
    boolean remove,
    boolean swordPokeVfx) {

    public EntityProjectileEvent {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if ((type & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + type);
        }
        if (kind == null) {
            throw new IllegalArgumentException("Projectile event kind cannot be null");
        }
        if ((collisionValue & ~0xFF) != 0) {
            throw new IllegalArgumentException("Collision value must be an unsigned byte: "
                + collisionValue);
        }
        if (linkDamage < 0) {
            throw new IllegalArgumentException("Link damage cannot be negative: " + linkDamage);
        }
        if (soundChannel == null) {
            throw new IllegalArgumentException("Projectile sound channel cannot be null");
        }
        if (soundId < -1 || soundId > 0xFF) {
            throw new IllegalArgumentException("Sound id must be -1 or an unsigned byte: "
                + soundId);
        }
        if (soundChannel == SoundChannel.NONE && soundId != -1) {
            throw new IllegalArgumentException("A sound id requires a sound channel");
        }
        if (soundChannel != SoundChannel.NONE && soundId == -1) {
            throw new IllegalArgumentException("A sound channel requires a sound id");
        }
    }

    public enum Kind {
        SHIELD_BLOCK,
        LINK_DAMAGE
    }

    /** Raw Game Boy sound request channel used by the later gameplay router. */
    public enum SoundChannel {
        NONE,
        JINGLE,
        WAVE,
        NOISE
    }

    /** Whether the ROM's collision byte will send the projectile into bounce/gravity. */
    public boolean startsWallTransition() {
        return collisionValue != 0;
    }
}
