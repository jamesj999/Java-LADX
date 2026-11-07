package linksawakening.world;

import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpDestination;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.RoomSession.*;

/** RoomTransientState operations for the live room session. */
final class RoomTransientState {
    private final RoomSession session;
    RoomTransientState(RoomSession session) { this.session = session; }

    void clearTransientRoomState() {
        session.context.pendingShutterCloseMask = session.context.closedShutterMask = session.context.shutterCloseTicks = 0;
        session.context.shutterCloseDirection = session.context.shutterCloseLocation = -1;
        session.context.shutterCloseStarted = false;
        session.context.shutterTileOverrides.clear();
        if (session.context.transientVfxSystem != null) {
            session.context.transientVfxSystem.clear();
        }
        if (session.context.droppableRupeeSystem != null) {
            session.context.droppableRupeeSystem.clear();
        }
        session.context.hookshotBridgeTileOverrides.clear();
        session.context.bombedWallTileOverrides.clear();
        session.context.bombedBlockTileOverrides.clear();
        session.context.bombedCaveDoorTileOverrides.clear();
        session.context.retainedTorchTiles.clear();
        session.context.entityBridge.pendingBombExplosionEvents.clear();
        session.context.pendingDungeonWarp = null;
        session.context.entityBridge.pendingRoomEntityEvents.clear();
        session.context.entityBridge.pendingChestRewardEvents.clear();
        session.context.entityBridge.pendingKeyRewardEvents.clear();
        session.context.entityBridge.pendingSlimeKeyRewardEvents.clear();
        session.context.entityBridge.pendingHeartContainerRewards.clear();
        session.context.entityBridge.pendingMadamMeowMeowFullHealRequests.clear();
        session.context.entityBridge.pendingRichardProgressEvents.clear();
        session.context.entityBridge.pendingSwordPickupRewards.clear();
        session.context.entityBridge.pendingToadstoolRewards.clear();
        session.context.entityBridge.pendingWitchExchangeEvents.clear();
        session.context.entityBridge.pendingWitchRewardEvents.clear();
        session.context.entityBridge.pendingRoomDialogRequests.clear();
        session.context.pendingManboTransition = false;
        session.context.pendingInstrumentTransition = false;
        session.context.pendingShovelDrop = null;
        session.context.shovelUseState = 0;
        session.context.roomTriggerCount = 0;
        session.context.activeRoomEvent = 0;
        session.context.roomEventEffectExecuted = false;
        session.context.thrownAtDoorTriggerResolved = false;
        session.context.roomEventChestRevealCountdown = 0;
        session.context.roomEventStairRevealCountdown = 0;
        session.context.staircaseState = session.STAIRCASE_NONE;
        session.context.staircaseX = 0;
        session.context.staircaseY = 0;
        session.context.switchButtonPressCounter = 0;
        session.context.indoorKeyDoorAnimationCountdown = 0;
        session.context.indoorKeyDoorDirection = -1;
        session.context.indoorKeyDoorLocation = -1;
        session.context.indoorBossDoorOpening = false;
        session.context.pushBlockContactTicks = 0;
        session.context.indoorPotPullFrames = 0;
        session.context.pushedBlockMotionFrames = 0;
        session.context.pushedBlockSourceLocation = -1;
        session.context.pushedBlockDestinationLocation = -1;
        session.context.pushedBlockDirection = -1;
        session.context.pushedBlockEntitySlot = -1;
        session.context.pushedBlocksTriggerEffectPending = false;
        session.context.worldLinkMotionBlockPending = false;
        session.context.tailCaveKeyholeCountdown = 0;
        session.context.tailCaveFinalMotionBlockPending = false;
        session.context.nextWorldMusicTrackCountdown = 0;
        session.context.entityBridge.pendingRoomMusicTrack = -1;
    }
}
