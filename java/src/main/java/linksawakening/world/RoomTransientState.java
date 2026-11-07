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
        session.pendingShutterCloseMask = session.closedShutterMask = session.shutterCloseTicks = 0;
        session.shutterCloseDirection = session.shutterCloseLocation = -1;
        session.shutterCloseStarted = false;
        session.shutterTileOverrides.clear();
        if (session.transientVfxSystem != null) {
            session.transientVfxSystem.clear();
        }
        if (session.droppableRupeeSystem != null) {
            session.droppableRupeeSystem.clear();
        }
        session.hookshotBridgeTileOverrides.clear();
        session.bombedWallTileOverrides.clear();
        session.bombedBlockTileOverrides.clear();
        session.bombedCaveDoorTileOverrides.clear();
        session.retainedTorchTiles.clear();
        session.entityBridge.pendingBombExplosionEvents.clear();
        session.pendingDungeonWarp = null;
        session.entityBridge.pendingRoomEntityEvents.clear();
        session.entityBridge.pendingChestRewardEvents.clear();
        session.entityBridge.pendingKeyRewardEvents.clear();
        session.entityBridge.pendingSlimeKeyRewardEvents.clear();
        session.entityBridge.pendingHeartContainerRewards.clear();
        session.entityBridge.pendingMadamMeowMeowFullHealRequests.clear();
        session.entityBridge.pendingRichardProgressEvents.clear();
        session.entityBridge.pendingSwordPickupRewards.clear();
        session.entityBridge.pendingToadstoolRewards.clear();
        session.entityBridge.pendingWitchExchangeEvents.clear();
        session.entityBridge.pendingWitchRewardEvents.clear();
        session.entityBridge.pendingRoomDialogRequests.clear();
        session.pendingManboTransition = false;
        session.pendingInstrumentTransition = false;
        session.pendingShovelDrop = null;
        session.shovelUseState = 0;
        session.roomTriggerCount = 0;
        session.activeRoomEvent = 0;
        session.roomEventEffectExecuted = false;
        session.thrownAtDoorTriggerResolved = false;
        session.roomEventChestRevealCountdown = 0;
        session.roomEventStairRevealCountdown = 0;
        session.staircaseState = session.STAIRCASE_NONE;
        session.staircaseX = 0;
        session.staircaseY = 0;
        session.switchButtonPressCounter = 0;
        session.indoorKeyDoorAnimationCountdown = 0;
        session.indoorKeyDoorDirection = -1;
        session.indoorKeyDoorLocation = -1;
        session.indoorBossDoorOpening = false;
        session.pushBlockContactTicks = 0;
        session.indoorPotPullFrames = 0;
        session.pushedBlockMotionFrames = 0;
        session.pushedBlockSourceLocation = -1;
        session.pushedBlockDestinationLocation = -1;
        session.pushedBlockDirection = -1;
        session.pushedBlockEntitySlot = -1;
        session.pushedBlocksTriggerEffectPending = false;
        session.worldLinkMotionBlockPending = false;
        session.tailCaveKeyholeCountdown = 0;
        session.tailCaveFinalMotionBlockPending = false;
        session.nextWorldMusicTrackCountdown = 0;
        session.entityBridge.pendingRoomMusicTrack = -1;
    }
}
