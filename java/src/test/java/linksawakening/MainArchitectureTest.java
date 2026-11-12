package linksawakening;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.app.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.screen.*;
import org.junit.jupiter.api.Test;

final class MainArchitectureTest {
    private static final String P = "linksawakening.app.gameplay.";

    private static void check(String type, String method, String... calls) {
        CompiledCode.calls(P + type, method, calls);
    }

    @Test
    void entryPointOwnsNoGameStateOrBackend() throws Exception {
        assertEquals(0, Main.class.getDeclaredFields().length);
        assertEquals(1, Main.class.getDeclaredMethods().length);
    }

    @Test
    void sessionStateIsNeverStaticAndScreensImplementLifecycle() throws Exception {
        assertFalse(java.util.Arrays.stream(GameSession.class.getDeclaredFields())
                .anyMatch(f -> java.lang.reflect.Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()));
        for (Class<?> screen : java.util.List.of(
                TitleScreen.class,
                CutsceneScreen.class,
                FileMenuScreen.class,
                FileSaveScreen.class,
                GameplayScreen.class)) assertTrue(Screen.class.isAssignableFrom(screen));
        assertEquals(
                java.util.List.of(
                        GameplayFrame.Phase.TRANSITIONS,
                        GameplayFrame.Phase.SIMULATION,
                        GameplayFrame.Phase.INPUT_COMPLETION),
                GameplayFrame.phaseOrder());
        CompiledCode.calls(
                "linksawakening.app.gameplay.GameplaySimulation",
                "tick",
                "DialogStoryPhase.tick",
                "LinkEquipmentPhase.tick",
                "CombatPhase.tick",
                "EntityPhase.tick",
                "completeGameplayFrame",
                "AnimatedTilesPhase.tick");
        assertFalse(CompiledCode.code("linksawakening.app.GameSession").contains("org/lwjgl"));
        assertFalse(CompiledCode.code("linksawakening.app.SessionBootstrap").contains("audio/openal"));
        var directory = java.nio.file.Path.of(Class.forName("linksawakening.app.gameplay.GameplaySimulation")
                .getResource("")
                .toURI());
        try (var classes = java.nio.file.Files.list(directory)) {
            for (var file : classes.filter(f -> f.toString().endsWith(".class")).toList()) {
                String name = file.getFileName().toString().replace(".class", "");
                assertFalse(
                        CompiledCode.code("linksawakening.app.gameplay." + name).contains("org/lwjgl"), name);
            }
        }
    }

    @Test
    void mainConsumesProjectileEventsAtTheEntityFrameBoundary() throws Exception {
        check("EntityPhase", "tick", "tickEntities", "EnemyProjectileEventConsumer.consume");
    }

    @Test
    void mainWiresSwordAndTailCaveCompletionAtTheEntityFrameBoundary() throws Exception {
        check("CombatPhase", "tick", "enemyCollisionBox", "resolveEntityCombat");
        check("EntityPhase", "tick", "isActive", "isMusicPlaying", "tickEntities");
        check("EntityPhase", "tick", "consumeHeartContainerRewards", "applyHeartContainerReward");
        check(
                "EntityPhase",
                "tick",
                "EnemyCombatEventConsumer.consume",
                "consumePendingMusicTrack",
                "playDirectMusic",
                "openEntityDialogRequests",
                "handlePendingInstrumentTransition");
    }

    @Test
    void mainAppliesTarinLinkWritesAtTheEntityFrameBoundary() throws Exception {
        check("EntityPhase", "tick", "consumeAttackWrites", "LinkCommandAdapter.from", "Link.apply");
        assertTrue(CompiledCode.code("linksawakening.entity.LinkCommand$Facing")
                .contains("faceRomDirectionPreservingWalkPhase"));
        assertTrue(CompiledCode.code("linksawakening.entity.LinkCommand$Action")
                .contains("clearRomAttackStepAnimationCountdown"));
    }

    @Test
    void mainMergesLinkAttackWritesInDescendingEntitySlotOrder() throws Exception {
        CompiledCode.calls(
                "linksawakening.world.room.RoomSession",
                "consumeAttackWrites",
                "consumeLinkAttackClearRequests",
                "consumeLinkAttackStepRequests",
                "RoomLinkWrites.attacks");
        assertTrue(CompiledCode.code("linksawakening.world.room.RoomLinkWrites").contains("sourceSlot"));
    }

    @Test
    void mainDoesNotSuppressHinoxThrowDamageBehindInvincibilityGate() throws Exception {
        String code = CompiledCode.method(P + "EntityPhase", "tick");
        String hinox = code.substring(code.indexOf("consumeHinoxLinkEffectRequests"));
        assertTrue(hinox.contains("applyRomEnemyDamage"));
        assertFalse(hinox.contains("invincibilityCounter"));
    }

    @Test
    void mainCapturesLinkFinalPositionBeforeLinkMotionForEntityPushes() throws Exception {
        check("LinkEquipmentPhase", "tick", "captureRomFinalPosition", "Link.update");
    }

    @Test
    void mainMergesSideViewPotContactWithOtherDescendingLinkWrites() throws Exception {
        CompiledCode.calls(
                "linksawakening.world.room.RoomSession",
                "consumePositionWrites",
                "consumeSideViewPlatformLinkRequests",
                "consumeSideViewPotLinkRequests",
                "consumeLinkFinalPositionRequests",
                "RoomLinkWrites.positions");
        assertTrue(CompiledCode.code(P + "EntityPhase").contains("LinkCommandAdapter.from"));
        assertTrue(CompiledCode.code("linksawakening.entity.LinkCommand$PotContact")
                .contains("applySideViewPotContact"));
        assertTrue(CompiledCode.code(P + "EntityPhase").contains("romFinalPositionX"));
    }

    @Test
    void mainAppliesHookshotPullEventsWithoutProjectileIgnoreSideEffects() throws Exception {
        assertTrue(CompiledCode.code(P + "EntityPhase").contains("HOOKSHOT_PULL"));
        check("EntityPhase", "tick", "HOOKSHOT_PULL", "LinkCommandAdapter.from", "Link.apply");
        CompiledCode.calls(
                "linksawakening.entity.LinkCommand$ProjectileResponse",
                "applyTo",
                "applyRomFinalPosition",
                "applyRomSpeed",
                "setCollisionIgnoreFrames");
    }

    @Test
    void mainRegistersBombWithLinkRomPlacementState() throws Exception {
        String code = CompiledCode.implementation(
                SessionBootstrap.class, linksawakening.equipment.Bomb.PlacementTarget.class);
        assertTrue(code.contains("placeBomb"));
        assertTrue(code.contains("romEntityX"));
        assertTrue(code.contains("romEntityY"));
        assertTrue(code.contains("romEntityZ"));
        assertTrue(code.contains("startRomItemAttackStep"));
    }

    @Test
    void mainStartsLinkItemAttackStepAfterASuccessfulBombThrow() throws Exception {
        check("LinkEquipmentPhase", "tick", "throwLiftedEntity");
        check("EntityPhase", "tick", "tickEntities", "consumeAttackWrites");
    }

    @Test
    void roomSessionExposesOnlyTheCanonicalBombPlacementBridge() throws Exception {
        var bridges = java.util.Arrays.stream(linksawakening.world.room.RoomSession.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("placeBomb"))
                .toList();
        assertEquals(1, bridges.size());
        assertEquals(4, bridges.getFirst().getParameterCount());
    }
}
