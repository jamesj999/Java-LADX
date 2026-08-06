# Color Dungeon room render Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Route static Color Dungeon room composition through the dedicated ROM palette, object, attribute, and BG tile data selected by the disassembly.

**Architecture:** Preserve the existing `RoomLoader` → `LoadedRoom` → `ActiveRoom` → render snapshot flow. Add map `$FF` branches at the existing ROM boundaries: `RoomPaletteLoader` selects `$21:$67D0`, `RoomTilemapBuilder` selects `$08:$4760` and `$23:$6000`, and `GPU` selects the Color Dungeon fixed blocks plus the `$20:$45EA` room tile source. Ordinary indoor maps remain on their current paths.

**Tech Stack:** Java 17, JUnit 5, Gradle, ROM-backed byte-array fixtures, existing `RomBank` and `GPU` tile decoder.

---

### Task 1: Load the Color Dungeon room palette

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomPaletteLoader.java`
- Test: `java/src/test/java/linksawakening/world/RoomPaletteLoaderTest.java`

- [x] **Step 1: Write the failing test**

Add a test that writes a known RGB555 color at `$21:$67D0` and a different
color at the ordinary indoor table location, then calls
`new RoomPaletteLoader(rom).loadIndoor(0xFF, 0x00, fallback)`. Assert that
palette row 0 color 0 decodes from `$21:$67D0`, not the fallback or ordinary
table.

```java
@Test
void colorDungeonUsesItsDedicatedPaletteBlock() {
    byte[] rom = new byte[RomBank.romOffset(0x21, 0x6800)];
    int palette = RomBank.romOffset(0x21, 0x67D0);
    rom[palette] = (byte) 0x1F;
    rom[palette + 1] = 0x00;
    int[][] fallback = {{0x123456, 0, 0, 0}};

    int[][] result = new RoomPaletteLoader(rom).loadIndoor(0xFF, 0x00, fallback);

    assertEquals(RomBank.decodeRgb555(0x001F), result[0][0]);
}
```

- [x] **Step 2: Run the focused test and verify it fails**

Run:

```bash
gradle test --tests linksawakening.world.RoomPaletteLoaderTest
```

Expected: FAIL because map `$FF` currently indexes the ordinary indoor palette
map instead of `$21:$67D0`.

- [x] **Step 3: Implement the minimal ROM branch**

Add constants for `MAP_COLOR_DUNGEON = 0xFF`, palette bank `$21`, and address
`$67D0`. At the top of `loadIndoor`, return `loadPaletteBlock` for map `$FF`
before the ordinary map-table arithmetic. Keep the existing fallback behavior
for all non-Color-Dungeon maps.

- [x] **Step 4: Run the focused test and verify it passes**

Run the same Gradle command; expected result is `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit the palette boundary**

```bash
git add java/src/main/java/linksawakening/world/RoomPaletteLoader.java \
  java/src/test/java/linksawakening/world/RoomPaletteLoaderTest.java
git commit -m "feat: load Color Dungeon room palettes from ROM"
```

