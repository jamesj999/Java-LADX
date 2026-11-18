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

/** Compiled entity ownership and the closed tick-plus-roles runtime boundary. */
class EntityRuntimeShapeTest {
    private static final String ENTITY = "linksawakening/world/entity/";
    private static final String RUNTIME = ENTITY + "RoomEntityRuntime";
    private static final String ASSEMBLY = ENTITY + "RoomEntityRuntimeBuilder";

    // Migration complete: no forwarding operation may return to the runtime facade.
    private static final Set<String> SEAM_BRIDGES = Set.of();

    private static final Set<String> ROLE_ACCESSORS = Set.of(
            "combat",
            "pickups",
            "spawns",
            "enemySpawns",
            "lifecycle",
            "outbox",
            "commands",
            "background",
            "inspection",
            "configuration",
            "debug",
            "enemies");

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
        var observedRoles = new HashSet<String>();
        var violations = new ArrayList<String>();
        for (Method method : RoomEntityRuntime.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) continue;
            String signature = signature(method);
            if (SEAM_BRIDGES.contains(signature)) {
                observedBridges.add(signature);
                continue;
            }
            if (signature.equals("tick(EntityFrameContext)")) continue;
            boolean role = ROLE_ACCESSORS.contains(method.getName())
                    && !Modifier.isStatic(method.getModifiers())
                    && method.getParameterCount() == 0
                    && method.getReturnType().getName().startsWith("linksawakening.world.entity.");
            if (!role) violations.add(signature);
            else observedRoles.add(method.getName());
        }
        assertTrue(violations.isEmpty(), String.join("\n", violations));
        assertEquals(ROLE_ACCESSORS, observedRoles, "Every intended behavior owner has an accessor");
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
