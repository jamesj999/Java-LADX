#!/usr/bin/env python3
"""Run headless original-ROM visual checks in opening-game order.

    python3 -m venv /tmp/ladx-visual-venv
    /tmp/ladx-visual-venv/bin/pip install -r tools/intro/requirements.txt
    /tmp/ladx-visual-venv/bin/python tools/verify_opening.py

Requires Java 21+ and Gradle on PATH. Uses the shipped classpath ROM; output is
under java/build. Emulator saves are never persisted. Existing comparisons have
explicit limitations: random intro rain is masked; menu phases align once per
scenario; house and dialogue captures synchronize initial NPC clocks; dialogue
checks settled pages and poses, not all transition frames or a Java input replay.
Any failed command stops the run with a nonzero exit code. Golden test fixtures
are deliberately not updated by this runner.
"""
import json
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def run(*args):
    print('+ ' + ' '.join(map(str, args)), flush=True)
    subprocess.run(args, cwd=ROOT, check=True)


def python(script, *args):
    run(sys.executable, script, *args)


def gradle(*args):
    run('gradle', '-p', 'java', *args)


def main():
    gradle('test', 'captureIntro')
    python('tools/intro/compare.py', '--all')
    python('tools/menu/compare.py', '--capture')
    gradle('captureMenu')
    python('tools/menu/compare.py')
    python('tools/house/compare.py', '--capture')
    gradle('captureHouse')
    python('tools/house/compare.py')
    results = {}
    for scenario in ('marin', 'tarin-offer', 'shield-pose', 'tarin-shield'):
        python('tools/dialog/compare.py', '--capture', '--scenario', scenario)
        gradle('captureDialog', '-PdialogScenario=' + scenario)
        python('tools/dialog/compare.py', '--scenario', scenario)
        folder = ROOT / 'java/build/dialog-comparison'
        if scenario != 'marin':
            folder /= scenario
        result = json.loads((folder / 'results.json').read_text())
        results[scenario] = {key: result[key] for key in ('frames', 'exact', 'max_pixels')}
    (ROOT / 'java/build/opening-verification.json').write_text(json.dumps(results, indent=2) + '\n')
    print('Opening verification passed. See java/build/*-comparison/report.html.')


if __name__ == '__main__':
    main()
