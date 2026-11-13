# Simplification round results

Baseline: develop a932af57. Final source metrics follow steps 1–6; documentation
changes in step 7 do not alter Java metrics. World is excluded throughout.
Counts include nested packages, and main/test are separate. Each cell is
**baseline → final** (a single number means unchanged).

Lines include imports and comments. Null checks count explicit equality
comparisons outside comments/string literals, not Objects.requireNonNull calls.
FQNs count inline JDK names outside imports/comments/strings; property names such
as java.io.tmpdir are not types. ForTest counts declared methods ending ForTest,
not call sites. Small types count named classes/interfaces/enums/records,
including nested declarations, with fewer than 40 nonblank, noncomment code
lines excluding standalone braces. The initial baseline commit used lexical
hook references and small-file counts; both columns below are recomputed from
the same revisions using the refined declaration/type definitions.

These are review indicators, not targets. Explicit imports and readable wrapping
increase physical lines even when source ownership becomes simpler.

| Scope/package | Files | Lines | Null checks | Wildcards | Console | JDK FQNs | ForTest members | Small types |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| main/(root) | 1 | 12 | 0 | 0 | 0 | 0 | 0 | 1 |
| main/app | 125 → 123 | 9130 → 9171 | 94 | 17 → 0 | 4 → 1 | 7 → 0 | 0 | 101 → 100 |
| main/audio | 46 | 5243 → 5260 | 42 | 4 → 0 | 5 → 0 | 0 | 0 | 25 |
| main/config | 2 | 393 → 394 | 5 | 0 | 6 → 0 | 0 | 0 | 4 |
| main/cutscene | 20 | 2552 → 2573 | 33 | 9 → 0 | 0 | 0 | 0 | 21 |
| main/dialog | 6 | 896 | 10 | 0 | 0 | 0 | 0 | 5 |
| main/entity | 80 → 64 | 10596 → 10775 | 147 | 83 → 0 | 0 | 0 | 0 | 74 → 64 |
| main/equipment | 19 | 1821 → 1814 | 18 | 2 → 0 | 0 | 0 | 3 → 0 | 25 → 26 |
| main/gameplay | 20 | 1052 | 22 | 0 | 0 | 0 | 0 | 15 |
| main/gpu | 11 | 1183 → 1184 | 10 | 0 | 0 | 1 → 0 | 0 | 2 |
| main/input | 3 | 246 → 266 | 1 | 2 → 0 | 2 → 0 | 1 → 0 | 0 | 2 |
| main/physics | 2 | 445 | 10 | 0 | 0 | 0 | 0 | 2 |
| main/recording | 3 | 333 → 336 | 5 | 0 | 1 → 0 | 2 → 0 | 0 | 1 |
| main/render | 48 | 5140 → 5169 | 216 → 208 | 0 | 0 | 0 | 0 | 31 |
| main/rom | 15 | 1363 | 0 | 0 | 0 | 0 | 0 | 8 |
| main/save | 7 | 1253 → 1269 | 3 | 3 → 0 | 1 → 0 | 0 | 0 | 11 |
| main/scene | 4 | 186 | 0 | 0 | 0 | 0 | 0 | 3 |
| main/startup | 4 | 447 | 0 | 0 | 0 | 0 | 0 | 6 |
| main/state | 24 | 1578 → 1579 | 1 | 22 → 0 | 0 | 0 | 0 | 14 |
| main/ui | 21 | 2925 | 32 | 0 | 0 | 0 | 0 | 13 |
| main/vfx | 4 | 531 → 532 | 7 | 1 → 0 | 0 | 0 | 0 | 6 |
| test/(root) | 4 | 824 | 1 | 14 → 0 | 2 | 1 → 0 | 0 | 0 |
| test/app | 42 | 3414 → 3547 | 0 | 69 → 0 | 12 | 15 → 0 | 0 | 26 |
| test/architecture | 5 → 6 | 579 → 630 | 2 | 3 → 0 | 0 | 5 → 0 | 0 | 8 → 9 |
| test/audio | 42 | 4146 → 4222 | 9 | 30 → 0 | 8 | 10 → 0 | 0 | 18 |
| test/config | 3 | 272 → 276 | 0 | 2 → 0 | 0 | 0 | 0 | 1 |
| test/cutscene | 8 | 827 → 831 | 3 | 1 → 0 | 0 | 2 → 0 | 0 | 5 |
| test/dialog | 6 | 735 → 738 | 1 | 3 → 0 | 2 | 0 | 0 | 3 |
| test/entity | 36 | 6791 → 6790 | 5 | 16 → 0 | 1 | 5 → 0 | 0 | 20 |
| test/equipment | 14 → 15 | 2048 → 2090 | 3 | 2 → 0 | 0 | 1 → 0 | 0 | 25 → 26 |
| test/gameplay | 8 | 1487 → 1489 | 6 | 1 → 0 | 0 | 2 → 0 | 0 | 8 |
| test/gpu | 13 | 703 → 707 | 4 | 1 → 0 | 1 | 0 | 0 | 11 |
| test/input | 2 | 58 → 62 | 0 | 2 → 0 | 0 | 0 | 0 | 2 |
| test/physics | 1 | 280 | 0 | 0 | 0 | 0 | 0 | 0 |
| test/recording | 2 | 214 → 220 | 0 | 2 → 0 | 0 | 1 → 0 | 0 | 1 |
| test/render | 51 | 5154 → 5239 | 25 | 36 → 0 | 4 | 6 → 0 | 0 | 24 |
| test/rom | 11 | 611 → 627 | 0 | 9 → 0 | 0 | 0 | 0 | 7 |
| test/save | 7 | 994 → 1010 | 0 | 6 → 0 | 0 | 4 → 0 | 0 | 2 |
| test/scene | 2 | 152 → 153 | 1 | 1 → 0 | 0 | 0 | 0 | 1 |
| test/startup | 5 | 337 → 338 | 0 | 1 → 0 | 1 | 0 | 0 | 2 |
| test/state | 12 | 1053 → 1061 | 0 | 9 → 0 | 0 | 1 → 0 | 0 | 9 |
| test/testing | 5 | 108 → 114 | 3 | 2 → 0 | 0 | 2 → 0 | 0 | 5 |
| test/ui | 18 | 1622 → 1655 | 0 | 11 → 0 | 1 | 3 → 0 | 0 | 9 |
| test/vfx | 5 | 425 → 427 | 4 | 1 → 0 | 0 | 0 | 0 | 3 |