### Task 2: Select Color Dungeon object tiles and attributes

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomTilemapBuilder.java`
- Test: `java/src/test/java/linksawakening/world/RoomTilemapBuilderTest.java`

- [x] **Step 1: Write the failing test**

Add a synthetic-room test with one active object ID. Put distinct tile bytes at
the ordinary indoor table and at `$08:$4760 + objectId * 4`; put a distinct
attribute byte at `$23:$6000 + objectId * 4`. Build with map `$FF` and assert
the first tile and first attribute come from the Color Dungeon tables.

```java
@Test
void colorDungeonUsesDedicatedObjectTileAndAttributeTables() {
    byte[] rom = syntheticRom();
    int objectId = 0x22;
    int[] objects = new int[RoomConstants.ROOM_OBJECTS_AREA_SIZE];
    Arrays.fill(objects, 0x100);
    objects[RoomConstants.ROOM_OBJECTS_BASE] = objectId;

    int ordinary = RomBank.romOffset(0x08, 0x43B0) + objectId * 4;
    rom[ordinary] = 0x11;
    int colorTile = RomBank.romOffset(0x08, 0x4760) + objectId * 4;
    rom[colorTile] = 0x66;
    int colorAttr = RomBank.romOffset(0x23, 0x6000) + objectId * 4;
    rom[colorAttr] = 0x57;

    RoomTilemap result = new RoomTilemapBuilder(rom).buildIndoor(0xFF, 0x00, objects);

    assertEquals(0x66, result.tileIds()[0]);
    assertEquals(0x57, result.tileAttrs()[0]);
}
```

- [x] **Step 2: Run the focused test and verify it fails**

Run:

```bash
gradle test --tests linksawakening.world.RoomTilemapBuilderTest
```

Expected: FAIL with the ordinary indoor tile byte or attribute byte.

- [x] **Step 3: Implement the two map-specific bases**

Add `MAP_COLOR_DUNGEON`, `COLOR_DUNGEON_TILEMAP_BANK = 0x08`,
`COLOR_DUNGEON_TILEMAP_ADDR = 0x4760`, `COLOR_DUNGEON_ATTR_BANK = 0x23`, and
`COLOR_DUNGEON_ATTR_ADDR = 0x6000`. In `build`, select these bases when
`indoor && mapId == MAP_COLOR_DUNGEON`; otherwise preserve the existing
ordinary indoor and overworld branches. Leave the existing 2x2 write loop and
unsigned object-byte lookup unchanged.

- [x] **Step 4: Run the focused test and the existing tilemap tests**

```bash
gradle test --tests linksawakening.world.RoomTilemapBuilderTest
```

Expected: all tilemap tests pass.

- [x] **Step 5: Commit the object-table boundary**

```bash
git add java/src/main/java/linksawakening/world/RoomTilemapBuilder.java \
  java/src/test/java/linksawakening/world/RoomTilemapBuilderTest.java
git commit -m "feat: select Color Dungeon room object tables"
```

### Task 3: Load Color Dungeon BG tile blocks

**Files:**
- Modify: `java/src/main/java/linksawakening/gpu/GPU.java`
- Test: `java/src/test/java/linksawakening/gpu/GPUColorDungeonTilesTest.java`

- [x] **Step 1: Write the failing test**

Add a test that writes markers to the room-source table at `$20:$45EA`, the
source row named by that table, fixed Color Dungeon blocks `$6000` and `$6100`,
the shared dungeon source, and the wall row selected by `$20:$45C9`. Call
`loadIndoorTiles(rom, 0xFF, roomId)` and assert markers at tile slots `$100`,
`$110`, `$0F0`, and `$120`.

```java
@Test
void loadsColorDungeonRoomAndFixedBgBlocksIntoGameplaySlots() {
    byte[] rom = syntheticRom();
    int roomId = 0x02;
    int table = RomBank.romOffset(0x20, 0x45EA) + roomId * 2;
    rom[table] = 0x63;
    rom[table + 1] = 0x35;
    rom[RomBank.romOffset(0x35, 0x6300)] = 0x31;
    rom[RomBank.romOffset(0x35, 0x6000)] = 0x32;
    rom[RomBank.romOffset(0x35, 0x6100)] = 0x33;
    rom[RomBank.romOffset(0x2D, 0x4000)] = 0x34;
    rom[RomBank.romOffset(0x20, 0x45C9)] = 0x4A;
    rom[RomBank.romOffset(0x2D, 0x4A00)] = 0x35;

    GPU gpu = new GPU();
    gpu.loadIndoorTiles(rom, 0xFF, roomId);

    assertEquals(0x31, Byte.toUnsignedInt(gpu.readVRAM(0x100 * GPU.TILE_DATA_SIZE)));
    assertEquals(0x32, Byte.toUnsignedInt(gpu.readVRAM(0x110 * GPU.TILE_DATA_SIZE)));
    assertEquals(0x33, Byte.toUnsignedInt(gpu.readVRAM(0x0F0 * GPU.TILE_DATA_SIZE)));
    assertEquals(0x35, Byte.toUnsignedInt(gpu.readVRAM(0x120 * GPU.TILE_DATA_SIZE)));
}
```

- [x] **Step 2: Run the focused test and verify it fails**

Run:

```bash
gradle test --tests linksawakening.gpu.GPUColorDungeonTilesTest
```

Expected: FAIL because the current method treats `$FF` as an ordinary map
index and never loads the room-selected Color Dungeon row.

- [x] **Step 3: Implement the dedicated loader path**

Add constants for the Color Dungeon bank `$35`, fixed addresses `$6000` and
`$6100`, room source table `$20:$45EA`, and wall pointer `$20:$45C9`. At the
start of `loadIndoorTiles`, dispatch map `$FF` to a private method that:

1. reads `[sourceHighByte, sourceBank]` from `$20:$45EA + roomId * 2` and
   copies 16 tiles from `sourceHighByte << 8` to slot `$100`;
2. copies 16 tiles from `$35:$6000` to slot `$110`;
3. copies the shared 96 tiles from adjusted bank `$2D:$4000` to slot `$120`;
4. reads the wall high byte from `$20:$45C9` and copies 32 tiles from bank
   `$2D:(highByte << 8)` to slot `$120`;
5. copies 16 tiles from `$35:$6100` to slot `$0F0`.

Use existing `loadTilesFromROM` validation and `bankAddrToRomOffset`; reject
out-of-range room IDs before reading the two-byte source table entry. Do not
change the ordinary indoor path in the same edit.

- [x] **Step 4: Run the focused GPU tests**

```bash
gradle test --tests linksawakening.gpu.GPUColorDungeonTilesTest
```

Expected: all Color Dungeon GPU tests pass.

- [x] **Step 5: Commit the BG loader boundary**

```bash
git add java/src/main/java/linksawakening/gpu/GPU.java \
  java/src/test/java/linksawakening/gpu/GPUColorDungeonTilesTest.java
