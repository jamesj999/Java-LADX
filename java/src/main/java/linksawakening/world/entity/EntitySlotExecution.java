package linksawakening.world.entity;

/** Current slot execution locals; replacement of the room slot remains explicit. */
public final class EntitySlotExecution {
    private RoomEntity lifted;
    private int liftedRomDirection;
    private boolean carryingEntity;
    private int index;
    private RoomEntity entity;
    private boolean moldormIntroOpenedThisFrame;
    private boolean rollingBonesHitThisFrame;
    private boolean kanaletLeafProducer;
    private RoomEntity originalEntity;
    private EntityStatus initialStatus;
    private boolean wasInitializing;
    private EntityStatus status;
    private boolean keyDropTransitionActive;
    private RoomEntityGroundInteraction.Result preAppliedGroundResult;
    private boolean applyGenericGroundInteraction;
    private RoomEntity updated;
    private EntityLinkCollisionRules.LinkPushPolicy genericLinkPushPolicy;
    private int variant;
    private int renderFlipAttribute;

    public RoomEntity lifted() {
        return lifted;
    }

    public RoomEntity lifted(RoomEntity value) {
        return lifted = value;
    }

    public int liftedRomDirection() {
        return liftedRomDirection;
    }

    public int liftedRomDirection(int value) {
        return liftedRomDirection = value;
    }

    public boolean carryingEntity() {
        return carryingEntity;
    }

    public boolean carryingEntity(boolean value) {
        return carryingEntity = value;
    }

    public int index() {
        return index;
    }

    public int index(int value) {
        return index = value;
    }

    public int postIndex(int delta) {
        int previous = index;
        index += delta;
        return previous;
    }

    public RoomEntity entity() {
        return entity;
    }

    public RoomEntity entity(RoomEntity value) {
        return entity = value;
    }

    public boolean moldormIntroOpenedThisFrame() {
        return moldormIntroOpenedThisFrame;
    }

    public boolean moldormIntroOpenedThisFrame(boolean value) {
        return moldormIntroOpenedThisFrame = value;
    }

    public boolean rollingBonesHitThisFrame() {
        return rollingBonesHitThisFrame;
    }

    public boolean rollingBonesHitThisFrame(boolean value) {
        return rollingBonesHitThisFrame = value;
    }

    public boolean kanaletLeafProducer() {
        return kanaletLeafProducer;
    }

    public boolean kanaletLeafProducer(boolean value) {
        return kanaletLeafProducer = value;
    }

    public RoomEntity originalEntity() {
        return originalEntity;
    }

    public RoomEntity originalEntity(RoomEntity value) {
        return originalEntity = value;
    }

    public EntityStatus initialStatus() {
        return initialStatus;
    }

    public EntityStatus initialStatus(EntityStatus value) {
        return initialStatus = value;
    }

    public boolean wasInitializing() {
        return wasInitializing;
    }

    public boolean wasInitializing(boolean value) {
        return wasInitializing = value;
    }

    public EntityStatus status() {
        return status;
    }

    public EntityStatus status(EntityStatus value) {
        return status = value;
    }

    public boolean keyDropTransitionActive() {
        return keyDropTransitionActive;
    }

    public boolean keyDropTransitionActive(boolean value) {
        return keyDropTransitionActive = value;
    }

    public RoomEntityGroundInteraction.Result preAppliedGroundResult() {
        return preAppliedGroundResult;
    }

    public RoomEntityGroundInteraction.Result preAppliedGroundResult(RoomEntityGroundInteraction.Result value) {
        return preAppliedGroundResult = value;
    }

    public boolean applyGenericGroundInteraction() {
        return applyGenericGroundInteraction;
    }

    public boolean applyGenericGroundInteraction(boolean value) {
        return applyGenericGroundInteraction = value;
    }

    public RoomEntity updated() {
        return updated;
    }

    public RoomEntity updated(RoomEntity value) {
        return updated = value;
    }

    public EntityLinkCollisionRules.LinkPushPolicy genericLinkPushPolicy() {
        return genericLinkPushPolicy;
    }

    public EntityLinkCollisionRules.LinkPushPolicy genericLinkPushPolicy(
            EntityLinkCollisionRules.LinkPushPolicy value) {
        return genericLinkPushPolicy = value;
    }

    public int variant() {
        return variant;
    }

    public int variant(int value) {
        return variant = value;
    }

    public int renderFlipAttribute() {
        return renderFlipAttribute;
    }

    public int renderFlipAttribute(int value) {
        return renderFlipAttribute = value;
    }

    void reset() {
        lifted = null;
        liftedRomDirection = 0;
        carryingEntity = false;
        index = 0;
        entity = null;
        moldormIntroOpenedThisFrame = false;
        rollingBonesHitThisFrame = false;
        kanaletLeafProducer = false;
        originalEntity = null;
        initialStatus = null;
        wasInitializing = false;
        status = null;
        keyDropTransitionActive = false;
        preAppliedGroundResult = null;
        applyGenericGroundInteraction = false;
        updated = null;
        genericLinkPushPolicy = null;
        variant = 0;
        renderFlipAttribute = 0;
    }
}
