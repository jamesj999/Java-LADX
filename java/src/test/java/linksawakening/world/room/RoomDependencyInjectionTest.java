package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomDependencyInjectionTest {
    @Test
    void runtimeCollaboratorsNeverRetainTheFacadeOrWiringBag() {
        for (Class<?> type : List.of(
                RoomEntityCombatBridge.class,
                RoomCollisionQueries.class,
                RoomTransientState.class,
                RoomTransitionController.class,
                RoomLoadCoordinator.class,
                LinkOutputs.class,
                RoomEntityRewards.class,
                RewardOutputs.class,
                RoomEntityBridge.class,
                RoomEntityFrameSetup.class,
                RoomPlayerItemActions.class,
                RoomTransitionCoordinator.class,
                RoomStoryState.class,
                RoomSessionViews.class,
                RoomPuzzleController.class,
                PresentationOutputs.class,
                RoomTileRedraw.class,
                RoomEntityAfterTick.class,
                RoomDoorInteractions.class,
                RoomObjectMutations.class)) {
            for (var field : type.getDeclaredFields()) {
                assertFalse(
                        List.of(RoomSession.class, RoomSessionContext.class, RoomSessionComponents.class)
                                .contains(field.getType()),
                        type.getName() + "." + field.getName());
            }
            for (var nested : type.getDeclaredClasses()) {
                if (!nested.isRecord()) continue;
                for (var component : nested.getRecordComponents()) {
                    assertFalse(
                            List.of(RoomSession.class, RoomSessionContext.class, RoomSessionComponents.class)
                                    .contains(component.getType()),
                            type.getName() + "." + component.getName());
                }
            }
        }
    }

    @Test
    void compositionRootKeepsAllWiringPrivate() {
        for (var field : RoomSessionContext.class.getDeclaredFields())
            assertTrue(Modifier.isPrivate(field.getModifiers()), field.toString());
        for (var constructor : RoomSessionContext.class.getDeclaredConstructors())
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }
}
