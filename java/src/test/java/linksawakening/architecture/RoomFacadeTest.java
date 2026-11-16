package linksawakening.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import linksawakening.world.room.EntityFrameResults;
import linksawakening.world.room.RoomDoors;
import linksawakening.world.room.RoomDungeon;
import linksawakening.world.room.RoomEntities;
import linksawakening.world.room.RoomEntityState;
import linksawakening.world.room.RoomPlayerItems;
import linksawakening.world.room.RoomProgress;
import linksawakening.world.room.RoomRooms;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomStory;
import linksawakening.world.room.RoomTerrain;
import linksawakening.world.room.RoomTransitions;
import org.junit.jupiter.api.Test;

/** Symbolic dependencies and class metadata keep role implementation independent of the lifecycle facade. */
class RoomFacadeTest {
    private static final String ROOM = "linksawakening/world/room/";
    private static final String FACADE = ROOM + "RoomSession";
    private static final String WIRING = ROOM + "RoomSessionContext";
    private static final String INVENTORY = ROOM + "RoomSessionComponents";
    private static final Set<Class<?>> ROLES = Set.of(
            RoomDoors.class,
            RoomPlayerItems.class,
            RoomTransitions.class,
            RoomStory.class,
            RoomRooms.class,
            RoomDungeon.class,
            RoomProgress.class,
            RoomTerrain.class,
            RoomEntities.class,
            RoomEntityState.class);

    @Test
    void roomImplementationsDoNotReferenceTheFacade() throws Exception {
        var types = ClassFileReferences.classes();
        for (var type : types) {
            if (!type.name().startsWith(ROOM)) continue;
            String owner = topLevel(type.name());
            if (owner.equals(FACADE) || owner.equals(WIRING)) continue;
            assertFalse(type.references().contains(FACADE), type.name() + " must receive its specific dependencies");
        }
        System.out.println("RoomSession references: "
                + types.stream()
                        .filter(type -> type.references().contains(FACADE))
                        .map(ClassFileReferences.CompiledType::name)
                        .sorted()
                        .toList());
    }

    @Test
    void constructionInventoryDoesNotEscapeWiring() throws Exception {
        for (var type : ClassFileReferences.classes()) {
            String owner = topLevel(type.name());
            if (owner.equals(WIRING) || owner.equals(INVENTORY)) continue;
            assertFalse(
                    type.references().stream()
                            .anyMatch(reference -> topLevel(reference).equals(INVENTORY)),
                    type.name() + " must receive individual collaborators rather than the wiring inventory");
        }
    }

    @Test
    void facadePublicMethodsAreOnlyTicksPresenceAndRoleOrResultAccessors() {
        for (var method : RoomSession.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) continue;
            assertFalse(Modifier.isStatic(method.getModifiers()), method.toString());
            if (method.getName().equals("tickEntities")) {
                assertEquals(1, method.getParameterCount(), method.toString());
                assertEquals(List.class, method.getReturnType(), method.toString());
            } else {
                assertEquals(0, method.getParameterCount(), method.toString());
                if (method.getName().equals("hasRoom")) assertEquals(boolean.class, method.getReturnType());
                else
                    assertTrue(
                            ROLES.contains(method.getReturnType())
                                    || method.getReturnType() == EntityFrameResults.class,
                            method.toString());
            }
        }
    }

    @Test
    void roleImplementationsNeverHoldTheFacade() throws Exception {
        for (var type : ClassFileReferences.classes()) {
            if (!type.name().startsWith(ROOM)) continue;
            Class<?> implementation = Class.forName(
                    type.name().replace('/', '.'), false, getClass().getClassLoader());
            if (ROLES.stream().noneMatch(role -> role.isAssignableFrom(implementation))) continue;
            for (var field : implementation.getDeclaredFields())
                assertFalse(
                        RoomSession.class.isAssignableFrom(field.getType()),
                        implementation.getName() + "." + field.getName());
        }
    }

    private static String topLevel(String name) {
        int nested = name.indexOf('$');
        return nested < 0 ? name : name.substring(0, nested);
    }
}
