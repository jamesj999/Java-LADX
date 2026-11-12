package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomFacadeCompatibilityTest {
    @Test
    void allOriginalExternalDelegatesRemainAccessibleAndMarkedForRemoval() throws Exception {
        var methods = List.of(
                RoomSession.class.getMethod("activeProjectileCount"),
                RoomSession.class.getMethod("activeRoom"),
                RoomSession.class.getMethod("advanceBoomerangSoundClock", boolean.class),
                RoomSession.class.getMethod("advanceShovel", int.class, int.class, int.class, int.class),
                RoomSession.class.getMethod("applyMarinWakeUpPresentation", int.class, int.class, int.class),
                RoomSession.class.getMethod("bombActive"),
                RoomSession.class.getMethod("boomerangActive"),
                RoomSession.class.getMethod("bowWowState"),
                RoomSession.class.getMethod("breakIndoorSwordCrystal", int.class),
                RoomSession.class.getMethod("clearEntity", int.class),
                RoomSession.class.getMethod(
                        "collectEntityIfNeeded",
                        int.class,
                        int.class,
                        int.class,
                        boolean.class,
                        boolean.class,
                        int.class,
                        int.class,
                        boolean.class,
                        int.class,
                        int.class,
                        int.class,
                        int.class),
                RoomSession.class.getMethod("colorDungeonItemFlagsSnapshot"),
                RoomSession.class.getMethod("colorDungeonRoomStatusSnapshot"),
                RoomSession.class.getMethod("consumeEntityDialogRequests"),
                RoomSession.class.getMethod("consumeLinkAttackClearRequests"),
                RoomSession.class.getMethod("consumeSwordPickupRewards"),
                RoomSession.class.getMethod("currentDungeonItemFlagsSnapshot"),
                RoomSession.class.getMethod("currentRoomId"),
                RoomSession.class.getMethod("deferContactRecoil", int.class, int.class, int.class),
                RoomSession.class.getMethod("dungeonItemFlagsSnapshot"),
                RoomSession.class.getMethod("dungeonMinimapRoomStatusSnapshot"),
                RoomSession.class.getMethod("dungeonProgressFlagsSnapshot"),
                RoomSession.class.getMethod("entityHealth", int.class),
                RoomSession.class.getMethod("fireBoomerang", int.class, int.class, int.class, int.class, int.class),
                RoomSession.class.getMethod(
                        "fireHookshot", int.class, int.class, int.class, int.class, boolean.class, boolean.class),
                RoomSession.class.getMethod(
                        "fireMagicRodFireball", int.class, int.class, int.class, int.class, boolean.class),
                RoomSession.class.getMethod("fireSwordBeam", int.class, int.class, int.class, int.class),
                RoomSession.class.getMethod("gotItemPresentationActive"),
                RoomSession.class.getMethod("hasActiveRoom"),
                RoomSession.class.getMethod("heartPiecePickupActive"),
                RoomSession.class.getMethod("hookshotActive"),
                RoomSession.class.getMethod("indoorARoomStatusSnapshot"),
                RoomSession.class.getMethod("indoorBRoomStatusSnapshot"),
                RoomSession.class.getMethod("indoorRoomPositionForSave"),
                RoomSession.class.getMethod("initializeNewGameWorldState"),
                RoomSession.class.getMethod("instrumentTrace"),
                RoomSession.class.getMethod("lastBombPlacementPlayedBump"),
                RoomSession.class.getMethod("liftedEntityState"),
                RoomSession.class.getMethod("loadIndoor", int.class, int.class),
                RoomSession.class.getMethod("loadIndoorFromSavedPosition", int.class, int.class, int.class),
                RoomSession.class.getMethod("loadInitialOverworld", int.class),
                RoomSession.class.getMethod("loadOverworld", int.class),
                RoomSession.class.getMethod("loadWarpDestination", linksawakening.world.transition.Warp.class),
                RoomSession.class.getMethod("mapCategory"),
                RoomSession.class.getMethod("mapId"),
                RoomSession.class.getMethod("moblinKingTrace"),
                RoomSession.class.getMethod("ocarinaAnimationState"),
                RoomSession.class.getMethod("ocarinaPlaying"),
                RoomSession.class.getMethod("overworldRoomStatusSnapshot"),
                RoomSession.class.getMethod("palettes"),
                RoomSession.class.getMethod("placeBomb", int.class, int.class, int.class, int.class),
                RoomSession.class.getMethod("powerUpPickupSequenceActive"),
                RoomSession.class.getMethod("renderSnapshot"),
                RoomSession.class.getMethod(
                        "resolveEntityCombat",
                        int.class,
                        int.class,
                        int.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        linksawakening.world.entity.EnemyAttackContext.class),
                RoomSession.class.getMethod(
                        "resolveEntityCombat",
                        int.class,
                        int.class,
                        int.class,
                        boolean.class,
                        int.class,
                        boolean.class,
                        boolean.class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        linksawakening.world.entity.EnemyAttackContext.class),
                RoomSession.class.getMethod("restoreDungeonItemFlags", byte[].class, byte[].class),
                RoomSession.class.getMethod("restoreDungeonProgressFlags", byte[].class),
                RoomSession.class.getMethod(
                        "restoreRoomStatuses", byte[].class, byte[].class, byte[].class, byte[].class),
                RoomSession.class.getMethod("richardSpokenFlag"),
                RoomSession.class.getMethod("setBirdKeyOwned", boolean.class),
                RoomSession.class.getMethod("setBowWowState", int.class),
                RoomSession.class.getMethod("setChestPlayerLevels", int.class, int.class, int.class),
                RoomSession.class.getMethod("setColorShellSoundSink", linksawakening.gameplay.GameplaySoundSink.class),
                RoomSession.class.getMethod("setDungeonItemForDebug", int.class, int.class, int.class),
                RoomSession.class.getMethod("setEntityActionButtonsHeld", boolean.class, boolean.class),
                RoomSession.class.getMethod("setEntityAttackStepAnimationCountdown", int.class),
                RoomSession.class.getMethod("setEntityBombButtonHeld", boolean.class),
                RoomSession.class.getMethod("setEntityDefaultMusicTrack", int.class),
                RoomSession.class.getMethod("setEntityDialogActive", boolean.class),
                RoomSession.class.getMethod("setEntityPowerBraceletButtonHeld", boolean.class),
                RoomSession.class.getMethod("setEntityTalkState", boolean.class, int.class, int.class),
                RoomSession.class.getMethod("setHouseNpcTradeItem", int.class),
                RoomSession.class.getMethod("setRichardSpokenFlag", int.class),
                RoomSession.class.getMethod(
                        "setSecretSeashellPegasusCollisionState", boolean.class, boolean.class, int.class, int.class),
                RoomSession.class.getMethod("setTailKeyOwned", boolean.class),
                RoomSession.class.getMethod("setTarinFlag", int.class),
                RoomSession.class.getMethod("setToadstoolPlayerState", boolean.class, int.class),
                RoomSession.class.getMethod(
                        "shootArrowResult", int.class, int.class, int.class, int.class, boolean.class),
                RoomSession.class.getMethod("shouldGetLostInMysteriousWoods"),
                RoomSession.class.getMethod("shovelAnimationState", int.class, int.class),
                RoomSession.class.getMethod("sprinkleMagicPowder", int.class, int.class, int.class, int.class),
                RoomSession.class.getMethod(
                        "startAdjacentOverworldScroll",
                        linksawakening.world.transition.ScrollController.class,
                        int.class,
                        int.class,
                        int.class),
                RoomSession.class.getMethod("startOcarina", int.class, int.class, int.class),
                RoomSession.class.getMethod("startShovel", int.class, int.class, int.class, boolean.class),
                RoomSession.class.getMethod("swordPickupSequenceActive"),
                RoomSession.class.getMethod("tailCaveKeyholeSequenceActive"),
                RoomSession.class.getMethod("tarinFlag"),
                RoomSession.class.getMethod("throwLiftedEntity", int.class),
                RoomSession.class.getMethod(
                        "tickCrackedFloor", linksawakening.world.room.RoomSession.LinkGroundFrame.class, boolean.class),
                RoomSession.class.getMethod(
                        "tickCrackedFloorOnLanding",
                        linksawakening.world.room.RoomSession.LinkGroundFrame.class,
                        boolean.class),
                RoomSession.class.getMethod(
                        "tickDungeonTitleMessage", boolean.class, boolean.class, int.class, boolean.class),
                RoomSession.class.getMethod("tickEntities", int.class, int.class, int.class),
                RoomSession.class.getMethod(
                        "tickEntities", int.class, int.class, int.class, int.class, boolean.class, int.class),
                RoomSession.class.getMethod("tickEntities", int.class, int.class, int.class, int.class, int.class),
                RoomSession.class.getMethod(
                        "tickEntitiesWithProjectileEvents",
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        boolean.class),
                RoomSession.class.getMethod("tickGameplayVBlank"),
                RoomSession.class.getMethod("tickTailCaveKeyholeSequence"),
                RoomSession.class.getMethod("tryInteractWithIndoorBlock", int.class, int.class, int.class, int.class),
                RoomSession.class.getMethod(
                        "tryLiftObject",
                        int.class,
                        int.class,
                        int.class,
                        boolean.class,
                        int.class,
                        boolean.class,
                        int.class,
                        boolean.class),
                RoomSession.class.getMethod("tryOpenChest", int.class, int.class, int.class, boolean.class, int.class),
                RoomSession.class.getMethod("tryUnlockIndoorKeyDoor", int.class, int.class, int.class, int.class),
                RoomSession.class.getMethod(
                        "tryUnlockKeyCavernKeyhole", int.class, int.class, int.class, int.class, int.class),
                RoomSession.class.getMethod(
                        "tryUnlockTailCaveKeyhole", int.class, int.class, int.class, int.class, boolean.class),
                RoomSession.class.getMethod("updateHouseTarinPresentation", int.class, int.class, int.class));
        assertEquals(104, methods.size());
        for (var method : methods) {
            assertTrue(Modifier.isPublic(method.getModifiers()), method.toString());
            assertTrue(Modifier.isPublic(method.getDeclaringClass().getModifiers()), method.toString());
            var annotation = method.getAnnotation(Deprecated.class);
            assertNotNull(annotation, method.toString());
            assertTrue(annotation.forRemoval(), method.toString());
        }
    }
}