git commit -m "feat: load Color Dungeon BG tiles from ROM"
```

### Task 4: Integrate, document, and verify the room path

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Test: `java/src/test/java/linksawakening/world/RoomSessionTest.java`

- [x] **Step 1: Add the shipped-ROM integration assertions**

Extend the existing `loadsColorDungeonEntityRowsThroughTheSpecialRoomPath`
test after `session.loadIndoor(0xFF, 0x00)` to assert that the active room
palette row 0 color 0 equals the decoded ROM color at `$21:$67D0`, and that
the room's first tile/attribute pair matches the dedicated room object path.
Use the active room's existing immutable accessors; do not inspect GPU internals
from this integration test.

- [x] **Step 2: Run the integration test and inspect the live snapshot**

```bash
gradle test --tests linksawakening.world.RoomSessionTest.loadsColorDungeonEntityRowsThroughTheSpecialRoomPath
```

Expected: PASS, with the entity rows, palette, and tilemap all loaded from the
shipped ROM path.

- [x] **Step 3: Update the reconstruction roadmap**

Add a dated entry under the verified parity sections stating that map `$FF`
now uses the dedicated Color Dungeon object tilemap/attributes, `$67D0` room
palettes, fixed BG blocks, and `$45EA` room-selected BG rows. Record the
non-goals: switch-block state, symbols, dynamic palette effects, and event
scripts remain outstanding.

- [x] **Step 4: Run the complete verification suite**

```bash
gradle clean test
git diff --check
git status --short --branch
```

Expected: `BUILD SUCCESSFUL`, no whitespace errors, and only the intended
roadmap/test/source changes present.

- [x] **Step 5: Request a focused code review**

Ask a fresh reviewer to inspect the four commits against the spec, with special
attention to ROM bank/address math, slot `$100`/`$110` ordering, and accidental
changes to ordinary indoor rooms. Address any Critical or Important finding,
rerun `gradle clean test`, and record the review result in the handoff.

- [x] **Step 6: Commit the integration documentation**

```bash
git add docs/reconstruction-roadmap.md \
  java/src/test/java/linksawakening/world/RoomSessionTest.java
git commit -m "docs: record Color Dungeon room render parity"
```
