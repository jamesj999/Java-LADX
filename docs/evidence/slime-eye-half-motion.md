# Slime Eye implementation evidence — bounded preliminary work

Source: LADX-Disassembly/src/code/entities/04_slime_eye.asm.
Implemented only pure half-eye states 2 and 3 (04:4F65–4FAD) in
SlimeEyeHalfMotion. This helper is not yet connected to entity dispatch.
State 2 sets Z speed $60; after Z integration, unsigned Z >= $70 enters
state 3 with countdown $30. State 3 hides the sprite ($FF), waits, then
sets state 1, countdown $18, private state 4 = 1, Z speed $C0,
coordinates to Link, and requests jingle $08.

Focused synthetic tests cover threshold, byte wrapping, retained animation,
waiting position, and drop outputs. These tests do not establish encounter
fidelity. Focused Gradle execution passed SlimeEyeHalfMotionTest and required
WorldDependencyTest, MainArchitectureTest, GlBoundaryTest,
WorldRomConstantGuardTest. No full suite was run.

Outstanding: registry/init integration, main body, ceiling shake trigger,
Zol spawning, stretch/combat/splitting, half-eye movement and damage,
slot-aware death/reward, ROM-backed rendering and sound consumption.
No emulator comparison, screenshot, RAM injection, or route replay was
performed. No normal-route D3 verification is claimed. No ROM assets added.