## Responsibility review

Merged sixteen stateless sprite registration family files into their immutable
registration-data owner; selection order and ROM recipes are unchanged. Kept
recipe values, ROM decoding and decoded lookup separate: their data ownership
and lifetimes differ. Merged CombatEquipment and ToolEquipment into
EquipmentWiring; all three were slices of construction and adapter registration.

Kept Link position, animation, timers/motion, airborne, swimming, damage and
commands: each represents independent state or a narrow role over those states.
Kept render layers by pass (background, OAM actors, inventory, dialog, fades,
warps) because ordering, retained scratch and frame configuration differ.
Kept player inventory/vitals/ammunition/progression/wallet components because
save layout and mutations have distinct ownership. Kept ROM table decoders and
pure pixel/physics algorithms: these implement algorithms with explicit inputs,
not forwarding slices of an owner's complete state.

The two obsolete numeric shape assertions now guard the merged responsibilities;
no gameplay assertion or test case was removed. Numeric limits are explicitly
superseded by this round's brief.

## Null contracts

Render: optional inspection now uses a no-op HardwareFrameObserver internally;
legacy inspection access still reports null for no observation. Dialog submission
uses an explicit ROM painter when no override is configured. GameFrameInputs
now documents screen-specific absence centrally: a title/file frame has no live
room/Link, and only the applicable screen snapshot is present. Missing graphics
planes mean not yet loaded, not an empty world. Cached scratch is lazily allocated.

Entity: LinkServices documents reduced standalone simulation capabilities;
missing collision, ROM tables, player/inventory and graphics disable those
capabilities. These existing semantics cannot be changed to fabricated player or
ROM data. Sound and tunic palettes are validated once. Unregistered sprites,
missing event requests and unavailable ROM variants remain optional domain data.

App: service records already validate required collaborators at construction.
Opening-story controllers, saved spawn positions, optional headless PCM and
frame-input caches have lifecycle absence; nullable cache keys are not nullable
service dependencies. Recording lifecycle is unchanged.

Audio: the primitive APU tap now normalizes absence to a no-op observer. Device
and stream handles remain optional before opening and after close; error/recovery
branches preserve their lifecycle. Cutscene snapshots and wave overlays are
optional outside their scene. UI allows omitted ROM instrument/dungeon panels
in minimal menus and omitted attributes for default palette/flip selection.
Gameplay absent dialog/event/item/interaction results remain no-action values.
Equipment allows ROM-free simulation without drawing tables, and unregistered
inventory items remain absent slots. Input validation checks are retained.

