#!/usr/bin/env python3
"""Report unmasked checkpoints from the shared Main/emulator opening route."""

import argparse
import base64
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import ROOT, add_output_argument, rgb555, pixel_mismatches  # noqa: E402

parser = argparse.ArgumentParser()
add_output_argument(parser, ROOT / "java/build/opening-route")
OUT = parser.parse_args().output
original_run = json.loads(OUT.joinpath("emulator.json").read_text())
java_run = json.loads(OUT.joinpath("java.json").read_text())
if original_run["route_sha256"] != java_run["route_sha256"]:
    raise ValueError("Captures use different route revisions; rerun both captures")
rows = []
checkpoint_count = len(OUT.joinpath("emulator-checkpoints.tsv").read_text().splitlines())
if checkpoint_count != java_run["checkpoints"]:
    raise ValueError("Capture checkpoint counts differ")
for index in range(checkpoint_count):
    source = OUT / "original" / f"{index:02}.png"
    original = rgb555(source)
    ours = rgb555(OUT / "ours" / source.name)
    rows.append(
        dict(checkpoint=int(source.stem), pixels=int(pixel_mismatches(original, ours).sum()))
    )
result = dict(
    checkpoints=len(rows),
    exact=sum(r["pixels"] == 0 for r in rows),
    worst_pixels=max(r["pixels"] for r in rows),
    differences=rows,
)
OUT.joinpath("results.json").write_text(json.dumps(result, indent=2))
html = [
    """<!doctype html><html><meta charset="utf-8"><style>
article{display:flex;gap:16px;flex-wrap:wrap}figure{margin:0}img{width:240px;image-rendering:pixelated}section{margin:20px 0}p{line-height:1.5}
</style>""",
    f"<p><strong>Continuous opening route: {result['exact']}/{len(rows)} checkpoints match.</strong> "
    f"Worst difference: {result['worst_pixels']}/23040 pixels at RGB555 precision.</p>",
    "<p>Both games wake Link, finish Marin’s conversation, hop out of bed, walk to Tarin, "
    "receive the shield, and exit into room A2. Extended routes use coordinate and room "
    "goals, releasing held input before waiting for transitions to finish. Java runs "
    "the desktop Main input, update and renderer; no scene or state injections follow bootstrap. "
    "The original is reached by normal file-menu input with fresh in-memory RAM.</p>",
    "<p>This is a diagnostic baseline, not a pixel-parity pass. Java starts at its new-game "
    "bootstrap; the emulator starts at the settled house after file-menu entry. Initial clocks "
    "are not synchronized. Faster loading is intentional; loading duration is not a parity "
    "requirement. Sprite, collision, input and display differences remain to investigate. "
    "No masks or screenshot alignment are applied.</p>",
]
samples = [(0, "Marin opening"), (16, "Shield received"), (21, "House exit")]
if checkpoint_count >= 69:
    samples = [
        (16, "Shield received"),
        (53, "Owl conversation"),
        (65, "Sword pickup"),
        (68, "Sword equipped"),
    ]
elif checkpoint_count > 22:
    samples.append((checkpoint_count - 1, "Beach approach"))
if checkpoint_count >= 100:
    samples += [
        (81, "Return stairway"),
        (85, "Bush cutting"),
        (93, "Village return"),
        (99, "Forest owl"),
    ]
if checkpoint_count >= 111:
    samples = [
        (16, "Shield received"),
        (99, "Forest owl"),
        (108, "Cave approach"),
        (110, "Mushroom cave entrance"),
    ]
if checkpoint_count >= 129:
    samples = [
        (110, "Cave entrance"),
        (117, "Rock puzzle"),
        (124, "Doorway approach"),
        (125, "Cave exit"),
        (127, "Mushroom pickup"),
        (128, "Mushroom collected"),
    ]
if checkpoint_count >= 182:
    samples = [
        (128, "Mushroom collected"),
        (173, "Witch house"),
        (174, "Inventory"),
        (178, "Brewing dialogue"),
        (179, "Powder ready"),
        (180, "Powder received"),
        (181, "Exchange complete"),
    ]
    html.append(
        "<p>The forest return has explicit ROM combat-button overrides at "
        + str(original_run.get("rom_button_overrides", 0))
        + " segments because enemy phases differ on arrival. Overrides can include "
        "extra combat in either runtime. Both runs remain alive throughout; this does not prove combat parity.</p>"
    )
if checkpoint_count >= 232:
    samples = [
        (181, "Powder collected"),
        (185, "Brazier tutorial"),
        (186, "Tutorial complete"),
        (229, "Raccoon approach"),
        (230, "Transformation dialogue"),
        (231, "Forest cleared"),
    ]
if checkpoint_count >= 243:
    samples = [
        (234, "Tail Key clearing"),
        (238, "Chest interaction boundary"),
        (239, "Tail Key received"),
        (241, "Owl directions"),
        (242, "Opened chest persists"),
    ]
if checkpoint_count >= 289:
    samples = [
        (281, "Tail Cave approach"),
        (283, "Gate opened"),
        (287, "Dungeon title"),
        (288, "Tail Cave entry settled"),
    ]
if checkpoint_count >= 290:
    samples = [
        (281, "Tail Cave approach"),
        (283, "Gate opened"),
        (287, "Dungeon title"),
        (289, "Entry at matching background-animation clock"),
    ]
for i, label in samples:
    html.append(f"<section><p>{label}: {rows[i]['pixels']} differing pixels</p><article>")
    for folder, title in [("original", "Original ROM"), ("ours", "Java Main loop")]:
        encoded = base64.b64encode(OUT.joinpath(folder, f"{i:02}.png").read_bytes()).decode()
        html.append(
            f'<figure><figcaption>{title}</figcaption><img src="data:image/png;base64,{encoded}"></figure>'
        )
    html.append("</article></section>")
html.append(
    "<p>Reproduce with tools/opening/capture.py, Gradle captureOpeningRoute and "
    "tools/opening/compare.py. Select the same route/output for both runtimes; "
    "see docs/reconstruction-roadmap.md for the exact beach commands. "
    "Python dependencies: tools/intro/requirements.txt.</p></html>"
)
OUT.joinpath("report.html").write_text("\n".join(html))
print({k: v for k, v in result.items() if k != "differences"})
raise SystemExit(result["exact"] != len(rows))
