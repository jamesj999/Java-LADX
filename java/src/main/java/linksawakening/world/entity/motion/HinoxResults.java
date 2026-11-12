package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;

/** Focused behavior using the state owned by HinoxMotion. */
final class HinoxResults {
    static HinoxMotion.Update update(HinoxMotion owner, HinoxResult inputs) {
        RoomEntity entity = inputs.entity();
        int x = inputs.x();
        int y = inputs.y();
        int transition = inputs.transition();
        int spriteVariant = inputs.spriteVariant();
        boolean blocked = inputs.blocked();
        int heldX = inputs.heldX();
        int heldY = inputs.heldY();
        int heldZ = inputs.heldZ();
        int linkSpeedX = inputs.linkSpeedX();
        int linkSpeedY = inputs.linkSpeedY();
        int linkVelocityZ = inputs.linkVelocityZ();
        int airborne = inputs.airborne();
        int linkDamage = inputs.linkDamage();
        int jingle = inputs.jingle();
        int wave = inputs.wave();
        boolean dust = inputs.dust();
        boolean heldLinkPositionWritten = inputs.heldLinkPositionWritten();
        boolean applyHeldLinkPose = inputs.applyHeldLinkPose();
        HinoxMotion.BombSpawn bomb = inputs.bomb();
        RoomEntity updated = new RoomEntity(
                entity.slot(),
                entity.sourceLoadOrder(),
                entity.type(),
                x & 0xFF,
                y & 0xFF,
                entity.status(),
                entity.spriteDefinition(),
                entity.spriteDefinition().supported()
                        ? Math.min(spriteVariant, entity.spriteDefinition().variantCount() - 1)
                        : -1,
                entity.entityFlipAttribute(),
                entity.spriteTileOffset(),
                entity.z(),
                -1,
                false);
        return new HinoxMotion.Update(
                updated,
                owner.state[entity.slot()],
                transition & 0xFF,
                spriteVariant,
                blocked,
                heldX & 0xFF,
                heldY & 0xFF,
                heldZ & 0xFF,
                linkSpeedX & 0xFF,
                linkSpeedY & 0xFF,
                linkVelocityZ & 0xFF,
                airborne & 0xFF,
                linkDamage & 0xFF,
                jingle,
                wave,
                dust,
                heldLinkPositionWritten,
                applyHeldLinkPose,
                bomb);
    }
}
