package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EntityLinkCollisionRulesTest {

    @Test
    void mapsRomPushHelpersToTheirSharedLinkSideEffects() {
        EntityLinkCollisionRules.LinkPushPolicy standard =
            new EntityLinkCollisionRules.LinkPushPolicy(true, true, true, false);

        assertEquals(standard, EntityLinkCollisionRules.policyFor(0x3F));
        assertEquals(standard, EntityLinkCollisionRules.policyFor(0x54));
        assertEquals(standard, EntityLinkCollisionRules.policyFor(0xD3));
        EntityLinkCollisionRules.LinkPushPolicy speedCleared =
            new EntityLinkCollisionRules.LinkPushPolicy(true, true, true, false, true);
        assertEquals(speedCleared, EntityLinkCollisionRules.policyFor(0xCD));
        assertEquals(speedCleared, EntityLinkCollisionRules.policyFor(0xEF));
        assertEquals(speedCleared,
            EntityLinkCollisionRules.policyFor(0x66));
    }

    @Test
    void coversEveryAuditedCommonHelperCallSite() {
        int[] standardTypes = {
            0x3E, 0x3F, 0x40, 0x41, 0x42, 0x4D, 0x4F, 0x54, 0x5C, 0x69,
            0x6A, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x79,
            0x7B, 0x7C, 0x7E, 0x7F, 0x88, 0x8C, 0x95, 0xA7, 0xAD, 0xB1,
            0xB4, 0xB5, 0xB6, 0xB7, 0xC1, 0xC3, 0xC4, 0xC7, 0xCE, 0xD0,
            0xD2, 0xD3
        };
        for (int type : standardTypes) {
            assertEquals(EntityLinkCollisionRules.STANDARD_PUSH,
                EntityLinkCollisionRules.policyFor(type),
                () -> "type 0x" + Integer.toHexString(type));
        }

        int[] speedClearedTypes = {
            0x66, 0x8F, 0x9D, 0xCD, 0xD1, 0xD7, 0xD8, 0xD9, 0xDC,
            0xEF, 0xF0, 0xF1, 0xF6, 0xF7, 0xFA
        };
        for (int type : speedClearedTypes) {
            assertEquals(EntityLinkCollisionRules.STANDARD_PUSH_WITH_SPEED_CLEAR,
                EntityLinkCollisionRules.policyFor(type),
                () -> "type 0x" + Integer.toHexString(type));
        }
    }

    @Test
    void keepsSpecialHandlersOnTheirOwnRomSideEffectPaths() {
        assertEquals(new EntityLinkCollisionRules.LinkPushPolicy(true, false, false, false),
            EntityLinkCollisionRules.policyFor(0xC5));
        assertEquals(new EntityLinkCollisionRules.LinkPushPolicy(true, true, false, false),
            EntityLinkCollisionRules.policyFor(0xCB));
        assertEquals(new EntityLinkCollisionRules.LinkPushPolicy(true, false, false, true),
            EntityLinkCollisionRules.policyFor(0x06));
    }
}
