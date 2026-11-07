# Building

This is an open source engine reimplementation for preservation and research.
Supply your own English Link's Awakening DX v1.0 ROM; ROMs and extracted
assets must never be committed or distributed.

Install JDK 21. From java/, use ./gradlew (gradlew.bat on Windows).
The wrapper pins Gradle 9.8.1. No system Gradle is required.

Place your ROM at java/src/main/resources/rom/azle.gbc, or pass
-Pla.rom=/absolute/path/azle.gbc or set LA_ROM. Run ./gradlew run.
The verifyRom helper reports a non-fatal MD5 mismatch against
07c211479386825042efb4ad31bb525f, from LADX-Disassembly/ladx.md5.

./gradlew test runs all tests when a ROM is available. Without one,
ROM integration tests are skipped with a reason. ./gradlew romFreeTest
excludes tests tagged rom. Mark every new ROM integration test class with
@linksawakening.testing.RequiresRom (including indirect ROM consumers).
This composed annotation supplies the JUnit rom tag and skips tests when the
ROM is absent. Use TestRom.open() for test ROM resource reads; unmarked test
classes fail with an instruction to add @RequiresRom. No classifier list is needed.

./gradlew distZip builds a distribution for the current platform.
Override with -Pplatform=macos, macos-arm64, linux, linux-arm64, or windows.
-PallPlatforms includes all five native classifiers. Distribution archives
exclude ROMs and saves. Supply your ROM separately when running.
The macOS run task adds -XstartOnFirstThread; when launching a distribution
on macOS, set JAVA_OPTS=-XstartOnFirstThread.

Emulator comparisons live in tools/{intro,menu,house,dialog,opening}.
Install Python dependencies with python3 -m pip install -r tools/requirements.txt.
Run the relevant compare.py --help (opening also has capture.py) for local
capture and comparison options. These tools require your own ROM and may
generate screenshots and reference data: never publish extracted assets.
