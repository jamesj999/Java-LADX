#!/usr/bin/env python3
"""Hook actual ROM audio write instructions; local register evidence contains no assets."""

import argparse
import importlib.metadata
import io
import json
import re
from pathlib import Path
from pyboy import PyBoy

ROOT = Path(__file__).resolve().parents[2]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument("--output", type=Path, default=Path("/tmp/ladx-intro-audio/noise-writes.jsonl"))
p.add_argument("--rom", type=Path, default=ROOT / "java/src/main/resources/rom/azle.gbc")
p.add_argument("--frames", type=int, default=1600)
a = p.parse_args()
# Derive instruction sites from annotated disassembly; verify opcode bytes before hooking.
rom = a.rom.read_bytes()
sites = {}
for source in (ROOT / "LADX-Disassembly/src/code/audio").glob("*.asm"):
    for line in source.read_text().splitlines():
        m = re.search(r";; ([0-9A-F]{2}):([0-9A-F]{4}) ((?:\$[0-9A-F]{2} ?)+)", line)
        if not m:
            continue
        bank, address = int(m[1], 16), int(m[2], 16)
        code = bytes(int(v, 16) for v in re.findall(r"\$([0-9A-F]{2})", m[3]))
        if code[0] == 0xE2 or (code[0] == 0xE0 and 0x20 <= code[1] <= 0x26):
            offset = bank * 0x4000 + address - 0x4000 if bank else address
            if rom[offset : offset + len(code)] != code:
                raise RuntimeError(f"ROM instruction mismatch {bank:02x}:{address:04x}")
            sites[bank, address] = code[1] if code[0] == 0xE0 else None
player = PyBoy(
    str(a.rom),
    window="null",
    sound_emulated=True,
    sound_sample_rate=48000,
    cgb=True,
    ram_file=io.BytesIO(bytes(0x8000)),
)
player.set_emulation_speed(0)
frame = 0
origin = None
records = []
audio_tick = 0


def tick(context):
    global audio_tick
    audio_tick += 1
    records.append(
        dict(
            kind="noise_tick",
            frame=frame,
            cycles=player._cycles(),
            audio_tick=audio_tick,
            rom_frame_counter=player.memory[0xFFE7],
        )
    )


player.hook_register(31, 0x64EC, tick, None)


def write(context):
    bank, address, fixed = context
    r = player.register_file
    reg = r.C if fixed is None else fixed
    if 0x20 <= reg <= 0x26:
        records.append(
            dict(
                frame=frame,
                cycles=player._cycles(),
                bank=bank,
                pc=address,
                register=reg,
                value=r.A,
                audio_tick=audio_tick,
            )
        )


for (bank, address), fixed in sites.items():
    player.hook_register(bank, address, write, (bank, address, fixed))
try:
    while origin is None or frame < origin + a.frames:
        if origin is None and frame >= 1000:
            raise RuntimeError("No intro origin")
        player.tick(1, render=False, sound=True)
        if origin is None and player.memory[0xDB96] == 3:
            origin = frame
        frame += 1
finally:
    player.stop(save=False)
for r in records:
    r["intro_frame"] = r["frame"] - origin
(a.output).write_text("".join(json.dumps(r) + "\n" for r in records))
print(
    json.dumps(
        dict(
            emulator=importlib.metadata.version("pyboy"),
            origin=origin,
            sites=len(sites),
            writes=len(records),
            output=str(a.output),
        )
    )
)
engine_path = a.output.parent / "engine.jsonl"
if engine_path.exists():
    engine = [json.loads(line) for line in engine_path.read_text().splitlines()]
    ew = [
        r for r in engine if r.get("register") in (0x21, 0x22, 0x23) and r["frame"] < a.frames - 1
    ]
    mw = [r for r in records if r.get("register") in (0x21, 0x22, 0x23) and r["intro_frame"] >= 128]
    # Bound both sides to completed corresponding writes, avoiding end-window truncation.
    if len(ew) != len(mw):
        raise RuntimeError("Write count differs; choose a bound between effects")
    matched = sum(
        (e["register"], e["value"]) == (m["register"], m["value"]) for e, m in zip(ew, mw)
    )
    starts = {r["frame"] for r in engine if "noise" in r}
    anchor_e = anchor_m = None
    deltas = []
    outliers = []
    for e, m in zip(ew, mw):
        if e["frame"] in starts and e["register"] == 0x21:
            anchor_e = e["frame"]
            anchor_m = m["audio_tick"]
        deltas.append((m["audio_tick"] - anchor_m) - (e["frame"] - anchor_e))
        if m["intro_frame"] - e["frame"] != 1:
            outliers.append(dict(engine=e, emulator=m))
    summary = dict(
        frames=a.frames,
        origin=origin,
        registers=["NR42", "NR43", "NR44"],
        compared=len(ew),
        values_and_order_matched=matched,
        relative_driver_tick_delta_min=min(deltas),
        relative_driver_tick_delta_max=max(deltas),
        frame_label_outliers=outliers,
        limits=[
            "Cycle values are diagnostic PyBoy CPU cycles, not audio sample timestamps.",
            "Hooks cover verified E0/E2 instructions in annotated audio source; this is not a general memory bus tracer.",
            "Per-effect driver tick comparison starts at the paired initial NR42 write.",
            "No sample-phase, envelope-clock-phase or analog mixer equivalence is established.",
        ],
    )
    a.output.with_suffix(".summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps(summary, indent=2))
