package linksawakening.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import linksawakening.world.entity.RoomEntityRuntime;
import org.junit.jupiter.api.Test;

/** Compiled entity ownership and the explicit, shrinking room-migration seam. */
class EntityRuntimeShapeTest {
    private static final String ENTITY = "linksawakening/world/entity/";
    private static final String RUNTIME = ENTITY + "RoomEntityRuntime";
    private static final String ASSEMBLY = ENTITY + "RoomEntityRuntimeBuilder";

    // Exact overloads still used outside world/entity. Caller files and replacements:
    // docs/archive/history/entity-runtime-seam.md. Remove entries when those callers migrate.
    private static final Set<String> SEAM_BRIDGES = Set.of(
            "applyMarinWakeUpPresentation(int,int,int)",
            "beginLift(int,int)",
            "builder(RoomEntitySnapshot)",
            "clearEntity(int)",
            "collectIfNeeded(EntityPickupFrameInputs)",
            "consumeBombExplosionEvents()",
            "consumeHeartPieceUpdates()",
            "consumePendingChestRewardEvents()",
            "consumePendingClearedEntityMask()",
            "consumePendingDialogRequests()",
            "consumePendingDungeonWarpRequests()",
            "consumePendingEntityEvents()",
            "consumePendingHeartContainerRewards()",
            "consumePendingHinoxLinkEffectRequests()",
            "consumePendingInstrumentCompletions()",
            "consumePendingInstrumentRewards()",
            "consumePendingKeyQuicksandEvents()",
            "consumePendingKeyRewardEvents()",
            "consumePendingLikeLikeEvents()",
            "consumePendingLinkAttackClearRequests()",
            "consumePendingLinkAttackStepRequests()",
            "consumePendingLinkFacingRequests()",
            "consumePendingLinkFallenPoseRequests()",
            "consumePendingLinkFinalPositionRequests()",
            "consumePendingLinkHeldItemPoseRequests()",
            "consumePendingLinkMotionBlockRequests()",
            "consumePendingLinkSwordFinalPoseRequests()",
            "consumePendingLinkSwordSpinPoseRequests()",
            "consumePendingMadamMeowMeowFullHealRequests()",
            "consumePendingMusicFadeOutCountdown()",
            "consumePendingMusicTrack()",
            "consumePendingOwlEventCompletions()",
            "consumePendingRichardProgressEvents()",
            "consumePendingRoomStatusMask()",
            "consumePendingRoomStatusPersistenceRequests()",
            "consumePendingRoosterLinkStateRequests()",
            "consumePendingScreenShakeRequests()",
            "consumePendingSideViewPlatformLinkRequests()",
            "consumePendingSideViewPotLinkRequests()",
            "consumePendingSlimeKeyRewardEvents()",
            "consumePendingSwitchBlockAnimationRequest()",
            "consumePendingSwordPickupRewards()",
            "consumePendingThrownBackgroundCollisions()",
            "consumePendingToadstoolRewards()",
            "consumePendingWarpLinkStateRequests()",
            "consumePendingWitchExchangeEvents()",
            "consumePendingWitchRewardEvents()",
            "deferContactRecoil(int,int,int)",
            "gelClingingToLink()",
            "movePushedBlock(int,int,int)",
            "removePushedBlock(int)",
            "resolveCombat(EnemyCombatInputs)",
            "resolveCombat(EntitySwordContactFrame)",
            "resolvePushedBlockLinkContact(int,int,int)",
            "setFreeMovementMode(boolean)",
            "setHeartPiecesCount(int)",
            "spawnArrow(int,int,int,int,boolean)",
            "spawnBomb(int,int,int,int)",
            "spawnBoomerang(int,int,int,int,int)",
            "spawnChestWithItem(int,int,int)",
            "spawnHookshotChain(int,int,int,int)",
            "spawnLiftableRockRubble(int,int)",
            "spawnLiftableRockSmash(int,int,int)",
            "spawnLiftedRoomObject(int,int,int,int)",
            "spawnMagicPowderSprinkle(int,int,int,int)",
            "spawnMagicRodFireball(int,int,int,int,boolean)",
            "spawnPushedBlock(int,int)",
            "spawnRoomEventKeyDrop()",
            "spawnShovelDrop(int,int,int,int,boolean)",
            "spawnSwordBeam(int,int,int,int)",
            "throwLiftedEntity(int)",
            "thrownDirection(int)",
            "updateHouseTarinPresentation(int,int)");

    @Test
    void onlyTheCompositionRootDependsOnTheRuntime() throws Exception {
        var violations = new ArrayList<String>();
        for (var type : ClassFileReferences.classes()) {
            if (!type.name().startsWith(ENTITY)
                    || type.name().equals(RUNTIME)
                    || type.name().equals(ASSEMBLY)) continue;
            for (var reference : type.references()) {
                if (reference.equals(RUNTIME) || reference.startsWith(RUNTIME + "$")) {
                    violations.add(type.name() + " -> " + reference);
                }
            }
        }
        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    @Test
    void runtimeExposesOnlyTickRolesAndTheDeclaredSeam() {
        var observedBridges = new HashSet<String>();
        var violations = new ArrayList<String>();
        for (Method method : RoomEntityRuntime.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) continue;
            String signature = signature(method);
            if (SEAM_BRIDGES.contains(signature)) {
                observedBridges.add(signature);
                continue;
            }
            if (signature.equals("tick(EntityFrameContext)")) continue;
            boolean role = !Modifier.isStatic(method.getModifiers())
                    && method.getParameterCount() == 0
                    && method.getReturnType().getName().startsWith("linksawakening.world.entity.");
            if (!role) violations.add(signature);
        }
        assertTrue(violations.isEmpty(), String.join("\n", violations));
        assertEquals(SEAM_BRIDGES, observedBridges, "Remove stale seam entries when bridges disappear");
    }

    @Test
    void mutableEntityFieldsArePrivateToTheirOwners() throws Exception {
        // The shared reader identifies class files but does not decode PUTFIELD/PUTSTATIC.
        // Enforce the stronger permitted alternative: no non-private mutable fields at all.
        var violations = new ArrayList<String>();
        for (var type : ClassFileReferences.classes()) {
            if (!type.name().startsWith(ENTITY)) continue;
            Class<?> owner = Class.forName(
                    type.name().replace('/', '.'), false, getClass().getClassLoader());
            for (var field : owner.getDeclaredFields()) {
                int flags = field.getModifiers();
                if (!Modifier.isPrivate(flags) && !Modifier.isFinal(flags)) {
                    violations.add(type.name() + "." + field.getName());
                }
            }
        }
        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    private static String signature(Method method) {
        return method.getName() + "("
                + Arrays.stream(method.getParameterTypes())
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(",")) + ")";
    }
}
