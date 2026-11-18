package linksawakening.world.room;

import static linksawakening.world.room.RoomNavigationFixture.PowderApproach;
import static linksawakening.world.room.RoomNavigationFixture.SwordApproach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.startup.NewGameStartProfile;
import linksawakening.startup.TarinShieldMotion;
import linksawakening.state.Direction;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;

/** Retained state and navigation steps for one continuous physical traversal; never reset between goals. */
final class GameplayRouteFixture {
    static final int CENTRAL_APPROACH_X = 0x50;
    static final int CENTRAL_APPROACH_Y = 0x50;
    static final int BEACH_SWORD_X = 0x58;
    static final int BEACH_SWORD_Y = 0x60;
    static final int TAIL_CAVE_KEYHOLE_X = 0x5A;
    static final int TAIL_CAVE_KEYHOLE_Y = 0x4A;
    static final int TAIL_CAVE_ENTRANCE_TILE = 0x16;
    static final int TAIL_CAVE_MAP_ID = 0x00;
    static final int TAIL_CAVE_ENTRY_ROOM = 0x17;

    byte[] rom;
    RomTables romTables;
    OverworldCollision collision;
    RoomSession session;
    PlayerState playerState;
    NewGameStartProfile profile;
    RoomEntity tarin;
    TarinShieldMotion shieldMotion;
    TarinShieldMotion.Update shieldReward;
    ScrollController scroll;
    TransitionController transition;
    RoomTransitionCoordinator coordinator;
    InputState inputState;
    InputConfig inputConfig;
    ItemRegistry itemRegistry;
    Link link;
    RoomEntity sword;
    RoomEntity owl;
    int frame;
    boolean owlDialogOpened;
    int pickupFrame;
    EntityPickupEvent pickup;
    int[] routeToForest;
    int[] expectedRooms;
    boolean forestOwlDialogOpened;
    int[] routeToToadstool;
    int[] toadstoolRouteRooms;
    RoomEntity toadstool;
    EntityPickupEvent toadstoolPickup;
    List<ToadstoolRewardEvent> toadstoolRewards;
    int[] routeToWitchHut;
    int[] witchRouteRooms;
    Warp witchHut;
    RoomEntity witch;
    List<WitchExchangeEvent> exchangeEvents;
    boolean brewingDialog;
    boolean readyDialog;
    int[] routeToRaccoon;
    int[] raccoonRouteRooms;
    boolean transformedDialogOpened;
    ChestOpenResult tailKeyChest;
    boolean tailKeyOwlDialogOpened;
    int tailKeyOwlDeadline;
    int tailKeyOwlExitDeadline;
    int[] routeToTailCave;
    int[] tailCaveRouteRooms;
    ArrayList<linksawakening.gameplay.GameplaySoundEvent> tailCaveSounds;
    int rumbleTicks;
    boolean shookRight;
    boolean shookLeft;
    Warp tailCaveEntrance;
    List<Integer> hardhatSlots;
    int room16Deadline;
    RoomEntity droppedSmallKey;
    int keyLandingDeadline;
    int keyPickupFrame;
    int[] entranceExit;
    int chestIndex;
    int buttonDeadline;
    int chestAppearanceDeadline;
    ChestOpenResult firstSmallKeyChest;
    int upperKeyDoorIndex;
    int lowerKeyDoorIndex;
    int firstKeyDoorLinkX;
    int firstKeyDoorLinkY;
    int rightCollisionBit;
    List<RoomEntity> cards;
    ChestOpenResult secondSmallKeyChest;
    int movableBlockIndex;
    int hiddenStairsIndex;
    List<RoomEntity> beetles;
    int beetleDeadline;
    ChestOpenResult featherChest;
    boolean hasFeather;
    int featherSubscreenSlot;
    ChestOpenResult nightmareKeyChest;
    RoomEntity rollingBones;
    int rollingBonesDeadline;
    RoomEntity moldorm;
    int moldormDeadline;
    RoomEntity heartContainer;
    int heartPickupFrame;
    int previousMaxHearts;
    RoomEntity instrument;
    int ticksToWarpJingle;
    boolean heardWarpJingle;
    int slowWarpTicks;
    boolean instrumentOwlDialogOpened;
    ArrayList<Integer> instrumentOwlDialogs;
    int instrumentOwlDeadline;
    int instrumentOwlExitDeadline;
    int[] routeToKidnappingWarning;
    int[] kidnappingWarningRooms;
    RoomEntity warningKid;
    int[] routeToMoblinHideout;
    int[] moblinHideoutRouteRooms;
    Warp moblinHideoutEntrance;
    RoomEntity hideoutGuard;
    boolean hideoutGuardDialogOpened;
    int guardIntroDeadline;
    int guardDeadline;
    List<Integer> hideoutMoblinSlots;
    int moblinRoomDeadline;
    RoomEntity moblinKing;
    boolean moblinKingIntroOpened;
    int moblinKingHits;
    int moblinKingDeadline;
    RoomEntity kidnappedBowWow;
    int bowWowRescueDeadline;
    Warp hideoutExit;
    boolean swampOwlDialogOpened;
    int swampOwlDeadline;
    int swampOwlExitDeadline;
    int swampOwlDepartureDeadline;
    int[] routeToGopongaSwamp;
    int[] gopongaSwampRouteRooms;
    Warp bottleGrottoEntrance;
    List<int[]> entrancePath;
    Warp bottleGrottoExit;
    PowderApproach firstTorchApproach;
    PowderApproach secondTorchApproach;
    List<Integer> bottleStalfosSlots;
    int firstBottleKeyDeadline;
    RoomEntity firstBottleKey;
    int bottleKeyLandingDeadline;
    List<int[]> keyPath;
    int bottleKeyPickupFrame;
    int room33RaisedBlockLocation;
    RoomEntity crystalSwitch;
    List<int[]> switchPath;
    Sword liveSword;
    Sword.CollisionBox initialSwordBox;
    Sword.CollisionBox swordBox;
    List<EntityCombatEvent> switchHit;
    List<Integer> maskedMimicSlots;
    int defeatedMaskedMimics;
    int mimicEventDeadline;
    RoomEntity secondBottleKey;
    int secondKeyLandingDeadline;
    List<int[]> secondKeyPath;
    int secondKeyPickupFrame;
    RoomEntity returnCrystalSwitch;
    List<int[]> returnSwitchPath;
    Sword returnSwitchSword;
    Sword.CollisionBox initialReturnSwordBox;
    Sword.CollisionBox returnSwordBox;
    List<EntityCombatEvent> returnSwitchHit;
    RoomEntity currentReturnCrystalSwitch;
    int bottomKeyDoorLocation;
    List<int[]> bottomDoorPath;
    int bottomDoorContactX;
    int bottomDoorContactY;
    List<Integer> room37RupeeSlots;
    RoomEntity maskedMimic;
    int maskedMimicSlot;
    int mimicDeathDeadline;
    int room37EventDeadline;
    int compassChestIndex;
    int chestRevealDeadline;
    List<int[]> compassChestPath;
    int compassChestContactX;
    int compassChestContactY;
    int compassChestInteractionLocation;
    ChestOpenResult compassChest;
    int compassChestSlot;
    boolean compassRewardObserved;
    boolean compassDialogObserved;
    boolean compassChestEnded;
    List<int[]> westDoorPath;
    int westDoorContactX;
    int westDoorContactY;
    List<Integer> room30KeeseSlots;
    int room30EventDeadline;
    RoomEntity hardhat;
    List<int[]> stoneBeakFeatherPath;
    int stoneBeakChestContactX;
    int stoneBeakChestContactY;
    int stoneBeakChestInteractionLocation;
    ChestOpenResult stoneBeakChest;
    int stoneBeakChestSlot;
    int stoneBeakDialogId;
    boolean stoneBeakRewardObserved;
    boolean stoneBeakDialogObserved;
    boolean stoneBeakChestEnded;
    RoomEntity postStoneCrystalSwitch;
    List<int[]> postStoneSwitchPath;
    Sword postStoneSword;
    Sword.CollisionBox postStoneInitialSwordBox;
    Sword.CollisionBox postStoneSwordBox;
    List<EntityCombatEvent> postStoneSwitchHit;
    int postStoneCrystalSwitchSlot;
    RoomEntity room38MoblinSword;
    RoomEntity room38CrystalSwitch;
    int room38LoweredBlockLocation;
    SwordApproach room38SwitchApproach;
    Sword postStoneLoweringSword;
    Sword.CollisionBox postStoneLoweringBox;
    List<EntityCombatEvent> postStoneLoweringHit;
    int postStoneLoweringSwitchSlot;
    List<int[]> room38ChestPath;
    ChestOpenResult room38Chest;
    int room38ChestSlot;
    int room38DialogId;
    boolean room38RewardObserved;
    boolean room38DialogObserved;
    boolean room38ChestEnded;
    List<int[]> room39ButtonPath;
    int room39EventDeadline;
    int room39ChestDeadline;
    List<int[]> room39ChestPath;
    ChestOpenResult room39Chest;
    int room39ChestSlot;
    boolean room39RewardObserved;
    boolean room39ChestEnded;
    List<int[]> staircaseExitPath;
    List<int[]> staircaseEntryPath;
    List<int[]> sideViewExitPath;
    Warp sideViewExit;
    List<int[]> sideViewReturnPath;
    List<int[]> sideViewWarpPath;
    RoomEntity room2CKeese;
    RoomEntity room2CSpark;
    RoomEntity room2CFloatingItem;
    List<int[]> room28EntryColumnPath;
    RoomEntity hinox;
    RoomEntity unresolvedHinoxWarp;
    int hinoxSlot;
    int hinoxSourceLoadOrder;
    boolean room28LedgeJumpObserved;
    boolean room28LedgeJumpLanded;
    List<int[]> dormantWarpPath;
    SwordApproach hinoxContact;
    int[] firstSwordPosition;
    Sword hinoxSword;
    Sword.CollisionBox hinoxSwordBox;
    List<EntityCombatEvent> hinoxHit;
    boolean hinoxBombResponseObserved;
    int bombDeadline;
    boolean hinoxDeathStarted;
    int deathDeadline;
    int clearEventDeadline;
    RoomEntity clearedWarp;
    List<int[]> disengagePath;
    int awayX;
    int awayY;
    List<int[]> warpPath;
    int warpCountdown;
    RoomEntity room36Warp;
    int room36WarpSlot;
    List<int[]> room36WarpDisengagePath;
    RoomEntity armedRoom36Warp;
    List<int[]> room36WarpContactPath;
    int room36WarpCountdown;
    int[] routeToRoom21;
    int[] routeToRoom21Rooms;
    List<int[]> braceletDoorPath;
    List<Integer> booSlots;
    int braceletRevealDeadline;
    List<int[]> braceletChestPath;
    ChestOpenResult braceletChest;
    int braceletChestSlot;
    boolean braceletRewardObserved;
    boolean braceletChestEnded;
    int[][] room21PotRoute;
    List<int[]> room21EastDoorPath;
    RoomEntity room22CrystalSwitch;
    List<RoomEntity> room22Hearts;
    SwordApproach room22SwitchApproach;
    Sword room22Sword;
    Sword.CollisionBox initialRoom22SwordBox;
    Sword.CollisionBox room22SwordBox;
    List<EntityCombatEvent> room22SwitchHit;
    List<int[]> room22ChestPath;
    ChestOpenResult room22Chest;
    int room22ChestSlot;
    int room22DialogId;
    boolean room22RewardObserved;
    boolean room22DialogObserved;
    boolean room22ChestEnded;
    RoomEntity room27PolsVoice;
    RoomEntity room27Keese;
    RoomEntity room27Moblin;
    int room27LiftDeadline;
    RoomEntity alignedPot;
    int polsDeathDeadline;
    int room27ChestIndex;
    int room27ChestRevealDeadline;
    ChestOpenResult room27Chest;
    int room27ChestSlot;
    int room27DialogId;
    boolean nightmareKeyRewardObserved;
    boolean nightmareKeyDialogObserved;
    boolean room27ChestEnded;
    RoomEntity room24CrystalSwitch;
    SwordApproach room24SwitchApproach;
    Sword room24Sword;
    Sword.CollisionBox initialRoom24SwordBox;
    Sword.CollisionBox room24SwordBox;
    List<EntityCombatEvent> room24SwitchHit;
    int[] room24SwitchReachablePosition;
    List<RoomEntity> room25PolsVoices;
    int[] room25PotLocations;
    int[] room25PotLinkX;
    RoomEntity room25Zol;
    int room25ZolDeadline;
    int room25ZolDeathDeadline;
    int gelDeadline;
    int room25StairRevealDeadline;
    RoomEntity room3BPlatform;
    List<RoomEntity> room3BPots;
    List<EntityCombatEvent> room3BPlatformEvents;
    int firstPlatformY;
    int riddenPlatformY;
    RoomEntity firstPot;
    List<EntityCombatEvent> potPickupEvents;
    List<EntityCombatEvent> throwEvents;
    RoomEntity thrownPot;
    Warp room3BExit;
    Warp room25SideViewEntry;
    Warp room3AWarpTo2D;
    List<RoomEntity> genieEntities;
    RoomEntity genie;
    List<RoomEntity> clearedBossSlots;

