# Link tunic palette implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render Link's green, red, and blue tunics using the exact ROM object palettes selected by `PlayerState.tunicType()`.

**Architecture:** `LinkTunicPalette` wraps the existing ROM object-palette decoder and maps the three source tunic values to palette rows 0, 2, and 3. `Link` receives that immutable source at construction and chooses the current row during rendering; old constructors retain a green fixture fallback.

**Tech Stack:** Java 21, JUnit 5, shipped `azle.gbc` ROM, existing framebuffer/tile renderer.

---

### Task 1: Add the ROM palette value object

**Files:**
- Create: `java/src/main/java/linksawakening/entity/LinkTunicPalette.java`
- Test: `java/src/test/java/linksawakening/entity/LinkTunicPaletteTest.java`

- [ ] **Step 1: Write the failing tests**

  Test `loadFromRomReadsTheThreeSourceTunicRows` with a synthetic ROM large
  enough for bank `$21`, write distinct RGB555 values to rows 0, 2, and 3 at
  `$5518`, and assert that green/red/blue return those decoded four-color rows.
  Test `returnedPaletteIsDefensive` by mutating a returned row and asserting a
  second lookup still returns the ROM value.

- [ ] **Step 2: Run the focused tests to verify the red phase**

  Run:

  ```bash
  gradle test --tests linksawakening.entity.LinkTunicPaletteTest
  ```

  Expected: compilation failure because `LinkTunicPalette` does not yet
  exist.

- [ ] **Step 3: Implement the minimal palette wrapper**

  Use `new EntitySpriteCatalog(romData).loadObjectPalettes()` as the only ROM
  decoding path. Store all six rows defensively, expose `forTunic(int)` with
  the exact mapping `{0 -> 0, 1 -> 2, 2 -> 3}`, validate tunic bytes, and add
  a green-only compatibility factory for old no-ROM Link fixtures.

- [ ] **Step 4: Run the focused tests**

  Run the same focused Gradle command. Expected: PASS with zero failures.

- [ ] **Step 5: Commit the value object**

  ```bash
  git add java/src/main/java/linksawakening/entity/LinkTunicPalette.java \
    java/src/test/java/linksawakening/entity/LinkTunicPaletteTest.java
  git commit -m "feat: load Link tunic palettes from ROM"
  ```

### Task 2: Wire the selected palette into Link rendering

**Files:**
- Modify: `java/src/main/java/linksawakening/entity/Link.java`
- Modify: `java/src/main/java/linksawakening/Main.java`
- Test: `java/src/test/java/linksawakening/entity/LinkTest.java`

- [ ] **Step 1: Write the failing framebuffer regression**

  Load the shipped ROM's `LinkSpriteSheet` and `LinkTunicPalette`, render the
  same Link frame with green and red `PlayerState`, and assert that a pixel
  whose decoded tile index is 2 has the green row's color in the first buffer
  and the red row's color in the second buffer at the same coordinate.

- [ ] **Step 2: Run the focused test to verify the red phase**

  ```bash
  gradle test --tests linksawakening.entity.LinkTest
  ```

  Expected: compilation failure for the new constructor or assertion failure
  because Link still always draws `SPRITE_PALETTE`.

- [ ] **Step 3: Implement the smallest integration**

  Add a `LinkTunicPalette` field and constructor overload. Have existing
  constructors delegate to the green compatibility source, replace the static
  palette lookup in `drawTile` with the selected row, and pass
  `LinkTunicPalette.loadFromRom(romData)` from `Main.initMenuSystem`.

- [ ] **Step 4: Run focused and complete tests**

  ```bash
  gradle test --tests linksawakening.entity.LinkTunicPaletteTest \
    --tests linksawakening.entity.LinkTest
  gradle clean test
  gradle test
  ```

  Expected: all commands exit 0 with zero failed tests.

- [ ] **Step 5: Update the roadmap and commit the integration**

  Add a dated `Verified ROM Link tunic palette` entry to
  `docs/reconstruction-roadmap.md`, run `git diff --check`, then commit:

  ```bash
  git add docs/reconstruction-roadmap.md \
    java/src/main/java/linksawakening/entity/Link.java \
    java/src/main/java/linksawakening/Main.java \
    java/src/test/java/linksawakening/entity/LinkTest.java
  git commit -m "feat: render Link tunics with ROM palettes"
  ```

## Plan self-review

- Every design goal has a task: source loading (Task 1), live selection and
  Main wiring (Task 2), and framebuffer/complete-suite verification (Tasks 1-2).
- No runtime colors are newly hardcoded; the only fallback is isolated
  compatibility construction for existing no-ROM tests.
- The mapping and constructor names are consistent across both tasks.
