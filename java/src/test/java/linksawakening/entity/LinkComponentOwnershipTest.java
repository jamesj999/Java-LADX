package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class LinkComponentOwnershipTest {
    @Test
    void behaviorsNeverRetainTheLinkFacade() {
        for (Class<?> component : List.of(
                LinkAnimator.class,
                LinkMotion.class,
                LinkAirborne.class,
                LinkSwimming.class,
                LinkDamage.class,
                LinkStateMachine.class,
                LinkSideView.class,
                LinkEquipment.class,
                LinkEntityEffects.class)) {
            for (var field : component.getDeclaredFields()) assertNotEquals(Link.class, field.getType());
            for (var constructor : component.getDeclaredConstructors()) {
                for (Class<?> type : constructor.getParameterTypes()) assertNotEquals(Link.class, type);
            }
        }
    }

    @Test
    void assemblyBindsEachDependencyToTheOwnedComponent() {
        Link link = new LinkTestBuilder()
                .input(null)
                .config(null)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(null)
                .items(null)
                .build();
        assertSame(link.linkMotion, link.assembly.linkMotion.get());
        assertSame(link.linkAnimationState, link.assembly.linkAnimationState.get());
        assertSame(link.linkItemState, link.assembly.linkItemState.get());
        assertThrows(IllegalStateException.class, () -> link.assembly.linkMotion.bind(link.linkMotion));
    }
}
