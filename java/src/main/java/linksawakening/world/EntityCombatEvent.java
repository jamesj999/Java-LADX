package linksawakening.world;

/** A single room-entity combat result produced by the ROM collision pass. */
public record EntityCombatEvent(int slot, int type, int linkDamage, boolean swordHit,
                                int enemyDamage, int enemySpecialAction,
                                SoundChannel soundChannel, int soundId,
                                SoundChannel secondarySoundChannel, int secondarySoundId,
                                SwordPokeVfx swordPokeVfx, LinkAction linkAction,
                                LinkCollisionResponse linkCollisionResponse) {

    public EntityCombatEvent(int slot, int type, int linkDamage, boolean swordHit,
                             int enemyDamage, int enemySpecialAction,
                             SoundChannel soundChannel, int soundId,
                             SoundChannel secondarySoundChannel, int secondarySoundId,
                             SwordPokeVfx swordPokeVfx) {
        this(slot, type, linkDamage, swordHit, enemyDamage, enemySpecialAction,
            soundChannel, soundId, secondarySoundChannel, secondarySoundId,
            swordPokeVfx, LinkAction.NONE, LinkCollisionResponse.NONE);
    }

    public EntityCombatEvent(int slot, int type, int linkDamage, boolean swordHit) {
        this(slot, type, linkDamage, swordHit, 0, -1,
            SoundChannel.NONE, -1, SoundChannel.NONE, -1, null, LinkAction.NONE,
            LinkCollisionResponse.NONE);
    }

    /** Compatibility constructor for callers that already provide raw sound. */
    public EntityCombatEvent(int slot, int type, int linkDamage, boolean swordHit,
                             SoundChannel soundChannel, int soundId) {
        this(slot, type, linkDamage, swordHit, 0, -1,
            soundChannel, soundId, SoundChannel.NONE, -1, null, LinkAction.NONE,
            LinkCollisionResponse.NONE);
    }

    /** Compatibility constructor for callers that provide combat data and one raw sound. */
    public EntityCombatEvent(int slot, int type, int linkDamage, boolean swordHit,
                             int enemyDamage, int enemySpecialAction,
                             SoundChannel soundChannel, int soundId) {
        this(slot, type, linkDamage, swordHit, enemyDamage, enemySpecialAction,
            soundChannel, soundId, SoundChannel.NONE, -1, null, LinkAction.NONE,
            LinkCollisionResponse.NONE);
    }

    /** Compatibility constructor for callers that provide both raw sounds. */
    public EntityCombatEvent(int slot, int type, int linkDamage, boolean swordHit,
                             int enemyDamage, int enemySpecialAction,
                             SoundChannel soundChannel, int soundId,
                             SoundChannel secondarySoundChannel, int secondarySoundId) {
        this(slot, type, linkDamage, swordHit, enemyDamage, enemySpecialAction,
            soundChannel, soundId, secondarySoundChannel, secondarySoundId,
            null, LinkAction.NONE, LinkCollisionResponse.NONE);
    }

    /** Compatibility constructor for handler-written Link actions. */
    public EntityCombatEvent(int slot, int type, int linkDamage, boolean swordHit,
                             int enemyDamage, int enemySpecialAction,
                             SoundChannel soundChannel, int soundId,
                             SoundChannel secondarySoundChannel, int secondarySoundId,
                             SwordPokeVfx swordPokeVfx, LinkAction linkAction) {
        this(slot, type, linkDamage, swordHit, enemyDamage, enemySpecialAction,
            soundChannel, soundId, secondarySoundChannel, secondarySoundId,
            swordPokeVfx, linkAction, LinkCollisionResponse.NONE);
    }

    /** Compatibility constructor for a generic Link collision response. */
    public EntityCombatEvent(int slot, int type, int linkDamage, boolean swordHit,
                             int enemyDamage, int enemySpecialAction,
                             SoundChannel soundChannel, int soundId,
                             SoundChannel secondarySoundChannel, int secondarySoundId,
                             SwordPokeVfx swordPokeVfx,
                             LinkCollisionResponse linkCollisionResponse) {
        this(slot, type, linkDamage, swordHit, enemyDamage, enemySpecialAction,
            soundChannel, soundId, secondarySoundChannel, secondarySoundId,
            swordPokeVfx, LinkAction.NONE, linkCollisionResponse);
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
        if (enemyDamage < 0) {
            throw new IllegalArgumentException("Enemy damage cannot be negative: " + enemyDamage);
        }
        if (enemySpecialAction < -1 || enemySpecialAction > 0xFF) {
            throw new IllegalArgumentException("Enemy special action must be -1 or an unsigned byte: "
                + enemySpecialAction);
        }
        if (enemyDamage > 0 && enemySpecialAction != -1) {
            throw new IllegalArgumentException("Numeric and special enemy damage cannot coexist");
        }
        if (linkAction == null) {
            throw new IllegalArgumentException("Link action cannot be null");
        }
        if (linkCollisionResponse == null) {
            throw new IllegalArgumentException("Link collision response cannot be null");
        }
        validateSoundPair(soundChannel, soundId, "Combat");
        validateSoundPair(secondarySoundChannel, secondarySoundId, "Secondary combat");
    }

    /** Source-shaped request for bank-$02's transient sword-poke renderer. */
    public record SwordPokeVfx(int worldX, int worldY) {
        public SwordPokeVfx {
            if ((worldX & ~0xFF) != 0 || (worldY & ~0xFF) != 0) {
                throw new IllegalArgumentException("Sword-poke coordinates must be unsigned bytes");
            }
        }
    }

    /** A handler-written response that must be applied to Link immediately. */
    public enum LinkAction {
        NONE,
        GOOMBA_BOUNCE_TOP_DOWN,
        GOOMBA_BOUNCE_SIDE_SCROLLING
    }

    /**
     * The generic ApplyLinkCollisionWithEnemy writes these Link HRAM values
     * after accepting a normal enemy collision.
     */
    public record LinkCollisionResponse(int speedX, int speedY,
                                        int ignoreCollisionCountdown) {
        public static final LinkCollisionResponse NONE = new LinkCollisionResponse(0, 0, 0);

        public LinkCollisionResponse {
            validateByte(speedX, "Link response X speed");
            validateByte(speedY, "Link response Y speed");
            validateByte(ignoreCollisionCountdown, "Link collision-ignore countdown");
        }

        public boolean active() {
            return speedX != 0 || speedY != 0 || ignoreCollisionCountdown != 0;
        }
    }

    private static void validateSoundPair(SoundChannel channel, int id, String label) {
        if (channel == null) {
            throw new IllegalArgumentException(label + " sound channel cannot be null");
        }
        if (id < -1 || id > 0xFF) {
            throw new IllegalArgumentException(label + " sound id must be -1 or an unsigned byte: "
                + id);
        }
        if (channel == SoundChannel.NONE && id != -1) {
            throw new IllegalArgumentException("A sound id requires a "
                + label.toLowerCase() + " sound channel");
        }
        if (channel != SoundChannel.NONE && id == -1) {
            throw new IllegalArgumentException("A " + label.toLowerCase()
                + " sound channel requires a sound id");
        }
    }

    private static void validateByte(int value, String label) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
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
