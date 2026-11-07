# ROM symbols

This open source engine reimplementation requires a user-supplied ROM. Never
check in ROM bytes or extracted assets.

Regenerate from a locally built RGBDS symbol file:

    python3 tools/rom/gen_symbols.py /path/to/LADX-Disassembly/azle.sym

Use `--check` to verify without writing. Normal builds need no disassembly.
`symbols.txt` is a curated allow-list of exact labels, one per line; blank lines
and full-line `#` comments are permitted. Missing and duplicate labels fail.

Use `RomSymbols.Label` with `Rom.u8`, `u16le`, or `slice`. `Rom` is a read-only
view of caller-owned bytes; callers must preserve the backing array. Slice
returns a copy. `RomAddress.offset()` uses the central bank-window calculation.
For GBC bank adjustment use an explicit `.inBank(label.bank() | 0x20)` at the
call site and document the original loader. Never silently adjust all symbols.

For world, gpu, render, and entity migration, first match the exact label and
bank against the local symbol file, retain any intentional offset, then replace
the numeric pair. Report mismatches before changing behavior. Do not substitute
an address merely because another label has a similar name.

`RomAddress.fromOffset` bridges existing file-offset cursors to typed reads;
`plus` advances in file order and `withGbcBank()` explicitly mirrors the original
GBC bank adjustment. Retain range checks at format boundaries for useful errors.

Audio clients can use `FrameAudioPlayer` with the existing `PcmSoundOutput`
interface. It advances music and SFX once per `update()` (60 Hz), renders an
integer number of samples with a carried remainder, and routes dialog PCM to
the same output. The caller owns the output lifetime. The legacy OpenAL browser
transport remains available with its existing sample-driven buffering behavior.

Rendering TODO: implement a future `RomOamAllocator` that follows the ROM's
per-frame OAM allocation, including direction/state-dependent Link and sword
ordering. Actual CGB hardware resolves OBJ overlap by lower OAM index (not X).
Until that allocator exists, production composition preserves the emulator-validated
legacy painter order: later opaque OBJ submissions win. Static slot assignments
are not a substitute for dynamic allocation. OBJ-vs-BG/window uses CGB color-zero,
BG/OBJ priority flags, and LCDC bit 0 independently of this temporary OBJ ordering.