    void crossIndoorBoundary(int direction) {
        finishQueuedShutterOpenings();
        RoomNavigationFixture.walkToAndCrossIndoorBoundary(coordinator, transition, scroll, collision, link, direction);
    }

    void crossOverworldBoundary(int direction) {
        RoomNavigationFixture.walkToAndCrossOverworldBoundary(session, coordinator, scroll, collision, link, direction);
    }

    void crossOverworldBoundary(int direction, int nextDirection) {
        RoomNavigationFixture.walkToAndCrossOverworldBoundary(
                session, coordinator, scroll, collision, link, direction, nextDirection);
    }

    void finishTransition() {
        while (transition.isActive()) transition.tick();
    }

    void finishScroll() {
        while (scroll.isActive()) scroll.tick(8);
    }

    void assertInRoom(int expectedRoom) {
        assertEquals(expectedRoom, session.rooms().currentRoomId());
    }

    void tickInteractiveAtCentralApproach() {
        RoomNavigationFixture.tickInteractiveEntities(session, frame++, CENTRAL_APPROACH_X, CENTRAL_APPROACH_Y);
    }

    void tickInteractiveAtLink() {
        RoomNavigationFixture.tickInteractiveEntities(
                session, frame++, link.position().romEntityX(), link.position().romEntityY());
    }

    void awaitShuttersOpen(int mask) {
        finishQueuedShutterOpenings();
        ActiveRoom room = session.rooms().activeRoomOrNull();
        assertEquals(0, room.shutterDoorMask() & mask, "Shutters still closed in room " + room.roomId());
    }

    private void finishQueuedShutterOpenings() {
        RoomPuzzleState.PerRoom puzzle =
                RoomSessionInspection.loadedRoom(session).contents().puzzles();
        for (int tick = 0; tick < 40 && shutterOpeningPending(puzzle); tick++) {
            tickInteractiveAtLink();
        }
        assertFalse(shutterOpeningPending(puzzle), "Shutter opening did not finish");
    }

    private static boolean shutterOpeningPending(RoomPuzzleState.PerRoom puzzle) {
        return puzzle.pendingShutterOpenMask() != 0 || (puzzle.shutterOpening() && puzzle.shutterCloseDirection() >= 0);
    }

    void tickEntitiesAtOriginFacingDown() {
        session.entities().tickEntitiesWithProjectileEvents(frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
    }
}
