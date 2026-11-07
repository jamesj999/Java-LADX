# Bush Breakables Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add disassembly-faithful overworld sword interaction for plain bushes and bush-covered stairs, including room-object mutation and redraw synchronization.

**Architecture:** Add a small overworld interaction helper that computes the sword-hit object cell from ROM tables, mutates the active room object state for supported bush object ids, and asks the main room renderer to refresh the changed cell from the authoritative object/overlay path. Keep sword animation and Link movement separate from map mutation.

**Tech Stack:** Java 21, JUnit 5, existing ROM-backed room/object renderer.

---

### Task 1: Add failing tests for bush reveal rules

**Files:**
- Create: `java/src/test/java/linksawakening/world/OverworldBushInteractionTest.java`
- Modify: `java/build.gradle`
- Test: `java/src/test/java/linksawakening/world/OverworldBushInteractionTest.java`

- [ ] **Step 1: Write the failing tests**
- [ ] **Step 2: Run the targeted test task and verify it fails**
- [ ] **Step 3: Add the minimal production scaffolding needed for compilation only**
- [ ] **Step 4: Re-run the targeted tests and verify they now fail on behavior**

### Task 2: Implement bush interaction core logic

**Files:**
- Create: `java/src/main/java/linksawakening/world/OverworldBushInteraction.java`
- Modify: `java/src/main/java/linksawakening/rom/RomTables.java`
- Test: `java/src/test/java/linksawakening/world/OverworldBushInteractionTest.java`

- [ ] **Step 1: Implement ROM-backed sword-hit cell lookup helpers**
- [ ] **Step 2: Implement plain bush reveal rules**
- [ ] **Step 3: Implement bush-covered stairs reveal rules**
- [ ] **Step 4: Run targeted tests and verify they pass**

### Task 3: Integrate interaction with mutable room state and redraw

**Files:**
- Modify: `java/src/main/java/linksawakening/Main.java`
- Modify: `java/src/main/java/linksawakening/equipment/Sword.java`
- Test: `java/src/test/java/linksawakening/world/OverworldBushInteractionTest.java`

- [ ] **Step 1: Expose the minimum sword collision-window state needed by the interaction layer**
- [ ] **Step 2: Add an overworld interaction call in the frame update path**
- [ ] **Step 3: Synchronize the mutable GBC backup object state and refresh the changed room tiles**
- [ ] **Step 4: Run targeted tests and verify they pass**

### Task 4: Full verification

**Files:**
- Test: `java/src/test/java/linksawakening/world/OverworldBushInteractionTest.java`
- Test: `java/src/test/java/linksawakening/equipment/SwordTest.java`

- [ ] **Step 1: Run the new targeted tests**
- [ ] **Step 2: Run the existing sword tests**
- [ ] **Step 3: Run the full Gradle test suite**
- [ ] **Step 4: Review for residual gaps against the spec**
