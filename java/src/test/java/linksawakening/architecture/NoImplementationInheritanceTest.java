package linksawakening.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import linksawakening.architecture.ClassFileReferences.CompiledType;
import org.junit.jupiter.api.Test;

/** Rejects superclass chains used to split one object's implementation across files. */
class NoImplementationInheritanceTest {
    // Temporary exceptions owned by the concurrent render/gpu/audio engineer.
    // Remove each entry when that engineer replaces its project-class superclass.
    // The owned entity/equipment/state/UI/app packages have no exceptions.
    private static final Set<String> TEMPORARY_EXTERNAL = Set.of(
            "linksawakening/gpu/GPU",
            "linksawakening/gpu/RomTileLoader",
            "linksawakening/audio/sfx/SfxCpu",
            "linksawakening/render/EntitySpritePainter",
            // Immutable tile subclass found by the compiled audit, also owned by gpu/.
            "linksawakening/gpu/EntitySpriteTileSnapshot$FrozenTile");

    @Test
    void mainClassesUseCompositionUnlessTheirSuperclassIsJdkOrSealed() throws Exception {
        var classes = ClassFileReferences.classes();
        Map<String, CompiledType> byName =
                classes.stream().collect(Collectors.toMap(CompiledType::name, Function.identity()));
        for (var type : classes) {
            String superclass = type.superclass();
            if (superclass == null
                    || superclass.startsWith("java/")
                    || superclass.startsWith("javax/")
                    || superclass.startsWith("org/lwjgl/")) continue;
            if (TEMPORARY_EXTERNAL.contains(type.name())) continue;
            var parent = byName.get(superclass);
            assertTrue(
                    parent != null && parent.permittedSubclasses().contains(type.name()),
                    type.name() + " extends " + superclass + "; compose responsibilities instead");
        }
    }

    @Test
    void sharedReaderReadsSuperclassAndPermittedSubclassesFromClassFiles() throws Exception {
        var byName = ClassFileReferences.classes(Path.of("build/classes/java/test/linksawakening")).stream()
                .collect(Collectors.toMap(CompiledType::name, Function.identity()));
        String prefix = "linksawakening/architecture/NoImplementationInheritanceTest$";
        assertEquals(
                prefix + "FixtureParent", byName.get(prefix + "FixtureChild").superclass());
        assertEquals(
                List.of(prefix + "FixtureChild"),
                byName.get(prefix + "FixtureParent").permittedSubclasses());
        assertEquals("java/lang/Record", byName.get(prefix + "FixtureRecord").superclass());
        assertEquals("java/lang/Enum", byName.get(prefix + "FixtureEnum").superclass());
    }

    private abstract static sealed class FixtureParent permits FixtureChild {}

    private static final class FixtureChild extends FixtureParent {}

    private record FixtureRecord(int value) {}

    private enum FixtureEnum {
        VALUE
    }
}
