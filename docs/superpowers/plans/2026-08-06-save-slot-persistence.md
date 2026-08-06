# ROM-Shaped Save-Slot Persistence Plan

**Date:** 2026-08-06  
**Spec:** `docs/superpowers/specs/2026-08-06-save-slot-persistence-design.md`

## Objective

Replace the file menu's hardcoded empty slot data and initialized-slot
exception with a small host-backed representation of the game's SRAM image.
The implementation will preserve source-defined bytes, expose only the
currently modeled fields to Java gameplay, and leave copy/erase and exact
in-game save triggers for later increments.

## Constraints and source-derived decisions

- Store the skipped `$100` SRAM bytes only as an image offset; do not model a
  fabricated CPU address space.
- Keep the three source-defined slot regions and their `$3AD` stride exactly:
  prefix `$05`, main `$380`, DX1 `$05`, DX2 `$20`, and DX3 `$03`.
- Validate the initialization prefix `[1, 3, 5, 7, 9]` before exposing a slot
  as initialized, matching `InitSaveFiles`.
- Derive the file-menu mask from the five name bytes, matching
  `func_5DC0`/`FileSelectionExecuteChoice`.
- Write a new-file slot with the ROM's name, health `$18`, max hearts `$03`,
  and zero death count before running the existing runtime new-game profile.
- Decode and apply only fields already represented by `PlayerState`; retain
  unknown main/DX bytes in the raw image for future increments.
- Use an injectable `Path` or in-memory store in tests, with a default save
  path under the user's home directory for the runtime.

## Implementation sequence

### 1. Add failing tests first

Create focused tests for the desired pure save APIs and update existing flow
tests before adding production classes. The tests will cover:

- `SaveRamLayout`: image size, skipped SRAM prefix, slot starts/stride, region
  offsets, and checked slot/field bounds.
- `SaveRamImage`: strict input length, valid-prefix initialization, defensive
  copies, name/mask discovery, new-file bytes, modeled field decoding, BCD
  rupee decoding, and preservation of unknown bytes.
- `SaveRamStore`: missing-file initialization, temporary-path round trip,
  flush, and in-memory injection.
- `PlayerState.applySavedGame`: modeled persistent fields replace the current
  state while transient runtime state is reset and unrelated behavior remains
  intact.
- `MainFileMenuFlowTest`: menu startup obtains mask/names from the store,
  `START_NEW_GAME` persists and flushes the selected slot before bootstrap,
  and `LOAD_GAME` no longer throws for an initialized slot.

Run the focused test set at this point and record the expected compile/test
failure. Do not write production implementation before the red test stage.

### 2. Implement the source-shaped raw image layer

Add `linksawakening.save.SaveRamLayout`, `SaveSlotState`, and
`SaveRamImage`.

`SaveRamLayout` will centralize all source-derived constants and checked
offset calculations. `SaveRamImage` will own a defensive byte array, create a
valid empty image, validate prefixes, derive the menu mask/names, write a
new-file slot, and decode a slot into an immutable `SaveSlotState`. Decode
packed BCD rupees using the two source bytes and clone all exposed arrays.

Run the layout/image tests, then commit this coherent raw-SRAM increment:

`feat: model ROM-shaped SRAM save slots`

### 3. Add player hydration and host persistence

Extend `PlayerState` with setters/helpers needed by the decoded save state,
including heart pieces, seashells, and a validated inventory replacement.
Implement `applySavedGame` so persistent fields are applied while transient
invincibility, powerups, boots, and buffers do not leak from the previous
session. Add a validated `Link.setDirection` for the saved-load indoor
standing-up direction.

Add `SaveRamStore` with:

- `open(Path)`, which loads an exact image or creates a valid empty image when
  the file is absent;
- explicit handling for malformed image/prefix data according to the image
  layer's source-shaped initialization behavior;
- `inMemory()` for tests;
- `flush()` that creates the parent directory and writes the exact raw image;
- delegates for mask/names, slot reads, and new-file writes.

Run save-layer and player tests, then commit:

`feat: persist and hydrate modeled save state`

### 4. Wire the file menu and saved-room entry

Add one `SaveRamStore` to `Main` and open it during menu initialization. Pass
its current mask and names into `FileMenuController` instead of literals.

On `START_NEW_GAME`, write the selected slot's ROM-shaped creation bytes and
flush before calling the existing `startNewGame` bootstrap. On `LOAD_GAME`,
decode the selected slot and:

- use the existing new-game profile when the saved spawn X is zero, as the ROM
  does immediately after file creation;
- otherwise apply the modeled save state, select the saved indoor or
  overworld room, set the saved entry position, and mirror the ROM's direction
  and indoor standing-up animation behavior.

Keep file-menu controller ownership and existing title/menu transitions
unchanged outside this boundary. Add source-level or pure flow assertions
where GLFW construction prevents direct integration testing.

Run focused menu tests and then the complete Gradle suite. Commit:

`feat: load persistent file-menu saves`

### 5. Document and verify the bounded result

Update `docs/reconstruction-roadmap.md` to record raw SRAM image persistence,
file-menu hydration, new-file writes, and initial saved-room loading as
verified. Explicitly retain the deferred items: exact in-game save triggers,
copy/erase screens, death-count updates, unmodeled WRAM/DX fields, and exact
save-menu effects/audio.

Run:

```text
gradle clean test
git diff --check
git status --short --branch
```

Inspect the final diff and confirm the root worktree's unrelated untracked
tests remain untouched.

## Verification checkpoints

At each checkpoint, use the smallest relevant Gradle test task first, then
the full suite after Main integration. A claim of completion requires the
actual command output and a clean diff check; failures should be diagnosed
before changing implementation.

## Out of scope for this slice

- Copy and erase menu actions.
- In-game save triggers and `SaveGameToFile` coverage beyond the new-file
  bytes needed by the file menu.
- Death-count mutation and all unmodeled WRAM fields.
- DX1/DX2 decoding beyond raw-byte preservation.
- Exact file-menu transition timing, audio, and effects.
- Full SRAM compatibility with arbitrary external save files beyond the
  source-defined image layout and modeled fields.
