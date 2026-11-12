package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.motion.*;

/** ROM story and NPC presentation sequences, sharing the owning room slot table. */
public final class EntityStoryController {
    private boolean shouldGetLostInMysteriousWoods;
    private final int[] kidPlayTimer = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidPlayState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidZSpeed = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] kidBall = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private int kidHintIndex;
    private final int[] kidKidnapState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidKidnapDelay = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidSpeedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] richardState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] richardDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] richardXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] richardInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private boolean playerHasToadstool;
    private int playerMagicPowderCount;
    private boolean playerHasTailKey;
    private int houseTarinFlag;
    private int houseTradeItem;
    private int houseInstrument3Flags;
    private int houseWalrusStatus;
    private final OwlEventState owl = new OwlEventState();
    final EntityStoryControllerServices runtime;

    EntityStoryController(EntityStoryControllerServices runtime) {
        this.runtime = runtime;
    }

    /** Bank 06:607F–620D. Speeds use the ROM's sixteenth-pixel units. */
    public void advanceVillageKid(RoomEntity entity, int frame, int linkX, int linkY) {
        VillageNpcBehavior.advanceVillageKid(this, entity, frame, linkX, linkY);
    }

    // Both 06:6234 and 06:613C restore visual Y to ground Y before testing
    // contact, so the child's hopping height does not move its collision box.
    void pushLinkFromKid(RoomEntity entity, int linkX, int linkY) {
        if (RoomEntityCombatRules.overlapsLink(withZ(entity, 0), linkX, linkY)) {
            runtime.backgroundMechanics().requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
        }
    }

    public void advanceKidnapWarningKid(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY) {
        VillageNpcBehavior.advanceKidnapWarningKid(this, entity, frameCounter, linkEntityX, linkEntityY);
    }

    public void advanceMadamMeowMeow(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne) {
        VillageNpcBehavior.advanceMadamMeowMeow(this, entity, frameCounter, linkEntityX, linkEntityY, linkAirborne);
    }

    public void advanceOwlStatue(
            RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, int linkMotionState, boolean linkAirborne) {
        VillageNpcBehavior.advanceOwlStatue(
                this, entity, linkEntityX, linkEntityY, linkZ, linkMotionState, linkAirborne);
    }

    public void advanceRichard(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne) {
        RichardBehavior.advanceRichard(this, entity, frameCounter, linkEntityX, linkEntityY, linkAirborne);
    }

    public void advanceOwlEvent(
            RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean handlerLinkInteractive) {
        OwlEventBehavior.advanceOwlEvent(this, entity, frameCounter, linkEntityX, linkEntityY, handlerLinkInteractive);
    }

    void advanceDepartingOwl(RoomEntity entity, int slot, int frameCounter) {
        OwlEventBehavior.advanceDepartingOwl(this, entity, slot, frameCounter);
    }

    public void advanceWitchGotItemPresentation(int slot) {
        int countdown =
                runtime.motions().get(WitchMotion.class, WitchMotion::new).readWitchGotItemCountdown(slot);
        if (countdown == 0) {
            return;
        }
        runtime.presentWitchGotItem(slot);
        if (runtime.linkState().dialogActive) {
            return;
        }
        countdown--;
        runtime.motions().get(WitchMotion.class, WitchMotion::new).writeWitchGotItemCountdown(slot, countdown);
        if (countdown == 0x02) {
            runtime.events().pendingDialogRequests.add(new DialogRequest(0, 0x99));
        }
    }

    public void presentWitchGotItem(int slot) {
        runtime.presentWitchGotItem(slot);
    }

    public void enableBowWowFollowing() {
        RichardBehavior.enableBowWowFollowing(this);
    }

    public void markBowWowRescued() {
        runtime.motions().get(BowWowMotion.class, BowWowMotion::new).writeBowWowState(0x01);
    }

    public void consumeToadstool() {
        this.writePlayerHasToadstool(false);
    }

    public void startWitchGotItemCountdown(int slot) {
        runtime.motions().get(WitchMotion.class, WitchMotion::new).writeWitchGotItemCountdown(slot, 0x2A);
    }

    public int bowWowState() {
        return runtime.motions().get(BowWowMotion.class, BowWowMotion::new).readBowWowState();
    }

    public FollowingNpcState followingNpcState() {
        return runtime.motions()
                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                .readFollowingNpcState();
    }

    public int booBuddyTriggerCount() {
        return runtime.motions().get(BooBuddyMotion.class, BooBuddyMotion::new).readBooBuddyTriggerCount();
    }

    public boolean playerHasToadstool() {
        return this.readPlayerHasToadstool();
    }

    public int playerMagicPowderCount() {
        return this.readPlayerMagicPowderCount();
    }

    public int bgPaletteEffectAddress() {
        return runtime.presentationState().readBgPaletteEffectAddress();
    }

    public int paletteDataFlags() {
        return runtime.presentationState().readPaletteDataFlags();
    }

    public void setShouldGetLostInMysteriousWoods(boolean value) {
        this.writeShouldGetLostInMysteriousWoods(value);
    }

    boolean readShouldGetLostInMysteriousWoods() {
        return shouldGetLostInMysteriousWoods;
    }

    boolean writeShouldGetLostInMysteriousWoods(boolean value) {
        return shouldGetLostInMysteriousWoods = value;
    }

    int readKidPlayTimer(int slot) {
        return kidPlayTimer[slot];
    }

    int writeKidPlayTimer(int slot, int value) {
        return kidPlayTimer[slot] = value;
    }

    int[] storageKidPlayTimer() {
        return kidPlayTimer;
    }

    int preKidPlayTimer(int slot, int delta) {
        return kidPlayTimer[slot] += delta;
    }

    int readKidPlayState(int slot) {
        return kidPlayState[slot];
    }

    int writeKidPlayState(int slot, int value) {
        return kidPlayState[slot] = value;
    }

    int[] storageKidPlayState() {
        return kidPlayState;
    }

    int readKidZSpeed(int slot) {
        return kidZSpeed[slot];
    }

    int writeKidZSpeed(int slot, int value) {
        return kidZSpeed[slot] = value;
    }

    int[] storageKidZSpeed() {
        return kidZSpeed;
    }

    int postKidZSpeed(int slot, int delta) {
        int previous = kidZSpeed[slot];
        kidZSpeed[slot] += delta;
        return previous;
    }

    boolean readKidBall(int slot) {
        return kidBall[slot];
    }

    boolean writeKidBall(int slot, boolean value) {
        return kidBall[slot] = value;
    }

    boolean[] storageKidBall() {
        return kidBall;
    }

    int postKidHintIndex(int delta) {
        int previous = kidHintIndex;
        kidHintIndex += delta;
        return previous;
    }

    int readKidKidnapState(int slot) {
        return kidKidnapState[slot];
    }

    int writeKidKidnapState(int slot, int value) {
        return kidKidnapState[slot] = value;
    }

    int[] storageKidKidnapState() {
        return kidKidnapState;
    }

    int writeKidKidnapDelay(int slot, int value) {
        return kidKidnapDelay[slot] = value;
    }

    int[] storageKidKidnapDelay() {
        return kidKidnapDelay;
    }

    int preKidKidnapDelay(int slot, int delta) {
        return kidKidnapDelay[slot] += delta;
    }

    int writeKidSpeedXAccumulator(int slot, int value) {
        return kidSpeedXAccumulator[slot] = value;
    }

    int[] storageKidSpeedXAccumulator() {
        return kidSpeedXAccumulator;
    }

    int writeKidSpeedYAccumulator(int slot, int value) {
        return kidSpeedYAccumulator[slot] = value;
    }

    int[] storageKidSpeedYAccumulator() {
        return kidSpeedYAccumulator;
    }

    int readRichardState(int slot) {
        return richardState[slot];
    }

    int writeRichardState(int slot, int value) {
        return richardState[slot] = value;
    }

    int readRichardDirection(int slot) {
        return richardDirection[slot];
    }

    int writeRichardDirection(int slot, int value) {
        return richardDirection[slot] = value;
    }

    int[] storageRichardXAccumulator() {
        return richardXAccumulator;
    }

    int postRichardInertia(int slot, int delta) {
        int previous = richardInertia[slot];
        richardInertia[slot] += delta;
        return previous;
    }

    boolean readPlayerHasToadstool() {
        return playerHasToadstool;
    }

    boolean writePlayerHasToadstool(boolean value) {
        return playerHasToadstool = value;
    }

    int readPlayerMagicPowderCount() {
        return playerMagicPowderCount;
    }

    int writePlayerMagicPowderCount(int value) {
        return playerMagicPowderCount = value;
    }

    boolean readPlayerHasTailKey() {
        return playerHasTailKey;
    }

    boolean writePlayerHasTailKey(boolean value) {
        return playerHasTailKey = value;
    }

    int readHouseTarinFlag() {
        return houseTarinFlag;
    }

    int writeHouseTarinFlag(int value) {
        return houseTarinFlag = value;
    }

    int readHouseTradeItem() {
        return houseTradeItem;
    }

    int writeHouseTradeItem(int value) {
        return houseTradeItem = value;
    }

    int readHouseInstrument3Flags() {
        return houseInstrument3Flags;
    }

    int writeHouseInstrument3Flags(int value) {
        return houseInstrument3Flags = value;
    }

    int readHouseWalrusStatus() {
        return houseWalrusStatus;
    }

    int writeHouseWalrusStatus(int value) {
        return houseWalrusStatus = value;
    }

    OwlEventState owlState() {
        return owl;
    }

    final RichardStep richardStep = new RichardStep();
    final VillageNpcFrameStep villageNpcFrameStep = new VillageNpcFrameStep();
    final OwlEventStep owlEventStep = new OwlEventStep();
}