## Test organization

The merged checkout has no numbered test classes or test sources over 800 lines
outside world. Kept existing behaviour-focused Link tests and shared setup
builders rather than repartitioning already cohesive tests. Added source hygiene
guards and a descriptive EquipmentInspection fixture for private timing checks;
no production timer getter is necessary. World test regrouping belongs to the
other engineers. Two new architecture tests protect explicit imports and the
absence of runtime ForTest APIs; existing tests and assertions remain intact.

## Readability review

| Reviewed file | Decision/change |
|---|---|
| EntitySpriteRegistrationData | Keep ROM recipe constants with the selector; family method names replace cross-file forwarding. Preserve priority. |
| SfxCpu | Name opcode dispatch bands; retain hex opcode labels as ISA encodings rather than inventing hundreds of constants. |
| IntroSequence | Keep ordered IntroHandler transitions and early returns; state remains in actors/water/publication owners. |
| FileMenuController | Retain FileSelectionHandler labels and separate editing state; no further file split. |
| DialogController | Retain DialogDrawNextCharacterHandler sequencing; page/layout cache does not alter input edges. |
| LinkStateMachine | Keep ApplyLinkMotionState dispatch order and ROM countdown comparisons. |
| AudioBrowserFrame | Keep Swing event wiring together; named handlers describe user actions and no game-ROM label applies to the host UI. |
| LinkAnimator | Preserve UpdateLinkAnimation label and explicit pose precedence. |
| EntitySpriteHandlerCatalog | Keep decoding dispatch separate from immutable recipes; named entity constants preserve ROM IDs. |
| Sword | Keep CheckItemsToUse/UpdateLinkAnimation provenance and attack/charge ownership. |
| EntitySpritePainter | Keep shape expansion order and OAM origin constants; no arbitrary line split. |
| LinkMotion | Retain BackgroundCollisionHandler provenance and ordered collision probes. |
| InventoryMenu | Keep transition state separate from menu rendering, with copy-on-change context ownership. |
| EquipmentWiring | Rename the opaque c parameter to inputs; clarify construction-only adapters and their dependencies. |
| LinkEntityEffects | Name the entity-driven responsibilities and OAM coordinate origins. |
| OverworldCollision | Preserve GetObjectPhysicsFlags provenance, aligned sample arithmetic and immutable retained samples. |
| IntroFramePublisher | Keep delayed VBlank publication separate from actor simulation; owned snapshot copies remain intentional. |
| GameFrameState | Retain immutable ownership boundary for captures; live rendering uses reusable inputs. |
| OpenGlFramePresenter | Name vertex attributes, stride and texture offset; preserve the exact GL argument values. |
| LinkAirborne | Preserve ApplyLinkGroundMotion provenance and integer/subpixel landing order. |

Long declarations, imports and ROM table/string literals are not grounds for
splitting a responsibility. Early exits remain where they preserve ordering;
no broad control-flow rewrite was justified in the timing-sensitive handlers.

## Other retained boundaries

Small message/value records retain protocol meaning; they are not file slices.
App service bundles validate their dependencies and retain typed subsystem
ownership. Navigation, save, input and screen flows have distinct lifecycle
responsibilities. Audio channels own synthesis state; music/SFX interpreters own
their respective clocks and registers. GPU tiles/VRAM, ROM table readers, SRAM
codecs, configuration parsing and input edge state retain their own data/format
boundaries. No further forwarding-only merge was justified in these families.

## Recording and world integration

RecordingHost construction and recorder factory setup could sit in app.wiring;
toggle, begin/capture-frame ordering and close belong to GameSession's lifecycle.
They were not moved. Recording changes are limited to imports and diagnostic
routing. No world signature changes are requested or required; all existing
world-facing APIs remain source-compatible.

## Verification

All seven steps use the required spotlessApply test command before committing.
Test count: 2,954 → 2,956; zero failures/errors and one pre-existing skipped test.
Scene parity: 986 identical, zero mismatches. No fixture edits or world edits.

EA-off warmed whole-frame measurements: idle 0, walking 596.53, inventory 0
B/frame, within existing budgets 0 / 747 / 0. Update-only: idle 0 and active 433
B/frame (budgets 0 / 542). Link walking with sword, dialog printing and HUD/
inventory submission each measure 0 B/frame. EA settings are temporary local
JVM arguments; no build settings changed. Default-JVM results are recorded in
the final commit message; JIT escape analysis can change the walking number.
