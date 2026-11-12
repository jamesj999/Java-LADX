package linksawakening.world.entity.event;

import linksawakening.world.entity.event.EntityCombatEvent.*;

/** Named construction; validation occurs when the immutable event is built. */
public final class EntityCombatEventBuilder {
    private int slot = 0;
    private int type = 0;
    private int linkDamage = 0;
    private boolean swordHit = false;
    private int enemyDamage = 0;
    private int enemySpecialAction = -1;
    private SoundChannel soundChannel = SoundChannel.NONE;
    private int soundId = -1;
    private SoundChannel secondarySoundChannel = SoundChannel.NONE;
    private int secondarySoundId = -1;
    private SwordPokeVfx swordPokeVfx = null;
    private LinkAction linkAction = LinkAction.NONE;
    private LinkCollisionResponse linkCollisionResponse = LinkCollisionResponse.NONE;

    EntityCombatEventBuilder(int slot, int type) {
        this.slot = slot;
        this.type = type;
    }

    public EntityCombatEventBuilder linkDamage(int value) {
        this.linkDamage = value;
        return this;
    }

    public EntityCombatEventBuilder swordHit(boolean value) {
        this.swordHit = value;
        return this;
    }

    public EntityCombatEventBuilder enemyDamage(int value) {
        this.enemyDamage = value;
        return this;
    }

    public EntityCombatEventBuilder enemySpecialAction(int value) {
        this.enemySpecialAction = value;
        return this;
    }

    public EntityCombatEventBuilder soundChannel(SoundChannel value) {
        this.soundChannel = value;
        return this;
    }

    public EntityCombatEventBuilder soundId(int value) {
        this.soundId = value;
        return this;
    }

    public EntityCombatEventBuilder secondarySoundChannel(SoundChannel value) {
        this.secondarySoundChannel = value;
        return this;
    }

    public EntityCombatEventBuilder secondarySoundId(int value) {
        this.secondarySoundId = value;
        return this;
    }

    public EntityCombatEventBuilder swordPokeVfx(SwordPokeVfx value) {
        this.swordPokeVfx = value;
        return this;
    }

    public EntityCombatEventBuilder linkAction(LinkAction value) {
        this.linkAction = value;
        return this;
    }

    public EntityCombatEventBuilder linkCollisionResponse(LinkCollisionResponse value) {
        this.linkCollisionResponse = value;
        return this;
    }

    public EntityCombatEvent build() {
        return new EntityCombatEvent(
                slot,
                type,
                linkDamage,
                swordHit,
                enemyDamage,
                enemySpecialAction,
                soundChannel,
                soundId,
                secondarySoundChannel,
                secondarySoundId,
                swordPokeVfx,
                linkAction,
                linkCollisionResponse);
    }
}
