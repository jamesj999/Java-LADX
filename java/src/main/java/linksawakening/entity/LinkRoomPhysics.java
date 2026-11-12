package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.state.Direction;
import linksawakening.state.LinkMotionState;

/** Owns room-entry identity and physics defaults for ApplyRoomTransition. */
final class LinkRoomPhysics {
    private int physicsModifier;

    int physicsModifier() {
        return physicsModifier;
    }

    int physicsModifier(int value) {
        return physicsModifier = value;
    }

    private long physicsRoomLoadToken = -1;

    long physicsRoomLoadToken() {
        return physicsRoomLoadToken;
    }

    long physicsRoomLoadToken(long value) {
        return physicsRoomLoadToken = value;
    }

    private int physicsRoomMapId = -1;

    int physicsRoomMapId() {
        return physicsRoomMapId;
    }

    int physicsRoomMapId(int value) {
        return physicsRoomMapId = value;
    }

    private int physicsRoomId = -1;

    int physicsRoomId() {
        return physicsRoomId;
    }

    int physicsRoomId(int value) {
        return physicsRoomId = value;
    }

    private int physicsRoomCategory = -1;

    int physicsRoomCategory() {
        return physicsRoomCategory;
    }

    int physicsRoomCategory(int value) {
        return physicsRoomCategory = value;
    }

    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkDamage> linkDamage;
    private final LinkComponentRef<LinkGroundMotion> linkGroundMotion;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkMotionStatus status;

    LinkRoomPhysics(
            LinkComponentRef<LinkAirborne> linkAirborne,
            LinkComponentRef<LinkDamage> linkDamage,
            LinkComponentRef<LinkGroundMotion> linkGroundMotion,
            LinkComponentRef<LinkMotion> linkMotion,
            LinkComponentRef<LinkServices> linkServices,
            LinkMotionStatus status) {
        this.linkAirborne = linkAirborne;
        this.linkDamage = linkDamage;
        this.linkGroundMotion = linkGroundMotion;
        this.linkMotion = linkMotion;
        this.linkServices = linkServices;
        this.status = status;
    }

    /** ApplyRoomTransition's completion path ($02:793D-$7977). */
    void finishRoomScroll(boolean downward) {
        linkMotion.get().lastRomSpeedX = 0;
        linkMotion.get().lastRomSpeedY = 0;
        linkAirborne.get().zVelocity = 0;
        if (!downward || status.debugNoClip() || linkServices.get().collision == null) {
            return;
        }
        // Run the ordinary downward collision handler, including its ledge
        // half-tile filters, before applying the source's fallback.
        int probeX = linkMotion.get().pixelX();
        int probeY = linkMotion.get().pixelY();
        linkMotion.get().lastRomSpeedY = 0x01;
        boolean blocked = linkMotion.get().leadingEdgeBlocked(probeX, probeY, Direction.DOWN.javaIndex());
        int object = linkServices
                .get()
                .collision
                .objectIdAtPoint(
                        probeX + COLLISION_POINTS_X[Direction.DOWN.javaIndex()][1],
                        probeY + COLLISION_POINTS_Y[Direction.DOWN.javaIndex()][1]);
        // Switch blocks $DB/$DC are exempt from fallback unstucking.
        if (object != 0xDB
                && object != 0xDC
                && !linkAirborne.get().overworldLedgeFallActive
                && (object == 0xE1 || blocked)) {
            // func_002_6EAD aligns X and enters LINK_MOTION_UNSTUCKING.
            // This transition fallback does not play the ledge jingle.
            linkMotion.get().subX = (linkMotion.get().romEntityX() & 0xF0) << Link.SUB_PIXEL_SHIFT;
            linkMotion.get().subY -= 8 << Link.SUB_PIXEL_SHIFT;
            status.motionState(LinkMotionState.UNSTUCKING);
            this.physicsModifier(0);
            linkAirborne.get().overworldLedgeFallActive = true;
            linkMotion.get().beginOverworldLedgeUnstuckingMotion();
        }
    }

    /**
     * Applies the source room-entry default once per loaded room. The load
     * serial also distinguishes a save/new-game reload of the same room, so a
     * prior ladder or diving modifier cannot leak across the reload.
     */
    void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        if (this.physicsRoomLoadToken() == loadToken
                && this.physicsRoomMapId() == mapId
                && this.physicsRoomId() == roomId
                && this.physicsRoomCategory() == mapCategory) {
            return;
        }
        linkGroundMotion.get().accessClearGroundEffects();
        this.physicsModifier(mapCategory == 2 ? 0x01 : 0x00);
        this.physicsRoomLoadToken(loadToken);
        this.physicsRoomMapId(mapId);
        this.physicsRoomId(roomId);
        this.physicsRoomCategory(mapCategory);
        linkDamage.get().collisionDamageImmune = false;
    }

    /** Applies the room-entry physics default after a completed warp. */
    void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId, int mapCategory) {
        linkGroundMotion.get().accessClearGroundEffects();
        this.physicsModifier(mapCategory == 2 ? 0x01 : 0x00);
        this.physicsRoomLoadToken(loadToken);
        this.physicsRoomMapId(mapId);
        this.physicsRoomId(roomId);
        this.physicsRoomCategory(mapCategory);
        linkDamage.get().collisionDamageImmune = false;
    }

    /** Mirrors source ladder/diving updates to hLinkPhysicsModifier. */
    void setRomPhysicsModifier(int modifier) {
        this.physicsModifier(modifier & 0xFF);
    }

    int accessRomPhysicsModifier() {
        return this.physicsModifier() & 0xFF;
    }
}
