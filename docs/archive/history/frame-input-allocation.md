# Frame input allocation consolidation

The app uses borrowed read-only entity, combat, pickup and ground registers. Each
holder belongs to its phase/factory and expires at the next capture. Room boundary
probes belong to the loaded RoomRuntime collision cache and expire at the next
probe. Consumers finish synchronously before refill; no per-frame wrapper is made.

Owned EntityFrameInputs, EntityTickInputs, EntityCombatInputs, EntityPickupInputs,
LinkGroundFrame and RoomBoundaryState constructors remain available. The factory's
create() keeps immutable history, tested across 240 scripted frames; borrow() is
a separate immediate-consumption API. Player/UI/button/sword leaves remain
immutable, cached values because owned captures retain them. EnemyAttackContext
also retains its public immutable combat-value contract.

EntityFrameContext and its projectile LinkState remain immutable: public
EntityHandler.advance callbacks receive that context and may retain it. Changing
those values into borrowed holders would weaken that public contract. Their
creation accounts for approximately 136 B of the remaining active-frame cost.
Changed entities and emitted events also retain owned value semantics.

EA-off bisection: initial walking 580.3 B/frame; borrowed app/room phase inputs
299.5; per-room borrowed boundary probes 139.5. Active update-only: 434 -> 137.
Idle and open inventory remain exactly zero. VFX rendering now uses liveSlots()
instead of constructing owned slot snapshots; all four representative renderer
fixtures measure zero instead of the previous 264 B/frame.

Whole-frame budgets: idle 0, walking 175, inventory 0 B/frame. Active simulation
budget: 172 B/frame; idle remains zero. Positive budgets add about 25% headroom.
AllocationProbe uses warmed quietest windows; results are scoped regression
measurements, not game-wide performance guarantees. EA-off settings stay local.
