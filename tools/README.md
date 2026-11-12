# Local research tools

Use Python **3.11+** (pins validated with CPython **3.12.15**). From the repository
root, create a virtual environment and install `tools/requirements.txt`.
`tools/intro/requirements.txt` forwards to the same pins. PyBoy stays at 2.8.1:
2.9.0 changes route input timing. NumPy 2.2.6 and Pillow 11.2.1 provide compatible
Python 3.11+ wheels. Ruff and pytest are pinned alongside runtime dependencies.

Run ROM-free checks from the root:

```sh
python -m ruff check tools
python -m ruff format --check tools
python -m pytest tools
```

Capture/comparison scripts are local research entry points. They use a
user-supplied ROM (usually `java/src/main/resources/rom/azle.gbc`; some accept
`--rom`). Java tasks also accept `LA_ROM`/`-Pla.rom`. Python scripts do not
universally honor those Java settings. Use `--help` where available before a
capture; some older probes run immediately and have no help parser. All emulator
instances are headless. Keep ROM-derived images, WAVs, binary dumps and HTML
reports local; never commit or distribute them. No capture is needed to lint
or unit-test these tools.

## Tool catalog

Paths below are relative to `tools/`. Gradle commands run from `java/` with
`./gradlew`. Existing CLI flags, default folders and comparison output formats
are preserved by the shared-helper cleanup.

| Tool | Purpose and inputs | Outputs | Java capture pairing |
| --- | --- | --- | --- |
| `intro/compare.py` | ROM intro execution; Java intro frames; `--all`, `--frames`, `--seed`, `--reuse-original`, `--output` | Original frames, manifests, RGB555 statistics and HTML in `java/build/intro-comparison` | `captureIntro` |
| `menu/compare.py` | `--capture` reaches synthetic save-summary menu scenarios; default compares Java frames | Scenario manifest/TSV, original frames, statistics and HTML in `java/build/menu-comparison` | Capture original first, then `captureMenu`, then compare |
| `house/compare.py` | `--capture` records settled house clocks/frames; default compares Java frames | Clocks, frames, results and HTML in `java/build/house-comparison` | `captureHouse` |
| `dialog/compare.py` | `--capture`, `--scenario` select opening dialog/page/bed-exit checkpoints | Clocks, page timings, frames, results and HTML in `java/build/dialog-comparison` | `captureDialog -PdialogScenario=<scenario>` |
| `opening/capture.py` | ROM plus `--route` whitespace TSV and `--output`; ordinary inputs with disclosed ROM overrides | Original checkpoint frames, trace TSVs and completion manifest in `java/build/opening-route` by default | `captureOpeningRoute` with the same route/output properties; see task definition |
| `opening/compare.py` | Both completion manifests and frame folders at `--output`; verifies route hashes/counts | RGB555 `results.json`, HTML; nonzero exit on differences | `captureOpeningRoute` |
| `verify_opening.py` | Orchestrates intro/menu/house and all dialog scenarios; needs system `gradle` on PATH | Comparison folders and `java/build/opening-verification.json`; stops on failure | Runs `test`, `captureIntro`, `captureMenu`, `captureHouse`, `captureDialog` |
| `audio/compare.py` | `--rom`, `--frames`, `--output`; ROM intro PCM/noise plus Java audio traces | Emulator WAV/JSONL, timing/noise/register summary in `/tmp/ladx-intro-audio` | `-I ../tools/audio/capture.gradle captureIntroAudio` |
| `audio/trace_noise_writes.py` | `--rom`, `--frames`, `--output`; original noise register hooks | Noise-write JSONL (default `/tmp/ladx-intro-audio/noise-writes.jsonl`) | `captureIntroAudio` register traces via the audio init script |
| `audio/compare_isolated_noise.py` | `--rom`, `--output`, optional `--before-wav`; isolated noise cases and Java PCM | WAVs and isolated-noise metrics in `/tmp/ladx-intro-audio` | `-I ../tools/audio/capture.gradle captureIsolatedNoise` |
| `sword-rng/capture.py` | `--rom`, `--inputs` (`inputs.tsv`), `--output`; disclosed state setup | RNG/state JSONL and setup hashes in `/tmp/ladx-sword-rng` | `-I ../tools/sword-rng/capture.gradle captureSwordRng` |
| `sword-rng/compare.py` | `--output`; emulator/engine RNG and state traces | `comparison.json` and printed summary | `captureSwordRng` via its init script |
| `slime-eye/capture.py` | `--rom`, `--output`; injected handler states/fixed poses | Original trace TSVs and local frames in `java/build/slime-eye` | No registered task; test-runtime probe harnesses |
| `slime-eye/compare.py` | `--output`; engine/emulator fixed-pose probes | Trace/image comparison JSON | No registered task; see [evidence](../docs/evidence/slime-eye-half-motion.md) |
| `slime-eye/live.py` | `--rom`, `--output`; normal buttons after disclosed D3 setup | Live emulator trace/checkpoint frames in `java/build/slime-eye/controls` | No registered task; test-runtime encounter harness |
| `slime-eye/live_compare.py` | `--output`; paired live-fight traces and frames | Bounded fight/checkpoint comparison JSON | See [controls evidence](../docs/evidence/slime-eye-controls.md) |
| `entry/capture_rom.py` | Local ROM and disassembly symbols; optional `--visual`; injected room-edge probes | Local state/VRAM/OAM traces and optional frames under `/tmp` | `entry/EntryProbe.java`, manual test-runtime classpath; no Gradle task |
| `entry/EntryProbe.java` | Java headless house route, then injected edge/fractional positions; optional timing args | Printed positions and local PNGs under `/tmp` | Manual test-runtime probe; no registered task |
| `leever/capture_rom.py` | Local ROM; `--case`, required `--output`; controlled contact fixtures | Contact trace/checkpoint evidence | No registered task; see [recoil evidence](../docs/evidence/leever-contact-recoil.md) |
| `lifting/probe.py` | Local ROM and `LADX-Disassembly/azle.sym`; injected lifting state | Printed per-frame observations | No registered task; isolated ROM routine probe |
| `rom/gen_symbols.py` | RGBDS `.sym` and exact-label list (`rom/symbols.txt`); see CLI help | Generated `RomSymbols.java` | No capture task; see [ROM symbols](../docs/ROM_SYMBOLS.md) |
| `rom/test_gen_symbols.py` | Synthetic symbol text and checked-in Java declarations | Unit assertions; optional local-ROM check skips when absent | None |
| `common.py`, `test_common.py` | Shared root/default ROM paths, output argument/folder helpers, RGB555 conversion and pixel masks | Reusable helpers and synthetic unit assertions | None |

`audio/capture.gradle` and `sword-rng/capture.gradle` register local capture
JavaExec tasks against the test runtime; they are not public-CI tasks.
`opening/*.tsv` and `sword-rng/inputs.tsv` are input records, not generated assets.
See [TESTING.md](../docs/TESTING.md) for opening route columns, checkpoint evidence
and the limits of a parity claim.
