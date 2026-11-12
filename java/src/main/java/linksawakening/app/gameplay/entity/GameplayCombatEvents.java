package linksawakening.app.gameplay.entity;

import java.util.List;
import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.state.GameplaySounds;
import linksawakening.entity.Link;
import linksawakening.gameplay.DamageConsequences;
import linksawakening.gameplay.EnemyCombatEventConsumer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.room.RoomFrameDrain;
import linksawakening.world.room.RoomSession;

/** Applies enemy damage outcomes and drains combat effects without session back-calls. */
final class GameplayCombatEvents implements DamageConsequences, RoomFrameDrain<EntityCombatEvent> {
    private final RoomSession rooms;
    private final Link link;
    private final GameplaySounds sounds;
    private final TransientVfxSystem effects;
    private final MusicDirector music;

    GameplayCombatEvents(
            RoomSession rooms, Link link, GameplaySounds sounds, TransientVfxSystem effects, MusicDirector music) {
        this.rooms = Objects.requireNonNull(rooms);
        this.link = Objects.requireNonNull(link);
        this.sounds = Objects.requireNonNull(sounds);
        this.effects = Objects.requireNonNull(effects);
        this.music = Objects.requireNonNull(music);
    }

    public void contactHurt() {
        rooms.entityState().resetGuardianAcornCounterOnHurt();
    }

    public void powerUpEnded() {
        music.restoreMusicAfterPowerUpEnds(rooms);
    }

    public void accept(List<? extends EntityCombatEvent> events) {
        EnemyCombatEventConsumer.consume(events, sounds.sink(), effects);
        for (int i = 0; i < events.size(); i++) {
            var event = events.get(i);
            if (event.linkDamage() == 0 && event.linkCollisionResponse().active())
                link.apply(LinkCommandAdapter.from(event));
        }
    }
}
