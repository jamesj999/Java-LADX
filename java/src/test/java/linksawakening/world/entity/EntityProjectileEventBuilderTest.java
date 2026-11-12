package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.EntityProjectileEvent.Kind;
import linksawakening.world.entity.event.EntityProjectileEvent.SoundChannel;
import org.junit.jupiter.api.Test;

class EntityProjectileEventBuilderTest {
    @Test
    void namedConstructionPreservesLegacyDefaultsAndExplicitCollisionWrites() {
        assertEquals(
                new EntityProjectileEvent(
                        2, 0x0C, Kind.LINK_DAMAGE, 0, 0, SoundChannel.NONE, -1, false, false, 0, 0, 0, 0, 0),
                EntityProjectileEvent.builder(2, 0x0C, Kind.LINK_DAMAGE).build());
        assertEquals(
                new EntityProjectileEvent(
                        2, 0x0C, Kind.LINK_DAMAGE, 0xFF, 4, SoundChannel.WAVE, 2, true, true, 72, 80, 0xF0, 0x10, 8),
                EntityProjectileEvent.builder(2, 0x0C, Kind.LINK_DAMAGE)
                        .collisionValue(0xFF)
                        .linkDamage(4)
                        .soundChannel(SoundChannel.WAVE)
                        .soundId(2)
                        .remove(true)
                        .swordPokeVfx(true)
                        .swordPokeX(72)
                        .swordPokeY(80)
                        .linkSpeedX(0xF0)
                        .linkSpeedY(0x10)
                        .linkIgnoreCollisionCountdown(8)
                        .build());
    }
}
