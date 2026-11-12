package linksawakening.world.entity;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** Effective inputs for this frame, including handler-adjusted Link coordinates. */
public final class EntityFrameInputs {
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
    private boolean creditsGameplay;
    private boolean swordCollisionActive;
    private int swordX;
    private int swordWidth;
    private int swordY;
    private int swordHeight;
    private int frame;
    private int romCollisionType;
    private RoomEntityBackgroundInteraction hinoxBackgroundInteraction;
    private List<EntityProjectileEvent> projectileEvents;
    private int romLinkDirection;

    public int frameCounter() {
        return frameCounter;
    }

    public int frameCounter(int value) {
        return frameCounter = value;
    }

    public int linkEntityX() {
        return linkEntityX;
    }

    public int linkEntityX(int value) {
        return linkEntityX = value;
    }

    public int linkEntityY() {
        return linkEntityY;
    }

    public int linkEntityY(int value) {
        return linkEntityY = value;
    }

    public int collisionType() {
        return collisionType;
    }

    public int collisionType(int value) {
        return collisionType = value;
    }

    public IntSupplier randomByteSupplier() {
        return randomByteSupplier;
    }

    public IntSupplier randomByteSupplier(IntSupplier value) {
        return randomByteSupplier = value;
    }

    public RoomEntityBackgroundCollision backgroundCollision() {
        return backgroundCollision;
    }

    public RoomEntityBackgroundCollision backgroundCollision(RoomEntityBackgroundCollision value) {
        return backgroundCollision = value;
    }

    public RoomEntityObjectCollision objectCollision() {
        return objectCollision;
    }

    public RoomEntityObjectCollision objectCollision(RoomEntityObjectCollision value) {
        return objectCollision = value;
    }

    public LinkPositionHistory linkPositionHistory() {
        return linkPositionHistory;
    }

    public LinkPositionHistory linkPositionHistory(LinkPositionHistory value) {
        return linkPositionHistory = value;
    }

    public int linkZ() {
        return linkZ;
    }

    public int linkZ(int value) {
        return linkZ = value;
    }

    public boolean linkAirborne() {
        return linkAirborne;
    }

    public boolean linkAirborne(boolean value) {
        return linkAirborne = value;
    }

    public int linkDirection() {
        return linkDirection;
    }

    public int linkDirection(int value) {
        return linkDirection = value;
    }

    public int entityYOffset() {
        return entityYOffset;
    }

    public int entityYOffset(int value) {
        return entityYOffset = value;
    }

    public EnemyProjectileCollision.LinkState projectileLinkState() {
        return projectileLinkState;
    }

    public EnemyProjectileCollision.LinkState projectileLinkState(EnemyProjectileCollision.LinkState value) {
        return projectileLinkState = value;
    }

    public boolean creditsGameplay() {
        return creditsGameplay;
    }

    public boolean creditsGameplay(boolean value) {
        return creditsGameplay = value;
    }

    public boolean swordCollisionActive() {
        return swordCollisionActive;
    }

    public boolean swordCollisionActive(boolean value) {
        return swordCollisionActive = value;
    }

    public int swordX() {
        return swordX;
    }

    public int swordX(int value) {
        return swordX = value;
    }

    public int swordWidth() {
        return swordWidth;
    }

    public int swordWidth(int value) {
        return swordWidth = value;
    }

    public int swordY() {
        return swordY;
    }

    public int swordY(int value) {
        return swordY = value;
    }

    public int swordHeight() {
        return swordHeight;
    }

    public int swordHeight(int value) {
        return swordHeight = value;
    }

    public int frame() {
        return frame;
    }

    public int frame(int value) {
        return frame = value;
    }

    public int romCollisionType() {
        return romCollisionType;
    }

    public int romCollisionType(int value) {
        return romCollisionType = value;
    }

    public RoomEntityBackgroundInteraction hinoxBackgroundInteraction() {
        return hinoxBackgroundInteraction;
    }

    public RoomEntityBackgroundInteraction hinoxBackgroundInteraction(RoomEntityBackgroundInteraction value) {
        return hinoxBackgroundInteraction = value;
    }

    public List<EntityProjectileEvent> projectileEvents() {
        return projectileEvents;
    }

    public List<EntityProjectileEvent> projectileEvents(List<EntityProjectileEvent> value) {
        return projectileEvents = value;
    }

    public int romLinkDirection() {
        return romLinkDirection;
    }

    public int romLinkDirection(int value) {
        return romLinkDirection = value;
    }
}
