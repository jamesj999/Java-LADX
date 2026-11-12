package linksawakening.world.entity.event;

import linksawakening.world.entity.event.EntityProjectileEvent.Kind;
import linksawakening.world.entity.event.EntityProjectileEvent.SoundChannel;

/** Named event construction; defaults match the legacy collision constructors. */
public final class EntityProjectileEventBuilder {
    private final int slot;
    private final int type;
    private final Kind kind;
    private int collisionValue = 0;
    private int linkDamage = 0;
    private SoundChannel soundChannel = SoundChannel.NONE;
    private int soundId = -1;
    private boolean remove = false;
    private boolean swordPokeVfx = false;
    private int swordPokeX = 0;
    private int swordPokeY = 0;
    private int linkSpeedX = 0;
    private int linkSpeedY = 0;
    private int linkIgnoreCollisionCountdown = 0;

    EntityProjectileEventBuilder(int slot, int type, Kind kind) {
        this.slot = slot;
        this.type = type;
        this.kind = kind;
    }

    public EntityProjectileEventBuilder collisionValue(int value) {
        collisionValue = value;
        return this;
    }

    public EntityProjectileEventBuilder linkDamage(int value) {
        linkDamage = value;
        return this;
    }

    public EntityProjectileEventBuilder soundChannel(SoundChannel value) {
        soundChannel = value;
        return this;
    }

    public EntityProjectileEventBuilder soundId(int value) {
        soundId = value;
        return this;
    }

    public EntityProjectileEventBuilder remove(boolean value) {
        remove = value;
        return this;
    }

    public EntityProjectileEventBuilder swordPokeVfx(boolean value) {
        swordPokeVfx = value;
        return this;
    }

    public EntityProjectileEventBuilder swordPokeX(int value) {
        swordPokeX = value;
        return this;
    }

    public EntityProjectileEventBuilder swordPokeY(int value) {
        swordPokeY = value;
        return this;
    }

    public EntityProjectileEventBuilder linkSpeedX(int value) {
        linkSpeedX = value;
        return this;
    }

    public EntityProjectileEventBuilder linkSpeedY(int value) {
        linkSpeedY = value;
        return this;
    }

    public EntityProjectileEventBuilder linkIgnoreCollisionCountdown(int value) {
        linkIgnoreCollisionCountdown = value;
        return this;
    }

    public EntityProjectileEvent build() {
        return new EntityProjectileEvent(
                slot,
                type,
                kind,
                collisionValue,
                linkDamage,
                soundChannel,
                soundId,
                remove,
                swordPokeVfx,
                swordPokeX,
                swordPokeY,
                linkSpeedX,
                linkSpeedY,
                linkIgnoreCollisionCountdown);
    }
}
