package linksawakening;
import linksawakening.app.*;
import linksawakening.app.screen.*;
import linksawakening.app.gameplay.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MainArchitectureTest {
    private static final String P = "linksawakening.app.gameplay.";
    private static void check(String type, String method, String... calls) { CompiledCode.calls(P + type, method, calls);
    }
    @Test
    void entryPointOwnsNoGameStateOrBackend() throws Exception {
        assertEquals(0, Main.class.getDeclaredFields().length);
        assertEquals(1, Main.class.getDeclaredMethods().length);
    }
    @Test
    void sessionStateIsNeverStaticAndScreensImplementLifecycle() throws Exception {
        assertFalse(java.util.Arrays.stream(GameSession.class.getDeclaredFields()).anyMatch(f -> java.lang.reflect.Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()));
        for (Class<?> screen : java.util.List.of(TitleScreen.class, CutsceneScreen.class, FileMenuScreen.class, FileSaveScreen.class, GameplayScreen.class)) assertTrue(Screen.class.isAssignableFrom(screen));
        assertEquals(java.util.List.of(GameplayFrame.Phase.TRANSITIONS, GameplayFrame.Phase.SIMULATION, GameplayFrame.Phase.INPUT_COMPLETION), GameplayFrame.phaseOrder());
        CompiledCode.calls("linksawakening.app.gameplay.GameplaySimulation", "tick",
        "DialogStoryPhase.tick", "LinkEquipmentPhase.tick", "CombatPhase.tick",
        "EntityPhase.tick", "completeGameplayFrame", "AnimatedTilesPhase.tick");
        assertFalse(CompiledCode.code("linksawakening.app.GameSession").contains("org/lwjgl"));
        assertFalse(CompiledCode.code("linksawakening.app.SessionBootstrap").contains("audio/openal"));
        var directory = java.nio.file.Path.of(Class.forName("linksawakening.app.gameplay.GameplaySimulation").getResource("").toURI());
        try (var classes = java.nio.file.Files.list(directory)) {
            for (var file : classes.filter(f -> f.toString().endsWith(".class")).toList()) {
                String name = file.getFileName().toString().replace(".class", "");
                assertFalse(CompiledCode.code("linksawakening.app.gameplay." + name).contains("org/lwjgl"), name);
            }
        }
    }
    @Test
    void mainConsumesProjectileEventsAtTheEntityFrameBoundary() throws Exception {
        check("EntityPhase", "tick", "tickEntitiesWithProjectileEvents", "EnemyProjectileEventConsumer.consume");
    }
    @Test
    void mainWiresSwordAndTailCaveCompletionAtTheEntityFrameBoundary() throws Exception {
        check("CombatPhase", "tick", "enemyCollisionBox", "resolveEntityCombat");
        check("EntityPhase", "tick", "setEntityDialogActive", "setEntityMusicActive", "tickEntitiesWithProjectileEvents");
        check("EntityPhase", "tick", "consumeHeartContainerRewards", "applyHeartContainerReward");
        check("EntityPhase", "tick", "EnemyCombatEventConsumer.consume", "consumePendingMusicTrack", "playDirectMusic", "openEntityDialogRequests", "handlePendingInstrumentTransition");
    }
    @Test
    void mainAppliesTarinLinkWritesAtTheEntityFrameBoundary() throws Exception {
        check("EntityPhase", "tick", "consumeLinkAttackClearRequests", "clearRomAttackStepAnimationCountdown");
        assertTrue(CompiledCode.code(P + "EntityPhase").contains("faceRomDirectionPreservingWalkPhase"));
    }
    @Test
    void mainMergesLinkAttackWritesInDescendingEntitySlotOrder() throws Exception {
        check("EntityPhase", "tick", "consumeLinkAttackClearRequests", "consumeLinkAttackStepRequests");
        assertTrue(CompiledCode.code(P + "EntityPhase").contains("sourceSlot"));
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
        check("EntityPhase", "tick", "consumeSideViewPotLinkRequests", "consumeLinkFinalPositionRequests");
        assertTrue(CompiledCode.code(P + "EntityPhase").contains("applySideViewPotContact"));
        assertTrue(CompiledCode.code(P + "EntityPhase").contains("setEntityFinalPositionX"));
    }
    @Test
    void mainAppliesHookshotPullEventsWithoutProjectileIgnoreSideEffects() throws Exception {
        assertTrue(CompiledCode.code(P + "EntityPhase").contains("HOOKSHOT_PULL"));
        check("EntityPhase", "tick", "applyRomFinalPosition", "applyRomSpeed");
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
        check("EntityPhase", "tick", "tickEntitiesWithProjectileEvents", "consumeLinkAttackStepRequests");
    }
    @Test
    void roomSessionExposesOnlyTheCanonicalBombPlacementBridge() throws Exception {
        var bridges = java.util.Arrays.stream(linksawakening.world.RoomSession.class.getDeclaredMethods()).filter(m -> m.getName().equals("placeBomb")).toList();
        assertEquals(1, bridges.size());
        assertEquals(4, bridges.getFirst().getParameterCount());
    }
}
