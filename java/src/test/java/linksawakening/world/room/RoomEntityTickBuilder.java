package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** Fluent fixture for the legacy tick-only path: does not overwrite cached room inputs. */
final class RoomEntityTickBuilder {
    private int frame, x, y, z, motion, direction, collision, shieldLevel, invincibility;
    private int swordX, swordY, swordWidth, swordHeight, speedX, speedY;
    private boolean airborne, shield, swordActive;

    static RoomEntityTickBuilder frame(int value) {
        var builder = new RoomEntityTickBuilder();
        builder.frame = value;
        return builder;
    }

    RoomEntityTickBuilder position(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    RoomEntityTickBuilder motion(boolean airborne, int motion, int direction, int collision) {
        this.airborne = airborne;
        this.motion = motion;
        this.direction = direction;
        this.collision = collision;
        return this;
    }

    RoomEntityTickBuilder shield(boolean active, int level, int invincibility) {
        this.shield = active;
        this.shieldLevel = level;
        this.invincibility = invincibility;
        return this;
    }

    RoomEntityTickBuilder sword(boolean active, int x, int width, int y, int height) {
        this.swordActive = active;
        this.swordX = x;
        this.swordWidth = width;
        this.swordY = y;
        this.swordHeight = height;
        return this;
    }

    RoomEntityTickBuilder speed(int x, int y) {
        speedX = x;
        speedY = y;
        return this;
    }

    List<EntityProjectileEvent> tick(RoomSession session) {
        return session.tickEntities(new EntityTickInputs(
                frame,
                new LinkPosition(
                        x, y, z, linksawakening.state.Direction.fromJavaOrUnknown(direction), collision, direction),
                new LinkMotion(
                        airborne,
                        linksawakening.world.room.LinkMotion.stateForRomByte(motion),
                        shield,
                        invincibility,
                        speedX,
                        speedY,
                        motion),
                shieldLevel,
                new SwordBox(swordActive, swordX, swordWidth, swordY, swordHeight)));
    }
}
