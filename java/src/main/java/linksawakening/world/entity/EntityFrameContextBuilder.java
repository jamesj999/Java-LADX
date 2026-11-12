package linksawakening.world.entity;

import java.util.function.IntSupplier;

/** Named frame inputs; service references and their evaluation order are retained. */
public final class EntityFrameContextBuilder {
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private int collisionType;
    private IntSupplier randomByteSupplier;
    private RoomEntityBackgroundCollision backgroundCollision;
    private RoomEntityObjectCollision objectCollision;
    private LinkPositionHistory linkPositionHistory;
    private int linkZ;
    private boolean linkAirborne;
    private int linkDirection;
    private int entityYOffset;
    private EnemyProjectileCollision.LinkState projectileLinkState;
    private boolean swordCollisionActive;
    private int swordX;
    private int swordWidth;
    private int swordY;
    private int swordHeight;
    private int linkSpeedX;
    private int linkSpeedY;
    private boolean creditsGameplay;
    private Boolean handlerLinkCollisionEnabled;

    EntityFrameContextBuilder() {}

    public EntityFrameContextBuilder frameCounter(int value) {
        frameCounter = value;
        return this;
    }

    public EntityFrameContextBuilder linkEntityX(int value) {
        linkEntityX = value;
        return this;
    }

    public EntityFrameContextBuilder linkEntityY(int value) {
        linkEntityY = value;
        return this;
    }

    public EntityFrameContextBuilder collisionType(int value) {
        collisionType = value;
        return this;
    }

    public EntityFrameContextBuilder randomByteSupplier(IntSupplier value) {
        randomByteSupplier = value;
        return this;
    }

    public EntityFrameContextBuilder backgroundCollision(RoomEntityBackgroundCollision value) {
        backgroundCollision = value;
        return this;
    }

    public EntityFrameContextBuilder objectCollision(RoomEntityObjectCollision value) {
        objectCollision = value;
        return this;
    }

    public EntityFrameContextBuilder linkPositionHistory(LinkPositionHistory value) {
        linkPositionHistory = value;
        return this;
    }

    public EntityFrameContextBuilder linkZ(int value) {
        linkZ = value;
        return this;
    }

    public EntityFrameContextBuilder linkAirborne(boolean value) {
        linkAirborne = value;
        return this;
    }

    public EntityFrameContextBuilder linkDirection(int value) {
        linkDirection = value;
        return this;
    }

    public EntityFrameContextBuilder entityYOffset(int value) {
        entityYOffset = value;
        return this;
    }

    public EntityFrameContextBuilder projectileLinkState(EnemyProjectileCollision.LinkState value) {
        projectileLinkState = value;
        return this;
    }

    public EntityFrameContextBuilder swordCollisionActive(boolean value) {
        swordCollisionActive = value;
        return this;
    }

    public EntityFrameContextBuilder swordX(int value) {
        swordX = value;
        return this;
    }

    public EntityFrameContextBuilder swordWidth(int value) {
        swordWidth = value;
        return this;
    }

    public EntityFrameContextBuilder swordY(int value) {
        swordY = value;
        return this;
    }

    public EntityFrameContextBuilder swordHeight(int value) {
        swordHeight = value;
        return this;
    }

    public EntityFrameContextBuilder linkSpeedX(int value) {
        linkSpeedX = value;
        return this;
    }

    public EntityFrameContextBuilder linkSpeedY(int value) {
        linkSpeedY = value;
        return this;
    }

    public EntityFrameContextBuilder creditsGameplay(boolean value) {
        creditsGameplay = value;
        return this;
    }

    public EntityFrameContextBuilder handlerLinkCollisionEnabled(boolean value) {
        handlerLinkCollisionEnabled = value;
        return this;
    }

    public EntityFrameContext build() {
        return new EntityFrameContext(
                frameCounter,
                linkEntityX,
                linkEntityY,
                collisionType,
                randomByteSupplier,
                backgroundCollision,
                objectCollision,
                linkPositionHistory,
                linkZ,
                linkAirborne,
                linkDirection,
                entityYOffset,
                projectileLinkState,
                swordCollisionActive,
                swordX,
                swordWidth,
                swordY,
                swordHeight,
                linkSpeedX,
                linkSpeedY,
                creditsGameplay,
                handlerLinkCollisionEnabled != null
                        ? handlerLinkCollisionEnabled
                        : projectileLinkState.motionState() < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE);
    }
}
