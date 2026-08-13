# Tail Cave Roc's Feather Route Design

## Goal

Continue the fresh-game ordered regression from room `$03` through side-view rooms `$19/$18` to the static Roc's Feather chest in room `$1D`.

## Source route

`MapLayout0` places side-view room `$19` immediately right of `$18`. Room `$03` warp 0 enters `$19`; crossing `$19`'s left edge performs an ordinary horizontal room transition into `$18`. Room `$18` warp 0 (`E1 00 01 48 60`) leaves the side-view area for top-down room `$01`. `MapLayout0` then places `$1C` north of `$01` and `$1D` north of `$1C`.

Room `$1D` contains closed chest object `$A0` at location `$24`. `IndoorA` chest table entry `$1D` is `CHEST_FEATHER` (`$07`), which maps to inventory item `INVENTORY_ROCS_FEATHER` (`$0A`). The room has no event gate, so the chest is statically available after reaching it.

## Boundary behavior

Both Tail Cave side-view rooms `$18/$19` use `CheckPositionForMapTransition`'s ROM entity-coordinate thresholds. In Java top-left coordinates these are:

- top: `pixelY < -4`;
- bottom: `pixelY >= 0x74`;
- left: `pixelX < -4`;
- right: `pixelX >= 0x94`.

Vertical crossings in these two ordinary rooms fade through warp 0. Horizontal crossings remain adjacent-room scrolls. The controller suppresses its older sprite-bound fallback inside the four-pixel margins so it cannot transition before the source threshold. Other category-2 rooms remain unchanged because their map/physics/entity exceptions are outside this slice.

## Verification

Focused boundary and coordinator tests prove exact horizontal margins, `$19 -> $18` scrolling, and `$18 -> $01` warp-zero fading. The fresh-game ordered regression then walks north through `$1C/$1D`, opens chest `$24` through the live chest path, applies the reward, and asserts Roc's Feather is present in inventory.

## Non-goals

This slice does not add complete side-view gravity, ladder physics, or other category-2 room exceptions. It implements only the ROM-authored Tail Cave path needed for ordered progression to the Feather.
