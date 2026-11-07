# Intro Cutscene Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the static intro placeholder with a ROM-backed intro cutscene path that preserves the Game Boy BG map model and animates the opening sequence.

**Architecture:** Decode encoded BG commands into full 32x32 `$9800` maps, render a scrolled viewport, and add a stateful intro runtime that mirrors the important `intro.asm` subtypes. Keep the first pass visual-first: audio calls are represented as no-op events.

**Tech Stack:** Java, Gradle, JUnit, existing ROM/GPU/render packages.

---

### Task 1: Full BG Map Rendering

**Files:**
- Modify: `java/src/main/java/linksawakening/rom/RomBank.java`
- Modify: `java/src/main/java/linksawakening/scene/BackgroundScene.java`
- Modify: `java/src/main/java/linksawakening/scene/BackgroundSceneLoader.java`
- Modify: `java/src/main/java/linksawakening/render/IndexedRenderer.java`
- Test: `java/src/test/java/linksawakening/scene/BackgroundSceneLoaderTest.java`

- [ ] Add tests proving decoded intro scenes are full `32x32` maps and preserve command positions.
- [ ] Decode draw commands relative to `$9800` instead of compacting from minimum address.
- [ ] Render a `20x18` viewport using scroll X/Y.

### Task 2: Intro Runtime State

**Files:**
- Create: `java/src/main/java/linksawakening/cutscene/IntroSequence.java`
- Modify: `java/src/main/java/linksawakening/cutscene/CutsceneManager.java`
- Modify: `java/src/main/java/linksawakening/Main.java`
- Test: `java/src/test/java/linksawakening/cutscene/IntroSequenceTest.java`

- [ ] Add a frame-ticked intro state machine with sea, Link face, beach, title reveal, and title hold states.
- [ ] Expose render state: scene id, scroll X/Y, and sprite overlays.
- [ ] Keep existing config-driven startup path.

### Task 3: Visual Animation Tables

**Files:**
- Create: `java/src/main/java/linksawakening/cutscene/IntroSprite.java`
- Modify: `java/src/main/java/linksawakening/cutscene/IntroSequence.java`
- Modify: `java/src/main/java/linksawakening/render/IndexedRenderer.java`
- Test: `java/src/test/java/linksawakening/cutscene/IntroSequenceTest.java`

- [ ] Port the essential intro OAM tables: ship, rain, lightning, Marin/Link placeholders, and title sparkles.
- [ ] Apply title logo reveal draw commands row by row.
- [ ] Render overlays through existing sprite tile drawing.
