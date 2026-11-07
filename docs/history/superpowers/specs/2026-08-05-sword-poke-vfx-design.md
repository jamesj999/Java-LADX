# ROM Sword-Poke VFX Design

## Context

The Java combat runtime already publishes raw enemy collision sound writes,
but it does not yet present the short sword-poke effect used when Link's sword
clinks from an entity whose `ENTITY_OPT1_SWORD_CLINK_OFF` option is set. The
effect is a visible combat response distinct from ordinary enemy damage: it
does not reduce enemy health or configure normal recoil.

The authoritative path is in `LADX-Disassembly/src/code/bank0.asm` and
`LADX-Disassembly/src/code/entities/bank3.asm`:

- `label_D07` converts `wC140/wC142` to effect coordinates by subtracting
  `$08`, writes jingle `$07`, and calls `AddTranscientVfx` with type `$05`.
- `EnemyCollidedWithSword` reaches that label only through the
  `ENTITY_OPT1_SWORD_CLINK_OFF` branch. That branch sets the entity's ignore
  hits countdown to `$10`, clears both recoil velocities, then proceeds to
  Link's collision handler. It does not apply normal enemy sword damage.
- `RenderTranscientSwordPoke` uses the decremented transient countdown's bit
  `$08` to select two OAM entries from `Data_002_57DD`. The two phases are
  tile `$3C` or `$3A`, each with attributes `$00` and `$20`, at raw offsets
  `(0, -1)` and `(0, +7)`.

The currently modeled combat entity whose source options table has this path
is `ENTITY_SPIKE_TRAP` (`$27`). Other source entities also carry the option,
but their dedicated handlers and collision behavior are not yet represented
by this Java runtime. The first increment therefore targets the spike-trap
branch only; it must not pretend that all sword hits are clinks.

## Design

### ROM VFX data and rendering

Add `SWORD_POKE` as transient VFX type `$05` with the source countdown `$0F`.
Extend the existing cut-leaves effect renderer with a two-placement sword-poke
method that decodes the exact phase data from `Data_002_57DD`. Preserve the
host's established OAM-origin conversion: transient world coordinates are
the source position after subtracting `$08`, and OAM Y values subtract the
Game Boy `$10` display bias. The render layer dispatches the new type without
changing bush leaves or poof behavior.

### Combat event boundary

Extend `EntityCombatEvent` with an optional `SwordPokeVfx(worldX, worldY)`
request. The event remains the boundary between ROM-shaped combat state and
presentation/audio consumers, so `RoomEntityRuntime` can publish the exact
source coordinates without coupling to the VFX slot system.

`EnemyCombatEventConsumer` keeps its existing two-argument API and adds an
overload accepting `TransientVfxSystem`. It maps jingle `$07` to a dedicated
`SWORD_POKE` gameplay sound event and spawns the event's optional VFX request.
Unknown raw writes remain ignored.

### Spike-trap collision behavior

Add a source-backed combat-rule predicate for the spike trap's clink-off
option. When a sword collision targets that type, `RoomEntityRuntime` emits:

- no enemy damage and no enemy special action;
- jingle channel `$07`;
- ignore-hits countdown `$10`;
- cleared recoil motion;
- a sword-poke VFX request at `(swordX - $08, swordY - $08)`, wrapped as an
  unsigned Game Boy byte.

Simultaneous Link contact damage remains governed by the existing link
collision input. All non-spike-trap sword collisions retain the current
damage, recoil, status, death, and sound behavior.

## Scope boundary

This increment does not implement every `SWORD_CLINK_OFF` entity handler, the
other transient VFX types, or the original transient OAM arbitration. It
establishes the exact sword-poke data and the one currently modeled gameplay
branch, with tests protecting the unchanged ordinary enemy path.

## Verification

- Assert the source VFX type, countdown, phase tiles, raw attributes, offsets,
  and transient countdown behavior.
- Assert event consumption routes jingle `$07` and spawns the requested VFX.
- Assert a spike-trap sword collision has no enemy damage, uses `$10` ignore
  hits, clears recoil, and publishes the source coordinates.
- Assert an ordinary enemy sword collision still uses normal damage/recoil and
  jingle `$03`/`$09` behavior.
- Run focused tests, `git diff --check`, and a fresh `gradle clean test`.
