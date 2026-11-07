package linksawakening;

import linksawakening.app.screen.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MainArchitectureTest {
    private static final GameSession game = new GameSession();

    @Test
    void entryPointOwnsNoGameStateOrBackend() {
        assertTrue(Main.class.getDeclaredFields().length == 0);
        assertTrue(java.util.Arrays.stream(Main.class.getDeclaredMethods())
            .allMatch(method -> method.getName().equals("main")));
    }

    @Test
    void sessionStateIsNeverStaticAndScreensImplementLifecycle() {
        assertFalse(java.util.Arrays.stream(GameSession.class.getDeclaredFields())
            .anyMatch(field -> java.lang.reflect.Modifier.isStatic(field.getModifiers())
                && !field.isSynthetic()));
        for (Class<?> screen : java.util.List.of(TitleScreen.class, CutsceneScreen.class,
                FileMenuScreen.class, FileSaveScreen.class, GameplayScreen.class)) {
            assertTrue(Screen.class.isAssignableFrom(screen));
        }
        org.junit.jupiter.api.Assertions.assertEquals(
            java.util.List.of(GameplayFrame.Phase.TRANSITIONS,
                GameplayFrame.Phase.SIMULATION, GameplayFrame.Phase.INPUT_COMPLETION),
            GameplayFrame.phaseOrder());
    }

    @Test
    void mainConsumesProjectileEventsAtTheEntityFrameBoundary() throws Exception {
        String source = ((((((Files.readString(Path.of("src/main/java/linksawakening/GameSession.java")) + (Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogStoryPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/CombatPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/EntityPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/AnimatedTilesPhase.java")))) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogInteractions.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SaveCoordinator.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SessionBootstrap.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/story/OpeningStory.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/screen/FileMenuScreen.java")));

        assertTrue(source.contains("tickEntitiesWithProjectileEvents"));
        assertTrue(source.contains("link.romCollisionType()"));
        assertTrue(source.contains("EnemyProjectileEventConsumer.consume"));
    }

    @Test
    void mainWiresSwordAndTailCaveCompletionAtTheEntityFrameBoundary() throws Exception {
        String source = ((((((Files.readString(Path.of("src/main/java/linksawakening/GameSession.java")) + (Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogStoryPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/CombatPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/EntityPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/AnimatedTilesPhase.java")))) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogInteractions.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SaveCoordinator.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SessionBootstrap.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/story/OpeningStory.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/screen/FileMenuScreen.java")));
        String normalizedSource = source.replaceAll("\\s+", " ");

        int swordBox = source.indexOf("sword.enemyCollisionBox(");
        int combat = source.indexOf("roomSession.resolveEntityCombat(");
        assertTrue(swordBox >= 0 && swordBox < combat,
            "Main must forward the live Sword collision box into entity combat");
        assertTrue(normalizedSource.contains(
            "swordBoxForEntityTick.active(), swordBoxForEntityTick.x(), "
                + "swordBoxForEntityTick.width(), swordBoxForEntityTick.y(), "
                + "swordBoxForEntityTick.height(), attackContext)"));

        int dialogGate = source.indexOf("roomSession.setEntityDialogActive(");
        int musicGate = source.indexOf("roomSession.setEntityMusicActive(");
        int entityTick = source.indexOf("roomSession.tickEntitiesWithProjectileEvents(");
        assertTrue(dialogGate >= 0 && dialogGate < entityTick);
        assertTrue(musicGate >= 0 && musicGate < entityTick);

        int heartReward = source.indexOf("roomSession.consumeHeartContainerRewards()");
        int applyHeart = source.indexOf("playerState.applyHeartContainerReward()");
        int pendingMusic = source.indexOf("roomSession.consumePendingMusicTrack()");
        int playPendingMusic = source.indexOf("playDirectMusic(chestMusicTrack)");
        int openEntityDialog = source.indexOf("openEntityDialogRequests();");
        int consumeEntityAudio = source.indexOf(
            "EnemyCombatEventConsumer.consume(roomSession.consumeEntityEvents()");
        int instrumentWarp = source.indexOf(
            "roomTransitionCoordinator.handlePendingInstrumentTransition(link)");
        assertTrue(heartReward >= 0 && heartReward < applyHeart);
        assertTrue(consumeEntityAudio >= 0 && consumeEntityAudio < instrumentWarp,
            "Main must audibly consume the instrument warp jingle before transition");
        assertTrue(pendingMusic >= 0 && pendingMusic < playPendingMusic
            && playPendingMusic < instrumentWarp,
            "Main must play the instrument's pending music before transition");
        assertTrue(openEntityDialog >= 0 && openEntityDialog < instrumentWarp,
            "Main must open the instrument dialog before transition");
    }

    @Test
    void mainAppliesTarinLinkWritesAtTheEntityFrameBoundary() throws Exception {
        String source = ((((((Files.readString(Path.of("src/main/java/linksawakening/GameSession.java")) + (Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogStoryPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/CombatPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/EntityPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/AnimatedTilesPhase.java")))) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogInteractions.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SaveCoordinator.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SessionBootstrap.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/story/OpeningStory.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/screen/FileMenuScreen.java")));

        assertTrue(source.contains("consumeLinkAttackClearRequests"));
        assertTrue(source.contains("link.clearRomAttackStepAnimationCountdown()"));
        assertTrue(source.contains("link.faceRomDirectionPreservingWalkPhase("));
    }

    @Test
    void mainMergesLinkAttackWritesInDescendingEntitySlotOrder() throws Exception {
        String source = ((((((Files.readString(Path.of("src/main/java/linksawakening/GameSession.java")) + (Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogStoryPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/CombatPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/EntityPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/AnimatedTilesPhase.java")))) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogInteractions.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SaveCoordinator.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SessionBootstrap.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/story/OpeningStory.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/screen/FileMenuScreen.java")));
        String normalizedSource = source.replaceAll("\\s+", " ");

        assertTrue(normalizedSource.contains("int linkAttackClearIndex = 0;"));
        assertTrue(normalizedSource.contains("int linkAttackStepIndex = 0;"));
        assertTrue(normalizedSource.contains("consumeLinkAttackClearRequests()"));
        assertTrue(normalizedSource.contains("consumeLinkAttackStepRequests()"));
        assertTrue(normalizedSource.contains("sourceSlot()"));
        assertTrue(normalizedSource.contains("while (linkAttackClearIndex <"));
    }

    @Test
    void mainDoesNotSuppressHinoxThrowDamageBehindInvincibilityGate() throws Exception {
        String normalizedSource = ((((((Files.readString(Path.of("src/main/java/linksawakening/GameSession.java")) + (Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogStoryPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/CombatPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/EntityPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/AnimatedTilesPhase.java")))) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogInteractions.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SaveCoordinator.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SessionBootstrap.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/story/OpeningStory.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/screen/FileMenuScreen.java")))
            .replaceAll("\\s+", " ");

        assertFalse(normalizedSource.contains(
            "request.damage() != 0 && playerState != null && playerState.invincibilityCounter() == 0"));
    }

    @Test
    void mainCapturesLinkFinalPositionBeforeLinkMotionForEntityPushes() throws Exception {
        String source = ((((((Files.readString(Path.of("src/main/java/linksawakening/GameSession.java")) + (Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogStoryPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/CombatPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/EntityPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/AnimatedTilesPhase.java")))) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogInteractions.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SaveCoordinator.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SessionBootstrap.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/story/OpeningStory.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/screen/FileMenuScreen.java")));

        int capture = source.indexOf("link.captureRomFinalPosition();");
        int update = source.indexOf("link.update();");
        assertTrue(capture >= 0, "Link final position must be captured for entity pushes");
        assertTrue(update >= 0, "Main must run Link motion");
        assertTrue(capture < update,
            "The ROM final-position shadow must be captured before Link moves");
    }

    @Test
    void mainMergesSideViewPotContactWithOtherDescendingLinkWrites() throws Exception {
        String source = ((((((Files.readString(Path.of("src/main/java/linksawakening/GameSession.java")) + (Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogStoryPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/CombatPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/EntityPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/AnimatedTilesPhase.java")))) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogInteractions.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SaveCoordinator.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SessionBootstrap.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/story/OpeningStory.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/screen/FileMenuScreen.java")));
        String normalizedSource = source.replaceAll("\\s+", " ");

        int potConsume = normalizedSource.indexOf(
            "consumeSideViewPotLinkRequests()");
        int finalConsume = normalizedSource.indexOf(
            "consumeLinkFinalPositionRequests()");
        assertTrue(potConsume >= 0 && finalConsume > potConsume);
        assertTrue(normalizedSource.contains("link.restoreRomFinalPositionX()"));
        assertTrue(normalizedSource.contains("link.applySideViewPotContact("));
        assertTrue(normalizedSource.contains("potContactRequestIndex"));
        assertTrue(normalizedSource.contains("setEntityFinalPositionX("));
    }

    @Test
    void mainAppliesHookshotPullEventsWithoutProjectileIgnoreSideEffects() throws Exception {
        String source = ((((((Files.readString(Path.of("src/main/java/linksawakening/GameSession.java")) + (Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogStoryPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/CombatPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/EntityPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/AnimatedTilesPhase.java")))) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogInteractions.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SaveCoordinator.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SessionBootstrap.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/story/OpeningStory.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/screen/FileMenuScreen.java")));
        String normalizedSource = source.replaceAll("\\s+", " ");

        assertTrue(source.contains("EntityProjectileEvent.Kind.HOOKSHOT_PULL"));
        assertTrue(normalizedSource.contains(
            "if (hookshotPull) { link.applyRomFinalPosition(event.linkSpeedX(), event.linkSpeedY()); "
                + "continue; } link.applyRomSpeed(event.linkSpeedX(), event.linkSpeedY());"));
        assertTrue(source.contains("link.applyRomFinalPosition(event.linkSpeedX(), event.linkSpeedY())"));
        assertTrue(source.contains("link.applyRomSpeed(event.linkSpeedX(), event.linkSpeedY())"));
    }

    @Test
    void mainRegistersBombWithLinkRomPlacementState() throws Exception {
        String source = ((((((Files.readString(Path.of("src/main/java/linksawakening/GameSession.java")) + (Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogStoryPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/CombatPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/EntityPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/AnimatedTilesPhase.java")))) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogInteractions.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SaveCoordinator.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SessionBootstrap.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/story/OpeningStory.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/screen/FileMenuScreen.java")));
        String normalizedSource = source.replaceAll("\\s+", " ");

        assertTrue(source.contains("PlayerState.INVENTORY_BOMBS"));
        assertTrue(source.contains("new Bomb("));
        assertTrue(source.contains("roomSession.placeBomb("));
        assertTrue(source.contains("link.romEntityX()"));
        assertTrue(source.contains("link.romEntityY()"));
        assertTrue(source.contains("link.romEntityZ()"));
        assertTrue(source.contains("callbacks.romDirection().applyAsInt(link.direction())"));
        assertTrue(source.contains("link::canUseItems"));
        assertTrue(normalizedSource.contains(
            "if (placed) { link.startRomItemAttackStep(); } return placed;"));
        assertFalse(normalizedSource.contains(
            "roomSession.placeBomb( link.romEntityX(), link.romEntityY(), link.romEntityZ(), "
                + "callbacks.romDirection().applyAsInt(link.direction()), link.isAirborne(), false"));
    }

    @Test
    void mainStartsLinkItemAttackStepAfterASuccessfulBombThrow() throws Exception {
        String source = ((((((Files.readString(Path.of("src/main/java/linksawakening/GameSession.java")) + (Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogStoryPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/CombatPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/EntityPhase.java")) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/AnimatedTilesPhase.java")))) + Files.readString(Path.of("src/main/java/linksawakening/app/gameplay/DialogInteractions.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SaveCoordinator.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/SessionBootstrap.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/story/OpeningStory.java"))) + Files.readString(Path.of("src/main/java/linksawakening/app/screen/FileMenuScreen.java")));
        String normalizedSource = source.replaceAll("\\s+", " ");

        assertTrue(normalizedSource.contains(
            "boolean bombBeingThrown = liftedState.type() == 0x02;"));
        assertTrue(normalizedSource.contains(
            "roomSession.throwLiftedEntity(link.direction());"));
        assertTrue(normalizedSource.contains(
            "var linkAttackStepRequests = roomSession.consumeLinkAttackStepRequests();"));
        int entityTick = source.indexOf("roomSession.tickEntitiesWithProjectileEvents(");
        int deferredAttackStep = source.indexOf("consumeLinkAttackStepRequests");
        assertTrue(entityTick >= 0 && deferredAttackStep > entityTick,
            "Lifted-throw attack-step writes must be deferred until after entity animation");
        assertFalse(normalizedSource.contains(
            "if (thrown) { link.startRomItemAttackStep(); }"));
    }

    @Test
    void roomSessionExposesOnlyTheCanonicalBombPlacementBridge() throws Exception {
        String source = Files.readString(Path.of("src/main/java/linksawakening/world/RoomSession.java"));
        String normalizedSource = source.replaceAll("\\s+", " ");

        assertTrue(normalizedSource.contains(
            "public boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection)"));
        assertFalse(normalizedSource.contains(
            "public boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, "
                + "int romDirection, boolean"));
    }
}
