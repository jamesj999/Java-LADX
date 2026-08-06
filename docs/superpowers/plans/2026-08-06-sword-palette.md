# ROM-backed sword palettes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the live sword renderer's hardcoded normal and charged colors with ROM `ObjectPalettes` rows 3 and 4 while preserving its existing geometry and animation.

**Architecture:** Add `SwordPalette` in the equipment package as an immutable two-row view over the existing `EntitySpriteCatalog` ROM decoder. `Sword` owns a palette and selects its normal or charged row at the existing render decision point; old constructors receive an explicit compatibility palette. `Main` supplies `SwordPalette.loadFromRom(romData)` to the live sword instance.

**Tech Stack:** Java, Gradle, JUnit 5, shipped `azle.gbc` ROM, existing RGB555 decoder and framebuffer renderer.

---

### Task 1: Add the failing ROM palette contract tests

**Files:**
- Create: `java/src/test/java/linksawakening/equipment/SwordPaletteTest.java`
- Modify: `java/src/test/java/linksawakening/equipment/SwordTest.java`

- [x] **Step 1: Write the synthetic-ROM palette tests**

Create `SwordPaletteTest` in package `linksawakening.equipment`. Build a ROM-sized byte array with `RomBank.romOffset(0x22, 0x4000)`, write distinct RGB555 rows at `RomBank.romOffset(0x21, 0x5518) + palette * 8`, and assert that `SwordPalette.loadFromRom` returns `RomBank.decodeRgb555` values for rows 3 and 4 through `normal()` and `charged()`. Mutate returned arrays and assert a subsequent accessor call is unchanged.

Use this exact palette-row writer shape:

```java
private static void writePaletteRow(byte[] rom, int palette, int... colors) {
    int offset = RomBank.romOffset(0x21, 0x5518) + palette * 8;
    for (int color : colors) {
        rom[offset++] = (byte) color;
        rom[offset++] = (byte) (color >>> 8);
    }
}
```

- [x] **Step 2: Add the shipped-ROM framebuffer palette test**

In `SwordTest`, add a test that loads `SwordPalette` and `SwordSpriteSheet` from the shipped ROM, constructs `Sword` with the new palette-aware constructor, advances to `STATE_HOLDING`, renders once at global frame phase `0` and once at phase `4`, and asserts:

```java
assertTrue(containsOpaqueColor(normalBuffer, palette.normal()[2]));
assertTrue(containsOpaqueColor(chargedBuffer, palette.charged()[2]));
assertEquals(Arrays.toString(alphaMask(normalBuffer)),
    Arrays.toString(alphaMask(chargedBuffer)));
```

The helper must only count pixels whose alpha byte is `0xFF`, and must scan the existing `Framebuffer.WIDTH * Framebuffer.HEIGHT * 4` buffer. This proves the palette changes while tile positions and transparency remain stable.

- [x] **Step 3: Run the focused tests and verify the expected red failure**

Run from `java/`:

```bash
gradle test --tests linksawakening.equipment.SwordPaletteTest \
  --tests linksawakening.equipment.SwordTest
```

Expected result at the red checkpoint: test compilation failed because
`SwordPalette` and the palette-aware `Sword` constructor did not exist yet.

### Task 2: Implement immutable ROM-backed sword palette selection

**Files:**
- Create: `java/src/main/java/linksawakening/equipment/SwordPalette.java`
- Modify: `java/src/main/java/linksawakening/equipment/Sword.java`

- [x] **Step 1: Implement `SwordPalette`**

Implement these public/package-visible behaviors:

```java
public final class SwordPalette {
    public static SwordPalette loadFromRom(byte[] romData);
    public int[] normal();
    public int[] charged();
    static SwordPalette compatibility();
}
```

`loadFromRom` must call `new EntitySpriteCatalog(romData).loadObjectPalettes()`, select source rows 3 and 4, validate four colors per row, and clone all stored/accessed arrays. `compatibility()` may contain the pre-existing fixture-only colors, but it must be the only compatibility source and must not be used by the live `Main` path.

- [x] **Step 2: Inject the palette into `Sword`**

Add a `SwordPalette` field and a constructor with this signature:

```java
public Sword(RomTables romTables, SwordSpriteSheet spriteSheet,
             GameplaySoundSink soundSink, IntSupplier randomByteSupplier,
             SwordPalette swordPalette)
```

Make the existing two-argument and four-argument constructors delegate to this constructor with `SwordPalette.compatibility()`. Reject a null palette with the existing constructor validation style. Replace the two hardcoded palette constants and change the render selection to:

```java
int[] palette = chargedFlashActive()
    ? swordPalette.charged()
    : swordPalette.normal();
```

Do not change tile lookup, coordinates, flips, transparency, or buffer writes.

The charged phase must use the global ROM frame counter: add the frame-aware
`EquippedItem.tick(boolean, int)` default, forward it through `ItemRegistry` and
`EquipmentController`, and make `Sword` use `(frameCounter & 0x04) != 0` when
fully charged. Keep the old one-argument tick overload for isolated fixtures.

- [x] **Step 3: Run the focused tests and verify they pass**

Run the same focused Gradle command from Task 1. Expected result: all
`SwordPaletteTest`, `SwordTest`, and frame-aware equipment tests pass.

### Task 3: Wire the live startup path to ROM data

**Files:**
- Modify: `java/src/main/java/linksawakening/Main.java`

- [x] **Step 1: Load the sword palette beside its ROM sprite sheet**

In `initMenuSystem`, after loading `SwordSpriteSheet`, construct:

```java
SwordPalette swordPalette = SwordPalette.loadFromRom(romData);
```

Pass it as the final argument to the five-argument palette-aware `Sword` constructor while retaining the existing sound sink and random-byte supplier.

- [x] **Step 2: Verify the live wiring and full suite**

Run:

```bash
gradle clean test
```

Expected result: `BUILD SUCCESSFUL`; the live construction has no call to `SwordPalette.compatibility()`.

### Task 4: Review, document, and commit the implementation

**Files:**
- Verify: `docs/superpowers/specs/2026-08-06-sword-palette-design.md`
- Verify: `docs/superpowers/plans/2026-08-06-sword-palette.md`
- Modify: `docs/reconstruction-roadmap.md`

- [x] **Step 1: Record the verified slice in the roadmap**

Add a dated section documenting the bank/address, row mapping, live `Main` wiring, framebuffer invariants, and the remaining GBC palette-upload/Color Dungeon non-goals.

- [x] **Step 2: Run final hygiene checks**

Run from the worktree root:

```bash
git diff --check
gradle test
git status --short --branch
```

Expected result: no whitespace errors, `BUILD SUCCESSFUL`, and only intentional committed changes.

- [x] **Step 3: Commit the implementation**

```bash
git add java/src/main/java/linksawakening/equipment/SwordPalette.java \
  java/src/main/java/linksawakening/equipment/Sword.java \
  java/src/main/java/linksawakening/Main.java \
  java/src/test/java/linksawakening/equipment/SwordPaletteTest.java \
  java/src/test/java/linksawakening/equipment/SwordTest.java \
  docs/reconstruction-roadmap.md
git commit -m "feat: render sword with ROM palettes"
```
