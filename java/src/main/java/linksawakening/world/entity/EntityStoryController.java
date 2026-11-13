package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.motion.*;

/** ROM story and NPC presentation sequences, sharing the owning room slot table. */
public final class EntityStoryController {
    final EntityStoryState state;
    final EntityMotionRegistry motions;
    // Optional ROM data; ROM-free runtimes retain the existing fallback behavior.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    final EntityBackgroundMechanics backgroundMechanics;
    final EntityBossState bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLinkState linkState;
    final EntityPhysicsTables physicsTables;
    final EntityPickupState pickups;
    final EntityPresentationState presentationState;
    final EntityRoomInspection inspection;
    final EntityRoomInspection roomInspection;
    final EntityRoomVariables roomVariables;
    final EntitySlotLifecycle lifecycle;
    final RoomEntity[] slots;
    final EntitySpriteViews spriteViews;
    final EntityPlayerCommands playerCommands;

    EntityStoryController(
            EntityStoryState state,
            EntityMotionRegistry motions,
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityBossState bossRewards,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLinkState linkState,
            EntityPhysicsTables physicsTables,
            EntityPickupState pickups,
            EntityPresentationState presentationState,
            EntityRoomInspection inspection,
            EntityRoomInspection roomInspection,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            RoomEntity[] slots,
            EntitySpriteViews spriteViews,
            EntityPlayerCommands playerCommands) {
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.spriteHandlers = spriteHandlers;
        this.backgroundMechanics = java.util.Objects.requireNonNull(backgroundMechanics, "backgroundMechanics");
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.inspection = java.util.Objects.requireNonNull(inspection, "inspection");
        this.roomInspection = java.util.Objects.requireNonNull(roomInspection, "roomInspection");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.spriteViews = java.util.Objects.requireNonNull(spriteViews, "spriteViews");
        this.playerCommands = java.util.Objects.requireNonNull(playerCommands, "playerCommands");
    }

    /** Bank 06:607F–620D. Speeds use the ROM's sixteenth-pixel units. */
    public void advanceVillageKid(RoomEntity entity, int frame, int linkX, int linkY) {
        VillageNpcBehavior.advanceVillageKid(this, entity, frame, linkX, linkY);
    }

    // Both 06:6234 and 06:613C restore visual Y to ground Y before testing
    // contact, so the child's hopping height does not move its collision box.
    void pushLinkFromKid(RoomEntity entity, int linkX, int linkY) {
        if (RoomEntityCombatRules.overlapsLink(withZ(entity, 0), linkX, linkY)) {
            this.backgroundMechanics.requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
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
        int countdown = this.motions.get(WitchMotion.class, WitchMotion::new).readWitchGotItemCountdown(slot);
        if (countdown == 0) {
            return;
        }
        this.playerCommands.presentWitchGotItem(slot);
        if (this.linkState.dialogActive) {
            return;
        }
        countdown--;
        this.motions.get(WitchMotion.class, WitchMotion::new).writeWitchGotItemCountdown(slot, countdown);
        if (countdown == 0x02) {
            this.events.pendingDialogRequests.add(new DialogRequest(0, 0x99));
        }
    }

    public void presentWitchGotItem(int slot) {
        this.playerCommands.presentWitchGotItem(slot);
    }

    public void enableBowWowFollowing() {
        RichardBehavior.enableBowWowFollowing(this);
    }

    public void markBowWowRescued() {
        this.motions.get(BowWowMotion.class, BowWowMotion::new).writeBowWowState(0x01);
    }

    public void consumeToadstool() {
        state.writePlayerHasToadstool(false);
    }

    public void startWitchGotItemCountdown(int slot) {
        this.motions.get(WitchMotion.class, WitchMotion::new).writeWitchGotItemCountdown(slot, 0x2A);
    }

    public int bowWowState() {
        return this.motions.get(BowWowMotion.class, BowWowMotion::new).readBowWowState();
    }

    public FollowingNpcState followingNpcState() {
        return this.motions
                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                .readFollowingNpcState();
    }

    public int booBuddyTriggerCount() {
        return this.motions.get(BooBuddyMotion.class, BooBuddyMotion::new).readBooBuddyTriggerCount();
    }

    public boolean playerHasToadstool() {
        return state.readPlayerHasToadstool();
    }

    public int playerMagicPowderCount() {
        return state.readPlayerMagicPowderCount();
    }

    public int bgPaletteEffectAddress() {
        return this.presentationState.readBgPaletteEffectAddress();
    }

    public int paletteDataFlags() {
        return this.presentationState.readPaletteDataFlags();
    }

    public void setShouldGetLostInMysteriousWoods(boolean value) {
        state.writeShouldGetLostInMysteriousWoods(value);
    }

    final RichardStep richardStep = new RichardStep();
    final VillageNpcFrameStep villageNpcFrameStep = new VillageNpcFrameStep();
    final OwlEventStep owlEventStep = new OwlEventStep();

    void blockLinkForOwl(int slot) {
        events.pendingLinkMotionBlockRequests.add(new linksawakening.world.entity.event.LinkMotionBlockRequest(slot));
    }
}
