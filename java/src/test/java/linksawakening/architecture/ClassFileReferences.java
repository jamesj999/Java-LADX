package linksawakening.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Reads symbolic references, descriptors and signatures; never executes a disassembler. */
final class ClassFileReferences {
    private ClassFileReferences() {}

    private static final Path ROOT = Path.of("build/classes/java/main/linksawakening");
    private static final Pattern TYPE = Pattern.compile("(?:linksawakening|org/lwjgl)/[A-Za-z0-9_$/]+");

    record FieldReference(String owner, String name) {}

    record MethodReference(String owner, String name) {}

    record CompiledType(
            String name,
            List<String> references,
            List<FieldReference> fields,
            List<MethodReference> methods,
            String superclass,
            List<String> permittedSubclasses) {}

    static List<CompiledType> classes() throws Exception {
        return classes(ROOT);
    }

    static List<CompiledType> classes(Path root) throws Exception {
        List<CompiledType> result = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                List<String> references = new ArrayList<>();
                List<FieldReference> fields = new ArrayList<>();
                List<MethodReference> methods = new ArrayList<>();
                String superclass;
                List<String> permittedSubclasses = new ArrayList<>();
                try (var input = new DataInputStream(Files.newInputStream(path))) {
                    if (input.readInt() != 0xCAFEBABE) throw new IllegalArgumentException(path.toString());
                    input.readUnsignedShort();
                    input.readUnsignedShort();
                    int count = input.readUnsignedShort();
                    String[] utf8 = new String[count];
                    int[] classNames = new int[count];
                    int[][] nameAndTypes = new int[count][];
                    List<int[]> fieldRefs = new ArrayList<>();
                    List<int[]> methodRefs = new ArrayList<>();
                    for (int i = 1; i < count; i++) {
                        switch (input.readUnsignedByte()) {
                            case 1 -> {
                                utf8[i] = input.readUTF();
                                var matcher = TYPE.matcher(utf8[i]);
                                while (matcher.find()) references.add(matcher.group());
                            }
                            case 3, 4 -> input.skipNBytes(4);
                            case 5, 6 -> {
                                input.skipNBytes(8);
                                i++;
                            }
                            case 7 -> classNames[i] = input.readUnsignedShort();
                            case 9 -> fieldRefs.add(new int[] {input.readUnsignedShort(), input.readUnsignedShort()});
                            case 12 -> nameAndTypes[i] =
                                    new int[] {input.readUnsignedShort(), input.readUnsignedShort()};
                            case 8, 16, 19, 20 -> input.skipNBytes(2);
                            case 10, 11 -> methodRefs.add(
                                    new int[] {input.readUnsignedShort(), input.readUnsignedShort()});
                            case 17, 18 -> input.skipNBytes(4);
                            case 15 -> input.skipNBytes(3);
                            default -> throw new IllegalArgumentException("Unknown constant-pool tag: " + path);
                        }
                    }
                    for (int[] method : methodRefs) {
                        methods.add(new MethodReference(utf8[classNames[method[0]]], utf8[nameAndTypes[method[1]][0]]));
                    }
                    for (int[] field : fieldRefs) {
                        fields.add(new FieldReference(utf8[classNames[field[0]]], utf8[nameAndTypes[field[1]][0]]));
                    }
                    input.readUnsignedShort(); // access flags
                    input.readUnsignedShort(); // this_class
                    int superIndex = input.readUnsignedShort();
                    superclass = superIndex == 0 ? null : utf8[classNames[superIndex]];
                    input.skipNBytes(2L * input.readUnsignedShort()); // interfaces
                    skipMembers(input); // fields
                    skipMembers(input); // methods
                    int attributes = input.readUnsignedShort();
                    for (int attribute = 0; attribute < attributes; attribute++) {
                        String name = utf8[input.readUnsignedShort()];
                        long length = Integer.toUnsignedLong(input.readInt());
                        if (name.equals("PermittedSubclasses")) {
                            int permitted = input.readUnsignedShort();
                            for (int i = 0; i < permitted; i++) {
                                permittedSubclasses.add(utf8[classNames[input.readUnsignedShort()]]);
                            }
                        } else {
                            input.skipNBytes(length);
                        }
                    }
                }
                result.add(new CompiledType(
                        "linksawakening/"
                                + root.relativize(path)
                                        .toString()
                                        .replace('\\', '/')
                                        .replace(".class", ""),
                        references,
                        fields,
                        methods,
                        superclass,
                        List.copyOf(permittedSubclasses)));
            }
        }
        assertTrue(!result.isEmpty(), "No compiled world classes found");
        return result;
    }

    private static void skipMembers(DataInputStream input) throws java.io.IOException {
        int count = input.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            input.skipNBytes(6); // access, name, descriptor
            int attributes = input.readUnsignedShort();
            for (int attribute = 0; attribute < attributes; attribute++) {
                input.readUnsignedShort();
                input.skipNBytes(Integer.toUnsignedLong(input.readInt()));
            }
        }
    }
}
