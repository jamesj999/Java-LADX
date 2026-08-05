package linksawakening.world;

/** A single room-entity combat result produced by the ROM collision pass. */
public record EntityCombatEvent(int slot, int type, int linkDamage, boolean swordHit,
                                SoundChannel soundChannel, int soundId) {

    public EntityCombatEvent(int slot, int type, int linkDamage, boolean swordHit) {
        this(slot, type, linkDamage, swordHit, SoundChannel.NONE, -1);
    }

    public EntityCombatEvent {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if ((type & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + type);
        }
        if (linkDamage < 0) {
            throw new IllegalArgumentException("Link damage cannot be negative: " + linkDamage);
        }
        if (soundChannel == null) {
            throw new IllegalArgumentException("Combat sound channel cannot be null");
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

    /** Raw Game Boy sound request written by the combat handler. */
    public enum SoundChannel {
        NONE,
        JINGLE,
        WAVE,
        NOISE
    }
}
