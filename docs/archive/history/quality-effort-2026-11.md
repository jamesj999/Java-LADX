# November 2026 quality effort

## Integration and hardening

- A1: render assembly now checks `hasRoom()` and reads required room metadata; all room role callers use explicitly nullable projection names. The four old nullable role methods are deleted. Full ROM gate: 2,985 tests, zero failures/errors; scene parity remains identical.
