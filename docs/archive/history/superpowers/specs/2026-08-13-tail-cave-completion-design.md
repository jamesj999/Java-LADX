# Tail Cave Completion Design

## Goal

Continue the single fresh-game ordered regression from the opened room `$0B`
boss door through Moldorm, the Heart Container, the Full Moon Cello sequence,
and the instrument-triggered dungeon exit.

## Source route and state

`MapLayout0` places boss room `$06` immediately north of `$0B` and instrument
room `$02` immediately north of `$06`. Moldorm (`$59`) is vulnerable only at
its final delayed-history tail segment. The regression must derive that segment
from the live dynamic display list, apply sword collision there without
overlapping the head, and allow the bank-$04 destruction sequence to produce
the Heart Container (`$36`). Collection must run through the held-item countdown
and `RoomSession` reward harvesting so room status is persisted normally.

After the boss-room kill-all event opens its shutters, Link enters `$02` and
collects instrument `$F2`. The existing source-shaped performance phases must
award instrument 1, request dialog `$100`, wait for dialog/music completion,
play the cello track, emit the warp jingle, and finally publish the room's ROM
warp request. The coordinator then consumes that request to return to the
overworld.

## Scope

This slice finishes Tail Cave's ordered-play sequence. It does not simulate UI
acknowledgement internals; it drives the same dialog/music-active inputs that
the runtime receives from the application loop.
