package linksawakening.architecture;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Construction belongs to wiring; domain-local helpers and state values stay with their owner. */
class AppPackageTest {
    private static final String APP = "linksawakening/app/";

    @Test
    void onlyWiringConstructsCollaboratorsAcrossAppDomains() throws Exception {
        for (var type : ClassFileReferences.classes()) {
            if (!type.name().startsWith(APP) || domain(type.name()).equals("wiring")) continue;
            for (var call : type.methods()) {
                if (!call.name().equals("<init>") || !call.owner().startsWith(APP)) continue;
                String target = domain(call.owner());
                // State/service values, entry points, and replay snapshots are data/entry APIs.
                if (List.of("state", "services", "headless", "entry").contains(target)) continue;
                assertEquals(domain(type.name()), target, type.name() + " constructs " + call.owner());
            }
        }
    }

    @Test
    void gameplayScreensAndStoryDoNotDependOnWiring() throws Exception {
        for (var type : ClassFileReferences.classes()) {
            if (!type.name().startsWith(APP)
                    || !List.of("gameplay", "screen", "story").contains(domain(type.name()))) continue;
            for (var ref : type.references()) assertFalse(ref.startsWith(APP + "wiring/"), type.name() + " -> " + ref);
        }
    }

    @Test
    void onlyHostsReferenceLwjgl() throws Exception {
        for (var type : ClassFileReferences.classes()) {
            if (!type.name().startsWith(APP) || domain(type.name()).equals("host")) continue;
            for (var ref : type.references()) assertFalse(ref.startsWith("org/lwjgl/"), type.name() + " -> " + ref);
        }
    }

    @Test
    void gameplayHasNoAppDependency() throws Exception {
        for (var type : ClassFileReferences.classes()) {
            if (!type.name().startsWith("linksawakening/gameplay/")) continue;
            for (var ref : type.references()) assertFalse(ref.startsWith(APP), type.name() + " -> " + ref);
        }
    }

    @Test
    void rawRomAccessIsConfinedToLegacyWorldWiring() throws Exception {
        for (var type : ClassFileReferences.classes()) {
            if (!type.name().startsWith(APP) && !type.name().startsWith("linksawakening/gameplay/")) continue;
            for (var call : type.methods()) {
                if (!call.owner().equals("linksawakening/rom/Rom")
                        || !call.name().equals("unsafeBytes")) continue;
                assertTrue(
                        List.of(APP + "wiring/SubsystemWiring", APP + "wiring/WorldWiring")
                                .contains(type.name()),
                        type.name() + " exposes raw ROM bytes");
            }
        }
    }

    private static String domain(String name) {
        String relative = name.substring(APP.length());
        int end = relative.indexOf('/');
        return end < 0 ? "entry" : relative.substring(0, end);
    }
}
