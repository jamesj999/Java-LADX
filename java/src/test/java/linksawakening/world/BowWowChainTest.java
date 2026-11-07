package linksawakening.world;

import linksawakening.world.entity.BowWowChain;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.EntityStatus;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BowWowChainTest {
    private static RoomEntity bowWow(int x, int y, int z) {
        return new RoomEntity(0, -1, 0x6D, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0x6D), 0, 0, 0, z, -1, false);
    }

    @Test
    void movingLinkPullsTheHeadAndPerpendicularAxisThroughAllSixLinks() {
        BowWowChain chain = new BowWowChain();
        chain.initialize(40, 40);
        RoomEntity entity = bowWow(40, 48, 8);
        entity = chain.advance(entity, 120, 80, 1, true);
        assertEquals(40, entity.x());
        assertEquals(48, entity.y());
        for (int frame = 1; frame < 20; frame++) {
            entity = chain.advance(entity, 120, 80, 1, true);
        }
        assertTrue(entity.x() > 40);
        assertTrue(entity.y() > 48);
        assertEquals(8, entity.z());
        assertTrue(entity.x() <= 120);
    }

    @Test
    void stationaryLinkAndKidnappedStateNeverPullTheHead() {
        for (int state : new int[] {0, 0x80, 1}) {
            BowWowChain chain = new BowWowChain();
            chain.initialize(40, 40);
            RoomEntity entity = bowWow(40, 40, 0);
            for (int frame = 0; frame < 50; frame++) {
                assertEquals(entity, chain.advance(entity, 120, 80, state, state != 1));
            }
        }
    }

    @Test
    void forwardPassUsesByteWrappingAndAlternatesTwoThenThreeOamLinks() {
        BowWowChain chain = new BowWowChain();
        chain.initialize(250, 40);
        chain.advance(bowWow(2, 40, 0), 0, 0, 0x80, false);
        var even = chain.entries(3, 0x80, 0);
        var odd = chain.entries(3, 0x80, 1);
        assertEquals(2, even.size());
        assertEquals(3, odd.size());
        assertEquals(0x4E, odd.getLast().tileIndex());
        assertEquals(0, odd.getLast().rawX()); // 250 + 2 + OAM offset 4 wraps.
        assertEquals(40, odd.getLast().rawY());
        assertEquals(3, odd.getLast().sourceSlot());
        assertEquals(0x7E, chain.entries(3, 0, 1).getFirst().tileIndex());
    }
}
