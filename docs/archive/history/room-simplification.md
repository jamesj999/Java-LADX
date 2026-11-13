# Room simplification

## Baseline

Scope: world/room, world/transition, world/object main and tests. Counts use
physical lines; null checks are equality/inequality comparisons with null.
Small classes use nonblank, non-import/package lines after stripping comments
(including braces). Numbered tests match `[12]Test` or `Support2`.

```text
main {"files": 155, "lines": 16361, "wildcards": 95, "null_checks": 328, "under40": 67, "numbered": 0}
test {"files": 81, "lines": 18327, "wildcards": 84, "null_checks": 140, "under40": 19, "numbered": 8}
```
