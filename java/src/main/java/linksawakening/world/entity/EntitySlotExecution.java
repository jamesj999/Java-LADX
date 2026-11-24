package linksawakening.world.entity;

/** Current descending-slot scratch; reset before reuse, borrowed only during the owning execution. */
public final class EntitySlotExecution {
    // Absent before initialization or while this reusable execution scratch is inactive.
    private RoomEntity lifted;
    private int liftedRomDirection;
    private boolean carryingEntity;
    private int index;
    // Absent before initialization or while this reusable execution scratch is inactive.
    private RoomEntity entity;
    private boolean moldormIntroOpenedThisFrame;
    private boolean rollingBonesHitThisFrame;
    private boolean kanaletLeafProducer;
    // Absent before initialization or while this reusable execution scratch is inactive.
    private RoomEntity originalEntity;
    // Absent before initialization or while this reusable execution scratch is inactive.
    private EntityStatus initialStatus;
    private boolean wasInitializing;
    private boolean finishingLethalSwordHit;
    private int ignoreHitsBeforeRecoil;
    // Absent before initialization or while this reusable execution scratch is inactive.
    private EntityStatus status;
    private boolean keyDropTransitionActive;
    // Absent before initialization or while this reusable execution scratch is inactive.
    /* Optional result of a ground pass already performed by the current handler. */ private RoomEntityGroundInteraction
                    .Result
            preAppliedGroundResult;
    private boolean applyGenericGroundInteraction;
    // Absent before initialization or while this reusable execution scratch is inactive.
    private RoomEntity updated;
    // Absent before initialization or while this reusable execution scratch is inactive.
    private EntityLinkCollisionRules.LinkPushPolicy genericLinkPushPolicy;
    private int variant;
    private int renderFlipAttribute;

    public RoomEntity lifted() {
        return lifted;
    }

    public RoomEntity lifted(RoomEntity value) {
        lifted = value;
        return lifted;
    }

    public int liftedRomDirection() {
        return liftedRomDirection;
    }

    public int liftedRomDirection(int value) {
        liftedRomDirection = value;
        return liftedRomDirection;
    }

    public boolean carryingEntity() {
        return carryingEntity;
    }

    public boolean carryingEntity(boolean value) {
        carryingEntity = value;
        return carryingEntity;
    }

    public int index() {
        return index;
    }

    public int index(int value) {
        index = value;
        return index;
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
        entity = value;
        return entity;
    }

    public boolean moldormIntroOpenedThisFrame() {
        return moldormIntroOpenedThisFrame;
    }

    public boolean moldormIntroOpenedThisFrame(boolean value) {
        moldormIntroOpenedThisFrame = value;
        return moldormIntroOpenedThisFrame;
    }

    public boolean rollingBonesHitThisFrame() {
        return rollingBonesHitThisFrame;
    }

    public boolean rollingBonesHitThisFrame(boolean value) {
        rollingBonesHitThisFrame = value;
        return rollingBonesHitThisFrame;
    }

    public boolean kanaletLeafProducer() {
        return kanaletLeafProducer;
    }

    public boolean kanaletLeafProducer(boolean value) {
        kanaletLeafProducer = value;
        return kanaletLeafProducer;
    }

    public RoomEntity originalEntity() {
        return originalEntity;
    }

    public RoomEntity originalEntity(RoomEntity value) {
        originalEntity = value;
        return originalEntity;
    }

    public EntityStatus initialStatus() {
        return initialStatus;
    }

    public EntityStatus initialStatus(EntityStatus value) {
        initialStatus = value;
        return initialStatus;
    }

    public boolean wasInitializing() {
        return wasInitializing;
    }

    public boolean wasInitializing(boolean value) {
        wasInitializing = value;
        return wasInitializing;
    }

    public boolean finishingLethalSwordHit() {
        return finishingLethalSwordHit;
    }

    void finishingLethalSwordHit(boolean value) {
        finishingLethalSwordHit = value;
    }

    public int ignoreHitsBeforeRecoil() {
        return ignoreHitsBeforeRecoil;
    }

    void ignoreHitsBeforeRecoil(int value) {
        ignoreHitsBeforeRecoil = value;
    }

    public EntityStatus status() {
        return status;
    }

    public EntityStatus status(EntityStatus value) {
        status = value;
        return status;
    }

    public boolean keyDropTransitionActive() {
        return keyDropTransitionActive;
    }

    public boolean keyDropTransitionActive(boolean value) {
        keyDropTransitionActive = value;
        return keyDropTransitionActive;
    }

    public RoomEntityGroundInteraction.Result preAppliedGroundResult() {
        return preAppliedGroundResult;
    }

    public RoomEntityGroundInteraction.Result preAppliedGroundResult(RoomEntityGroundInteraction.Result value) {
        preAppliedGroundResult = value;
        return preAppliedGroundResult;
    }

    public boolean applyGenericGroundInteraction() {
        return applyGenericGroundInteraction;
    }

    public boolean applyGenericGroundInteraction(boolean value) {
        applyGenericGroundInteraction = value;
        return applyGenericGroundInteraction;
    }

    public RoomEntity updated() {
        return updated;
    }

    public RoomEntity updated(RoomEntity value) {
        updated = value;
        return updated;
    }

    public EntityLinkCollisionRules.LinkPushPolicy genericLinkPushPolicy() {
        return genericLinkPushPolicy;
    }

    public EntityLinkCollisionRules.LinkPushPolicy genericLinkPushPolicy(
            EntityLinkCollisionRules.LinkPushPolicy value) {
        genericLinkPushPolicy = value;
        return genericLinkPushPolicy;
    }

    public int variant() {
        return variant;
    }

    public int variant(int value) {
        variant = value;
        return variant;
    }

    public int renderFlipAttribute() {
        return renderFlipAttribute;
    }

    public int renderFlipAttribute(int value) {
        renderFlipAttribute = value;
        return renderFlipAttribute;
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
        ignoreHitsBeforeRecoil = 0;
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
