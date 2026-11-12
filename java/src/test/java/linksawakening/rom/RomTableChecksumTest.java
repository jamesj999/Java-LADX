package linksawakening.rom;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

/** Captured from the original RomTables before splitting; signed values hash as sign-extended ints. */
@RequiresRom
class RomTableChecksumTest {
    private static final String EXPECTED =
            """
physicsFlags=3b4c5c944bfc899c0b996547699d6f1f789b5c6ef893f6ffd51d48f819065a17
entityOptions1=b4a7e90b4986ba4a8efc3e927ed22c652d2d1a842726c85cf4ace13716030935
entityHitboxFlags=1c904db891f25d2dd80a0790a46b7320f575124c7971a31ba71ec6ad4cc50cd2
entityCollisionPointsX=58e95b8db9c2877068f5dbe008dbfa206933aff96332b3ea2a1c9496965c3d8b
entityCollisionPointsY=05384ab2d8bb45b49c3a8a3842de36f7145f7c091864ecf1e64e5aee8cbcb78a
entityFineCollisionShapes=9fb9f81abed9f745afa8e05e7d8c4b135d0d8a67be58c3030edea7c8925c16a1
linkFineCollisionShapes=5cb774208633f0f5822d9e1743d2d404114617d9e2c32633fe1d671417f97333
pitWarpArrivalDirections=3c12e8b862981c129e6b76f44f0679ce26cb6afdcdd3ac7f36680dcd076ee53e
waterCurrentX=76f86ab8dd8bcf5f3d17e98bbc9398d054e91c2dd6ea3d5167eb233ce72d8fa4
waterCurrentY=b39959eecf724f3556bf02ae427942a38c89dd0269312be80a516e0df0630dc8
linkSpeedX=d193180d48f8f2d2b8edf2d847e12c06fb139abe43bd6320aeedf3ccf3f5c9ec
linkSpeedY=b7a2844efabfb51b8dbe302e5ece83bf28d4a6c7f02b6ba0d0cdb7b73ae6e20c
airSpeedX=f4a7b18b8df5004373122dc9b4fcf985a812d11bc1910989632981f705c4d652
airSpeedY=fc278b7b02103329e3380ccb24fcf5e6d128b90f9c9dbe2edbfe9d3f1c2ab411
swimmingSpeedX=832c99a6ac4f2f458a353bee211769dfd6a5cee49bdc25eb23f5c962fda7a7bd
swimmingSpeedY=b270084044b5b758ea8872d2f6116cc467b3ef52ae5a27eb6cf48be9c46dd7f1
swimmingEntrySpeedX=eb5070bf21ef25215082f4d44f1c52c750513802526e33a12e9409c92c3f42f5
swimmingEntrySpeedY=cc269f260b103e1b3975c5b94e9cacbd088ce91b7bd8c54ce6aa5b27d7764531
swordAnimState=ab1d00488de62516c498658ca4f0690dd089d16dbeb1ccca48ac12d78af16f9d
swordDirection=253084cf71fd3b2b8c466985fa95413fe8146d6fdf22311d0d3a044108e077c9
swordSpinAnimationState=f33f1258f5f5453071f852ffafc959279ad941c5d7ba86bb40249c0ca2ee719f
swordSpinAbsoluteDirection=95330626e7f9740c690b054209b826bb2afe151ef3ccc23f5aca25349fb3aa1c
swordXOffset=919c17f6d5416de1fc7d4cff5f4f14e2c5820d001ffb6a5c008bae33beb67a37
swordYOffset=6d115f5e5754a1dfdcf6799b6cfc02f02ceae15d212b5f9d8cb5226b889a90ca
swordYBase=9e189a7cc2aec83547eb1470b23b6fdd9c68a4b77bbc5aa9263d646c46376fe9
swordCollisionNeeded=272006831023e3a5a5d87341a23944e8ce50723c06ec9e6f1f5caa52d72c480b
swordCollisionWidth=0c8da8f563f21510a1fcb041d278d9f6cde20349aefc61d57f5a0e1dfd6941a9
swordCollisionOffset=09f1146d19cc835d85afe73a6fc88266123033f895ca1ade34f06bfb3c98273a
swordCollisionHeight=bbe999a355ef1a8b0bfd7e84437c9a83941a2c160d486d0a4f3a9bfa930f844f
swordSpriteTiles=de88606c56f4dcfff3ce619b2f1ad157aece977373cd081fd505316537b54f51
swordSpriteAttrs=d5750eb675cbcaaf57e6684251b3ff005ae8bdf72458935335c4fa75aae32995
magicRodXOffset=0e5ae9b4406f131a48beac1aec04d11ac819da498e7c3980b0ccd717e39356b8
magicRodYOffset=832f4d32b706d585e092461bec4d812409601844570760a940348085f2afdabc
magicRodTiles=40eb0ff2692275be5fbfcf4c6a2ffde44fa73986947dfc98ea62a897f5b0b4ac
magicRodAttributes=4e0c9a17fc31e2ff21ef25abc7344ce1213f3111a1014db88bf490c611c6b081
shovelAnimationStates=f82c3b4d1589343ca3cf52bee3f54eeb9b480f06b59a8db4774a69ff01510c44
staticSwordCollisionX=4e80ec02f75c2a83611158f2730ea6196d3fa246e7b6851409ee7f4b48e86ebc
staticSwordCollisionY=d54a4c43e2b6ed8edf71fc300222fa6eb5379c7536ab6e5235a108cb56c78223
revolvingDoorAnimation=1285a5ee8be8c6d4f5c4e2c30f08da8a2989910ee253e3d341d21a96e42b81bc
revolvingDoorPalettes=930d01137bd88b22d96200a6cf43d9b8c6b479642c014207a78344b09dd6a296
""";

    @Test
    void everyTableMatchesOriginalChecksum() throws Exception {
        try (var input = TestRom.open()) {
            Map<String, String> actual = new TreeMap<>();
            collect(RomTables.loadFromRom(input.readAllBytes()), actual);
            Map<String, String> expected = new TreeMap<>();
            for (String line : EXPECTED.strip().split("\n")) {
                String[] pair = line.split("=");
                expected.put(pair[0], pair[1]);
            }
            assertEquals(expected, actual);
        }
    }

    private static void collect(Object holder, Map<String, String> result) throws Exception {
        for (Field f : holder.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);
            Object value = f.get(holder);
            if (f.getType().isArray()) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                hash(md, value);
                assertNull(result.put(f.getName(), HexFormat.of().formatHex(md.digest())), f.getName());
            } else if (f.getType().getPackageName().equals("linksawakening.rom")) collect(value, result);
        }
    }

    private static void hash(MessageDigest md, Object array) {
        for (int i = 0; i < Array.getLength(array); i++) {
            Object value = Array.get(array, i);
            if (value.getClass().isArray()) hash(md, value);
            else {
                int n = ((Number) value).intValue();
                md.update((byte) (n >>> 24));
                md.update((byte) (n >>> 16));
                md.update((byte) (n >>> 8));
                md.update((byte) n);
            }
        }
    }
}
