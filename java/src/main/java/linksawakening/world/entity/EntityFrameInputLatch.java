package linksawakening.world.entity;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** Input registers latched for one acquired ExecuteEntities invocation; never retained as a snapshot. */
public final class EntityFrameInputLatch {
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private int collisionType;
    // Absent before initialization or while this reusable execution scratch is inactive.
    private IntSupplier randomByteSupplier;
    private RoomEntityBackgroundCollision backgroundCollision;
    private RoomEntityObjectCollision objectCollision;
    // Optional room geometry/history; null preserves the path without that room input.
    private LinkPositionHistory linkPositionHistory;
    private int linkZ;
    private boolean linkAirborne;
    private int linkDirection;
    private int entityYOffset;
    // Absent before initialization or while this reusable execution scratch is inactive.
    private EnemyProjectileCollision.LinkState projectileLinkState;
    private boolean creditsGameplay;
    private boolean swordCollisionActive;
    private int swordX;
    private int swordWidth;
    private int swordY;
    private int swordHeight;
    private int frame;
    private int romCollisionType;
    private RoomEntityBackgroundInteraction hinoxBackgroundInteraction;
    // Absent before initialization or while this reusable execution scratch is inactive.
    private List<EntityProjectileEvent> projectileEvents;
    private int romLinkDirection;

    public int frameCounter() {
        return frameCounter;
    }

    public int frameCounter(int value) {
        frameCounter = value;
        return frameCounter;
    }

    public int linkEntityX() {
        return linkEntityX;
    }

    public int linkEntityX(int value) {
        linkEntityX = value;
        return linkEntityX;
    }

    public int linkEntityY() {
        return linkEntityY;
    }

    public int linkEntityY(int value) {
        linkEntityY = value;
        return linkEntityY;
    }

    public int collisionType() {
        return collisionType;
    }

    public int collisionType(int value) {
        collisionType = value;
        return collisionType;
    }

    public IntSupplier randomByteSupplier() {
        return randomByteSupplier;
    }

    public IntSupplier randomByteSupplier(IntSupplier value) {
        randomByteSupplier = value;
        return randomByteSupplier;
    }

    public RoomEntityBackgroundCollision backgroundCollision() {
        return backgroundCollision;
    }

    public RoomEntityBackgroundCollision backgroundCollision(RoomEntityBackgroundCollision value) {
        backgroundCollision = value;
        return backgroundCollision;
    }

    public RoomEntityObjectCollision objectCollision() {
        return objectCollision;
    }

    public RoomEntityObjectCollision objectCollision(RoomEntityObjectCollision value) {
        objectCollision = value;
        return objectCollision;
    }

    public LinkPositionHistory linkPositionHistory() {
        return linkPositionHistory;
    }

    public LinkPositionHistory linkPositionHistory(
            /* Optional room geometry/history; null preserves the path without that room input. */ LinkPositionHistory
                    value) {
        linkPositionHistory = value;
        return linkPositionHistory;
    }

    public int linkZ() {
        return linkZ;
    }

    public int linkZ(int value) {
        linkZ = value;
        return linkZ;
    }

    public boolean linkAirborne() {
        return linkAirborne;
    }

    public boolean linkAirborne(boolean value) {
        linkAirborne = value;
        return linkAirborne;
    }

    public int linkDirection() {
        return linkDirection;
    }

    public int linkDirection(int value) {
        linkDirection = value;
        return linkDirection;
    }

    public int entityYOffset() {
        return entityYOffset;
    }

    public int entityYOffset(int value) {
        entityYOffset = value;
        return entityYOffset;
    }

    public EnemyProjectileCollision.LinkState projectileLinkState() {
        return projectileLinkState;
    }

    public EnemyProjectileCollision.LinkState projectileLinkState(EnemyProjectileCollision.LinkState value) {
        projectileLinkState = value;
        return projectileLinkState;
    }

    public boolean creditsGameplay() {
        return creditsGameplay;
    }

    public boolean creditsGameplay(boolean value) {
        creditsGameplay = value;
        return creditsGameplay;
    }

    public boolean swordCollisionActive() {
        return swordCollisionActive;
    }

    public boolean swordCollisionActive(boolean value) {
        swordCollisionActive = value;
        return swordCollisionActive;
    }

    public int swordX() {
        return swordX;
    }

    public int swordX(int value) {
        swordX = value;
        return swordX;
    }

    public int swordWidth() {
        return swordWidth;
    }

    public int swordWidth(int value) {
        swordWidth = value;
        return swordWidth;
    }

    public int swordY() {
        return swordY;
    }

    public int swordY(int value) {
        swordY = value;
        return swordY;
    }

    public int swordHeight() {
        return swordHeight;
    }

    public int swordHeight(int value) {
        swordHeight = value;
        return swordHeight;
    }

    public int frame() {
        return frame;
    }

    public int frame(int value) {
        frame = value;
        return frame;
    }

    public int romCollisionType() {
        return romCollisionType;
    }

    public int romCollisionType(int value) {
        romCollisionType = value;
        return romCollisionType;
    }

    public RoomEntityBackgroundInteraction hinoxBackgroundInteraction() {
        return hinoxBackgroundInteraction;
    }

    public RoomEntityBackgroundInteraction hinoxBackgroundInteraction(RoomEntityBackgroundInteraction value) {
        hinoxBackgroundInteraction = value;
        return hinoxBackgroundInteraction;
    }

    public List<EntityProjectileEvent> projectileEvents() {
        return projectileEvents;
    }

    public List<EntityProjectileEvent> projectileEvents(List<EntityProjectileEvent> value) {
        projectileEvents = value;
        return projectileEvents;
    }

    public int romLinkDirection() {
        return romLinkDirection;
    }

    public int romLinkDirection(int value) {
        romLinkDirection = value;
        return romLinkDirection;
    }

    void reset() {
        frameCounter = 0;
        linkEntityX = 0;
        linkEntityY = 0;
        collisionType = 0;
        randomByteSupplier = null;
        backgroundCollision = null;
        objectCollision = null;
        linkPositionHistory = null;
        linkZ = 0;
        linkAirborne = false;
        linkDirection = 0;
        entityYOffset = 0;
        projectileLinkState = null;
        creditsGameplay = false;
        swordCollisionActive = false;
        swordX = 0;
        swordWidth = 0;
        swordY = 0;
        swordHeight = 0;
        frame = 0;
        romCollisionType = 0;
        hinoxBackgroundInteraction = null;
        projectileEvents = null;
        romLinkDirection = 0;
    }
}
